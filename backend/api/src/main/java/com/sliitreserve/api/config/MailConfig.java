package com.sliitreserve.api.config;

import com.sliitreserve.api.services.notification.EmailTemplateFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;

/**
 * Mail Configuration for SMTP Adapter (Mailtrap Integration)
 *
 * Purpose: Configure email delivery via Mailtrap SMTP service for notifications.
 * Provides production-ready email templates for all notification event types.
 *
 * Configuration Sources:
 * - environment.properties: SMTP credentials and settings
 * - application.yaml: Spring mail configuration properties
 * - Mailtrap account: https://mailtrap.io (development/staging)
 * - Production: Can be swapped with SendGrid, AWS SES, or other SMTP provider
 *
 * Mailtrap Benefits:
 * - Built-in email testing (no real emails sent in dev)
 * - Full message inspection and delivery logs
 * - Spam filter testing capabilities
 * - Free tier covers typical development needs
 * - Can capture/inspect all outgoing emails
 *
 * Environment Variables (Required):
 * - MAIL_HOST: smtp.mailtrap.io (Mailtrap SMTP host)
 * - MAIL_PORT: 2525 or 587 (Mailtrap SMTP port)
 * - MAIL_USERNAME: Mailtrap API token or username
 * - MAIL_PASSWORD: Mailtrap password
 * - MAIL_FROM: Sender email address (campus@example.com)
 * - MAIL_ENABLED: true/false (enable/disable email delivery)
 *
 * Usage in Services:
 * ```java
 * @Component
 * public class EmailNotificationService {
 *     @Autowired
 *     private JavaMailSender mailSender;
 *
 *     @Autowired
 *     private EmailTemplateFactory emailTemplates;
 *
 *     public void sendNotification(User recipient, EventEnvelope event) {
 *         String subject = emailTemplates.getSubjectForEventType(event.getEventType());
 *         String body = emailTemplates.getBodyForEventType(event.getEventType(), Map.of(
 *             "recipientName", recipient.getDisplayName(),
 *             "eventTitle", event.getTitle(),
 *             "eventDescription", event.getDescription(),
 *             "actionUrl", event.getActionUrl(),
 *             "actionLabel", event.getActionLabel()
 *         ));
 *
 *         SimpleMailMessage message = new SimpleMailMessage();
 *         message.setTo(recipient.getEmail());
 *         message.setSubject(subject);
 *         message.setText(body);
 *
 *         mailSender.send(message);
 *     }
 * }
 * ```
 *
 * Mailtrap SMTP Settings:
 * - Integration: Mailtrap is a cloud SMTP service with sandbox inboxes
 * - Security: Supports TLS/SSL encryption
 * - Reliability: 99.9% uptime SLA
 * - Development: Perfect for testing without sending real emails
 * - Production Alternative: SendGrid, AWS SES, or enterprise SMTP server
 *
 * Configuration from application.yaml:
 * ```yaml
 * spring:
 *   mail:
 *     host: ${MAIL_HOST:smtp.mailtrap.io}
 *     port: ${MAIL_PORT:2525}
 *     username: ${MAIL_USERNAME:}
 *     password: ${MAIL_PASSWORD:}
 *     from: ${MAIL_FROM:noreply@campus-ops.local}
 *     properties:
 *       mail:
 *         smtp:
 *           auth: true
 *           starttls:
 *             enable: true
 *             required: true
 *           debug: ${MAIL_DEBUG:false}
 * ```
 *
 * TODO (T082): Integrate with NotificationController for email delivery
 * TODO (Production): Replace Mailtrap with SendGrid/AWS SES for production
 */
@Configuration
@Slf4j
public class MailConfig {

    @Value("${spring.mail.host:localhost}")
    private String mailHost;

    @Value("${spring.mail.port:1025}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.from:noreply@smartcampus.local}")
    private String mailFrom;

