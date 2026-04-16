package com.sliitreserve.api.factories;

import com.sliitreserve.api.dto.facility.FacilityRequestDTO;
import com.sliitreserve.api.entities.facility.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Simple Factory for creating Facility subtype instances.
 * Used by import routines, tests and seeding data.
 */
@Component
public class FacilityFactory {

    public Facility createFacility(FacilityRequestDTO request) {
        if (request == null || request.getType() == null) {
            throw new IllegalArgumentException("Facility type is required");
        }

        String facilityCode = resolveFacilityCode(request);
        Facility facility;

        switch (request.getType()) {
            case LECTURE_HALL:
                facility = new LectureHall(facilityCode, request.getName(), request.getCapacity(), request.getLocationDescription());
                break;
            case LAB:
                facility = new Lab(facilityCode, request.getName(), request.getCapacity(), request.getLocationDescription(), "standard");
                break;
            case MEETING_ROOM:
                facility = new MeetingRoom(facilityCode, request.getName(), request.getCapacity(), request.getLocationDescription());
                break;
            case AUDITORIUM:
                facility = new Auditorium(facilityCode, request.getName(), request.getCapacity(), request.getLocationDescription());
                break;
            case EQUIPMENT:
                facility = new Equipment(facilityCode, request.getName(), request.getCapacity(), request.getLocationDescription(), "unknown");
                break;
            case SPORTS:
            case SPORTS_FACILITY:
                facility = new SportsFacility(facilityCode, request.getName(), request.getCapacity(), request.getLocationDescription(), "multi");
                break;
            default:
                facility = new Facility();
                facility.setFacilityCode(facilityCode);
                facility.setName(request.getName());
                facility.setType(request.getType());
                facility.setCapacity(request.getCapacity());
                facility.setLocationDescription(request.getLocationDescription());
        }

        facility.setBuilding(request.getBuilding());
        facility.setFloor(request.getFloor());
        facility.setLocationDescription(request.getLocationDescription());
        facility.setAvailabilityStartTime(request.getAvailabilityStartTime());
        facility.setAvailabilityEndTime(request.getAvailabilityEndTime());
        facility.setStatus(request.getStatus() == null ? Facility.FacilityStatus.ACTIVE : request.getStatus());

        applySubtypeAttributes(facility, request.getSubtypeAttributes());
        return facility;
    }

    public void applySubtypeAttributes(Facility facility, Map<String, Object> attributes) {
        if (facility == null || attributes == null || attributes.isEmpty()) {
            return;
        }

        if (facility instanceof LectureHall lectureHall) {
            lectureHall.setWheelchairAccessible(asBoolean(attributes.get("wheelchairAccessible"), false));
        } else if (facility instanceof Lab lab) {
            String labType = asString(attributes.get("labType"));
            if (labType != null) {
                lab.setLabType(labType);
            }
        } else if (facility instanceof MeetingRoom meetingRoom) {
            meetingRoom.setAvEnabled(asBoolean(attributes.get("avEnabled"), false));
            meetingRoom.setCateringAllowed(asBoolean(attributes.get("cateringAllowed"), false));
        } else if (facility instanceof Auditorium auditorium) {
            String stageType = asString(attributes.get("stageType"));
            String soundSystem = asString(attributes.get("soundSystem"));
            if (stageType != null) {
                auditorium.setStageType(stageType);
            }
            if (soundSystem != null) {
                auditorium.setSoundSystem(soundSystem);
            }
        } else if (facility instanceof Equipment equipment) {
            String brand = asString(attributes.get("brand"));
            if (brand != null) {
                equipment.setBrand(brand);
            }
            String model = asString(attributes.get("model"));
            if (model != null) {
                equipment.setModel(model);
            }
            String serialNumber = asString(attributes.get("serialNumber"));
            if (serialNumber != null) {
                equipment.setSerialNumber(serialNumber);
            }
            String maintenanceSchedule = asString(attributes.get("maintenanceSchedule"));
            if (maintenanceSchedule != null) {
                equipment.setMaintenanceSchedule(maintenanceSchedule);
            }
        } else if (facility instanceof SportsFacility sportsFacility) {
            String sportsType = asString(attributes.get("sportsType"));
            if (sportsType != null) {
                sportsFacility.setSportsType(sportsType);
            }
        }
    }

    private String resolveFacilityCode(FacilityRequestDTO request) {
        if (request.getFacilityCode() != null && !request.getFacilityCode().isBlank()) {
            return request.getFacilityCode().trim();
        }

        String prefix = request.getType().name().replace("_", "");
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private boolean asBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }

        if (value instanceof String text) {
            return Boolean.parseBoolean(text);
        }

        return fallback;
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
