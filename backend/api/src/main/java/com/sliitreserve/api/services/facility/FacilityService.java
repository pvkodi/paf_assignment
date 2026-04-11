package com.sliitreserve.api.services.facility;

import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.facility.Facility.FacilityStatus;
import com.sliitreserve.api.entities.facility.Facility.FacilityType;
import com.sliitreserve.api.dto.FacilityResponseDTO;
import com.sliitreserve.api.exception.ResourceNotFoundException;
import com.sliitreserve.api.repositories.FacilityRepository;
import com.sliitreserve.api.util.mapping.FacilityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for Facility query and search operations.
 * Handles facility discovery with support for filtering by type, capacity, location, and building.
 * All queries return only ACTIVE facilities by default.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final FacilityMapper facilityMapper;

    /**
     * Get facility by ID.
     *
     * @param facilityId UUID of the facility
     * @return FacilityResponseDTO
     * @throws ResourceNotFoundException if facility not found
     */
    public FacilityResponseDTO getFacilityById(UUID facilityId) {
        log.debug("Fetching facility with ID: {}", facilityId);
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> {
                    log.warn("Facility not found with ID: {}", facilityId);
                    return new ResourceNotFoundException("Facility not found with ID: " + facilityId);
                });
        return facilityMapper.toResponseDTO(facility);
    }

    /**
     * Get facility by facility code.
     *
     * @param facilityCode Unique facility code
     * @return FacilityResponseDTO
     * @throws ResourceNotFoundException if facility not found
     */
    public FacilityResponseDTO getFacilityByCode(String facilityCode) {
        log.debug("Fetching facility with code: {}", facilityCode);
        Facility facility = facilityRepository.findByFacilityCode(facilityCode)
                .orElseThrow(() -> {
                    log.warn("Facility not found with code: {}", facilityCode);
                    return new ResourceNotFoundException("Facility not found with code: " + facilityCode);
                });
        return facilityMapper.toResponseDTO(facility);
    }

    /**
     * Get all active facilities.
     *
     * @return List of all active FacilityResponseDTOs
     */
    public List<FacilityResponseDTO> getAllActiveFacilities() {
        log.debug("Fetching all active facilities");
        List<Facility> facilities = facilityRepository.findByStatus(FacilityStatus.ACTIVE);
        return facilities.stream()
                .map(facilityMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all facilities of a specific type (active only).
     *
     * @param type Facility type (LECTURE_HALL, LAB, MEETING_ROOM, AUDITORIUM, EQUIPMENT, SPORTS_FACILITY)
     * @return List of active facilities of specified type
     */
    public List<FacilityResponseDTO> getFacilitiesByType(FacilityType type) {
        log.debug("Fetching facilities of type: {}", type);
        List<Facility> facilities = facilityRepository.findByTypeAndStatus(type, FacilityStatus.ACTIVE);
        return facilities.stream()
                .map(facilityMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get active facilities with minimum capacity.
     *
     * @param minCapacity Minimum required capacity
     * @return List of active facilities meeting capacity requirement, sorted by capacity DESC
     */
    public List<FacilityResponseDTO> getFacilitiesByMinCapacity(Integer minCapacity) {
        log.debug("Fetching facilities with minimum capacity: {}", minCapacity);
        List<Facility> facilities = facilityRepository.findActiveWithMinCapacity(minCapacity, FacilityStatus.ACTIVE);
        return facilities.stream()
                .map(facilityMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get facilities by location (active only).
     *
     * @param location Location name (case-insensitive)
     * @return List of active facilities at location
     */
    public List<FacilityResponseDTO> getFacilitiesByLocation(String location) {
        log.debug("Fetching facilities at location: {}", location);
        List<Facility> facilities = facilityRepository.findByLocationContainingIgnoreCase(location);
        
        // Filter to only active facilities
        List<Facility> activeFacilities = facilities.stream()
                .filter(f -> f.getStatus() == FacilityStatus.ACTIVE)
                .collect(Collectors.toList());
        
        return activeFacilities.stream()
                .map(facilityMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get facilities by building (active only).
     *
     * @param building Building name
     * @return List of active facilities in building
     */
    public List<FacilityResponseDTO> getFacilitiesByBuilding(String building) {
        log.debug("Fetching facilities in building: {}", building);
        List<Facility> facilities = facilityRepository.findByBuilding(building);
        
        // Filter to only active facilities
        List<Facility> activeFacilities = facilities.stream()
                .filter(f -> f.getStatus() == FacilityStatus.ACTIVE)
                .collect(Collectors.toList());
        
        return activeFacilities.stream()
                .map(facilityMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get facilities by building and floor (active only).
     *
     * @param building Building name
     * @param floor Floor number/letter
     * @return List of active facilities on floor
     */
    public List<FacilityResponseDTO> getFacilitiesByBuildingAndFloor(String building, String floor) {
        log.debug("Fetching facilities in building: {}, floor: {}", building, floor);
        List<Facility> facilities = facilityRepository.findByBuildingAndFloor(building, floor);
        
        // Filter to only active facilities
        List<Facility> activeFacilities = facilities.stream()
                .filter(f -> f.getStatus() == FacilityStatus.ACTIVE)
                .collect(Collectors.toList());
        
        return activeFacilities.stream()
                .map(facilityMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Search facilities by name pattern (active only).
     *
     * @param namePattern Search pattern (case-insensitive, partial match)
     * @return List of active facilities matching pattern
     */
    public List<FacilityResponseDTO> searchFacilitiesByName(String namePattern) {
        log.debug("Searching facilities by name pattern: {}", namePattern);
        List<Facility> facilities = facilityRepository.findActiveByNamePattern(namePattern, FacilityStatus.ACTIVE);
        return facilities.stream()
                .map(facilityMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Complex facility search with multiple filter criteria.
     * Only returns ACTIVE facilities. All filter parameters are optional.
     *
     * @param type Facility type (optional)
     * @param minCapacity Minimum capacity (optional)
     * @param location Location name (optional, case-insensitive)
     * @param building Building name (optional)
     * @return List of active facilities matching all specified criteria
     */
    public List<FacilityResponseDTO> searchFacilities(
            Optional<FacilityType> type,
            Optional<Integer> minCapacity,
            Optional<String> location,
            Optional<String> building) {

        log.debug("Searching facilities with filters - type: {}, minCapacity: {}, location: {}, building: {}",
                type, minCapacity, location, building);

        List<Facility> allActiveFacilities = facilityRepository.findByStatus(FacilityStatus.ACTIVE);

        // Apply type filter
        if (type.isPresent()) {
            allActiveFacilities = allActiveFacilities.stream()
                    .filter(f -> f.getType() == type.get())
                    .collect(Collectors.toList());
        }

        // Apply minimum capacity filter
        if (minCapacity.isPresent()) {
            allActiveFacilities = allActiveFacilities.stream()
                    .filter(f -> f.getCapacity() >= minCapacity.get())
                    .collect(Collectors.toList());
        }

        // Apply location filter
        if (location.isPresent()) {
            String locationFilter = location.get().toLowerCase();
            allActiveFacilities = allActiveFacilities.stream()
                    .filter(f -> f.getLocation() != null && 
                                 f.getLocation().toLowerCase().contains(locationFilter))
                    .collect(Collectors.toList());
        }

        // Apply building filter
        if (building.isPresent()) {
            String buildingFilter = building.get().toLowerCase();
            allActiveFacilities = allActiveFacilities.stream()
                    .filter(f -> f.getBuilding() != null && 
                                 f.getBuilding().toLowerCase().contains(buildingFilter))
                    .collect(Collectors.toList());
        }

        log.debug("Search returned {} facilities", allActiveFacilities.size());

        return allActiveFacilities.stream()
                .map(facilityMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get facility count by type.
     *
     * @param type Facility type
     * @return Number of facilities of specified type
     */
    public long countFacilitiesByType(FacilityType type) {
        log.debug("Counting facilities of type: {}", type);
        return facilityRepository.countByTypeAndStatus(type, FacilityStatus.ACTIVE);
    }

    /**
     * Get total count of active facilities.
     *
     * @return Total count of active facilities
     */
    public long countActiveFacilities() {
        log.debug("Counting all active facilities");
        return facilityRepository.countByStatus(FacilityStatus.ACTIVE);
    }

    /**
     * Check if facility exists and is active.
     *
     * @param facilityId UUID of facility
     * @return true if facility exists and is active
     */
    public boolean isActiveFacilityExists(UUID facilityId) {
        return facilityRepository.findById(facilityId)
                .map(f -> f.getStatus() == FacilityStatus.ACTIVE)
                .orElse(false);
    }

    /**
     * Check if facility code exists.
     *
     * @param facilityCode Facility code
     * @return true if facility code exists
     */
    public boolean facilityCodeExists(String facilityCode) {
        return facilityRepository.existsByFacilityCode(facilityCode);
    }
}
