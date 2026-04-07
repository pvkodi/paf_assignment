package com.sliitreserve.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic paginated response DTO.
 * Used for list endpoints that return multiple records with pagination.
 *
 * @param <T> Type of items in the page
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDTO<T> implements BaseDTO {

    @JsonProperty("items")
    private List<T> items;

    @JsonProperty("total_count")
    private long totalCount;

    @JsonProperty("page_number")
    private int pageNumber;

    @JsonProperty("page_size")
    private int pageSize;

    @JsonProperty("total_pages")
    private int totalPages;

    @JsonProperty("has_next")
    private boolean hasNext;

    @JsonProperty("has_previous")
    private boolean hasPrevious;
}
