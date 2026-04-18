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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RegistrationRequest entity representing a pending user registration.
 *
 * <p><b>Purpose</b>: Store user registration requests awaiting admin approval. Once approved,
 * a User entity is created; if rejected, the request is marked rejected and the user can
 * resubmit a new registration request.
 *
 * <p><b>Status Flow</b>: PENDING → (APPROVED | REJECTED)
 *
 * <p><b>Key Fields</b>:
 * <ul>
 *   <li><b>id</b>: UUID primary key
 *   <li><b>email</b>: User's email (checked for existing registrations)
 *   <li><b>displayName</b>: User's display name
 *   <li><b>passwordHash</b>: Bcrypt-hashed password (stored until approval)
 *   <li><b>roleRequested</b>: Role requested by the user (USER, LECTURER, TECHNICIAN, FACILITY_MANAGER)
 *   <li><b>registrationNumber</b>: Registration number (required if roleRequested = USER)
 *   <li><b>employeeNumber</b>: Employee number (required if roleRequested != USER)
 *   <li><b>status</b>: PENDING, APPROVED, or REJECTED
 *   <li><b>rejectionReason</b>: Reason for rejection (nullable; set only if REJECTED)
 *   <li><b>reviewedByAdminId</b>: UUID of admin who approved/rejected (nullable until reviewed)
 *   <li><b>createdAt</b>: Request submission timestamp
 *   <li><b>reviewedAt</b>: Approval/rejection timestamp (nullable until reviewed)
 * </ul>
 *
 * @see User for the entity created when this request is approved
 * @see Role for the role enumeration
 */
@Entity
@Table(
    name = "registration_request",
    indexes = {
      @Index(name = "idx_registration_request_status", columnList = "status"),
      @Index(name = "idx_registration_request_email_status", columnList = "email, status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Email(message = "Email must be valid")
  @NotBlank(message = "Email is required")
  @Column(nullable = false, length = 255)
  private String email;

  @NotBlank(message = "Display name is required")
  @Column(name = "display_name", nullable = false, length = 255)
  private String displayName;

  @NotBlank(message = "Password hash is required")
  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @NotNull(message = "Role is required")
  @Enumerated(EnumType.STRING)
  @Column(name = "role_requested", nullable = false, length = 50)
  private Role roleRequested;

  @Column(name = "registration_number", length = 50)
  private String registrationNumber;

  @Column(name = "employee_number", length = 50)
  private String employeeNumber;

  @NotNull(message = "Status is required")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  @Builder.Default
  private RegistrationRequestStatus status = RegistrationRequestStatus.PENDING;

  @Column(name = "rejection_reason", columnDefinition = "text")
  private String rejectionReason;

  @Column(name = "reviewed_by_admin_id", columnDefinition = "uuid")
  private UUID reviewedByAdminId;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "reviewed_at")
  private LocalDateTime reviewedAt;

  /**
   * Status enumeration for registration requests.
   */
  public enum RegistrationRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
  }
}
