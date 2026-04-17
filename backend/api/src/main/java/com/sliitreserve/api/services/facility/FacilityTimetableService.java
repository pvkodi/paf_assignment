package com.sliitreserve.api.services.facility;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages facility timetable data and provides availability queries.
 * Caches parsed timetable to avoid repeated parsing.
 * Integrates room codes with facility codes via normalization.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FacilityTimetableService {

    private final FacilityRepository facilityRepository;
    private final TimetableParserService timetableParserService;

    /**
     * In-memory cache: roomCode -> day -> occupiedTimes
     * Keyed by normalized room code for matching with facility codes.
     */
    private final Map<String, Map<DayOfWeek, Set<LocalTime>>> timetableCache = new ConcurrentHashMap<>();

    private volatile boolean isLoaded = false;

    /**
     * Load and parse timetable from file.
     * Subsequent calls use cached data unless reload is forced.
     */
    public synchronized void loadTimetable(File timetableFile, boolean forceReload) throws IOException {
        if (isLoaded && !forceReload) {
            log.debug("Timetable already loaded, skipping (use forceReload=true to reload)");
            return;
        }

        try {
            timetableCache.clear();
            Map<String, Map<DayOfWeek, Set<LocalTime>>> parsed = timetableParserService.parseFile(timetableFile);
            timetableCache.putAll(parsed);
            isLoaded = true;
            log.info("Timetable loaded and cached: {} rooms", timetableCache.size());
        } catch (IOException e) {
            isLoaded = false;
            log.error("Failed to load timetable: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Load timetable from HTML string.
     */
    public synchronized void loadTimetableFromHtml(String htmlContent, boolean forceReload) {
        if (isLoaded && !forceReload) {
            log.debug("Timetable already loaded, skipping");
            return;
        }

        try {
            timetableCache.clear();
            Map<String, Map<DayOfWeek, Set<LocalTime>>> parsed = timetableParserService.parseHtml(htmlContent);
            timetableCache.putAll(parsed);
            isLoaded = true;
            log.info("Timetable loaded from HTML: {} rooms", timetableCache.size());
        } catch (Exception e) {
            isLoaded = false;
            log.error("Failed to load timetable from HTML: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if a facility is occupied at a specific day and time.
     * Resolves facility to its room code first.
     */
    public boolean isOccupied(String facilityCode, DayOfWeek day, LocalTime time) {
        if (!isLoaded) {
            return false; // Timetable not loaded, assume not occupied
        }

        String normalizedCode = normalizeFacilityCode(facilityCode);
        Map<DayOfWeek, Set<LocalTime>> dayMap = timetableCache.get(normalizedCode);

        if (dayMap == null) {
            return false;
        }

        Set<LocalTime> occupiedTimes = dayMap.get(day);
        if (occupiedTimes == null) {
            return false;
        }

        return occupiedTimes.contains(time);
    }

    /**
     * Get all occupied time slots for a facility on a specific day.
     */
    public Set<LocalTime> getOccupiedSlots(String facilityCode, DayOfWeek day) {
        if (!isLoaded) {
            return Collections.emptySet();
        }

        String normalizedCode = normalizeFacilityCode(facilityCode);
        Map<DayOfWeek, Set<LocalTime>> dayMap = timetableCache.get(normalizedCode);

        if (dayMap == null || dayMap.get(day) == null) {
            return Collections.emptySet();
        }

        return new HashSet<>(dayMap.get(day));
    }

    /**
     * Get all available time slots for a facility on a specific day.
     * Uses facility's availability window (e.g., 08:00 to 17:00).
     */
    public Set<LocalTime> getAvailableSlots(String facilityCode, DayOfWeek day) {
        if (!isLoaded) {
            return Collections.emptySet();
        }

        // Resolve facility to get its availability window
        var facility = facilityRepository.findByFacilityCode(facilityCode);
        if (facility.isEmpty()) {
            log.warn("Facility not found: {}", facilityCode);
            return Collections.emptySet();
        }

        LocalTime startTime = facility.get().getAvailabilityStartTime();
        LocalTime endTime = facility.get().getAvailabilityEndTime();

        Set<LocalTime> available = new HashSet<>();
        Set<LocalTime> occupied = getOccupiedSlots(facilityCode, day);

        // Generate all hourly slots within facility's availability window
        LocalTime current = startTime;
        while (current.isBefore(endTime)) {
            if (!occupied.contains(current)) {
                available.add(current);
            }
            current = current.plusHours(1);
        }

        return available;
    }

    /**
     * Get occupancy summary for a facility across all days.
     */
    public Map<DayOfWeek, Set<LocalTime>> getFacilityOccupancy(String facilityCode) {
        if (!isLoaded) {
            return Collections.emptyMap();
        }

        String normalizedCode = normalizeFacilityCode(facilityCode);
        Map<DayOfWeek, Set<LocalTime>> occupancy = timetableCache.get(normalizedCode);

        if (occupancy == null) {
            return Collections.emptyMap();
        }

        // Return defensive copy
        Map<DayOfWeek, Set<LocalTime>> result = new HashMap<>();
        occupancy.forEach((day, times) -> result.put(day, new HashSet<>(times)));
        return result;
    }

    /**
     * Clear timetable cache.
     */
    public synchronized void clearCache() {
        timetableCache.clear();
        isLoaded = false;
        log.info("Timetable cache cleared");
    }

    /**
     * Check if timetable is currently loaded.
     */
    public boolean isTimetableLoaded() {
        return isLoaded;
    }

    /**
     * Get timetable statistics.
     */
    public Map<String, Object> getStatistics() {
        return Map.of(
                "isLoaded", isLoaded,
                "roomsCount", timetableCache.size(),
                "totalOccupiedSlots", calculateTotalOccupiedSlots(),
                "averageSlotsPerRoom", calculateAverageSlotsPerRoom()
        );
    }

    //==== Internal helpers ====

    /**
     * Normalize facility code for matching against timetable room codes.
     * Uppercase, trim, and extract room identifier.
     */
    private String normalizeFacilityCode(String facilityCode) {
        if (facilityCode == null) {
            return "";
        }

        return facilityCode.trim().toUpperCase();
    }

    private long calculateTotalOccupiedSlots() {
        return timetableCache.values().stream()
                .flatMap(dayMap -> dayMap.values().stream())
                .mapToLong(Set::size)
                .sum();
    }

    private double calculateAverageSlotsPerRoom() {
        if (timetableCache.isEmpty()) {
            return 0.0;
        }

        long totalSlots = calculateTotalOccupiedSlots();
        return (double) totalSlots / timetableCache.size();
    }
}
