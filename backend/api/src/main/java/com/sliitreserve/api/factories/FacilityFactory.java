package com.sliitreserve.api.factories;

import com.sliitreserve.api.entities.facility.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Map;

/**
 * Factory for creating Facility instances of specific subtypes.
 * Uses the Factory pattern to encapsulate facility creation logic.
 * Supports creation of all facility subtypes: LectureHall, Lab, MeetingRoom,
 * Auditorium, Equipment, and SportsFacility.
 */
@Component
@Slf4j
public class FacilityFactory {

    /**
     * Creates a facility instance of the specified type.
     *
     * @param facilityCode   Unique facility code
     * @param name           Facility name
     * @param type           Facility type (enum)
     * @param capacity       Booking capacity
     * @param location       Physical location/building
     * @param building       Building name
     * @param floor          Floor number
     * @param availabilityStart Start of availability period (local time)
     * @param availabilityEnd   End of availability period (local time)
     * @return Facility instance of the appropriate subtype
     * @throws IllegalArgumentException if facility type is not supported
     */
    public Facility createFacility(
            String facilityCode,
            String name,
            Facility.FacilityType type,
            Integer capacity,
            String location,
            String building,
            String floor,
            LocalTime availabilityStart,
            LocalTime availabilityEnd) {

        log.debug("Creating facility {} of type {}", facilityCode, type);

        Facility facility = switch (type) {
            case LECTURE_HALL -> createLectureHall(facilityCode, name, capacity, location, building, floor, availabilityStart, availabilityEnd);
            case LAB -> createLab(facilityCode, name, capacity, location, building, floor, availabilityStart, availabilityEnd);
            case MEETING_ROOM -> createMeetingRoom(facilityCode, name, capacity, location, building, floor, availabilityStart, availabilityEnd);
            case AUDITORIUM -> createAuditorium(facilityCode, name, capacity, location, building, floor, availabilityStart, availabilityEnd);
            case EQUIPMENT -> createEquipment(facilityCode, name, capacity, location, building, floor, availabilityStart, availabilityEnd);
            case SPORTS_FACILITY -> createSportsFacility(facilityCode, name, capacity, location, building, floor, availabilityStart, availabilityEnd);
            default -> throw new IllegalArgumentException("Unsupported facility type: " + type);
        };

        log.debug("Successfully created facility: {}", facilityCode);
        return facility;
    }

    /**
     * Creates a LectureHall facility.
     * Initializes with default values: wheelchair accessibility false, empty AV equipment set.
     */
    private LectureHall createLectureHall(
            String facilityCode,
            String name,
            Integer capacity,
            String location,
            String building,
            String floor,
            LocalTime availabilityStart,
            LocalTime availabilityEnd) {

        LectureHall lectureHall = new LectureHall();
        lectureHall.setFacilityCode(facilityCode);
        lectureHall.setName(name);
        lectureHall.setType(Facility.FacilityType.LECTURE_HALL);
        lectureHall.setCapacity(capacity);
        lectureHall.setLocation(location);
        lectureHall.setBuilding(building);
        lectureHall.setFloor(floor);
        lectureHall.setAvailabilityStart(availabilityStart);
        lectureHall.setAvailabilityEnd(availabilityEnd);
        lectureHall.setStatus(Facility.FacilityStatus.ACTIVE);
        lectureHall.setWheelchairAccessible(false);

        return lectureHall;
    }

    /**
     * Creates a Lab facility.
     * Initializes with default values: empty software list and safety equipment set.
     */
    private Lab createLab(
            String facilityCode,
            String name,
            Integer capacity,
            String location,
            String building,
            String floor,
            LocalTime availabilityStart,
            LocalTime availabilityEnd) {

        Lab lab = new Lab();
        lab.setFacilityCode(facilityCode);
        lab.setName(name);
        lab.setType(Facility.FacilityType.LAB);
        lab.setCapacity(capacity);
        lab.setLocation(location);
        lab.setBuilding(building);
        lab.setFloor(floor);
        lab.setAvailabilityStart(availabilityStart);
        lab.setAvailabilityEnd(availabilityEnd);
        lab.setStatus(Facility.FacilityStatus.ACTIVE);

        return lab;
    }

