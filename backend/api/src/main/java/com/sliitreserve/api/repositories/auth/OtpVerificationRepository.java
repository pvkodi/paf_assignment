package com.sliitreserve.api.repositories.auth;

import com.sliitreserve.api.entities.auth.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {

  /**
   * Find the latest (most recent) OTP for an email address
   */
  @Query("SELECT o FROM OtpVerification o WHERE o.email = :email ORDER BY o.createdAt DESC LIMIT 1")
  Optional<OtpVerification> findLatestByEmail(@Param("email") String email);

  /**
   * Find OTP by email and code
   */
  Optional<OtpVerification> findByEmailAndCode(String email, String code);

  /**
   * Check if there's a pending OTP for this email
   */
  @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM OtpVerification o WHERE o.email = :email AND o.status = 'PENDING'")
  boolean hasPendingOtpForEmail(@Param("email") String email);

  /**
   * Delete expired OTPs (cleanup)
   */
  @Query("DELETE FROM OtpVerification o WHERE o.expiresAt < :now")
  void deleteExpiredOtps(@Param("now") LocalDateTime now);

  /**
   * Find verified OTP by email (for tracking verification history)
   */
  @Query("SELECT o FROM OtpVerification o WHERE o.email = :email AND o.status = 'VERIFIED' ORDER BY o.verifiedAt DESC LIMIT 1")
  Optional<OtpVerification> findLatestVerifiedByEmail(@Param("email") String email);
}
