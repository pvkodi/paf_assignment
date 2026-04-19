package com.sliitreserve.api.util.mapping;

import com.sliitreserve.api.dto.facility.AvailabilityWindowDTO;
import com.sliitreserve.api.dto.facility.FacilityRequestDTO;
import com.sliitreserve.api.dto.facility.FacilityResponseDTO;
import com.sliitreserve.api.entities.facility.Auditorium;
import com.sliitreserve.api.entities.facility.AvailabilityWindow;
import com.sliitreserve.api.entities.facility.Equipment;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.facility.Lab;
import com.sliitreserve.api.entities.facility.LectureHall;
import com.sliitreserve.api.entities.facility.MeetingRoom;
import com.sliitreserve.api.entities.facility.SportsFacility;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Facility entities and DTOs.
 * Handles bidirectional mapping: Facility <-> FacilityResponseDTO and FacilityRequestDTO.
 */
@Component
public class FacilityMapper implements BaseMapper<Facility, FacilityRequestDTO, FacilityResponseDTO> {

    /**
     * Convert Facility entity to FacilityResponseDTO.
     *
     * @param facility Facility entity
     * @return FacilityResponseDTO with all fields populated
     */
    @Override
    public FacilityResponseDTO toResponseDTO(Facility facility) {
        if (facility == null) {
            return null;
        }

        FacilityResponseDTO dto = new FacilityResponseDTO();
        dto.setId(facility.getId());
        dto.setFacilityCode(facility.getFacilityCode());
        dto.setName(facility.getName());
        dto.setType(facility.getType());
        dto.setCapacity(facility.getCapacity());
        dto.setBuilding(facility.getBuilding());
        dto.setFloor(facility.getFloor());
        dto.setLocationDescription(facility.getLocationDescription());
        dto.setStatus(facility.getStatus());
        dto.setAvailabilityStartTime(facility.getAvailabilityStartTime());
        dto.setAvailabilityEndTime(facility.getAvailabilityEndTime());
        // Map multi-window schedule
        if (facility.getAvailabilityWindows() != null) {
            dto.setAvailabilityWindows(
                facility.getAvailabilityWindows().stream()
                    .map(w -> AvailabilityWindowDTO.builder()
                        .dayOfWeek(w.getDayOfWeek())
                        .startTime(w.getStartTime())
                        .endTime(w.getEndTime())
                        .build())
                    .collect(Collectors.toList())
            );
        }
        // Map geofencing fields
        dto.setLatitude(facility.getLatitude());
        dto.setLongitude(facility.getLongitude());
        dto.setWifiSSID(facility.getWifiSSID());
        dto.setWifiMacAddress(facility.getWifiMacAddress());
        dto.setGeofenceRadiusMeters(facility.getGeofenceRadiusMeters());
        
        dto.setSubtypeAttributes(extractSubtypeAttributes(facility));
        dto.setCreatedAt(facility.getCreatedAt());
        dto.setUpdatedAt(facility.getUpdatedAt());

        return dto;
    }

    /**
     * Convert FacilityRequestDTO to Facility entity.
     * Does not set ID or timestamps (server generates these).
     *
     * @param requestDTO FacilityRequestDTO
     * @return Facility entity (without ID/timestamps)
     */
    @Override
    public Facility toEntity(FacilityRequestDTO requestDTO) {
        if (requestDTO == null) {
            return null;
        }

        Facility facility = new Facility();
        facility.setFacilityCode(requestDTO.getFacilityCode());
        facility.setName(requestDTO.getName());
        facility.setType(requestDTO.getType());
        facility.setCapacity(requestDTO.getCapacity());
        facility.setLocationDescription(requestDTO.getLocationDescription());
        facility.setBuilding(requestDTO.getBuilding());
        facility.setFloor(requestDTO.getFloor());
        facility.setStatus(requestDTO.getStatus());
        facility.setAvailabilityStartTime(requestDTO.getAvailabilityStartTime());
        facility.setAvailabilityEndTime(requestDTO.getAvailabilityEndTime());
        // Set geofencing fields
        facility.setLatitude(requestDTO.getLatitude());
        facility.setLongitude(requestDTO.getLongitude());
        facility.setWifiSSID(requestDTO.getWifiSSID());
        facility.setWifiMacAddress(requestDTO.getWifiMacAddress());
        facility.setGeofenceRadiusMeters(requestDTO.getGeofenceRadiusMeters());

        return facility;
    }

    /**
     * Update existing Facility from FacilityRequestDTO.
     * Preserves ID and timestamps from existing entity.
     *
     * @param requestDTO FacilityRequestDTO with updated fields
     * @param existingFacility Existing Facility entity
     * @return Updated Facility entity
     */
    @Override
    public Facility updateEntity(FacilityRequestDTO requestDTO, Facility existingFacility) {
        if (requestDTO == null || existingFacility == null) {
            return existingFacility;
        }

        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());
        
