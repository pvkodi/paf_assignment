package com.sliitreserve.api.repositories.ticket;

import com.sliitreserve.api.entities.ticket.MaintenanceTicket;
import com.sliitreserve.api.entities.ticket.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MaintenanceTicket entity.
 *
 * <p>Provides data access methods for ticket queries, including SLA deadline checks,
 * escalation status queries, and facility-based ticket retrieval.
 *
 * <p><b>Key Query Methods</b>:
 * <ul>
 *   <li>{@link #findByFacilityId(UUID)} - Get all tickets for a facility
 *   <li>{@link #findByStatusNotInAndSlaDueAtBefore(List, LocalDateTime)} - Find breached, non-terminal tickets
 *   <li>{@link #findByEscalationLevelAndSlaDueAtBefore(Integer, LocalDateTime)} - Find breached tickets by escalation level
 *   <li>{@link #findByStatusAndEscalationLevel(TicketStatus, Integer)} - Find tickets by status and escalation level
 * </ul>
 *
 * <p><b>Usage</b>:
 * <pre>
 * // Find all breached tickets ready for escalation
 * List&lt;MaintenanceTicket&gt; breached = ticketRepository
 *     .findByStatusNotInAndSlaDueAtBefore(
 *         List.of(TicketStatus.CLOSED, TicketStatus.REJECTED),
 *         LocalDateTime.now()
 *     );
 *
 * // Find specific escalation level breached tickets
 * List&lt;MaintenanceTicket&gt; level1 = ticketRepository
 *     .findByEscalationLevelAndSlaDueAtBefore(1, LocalDateTime.now());
 * </pre>
 *
 * @see MaintenanceTicket for entity definition
 * @see TicketStatus for status enumeration
 */
@Repository
public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, UUID> {

  /**
   * Find all tickets for a specific facility.
   *
   * @param facilityId the facility ID
   * @return list of tickets for that facility
   */
  List<MaintenanceTicket> findByFacilityId(UUID facilityId);

  /**
   * Find all non-terminal tickets with SLA deadlines that have passed.
   *
   * Used for scheduled SLA breach detection and automatic escalation processing.
   *
   * @param statusExcludeList terminal statuses to exclude (CLOSED, REJECTED)
   * @param slaDueAt current time threshold
   * @return list of breached, non-terminal tickets
   */
  List<MaintenanceTicket> findByStatusNotInAndSlaDueAtBefore(
      List<TicketStatus> statusExcludeList, LocalDateTime slaDueAt);

  /**
   * Find tickets at a specific escalation level with breached SLA deadlines.
   *
   * Used for processing escalation actions at a specific level.
   *
   * @param escalationLevel the escalation level to filter by (1, 2, 3)
   * @param slaDueAt current time threshold
   * @return list of breached tickets at that level
   */
  List<MaintenanceTicket> findByEscalationLevelAndSlaDueAtBefore(
      Integer escalationLevel, LocalDateTime slaDueAt);

  /**
   * Find tickets by status and escalation level.
   *
   * Used for workflow queries and escalation tracking.
   *
   * @param status the ticket status
   * @param escalationLevel the escalation level
   * @return list of matching tickets
   */
  List<MaintenanceTicket> findByStatusAndEscalationLevel(
      TicketStatus status, Integer escalationLevel);

  /**
   * Count active tickets assigned to a specific technician.
   *
   * Active tickets are those NOT in terminal states (CLOSED, REJECTED).
   * Used for load-based assignment during automatic escalation.
   *
   * @param technicianId the technician's user ID
   * @return number of active tickets assigned to this technician
   */
  @Query("SELECT COUNT(t) FROM MaintenanceTicket t " +
      "WHERE t.assignedTechnician.id = :technicianId " +
      "AND t.status NOT IN ('CLOSED', 'REJECTED')")
  long countActiveTicketsByTechnician(UUID technicianId);
}
