package com.sliitreserve.api.entities.facility;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * MeetingRoom subtype of Facility.
 * Inherits all base Facility properties and adds meeting room-specific metadata.
 */
@Entity
@DiscriminatorValue("MEETING_ROOM")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MeetingRoom extends Facility {

    @Column(name = "av_enabled")
    private Boolean avEnabled = false;

    @Column(name = "catering_allowed")
    private Boolean cateringAllowed = false;

    public MeetingRoom(String facilityCode, String name, Integer capacity, String location) {
        super();
        this.setFacilityCode(facilityCode);
        this.setName(name);
        this.setType(FacilityType.MEETING_ROOM);
        this.setCapacity(capacity);
        this.setLocation(location);
        this.avEnabled = false;
        this.cateringAllowed = false;
    }
}
