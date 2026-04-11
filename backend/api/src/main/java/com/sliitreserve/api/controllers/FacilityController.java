package com.sliitreserve.api.controllers;

import com.sliitreserve.api.dto.FacilityResponseDTO;
import com.sliitreserve.api.entities.facility.Facility.FacilityType;
import com.sliitreserve.api.services.facility.FacilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for facility discovery and search endpoints.
 *
 * Endpoints:
 * - GET /facilities - Search and filter available facilities
 *   - Supports filtering by type, minCapacity, location, and building
 *   - Returns only ACTIVE facilities
 *   - All authenticated users can search
 */
@RestController
@RequestMapping("/api/v1/facilities")
@RequiredArgsConstructor
@Slf4j
public class FacilityController {

    private final FacilityService facilityService;

    /**
     * Search and filter available facilities.
     *
     * Query parameters (all optional):
     * - type: Facility type (LECTURE_HALL, LAB, MEETING_ROOM, AUDITORIUM, EQUIPMENT, SPORTS_FACILITY)
     * - minCapacity: Minimum required capacity
     * - location: Location name (partial match, case-insensitive)
     * - building: Building name (exact match)
     *
     * Returns only ACTIVE facilities. If no filters are provided, returns all active facilities.
     *
     * @param type Facility type filter (optional)
     * @param minCapacity Minimum capacity filter (optional)
     * @param location Location filter (optional)
     * @param building Building filter (optional)
     * @return List of FacilityResponseDTO matching specified criteria
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FacilityResponseDTO>> searchFacilities(
            @RequestParam(name = "type", required = false) Optional<FacilityType> type,
            @RequestParam(name = "minCapacity", required = false) Optional<Integer> minCapacity,
            @RequestParam(name = "location", required = false) Optional<String> location,
            @RequestParam(name = "building", required = false) Optional<String> building) {

        log.debug("Searching facilities with filters - type: {}, minCapacity: {}, location: {}, building: {}",
                type, minCapacity, location, building);

        List<FacilityResponseDTO> facilities = facilityService.searchFacilities(type, minCapacity, location, building);

        log.debug("Search returned {} facilities", facilities.size());
        return new ResponseEntity<>(facilities, HttpStatus.OK);
    }

    /**
     * Get all active facilities without filtering.
     *
     * @return List of all active FacilityResponseDTO
     */
    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FacilityResponseDTO>> getAllFacilities() {
        log.debug("Fetching all active facilities");
        List<FacilityResponseDTO> facilities = facilityService.getAllActiveFacilities();
        return new ResponseEntity<>(facilities, HttpStatus.OK);
    }

    /**
     * Get facilities by type only.
     *
     * @param type Facility type
     * @return List of FacilityResponseDTO of specified type
     */
    @GetMapping("/by-type/{type}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FacilityResponseDTO>> getFacilitiesByType(
            @PathVariable FacilityType type) {

        log.debug("Fetching facilities of type: {}", type);
        List<FacilityResponseDTO> facilities = facilityService.getFacilitiesByType(type);
        return new ResponseEntity<>(facilities, HttpStatus.OK);
    }

    /**
     * Get facilities by minimum capacity.
     *
     * @param minCapacity Minimum required capacity
     * @return List of FacilityResponseDTO meeting capacity requirement
     */
    @GetMapping("/by-capacity/{minCapacity}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FacilityResponseDTO>> getFacilitiesByCapacity(
            @PathVariable Integer minCapacity) {

        log.debug("Fetching facilities with minimum capacity: {}", minCapacity);
        List<FacilityResponseDTO> facilities = facilityService.getFacilitiesByMinCapacity(minCapacity);
        return new ResponseEntity<>(facilities, HttpStatus.OK);
    }

    /**
     * Get facilities by location.
     *
     * @param location Location name
     * @return List of FacilityResponseDTO at location
     */
    @GetMapping("/by-location/{location}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FacilityResponseDTO>> getFacilitiesByLocation(
            @PathVariable String location) {

        log.debug("Fetching facilities at location: {}", location);
        List<FacilityResponseDTO> facilities = facilityService.getFacilitiesByLocation(location);
        return new ResponseEntity<>(facilities, HttpStatus.OK);
    }

    /**
     * Get facilities by building.
     *
     * @param building Building name
     * @return List of FacilityResponseDTO in building
     */
    @GetMapping("/by-building/{building}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FacilityResponseDTO>> getFacilitiesByBuilding(
            @PathVariable String building) {

        log.debug("Fetching facilities in building: {}", building);
        List<FacilityResponseDTO> facilities = facilityService.getFacilitiesByBuilding(building);
        return new ResponseEntity<>(facilities, HttpStatus.OK);
    }

    /**
     * Get facilities by building and floor.
     *
     * @param building Building name
     * @param floor Floor number/letter
     * @return List of FacilityResponseDTO on floor
     */
    @GetMapping("/by-building-floor/{building}/{floor}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FacilityResponseDTO>> getFacilitiesByBuildingAndFloor(
            @PathVariable String building,
            @PathVariable String floor) {

        log.debug("Fetching facilities in building: {}, floor: {}", building, floor);
        List<FacilityResponseDTO> facilities = facilityService.getFacilitiesByBuildingAndFloor(building, floor);
        return new ResponseEntity<>(facilities, HttpStatus.OK);
    }

    /**
     * Search facilities by name pattern.
     *
     * @param namePattern Search pattern (case-insensitive, partial match)
     * @return List of FacilityResponseDTO matching pattern
     */
    @GetMapping("/search-by-name")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FacilityResponseDTO>> searchFacilitiesByName(
            @RequestParam(name = "query") String namePattern) {

        log.debug("Searching facilities by name pattern: {}", namePattern);
        List<FacilityResponseDTO> facilities = facilityService.searchFacilitiesByName(namePattern);
        return new ResponseEntity<>(facilities, HttpStatus.OK);
    }

    /**
     * Get facility statistics.
     *
     * @return JSON object with total count and counts by type
     */
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getFacilityStatistics() {
        log.debug("Fetching facility statistics");

        long totalCount = facilityService.countActiveFacilities();
        long lectureHallCount = facilityService.countFacilitiesByType(FacilityType.LECTURE_HALL);
        long labCount = facilityService.countFacilitiesByType(FacilityType.LAB);
        long meetingRoomCount = facilityService.countFacilitiesByType(FacilityType.MEETING_ROOM);
        long auditoriumCount = facilityService.countFacilitiesByType(FacilityType.AUDITORIUM);
        long equipmentCount = facilityService.countFacilitiesByType(FacilityType.EQUIPMENT);
        long sportsFacilityCount = facilityService.countFacilitiesByType(FacilityType.SPORTS_FACILITY);

        return new ResponseEntity<>(
                Map.of(
                        "total", totalCount,
                        "lectureHalls", lectureHallCount,
                        "labs", labCount,
                        "meetingRooms", meetingRoomCount,
                        "auditoriums", auditoriumCount,
                        "equipment", equipmentCount,
                        "sportsFacilities", sportsFacilityCount
                ),
                HttpStatus.OK
        );
    }
}
