package com.sliitreserve.api.state;

import com.sliitreserve.api.entities.ticket.MaintenanceTicket;
import com.sliitreserve.api.entities.ticket.TicketStatus;

/**
 * State Machine Interface for Ticket Lifecycle Management
 *
 * <p>Purpose: Define a contract for ticket status transitions with guard conditions and state
 * validation.
 *
 * <p><b>Valid State Transitions</b> (from FR-025):
 * <ul>
 *   <li>OPEN → IN_PROGRESS, REJECTED
 *   <li>IN_PROGRESS → RESOLVED, REJECTED
 *   <li>RESOLVED → CLOSED
 *   <li>Terminal States (CLOSED, REJECTED) → no further transitions
 * </ul>
 *
 * @author Maintenance Ticketing Module
 */
public interface TicketStateMachine {

  /**
   * Check if a transition from current status to target status is allowed by the state machine
   * rules.
   *
   * <p>This method performs a pure check without modifying any state.
   *
   * @param currentStatus the current ticket status
   * @param targetStatus the desired target status
   * @return true if transition is allowed, false otherwise
   * @throws IllegalArgumentException if either parameter is null
   */
  boolean canTransition(TicketStatus currentStatus, TicketStatus targetStatus);

  /**
   * Perform a state transition on the ticket.
   *
   * <p>This method validates that the transition is allowed before performing it. If the
   * transition is not allowed, it throws an exception and leaves the ticket unchanged.
   *
   * @param ticket the ticket to transition (will have its status updated)
   * @param targetStatus the desired target status
   * @throws IllegalArgumentException if ticket or targetStatus is null
   * @throws IllegalStateException if the transition is not allowed from the current status
   */
  void transition(MaintenanceTicket ticket, TicketStatus targetStatus);
}
