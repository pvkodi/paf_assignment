package com.sliitreserve.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO for user information (no password hash).
 * Used for displaying user details in admin and user-facing screens.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

  private UUID id;
  private String email;
  private String displayName;
  private Set<String> roles; // Role names as strings
  private LocalDateTime suspendedUntil;
  private Integer noShowCount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  /**
   * Convert User entity to DTO.
   *
   * @param user User entity
   * @return UserDTO
   */
  public static UserDTO fromEntity(com.sliitreserve.api.entities.auth.User user) {
    if (user == null) {
      return null;
    }

    return UserDTO.builder()
        .id(user.getId())
        .email(user.getEmail())
        .displayName(user.getDisplayName())
        .roles(
            user.getRoles() != null
                ? user.getRoles().stream()
                    .map(role -> role.name())
                    .collect(Collectors.toSet())
                : Set.of())
        .suspendedUntil(user.getSuspendedUntil())
        .noShowCount(user.getNoShowCount())
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .build();
  }
}
