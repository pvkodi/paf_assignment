package com.sliitreserve.api.service.facility;

import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.facility.Facility.FacilityType;
import com.sliitreserve.api.repositories.FacilityRepository;
import com.sliitreserve.api.repositories.specifications.FacilitySpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FacilityService {

    @Autowired
    private FacilityRepository facilityRepository;

    public List<Facility> searchFacilities(Boolean active, FacilityType type, Integer minCapacity, String building, String namePattern) {
        Specification<Facility> spec = null;

        if (Boolean.TRUE.equals(active)) {
            spec = (spec == null) ? FacilitySpecifications.isActive() : spec.and(FacilitySpecifications.isActive());
        }

        if (type != null) {
            spec = (spec == null) ? FacilitySpecifications.ofType(type) : spec.and(FacilitySpecifications.ofType(type));
        }

        if (minCapacity != null) {
            spec = (spec == null) ? FacilitySpecifications.hasMinCapacity(minCapacity) : spec.and(FacilitySpecifications.hasMinCapacity(minCapacity));
        }

        if (building != null && !building.isEmpty()) {
            spec = (spec == null) ? FacilitySpecifications.inBuilding(building) : spec.and(FacilitySpecifications.inBuilding(building));
        }

        if (namePattern != null && !namePattern.isEmpty()) {
            spec = (spec == null) ? FacilitySpecifications.nameContains(namePattern) : spec.and(FacilitySpecifications.nameContains(namePattern));
        }

        return facilityRepository.findAll(spec);
    }

    public Facility findByCode(String facilityCode) {
        return facilityRepository.findByFacilityCode(facilityCode).orElse(null);
    }
}
