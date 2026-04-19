package com.sliitreserve.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sliitreserve.api.dto.BaseResponseDTO;
import com.sliitreserve.api.entities.auth.SuspensionAppealStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for suspension appeal response.
 *
 * <p><b>Purpose</b>: Represents appeal details in API responses for viewing,
 * listing, and reviewing appeals.
 *
 * <p><b>Fields</b>:
 * <ul>
 *   <li><b>id</b>: Appeal UUID (inherited from BaseResponseDTO)
 *   <li><b>userId</b>: User who submitted the appeal
 *   <li><b>userEmail</b>: Email of user (for admin convenience)
 *   <li><b>reason</b>: Appeal justification
 *   <li><b>status</b>: SUBMITTED, APPROVED, REJECTED
 *   <li><b>reviewedByUserEmail</b>: Email of admin who reviewed (null if pending)
 *   <li><b>reviewedAt</b>: Timestamp of review decision (null if pending)
 *   <li><b>decision</b>: Admin's decision reason (null if pending)
 *   <li><b>createdAt, updatedAt</b>: Audit timestamps (inherited from BaseResponseDTO)
 * </ul>
 *
 * <p><b>Usage Scenarios</b>:
 * <ul>
 *   <li>POST /appeal response: Return submitted appeal
 *   <li>GET /appeals/{id}: Return appeal details
 *   <li>GET /appeals: Return list of appeals (filtered by role/user)
 *   <li>POST /appeals/{id}/approve: Return approved appeal
 *   <li>POST /appeals/{id}/reject: Return rejected appeal
 * </ul>
 *
 * @see AppealRequest for the request DTO
 * @see com.sliitreserve.api.entities.auth.SuspensionAppeal for the entity
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppealResponse extends BaseResponseDTO {

  private static final long serialVersionUID = 1L;

  /**
   * UUID of the user who submitted the appeal.
   */
  @JsonProperty("user_id")
  private UUID userId;

  /**
   * Email of the user who submitted the appeal.
   *
   * <p>Provided for admin convenience when reviewing appeals.
   */
  @JsonProperty("user_email")
  private String userEmail;

  /**
   * Appeal reason/justification provided by the user.
   */
  @JsonProperty("reason")
  private String reason;

  /**
   * Current status of the appeal.
   *
   * <p>Values:
   * <ul>
   *   <li>SUBMITTED: Awaiting admin review
   *   <li>APPROVED: Admin approved; suspension lifted
   *   <li>REJECTED: Admin rejected; suspension remains
   * </ul>
   */
  @JsonProperty("status")
  private SuspensionAppealStatus status;

  /**
   * Email of the admin user who reviewed this appeal.
   *
   * <p>Null if status is SUBMITTED (not yet reviewed).
   */
  @JsonProperty("reviewed_by_user_email")
  private String reviewedByUserEmail;

  /**
   * Timestamp when the admin reviewed this appeal.
   *
   * <p>Null if status is SUBMITTED (not yet reviewed).
   * Set when appeal is approved or rejected.
   */
  @JsonProperty("reviewed_at")
  private LocalDateTime reviewedAt;

  /**
   * Admin's decision reason/notes.
   *
   * <p>Null if status is SUBMITTED (not yet reviewed).
   * Can contain explanation for approval or rejection.
   */
  @JsonProperty("decision")
  private String decision;

  /**
   * Constructor for creating AppealResponse from appeal details.
   *
   * @param id Appeal UUID
   * @param userId User ID who submitted
   * @param userEmail User email
   * @param reason Appeal reason
   * @param status Appeal status
   * @param reviewedByUserEmail Admin email (null if pending)
   * @param reviewedAt Review timestamp (null if pending)
   * @param decision Admin decision notes (null if pending)
   * @param createdAt Creation timestamp
   * @param updatedAt Last update timestamp
   */
  public AppealResponse(UUID id, UUID userId, String userEmail, String reason,
                        SuspensionAppealStatus status, String reviewedByUserEmail,
                        LocalDateTime reviewedAt, String decision,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
    super(id, createdAt, updatedAt);
    this.userId = userId;
    this.userEmail = userEmail;
    this.reason = reason;
    this.status = status;
    this.reviewedByUserEmail = reviewedByUserEmail;
    this.reviewedAt = reviewedAt;
    this.decision = decision;
  }
}