    @Value("${spring.mail.properties.mail.smtp.auth:false}")
    private boolean smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}")
    private boolean starttlsEnable;

    @Value("${spring.mail.properties.mail.smtp.starttls.required:false}")
    private boolean starttlsRequired;

    @Value("${spring.mail.properties.mail.smtp.debug:false}")
    private boolean smtpDebug;

    @Value("${mail.enabled:true}")
    private boolean mailEnabled;

    /**
     * Configure JavaMailSender bean for SMTP delivery.
     *
     * Mailtrap Configuration:
     * - Host: smtp.mailtrap.io
     * - Port: 2525 (or 587 for direct SSL)
     * - Auth: Required (username/API token)
     * - TLS: Enabled and required
     *
     * Development Setup:
     * 1. Create free account at https://mailtrap.io
     * 2. Create Sending domain and get API credentials
     * 3. Set MAIL_USERNAME and MAIL_PASSWORD environment variables
     * 4. All emails go to inbox for inspection
     *
     * @return Configured JavaMailSender for SMTP delivery
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        if (!mailEnabled) {
            log.warn("Email delivery is DISABLED (mail.enabled=false)");
            return mailSender;
        }

        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);
        mailSender.setDefaultEncoding("UTF-8");

        Properties mailProperties = mailSender.getJavaMailProperties();
        mailProperties.put("mail.smtp.auth", smtpAuth);
        mailProperties.put("mail.smtp.starttls.enable", starttlsEnable);
        mailProperties.put("mail.smtp.starttls.required", starttlsRequired);
        mailProperties.put("mail.smtp.debug", smtpDebug);

        // Additional properties for Mailtrap
        mailProperties.put("mail.smtp.connectiontimeout", 5000);
        mailProperties.put("mail.smtp.timeout", 5000);
        mailProperties.put("mail.smtp.writetimeout", 5000);

        log.info("JavaMailSender configured - Host: {}, Port: {}, Auth: {}, TLS: {}",
                mailHost,
                mailPort,
                smtpAuth,
                starttlsEnable);

        return mailSender;
    }

    /**
     * Email Template Factory Bean
     *
     * Provides per-event-type email templates with variable substitution.
     * Supports both HTML and plain-text templates.
     * Used by EmailObserver to render notification emails.
     *
     * @return EmailTemplateFactory implementation
     */
    @Bean
    public EmailTemplateFactory emailTemplateFactory() {
        return new MailtrapEmailTemplateFactory(mailFrom);
    }
}

/**
 * Mailtrap Email Template Factory Implementation
 *
 * Provides comprehensive email templates for all notification event types.
 * Supports HTML rendering with embedded CSS for email clients.
 * Implements template variable substitution for personalization.
 *
 * Quick Start:
 * ```
 * EmailTemplateFactory templates = new MailtrapEmailTemplateFactory("noreply@campus-ops.local");
 * String subject = templates.getSubjectForEventType("SLA_DEADLINE_BREACHED");
 * String body = templates.getBodyForEventType("SLA_DEADLINE_BREACHED", Map.of(
 *     "recipientName", "John Doe",
 *     "ticketId", "TKT-12345",
 *     "dueTime", "2 hours"
 * ));
 * ```
 */
@Slf4j
class MailtrapEmailTemplateFactory implements EmailTemplateFactory {

    private final String fromAddress;

