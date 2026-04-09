package com.sliitreserve.api.entities.ticket;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.auth.User;

/**
 * MaintenanceTicket entity representing a facility maintenance issue.
 *
 * <p><b>Purpose</b>: Store maintenance requests with category, priority, status lifecycle,
 * assignment, SLA deadlines, escalation level, and timestamps. Supports comment and attachment
 * threads (FR-024 to FR-033).
 *
 * <p><b>Key Fields</b> (from data-model):
 * <ul>
 *   <li><b>id</b>: UUID primary key
 *   <li><b>facility</b>: Many-to-one reference to Facility
 *   <li><b>createdBy</b>: User who reported the issue
 *   <li><b>assignedTechnician</b>: Assigned staff member (nullable; assigned at triage)
 *   <li><b>category</b>: TicketCategory enum (ELECTRICAL, PLUMBING, HVAC, IT_NETWORKING,
 *       STRUCTURAL, CLEANING, SAFETY, OTHER)
 *   <li><b>priority</b>: TicketPriority enum (LOW=72h, MEDIUM=24h, HIGH=8h, CRITICAL=4h SLA)
 *   <li><b>status</b>: TicketStatus enum (OPEN → IN_PROGRESS → RESOLVED → CLOSED or REJECTED)
 *   <li><b>title</b>: Short issue summary
 *   <li><b>description</b>: Detailed issue description
 *   <li><b>slaDueAt</b>: SLA deadline (24x7 elapsed from ticket creation in campus timezone)
 *   <li><b>escalationLevel</b>: Current escalation level (0 = open, 1-4 for LEVEL_1-LEVEL_4)
 *   <li><b>createdAt, updatedAt, resolvedAt, closedAt</b>: Audit timestamps
 * </ul>
 *
 * <p><b>SLA and Escalation</b> (FR-032, FR-033):
 * <ul>
 *   <li>SLA is calculated in 24x7 elapsed time (no night/weekend/holiday exemptions).
 *   <li>When SLA breaches, escalation handler triggers reassignment and notifications.
 *   <li>Escalation level increments as higher thresholds are breached (LEVEL_1 → LEVEL_2 →
 *       LEVEL_3 → LEVEL_4).
 *   <li>Each escalation sends HIGH or STANDARD severity notifications per FR-034, FR-035.
 * </ul>
 *
 * <p><b>Comment and Attachment Threads</b> (FR-026, FR-030, FR-031):
 * <ul>
 *   <li>Up to 3 image attachments per ticket (JPEG, PNG, GIF, WebP up to 5MB each).
 *   <li>Comments can be public or internal (visibility = PUBLIC or INTERNAL).
 *   <li>Internal comments visible to staff only (TECHNICIAN, FACILITY_MANAGER, ADMIN roles).
 *   <li>Authors can edit/delete own comments; admins can delete any comment.
 * </ul>
 *
 * @see TicketCategory for category enumeration
 * @see TicketPriority for priority enumeration
 * @see TicketStatus for status enumeration
 * @see Facility for facility reference
 * @see User for creator and technician references
 */
@Entity
@Table(
    name = "maintenance_ticket",
    indexes = {
      @Index(name = "idx_ticket_facility", columnList = "facility_id"),
      @Index(name = "idx_ticket_created_by", columnList = "created_by_user_id"),
      @Index(name = "idx_ticket_assigned_technician", columnList = "assigned_technician_user_id"),
      @Index(name = "idx_ticket_status", columnList = "status"),
      @Index(name = "idx_ticket_priority", columnList = "priority"),
      @Index(name = "idx_ticket_sla_due", columnList = "sla_due_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceTicket {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull(message = "Facility is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "facility_id", nullable = false)
  private Facility facility;

  @NotNull(message = "Created by user is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_user_id", nullable = false)
  private User createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_technician_user_id")
  private User assignedTechnician;

  @NotNull(message = "Category is required")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private TicketCategory category;

  @NotNull(message = "Priority is required")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private TicketPriority priority;

  @NotNull(message = "Status is required")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private TicketStatus status = TicketStatus.OPEN;

  @NotBlank(message = "Title is required")
  @Column(nullable = false, length = 255)
  private String title;

  @NotBlank(message = "Description is required")
  @Column(nullable = false, columnDefinition = "TEXT")
  private String description;

  @NotNull(message = "SLA due time is required")
  @Column(name = "sla_due_at", nullable = false)
  private LocalDateTime slaDueAt;

  @Column(name = "escalation_level", nullable = false)
  private Integer escalationLevel = 0;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;

  @Column(name = "closed_at")
  private LocalDateTime closedAt;

  /**
   * Check if the ticket is in a terminal status (CLOSED or REJECTED).
   *
   * @return true if terminal
   */
  public boolean isTerminal() {
    return status == TicketStatus.CLOSED || status == TicketStatus.REJECTED;
  }

  /**
   * Check if SLA has been breached at the current time.
   *
   * @return true if current time exceeds slaDueAt
   */
  public boolean isSlaBreached() {
    return LocalDateTime.now().isAfter(slaDueAt);
  }

  /**
   * Get the SLA threshold in milliseconds for this ticket's priority.
   *
   * @return milliseconds (4h for CRITICAL, 8h for HIGH, 24h for MEDIUM, 72h for LOW)
   */
  public long getSlaThresholdMs() {
    return switch (priority) {
      case CRITICAL -> 4 * 60 * 60 * 1000L; // 4 hours
      case HIGH -> 8 * 60 * 60 * 1000L; // 8 hours
      case MEDIUM -> 24 * 60 * 60 * 1000L; // 24 hours
      case LOW -> 72 * 60 * 60 * 1000L; // 72 hours
    };
  }
}
