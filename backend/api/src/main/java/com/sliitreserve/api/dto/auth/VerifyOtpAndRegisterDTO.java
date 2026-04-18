package com.sliitreserve.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sliitreserve.api.entities.auth.Role;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for verifying OTP and completing user registration
 * Step 2 of OTP registration flow
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpAndRegisterDTO {

  @Email(message = "Email must be valid")
  @NotBlank(message = "Email is required")
  @JsonProperty("email")
  private String email;

  @NotBlank(message = "OTP code is required")
  @Size(min = 6, max = 6, message = "OTP must be 6 digits")
  @Pattern(regexp = "\\d{6}", message = "OTP must contain only digits")
  @JsonProperty("otp")
  private String otp;

  @NotBlank(message = "Display name is required")
  @Size(min = 2, max = 255, message = "Display name must be between 2 and 255 characters")
  @JsonProperty("displayName")
  private String displayName;

  @NotBlank(message = "Password is required")
  @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
  @JsonProperty("password")
  private String password;

  @NotBlank(message = "Password confirmation is required")
  @JsonProperty("confirmPassword")
  private String confirmPassword;

  @NotNull(message = "Role is required")
  @JsonProperty("roleRequested")
  private Role roleRequested;

  @JsonProperty("registrationNumber")
  private String registrationNumber; // Required if roleRequested = USER (STUDENT)

  @JsonProperty("employeeNumber")
  private String employeeNumber; // Required if roleRequested != USER

  /**
   * Validate that passwords match
   */
  public void validatePasswords() {
    if (!this.password.equals(this.confirmPassword)) {
      throw new IllegalArgumentException("Passwords do not match");
    }
  }

  /**
   * Validate role-specific credentials
   */
  public void validateRoleCredentials() {
    if (roleRequested == Role.USER) {
      if (registrationNumber == null || registrationNumber.isBlank()) {
        throw new IllegalArgumentException("Registration number is required for STUDENT role");
      }
    } else {
      if (employeeNumber == null || employeeNumber.isBlank()) {
        throw new IllegalArgumentException("Employee number is required for " + roleRequested + " role");
      }
    }
  }
}
