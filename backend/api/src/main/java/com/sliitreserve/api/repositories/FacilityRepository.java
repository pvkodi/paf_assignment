package com.sliitreserve.api.repositories;

import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.facility.Facility.FacilityStatus;
import com.sliitreserve.api.entities.facility.Facility.FacilityType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Facility entities with support for SINGLE_TABLE inheritance queries.
 * Handles queries across all facility subtypes (LectureHall, Lab, MeetingRoom, Auditorium, Equipment, SportsFacility).
 */
@Repository
public interface FacilityRepository extends BaseRepository<Facility, UUID> {

    /**
     * Find facility by unique code.
     *
     * @param facilityCode Facility code
     * @return Facility if found
     */
    Optional<Facility> findByFacilityCode(String facilityCode);

    /**
     * Check if facility code exists.
     *
     * @param facilityCode Facility code
     * @return true if exists
     */
    boolean existsByFacilityCode(String facilityCode);

    /**
     * Find all active facilities.
     *
     * @return List of active facilities
     */
    List<Facility> findByStatus(FacilityStatus status);

    /**
     * Find all facilities of a specific type.
     *
     * @param type Facility type (LECTURE_HALL, LAB, MEETING_ROOM, AUDITORIUM, EQUIPMENT, SPORTS_FACILITY)
     * @return List of facilities of specified type
     */
    List<Facility> findByType(FacilityType type);

    /**
     * Find all active facilities of a specific type.
     *
     * @param type   Facility type
     * @param status Facility status
     * @return List of active facilities of specified type
     */
    List<Facility> findByTypeAndStatus(FacilityType type, FacilityStatus status);

    /**
     * Find facilities by building.
     *
     * @param building Building name
     * @return List of facilities in building
     */
    List<Facility> findByBuilding(String building);

    /**
     * Find facilities by building and floor.
     *
     * @param building Building name
     * @param floor    Floor number/letter
     * @return List of facilities on floor
     */
    List<Facility> findByBuildingAndFloor(String building, String floor);

    /**
     * Find facilities with minimum capacity.
     *
     * @param minCapacity Minimum capacity required
     * @return List of facilities meeting capacity requirement
     */
    List<Facility> findByCapacityGreaterThanEqual(Integer minCapacity);

    /**
     * Find active facilities with minimum capacity.
     *
     * @param minCapacity Minimum capacity required
     * @return List of active facilities meeting capacity requirement
     */
    @Query("SELECT f FROM Facility f WHERE f.capacity >= :minCapacity AND f.status = :status")
    List<Facility> findActiveWithMinCapacity(
            @Param("minCapacity") Integer minCapacity,
            @Param("status") FacilityStatus status
    );

    /**
     * Find facilities by name pattern.
     *
     * @param namePattern Name search pattern
     * @return List of facilities matching pattern
     */
    List<Facility> findByNameContainingIgnoreCase(String namePattern);

    /**
     * Find active facilities by name pattern.
     *
     * @param namePattern Name search pattern
     * @param status      Facility status
     * @return List of active facilities matching pattern
     */
    @Query("SELECT f FROM Facility f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :namePattern, '%')) AND f.status = :status")
    List<Facility> findActiveByNamePattern(
            @Param("namePattern") String namePattern,
            @Param("status") FacilityStatus status
    );

    /**
     * Find facilities by location.
     *
     * @param location Location name
     * @return List of facilities at location
     */
    List<Facility> findByLocationContainingIgnoreCase(String location);

    /**
     * Find all active facilities sorted by capacity.
     *
     * @param status Facility status
     * @return List of active facilities sorted by capacity
     */
    @Query("SELECT f FROM Facility f WHERE f.status = :status ORDER BY f.capacity DESC")
    List<Facility> findAllActiveOrderByCapacity(@Param("status") FacilityStatus status);

    /**
     * Count facilities by type.
     *
     * @param type Facility type
     * @return Number of facilities of specified type
     */
    long countByType(FacilityType type);

    /**
     * Count active facilities.
     *
     * @param status Facility status
     * @return Number of active facilities
     */
    long countByStatus(FacilityStatus status);

    /**
     * Count facilities by type and status.
     *
     * @param type   Facility type
     * @param status Facility status
     * @return Number of facilities matching criteria
     */
    long countByTypeAndStatus(FacilityType type, FacilityStatus status);
}
