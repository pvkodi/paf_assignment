package com.sliitreserve.api.entities.facility;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Lab subtype of Facility.
 * Inherits all base Facility properties and adds lab-specific metadata.
 */
@Entity
@DiscriminatorValue("LAB")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Lab extends Facility {

    @Column(name = "lab_type", length = 100)
    private String labType;

    @ElementCollection
    @Column(name = "software_item")
    private Set<String> softwareList;

    @ElementCollection
    @Column(name = "safety_equipment")
    private Set<String> safetyEquipment;

    public Lab(String facilityCode, String name, Integer capacity, String location, String labType) {
        super();
        this.setFacilityCode(facilityCode);
        this.setName(name);
        this.setType(FacilityType.LAB);
        this.setCapacity(capacity);
        this.setLocation(location);
        this.labType = labType;
    }
}
