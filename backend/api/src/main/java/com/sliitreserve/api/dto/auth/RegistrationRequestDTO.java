package com.sliitreserve.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sliitreserve.api.entities.auth.RegistrationRequest;
import com.sliitreserve.api.entities.auth.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for viewing registration request details.
 * Used by admin UI to display registration requests with all relevant information.
 * Password hash is NOT included for security.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationRequestDTO {

  @JsonProperty("id")
  private UUID id;

  @JsonProperty("email")
  private String email;

  @JsonProperty("displayName")
  private String displayName;

  @JsonProperty("roleRequested")
  private Role roleRequested;

  @JsonProperty("registrationNumber")
  private String registrationNumber;

  @JsonProperty("employeeNumber")
  private String employeeNumber;

  @JsonProperty("status")
  private RegistrationRequest.RegistrationRequestStatus status;

  @JsonProperty("rejectionReason")
  private String rejectionReason;

  @JsonProperty("reviewedByAdminId")
  private UUID reviewedByAdminId;

  @JsonProperty("createdAt")
  private LocalDateTime createdAt;

  @JsonProperty("reviewedAt")
  private LocalDateTime reviewedAt;

  /**
   * Convert entity to DTO.
   * 
   * @param entity The RegistrationRequest entity
   * @return DTO representation (password hash is NOT included)
   */
  public static RegistrationRequestDTO fromEntity(RegistrationRequest entity) {
    return RegistrationRequestDTO.builder()
        .id(entity.getId())
        .email(entity.getEmail())
        .displayName(entity.getDisplayName())
        .roleRequested(entity.getRoleRequested())
        .registrationNumber(entity.getRegistrationNumber())
        .employeeNumber(entity.getEmployeeNumber())
        .status(entity.getStatus())
        .rejectionReason(entity.getRejectionReason())
        .reviewedByAdminId(entity.getReviewedByAdminId())
        .createdAt(entity.getCreatedAt())
        .reviewedAt(entity.getReviewedAt())
        .build();
  }
}
