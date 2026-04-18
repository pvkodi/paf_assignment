package com.sliitreserve.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for rejecting a registration request.
 * Admin must provide a reason when rejecting (user will receive this reason in email).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RejectRegistrationRequestDTO {

  @NotBlank(message = "Rejection reason is required")
  @JsonProperty("reason")
  private String reason;
}
