package com.sliitreserve.api.entities.facility;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Auditorium subtype of Facility.
 * Inherits all base Facility properties and adds auditorium-specific metadata.
 */
@Entity
@DiscriminatorValue("AUDITORIUM")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Auditorium extends Facility {

    @Column(name = "stage_type", length = 100)
    private String stageType;

    @Column(name = "sound_system", length = 100)
    private String soundSystem;

    public Auditorium(String facilityCode, String name, Integer capacity, String location) {
        super();
        this.setFacilityCode(facilityCode);
        this.setName(name);
        this.setType(FacilityType.AUDITORIUM);
        this.setCapacity(capacity);
        this.setLocation(location);
    }
}
