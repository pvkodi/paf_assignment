package com.sliitreserve.api.workflow.escalation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Escalation result record.
 *
 * <p>Represents the outcome of an escalation handler's evaluation of a ticket's SLA deadline,
 * encapsulating whether escalation was triggered, the escalation level, and summaries of actions
 * taken.
 *
 * <p><b>Escalation Outcome</b>:
 * <ul>
 *   <li><b>escalated=true</b>: SLA deadline has been breached at this level; escalation actions
 *       (reassignment, notifications) have been executed.
 *   <li><b>escalated=false</b>: SLA deadline has not been breached at this level; no escalation
 *       actions taken; the chain may continue to the next handler.
 * </ul>
 *
 * <p><b>Action Summaries</b>: The {@code actionsTaken} list records all escalation operations
 * executed by this handler and upstream handlers (e.g., "Reassigned to Senior Technician John
 * Doe", "Sent HIGH-priority email notification to Facility Manager"). Used for audit logging and
 * decision transparency.
 *
 * <p><b>Example</b>:
 * <pre>{@code
 * EscalationResult result = EscalationResult.builder()
 *     .escalated(true)
 *     .escalationLevel(EscalationLevel.LEVEL_1)
 *     .actionsTaken(List.of(
 *         "SLA breached at CRITICAL (4h) threshold",
 *         "Reassigned ticket to Senior Technician: Jane Smith",
 *         "Sent HIGH-severity email notification to facility manager"
 *     ))
 *     .build();
 * }</pre>
 *
 * @see EscalationHandler for the handler interface that produces this result
 * @see EscalationLevel for the escalation level values
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscalationResult {

  /**
   * Whether escalation was triggered at this level (SLA breached and actions executed).
   */
  private boolean escalated;

  /** The escalation level at which this result was determined. */
  private EscalationLevel escalationLevel;

  /** Summary of escalation actions taken (e.g., reassignment, notifications). */
  @Builder.Default private List<String> actionsTaken = new ArrayList<>();

  /**
   * Optional escalation message or justification for audit logs and user communication.
   */
  private String message;

  /**
   * Add an action summary to the list of actions taken.
   *
   * @param action a description of the escalation action
   * @return this result for fluent chaining
   */
  public EscalationResult addAction(String action) {
    if (this.actionsTaken == null) {
      this.actionsTaken = new ArrayList<>();
    }
    this.actionsTaken.add(action);
    return this;
  }

  /**
   * Check if escalation was triggered.
   *
   * @return true if escalated
   */
  public boolean wasEscalated() {
    return escalated;
  }
}
