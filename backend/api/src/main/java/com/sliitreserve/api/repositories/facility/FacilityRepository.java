package com.sliitreserve.api.repositories.facility;

import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.facility.Facility.FacilityStatus;
import com.sliitreserve.api.entities.facility.Facility.FacilityType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FacilityRepository extends com.sliitreserve.api.repositories.BaseRepository<Facility, UUID> {

    Optional<Facility> findByFacilityCode(String facilityCode);

    boolean existsByFacilityCode(String facilityCode);

    List<Facility> findByStatus(FacilityStatus status);

    List<Facility> findByType(FacilityType type);

    List<Facility> findByTypeAndStatus(FacilityType type, FacilityStatus status);

    List<Facility> findByBuilding(String building);

    List<Facility> findByBuildingAndFloor(String building, String floor);

    List<Facility> findByCapacityGreaterThanEqual(Integer minCapacity);

    @Query("SELECT f FROM Facility f WHERE f.capacity >= :minCapacity AND f.status = :status")
    List<Facility> findActiveWithMinCapacity(
            @Param("minCapacity") Integer minCapacity,
            @Param("status") FacilityStatus status
    );

    List<Facility> findByNameContainingIgnoreCase(String namePattern);

    @Query("SELECT f FROM Facility f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :namePattern, '%')) AND f.status = :status")
    List<Facility> findActiveByNamePattern(
            @Param("namePattern") String namePattern,
            @Param("status") FacilityStatus status
    );

    List<Facility> findByLocationContainingIgnoreCase(String location);

    @Query("SELECT f FROM Facility f WHERE f.status = :status ORDER BY f.capacity DESC")
    List<Facility> findAllActiveOrderByCapacity(@Param("status") FacilityStatus status);

    long countByType(FacilityType type);

    long countByStatus(FacilityStatus status);

    long countByTypeAndStatus(FacilityType type, FacilityStatus status);
}
