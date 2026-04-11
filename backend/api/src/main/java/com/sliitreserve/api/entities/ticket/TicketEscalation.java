package com.sliitreserve.api.entities.ticket;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sliitreserve.api.entities.auth.User;

/**
 * TicketEscalation entity representing an escalation event in a maintenance ticket's lifecycle.
 *
 * <p><b>Purpose</b>: Provide an immutable audit trail of escalation events triggered by SLA
 * breaches or manual intervention. Each escalation records the escalation level change, who
 * triggered it, which technician it was assigned to before and after, and the reason
 * (FR-032, FR-033).
 *
 * <p><b>Key Fields</b> (from user request):
 * <ul>
 *   <li><b>id</b>: UUID primary key
 *   <li><b>ticket</b>: Many-to-one reference to MaintenanceTicket
 *   <li><b>fromLevel</b>: Previous escalation level (0-3)
 *   <li><b>toLevel</b>: New escalation level (1-3, must be > fromLevel)
 *   <li><b>escalatedBy</b>: User who triggered escalation (admin/system)
 *   <li><b>escalatedAt</b>: Timestamp of escalation (creation time)
 *   <li><b>escalationReason</b>: Reason for escalation (SLA breach, unresolved, etc.)
 *   <li><b>previousAssignee</b>: User assigned before escalation (nullable)
 *   <li><b>newAssignee</b>: User assigned after escalation (nullable)
 *   <li><b>notes</b>: Optional escalation notes (nullable)
 * </ul>
 *
 * <p><b>SLA and Escalation Logic</b> (FR-032, FR-033):
 * <ul>
 *   <li>CRITICAL priority: 4-hour SLA → escalates to LEVEL_1
 *   <li>HIGH priority: 8-hour SLA → escalates to LEVEL_2
 *   <li>MEDIUM priority: 24-hour SLA → escalates to LEVEL_3
 *   <li>LOW priority: 72-hour SLA → no escalation (max LEVEL_4, no auto-escalation)
 *   <li>Each breach is recorded as a separate escalation event with reason (immutable).
 * </ul>
 *
 * <p><b>Relationships</b>:
 * <ul>
 *   <li>Many-to-one MaintenanceTicket (inverse of Ticket.escalationHistory)
 *   <li>Many-to-one User (escalatedBy, previousAssignee, newAssignee)
 * </ul>
 *
 * @see MaintenanceTicket
 * @see User
 * @see TicketPriority for SLA thresholds
 */
@Entity
@Table(
    name = "ticket_escalation",
    indexes = {
      @Index(name = "idx_escalation_ticket", columnList = "ticket_id"),
      @Index(name = "idx_escalation_escalated_by", columnList = "escalated_by_user_id"),
      @Index(name = "idx_escalation_escalated_at", columnList = "escalated_at"),
      @Index(name = "idx_escalation_to_level", columnList = "to_level")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketEscalation {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull(message = "Ticket is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticket_id", nullable = false)
  private MaintenanceTicket ticket;

  @NotNull(message = "From level is required")
  @Min(value = 0, message = "From level must be >= 0")
  @Column(name = "from_level", nullable = false)
  private Integer fromLevel;

  @NotNull(message = "To level is required")
  @Min(value = 0, message = "To level must be >= 0")
  @Column(name = "to_level", nullable = false)
  private Integer toLevel;

  @NotNull(message = "Escalated by user is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "escalated_by_user_id", nullable = false)
  private User escalatedBy;

  @CreationTimestamp
  @Column(name = "escalated_at", nullable = false, updatable = false)
  private LocalDateTime escalatedAt;

  @NotBlank(message = "Escalation reason is required")
  @Column(name = "escalation_reason", nullable = false, length = 255)
  private String escalationReason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "previous_assignee_user_id")
  private User previousAssignee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "new_assignee_user_id")
  private User newAssignee;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  /**
   * Validate that toLevel > fromLevel.
   *
   * @return true if toLevel is greater than fromLevel
   */
  public boolean isValidEscalation() {
    return toLevel > fromLevel;
  }
}
