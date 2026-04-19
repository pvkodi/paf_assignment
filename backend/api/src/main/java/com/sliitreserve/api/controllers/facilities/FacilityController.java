package com.sliitreserve.api.controllers.facilities;

import com.sliitreserve.api.dto.facility.FacilityRequestDTO;
import com.sliitreserve.api.dto.facility.FacilityResponseDTO;
import com.sliitreserve.api.dto.facility.FacilitySuggestionDTO;
import com.sliitreserve.api.dto.facility.FacilitySuggestionRequestDTO;
import com.sliitreserve.api.dto.facility.FacilityUtilizationDTO;
import com.sliitreserve.api.dto.facility.UnderutilizedFacilityDTO;
import com.sliitreserve.api.dto.facility.TimetableAvailabilityDTO;
import com.sliitreserve.api.dto.facility.TimetableUploadResultDTO;
import com.sliitreserve.api.dto.auth.UserProfileResponse;
import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.repositories.auth.UserRepository;
import com.sliitreserve.api.services.facility.FacilityOptimizationService;
import com.sliitreserve.api.services.facility.FacilityService;
import com.sliitreserve.api.services.facility.FacilityTimetableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.sliitreserve.api.services.facility.TimetableParserService;
import com.sliitreserve.api.services.facility.FacilityRuleEngine;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
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
    private final TimetableParserService timetableParserService;
    private final FacilityRuleEngine facilityRuleEngine;
    private final com.sliitreserve.api.repositories.facility.FacilityRepository facilityRepository;
    private final UserRepository userRepository;

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
            @RequestParam(required = false) Facility.FacilityType type,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Facility.FacilityStatus status,
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

    @GetMapping("/{id}/technicians")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserProfileResponse>> getTechniciansByFacility(@PathVariable UUID id) {
        // Verify facility exists
        facilityService.getFacilityById(id);
        
        // Fetch all technicians and convert to response DTOs
        List<UserProfileResponse> technicians = userRepository
                .findByRoleAndActiveTrue(Role.TECHNICIAN)
                .stream()
                .map(user -> new UserProfileResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getDisplayName(),
                        user.getRoles(),
                        user.isSuspended()
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(technicians);
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

    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> batchDeleteFacilities(
            @RequestBody List<UUID> ids,
            @RequestParam(defaultValue = "false") boolean force) {
        facilityService.bulkHardDelete(ids, force);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bulk-action")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> bulkAction(@RequestBody Map<String, Object> request) {
        String action = (String) request.get("action");
        List<String> idStrings = (List<String>) request.get("ids");
        boolean force = Boolean.TRUE.equals(request.get("force"));
        List<UUID> ids = idStrings.stream().map(UUID::fromString).collect(Collectors.toList());

        if ("DEACTIVATE".equalsIgnoreCase(action)) {
            facilityService.bulkDeactivate(ids);
        } else if ("DELETE".equalsIgnoreCase(action)) {
            facilityService.bulkHardDelete(ids, force);
        } else {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> hardDeleteFacility(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean force) {
        facilityService.deleteFacility(id, force);
        return ResponseEntity.noContent().build();
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

    @PostMapping("/timetable/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TimetableUploadResultDTO> uploadTimetable(
            @RequestParam("file") MultipartFile file) {
        try {
            String htmlContent = new String(file.getBytes(), StandardCharsets.UTF_8);

            // 1. Parse to rich slot list first (for the result report)
            List<TimetableParserService.ExtractedSlot> slots =
                    timetableParserService.parseHtmlToSlots(htmlContent);

            // 2. Load into the timetable cache (force reload)
            facilityTimetableService.loadTimetableFromHtml(htmlContent, true);

            // 3. Compute per-room schedules: Map<Room, Map<DayOfWeek, List<String>>>
            Map<String, Map<DayOfWeek, List<String>>> roomSchedules = slots.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            TimetableParserService.ExtractedSlot::getRoomCode,
                            java.util.stream.Collectors.groupingBy(
                                    TimetableParserService.ExtractedSlot::getDay,
                                    java.util.stream.Collectors.collectingAndThen(
                                            java.util.stream.Collectors.mapping(
                                                    TimetableParserService.ExtractedSlot::getTime,
                                                    java.util.stream.Collectors.toSet()
                                            ),
                                            this::calculateAvailableBlocks
                                    )
                            )
                    ));

            // Note: sorting is handled natively inside calculateAvailableBlocks

            // 4. Determine matched vs unmatched rooms against Facility DB
            Set<String> allRooms = roomSchedules.keySet();
            Set<String> knownFacilityCodes = facilityRepository
                    .findAll()
                    .stream()
                    .map(f -> f.getFacilityCode() == null ? "" : f.getFacilityCode().trim().toUpperCase())
                    .collect(java.util.stream.Collectors.toSet());

            List<String> matched   = allRooms.stream()
                    .filter(knownFacilityCodes::contains)
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());

            List<String> unmatchedStrings = allRooms.stream()
                    .filter(r -> !knownFacilityCodes.contains(r))
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());
                    
            java.util.Map<String, List<TimetableParserService.ExtractedSlot>> unmatchedContext = slots.stream()
                    .filter(s -> !knownFacilityCodes.contains(s.getRoomCode()))
                    .collect(java.util.stream.Collectors.groupingBy(TimetableParserService.ExtractedSlot::getRoomCode));

            List<com.sliitreserve.api.dto.facility.ParsedFacilitySuggestionDTO> unmatched = 
                    facilityRuleEngine.generateSuggestions(unmatchedContext);

            TimetableUploadResultDTO result = TimetableUploadResultDTO.builder()
                    .roomsFound(allRooms.size())
                    .totalSlotsExtracted(slots.size())
                    .cacheLoaded(facilityTimetableService.isTimetableLoaded())
                    .roomSchedules(roomSchedules)
                    .matchedRooms(matched)
                    .unmatchedRooms(unmatched)
                    .build();

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FacilityResponseDTO>> batchCreateFacilities(
            @Valid @RequestBody List<FacilityRequestDTO> requests) {
        
        List<FacilityResponseDTO> createdFacilities = requests.stream()
                .map(facilityService::createFacility)
                .collect(java.util.stream.Collectors.toList());
                
        return ResponseEntity.status(HttpStatus.CREATED).body(createdFacilities);
    }

    private List<String> calculateAvailableBlocks(java.util.Set<java.time.LocalTime> occupiedTimes) {
        List<String> available = new java.util.ArrayList<>();
        int currentStart = 8;
        while (currentStart < 18) {
            if (!occupiedTimes.contains(java.time.LocalTime.of(currentStart, 0))) {
                int blockStart = currentStart;
                while (currentStart < 18 && !occupiedTimes.contains(java.time.LocalTime.of(currentStart, 0))) {
                    currentStart++;
                }
                java.time.LocalTime startTime = java.time.LocalTime.of(blockStart, 0);
                java.time.LocalTime endTime = java.time.LocalTime.of(currentStart, 0);
                available.add(startTime.toString() + " - " + endTime.toString());
            } else {
                currentStart++;
            }
        }
        return available;
    }
}
