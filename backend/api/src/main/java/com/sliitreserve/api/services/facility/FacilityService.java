package com.sliitreserve.api.services.facility;

import com.sliitreserve.api.dto.facility.FacilityRequestDTO;
import com.sliitreserve.api.dto.facility.FacilityResponseDTO;
import com.sliitreserve.api.entities.facility.AvailabilityWindow;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.exception.ConflictException;
import com.sliitreserve.api.exception.ResourceNotFoundException;
import com.sliitreserve.api.exception.ValidationException;
import com.sliitreserve.api.factories.FacilityFactory;
import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.entities.booking.BookingStatus;
import com.sliitreserve.api.entities.booking.CheckInRecord;
import com.sliitreserve.api.entities.booking.ApprovalStep;
import com.sliitreserve.api.entities.ticket.MaintenanceTicket;
import com.sliitreserve.api.entities.analytics.UtilizationSnapshot;
import com.sliitreserve.api.observers.EventEnvelope;
import com.sliitreserve.api.observers.EventPublisher;
import com.sliitreserve.api.observers.EventSeverity;
import com.sliitreserve.api.repositories.bookings.BookingRepository;
import com.sliitreserve.api.repositories.bookings.CheckInRepository;
import com.sliitreserve.api.repositories.bookings.ApprovalStepRepository;
import com.sliitreserve.api.repositories.facility.UtilizationSnapshotRepository;
import com.sliitreserve.api.repositories.ticket.MaintenanceTicketRepository;
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import com.sliitreserve.api.repositories.specifications.FacilitySpecifications;
import com.sliitreserve.api.services.integration.MaintenanceIntegrationService;
import com.sliitreserve.api.util.mapping.FacilityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final FacilityFactory facilityFactory;
    private final FacilityMapper facilityMapper;
    private final MaintenanceIntegrationService maintenanceIntegrationService;
    private final FacilityTimetableService facilityTimetableService;
    private final BookingRepository bookingRepository;
    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final UtilizationSnapshotRepository utilizationSnapshotRepository;
    private final CheckInRepository checkInRepository;
    private final ApprovalStepRepository approvalStepRepository;
    private final EventPublisher notificationService;

    public Page<FacilityResponseDTO> listFacilities(Pageable pageable) {
        return facilityRepository.findAll(pageable).map(facilityMapper::toResponseDTO);
    }

    public FacilityResponseDTO getFacilityById(UUID facilityId) {
        return facilityMapper.toResponseDTO(getFacilityEntity(facilityId));
    }

    public Page<FacilityResponseDTO> searchFacilities(
            Facility.FacilityType type,
            Integer minCapacity,
            String building,
            String location,
            String searchQuery,
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
        if (searchQuery != null && !searchQuery.isBlank()) {
            specification = specification.and(FacilitySpecifications.keywordContains(searchQuery));
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

        // Relaxed validation: only check if times are provided
        if (normalizedRequest.getAvailabilityStartTime() != null && normalizedRequest.getAvailabilityEndTime() != null) {
            validateTimeRange(normalizedRequest.getAvailabilityStartTime(), normalizedRequest.getAvailabilityEndTime());
        }

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
        cancelAllBookings(facilityId, facility.getName());
    }

    @Transactional
    public void deleteFacility(UUID facilityId, boolean force) {
        Facility facility = getFacilityEntity(facilityId);
        
        long bookingCount = bookingRepository.countByFacility_Id(facilityId);
        if (bookingCount > 0 && !force) {
            throw new ConflictException("Facility cannot be deleted because it has " + bookingCount + " active/scheduled bookings. Use 'Force Delete' to cancel all bookings and proceed.");
        }

        if (force && bookingCount > 0) {
            cancelAllBookings(facilityId, facility.getName());
        }

        nukeAssociatedData(facilityId);
        facilityRepository.delete(facility);
    }

    @Transactional
    public void bulkDeactivate(List<UUID> ids) {
        List<Facility> facilities = facilityRepository.findAllById(ids);
        for (Facility facility : facilities) {
            facility.setStatus(Facility.FacilityStatus.OUT_OF_SERVICE);
            facilityRepository.save(facility);
            cancelAllBookings(facility.getId(), facility.getName());
        }
    }

    @Transactional
    public void bulkHardDelete(List<UUID> ids, boolean force) {
        for (UUID id : ids) {
            try {
                deleteFacility(id, force);
            } catch (ConflictException e) {
                if (ids.size() == 1) throw e;
                log.warn("Skipping facility {} due to active bookings in bulk delete", id);
            }
        }
    }

    private void cancelAllBookings(UUID facilityId, String facilityName) {
        List<Booking> bookings = bookingRepository.findByFacility_Id(facilityId);
        LocalDateTime now = LocalDateTime.now();
        for (Booking booking : bookings) {
            if (booking.getStatus() == BookingStatus.PENDING || booking.getStatus() == BookingStatus.APPROVED) {
                if (booking.getBookingDate() == null || booking.getStartTime() == null) {
                    continue;
                }

                LocalDateTime bookingStart = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
                if (bookingStart.isBefore(now)) {
                    continue;
                }

                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);

                // Notify user
                try {
                    notificationService.publish(EventEnvelope.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType("FACILITY_REMOVED_CANCELLED")
                        .severity(EventSeverity.HIGH)
                        .affectedUserId(booking.getRequestedBy().getId().getMostSignificantBits())
                        .title("Booking Cancelled: Facility Unavailable")
                        .description(String.format("We're sorry, your booking for %s on %s has been cancelled because the facility is currently unavailable.", 
                            facilityName, booking.getBookingDate()))
                        .source("FacilityService")
                        .entityReference("booking:" + booking.getId())
                        .metadata(Map.of("userId", booking.getRequestedBy().getId().toString()))
                        .build());
                } catch (Exception e) {
                    log.error("Failed to send cancellation notification for booking {}", booking.getId(), e);
                }
            }
        }
    }

    private void nukeAssociatedData(UUID facilityId) {
        // 1. Utilization Snapshots
        List<UtilizationSnapshot> snapshots = utilizationSnapshotRepository.findByFacility_IdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            facilityId, LocalDate.of(2000, 1, 1), LocalDate.of(2100, 12, 31));
        utilizationSnapshotRepository.deleteAll(snapshots);

        // 2. Maintenance Tickets
        List<MaintenanceTicket> tickets = maintenanceTicketRepository.findByFacilityId(facilityId);
        maintenanceTicketRepository.deleteAll(tickets);
        
        // 3. Bookings and their children (Check-ins, Approvals)
        List<Booking> bookings = bookingRepository.findByFacility_Id(facilityId);
        for (Booking booking : bookings) {
            // Delete Check-ins
            List<CheckInRecord> checkIns = checkInRepository.findByBooking_Id(booking.getId());
            checkInRepository.deleteAll(checkIns);
            
            // Delete Approval Steps
            List<ApprovalStep> approvals = approvalStepRepository.findApprovalHistoryByBookingId(booking.getId());
            approvalStepRepository.deleteAll(approvals);
            
            // Delete the Booking itself
            bookingRepository.delete(booking);
        }
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

    public List<Facility> findActiveByType(Facility.FacilityType type) {
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
        if (request != null && request.getType() == Facility.FacilityType.SPORTS_FACILITY) {
            request.setType(Facility.FacilityType.SPORTS);
        }
        return request;
    }

    private Facility.FacilityType normalizeType(Facility.FacilityType type) {
        return type == Facility.FacilityType.SPORTS_FACILITY ? Facility.FacilityType.SPORTS : type;
    }

    public List<Facility> searchFacilities(Boolean active, Facility.FacilityType type, Integer minCapacity, String building, String namePattern) {
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
