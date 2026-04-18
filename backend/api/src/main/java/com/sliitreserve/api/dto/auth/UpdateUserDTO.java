package com.sliitreserve.api.dto.auth;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for updating user details by admin.
 * Allows admin to edit user profile information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserDTO {

  /** User's display name */
  private String displayName;

  /** User's email address */
  @Email(message = "Email must be valid")
  private String email;

  /** Suspension end timestamp (nullable - set to null to unsuspend) */
  private LocalDateTime suspendedUntil;

  /** Number of no-shows (used for tracking attendance violations) */
  private Integer noShowCount;
}
