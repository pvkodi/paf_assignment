package com.sliitreserve.api.services.auth;

import com.sliitreserve.api.entities.auth.OtpVerification;
import com.sliitreserve.api.repositories.auth.OtpVerificationRepository;
import com.sliitreserve.api.observers.EventPublisher;
import com.sliitreserve.api.observers.EventEnvelope;
import com.sliitreserve.api.observers.EventSeverity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Service for handling OTP (One-Time Password) generation, sending, and verification
 * Used for email-based user registration without admin approval
 */
@Service
@Slf4j
public class OtpService {

  @Autowired
  private OtpVerificationRepository otpVerificationRepository;

  @Autowired
  private EventPublisher eventPublisher;

  // OTP expiration time in minutes (configurable)
  @Value("${otp.expiration-minutes:10}")
  private int otpExpirationMinutes;

  // Maximum number of OTP verification attempts allowed
  @Value("${otp.max-attempts:5}")
  private int maxAttempts;

  // Allowed domain for registration
  private static final String ALLOWED_DOMAIN = "@smartcampus.edu";

  /**
   * Generate a random 6-digit OTP code
   */
  public String generateOtpCode() {
    Random random = new Random();
    int code = 100000 + random.nextInt(900000); // Generates 6-digit number
    return String.format("%06d", code);
  }

  /**
   * Send OTP to user's email
   * Validates that email is from smartcampus.edu domain
   * Deletes any existing pending OTP for this email and creates a new one
   */
  @Transactional
  public OtpVerification sendOtpToEmail(String email) {
    log.info("Requesting OTP for email: {}", email);

    // Validate email domain
    if (!email.toLowerCase().endsWith(ALLOWED_DOMAIN)) {
      log.warn("OTP request rejected: invalid domain for email: {}", email);
      throw new IllegalArgumentException("Only " + ALLOWED_DOMAIN + " email addresses are allowed");
    }

    // Normalize email to lowercase for consistency
    String normalizedEmail = email.toLowerCase();

    // Delete any existing pending OTP for this email
    Optional<OtpVerification> existingOtp = otpVerificationRepository.findLatestByEmail(normalizedEmail);
    if (existingOtp.isPresent() && existingOtp.get().getStatus() == OtpVerification.OtpStatus.PENDING) {
      log.debug("Deleting existing pending OTP for email: {}", normalizedEmail);
      otpVerificationRepository.delete(existingOtp.get());
    }

    // Generate new OTP code
    String otpCode = generateOtpCode();
    LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpExpirationMinutes);

    // Create and save OTP verification record
    OtpVerification otpVerification = OtpVerification.builder()
        .email(normalizedEmail)
        .code(otpCode)
        .status(OtpVerification.OtpStatus.PENDING)
        .expiresAt(expiresAt)
        .attempts(0)
        .build();

    otpVerification = otpVerificationRepository.save(otpVerification);
    log.info("OTP created for email: {} (expires in {} minutes)", normalizedEmail, otpExpirationMinutes);

    // Publish OTP_SENT event (triggers email via OtpEmailObserver)
    eventPublisher.publish(
        EventEnvelope.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("OTP_SENT")
            .severity(EventSeverity.HIGH)
            .title("Your OTP Code for VenueLink Registration")
            .description("Use this OTP to complete your registration")
            .source("OtpService")
            .occurrenceTime(ZonedDateTime.now())
            .metadata(Map.of(
                "email", normalizedEmail,
                "otpCode", otpCode,
                "otpId", otpVerification.getId().toString()))
            .build());

    return otpVerification;
  }

  /**
   * Verify OTP code entered by user
   * Checks:
   * - OTP exists for the email
   * - OTP code matches
   * - OTP has not expired
   * - OTP has not been verified yet
   * - User hasn't exceeded max attempts
   */
  @Transactional
  public OtpVerification verifyOtp(String email, String code) {
    log.info("Verifying OTP for email: {}", email);

    String normalizedEmail = email.toLowerCase();

    // Find OTP record
    Optional<OtpVerification> otpOpt = otpVerificationRepository.findByEmailAndCode(normalizedEmail, code);
    if (otpOpt.isEmpty()) {
      log.warn("OTP verification failed: no matching OTP found for email: {} and code: {}", normalizedEmail, code);
      throw new IllegalArgumentException("Invalid OTP code");
    }

    OtpVerification otp = otpOpt.get();

    // Check if OTP has already been verified
    if (otp.getStatus() == OtpVerification.OtpStatus.VERIFIED) {
      log.warn("OTP verification failed: OTP already verified for email: {}", normalizedEmail);
      throw new IllegalArgumentException("OTP has already been used");
    }

    // Check if OTP has expired
    if (otp.isExpired()) {
      log.warn("OTP verification failed: OTP expired for email: {}", normalizedEmail);
      throw new IllegalArgumentException("OTP has expired. Please request a new one.");
    }

    // Check if max attempts exceeded
    if (otp.getAttempts() >= maxAttempts) {
      log.warn("OTP verification failed: max attempts exceeded for email: {}", normalizedEmail);
      throw new IllegalArgumentException("Maximum verification attempts exceeded. Please request a new OTP.");
    }

    // Mark OTP as verified
    otp.markAsVerified();
    otp = otpVerificationRepository.save(otp);

    log.info("OTP verified successfully for email: {}", normalizedEmail);
    return otp;
  }

  /**
   * Record failed OTP verification attempt
   * Used to track and limit brute force attempts
   */
  @Transactional
  public void recordFailedAttempt(String email, String code) {
    log.debug("Recording failed OTP verification attempt for email: {}", email);

    String normalizedEmail = email.toLowerCase();

    Optional<OtpVerification> otpOpt = otpVerificationRepository.findByEmailAndCode(normalizedEmail, code);
    if (otpOpt.isPresent()) {
      OtpVerification otp = otpOpt.get();
      otp.incrementAttempts();
      otpVerificationRepository.save(otp);
      log.debug("Failed attempt recorded (attempts: {} / {})", otp.getAttempts(), maxAttempts);
    }
  }

  /**
   * Check if email has a pending OTP
   */
  public boolean hasPendingOtp(String email) {
    String normalizedEmail = email.toLowerCase();
    return otpVerificationRepository.hasPendingOtpForEmail(normalizedEmail);
  }

  /**
   * Get latest pending OTP for email
   */
  public Optional<OtpVerification> getPendingOtpForEmail(String email) {
    String normalizedEmail = email.toLowerCase();
    Optional<OtpVerification> otpOpt = otpVerificationRepository.findLatestByEmail(normalizedEmail);
    if (otpOpt.isPresent() && otpOpt.get().getStatus() == OtpVerification.OtpStatus.PENDING) {
      return otpOpt;
    }
    return Optional.empty();
  }

  /**
   * Clean up expired OTPs (should be called periodically via scheduled task)
   */
  @Transactional
  public void cleanupExpiredOtps() {
    log.info("Starting cleanup of expired OTPs");
    otpVerificationRepository.deleteExpiredOtps(LocalDateTime.now());
    log.info("Cleanup of expired OTPs completed");
  }

  /**
   * Validate that email is from allowed domain
   */
  public boolean isValidDomain(String email) {
    return email.toLowerCase().endsWith(ALLOWED_DOMAIN);
  }
}
