package com.sliitreserve.api.factories;

import com.sliitreserve.api.entities.facility.*;

/**
 * Simple Factory for creating Facility subtype instances.
 * Used by import routines, tests and seeding data.
 */
public class FacilityFactory {

    public static Facility create(Facility.FacilityType type, String facilityCode, String name, Integer capacity, String location) {
        if (type == null) return null;

        switch (type) {
            case LECTURE_HALL:
                return new LectureHall(facilityCode, name, capacity, location);
            case LAB:
                // default lab type for quick creation
                return new Lab(facilityCode, name, capacity, location, "standard");
            case MEETING_ROOM:
                return new MeetingRoom(facilityCode, name, capacity, location);
            case AUDITORIUM:
                return new Auditorium(facilityCode, name, capacity, location);
            case EQUIPMENT:
                return new Equipment(facilityCode, name, capacity, location, "unknown");
            case SPORTS_FACILITY:
                return new SportsFacility(facilityCode, name, capacity, location, "multi");
            default:
                Facility f = new Facility();
                f.setFacilityCode(facilityCode);
                f.setName(name);
                f.setType(type);
                f.setCapacity(capacity);
                f.setLocation(location);
                return f;
        }
    }
}
