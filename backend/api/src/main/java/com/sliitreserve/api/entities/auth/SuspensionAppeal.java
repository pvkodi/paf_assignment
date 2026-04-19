package com.sliitreserve.api.entities.auth;

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

/**
 * SuspensionAppeal entity representing a user's request to appeal their suspension.
 *
 * <p><b>Purpose</b>: Store user appeals against suspension status. Users can submit an appeal
 * when suspended (FR-023). Admin reviews and approves (lifts suspension + resets no-show count)
 * or rejects (suspension continues). Appeals track submission, review status, and decision.
 *
 * <p><b>Key Fields</b> (from data-model):
 * <ul>
 *   <li><b>id</b>: UUID primary key
 *   <li><b>userId</b>: User submitting the appeal (foreign key to User)
 *   <li><b>reason</b>: Appeal reason/justification (user-provided text)
 *   <li><b>status</b>: SUBMITTED, APPROVED, REJECTED (enum)
 *   <li><b>reviewedByUserId</b>: Admin who reviewed the appeal (nullable; null if SUBMITTED)
 *   <li><b>reviewedAt</b>: Timestamp of review decision (nullable; null if SUBMITTED)
 *   <li><b>decision</b>: Optional admin decision reason (nullable)
 *   <li><b>createdAt, updatedAt</b>: Audit timestamps
 * </ul>
 *
 * <p><b>Lifecycle</b>:
 * <ul>
 *   <li><b>SUBMITTED</b>: User creates appeal. reviewedByUserId and reviewedAt are null.
 *   <li><b>APPROVED</b>: Admin approves. User's suspension is lifted and noShowCount is reset.
 *   <li><b>REJECTED</b>: Admin rejects. User's suspension remains.
 * </ul>
 *
 * <p><b>Integration Points</b>:
 * <ul>
 *   <li>AppealService: Submit, review, approve, reject appeals
 *   <li>SuspensionPolicyService: Called by AppealService to release suspension on approval
 *   <li>AppealController: REST endpoints for submission and review
 *   <li>EventPublisher: Emits APPEAL_SUBMITTED, APPEAL_APPROVED, APPEAL_REJECTED events
 *   <li>NotificationService: Listens for appeal events and notifies users/admins
 * </ul>
 *
 * @see User for the user submitting the appeal
 * @see SuspensionAppealStatus for status enumeration
 */
@Entity
@Table(
    name = "suspension_appeal",
    indexes = {
      @Index(name = "idx_appeal_user", columnList = "user_id"),
      @Index(name = "idx_appeal_status", columnList = "status"),
      @Index(name = "idx_appeal_created_at", columnList = "created_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuspensionAppeal {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull(message = "User is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @NotBlank(message = "Appeal reason is required")
  @Column(nullable = false, length = 1000)
  private String reason;

  @NotNull(message = "Status is required")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  @Builder.Default
  private SuspensionAppealStatus status = SuspensionAppealStatus.SUBMITTED;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewed_by_user_id")
  private User reviewedByUser;

  @Column(name = "reviewed_at")
  private LocalDateTime reviewedAt;

  @Column(name = "decision", length = 500)
  private String decision;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /**
   * Check if this appeal is pending review.
   *
   * @return true if status is SUBMITTED
   */
  public boolean isPending() {
    return status == SuspensionAppealStatus.SUBMITTED;
  }

  /**
   * Check if this appeal has been reviewed.
   *
   * @return true if status is APPROVED or REJECTED
   */
  public boolean isReviewed() {
    return status != SuspensionAppealStatus.SUBMITTED;
  }

  /**
   * Check if this appeal was approved.
   *
   * @return true if status is APPROVED
   */
  public boolean isApproved() {
    return status == SuspensionAppealStatus.APPROVED;
  }

  /**
   * Check if this appeal was rejected.
   *
   * @return true if status is REJECTED
   */
  public boolean isRejected() {
    return status == SuspensionAppealStatus.REJECTED;
  }
}
