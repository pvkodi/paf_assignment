package com.sliitreserve.api.dto.ticket;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.sliitreserve.api.entities.ticket.TicketStatus;

/**
 * Request DTO for updating ticket status via state machine transitions.
 *
 * <p><b>Validation</b>:
 * <ul>
 *   <li>Status must be a valid TicketStatus enum value
 *   <li>Status transition must be valid per TicketStateMachine rules
 *   <li>Current user must have permission to transition (FACILITY_MANAGER, TECHNICIAN, ADMIN)
 * </ul>
 *
 * <p><b>State Machine Rules</b>:
 * <ul>
 *   <li>OPEN → IN_PROGRESS (technician starts work)
 *   <li>IN_PROGRESS → CLOSED (work completed)
 *   <li>IN_PROGRESS → ESCALATED (requires escalation)
 *   <li>Any status → REJECTED (manager/admin rejection)
 *   <li>CLOSED, REJECTED are terminal states (no outbound transitions)
 * </ul>
 *
 * @see com.sliitreserve.api.state.TicketStateMachine for transition rules
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketStatusUpdate {

  /**
   * New status to transition to.
   * Must be a valid TicketStatus enum (OPEN, IN_PROGRESS, RESOLVED, CLOSED, REJECTED).
   */
  @NotNull(message = "Status is required")
  private TicketStatus status;

  /**
   * Optional rejection reason - required when status is REJECTED.
   * Must be at least 10 characters for REJECTED status.
   */
  private String rejectionReason;
}