        // Update fields (do not update ID or createdAt)
        if (requestDTO.getFacilityCode() != null) {
            existingFacility.setFacilityCode(requestDTO.getFacilityCode());
        }
        existingFacility.setName(requestDTO.getName());
        // DO NOT update type during update operations - type changes are not allowed
        // existingFacility.setType(requestDTO.getType());
        existingFacility.setCapacity(requestDTO.getCapacity());
        existingFacility.setLocationDescription(requestDTO.getLocationDescription());
        existingFacility.setBuilding(requestDTO.getBuilding());
        existingFacility.setFloor(requestDTO.getFloor());
        existingFacility.setStatus(requestDTO.getStatus());
        existingFacility.setAvailabilityStartTime(requestDTO.getAvailabilityStartTime());
        existingFacility.setAvailabilityEndTime(requestDTO.getAvailabilityEndTime());
        // Replace availability windows if provided
        if (requestDTO.getAvailabilityWindows() != null) {
            List<AvailabilityWindow> windows = requestDTO.getAvailabilityWindows().stream()
                .map(dto -> AvailabilityWindow.builder()
                    .dayOfWeek(dto.getDayOfWeek())
                    .startTime(dto.getStartTime())
                    .endTime(dto.getEndTime())
                    .build())
                .collect(Collectors.toList());
            existingFacility.getAvailabilityWindows().clear();
            existingFacility.getAvailabilityWindows().addAll(windows);
        }
        
        // Update geofencing fields
        if (requestDTO.getLatitude() != null) {
            logger.debug("🔄 Updating latitude from {} to {}", existingFacility.getLatitude(), requestDTO.getLatitude());
            existingFacility.setLatitude(requestDTO.getLatitude());
        }
        if (requestDTO.getLongitude() != null) {
            logger.debug("🔄 Updating longitude from {} to {}", existingFacility.getLongitude(), requestDTO.getLongitude());
            existingFacility.setLongitude(requestDTO.getLongitude());
        }
        if (requestDTO.getWifiSSID() != null) {
            existingFacility.setWifiSSID(requestDTO.getWifiSSID());
        }
        if (requestDTO.getWifiMacAddress() != null) {
            existingFacility.setWifiMacAddress(requestDTO.getWifiMacAddress());
        }
        if (requestDTO.getGeofenceRadiusMeters() != null) {
            logger.debug("🔄 Updating geofence radius from {} to {}", existingFacility.getGeofenceRadiusMeters(), requestDTO.getGeofenceRadiusMeters());
            existingFacility.setGeofenceRadiusMeters(requestDTO.getGeofenceRadiusMeters());
        }

        return existingFacility;
    }

    private Map<String, Object> extractSubtypeAttributes(Facility facility) {
        Map<String, Object> attributes = new HashMap<>();

        if (facility instanceof LectureHall) {
            LectureHall lectureHall = (LectureHall) facility;
            attributes.put("avEquipment", lectureHall.getAvEquipment());
            attributes.put("wheelchairAccessible", lectureHall.getWheelchairAccessible());
        } else if (facility instanceof Lab) {
            Lab lab = (Lab) facility;
            attributes.put("labType", lab.getLabType());
            attributes.put("softwareList", lab.getSoftwareList());
            attributes.put("safetyEquipment", lab.getSafetyEquipment());
        } else if (facility instanceof MeetingRoom) {
            MeetingRoom meetingRoom = (MeetingRoom) facility;
            attributes.put("avEnabled", meetingRoom.getAvEnabled());
            attributes.put("cateringAllowed", meetingRoom.getCateringAllowed());
        } else if (facility instanceof Auditorium) {
            Auditorium auditorium = (Auditorium) facility;
            attributes.put("stageType", auditorium.getStageType());
            attributes.put("soundSystem", auditorium.getSoundSystem());
        } else if (facility instanceof Equipment) {
            Equipment equipment = (Equipment) facility;
            attributes.put("brand", equipment.getBrand());
            attributes.put("model", equipment.getModel());
            attributes.put("serialNumber", equipment.getSerialNumber());
            attributes.put("maintenanceSchedule", equipment.getMaintenanceSchedule());
        } else if (facility instanceof SportsFacility) {
            SportsFacility sportsFacility = (SportsFacility) facility;
            attributes.put("sportsType", sportsFacility.getSportsType());
            attributes.put("equipmentAvailable", sportsFacility.getEquipmentAvailable());
        }

        return attributes;
    }
}
