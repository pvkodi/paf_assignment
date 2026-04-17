package com.sliitreserve.api.services.facility;

import com.sliitreserve.api.dto.facility.AvailabilityStatusDTO;
import com.sliitreserve.api.dto.facility.BookingSlotDTO;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.repositories.bookings.BookingRepository;
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import com.sliitreserve.api.repositories.facility.UtilizationSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating facility insights and availability information
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FacilityInsightsService {
    
    private final FacilityRepository facilityRepository;
    private final BookingRepository bookingRepository;
    private final UtilizationSnapshotRepository utilizationSnapshotRepository;
    
    /**
     * Get comprehensive availability insights for a facility
     */
    public AvailabilityStatusDTO getFacilityInsights(String facilityIdStr) {
        log.debug("Getting insights for facility: {}", facilityIdStr);
        
        UUID facilityId = UUID.fromString(facilityIdStr);
        Facility facility = facilityRepository.findById(facilityId)
            .orElseThrow(() -> new IllegalArgumentException("Facility not found: " + facilityIdStr));
        
        LocalDateTime now = LocalDateTime.now();
        
        // Calculate current availability
        long currentBookings = bookingRepository.countActiveBookings(facilityIdStr, now);
        String currentStatus = calculateStatus(currentBookings, facility.getCapacity());
        
        // Calculate minutes until free
        long minutesUntilFree = calculateMinutesUntilFree(facilityIdStr, now);
        
        // Get utilization averages
        Integer avg30day = utilizationSnapshotRepository.getAverageUtilization(facilityIdStr, 30);
        Integer avg7day = utilizationSnapshotRepository.getAverageUtilization(facilityIdStr, 7);
        
        // Calculate trend
        String trendDirection = calculateTrendDirection(avg7day, avg30day);
        Double trendPercentChange = calculatePercentChange(avg30day, avg7day);
        
        // Get best booking slots
        List<BookingSlotDTO> bestSlots = getBestBookingSlots(facilityIdStr, 3);
        
        String nextBookingTime = getNextBookingTime(facilityIdStr, now);
        
        return AvailabilityStatusDTO.builder()
            .facilityId(facilityIdStr)
            .facilityName(facility.getName())
            .facilityType(facility.getType() != null ? facility.getType().name() : "UNKNOWN")
            .building(facility.getBuilding())
            .capacity(facility.getCapacity())
            .currentStatus(currentStatus)
            .spotsAvailable((int) Math.max(0, facility.getCapacity() - currentBookings))
            .minutesUntilFree(minutesUntilFree)
            .nextBookingTime(nextBookingTime)
            .avgUtilization30day(avg30day != null ? avg30day : 0)
            .avgUtilization7day(avg7day != null ? avg7day : 0)
            .trendDirection(trendDirection)
            .trendPercentChange(trendPercentChange)
            .bestBookingSlots(bestSlots)
            .build();
    }
    
    /**
     * Determine current status based on bookings and capacity
     */
    private String calculateStatus(long currentBookings, int capacity) {
        if (currentBookings >= capacity) {
            return "FULL";
        } else if (currentBookings > 0) {
            return "BUSY";
        } else {
            return "FREE";
        }
    }
    
    /**
     * Calculate minutes until facility becomes free or changes status
     */
    private long calculateMinutesUntilFree(String facilityId, LocalDateTime now) {
        // Get earliest end time of current/upcoming bookings
        var nextEndTime = bookingRepository.findNextAvailabilityTime(facilityId, now);
        
        if (nextEndTime.isEmpty()) {
            return Long.MAX_VALUE; // Facility won't be booked soon
        }
        
        // Convert LocalTime to LocalDateTime for comparison
        LocalDateTime nextEndDateTime = now.toLocalDate().atTime(nextEndTime.get());
        if (nextEndDateTime.isBefore(now)) {
            nextEndDateTime = nextEndDateTime.plusDays(1);
        }
        
        return ChronoUnit.MINUTES.between(now, nextEndDateTime);
    }
    
    /**
     * Get next booking time after current time
     */
    private String getNextBookingTime(String facilityId, LocalDateTime now) {
        var nextBooking = bookingRepository.findNextBooking(facilityId, now);
        return nextBooking.isPresent() ? nextBooking.get().getStartTime().toString() : "No bookings";
    }
    
    /**
     * Determine trend direction by comparing 7-day to 30-day average
     */
    private String calculateTrendDirection(Integer avg7day, Integer avg30day) {
        if (avg7day == null || avg30day == null || avg30day == 0) {
            return "STABLE";
        }
        
        if (avg7day > avg30day * 1.05) { // 5% increase threshold
            return "UP";
        } else if (avg7day < avg30day * 0.95) { // 5% decrease threshold
            return "DOWN";
        } else {
            return "STABLE";
        }
    }
    
    /**
     * Calculate percentage change from old to new value
     */
    private Double calculatePercentChange(Integer oldValue, Integer newValue) {
        if (oldValue == null || newValue == null || oldValue == 0) {
            return 0.0;
        }
        
        return ((double) (newValue - oldValue) / oldValue) * 100;
    }
    
    /**
     * Get best booking slots (historically least booked times)
     */
    private List<BookingSlotDTO> getBestBookingSlots(String facilityId, int count) {
        // Get hourly utilization data grouped by day of week
        var weeklyHeatmap = utilizationSnapshotRepository.getWeeklyHeatmapData(facilityId);
        
        List<BookingSlotDTO> slots = new ArrayList<>();
        
        // Sort by lowest utilization and take top N
        weeklyHeatmap.entrySet().stream()
            .sorted((a, b) -> a.getValue().compareTo(b.getValue()))
            .limit(count)
            .forEach(entry -> {
                String[] parts = entry.getKey().split("_");
                int dayOfWeek = Integer.parseInt(parts[0]);
                int hour = Integer.parseInt(parts[1]);
                
                slots.add(BookingSlotDTO.builder()
                    .dayOfWeek(getDayName(dayOfWeek))
                    .timeSlot(String.format("%02d:00-%02d:00", hour, Math.min(hour + 1, 23)))
                    .availabilityPercent(100 - entry.getValue())
                    .reason("Typically free at this time")
                    .build());
            });
        
        return slots;
    }
    
    /**
     * Get day name from day of week number
     */
    private String getDayName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 0 -> "Sunday";
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4 -> "Thursday";
            case 5 -> "Friday";
            case 6 -> "Saturday";
            default -> "Unknown";
        };
    }
}