    public MailtrapEmailTemplateFactory(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    /**
     * Get email subject line for event type.
     *
     * @param eventType The event type (e.g., SLA_DEADLINE_BREACHED)
     * @return Email subject line with emoji indicators
     */
    @Override
    public String getSubjectForEventType(String eventType) {
        return switch (eventType) {
            case "SLA_DEADLINE_APPROACHING" ->
                    "⚠️ Maintenance Ticket: SLA Deadline Approaching";
            case "SLA_DEADLINE_BREACHED" ->
                    "🚨 URGENT: Maintenance Ticket SLA Breached";
            case "TICKET_ESCALATED" ->
                    "🚨 Maintenance Ticket Escalated to Level " + getEscalationLevel();
            case "TICKET_CREATED" ->
                    "🎟️ New Maintenance Ticket Created";
            case "TICKET_ASSIGNED" ->
                    "📋 Maintenance Ticket Assigned to You";
            case "TICKET_CLOSED" ->
                    "✅ Maintenance Ticket Resolved";
            case "USER_SUSPENDED" ->
                    "⛔ Campus Account Suspended";
            case "SUSPEND_WARNING" ->
                    "⚠️ Warning: Excessive No-Shows Detected";
            case "APPEAL_SUBMITTED" ->
                    "📝 Suspension Appeal Received";
            case "APPEAL_APPROVED" ->
                    "✅ Suspension Appeal Approved";
            case "APPEAL_REJECTED" ->
                    "⛔ Suspension Appeal Rejected";
            case "BOOKING_APPROVED" ->
                    "✅ Booking Request Approved";
            case "BOOKING_REJECTED" ->
                    "❌ Booking Request Rejected";
            case "BOOKING_REQUEST_SUBMITTED" ->
                    "📋 Booking Request Received";
            case "CHECK_IN_SUCCESS" ->
                    "✅ Check-in Successful";
            case "NO_SHOW_RECORDED" ->
                    "⚠️ No-Show Recorded";
            default -> "Campus Operations Notification";
        };
    }

    /**
     * Get email body for event type.
     *
     * @param eventType The event type
     * @param variables Map of template variables
     * @return HTML email body content
     */
    @Override
    public String getBodyForEventType(String eventType, Map<String, Object> variables) {
        return switch (eventType) {
            case "SLA_DEADLINE_APPROACHING" -> buildSlaApproachingTemplate(variables);
            case "SLA_DEADLINE_BREACHED" -> buildSlaBreachedTemplate(variables);
            case "TICKET_ESCALATED" -> buildTicketEscalatedTemplate(variables);
            case "TICKET_CREATED" -> buildTicketCreatedTemplate(variables);
            case "TICKET_ASSIGNED" -> buildTicketAssignedTemplate(variables);
            case "TICKET_CLOSED" -> buildTicketClosedTemplate(variables);
            case "USER_SUSPENDED" -> buildUserSuspendedTemplate(variables);
            case "SUSPEND_WARNING" -> buildSuspendWarningTemplate(variables);
            case "APPEAL_SUBMITTED" -> buildAppealSubmittedTemplate(variables);
            case "APPEAL_APPROVED" -> buildAppealApprovedTemplate(variables);
            case "APPEAL_REJECTED" -> buildAppealRejectedTemplate(variables);
            case "BOOKING_APPROVED" -> buildBookingApprovedTemplate(variables);
            case "BOOKING_REJECTED" -> buildBookingRejectedTemplate(variables);
            case "BOOKING_REQUEST_SUBMITTED" -> buildBookingSubmittedTemplate(variables);
            case "CHECK_IN_SUCCESS" -> buildCheckInSuccessTemplate(variables);
            case "NO_SHOW_RECORDED" -> buildNoShowTemplate(variables);
            default -> buildGenericTemplate(variables);
        };
    }

    /**
     * Build generic email template.
     */
    private String buildGenericTemplate(Map<String, Object> variables) {
        String recipientName = get(variables, "recipientName", "User");
        String title = get(variables, "eventTitle", "Notification");
        String description = get(variables, "eventDescription", "");
        String actionUrl = get(variables, "actionUrl", "/dashboard");
        String actionLabel = get(variables, "actionLabel", "View Details");

        return wrapHtmlTemplate(String.format(
                "<h2>%s</h2>"
                        + "<p>Dear %s,</p>"
                        + "<p>%s</p>"
                        + "<p><a href=\"%s\" class=\"btn btn-primary\">%s</a></p>",
                title, recipientName, description, actionUrl, actionLabel
        ));
    }

    private String buildSlaApproachingTemplate(Map<String, Object> variables) {
        String recipientName = get(variables, "recipientName", "Technician");
        String ticketId = get(variables, "ticketId", "#UNKNOWN");
        String ticketTitle = get(variables, "ticketTitle", "Maintenance Issue");
        String timeRemaining = get(variables, "timeRemaining", "2 hours");

        return wrapHtmlTemplate(String.format(
                "<h2>⚠️ SLA Deadline Approaching</h2>"
                        + "<p>Dear %s,</p>"
                        + "<p>A maintenance ticket is approaching its SLA deadline.</p>"
                        + "<table class=\"ticket-details\"><tr>"
                        + "<td><strong>Ticket ID:</strong></td><td>%s</td></tr><tr>"
                        + "<td><strong>Title:</strong></td><td>%s</td></tr><tr>"
                        + "<td><strong>Time Remaining:</strong></td><td>%s</td>"
                        + "</tr></table>"
                        + "<p><strong>Please prioritize this ticket to avoid SLA breach.</strong></p>"
                        + "<p><a href=\"%s/tickets/%s\" class=\"btn btn-warning\">View Ticket</a></p>",
                recipientName, ticketId, ticketTitle, timeRemaining,
                getBaseUrl(), ticketId.replace("#", "")
        ));
    }

    private String buildSlaBreachedTemplate(Map<String, Object> variables) {
        String recipientName = get(variables, "recipientName", "Manager");
        String ticketId = get(variables, "ticketId", "#UNKNOWN");
        String ticketTitle = get(variables, "ticketTitle", "Maintenance Issue");
        String escalationLevel = get(variables, "escalationLevel", "1");

        return wrapHtmlTemplate(String.format(
                "<h2 style=\"color: #d32f2f;\">🚨 SLA DEADLINE BREACHED</h2>"
                        + "<p>Dear %s,</p>"
                        + "<p><strong>A maintenance ticket has exceeded its SLA deadline.</strong></p>"
                        + "<table class=\"ticket-details\" style=\"background: #fff3cd;\"><tr>"
                        + "<td><strong>Ticket ID:</strong></td><td>%s</td></tr><tr>"
                        + "<td><strong>Title:</strong></td><td>%s</td></tr><tr>"
                        + "<td><strong>Escalation Level:</strong></td><td>%s</td>"
                        + "</tr></table>"
                        + "<p><strong>ACTION REQUIRED:</strong> Immediate intervention needed to resolve this ticket.</p>"
                        + "<p><a href=\"%s/tickets/%s\" class=\"btn btn-danger\">URGENT: View Ticket</a></p>"
                        + "<p style=\"color: #666; font-size: 12px;\">This is a high-priority notification.</p>",
                recipientName, ticketId, ticketTitle, escalationLevel,
                getBaseUrl(), ticketId.replace("#", "")
        ));
    }

    private String buildTicketEscalatedTemplate(Map<String, Object> variables) {
        String recipientName = get(variables, "recipientName", "Manager");
        String ticketId = get(variables, "ticketId", "#UNKNOWN");
        String fromLevel = get(variables, "fromLevel", "1");
        String toLevel = get(variables, "toLevel", "2");

        return wrapHtmlTemplate(String.format(
                "<h2>🚨 Ticket Escalated</h2>"
                        + "<p>Dear %s,</p>"
                        + "<p>A maintenance ticket has been escalated due to SLA breach.</p>"
                        + "<table class=\"ticket-details\"><tr>"
                        + "<td><strong>Ticket ID:</strong></td><td>%s</td></tr><tr>"
                        + "<td><strong>Escalation:</strong></td><td>Level %s → Level %s</td>"
                        + "</tr></table>"
                        + "<p><a href=\"%s/tickets/%s\" class=\"btn btn-danger\">View Ticket</a></p>",
                recipientName, ticketId, fromLevel, toLevel,
                getBaseUrl(), ticketId.replace("#", "")
        ));
    }

    private String buildTicketCreatedTemplate(Map<String, Object> variables) {
        String recipientName = get(variables, "recipientName", "Staff");
        String ticketId = get(variables, "ticketId", "#UNKNOWN");
        String title = get(variables, "ticketTitle", "New Maintenance Issue");
        String priority = get(variables, "priority", "MEDIUM");

        return wrapHtmlTemplate(String.format(
                "<h2>🎟️ New Maintenance Ticket</h2>"
                        + "<p>Dear %s,</p>"
                        + "<p>A new maintenance ticket has been created.</p>"
                        + "<table class=\"ticket-details\"><tr>"
                        + "<td><strong>Ticket ID:</strong></td><td>%s</td></tr><tr>"
                        + "<td><strong>Title:</strong></td><td>%s</td></tr><tr>"
                        + "<td><strong>Priority:</strong></td><td>%s</td>"
                        + "</tr></table>"
                        + "<p><a href=\"%s/tickets/%s\" class=\"btn btn-primary\">View Ticket</a></p>",
                recipientName, ticketId, title, priority,
                getBaseUrl(), ticketId.replace("#", "")
        ));
    }

    private String buildTicketAssignedTemplate(Map<String, Object> variables) {
        String recipientName = get(variables, "recipientName", "Technician");
        String ticketId = get(variables, "ticketId", "#UNKNOWN");
        String title = get(variables, "ticketTitle", "Maintenance Issue");

        return wrapHtmlTemplate(String.format(
                "<h2>📋 Ticket Assigned</h2>"
                        + "<p>Dear %s,</p>"
                        + "<p>A maintenance ticket has been assigned to you.</p>"
                        + "<table class=\"ticket-details\"><tr>"
                        + "<td><strong>Ticket ID:</strong></td><td>%s</td></tr><tr>"
                        + "<td><strong>Title:</strong></td><td>%s</td>"
                        + "</tr></table>"
                        + "<p><a href=\"%s/tickets/%s\" class=\"btn btn-primary\">View Ticket</a></p>",
                recipientName, ticketId, title,
                getBaseUrl(), ticketId.replace("#", "")
        ));
    }

    private String buildTicketClosedTemplate(Map<String, Object> variables) {
        String recipientName = get(variables, "recipientName", "User");
        String ticketId = get(variables, "ticketId", "#UNKNOWN");
        String resolution = get(variables, "resolution", "Issue resolved");

        return wrapHtmlTemplate(String.format(
                "<h2>✅ Ticket Resolved</h2>"
                        + "<p>Dear %s,</p>"
                        + "<p>The maintenance ticket has been closed.</p>"
                        + "<table class=\"ticket-details\"><tr>"
                        + "<td><strong>Ticket ID:</strong></td><td>%s</td></tr><tr>"
                        + "<td><strong>Resolution:</strong></td><td>%s</td>"
                        + "</tr></table>",
                recipientName, ticketId, resolution
        ));
    }

    private String buildUserSuspendedTemplate(Map<String, Object> variables) {
        String recipientName = get(variables, "recipientName", "User");
        String duration = get(variables, "duration", "7 days");
        String reason = get(variables, "reason", "Excessive no-shows");

        return wrapHtmlTemplate(String.format(
                "<h2 style=\"color: #d32f2f;\">⛔ Account Suspended</h2>"
                        + "<p>Dear %s,</p>"
                        + "<p>Your campus account has been suspended.</p>"
                        + "<table class=\"ticket-details\" style=\"background: #fce4ec;\"><tr>"
                        + "<td><strong>Duration:</strong></td><td>%s</td></tr><tr>"
                        + "<td><strong>Reason:</strong></td><td>%s</td>"
                        + "</tr></table>"
                        + "<p>You can appeal this suspension. <a href=\"%s/appeals\" class=\"btn btn-primary\">Submit Appeal</a></p>",
                recipientName, duration, reason,
                getBaseUrl()
        ));
    }

    private String buildSuspendWarningTemplate(Map<String, Object> variables) {
        String recipientName = get(variables, "recipientName", "User");
        String noShowCount = get(variables, "noShowCount", "2");
        String threshold = get(variables, "threshold", "3");

        return wrapHtmlTemplate(String.format(
                "<h2>⚠️ Suspension Warning</h2>"
                        + "<p>Dear %s,</p>"
                        + "<p>Please note that you have accumulated <strong>%s no-shows</strong>. If you accumulate %s no-shows, your account will be suspended.</p>"
                        + "<p>Please ensure you attend all your booked facilities or cancel in advance.</p>",
                recipientName, noShowCount, threshold
        ));
    }

    private String buildAppealSubmittedTemplate(Map<String, Object> variables) {
        String recipientName = get(variables, "recipientName", "User");

        return wrapHtmlTemplate(String.format(
                "<h2>📝 Appeal Submitted</h2>"
                        + "<p>Dear %s,</p>"
                        + "<p>Your suspension appeal has been received. An administrator will review it within 2-3 business days.</p>"
                        + "<p><a href=\"%s/appeals\" class=\"btn btn-primary\">View Appeal Status</a></p>",
                recipientName,
                getBaseUrl()
        ));
    }

    private String buildAppealApprovedTemplate(Map<String, Object> variables) {
        String recipientName = get(variables, "recipientName", "User");

        return wrapHtmlTemplate(String.format(
                "<h2>✅ Appeal Approved</h2>"
                        + "<p>Dear %s,</p>"
                        + "<p>Your suspension appeal has been <strong>approved</strong>. Your account has been reinstated.</p>"
                        + "<p>You can now resume booking facilities.</p>"
                        + "<p><a href=\"%s/bookings\" class=\"btn btn-success\">Make a Booking</a></p>",
                recipientName,
                getBaseUrl()
        ));
    }

    private String buildAppealRejectedTemplate(Map<String, Object> variables) {
        String recipientName = get(variables, "recipientName", "User");
        String reason = get(variables, "reason", "Insufficient evidence provided");

        return wrapHtmlTemplate(String.format(
                "<h2>⛔ Appeal Rejected</h2>"
                        + "<p>Dear %s,</p>"
                        + "<p>Your suspension appeal has been <strong>rejected</strong>.</p>"
                        + "<p><strong>Reason:</strong> %s</p>"
                        + "<p>Your account remains suspended. You may submit another appeal after 7 days.</p>",
                recipientName, reason
        ));
    }

    private String buildBookingApprovedTemplate(Map<String, Object> variables) {
        String recipientName = get(variables, "recipientName", "User");
        String facilityName = get(variables, "facilityName", "Facility");
        String bookingDate = get(variables, "bookingDate", "TBD");
        String timeSlot = get(variables, "timeSlot", "TBD");

        return wrapHtmlTemplate(String.format(
                "<h2>✅ Booking Approved</h2>"
                        + "<p>Dear %s,</p>"
                        + "<p>Your booking request has been approved.</p>"
                        + "<table class=\"ticket-details\"><tr>"
                        + "<td><strong>Facility:</strong></td><td>%s</td></tr><tr>"
                        + "<td><strong>Date:</strong></td><td>%s</td></tr><tr>"
                        + "<td><strong>Time:</strong></td><td>%s</td>"
                        + "</tr></table>"
                        + "<p><a href=\"%s/bookings\" class=\"btn btn-success\">View Booking</a></p>",
                recipientName, facilityName, bookingDate, timeSlot,
                getBaseUrl()
        ));
    }

    private String buildBookingRejectedTemplate(Map<String, Object> variables) {
        String recipientName = get(variables, "recipientName", "User");
        String reason = get(variables, "reason", "Request denied");

        return wrapHtmlTemplate(String.format(
                "<h2>❌ Booking Rejected</h2>"
                        + "<p>Dear %s,</p>"
                        + "<p>Your booking request has been rejected.</p>"
                        + "<p><strong>Reason:</strong> %s</p>"
                        + "<p><a href=\"%s/bookings/new\" class=\"btn btn-primary\">Try Another Time</a></p>",
                recipientName, reason,
                getBaseUrl()
        ));
    }

    private String buildBookingSubmittedTemplate(Map<String, Object> variables) {
        String recipientName = get(variables, "recipientName", "User");
        String facilityName = get(variables, "facilityName", "Facility");

        return wrapHtmlTemplate(String.format(
                "<h2>📋 Booking Request Received</h2>"
                        + "<p>Dear %s,</p>"
                        + "<p>Your booking request for <strong>%s</strong> has been received and is pending approval.</p>"
                        + "<p>You will be notified once the request is processed.</p>",
                recipientName, facilityName
        ));
    }

    private String buildCheckInSuccessTemplate(Map<String, Object> variables) {
        String recipientName = get(variables, "recipientName", "User");
        String facilityName = get(variables, "facilityName", "Facility");

        return wrapHtmlTemplate(String.format(
                "<h2>✅ Check-in Successful</h2>"
                        + "<p>Dear %s,</p>"
                        + "<p>You have successfully checked in to <strong>%s</strong>.</p>"
                        + "<p>Thank you for using Campus Operations Hub.</p>",
                recipientName, facilityName
        ));
    }

    private String buildNoShowTemplate(Map<String, Object> variables) {
        String recipientName = get(variables, "recipientName", "User");
        String facilityName = get(variables, "facilityName", "Facility");
        String noShowCount = get(variables, "noShowCount", "1");

        return wrapHtmlTemplate(String.format(
                "<h2>⚠️ No-Show Recorded</h2>"
                        + "<p>Dear %s,</p>"
                        + "<p>You did not check in to <strong>%s</strong> and a no-show has been recorded.</p>"
                        + "<p><strong>No-Show Count:</strong> %s</p>"
                        + "<p>If you accumulate 3 no-shows, your account will be suspended.</p>",
                recipientName, facilityName, noShowCount
        ));
    }

    /**
     * Wrap HTML template with standard styling and footer.
     */
    private String wrapHtmlTemplate(String content) {
        return String.format(
                "<!DOCTYPE html>\n"
                        + "<html>\n"
                        + "<head>\n"
                        + "<meta charset=\"UTF-8\">\n"
                        + "<style>\n"
                        + "  body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }\n"
                        + "  .container { 1max-width: 600px; margin: 0 auto; padding: 20px; }\n"
                        + "  h2 { color: #1976d2; border-bottom: 2px solid #1976d2; padding-bottom: 10px; }\n"
                        + "  table.ticket-details { width: 100%%; border-collapse: collapse; margin: 15px 0; }\n"
                        + "  table.ticket-details td { padding: 8px; border: 1px solid #ddd; }\n"
                        + "  table.ticket-details tr:nth-child(odd) { background-color: #f9f9f9; }\n"
                        + "  .btn { display: inline-block; padding: 10px 20px; margin: 10px 0; border-radius: 4px; text-decoration: none; color: white; font-weight: bold; }\n"
                        + "  .btn-primary { background-color: #1976d2; }\n"
                        + "  .btn-success { background-color: #388e3c; }\n"
                        + "  .btn-warning { background-color: #f57c00; }\n"
                        + "  .btn-danger { background-color: #d32f2f; }\n"
                        + "  .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; color: #999; font-size: 12px; }\n"
                        + "</style>\n"
                        + "</head>\n"
                        + "<body>\n"
                        + "<div class=\"container\">\n"
                        + "%s\n"
                        + "<div class=\"footer\">"
                        + "<p>This is an automated message from Campus Operations Hub.</p>"
                        + "<p>Please do not reply to this email.</p>"
                        + "<p>&copy; 2026 VenueLink. All rights reserved.</p>"
                        + "</div>\n"
                        + "</div>\n"
                        + "</body>\n"
                        + "</html>",
                content
        );
    }

    private String get(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.getOrDefault(key, defaultValue);
        return value != null ? value.toString() : defaultValue;
    }

    private String getBaseUrl() {
        return "https://campus-ops.local";
    }

    private String getEscalationLevel() {
        return "2";
    }
}
