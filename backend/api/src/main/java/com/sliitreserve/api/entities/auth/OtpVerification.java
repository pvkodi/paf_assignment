package com.sliitreserve.api.entities.auth;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * OtpVerification entity to store OTP codes for email-based user registration.
 * OTPs have an expiration time and can be marked as verified.
 */
@Entity
@Table(
    name = "otp_verification",
    indexes = {
      @Index(name = "idx_otp_email", columnList = "email"),
      @Index(name = "idx_otp_email_code", columnList = "email, code"),
      @Index(name = "idx_otp_status", columnList = "status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerification {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 255)
  private String email;

  @Column(nullable = false, length = 6)
  private String code; // 6-digit OTP

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  @Builder.Default
  private OtpStatus status = OtpStatus.PENDING; // PENDING or VERIFIED

  @Column(nullable = false)
  private LocalDateTime expiresAt; // OTP expiration time

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "verified_at")
  private LocalDateTime verifiedAt; // Timestamp when OTP was successfully verified

  @Column(name = "attempts", nullable = false)
  @Builder.Default
  private Integer attempts = 0; // Track failed verification attempts

  public enum OtpStatus {
    PENDING,  // OTP sent, waiting for verification
    VERIFIED  // OTP verified successfully
  }

  /**
   * Check if OTP has expired
   */
  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiresAt);
  }

  /**
   * Check if OTP is valid (not expired and not verified)
   */
  public boolean isValid() {
    return !isExpired() && status == OtpStatus.PENDING;
  }

  /**
   * Mark OTP as verified
   */
  public void markAsVerified() {
    this.status = OtpStatus.VERIFIED;
    this.verifiedAt = LocalDateTime.now();
  }

  /**
   * Increment failed attempts
   */
  public void incrementAttempts() {
    this.attempts++;
  }
}
