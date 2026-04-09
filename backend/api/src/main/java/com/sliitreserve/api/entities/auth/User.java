package com.sliitreserve.api.entities.auth;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;

/**
 * User entity representing a campus platform user.
 *
 * <p><b>Purpose</b>: Store user identity, authentication Subject (OAuth), roles, and suspension
 * state. Supports multi-role assignment and per-user no-show tracking for suspension lifecycle
 * (FR-022: 3 no-shows → 1-week suspension).
 *
 * <p><b>Key Fields</b> (from data-model):
 * <ul>
 *   <li><b>id</b>: UUID primary key
 *   <li><b>googleSubject</b>: Google OAuth 2.0 Subject identifier (unique, immutable)
 *   <li><b>email</b>: Institutional email (unique)
 *   <li><b>displayName</b>: User's display name
 *   <li><b>roles</b>: Set of assigned roles from {USER, LECTURER, TECHNICIAN, FACILITY_MANAGER,
 *       ADMIN}
 *   <li><b>active</b>: Account enabled flag
 *   <li><b>suspendedUntil</b>: Suspension end timestamp (nullable; null = not suspended)
 *   <li><b>noShowCount</b>: Cumulative no-show counter; incremented on check-in failure within
 *       15min grace
 *   <li><b>createdAt, updatedAt</b>: Audit timestamps
 * </ul>
 *
 * <p><b>Suspension Logic</b> (FR-022, FR-023):
 * <ul>
 *   <li>When noShowCount reaches 3, an automatic 1-week suspension is applied.
 *   <li>Suspended users can call appeal endpoint; admin reviews and accepts/rejects.
 *   <li>On appeal approval, suspension is lifted and noShowCount resets.
 * </ul>
 *
 * <p><b>Multi-Role Policy</b> (FR-042): For users assigned multiple roles, quota/peak-hour/
 * advance-window policies use the most permissive applicable role.
 *
 * @see Role for role enumeration
 */
@Entity
@Table(
    name = "\"user\"",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = "google_subject", name = "uk_user_google_subject"),
      @UniqueConstraint(columnNames = "email", name = "uk_user_email")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank(message = "Google Subject is required")
  @Column(name = "google_subject", nullable = false, unique = true, length = 255)
  private String googleSubject;

  @Email(message = "Email must be valid")
  @NotBlank(message = "Email is required")
  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @NotBlank(message = "Display name is required")
  @Column(name = "display_name", nullable = false, length = 255)
  private String displayName;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 50)
  @NotNull(message = "User must have at least one role")
  private Set<Role> roles = new HashSet<>();

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "suspended_until")
  private LocalDateTime suspendedUntil;

  @Column(name = "no_show_count", nullable = false)
  private Integer noShowCount = 0;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /**
   * Check if the user is currently suspended.
   *
   * @return true if suspendedUntil is set and in the future
   */
  public boolean isSuspended() {
    return suspendedUntil != null && suspendedUntil.isAfter(LocalDateTime.now());
  }

  /**
   * Get the suspension reason or null if not suspended.
   *
   * @return suspension end time, or null if not suspended
   */
  public LocalDateTime getSuspensionEndTime() {
    return isSuspended() ? suspendedUntil : null;
  }

  /**
   * Check if user has a specific role.
   *
   * @param role the role to check
   * @return true if the user has this role
   */
  public boolean hasRole(Role role) {
    return roles != null && roles.contains(role);
  }

  /**
   * Check if user has any of the given roles.
   *
   * @param requiredRoles the roles to check against
   * @return true if the user has at least one of the required roles
   */
  public boolean hasAnyRole(Role... requiredRoles) {
    if (roles == null || roles.isEmpty()) {
      return false;
    }
    for (Role role : requiredRoles) {
      if (roles.contains(role)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Increment the no-show counter by 1.
   */
  public void incrementNoShowCount() {
    this.noShowCount = (this.noShowCount == null ? 0 : this.noShowCount) + 1;
  }

  /**
   * Reset the no-show counter to 0 (used on appeal approval).
   */
  public void resetNoShowCount() {
    this.noShowCount = 0;
  }
}
