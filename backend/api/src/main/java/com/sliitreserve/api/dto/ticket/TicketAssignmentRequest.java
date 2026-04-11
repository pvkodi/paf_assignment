package com.sliitreserve.api.dto.ticket;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for assigning/unassigning a ticket to a technician.
 *
 * <p><b>Constraints</b>:
 * <ul>
 *   <li>Technician ID: Must reference an existing user with TECHNICIAN role (nullable for unassignment)
 * </ul>
 *
 * <p><b>Permissions</b>:
 * <ul>
 *   <li>Only FACILITY_MANAGER and ADMIN can assign/unassign tickets
 *   <li>Technician must belong to the same facility
 * </ul>
 *
 * <p><b>Usage</b>:
 * <ul>
 *   <li>Assignment: POST /api/tickets/{ticketId}/assign with technicianId
 *   <li>Unassignment: POST /api/tickets/{ticketId}/assign with technicianId=null
 * </ul>
 *
 * @see com.sliitreserve.api.entities.ticket.MaintenanceTicket for entity structure
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketAssignmentRequest {

  /**
   * Technician user ID to assign to the ticket.
   * Must reference a user with TECHNICIAN role in the same facility.
   * Can be null to unassign from current technician.
   */
  @NotNull(message = "Technician ID is required for assignment")
  private UUID technicianId;
}
