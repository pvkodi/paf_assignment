package com.sliitreserve.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sliitreserve.api.entities.auth.Role;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for creating a new registration request.
 * Extends the basic registration data with role and credential field validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateRegistrationRequestDTO {

  @Email(message = "Email must be valid")
  @NotBlank(message = "Email is required")
  @JsonProperty("email")
  private String email;

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
  private String registrationNumber;

  @JsonProperty("employeeNumber")
  private String employeeNumber;

  /**
   * Validates that password and confirmPassword match.
   * 
   * @throws IllegalArgumentException if passwords don't match
   */
  public void validatePasswords() {
    if (!this.password.equals(this.confirmPassword)) {
      throw new IllegalArgumentException("Passwords do not match");
    }
  }

  /**
   * Validates that role-specific credential fields are provided.
   * - USER/STUDENT role requires registrationNumber
   * - Other roles require employeeNumber
   * 
   * @throws IllegalArgumentException if required field is missing for the role
   */
  public void validateRoleCredentials() {
    if (roleRequested == Role.USER) {
      if (registrationNumber == null || registrationNumber.isBlank()) {
        throw new IllegalArgumentException(
            "Registration number is required for STUDENT role");
      }
    } else {
      if (employeeNumber == null || employeeNumber.isBlank()) {
        throw new IllegalArgumentException(
            "Employee number is required for " + roleRequested + " role");
      }
    }
  }
}
