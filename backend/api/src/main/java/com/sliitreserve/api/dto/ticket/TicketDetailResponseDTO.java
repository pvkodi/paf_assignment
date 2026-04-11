package com.sliitreserve.api.dto.ticket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.sliitreserve.api.entities.ticket.TicketCategory;
import com.sliitreserve.api.entities.ticket.TicketPriority;
import com.sliitreserve.api.entities.ticket.TicketStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for detailed ticket information.
 *
 * <p>Used for GET /tickets/{id} endpoints. Includes all ticket metadata,
 * comments (filtered by visibility), and attachments.
 *
 * <p><b>Comment Visibility Filtering</b>:
 * <ul>
 *   <li>PUBLIC comments visible to all permitted users
 *   <li>INTERNAL comments visible to staff only
 *   <li>Deleted comments excluded
 * </ul>
 *
 * <p><b>Permission Checks</b>:
 * <ul>
 *   <li>Creator can always view their own tickets
 *   <li>Assigned technician can view assigned tickets
 *   <li>Staff can view all facility tickets
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketDetailResponseDTO {

  private UUID id;
  private String title;
  private String description;
  private TicketCategory category;
  private TicketPriority priority;
  private TicketStatus status;

  private Integer escalationLevel;
  private LocalDateTime slaDueAt;
  private Boolean slaBreach;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private UUID facilityId;
  private String facilityName;

  private UUID createdById;
  private String createdByName;

  private UUID assignedTechnicianId;
  private String assignedTechnicianName;

  private List<TicketCommentResponseDTO> comments;
  private List<TicketAttachmentResponseDTO> attachments;
  private List<TicketEscalationHistoryDTO> escalationHistory;
}
