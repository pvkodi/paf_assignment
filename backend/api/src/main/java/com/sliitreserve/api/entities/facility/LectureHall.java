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
 * LectureHall subtype of Facility.
 * Inherits all base Facility properties and adds specific lecture hall metadata.
 */
@Entity
@DiscriminatorValue("LECTURE_HALL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LectureHall extends Facility {

    @ElementCollection
    @Column(name = "av_equipment")
    private Set<String> avEquipment;

    @Column(name = "wheelchair_accessible")
    private Boolean wheelchairAccessible = false;

    public LectureHall(String facilityCode, String name, Integer capacity, String location) {
        super();
        this.setFacilityCode(facilityCode);
        this.setName(name);
        this.setType(FacilityType.LECTURE_HALL);
        this.setCapacity(capacity);
        this.setLocation(location);
        this.wheelchairAccessible = false;
    }
}
