package com.sliitreserve.api.entities.booking;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.auth.Role;

/**
 * ApprovalStep entity representing a single step in the booking approval workflow.
 *
 * <p><b>Purpose</b>: Record approval decisions within the approval chain. Each step tracks the
 * approver role, decision, and decision timestamp for audit and process visibility.
 *
 * <p><b>Key Fields</b> (from data-model):
 * <ul>
 *   <li><b>id</b>: UUID primary key
 *   <li><b>booking</b>: Many-to-one reference to Booking
 *   <li><b>stepOrder</b>: Sequence number in the approval chain (1, 2, 3, ...)
 *   <li><b>approverRole</b>: Role required for this step (LECTURER, FACILITY_MANAGER, ADMIN)
 *   <li><b>decision</b>: ApprovalStepDecision enum (PENDING, APPROVED, REJECTED)
 *   <li><b>decidedBy</b>: User who made the decision (nullable if still pending)
 *   <li><b>decidedAt</b>: Decision timestamp (nullable if pending)
 *   <li><b>note</b>: Optional approval note or rejection reason
 *   <li><b>createdAt</b>: Step creation timestamp
 * </ul>
 *
 * <p><b>Typical Approval Workflow</b>:
 * <ul>
 *   <li><b>USER booking</b>: stepOrder=1 (LECTURER), stepOrder=2 (ADMIN)
 *   <li><b>LECTURER booking</b>: stepOrder=1 (auto-approve LECTURER) or stepOrder=1
 *       (FACILITY_MANAGER if high-capacity)
 *   <li><b>High-capacity facility</b>: Additional step with FACILITY_MANAGER role
 *   <li><b>ADMIN booking</b>: Bypasses all approval steps
 * </ul>
 *
 * @see Booking for the booking reference
 * @see ApprovalStepDecision for decision enumeration
 * @see User for the decision maker
 * @see Role for the approver role
 */
@Entity
@Table(
    name = "approval_step",
    indexes = {
      @Index(name = "idx_approval_step_booking", columnList = "booking_id"),
      @Index(name = "idx_approval_step_order", columnList = "booking_id, step_order")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalStep {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull(message = "Booking is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booking_id", nullable = false)
  private Booking booking;

  @Min(value = 1, message = "Step order must be at least 1")
  @Column(name = "step_order", nullable = false)
  private Integer stepOrder;

  @NotNull(message = "Approver role is required")
  @Enumerated(EnumType.STRING)
  @Column(name = "approver_role", nullable = false, length = 50)
  private Role approverRole;

  @NotNull(message = "Decision is required")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private ApprovalStepDecision decision = ApprovalStepDecision.PENDING;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "decided_by_user_id")
  private User decidedBy;

  @Column(name = "decided_at")
  private LocalDateTime decidedAt;

  @Column(length = 500)
  private String note;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * Check if this approval step has been decided (APPROVED or REJECTED).
   *
   * @return true if decision is terminal
   */
  public boolean isDecided() {
    return decision != ApprovalStepDecision.PENDING;
  }

  /**
   * Check if this approval step was approved.
   *
   * @return true if decision is APPROVED
   */
  public boolean isApproved() {
    return decision == ApprovalStepDecision.APPROVED;
  }

  /**
   * Check if this approval step was rejected.
   *
   * @return true if decision is REJECTED
   */
  public boolean isRejected() {
    return decision == ApprovalStepDecision.REJECTED;
  }
}
