package com.sliitreserve.api.util.mapping;

import com.sliitreserve.api.dto.FacilityRequestDTO;
import com.sliitreserve.api.dto.FacilityResponseDTO;
import com.sliitreserve.api.entities.facility.Facility;
import org.springframework.stereotype.Component;

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
        dto.setType(facility.getType() != null ? facility.getType().toString() : null);
        dto.setCapacity(facility.getCapacity());
        dto.setLocation(facility.getLocation());
        dto.setBuilding(facility.getBuilding());
        dto.setFloor(facility.getFloor());
        dto.setStatus(facility.getStatus() != null ? facility.getStatus().toString() : null);
        dto.setAvailabilityStart(facility.getAvailabilityStart());
        dto.setAvailabilityEnd(facility.getAvailabilityEnd());
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
        
        if (requestDTO.getType() != null) {
            try {
                facility.setType(Facility.FacilityType.valueOf(requestDTO.getType()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid facility type: " + requestDTO.getType());
            }
        }
        
        facility.setCapacity(requestDTO.getCapacity());
        facility.setLocation(requestDTO.getLocation());
        facility.setBuilding(requestDTO.getBuilding());
        facility.setFloor(requestDTO.getFloor());
        
        if (requestDTO.getStatus() != null) {
            try {
                facility.setStatus(Facility.FacilityStatus.valueOf(requestDTO.getStatus()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid facility status: " + requestDTO.getStatus());
            }
        }
        
        facility.setAvailabilityStart(requestDTO.getAvailabilityStart());
        facility.setAvailabilityEnd(requestDTO.getAvailabilityEnd());

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

        // Update fields (do not update ID or createdAt)
        if (requestDTO.getFacilityCode() != null) {
            existingFacility.setFacilityCode(requestDTO.getFacilityCode());
        }
        if (requestDTO.getName() != null) {
            existingFacility.setName(requestDTO.getName());
        }
        if (requestDTO.getType() != null) {
            try {
                existingFacility.setType(Facility.FacilityType.valueOf(requestDTO.getType()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid facility type: " + requestDTO.getType());
            }
        }
        if (requestDTO.getCapacity() != null) {
            existingFacility.setCapacity(requestDTO.getCapacity());
        }
        if (requestDTO.getLocation() != null) {
            existingFacility.setLocation(requestDTO.getLocation());
        }
        if (requestDTO.getBuilding() != null) {
            existingFacility.setBuilding(requestDTO.getBuilding());
        }
        if (requestDTO.getFloor() != null) {
            existingFacility.setFloor(requestDTO.getFloor());
        }
        if (requestDTO.getStatus() != null) {
            try {
                existingFacility.setStatus(Facility.FacilityStatus.valueOf(requestDTO.getStatus()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid facility status: " + requestDTO.getStatus());
            }
        }
        if (requestDTO.getAvailabilityStart() != null) {
            existingFacility.setAvailabilityStart(requestDTO.getAvailabilityStart());
        }
        if (requestDTO.getAvailabilityEnd() != null) {
            existingFacility.setAvailabilityEnd(requestDTO.getAvailabilityEnd());
        }

        return existingFacility;
    }
}
