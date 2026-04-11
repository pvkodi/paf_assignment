package com.sliitreserve.api.repositories.ticket;

import com.sliitreserve.api.entities.ticket.MaintenanceTicket;
import com.sliitreserve.api.entities.ticket.TicketEscalation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for TicketEscalation entity persistence operations.
 *
 * <p>Provides CRUD operations and custom queries for managing ticket escalation audit trail,
 * including querying escalation history and recording escalation events.
 *
 * @see TicketEscalation
 * @see MaintenanceTicket
 */
@Repository
public interface TicketEscalationRepository extends JpaRepository<TicketEscalation, UUID> {

  /**
   * Find all escalation events for a given ticket, ordered by escalation time (ascending).
   *
   * @param ticket the ticket to find escalation events for
   * @return list of escalation events in chronological order
   */
  List<TicketEscalation> findByTicketOrderByEscalatedAtAsc(MaintenanceTicket ticket);

  /**
   * Find the most recent escalation event for a ticket.
   *
   * @param ticket the ticket to find the latest escalation for
   * @return the most recent escalation event if any exist, or empty if none
   */
  TicketEscalation findFirstByTicketOrderByEscalatedAtDesc(MaintenanceTicket ticket);

  /**
   * Count escalation events for a ticket at a specific level.
   *
   * @param ticket the ticket to check
   * @param level the escalation level to count
   * @return count of escalation events at that level
   */
  long countByTicketAndToLevel(MaintenanceTicket ticket, Integer level);

  /**
   * Delete all escalation events for a ticket (cascade on ticket deletion).
   *
   * @param ticket the ticket whose escalation events should be deleted
   */
  void deleteAllByTicket(MaintenanceTicket ticket);
}
