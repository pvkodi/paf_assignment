package com.sliitreserve.api.util.mapping;

/**
 * Base mapper interface for all entity-to-DTO conversions.
 * Provides contract for bidirectional mapping between entities and DTOs.
 *
 * @param <E> Entity type
 * @param <RQ> Request DTO type
 * @param <RS> Response DTO type
 */
public interface BaseMapper<E, RQ, RS> {

    /**
     * Convert entity to response DTO.
     *
     * @param entity Entity to convert
     * @return Response DTO
     */
    RS toResponseDTO(E entity);

    /**
     * Convert request DTO to entity.
     * Note: Returned entity will not have ID or timestamps set.
     *
     * @param requestDTO Request DTO
     * @return Entity (without ID/timestamps)
     */
    E toEntity(RQ requestDTO);

    /**
     * Update existing entity from request DTO.
     * Preserves ID and timestamps from existing entity.
     *
     * @param requestDTO Request DTO with updated fields
     * @param existingEntity Existing entity to update
     * @return Updated entity
     */
    E updateEntity(RQ requestDTO, E existingEntity);
}
