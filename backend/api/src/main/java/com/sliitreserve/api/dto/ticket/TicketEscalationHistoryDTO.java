package com.sliitreserve.api.dto.ticket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.sliitreserve.api.workflow.escalation.EscalationLevel;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for escalation history events.
 *
 * <p>Audit trail showing all escalation transitions for a ticket,
 * including timestamps, levels, and responsible users.
 *
 * @see com.sliitreserve.api.entities.ticket.TicketEscalation for entity structure
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketEscalationHistoryDTO {

  private UUID id;
  private EscalationLevel fromLevel;
  private EscalationLevel toLevel;
  private String reason;

  private UUID escalatedById;
  private String escalatedByName;

  private LocalDateTime escalatedAt;
}
