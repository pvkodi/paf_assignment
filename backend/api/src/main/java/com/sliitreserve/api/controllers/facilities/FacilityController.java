package com.sliitreserve.api.controllers.facilities;

import com.sliitreserve.api.dto.facility.FacilityRequestDTO;
import com.sliitreserve.api.dto.facility.FacilityResponseDTO;
import com.sliitreserve.api.dto.facility.FacilitySuggestionDTO;
import com.sliitreserve.api.dto.facility.FacilitySuggestionRequestDTO;
import com.sliitreserve.api.dto.facility.FacilityUtilizationDTO;
import com.sliitreserve.api.dto.facility.UnderutilizedFacilityDTO;
import com.sliitreserve.api.dto.facility.TimetableAvailabilityDTO;
import com.sliitreserve.api.entities.facility.Facility.FacilityType;
import com.sliitreserve.api.entities.facility.Facility.FacilityStatus;
import com.sliitreserve.api.services.facility.FacilityOptimizationService;
import com.sliitreserve.api.services.facility.FacilityService;
import com.sliitreserve.api.services.facility.FacilityTimetableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/v1/facilities", "/api/facilities"})
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityService facilityService;
    private final FacilityOptimizationService facilityOptimizationService;
    private final FacilityTimetableService facilityTimetableService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<FacilityResponseDTO>> listFacilities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(facilityService.listFacilities(pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<FacilityResponseDTO>> searchFacilities(
            @RequestParam(required = false) FacilityType type,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) FacilityStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                facilityService.searchFacilities(type, minCapacity, building, location, status, pageable)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FacilityResponseDTO> getFacilityById(@PathVariable UUID id) {
        return ResponseEntity.ok(facilityService.getFacilityById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FacilityResponseDTO> createFacility(@Valid @RequestBody FacilityRequestDTO request) {
        return ResponseEntity.status(201).body(facilityService.createFacility(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FacilityResponseDTO> updateFacility(
            @PathVariable UUID id,
            @Valid @RequestBody FacilityRequestDTO request
    ) {
        return ResponseEntity.ok(facilityService.updateFacility(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markOutOfService(@PathVariable UUID id) {
        facilityService.markOutOfService(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/utilization")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FacilityUtilizationDTO> getFacilityUtilization(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        LocalDateTime effectiveEnd = end == null ? LocalDateTime.now() : end;
        LocalDateTime effectiveStart = start == null ? effectiveEnd.minusDays(30) : start;

        return ResponseEntity.ok(
                facilityOptimizationService.getFacilityUtilization(id, effectiveStart, effectiveEnd)
        );
    }

    @GetMapping("/underutilized")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UnderutilizedFacilityDTO>> getUnderutilizedFacilities(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return ResponseEntity.ok(facilityOptimizationService.getUnderutilizedFacilities(end));
    }

    @PostMapping("/suggestions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FacilitySuggestionDTO>> suggestAlternativeFacilities(
            @Valid @RequestBody FacilitySuggestionRequestDTO request
    ) {
        return ResponseEntity.ok(facilityOptimizationService.suggestAlternativeFacilities(request));
    }

    @GetMapping("/{id}/timetable-availability")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TimetableAvailabilityDTO> getTimetableAvailability(
            @PathVariable UUID id,
            @RequestParam(required = false) String day
    ) {
        FacilityResponseDTO facility = facilityService.getFacilityById(id);
        
        // Parse day parameter or default to MONDAY
        DayOfWeek dayOfWeek = day != null 
            ? DayOfWeek.valueOf(day.toUpperCase())
            : DayOfWeek.MONDAY;

        // Get occupied slots from timetable
        Set<String> occupiedSlots = facilityTimetableService
                .getOccupiedSlots(facility.getFacilityCode(), dayOfWeek)
                .stream()
                .map(Object::toString)
                .collect(Collectors.toSet());

        // Get available slots
        Set<String> availableSlots = facilityTimetableService
                .getAvailableSlots(facility.getFacilityCode(), dayOfWeek)
                .stream()
                .map(Object::toString)
                .collect(Collectors.toSet());

        TimetableAvailabilityDTO response = TimetableAvailabilityDTO.builder()
                .facilityCode(facility.getFacilityCode())
                .facilityName(facility.getName())
                .day(dayOfWeek)
                .occupiedSlots(occupiedSlots)
                .availableSlots(availableSlots)
                .totalOccupiedCount(occupiedSlots.size())
                .totalAvailableCount(availableSlots.size())
                .timetableLoaded(facilityTimetableService.isTimetableLoaded())
                .build();

        return ResponseEntity.ok(response);
    }
}
