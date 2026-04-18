package com.sliitreserve.api.dto.ticket;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for manual ticket escalation.
 *
 * <p>Allows staff (TECHNICIAN, FACILITY_MANAGER, ADMIN) to manually escalate
 * a ticket to the next severity level with a reason for audit trail and assign
 * it to a specific staff member.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualEscalationRequest {

  /**
   * Reason for manual escalation. Required field.
   * Examples:
   * - "Electrical hazard detected - safety risk"
   * - "Issue more complex than initially assessed"
   * - "Requires specialist intervention"
   * - "Customer escalated due to repeated failures"
   */
  private String reason;

  /**
   * ID of the staff member to assign the escalated ticket to. Required field.
   * This ensures the escalated ticket is immediately assigned for action.
   */
  private UUID assigneeId;
}
