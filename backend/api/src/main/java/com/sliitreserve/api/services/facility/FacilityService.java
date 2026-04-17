package com.sliitreserve.api.services.facility;

import com.sliitreserve.api.dto.facility.AvailabilityWindowDTO;
import com.sliitreserve.api.dto.facility.FacilityRequestDTO;
import com.sliitreserve.api.dto.facility.FacilityResponseDTO;
import com.sliitreserve.api.entities.facility.AvailabilityWindow;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.facility.Facility.FacilityType;
import com.sliitreserve.api.exception.ConflictException;
import com.sliitreserve.api.exception.ResourceNotFoundException;
import com.sliitreserve.api.exception.ValidationException;
import com.sliitreserve.api.factories.FacilityFactory;
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import com.sliitreserve.api.repositories.specifications.FacilitySpecifications;
import com.sliitreserve.api.services.integration.MaintenanceIntegrationService;
import com.sliitreserve.api.util.mapping.FacilityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final FacilityFactory facilityFactory;
    private final FacilityMapper facilityMapper;
    private final MaintenanceIntegrationService maintenanceIntegrationService;
    private final FacilityTimetableService facilityTimetableService;

    public Page<FacilityResponseDTO> listFacilities(Pageable pageable) {
        return facilityRepository.findAll(pageable).map(facilityMapper::toResponseDTO);
    }

    public FacilityResponseDTO getFacilityById(UUID facilityId) {
        return facilityMapper.toResponseDTO(getFacilityEntity(facilityId));
    }

    public Page<FacilityResponseDTO> searchFacilities(
            FacilityType type,
            Integer minCapacity,
            String building,
            String location,
            Facility.FacilityStatus status,
            Pageable pageable
    ) {
        Specification<Facility> specification = (root, query, cb) -> cb.conjunction();

        if (type != null) {
            specification = specification.and(FacilitySpecifications.ofType(normalizeType(type)));
        }
        if (minCapacity != null) {
            specification = specification.and(FacilitySpecifications.hasMinCapacity(minCapacity));
        }
        if (building != null && !building.isBlank()) {
            specification = specification.and(FacilitySpecifications.inBuilding(building));
        }
        if (location != null && !location.isBlank()) {
            specification = specification.and(FacilitySpecifications.locationContains(location));
        }
        if (status != null) {
            specification = specification.and(FacilitySpecifications.hasStatus(status));
        }

        return facilityRepository.findAll(specification, pageable).map(facilityMapper::toResponseDTO);
    }

    @Transactional
    public FacilityResponseDTO createFacility(FacilityRequestDTO request) {
        FacilityRequestDTO normalizedRequest = normalizeRequestType(request);
        validateTimeRange(normalizedRequest.getAvailabilityStartTime(), normalizedRequest.getAvailabilityEndTime());

        if (normalizedRequest.getFacilityCode() != null
                && !normalizedRequest.getFacilityCode().isBlank()
                && facilityRepository.existsByFacilityCode(normalizedRequest.getFacilityCode().trim())) {
            throw new ConflictException("Facility code already exists");
        }

        Facility facility = facilityFactory.createFacility(normalizedRequest);

        // Apply multi-window availability schedule
        if (normalizedRequest.getAvailabilityWindows() != null && !normalizedRequest.getAvailabilityWindows().isEmpty()) {
            List<AvailabilityWindow> windows = normalizedRequest.getAvailabilityWindows().stream()
                .map(dto -> AvailabilityWindow.builder()
                    .dayOfWeek(dto.getDayOfWeek())
                    .startTime(dto.getStartTime())
                    .endTime(dto.getEndTime())
                    .build())
                .collect(Collectors.toList());
            facility.getAvailabilityWindows().addAll(windows);
        }

        Facility savedFacility = facilityRepository.save(facility);
        return facilityMapper.toResponseDTO(savedFacility);
    }

    @Transactional
    public FacilityResponseDTO updateFacility(UUID facilityId, FacilityRequestDTO request) {
        Facility existingFacility = getFacilityEntity(facilityId);
        FacilityRequestDTO normalizedRequest = normalizeRequestType(request);

        validateTimeRange(normalizedRequest.getAvailabilityStartTime(), normalizedRequest.getAvailabilityEndTime());

        if (normalizedRequest.getType() != null && normalizeType(normalizedRequest.getType()) != normalizeType(existingFacility.getType())) {
            throw new ValidationException("Facility type change is not supported in this module");
        }

        if (normalizedRequest.getFacilityCode() != null
                && !normalizedRequest.getFacilityCode().isBlank()
                && !normalizedRequest.getFacilityCode().equals(existingFacility.getFacilityCode())
                && facilityRepository.existsByFacilityCode(normalizedRequest.getFacilityCode().trim())) {
            throw new ConflictException("Facility code already exists");
        }

        facilityMapper.updateEntity(normalizedRequest, existingFacility);
        facilityFactory.applySubtypeAttributes(existingFacility, normalizedRequest.getSubtypeAttributes());
        if (normalizedRequest.getStatus() == null) {
            existingFacility.setStatus(existingFacility.getStatus());
        }

        Facility updatedFacility = facilityRepository.save(existingFacility);
        return facilityMapper.toResponseDTO(updatedFacility);
    }

    @Transactional
    public void markOutOfService(UUID facilityId) {
        Facility facility = getFacilityEntity(facilityId);
        facility.setStatus(Facility.FacilityStatus.OUT_OF_SERVICE);
        facilityRepository.save(facility);
    }

    public boolean isFacilityOperational(UUID facilityId, LocalDateTime start, LocalDateTime end) {
        Facility facility = getFacilityEntity(facilityId);
        validateDateTimeRange(start, end);

        if (facility.getStatus() == Facility.FacilityStatus.OUT_OF_SERVICE) {
            return false;
        }
        if (facility.getStatus() == Facility.FacilityStatus.MAINTENANCE) {
            return false;
        }

        // Check each hour in the requested range against the availability schedule
        LocalDateTime current = start;
        while (current.isBefore(end)) {
            DayOfWeek day = current.getDayOfWeek();
            LocalTime time = current.toLocalTime();

            // Check availability windows (multi-window schedule)
            if (!facility.isAvailableAt(day, time)) {
                return false;
            }

            // Check timetable occupancy
            if (facilityTimetableService.isOccupied(facility.getFacilityCode(), day, time)) {
                return false;
            }

            current = current.plusHours(1);
        }

        // Check for maintenance tickets
        if (maintenanceIntegrationService.isFacilityUnderMaintenance(facilityId, start, end)) {
            return false;
        }

        return true;
    }

    public List<Facility> findActiveByType(FacilityType type) {
        return facilityRepository.findByTypeAndStatus(normalizeType(type), Facility.FacilityStatus.ACTIVE);
    }

    private Facility getFacilityEntity(UUID facilityId) {
        return facilityRepository.findById(facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility", String.valueOf(facilityId)));
    }

    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new ValidationException("Invalid availability time range");
        }
    }

    private void validateDateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new ValidationException("Invalid date-time range");
        }
    }

    private FacilityRequestDTO normalizeRequestType(FacilityRequestDTO request) {
        if (request != null && request.getType() == FacilityType.SPORTS_FACILITY) {
            request.setType(FacilityType.SPORTS);
        }
        return request;
    }

    private FacilityType normalizeType(FacilityType type) {
        return type == FacilityType.SPORTS_FACILITY ? FacilityType.SPORTS : type;
    }

    public List<Facility> searchFacilities(Boolean active, FacilityType type, Integer minCapacity, String building, String namePattern) {
        Specification<Facility> spec = null;

        if (Boolean.TRUE.equals(active)) {
            spec = (spec == null) ? FacilitySpecifications.isActive() : spec.and(FacilitySpecifications.isActive());
        }

        if (type != null) {
            spec = (spec == null) ? FacilitySpecifications.ofType(type) : spec.and(FacilitySpecifications.ofType(type));
        }

        if (minCapacity != null) {
            spec = (spec == null) ? FacilitySpecifications.hasMinCapacity(minCapacity) : spec.and(FacilitySpecifications.hasMinCapacity(minCapacity));
        }

        if (building != null && !building.isEmpty()) {
            spec = (spec == null) ? FacilitySpecifications.inBuilding(building) : spec.and(FacilitySpecifications.inBuilding(building));
        }

        if (namePattern != null && !namePattern.isEmpty()) {
            spec = (spec == null) ? FacilitySpecifications.nameContains(namePattern) : spec.and(FacilitySpecifications.nameContains(namePattern));
        }

        if (spec == null) {
            return facilityRepository.findAll();
        }

        return facilityRepository.findAll(spec);
    }

    public Facility findByCode(String facilityCode) {
        return facilityRepository.findByFacilityCode(facilityCode).orElse(null);
    }
}
