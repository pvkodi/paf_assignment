package com.sliitreserve.api.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.sliitreserve.api.entities.ticket.TicketCommentVisibility;

/**
 * Request DTO for adding a comment to a ticket.
 *
 * <p><b>Constraints</b>:
 * <ul>
 *   <li>Content: 1-5000 characters (required)
 *   <li>Visibility: Must be PUBLIC or INTERNAL
 * </ul>
 *
 * <p><b>Visibility Rules</b>:
 * <ul>
 *   <li>PUBLIC: Visible to ticket creator, assigned technician, staff (TECHNICIAN, FACILITY_MANAGER, ADMIN)
 *   <li>INTERNAL: Staff-only visibility (TECHNICIAN, FACILITY_MANAGER, ADMIN)
 * </ul>
 *
 * <p><b>Permissions</b>:
 * <ul>
 *   <li>Any authenticated user can add PUBLIC comments
 *   <li>Only staff (TECHNICIAN, FACILITY_MANAGER, ADMIN) can add INTERNAL comments
 * </ul>
 *
 * @see com.sliitreserve.api.entities.ticket.TicketComment for entity structure
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketCommentRequest {

  /**
   * Content of the comment.
   * Can be plain text or HTML (sanitized by backend).
   * Examples: "Status update: water source identified", "Waiting for parts arrival"
   */
  @NotBlank(message = "Comment content is required")
  @Size(min = 1, max = 5000, message = "Comment must be between 1 and 5000 characters")
  private String content;

  /**
   * Visibility level of the comment.
   * PUBLIC: Visible to creator, technician, staff
   * INTERNAL: Staff-only visibility
   */
  @NotNull(message = "Visibility is required")
  private TicketCommentVisibility visibility;
}
