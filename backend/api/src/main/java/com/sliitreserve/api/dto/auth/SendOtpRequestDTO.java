package com.sliitreserve.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for sending OTP to email
 * Step 1 of OTP registration flow
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpRequestDTO {

  @Email(message = "Email must be valid")
  @NotBlank(message = "Email is required")
  @JsonProperty("email")
  private String email;

  // Email domain must be @smartcampus.edu - validated by backend
}
