package com.sliitreserve.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for admin appeal decision (approve/reject).
 *
 * <p><b>Purpose</b>: Captures admin's approval/rejection decision with optional reason.
 * This is the request body for POST /api/v1/appeals/{id}/approve and /reject.
 *
 * <p><b>Fields</b>:
 * <ul>
 *   <li><b>approved</b>: Boolean indicating if appeal is approved (true) or rejected (false)
 *   <li><b>decision</b>: Optional admin reason/notes (nullable)
 * </ul>
 *
 * <p><b>Validation</b>:
 * <ul>
 *   <li>approved: Must not be null
 *   <li>decision: Optional, max 500 characters (validated on entity)
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppealDecisionRequest {

  /**
   * Whether the appeal is approved (true) or rejected (false).
   *
   * <p>Approved: User's suspension is lifted and noShowCount is reset.
   * Rejected: User's suspension continues.
   */
  @JsonProperty("approved")
  @NotNull(message = "Approval decision is required")
  private Boolean approved;

  /**
   * Optional admin reason for the decision.
   *
   * <p>Max length: 500 characters (validated on SuspensionAppeal entity).
   * Visible to the user and stored in appeal record.
   */
  @JsonProperty("decision")
  private String decision;
}
