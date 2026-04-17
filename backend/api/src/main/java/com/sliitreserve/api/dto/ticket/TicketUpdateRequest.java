package com.sliitreserve.api.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sliitreserve.api.entities.ticket.TicketCategory;
import com.sliitreserve.api.entities.ticket.TicketPriority;

/**
 * Request DTO for updating an existing maintenance ticket.
 *
 * <p><b>Rules</b>:
 * <ul>
 *   <li>Only ticket creator can edit when status is OPEN
 *   <li>ADMIN can edit anytime
 *   <li>Cannot edit if ticket is assigned or in progress
 * </ul>
 *
 * <p><b>Constraints</b>:
 * <ul>
 *   <li>Title: 20-200 characters
 *   <li>Description: 50+ characters
 *   <li>Category: Must be valid TicketCategory enum
 *   <li>Priority: Must be valid TicketPriority enum
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketUpdateRequest {

  @NotBlank(message = "Title is required")
  @Size(min = 20, max = 200, message = "Title must be between 20 and 200 characters")
  @JsonProperty("title")
  private String title;

  @NotBlank(message = "Description is required")
  @Size(min = 50, message = "Description must be at least 50 characters")
  @JsonProperty("description")
  private String description;

  @JsonProperty("category")
  private TicketCategory category;

  @JsonProperty("priority")
  private TicketPriority priority;
}
