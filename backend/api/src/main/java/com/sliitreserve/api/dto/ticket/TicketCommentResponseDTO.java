package com.sliitreserve.api.dto.ticket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.sliitreserve.api.entities.ticket.TicketCommentVisibility;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for ticket comments.
 *
 * <p><b>Visibility Rules</b>:
 * <ul>
 *   <li>PUBLIC comments visible to creator, assigned technician, staff
 *   <li>INTERNAL comments visible to staff only (TECHNICIAN, FACILITY_MANAGER, ADMIN)
 *   <li>Deleted comments (deletedAt != null) excluded from responses
 * </ul>
 *
 * @see com.sliitreserve.api.entities.ticket.TicketComment for entity structure
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketCommentResponseDTO {

  private UUID id;
  private String content;
  private TicketCommentVisibility visibility;

  private UUID authorId;
  private String authorName;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Boolean isEdited;
}
