package com.sliitreserve.api.controllers;

import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.facility.Facility.FacilityType;
import com.sliitreserve.api.services.facility.FacilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/facilities", "/api/facilities"})
public class FacilityController {

    @Autowired
    private FacilityService facilityService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Facility>> search(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) FacilityType type,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) String building,
            @RequestParam(required = false, name = "name") String namePattern
    ) {
        List<Facility> result = facilityService.searchFacilities(active, type, minCapacity, building, namePattern);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{code}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getByCode(@PathVariable("code") String code) {
        Facility f = facilityService.findByCode(code);
        if (f == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(f);
    }
}
