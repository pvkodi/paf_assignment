package com.sliitreserve.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sliitreserve.api.entities.auth.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * DTO for OTP verification and auto-registration response
 * Contains the newly created user information after successful registration
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpRegistrationResponseDTO {

  @JsonProperty("userId")
  private UUID userId;

  @JsonProperty("email")
  private String email;

  @JsonProperty("displayName")
  private String displayName;

  @JsonProperty("roles")
  private Set<Role> roles;

  @JsonProperty("message")
  private String message; // "Registration completed successfully. You can now login."

  @JsonProperty("createdAt")
  private LocalDateTime createdAt;

  @JsonProperty("status")
  private String status; // "REGISTERED"
}