    /**
     * Creates a MeetingRoom facility.
     * Initializes with default values: AV disabled, catering not allowed.
     */
    private MeetingRoom createMeetingRoom(
            String facilityCode,
            String name,
            Integer capacity,
            String location,
            String building,
            String floor,
            LocalTime availabilityStart,
            LocalTime availabilityEnd) {

        MeetingRoom meetingRoom = new MeetingRoom();
        meetingRoom.setFacilityCode(facilityCode);
        meetingRoom.setName(name);
        meetingRoom.setType(Facility.FacilityType.MEETING_ROOM);
        meetingRoom.setCapacity(capacity);
        meetingRoom.setLocation(location);
        meetingRoom.setBuilding(building);
        meetingRoom.setFloor(floor);
        meetingRoom.setAvailabilityStart(availabilityStart);
        meetingRoom.setAvailabilityEnd(availabilityEnd);
        meetingRoom.setStatus(Facility.FacilityStatus.ACTIVE);
        meetingRoom.setAvEnabled(false);
        meetingRoom.setCateringAllowed(false);

        return meetingRoom;
    }

    /**
     * Creates an Auditorium facility.
     * Initializes with empty stage type and sound system fields.
     */
    private Auditorium createAuditorium(
            String facilityCode,
            String name,
            Integer capacity,
            String location,
            String building,
            String floor,
            LocalTime availabilityStart,
            LocalTime availabilityEnd) {

        Auditorium auditorium = new Auditorium();
        auditorium.setFacilityCode(facilityCode);
        auditorium.setName(name);
        auditorium.setType(Facility.FacilityType.AUDITORIUM);
        auditorium.setCapacity(capacity);
        auditorium.setLocation(location);
        auditorium.setBuilding(building);
        auditorium.setFloor(floor);
        auditorium.setAvailabilityStart(availabilityStart);
        auditorium.setAvailabilityEnd(availabilityEnd);
        auditorium.setStatus(Facility.FacilityStatus.ACTIVE);

        return auditorium;
    }

    /**
     * Creates an Equipment facility.
     * Brand is required; model, serial number, and maintenance schedule are optional.
     */
    private Equipment createEquipment(
            String facilityCode,
            String name,
            Integer capacity,
            String location,
            String building,
            String floor,
            LocalTime availabilityStart,
            LocalTime availabilityEnd) {

        Equipment equipment = new Equipment();
        equipment.setFacilityCode(facilityCode);
        equipment.setName(name);
        equipment.setType(Facility.FacilityType.EQUIPMENT);
        equipment.setCapacity(capacity);
        equipment.setLocation(location);
        equipment.setBuilding(building);
        equipment.setFloor(floor);
        equipment.setAvailabilityStart(availabilityStart);
        equipment.setAvailabilityEnd(availabilityEnd);
        equipment.setStatus(Facility.FacilityStatus.ACTIVE);
        // Brand must be provided when creating equipment via controller/service

        return equipment;
    }

    /**
     * Creates a SportsFacility facility.
     * Initializes with empty sports type and equipment available set.
     */
    private SportsFacility createSportsFacility(
            String facilityCode,
            String name,
            Integer capacity,
            String location,
            String building,
            String floor,
            LocalTime availabilityStart,
            LocalTime availabilityEnd) {

        SportsFacility sportsFacility = new SportsFacility();
        sportsFacility.setFacilityCode(facilityCode);
        sportsFacility.setName(name);
        sportsFacility.setType(Facility.FacilityType.SPORTS_FACILITY);
        sportsFacility.setCapacity(capacity);
        sportsFacility.setLocation(location);
        sportsFacility.setBuilding(building);
        sportsFacility.setFloor(floor);
        sportsFacility.setAvailabilityStart(availabilityStart);
        sportsFacility.setAvailabilityEnd(availabilityEnd);
        sportsFacility.setStatus(Facility.FacilityStatus.ACTIVE);

        return sportsFacility;
    }

    /**
     * Builder method for creating a facility with fluent API.
     * Allows setting type-specific metadata after initial construction.
     *
     * @param facilityCode Unique facility code
     * @param name Facility name
     * @param type Facility type
     * @param capacity Booking capacity
     * @param location Physical location
     * @param availabilityStart Start time
     * @param availabilityEnd End time
     * @return Facility instance ready for customization
     */
    public Facility createWithDefaults(
            String facilityCode,
            String name,
            Facility.FacilityType type,
            Integer capacity,
            String location,
            LocalTime availabilityStart,
            LocalTime availabilityEnd) {

        return createFacility(
            facilityCode,
            name,
            type,
            capacity,
            location,
            null,  // building
            null,  // floor
            availabilityStart,
            availabilityEnd
        );
    }
}
