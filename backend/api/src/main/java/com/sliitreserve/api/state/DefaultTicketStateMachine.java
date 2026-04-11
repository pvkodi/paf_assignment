package com.sliitreserve.api.state;

import com.sliitreserve.api.entities.ticket.MaintenanceTicket;
import com.sliitreserve.api.entities.ticket.TicketStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Default Implementation of Ticket State Machine
 *
 * <p>Purpose: Enforce valid ticket status transitions according to the ticket lifecycle rules
 * (FR-025). This implementation uses a State Pattern with explicit transition rules defined in a
 * transition matrix.
 *
 * <p><b>Valid Transitions</b>:
 * <ul>
 *   <li>OPEN → {IN_PROGRESS, REJECTED}
 *   <li>IN_PROGRESS → {RESOLVED, REJECTED}
 *   <li>RESOLVED → {CLOSED}
 *   <li>CLOSED → {} (terminal, no transitions)
 *   <li>REJECTED → {} (terminal, no transitions)
 * </ul>
 *
 * <p><b>Idempotent Behavior</b>: Transitions to the same state are allowed (no-op).
 *
 * @author Maintenance Ticketing Module
 */
@Slf4j
@Component
public class DefaultTicketStateMachine implements TicketStateMachine {

  private final Map<TicketStatus, Set<TicketStatus>> transitionMatrix;

  public DefaultTicketStateMachine() {
    this.transitionMatrix = buildTransitionMatrix();
  }

  /**
   * Build the transition matrix that defines all allowed transitions.
   *
   * @return a map of current status to set of allowed target statuses
   */
  private Map<TicketStatus, Set<TicketStatus>> buildTransitionMatrix() {
    Map<TicketStatus, Set<TicketStatus>> matrix = new HashMap<>();

    // OPEN state: can transition to IN_PROGRESS or REJECTED
    Set<TicketStatus> openTransitions = new HashSet<>();
    openTransitions.add(TicketStatus.OPEN); // Idempotent
    openTransitions.add(TicketStatus.IN_PROGRESS);
    openTransitions.add(TicketStatus.REJECTED);
    matrix.put(TicketStatus.OPEN, openTransitions);

    // IN_PROGRESS state: can transition to RESOLVED or REJECTED
    Set<TicketStatus> inProgressTransitions = new HashSet<>();
    inProgressTransitions.add(TicketStatus.IN_PROGRESS); // Idempotent
    inProgressTransitions.add(TicketStatus.RESOLVED);
    inProgressTransitions.add(TicketStatus.REJECTED);
    matrix.put(TicketStatus.IN_PROGRESS, inProgressTransitions);

    // RESOLVED state: can transition to CLOSED
    Set<TicketStatus> resolvedTransitions = new HashSet<>();
    resolvedTransitions.add(TicketStatus.RESOLVED); // Idempotent
    resolvedTransitions.add(TicketStatus.CLOSED);
    matrix.put(TicketStatus.RESOLVED, resolvedTransitions);

    // CLOSED state: terminal state, only idempotent transition
    Set<TicketStatus> closedTransitions = new HashSet<>();
    closedTransitions.add(TicketStatus.CLOSED); // Idempotent only
    matrix.put(TicketStatus.CLOSED, closedTransitions);

    // REJECTED state: terminal state, only idempotent transition
    Set<TicketStatus> rejectedTransitions = new HashSet<>();
    rejectedTransitions.add(TicketStatus.REJECTED); // Idempotent only
    matrix.put(TicketStatus.REJECTED, rejectedTransitions);

    return matrix;
  }

  /**
   * Check if a transition from current status to target status is allowed.
   *
   * @param currentStatus the current ticket status
   * @param targetStatus the desired target status
   * @return true if transition is allowed, false otherwise
   * @throws IllegalArgumentException if either parameter is null
   */
  @Override
  public boolean canTransition(TicketStatus currentStatus, TicketStatus targetStatus) {
    if (currentStatus == null || targetStatus == null) {
      throw new IllegalArgumentException(
          "Status parameters cannot be null. currentStatus=" + currentStatus
              + ", targetStatus=" + targetStatus);
    }

    Set<TicketStatus> allowedTransitions = transitionMatrix.get(currentStatus);
    if (allowedTransitions == null) {
      log.warn("Unknown current status in transition matrix: {}", currentStatus);
      return false;
    }

    return allowedTransitions.contains(targetStatus);
  }

  /**
   * Perform a state transition on the ticket.
   *
   * <p>Validates that the transition is allowed before executing it. If validation fails, throws
   * an exception without modifying the ticket.
   *
   * @param ticket the ticket to transition
   * @param targetStatus the desired target status
   * @throws IllegalArgumentException if ticket or targetStatus is null
   * @throws IllegalStateException if the transition is not allowed from the current status
   */
  @Override
  public void transition(MaintenanceTicket ticket, TicketStatus targetStatus) {
    if (ticket == null) {
      throw new IllegalArgumentException("Ticket cannot be null");
    }

    if (targetStatus == null) {
      throw new IllegalArgumentException("Target status cannot be null");
    }

    TicketStatus currentStatus = ticket.getStatus();

    if (!canTransition(currentStatus, targetStatus)) {
      String message =
          String.format(
              "Invalid ticket status transition: %s -> %s. This transition is not allowed by the state machine.",
              currentStatus, targetStatus);
      log.warn(message);
      throw new IllegalStateException(message);
    }

    // Log the transition
    log.debug("Transitioning ticket {} from {} to {}", ticket.getId(), currentStatus, targetStatus);

    // The actual status update is the caller's responsibility,
    // but we can provide additional business logic here if needed.
    // For now, the state machine is purely a validator.
  }
}
