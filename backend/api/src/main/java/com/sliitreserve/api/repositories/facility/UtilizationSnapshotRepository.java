package com.sliitreserve.api.repositories.facility;

import com.sliitreserve.api.entities.analytics.UtilizationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UtilizationSnapshotRepository extends JpaRepository<UtilizationSnapshot, UUID> {

    List<UtilizationSnapshot> findByFacility_IdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
     UUID facilityId,
     LocalDate startDate,
     LocalDate endDate
    );

    @Query("SELECT u FROM UtilizationSnapshot u WHERE u.underutilized = true AND u.snapshotDate BETWEEN :startDate AND :endDate ORDER BY u.utilizationPercent ASC")
    List<UtilizationSnapshot> findUnderutilizedInRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<UtilizationSnapshot> findBySnapshotDateBetweenOrderBySnapshotDateAsc(LocalDate startDate, LocalDate endDate);

    Optional<UtilizationSnapshot> findFirstByFacility_IdOrderBySnapshotDateDesc(UUID facilityId);

    Optional<UtilizationSnapshot> findByFacility_IdAndSnapshotDate(UUID facilityId, LocalDate snapshotDate);

    @Query(value = "SELECT ROUND(AVG(u.utilization_percent)) FROM utilization_snapshot u " +
        "WHERE u.facility_id = :facilityId " +
        "AND u.snapshot_date >= DATE_SUB(CURDATE(), INTERVAL :days DAY) " +
        "AND u.snapshot_date <= CURDATE()", nativeQuery = true)
    Integer getAverageUtilization(@Param("facilityId") String facilityId, @Param("days") Integer days);

    @Query(value = "SELECT CONCAT(DAYOFWEEK(u.snapshot_date) - 1, '_', HOUR(u.snapshot_date)) as slot, " +
        "ROUND(AVG(u.utilization_percent)) as util " +
        "FROM utilization_snapshot u " +
        "WHERE u.facility_id = :facilityId " +
        "GROUP BY DAYOFWEEK(u.snapshot_date), HOUR(u.snapshot_date)", nativeQuery = true)
    java.util.Map<String, Integer> getWeeklyHeatmapData(@Param("facilityId") String facilityId);

    @Query(value = "SELECT CONCAT(DAYOFWEEK(u.snapshot_date) - 1, '_', HOUR(u.snapshot_date)) as slot, " +
        "ROUND(AVG(u.utilization_percent)) as util " +
        "FROM utilization_snapshot u " +
        "WHERE u.facility_id = :facilityId " +
        "AND u.snapshot_date BETWEEN :startDate AND :endDate " +
        "GROUP BY DAYOFWEEK(u.snapshot_date), HOUR(u.snapshot_date)", nativeQuery = true)
    java.util.Map<String, Integer> getWeeklyHeatmapData(@Param("facilityId") String facilityId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT CAST(HOUR(u.snapshot_date) AS CHAR) as hour, " +
        "ROUND(AVG(u.utilization_percent)) as util " +
        "FROM utilization_snapshot u " +
        "WHERE u.facility_id = :facilityId " +
        "AND DATE(u.snapshot_date) = :date " +
        "GROUP BY HOUR(u.snapshot_date)", nativeQuery = true)
    java.util.Map<String, Integer> getDailyHeatmapData(@Param("facilityId") String facilityId,
                                @Param("date") LocalDate date);

    @Query(value = "SELECT u.utilization_percent FROM utilization_snapshot u " +
        "WHERE u.facility_id = :facilityId " +
        "AND DATE(u.snapshot_date) = DATE(:dateTime) " +
        "AND HOUR(u.snapshot_date) = HOUR(:dateTime)", nativeQuery = true)
    Optional<Integer> getUtilizationAtTime(@Param("facilityId") String facilityId,
                         @Param("dateTime") java.time.LocalDateTime dateTime);

    @Query(value = "SELECT COUNT(DISTINCT u.snapshot_date) FROM utilization_snapshot u " +
        "WHERE u.facility_id = :facilityId " +
        "AND u.utilization_percent < :threshold " +
        "AND u.snapshot_date >= DATE_SUB(CURDATE(), INTERVAL :days DAY)", nativeQuery = true)
    Integer countUnderutilizedDays(@Param("facilityId") String facilityId,
                    @Param("threshold") Integer threshold,
                    @Param("days") Integer days);

    @Query("SELECT 1 FROM UtilizationSnapshot u " +
        "WHERE u.facility.id = :facilityId " +
        "AND u.utilizationPercent < :threshold " +
        "ORDER BY u.snapshotDate DESC " +
        "LIMIT 30")
    Integer findConsecutiveUnderutilizedDays(@Param("facilityId") String facilityId,
                           @Param("threshold") Integer threshold);

    @Query(value = "SELECT CONCAT(DAYOFWEEK(u.snapshot_date) - 1, '_', HOUR(u.snapshot_date)) as slot, " +
        "ROUND(AVG(u.utilization_percent)) as util " +
        "FROM utilization_snapshot u " +
        "WHERE u.facility_id = :facilityId " +
        "AND u.utilization_percent > :threshold " +
        "GROUP BY DAYOFWEEK(u.snapshot_date), HOUR(u.snapshot_date)", nativeQuery = true)
    java.util.Map<String, Integer> findPeakUtilizationTimeslots(@Param("facilityId") String facilityId,
                                      @Param("threshold") Integer threshold);
}
