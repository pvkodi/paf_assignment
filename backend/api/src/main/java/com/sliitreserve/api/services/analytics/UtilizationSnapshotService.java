package com.sliitreserve.api.services.analytics;

import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.facility.Facility.FacilityStatus;
import com.sliitreserve.api.entities.analytics.UtilizationSnapshot;
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import com.sliitreserve.api.repositories.facility.UtilizationSnapshotRepository;
import com.sliitreserve.api.services.integration.BookingIntegrationService;
import com.sliitreserve.api.services.integration.MaintenanceIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Daily utilization snapshot generation service.
 *
 * Responsibilities:
 * - Compute booked-hours to available-hours utilization for active facilities
 * - Persist one immutable snapshot per facility per day (upsert-safe)
 * - Mark underutilization using a 30-day rolling window average
 * - Track consecutive underutilized day streaks
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UtilizationSnapshotService {

    private static final BigDecimal UNDERUTILIZED_THRESHOLD_PERCENT = new BigDecimal("30.00");
    private static final BigDecimal MIN_ANALYSIS_AVAILABLE_HOURS = new BigDecimal("50.00");
    private static final int ROLLING_WINDOW_DAYS = 30;

    private final FacilityRepository facilityRepository;
    private final UtilizationSnapshotRepository snapshotRepository;
    private final BookingIntegrationService bookingIntegrationService;
    private final MaintenanceIntegrationService maintenanceIntegrationService;
    private final ZoneId campusZoneId;

    /**
     * Daily scheduled snapshot job. Runs shortly after midnight campus time,
     * and captures metrics for the previous calendar day.
     */
    @Scheduled(
        cron = "${app.analytics.utilization.snapshot-cron:0 5 0 * * *}",
        zone = "${app.time-policy.campus-timezone:Asia/Colombo}"
    )
    @Transactional
    public void generateDailySnapshotsJob() {
        LocalDate snapshotDate = LocalDate.now(campusZoneId).minusDays(1);
        int processed = generateDailySnapshots(snapshotDate);
        log.info("Completed utilization snapshot job for {}. Processed {} facilities", snapshotDate, processed);
    }

    /**
     * Generate snapshots for all active facilities for a target date.
     *
     * @param snapshotDate target date to compute
     * @return number of facilities processed
     */
    @Transactional
    public int generateDailySnapshots(LocalDate snapshotDate) {
        List<Facility> activeFacilities = facilityRepository.findByStatus(FacilityStatus.ACTIVE);
        int processedCount = 0;

        for (Facility facility : activeFacilities) {
            LocalDateTime rangeStart = snapshotDate.atStartOfDay();
            LocalDateTime rangeEnd = snapshotDate.plusDays(1L).atStartOfDay();

            if (maintenanceIntegrationService.isFacilityUnderMaintenance(facility.getId(), rangeStart, rangeEnd)) {
                log.debug("Skipping facility {} due to maintenance window", facility.getId());
                continue;
            }

            BigDecimal availableHours = calculateDailyAvailableHours(facility);
            if (availableHours.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("Skipping facility {} due to zero available hours", facility.getId());
                continue;
            }

            BigDecimal bookedHours = calculateDailyBookedHours(facility.getId(), rangeStart, rangeEnd);
            BigDecimal utilizationPercent = toScaledBigDecimal(
                UtilizationCalculator.calculateUtilizationPercent(
                    availableHours.doubleValue(),
                    bookedHours.doubleValue()
                )
            );

            UnderutilizationResult underutilizationResult = calculateUnderutilization(
                facility.getId(),
                snapshotDate,
                availableHours,
                bookedHours
            );

            UtilizationSnapshot snapshot = snapshotRepository
                .findByFacility_IdAndSnapshotDate(facility.getId(), snapshotDate)
                .orElseGet(() -> UtilizationSnapshot.builder()
                    .facility(facility)
                    .snapshotDate(snapshotDate)
                    .build());

            snapshot.setAvailableHours(availableHours);
            snapshot.setBookedHours(bookedHours);
            snapshot.setUtilizationPercent(utilizationPercent);
            snapshot.setUnderutilized(underutilizationResult.underutilized());
            snapshot.setConsecutiveUnderutilizedDays(underutilizationResult.consecutiveDays());

            snapshotRepository.save(snapshot);
            processedCount++;
        }

        return processedCount;
    }

    private BigDecimal calculateDailyAvailableHours(Facility facility) {
        LocalTime start = facility.getAvailabilityStart();
        LocalTime end = facility.getAvailabilityEnd();

        if (start == null || end == null || !end.isAfter(start)) {
            return BigDecimal.ZERO;
        }

        long minutes = Duration.between(start, end).toMinutes();
        return minutesToHours(minutes);
    }

    private BigDecimal calculateDailyBookedHours(UUID facilityId, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        double bookedHours = bookingIntegrationService.getBookedHours(facilityId, rangeStart, rangeEnd);
        if (bookedHours <= 0.0) {
            return BigDecimal.ZERO;
        }
        return toScaledBigDecimal(bookedHours);
    }

    private UnderutilizationResult calculateUnderutilization(
        UUID facilityId,
        LocalDate snapshotDate,
        BigDecimal currentAvailableHours,
        BigDecimal currentBookedHours
    ) {
        LocalDate windowStart = snapshotDate.minusDays(ROLLING_WINDOW_DAYS - 1L);
        LocalDate windowEnd = snapshotDate.minusDays(1L);

        List<UtilizationSnapshot> historicalSnapshots = snapshotRepository
            .findByFacility_IdAndSnapshotDateBetweenOrderBySnapshotDateAsc(facilityId, windowStart, windowEnd);

        BigDecimal totalAvailableHours = currentAvailableHours;
        BigDecimal totalBookedHours = currentBookedHours;

        for (UtilizationSnapshot historicalSnapshot : historicalSnapshots) {
            totalAvailableHours = totalAvailableHours.add(zeroIfNull(historicalSnapshot.getAvailableHours()));
            totalBookedHours = totalBookedHours.add(zeroIfNull(historicalSnapshot.getBookedHours()));
        }

        if (totalAvailableHours.compareTo(MIN_ANALYSIS_AVAILABLE_HOURS) < 0) {
            return new UnderutilizationResult(false, 0);
        }

        BigDecimal rollingUtilizationPercent = toScaledBigDecimal(
            UtilizationCalculator.calculateUtilizationPercent(
                totalAvailableHours.doubleValue(),
                totalBookedHours.doubleValue()
            )
        );

        boolean underutilized = UtilizationCalculator.isUnderutilized(
            rollingUtilizationPercent.doubleValue(),
            UNDERUTILIZED_THRESHOLD_PERCENT.doubleValue()
        );

        int consecutiveDays = underutilized
            ? resolveConsecutiveUnderutilizedDays(facilityId, snapshotDate)
            : 0;

        return new UnderutilizationResult(underutilized, consecutiveDays);
    }

    private int resolveConsecutiveUnderutilizedDays(UUID facilityId, LocalDate snapshotDate) {
        Optional<UtilizationSnapshot> previousDaySnapshot = snapshotRepository
            .findByFacility_IdAndSnapshotDate(facilityId, snapshotDate.minusDays(1L));

        return previousDaySnapshot
            .filter(UtilizationSnapshot::isUnderutilized)
            .map(snapshot -> zeroIfNull(snapshot.getConsecutiveUnderutilizedDays()) + 1)
            .orElse(1);
    }

    private BigDecimal minutesToHours(long minutes) {
        if (minutes <= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(minutes)
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal toScaledBigDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    private static final class UnderutilizationResult {
        private final boolean underutilized;
        private final int consecutiveDays;

        UnderutilizationResult(boolean underutilized, int consecutiveDays) {
            this.underutilized = underutilized;
            this.consecutiveDays = consecutiveDays;
        }

        public boolean underutilized() {
            return underutilized;
        }

        public int consecutiveDays() {
            return consecutiveDays;
        }
    }
}
