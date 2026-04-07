package com.sliitreserve.api.repositories.specifications;

import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.facility.Facility.FacilityStatus;
import com.sliitreserve.api.entities.facility.Facility.FacilityType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for complex Facility queries.
 * Provides reusable query building blocks for facility searches.
 */
public class FacilitySpecifications {

    /**
     * Create a specification for active facilities only.
     *
     * @return Specification for active status
     */
    public static Specification<Facility> isActive() {
        return (root, query, cb) -> cb.equal(root.get("status"), FacilityStatus.ACTIVE);
    }

    /**
     * Create a specification for facilities of specific type.
     *
     * @param type Facility type
     * @return Specification for facility type
     */
    public static Specification<Facility> ofType(FacilityType type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    /**
     * Create a specification for facilities with minimum capacity.
     *
     * @param capacity Minimum capacity
     * @return Specification for capacity
     */
    public static Specification<Facility> hasMinCapacity(Integer capacity) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("capacity"), capacity);
    }

    /**
     * Create a specification for facilities in a building.
     *
     * @param building Building name
     * @return Specification for building
     */
    public static Specification<Facility> inBuilding(String building) {
        return (root, query, cb) -> cb.equal(root.get("building"), building);
    }

    /**
     * Create a specification for facilities on a floor.
     *
     * @param floor Floor number/letter
     * @return Specification for floor
     */
    public static Specification<Facility> onFloor(String floor) {
        return (root, query, cb) -> cb.equal(root.get("floor"), floor);
    }

    /**
     * Create a specification for facilities matching name pattern.
     *
     * @param namePattern Name search pattern
     * @return Specification for name pattern
     */
    public static Specification<Facility> nameContains(String namePattern) {
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("name")),
                "%" + namePattern.toLowerCase() + "%"
        );
    }

    /**
     * Create a specification for facilities at location.
     *
     * @param location Location search pattern
     * @return Specification for location
     */
    public static Specification<Facility> locationContains(String location) {
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("location")),
                "%" + location.toLowerCase() + "%"
        );
    }

    /**
     * Create a specification with multiple criteria combined.
     *
     * @param active   Include only active facilities
     * @param type     Filter by type (null to skip)
     * @param minCapacity Minimum capacity (null to skip)
     * @param building Filter by building (null to skip)
     * @param namePattern Filter by name pattern (null to skip)
     * @return Combined specification
     */
    public static Specification<Facility> advancedSearch(
            boolean active,
            FacilityType type,
            Integer minCapacity,
            String building,
            String namePattern) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (active) {
                predicates.add(cb.equal(root.get("status"), FacilityStatus.ACTIVE));
            }

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            if (minCapacity != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("capacity"), minCapacity));
            }

            if (building != null && !building.isEmpty()) {
                predicates.add(cb.equal(root.get("building"), building));
            }

            if (namePattern != null && !namePattern.isEmpty()) {
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" + namePattern.toLowerCase() + "%"
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Create a specification for facilities within capacity range.
     *
     * @param minCapacity Minimum capacity
     * @param maxCapacity Maximum capacity
     * @return Specification for capacity range
     */
    public static Specification<Facility> capacityBetween(Integer minCapacity, Integer maxCapacity) {
        return (root, query, cb) -> cb.between(root.get("capacity"), minCapacity, maxCapacity);
    }
}
