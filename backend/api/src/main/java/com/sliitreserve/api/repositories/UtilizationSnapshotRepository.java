package com.sliitreserve.api.repositories;

import com.sliitreserve.api.entities.analytics.UtilizationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UtilizationSnapshot entity.
 *
 * Queries:
 * - Find snapshots for a facility in date range
 * - Find underutilized facilities in date range
 * - Find all snapshots in date range (for heatmap)
 */
@Repository
public interface UtilizationSnapshotRepository extends JpaRepository<UtilizationSnapshot, UUID> {

    /**
     * Find all snapshots for a facility in date range.
     *
     * @param facilityId The facility ID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of snapshots sorted by date
     */
    List<UtilizationSnapshot> findByFacility_IdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
        UUID facilityId,
        LocalDate startDate,
        LocalDate endDate
    );

    /**
     * Find all underutilized facilities in date range.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of underutilized snapshots
     */
    @Query("SELECT u FROM UtilizationSnapshot u WHERE u.underutilized = true AND u.snapshotDate BETWEEN :startDate AND :endDate ORDER BY u.utilizationPercent ASC")
    List<UtilizationSnapshot> findUnderutilizedInRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find all snapshots in date range (for heatmap generation).
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of all snapshots in range
     */
    List<UtilizationSnapshot> findBySnapshotDateBetweenOrderBySnapshotDateAsc(LocalDate startDate, LocalDate endDate);

    /**
     * Find latest snapshot for a facility.
     *
     * @param facilityId The facility ID
     * @return Optional latest snapshot
     */
    Optional<UtilizationSnapshot> findFirstByFacility_IdOrderBySnapshotDateDesc(UUID facilityId);

    /**
     * Find snapshot for specific facility and date.
     *
     * @param facilityId The facility ID
     * @param snapshotDate The snapshot date
     * @return Optional snapshot if exists
     */
    Optional<UtilizationSnapshot> findByFacility_IdAndSnapshotDate(UUID facilityId, LocalDate snapshotDate);
}
