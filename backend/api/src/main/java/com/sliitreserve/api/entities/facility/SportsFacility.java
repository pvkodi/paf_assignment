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
 * SportsFacility subtype of Facility.
 * Inherits all base Facility properties and adds sports facility-specific metadata.
 */
@Entity
@DiscriminatorValue("SPORTS_FACILITY")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SportsFacility extends Facility {

    @Column(name = "sports_type", length = 100)
    private String sportsType;

    @ElementCollection
    @Column(name = "equipment_item")
    private Set<String> equipmentAvailable;

    public SportsFacility(String facilityCode, String name, Integer capacity, String location, String sportsType) {
        super();
        this.setFacilityCode(facilityCode);
        this.setName(name);
        this.setType(FacilityType.SPORTS);
        this.setCapacity(capacity);
        this.setLocation(location);
        this.sportsType = sportsType;
    }
}
