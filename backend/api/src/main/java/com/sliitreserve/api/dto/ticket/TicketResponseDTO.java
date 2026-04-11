package com.sliitreserve.api.dto.ticket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.sliitreserve.api.entities.ticket.TicketCategory;
import com.sliitreserve.api.entities.ticket.TicketPriority;
import com.sliitreserve.api.entities.ticket.TicketStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for ticket summary information.
 *
 * <p>Used for list endpoints and ticket overview screens.
 * Includes essential ticket metadata but not full comments/attachments.
 *
 * <p><b>Visibility Rules</b>:
 * <ul>
 *   <li>Ticket creators always see their own tickets
 *   <li>Assigned technicians see their assigned tickets
 *   <li>Staff (FACILITY_MANAGER, ADMIN) see all facility tickets
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponseDTO {

  private UUID id;
  private String title;
  private String description;
  private TicketCategory category;
  private TicketPriority priority;
  private TicketStatus status;
  private Integer escalationLevel;
  private LocalDateTime slaDueAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private UUID facilityId;
  private String facilityName;

  private UUID createdById;
  private String createdByName;

  private UUID assignedTechnicianId;
  private String assignedTechnicianName;

  private Integer attachmentCount;
  private Integer commentCount;
}
