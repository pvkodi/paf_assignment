package com.sliitreserve.api.observers.impl;

import com.sliitreserve.api.observers.EventEnvelope;
import com.sliitreserve.api.observers.EventPublisher;
import com.sliitreserve.api.observers.EventSeverity;
import com.sliitreserve.api.observers.Observer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * OTP Email Notification Observer Implementation
 *
 * Purpose: Handles delivery of OTP (One-Time Password) emails for user registration.
 *
 * Differences from EmailObserver:
 * - Handles OTP_SENT events with OTP codes
 * - Does NOT require userId (email-based instead)
 * - Extracts email and OTP code from event metadata
 * - Sends to the email address directly without user lookup
 *
 * Design:
 * - Non-blocking: Marked with @Async to prevent blocking event publisher
 * - Exception-safe: Catches and logs exceptions without re-throwing
 * - Discoverable: Marked with @Component for Spring classpath scanning
 * - Specialized: Only handles OTP_SENT events
 *
 * Integration:
 * - Subscribed automatically to EventPublisher via @Component discovery
 * - Receives OTP_SENT events from OtpService
 * - Uses SMTP configuration defined in MailConfig
 *
 * Example Event:
 * - OTP_SENT: 6-digit OTP code sent to email for registration verification
 */
@Component
@Async
@Slf4j
public class OtpEmailObserver implements Observer {

  @Autowired
  private EventPublisher eventPublisher;

  @Autowired
  private JavaMailSender javaMailSender;

  /**
   * Subscribe this observer to the EventPublisher on bean initialization.
   */
  @PostConstruct
  public void init() {
    eventPublisher.subscribe(this);
    log.info("OtpEmailObserver subscribed to EventPublisher");
  }

  /**
   * Handle OTP_SENT event for email delivery.
   *
   * @param event The domain event containing OTP details
   */
  @Override
  public void handleEvent(EventEnvelope event) {
    if (event == null) {
      log.warn("OtpEmailObserver: Received null event");
      return;
    }

    // Only handle OTP_SENT events
    if (!"OTP_SENT".equals(event.getEventType())) {
      return;
    }

    try {
      log.debug("OtpEmailObserver: Processing OTP_SENT event");

      // Extract email from metadata
      String email = (String) event.getMetadata().get("email");
      String otpCode = (String) event.getMetadata().get("otpCode");

      if (email == null || email.isBlank()) {
        log.warn("OtpEmailObserver: No email in event metadata");
        return;
      }

      if (otpCode == null || otpCode.isBlank()) {
        log.warn("OtpEmailObserver: No OTP code in event metadata");
        return;
      }

      // Create and send OTP email
      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(email);
      message.setSubject("🔐 Your OTP Code for Smart Campus Registration");
      message.setText(formatOtpEmailBody(email, otpCode));

      javaMailSender.send(message);

      log.info("OtpEmailObserver: Successfully sent OTP email to {}", email);

    } catch (Exception e) {
      log.error("OtpEmailObserver failed to send OTP email - event type: {}: {}",
          event.getEventType(), e.getMessage(), e);
    }
  }

  /**
   * Check if this observer is interested in the given event.
   *
   * @param event The event to evaluate for handling
   * @return true if this observer should handle the event (OTP_SENT events only)
   */
  @Override
  public boolean canHandle(EventEnvelope event) {
    if (event == null) {
      return false;
    }

    return "OTP_SENT".equals(event.getEventType())
        && event.getMetadata() != null
        && event.getMetadata().containsKey("email")
        && event.getMetadata().containsKey("otpCode");
  }

  /**
   * Format OTP email body
   *
   * @param email Recipient email address
   * @param otpCode The 6-digit OTP code
   * @return Formatted email body text
   */
  private String formatOtpEmailBody(String email, String otpCode) {
    return "Welcome to Smart Campus,\n\n"
        + "Thank you for registering. To complete your registration, please use the following OTP code:\n\n"
        + "═══════════════════════════════════\n"
        + "OTP CODE: " + otpCode + "\n"
        + "═══════════════════════════════════\n\n"
        + "This code will expire in 10 minutes.\n\n"
        + "Important Security Notice:\n"
        + "- Never share this code with anyone\n"
        + "- Smart Campus staff will never ask for this code\n"
        + "- If you did not request this code, please ignore this email\n\n"
        + "If you have any questions, please contact our support team.\n\n"
        + "Best regards,\n"
        + "Smart Campus Operations System";
  }
}
