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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

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
    
    public static final String ALL_CAMPUS_ID_STR = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2";

    /**
     * Get comprehensive availability insights for a facility
     */
    public AvailabilityStatusDTO getFacilityInsights(String facilityIdStr) {
        log.debug("Getting insights for facility: {}", facilityIdStr);
        
        if (ALL_CAMPUS_ID_STR.equals(facilityIdStr)) {
            return getCampusWideInsights();
        }

        UUID facilityId = UUID.fromString(facilityIdStr);
        Facility facility = facilityRepository.findById(facilityId)
            .orElseThrow(() -> new IllegalArgumentException("Facility not found: " + facilityIdStr));
        
        LocalDateTime now = LocalDateTime.now();
        
        // Calculate current availability
        long currentBookings = bookingRepository.countActiveBookings(facilityIdStr, now);
        String currentStatus = calculateStatus(currentBookings, facility.getCapacity());
        
        // Calculate minutes until free
        long minutesUntilFree = calculateMinutesUntilFree(facilityIdStr, now);

        LocalDate today = now.toLocalDate();
        
        // Get utilization averages
        Integer avg30day = calculateAverageUtilization(facilityId, 30, today);
        Integer avg7day = calculateAverageUtilization(facilityId, 7, today);
        
        // Calculate trend
        String trendDirection = calculateTrendDirection(avg7day, avg30day);
        Double trendPercentChange = calculatePercentChange(avg30day, avg7day);
        
        // Get best booking slots
        List<BookingSlotDTO> bestSlots = getBestBookingSlots(facility, 3, today);
        
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
     * Calculate average utilization from snapshot records for the trailing period.
     */
    private Integer calculateAverageUtilization(UUID facilityId, int days, LocalDate endDate) {
        LocalDate startDate = endDate.minusDays(Math.max(days - 1L, 0L));
        var snapshots = utilizationSnapshotRepository
            .findByFacility_IdAndSnapshotDateBetweenOrderBySnapshotDateAsc(facilityId, startDate, endDate);

        if (snapshots.isEmpty()) {
            return 0;
        }

        double avg = snapshots.stream()
            .mapToDouble(snapshot -> snapshot.getUtilizationPercent() != null
                ? snapshot.getUtilizationPercent().doubleValue()
                : 0.0)
            .average()
            .orElse(0.0);

        return (int) Math.round(avg);
    }
    
    /**
     * Get best booking slots (historically least booked times)
     */
    private List<BookingSlotDTO> getBestBookingSlots(Facility facility, int count, LocalDate endDate) {
        LocalDate startDate = endDate.minusDays(29);
        var snapshots = utilizationSnapshotRepository
            .findByFacility_IdAndSnapshotDateBetweenOrderBySnapshotDateAsc(facility.getId(), startDate, endDate);

        if (snapshots.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, SlotAggregate> slotAggregates = new HashMap<>();

        for (var snapshot : snapshots) {
            if (snapshot.getSnapshotDate() == null) {
                continue;
            }

            int dayOfWeek = snapshot.getSnapshotDate().getDayOfWeek().getValue() - 1;
            int utilization = snapshot.getUtilizationPercent() != null
                ? (int) Math.round(snapshot.getUtilizationPercent().doubleValue())
                : 0;
            int addedHours = 0;

            for (int hour = 0; hour < 24; hour++) {
                LocalTime slot = LocalTime.of(hour, 0);
                if (!facility.isAvailableAt(snapshot.getSnapshotDate().getDayOfWeek(), slot)) {
                    continue;
                }

                String key = dayOfWeek + "_" + hour;
                slotAggregates.computeIfAbsent(key, k -> new SlotAggregate()).add(utilization);
                addedHours++;
            }

            if (addedHours == 0) {
                int fallbackHour = resolveFallbackHour(facility);
                String key = dayOfWeek + "_" + fallbackHour;
                slotAggregates.computeIfAbsent(key, k -> new SlotAggregate()).add(utilization);
            }
        }

        List<BookingSlotDTO> slots = new ArrayList<>();

        // Sort by lowest utilization and take top N
        slotAggregates.entrySet().stream()
            .sorted(Comparator.comparingInt(entry -> entry.getValue().average()))
            .limit(count)
            .forEach(entry -> {
                String[] parts = entry.getKey().split("_");
                int dayOfWeek = Integer.parseInt(parts[0]);
                int hour = Integer.parseInt(parts[1]);
                int avgUtilization = entry.getValue().average();
                
                slots.add(BookingSlotDTO.builder()
                    .dayOfWeek(getDayName(dayOfWeek))
                    .timeSlot(String.format("%02d:00-%02d:00", hour, Math.min(hour + 1, 23)))
                    .availabilityPercent(Math.max(0, 100 - avgUtilization))
                    .reason("Typically free at this time")
                    .build());
            });

        return slots;
    }

    private int resolveFallbackHour(Facility facility) {
        if (facility.getAvailabilityStart() != null) {
            return facility.getAvailabilityStart().getHour();
        }
        return 12;
    }
    
    /**
     * Get day name from day of week number
     */
    private String getDayName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 0 -> "Monday";
            case 1 -> "Tuesday";
            case 2 -> "Wednesday";
            case 3 -> "Thursday";
            case 4 -> "Friday";
            case 5 -> "Saturday";
            case 6 -> "Sunday";
            default -> "Unknown";
        };
    }

    private static final class SlotAggregate {
        private int total;
        private int count;

        void add(int utilization) {
            total += utilization;
            count++;
        }

        int average() {
            if (count == 0) {
                return 0;
            }
            return Math.round((float) total / count);
        }
    }

    /**
     * Get aggregate insights for the entire campus
     */
    private AvailabilityStatusDTO getCampusWideInsights() {
        List<Facility> activeFacilities = facilityRepository.findByStatus(Facility.FacilityStatus.ACTIVE);
        int totalCapacity = activeFacilities.stream().mapToInt(Facility::getCapacity).sum();
        
        LocalDateTime now = LocalDateTime.now();
        long totalCurrentBookings = 0;
        for (Facility f : activeFacilities) {
            totalCurrentBookings += bookingRepository.countActiveBookings(f.getId().toString(), now);
        }
        
        String currentStatus = calculateStatus(totalCurrentBookings, totalCapacity);
        
        return AvailabilityStatusDTO.builder()
            .facilityId(ALL_CAMPUS_ID_STR)
            .facilityName("All Campus")
            .facilityType("CAMPUS")
            .building("Main Campus")
            .capacity(totalCapacity)
            .currentStatus(currentStatus)
            .spotsAvailable((int) Math.max(0, totalCapacity - totalCurrentBookings))
            .avgUtilization30day(0) // Aggregation would be slow, return 0 for now
            .avgUtilization7day(0)
            .trendDirection("STABLE")
            .trendPercentChange(0.0)
            .bestBookingSlots(new ArrayList<>())
            .build();
    }
}
