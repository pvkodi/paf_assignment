package com.sliitreserve.api.services.notification;

import java.util.Map; /**
 * Email Template Factory Interface (placeholder for T079).
 *
 * Provides per-event-type email templates for SMTP adapter integration.
 * Will be implemented by MailConfig (T079) with Mailtrap setup.
 */
public interface EmailTemplateFactory {
    String getSubjectForEventType(String eventType);

    String getBodyForEventType(String eventType, Map<String, Object> variables);
}
