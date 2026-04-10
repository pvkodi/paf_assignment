package com.sliitreserve.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for submitting a suspension appeal.
 *
 * <p><b>Purpose</b>: Captures user's appeal request with justification text.
 * This is the request body for POST /api/v1/appeals.
 *
 * <p><b>Fields</b>:
 * <ul>
 *   <li><b>reason</b>: User's explanation for the appeal (required, non-blank)
 * </ul>
 *
 * <p><b>Validation</b>:
 * <ul>
 *   <li>reason: Must not be blank; max 1000 characters (validated on entity)
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppealRequest {

  /**
   * User's appeal reason/justification.
   *
   * <p>Max length: 1000 characters (validated on SuspensionAppeal entity).
   * User should provide clear explanation for why suspension should be lifted.
   */
  @JsonProperty("reason")
  @NotBlank(message = "Appeal reason is required")
  private String reason;
}
