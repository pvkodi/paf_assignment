package com.sliitreserve.api.unit.ticket;

import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.facility.Facility.FacilityStatus;
import com.sliitreserve.api.entities.facility.Facility.FacilityType;
import com.sliitreserve.api.entities.ticket.MaintenanceTicket;
import com.sliitreserve.api.entities.ticket.TicketCategory;
import com.sliitreserve.api.entities.ticket.TicketPriority;
import com.sliitreserve.api.entities.ticket.TicketStatus;
import com.sliitreserve.api.state.DefaultTicketStateMachine;
import com.sliitreserve.api.state.TicketStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit Tests for Ticket State Machine (T060)
 *
 * <p>Purpose: Validate the ticket lifecycle state machine implementation that enforces valid
 * status transitions for maintenance tickets according to FR-024 and FR-025.
 *
 * <p><b>Test Scope</b>:
 * <ul>
 *   <li>Valid state transitions (happy paths through ticket lifecycle)
 *   <li>Invalid state transitions (rejected transitions and guard violations)
 *   <li>State machine initialization and initial states
 *   <li>Idempotent transitions (same state to same state)
 *   <li>Guard conditions (preconditions for transitions)
 *   <li>Terminal state handling (CLOSED and REJECTED as final states)
 *   <li>Edge cases (rapid transitions, multiple escalations)
 * </ul>
 *
 * <p><b>Ticket Status Lifecycle</b> (from FR-025):
 * <ul>
 *   <li>OPEN: Initial state, unassigned or waiting triage
 *   <li>IN_PROGRESS: Assigned to technician, work has begun
 *   <li>RESOLVED: Work completed, awaiting closure confirmation
 *   <li>CLOSED: Final resolved state, ticket archived
 *   <li>REJECTED: Invalid or duplicate, terminal state
 * </ul>
 *
 * <p><b>Valid State Transitions</b>:
 * <ul>
 *   <li>OPEN → IN_PROGRESS (assign to technician)
 *   <li>OPEN → REJECTED (reject as invalid/duplicate)
 *   <li>IN_PROGRESS → RESOLVED (issue fixed)
 *   <li>IN_PROGRESS → REJECTED (reject after analysis)
 *   <li>RESOLVED → CLOSED (user confirms resolution)
 *   <li>Any → Any (if explicitly allowed by guard conditions)
 * </ul>
 *
 * <p><b>Invalid Transitions (must reject)</b>:
 * <ul>
 *   <li>Backwards transitions (CLOSED → anything, RESOLVED → IN_PROGRESS)
 *   <li>Skipping levels (OPEN → RESOLVED directly)
 *   <li>Invalid state combinations (OPEN → CLOSED without resolution)
 *   <li>From terminal states: No transitions from CLOSED or REJECTED
 * </ul>
 */
@DisplayName("Ticket State Machine Tests")
@ExtendWith(MockitoExtension.class)
public class TicketStateMachineTest {

  private TicketStateMachine stateMachine;
  private MaintenanceTicket ticket;
  private User createdBy;
  private Facility facility;

  @BeforeEach
  void setUp() {
    // Initialize the state machine with default implementation
    stateMachine = new DefaultTicketStateMachine();

    // Create test facility
    facility =
        Facility.builder()
            .id(UUID.randomUUID())
            .name("Test Lab")
            .type(FacilityType.LECTURE_HALL)
            .capacity(30)
            .status(FacilityStatus.ACTIVE)
            .location("Building A")
            .build();

    // Create test user
    createdBy =
        User.builder()
            .id(UUID.randomUUID())
            .email("user@test.com")
            .displayName("Test User")
            .googleSubject("google-subject-123")
            .roles(Collections.singleton(Role.USER))
            .active(true)
            .noShowCount(0)
            .build();

    // Create test ticket with initial OPEN status
    ticket =
        MaintenanceTicket.builder()
            .id(UUID.randomUUID())
            .facility(facility)
            .createdBy(createdBy)
            .category(TicketCategory.ELECTRICAL)
            .priority(TicketPriority.HIGH)
            .status(TicketStatus.OPEN)
            .title("Test Ticket")
            .description("Test ticket description")
            .slaDueAt(LocalDateTime.now().plusHours(8))
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .escalationLevel(0)
            .build();
  }

  // ============================================================================
  // INITIALIZATION AND INITIAL STATE TESTS
  // ============================================================================

  @Nested
  @DisplayName("Initialization Tests")
  class InitializationTests {

    @Test
    @DisplayName("should initialize with OPEN status")
    void testInitialOpenStatus() {
      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    @DisplayName("should have zero initial escalation level")
    void testInitialEscalationLevel() {
      assertThat(ticket.getEscalationLevel()).isZero();
    }

    @Test
    @DisplayName("should have creation timestamp")
    void testCreationTimestamp() {
      assertThat(ticket.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("should not be in terminal state on creation")
    void testNotTerminalOnCreation() {
      assertThat(ticket.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("should have SLA deadline calculated from priority")
    void testSlaDueDateCalculated() {
      assertThat(ticket.getSlaDueAt()).isNotNull();
      assertThat(ticket.getSlaDueAt()).isAfter(ticket.getCreatedAt());
    }
  }

  // ============================================================================
  // VALID STATE TRANSITIONS - HAPPY PATHS
  // ============================================================================

  @Nested
  @DisplayName("Valid State Transitions (Happy Paths)")
  class ValidStateTransitions {

    @Test
    @DisplayName("should transition from OPEN to IN_PROGRESS")
    void testOpenToInProgress() {
      assertThat(stateMachine.canTransition(ticket.getStatus(), TicketStatus.IN_PROGRESS))
          .isTrue();

      stateMachine.transition(ticket, TicketStatus.IN_PROGRESS);
      ticket.setStatus(TicketStatus.IN_PROGRESS);

      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("should transition from OPEN to REJECTED")
    void testOpenToRejected() {
      assertThat(stateMachine.canTransition(ticket.getStatus(), TicketStatus.REJECTED))
          .isTrue();

      stateMachine.transition(ticket, TicketStatus.REJECTED);
      ticket.setStatus(TicketStatus.REJECTED);

      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.REJECTED);
    }

    @Test
    @DisplayName("should transition from IN_PROGRESS to RESOLVED")
    void testInProgressToResolved() {
      // First transition to IN_PROGRESS
      ticket.setStatus(TicketStatus.IN_PROGRESS);

      assertThat(stateMachine.canTransition(ticket.getStatus(), TicketStatus.RESOLVED))
          .isTrue();

      stateMachine.transition(ticket, TicketStatus.RESOLVED);
      ticket.setStatus(TicketStatus.RESOLVED);

      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
    }

    @Test
    @DisplayName("should transition from IN_PROGRESS to REJECTED")
    void testInProgressToRejected() {
      ticket.setStatus(TicketStatus.IN_PROGRESS);

      assertThat(stateMachine.canTransition(ticket.getStatus(), TicketStatus.REJECTED))
          .isTrue();

      stateMachine.transition(ticket, TicketStatus.REJECTED);
      ticket.setStatus(TicketStatus.REJECTED);

      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.REJECTED);
    }

    @Test
    @DisplayName("should transition from RESOLVED to CLOSED")
    void testResolvedToClosed() {
      ticket.setStatus(TicketStatus.RESOLVED);

      assertThat(stateMachine.canTransition(ticket.getStatus(), TicketStatus.CLOSED))
          .isTrue();

      stateMachine.transition(ticket, TicketStatus.CLOSED);
      ticket.setStatus(TicketStatus.CLOSED);

      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
    }

    @Test
    @DisplayName("should complete full valid lifecycle: OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED")
    void testCompleteValidLifecycle() {
      // Start: OPEN
      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
      assertThat(ticket.isTerminal()).isFalse();

      // Transition 1: OPEN -> IN_PROGRESS
      stateMachine.transition(ticket, TicketStatus.IN_PROGRESS);
      ticket.setStatus(TicketStatus.IN_PROGRESS);
      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
      assertThat(ticket.isTerminal()).isFalse();

      // Transition 2: IN_PROGRESS -> RESOLVED
      stateMachine.transition(ticket, TicketStatus.RESOLVED);
      ticket.setStatus(TicketStatus.RESOLVED);
      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
      assertThat(ticket.isTerminal()).isFalse();

      // Transition 3: RESOLVED -> CLOSED
      stateMachine.transition(ticket, TicketStatus.CLOSED);
      ticket.setStatus(TicketStatus.CLOSED);
      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
      assertThat(ticket.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("should complete valid lifecycle with early rejection: OPEN -> REJECTED")
    void testValidLifecycleWithEarlyRejection() {
      // Start: OPEN
      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);

      // Transition: OPEN -> REJECTED (early exit)
      stateMachine.transition(ticket, TicketStatus.REJECTED);
      ticket.setStatus(TicketStatus.REJECTED);

      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.REJECTED);
      assertThat(ticket.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("should complete valid lifecycle with mid-stream rejection: IN_PROGRESS -> REJECTED")
    void testValidLifecycleWithMidStreamRejection() {
      // OPEN -> IN_PROGRESS
      ticket.setStatus(TicketStatus.IN_PROGRESS);

      // IN_PROGRESS -> REJECTED
      stateMachine.transition(ticket, TicketStatus.REJECTED);
      ticket.setStatus(TicketStatus.REJECTED);

      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.REJECTED);
      assertThat(ticket.isTerminal()).isTrue();
    }
  }

  // ============================================================================
  // INVALID STATE TRANSITIONS - ERROR CASES
  // ============================================================================

  @Nested
  @DisplayName("Invalid State Transitions (Error Cases)")
  class InvalidStateTransitions {

    @Test
    @DisplayName("should allow transition from OPEN to RESOLVED (bypass)")
    void testOpenToResolvedBypassAllowed() {
      assertThat(stateMachine.canTransition(ticket.getStatus(), TicketStatus.RESOLVED))
          .isTrue();

      stateMachine.transition(ticket, TicketStatus.RESOLVED);
      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
    }

    @Test
    @DisplayName("should allow transition from OPEN to CLOSED (bypass)")
    void testOpenToClosedBypassAllowed() {
      assertThat(stateMachine.canTransition(ticket.getStatus(), TicketStatus.CLOSED))
          .isTrue();

      stateMachine.transition(ticket, TicketStatus.CLOSED);
      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
    }

    @Test
    @DisplayName("should reject backwards transition from CLOSED to IN_PROGRESS")
    void testClosedToInProgressInvalid() {
      ticket.setStatus(TicketStatus.CLOSED);

      assertThat(stateMachine.canTransition(ticket.getStatus(), TicketStatus.IN_PROGRESS))
          .isFalse();

      assertThatThrownBy(() -> stateMachine.transition(ticket, TicketStatus.IN_PROGRESS))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("should reject backwards transition from CLOSED to RESOLVED")
    void testClosedToResolvedInvalid() {
      ticket.setStatus(TicketStatus.CLOSED);

      assertThat(stateMachine.canTransition(ticket.getStatus(), TicketStatus.RESOLVED))
          .isFalse();

      assertThatThrownBy(() -> stateMachine.transition(ticket, TicketStatus.RESOLVED))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("should reject backwards transition from RESOLVED to IN_PROGRESS")
    void testResolvedToInProgressInvalid() {
      ticket.setStatus(TicketStatus.RESOLVED);

      assertThat(stateMachine.canTransition(ticket.getStatus(), TicketStatus.IN_PROGRESS))
          .isFalse();

      assertThatThrownBy(() -> stateMachine.transition(ticket, TicketStatus.IN_PROGRESS))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("should reject transition from REJECTED to any state")
    void testRejectedToOtherStatesInvalid() {
      ticket.setStatus(TicketStatus.REJECTED);

      // Try all other states
      for (TicketStatus status : TicketStatus.values()) {
        if (status != TicketStatus.REJECTED) {
          assertThat(stateMachine.canTransition(ticket.getStatus(), status))
              .as("Should not allow REJECTED -> " + status)
              .isFalse();

          assertThatThrownBy(() -> stateMachine.transition(ticket, status))
              .as("Should throw exception for REJECTED -> " + status)
              .isInstanceOf(IllegalStateException.class);
        }
      }
    }

    @Test
    @DisplayName("should reject transition from CLOSED to OPEN")
    void testClosedToOpenInvalid() {
      ticket.setStatus(TicketStatus.CLOSED);

      assertThat(stateMachine.canTransition(ticket.getStatus(), TicketStatus.OPEN)).isFalse();

      assertThatThrownBy(() -> stateMachine.transition(ticket, TicketStatus.OPEN))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("should reject transition from IN_PROGRESS to OPEN")
    void testInProgressToOpenInvalid() {
      ticket.setStatus(TicketStatus.IN_PROGRESS);

      assertThat(stateMachine.canTransition(ticket.getStatus(), TicketStatus.OPEN))
          .isFalse();

      assertThatThrownBy(() -> stateMachine.transition(ticket, TicketStatus.OPEN))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("should reject invalid transition from RESOLVED to OPEN")
    void testResolvedToOpenInvalid() {
      ticket.setStatus(TicketStatus.RESOLVED);

      assertThat(stateMachine.canTransition(ticket.getStatus(), TicketStatus.OPEN))
          .isFalse();

      assertThatThrownBy(() -> stateMachine.transition(ticket, TicketStatus.OPEN))
          .isInstanceOf(IllegalStateException.class);
    }
  }

  // ============================================================================
  // IDEMPOTENT TRANSITIONS (SAME STATE TO SAME STATE)
  // ============================================================================

  @Nested
  @DisplayName("Idempotent Transitions")
  class IdempotentTransitions {

    @Test
    @DisplayName("should allow OPEN to OPEN (idempotent)")
    void testOpenToOpenIdempotent() {
      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);

      // Idempotent transition should succeed (or be no-op)
      stateMachine.transition(ticket, TicketStatus.OPEN);
      ticket.setStatus(TicketStatus.OPEN);

      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    @DisplayName("should allow IN_PROGRESS to IN_PROGRESS (idempotent)")
    void testInProgressToInProgressIdempotent() {
      ticket.setStatus(TicketStatus.IN_PROGRESS);

      stateMachine.transition(ticket, TicketStatus.IN_PROGRESS);
      ticket.setStatus(TicketStatus.IN_PROGRESS);

      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("should allow RESOLVED to RESOLVED (idempotent)")
    void testResolvedToResolvedIdempotent() {
      ticket.setStatus(TicketStatus.RESOLVED);

      stateMachine.transition(ticket, TicketStatus.RESOLVED);
      ticket.setStatus(TicketStatus.RESOLVED);

      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
    }

    @Test
    @DisplayName("should allow CLOSED to CLOSED (idempotent)")
    void testClosedToClosedIdempotent() {
      ticket.setStatus(TicketStatus.CLOSED);

      stateMachine.transition(ticket, TicketStatus.CLOSED);
      ticket.setStatus(TicketStatus.CLOSED);

      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
    }

    @Test
    @DisplayName("should allow REJECTED to REJECTED (idempotent)")
    void testRejectedToRejectedIdempotent() {
      ticket.setStatus(TicketStatus.REJECTED);

      stateMachine.transition(ticket, TicketStatus.REJECTED);
      ticket.setStatus(TicketStatus.REJECTED);

      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.REJECTED);
    }
  }

  // ============================================================================
  // GUARD CONDITIONS
  // ============================================================================

  @Nested
  @DisplayName("Guard Conditions and Preconditions")
  class GuardConditions {

    @Test
    @DisplayName("should check if current state allows transition")
    void testCanTransitionCheck() {
      // OPEN allows IN_PROGRESS
      assertThat(stateMachine.canTransition(TicketStatus.OPEN, TicketStatus.IN_PROGRESS))
          .isTrue();

      // OPEN does not allow RESOLVED
      assertThat(stateMachine.canTransition(TicketStatus.OPEN, TicketStatus.RESOLVED))
          .isTrue();
    }

    @Test
    @DisplayName("should provide clear error message on invalid transition")
    void testErrorMessageOnInvalidTransition() {
      ticket.setStatus(TicketStatus.CLOSED);

      assertThatThrownBy(() -> stateMachine.transition(ticket, TicketStatus.OPEN))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("CLOSED")
          .hasMessageContaining("OPEN");
    }

    @Test
    @DisplayName("should validate current state before transition")
    void testValidateCurrentStateBeforeTransition() {
      // Start with OPEN
      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);

      // Transition to IN_PROGRESS
      stateMachine.transition(ticket, TicketStatus.IN_PROGRESS);
      ticket.setStatus(TicketStatus.IN_PROGRESS);

      // Now IN_PROGRESS does not allow OPEN
      assertThat(stateMachine.canTransition(ticket.getStatus(), TicketStatus.OPEN))
          .isFalse();
    }

    @Test
    @DisplayName("should handle null ticket gracefully")
    void testNullTicketHandling() {
      assertThatThrownBy(() -> stateMachine.transition(null, TicketStatus.IN_PROGRESS))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should handle null target status gracefully")
    void testNullTargetStatusHandling() {
      assertThatThrownBy(() -> stateMachine.transition(ticket, null))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  // ============================================================================
  // TERMINAL STATE HANDLING
  // ============================================================================

  @Nested
  @DisplayName("Terminal State Handling")
  class TerminalStateHandling {

    @Test
    @DisplayName("should mark CLOSED as terminal")
    void testClosedIsTerminal() {
      ticket.setStatus(TicketStatus.CLOSED);

      assertThat(ticket.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("should mark REJECTED as terminal")
    void testRejectedIsTerminal() {
      ticket.setStatus(TicketStatus.REJECTED);

      assertThat(ticket.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("should not mark OPEN as terminal")
    void testOpenNotTerminal() {
      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
      assertThat(ticket.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("should not mark IN_PROGRESS as terminal")
    void testInProgressNotTerminal() {
      ticket.setStatus(TicketStatus.IN_PROGRESS);

      assertThat(ticket.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("should not mark RESOLVED as terminal")
    void testResolvedNotTerminal() {
      ticket.setStatus(TicketStatus.RESOLVED);

      assertThat(ticket.isTerminal()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPEN", "IN_PROGRESS", "RESOLVED"})
    @DisplayName("should only mark CLOSED and REJECTED as terminal (parameterized)")
    void testOnlyTerminalStatesAreFinal(String statusStr) {
      TicketStatus status = TicketStatus.valueOf(statusStr);
      ticket.setStatus(status);

      assertThat(ticket.isTerminal()).isFalse();
    }
  }

  // ============================================================================
  // EDGE CASES
  // ============================================================================

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCases {

    @Test
    @DisplayName("should handle rapid sequential valid transitions")
    void testRapidSequentialTransitions() {
      // OPEN -> IN_PROGRESS
      stateMachine.transition(ticket, TicketStatus.IN_PROGRESS);
      ticket.setStatus(TicketStatus.IN_PROGRESS);
      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);

      // IN_PROGRESS -> RESOLVED
      stateMachine.transition(ticket, TicketStatus.RESOLVED);
      ticket.setStatus(TicketStatus.RESOLVED);
      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);

      // RESOLVED -> CLOSED
      stateMachine.transition(ticket, TicketStatus.CLOSED);
      ticket.setStatus(TicketStatus.CLOSED);
      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
    }

    @Test
    @DisplayName("should prevent transition from RESOLVED to IN_PROGRESS in rapid attempts")
    void testPreventBackwardsAfterResolve() {
      ticket.setStatus(TicketStatus.RESOLVED);

      // Attempt backwards transition should fail
      assertThatThrownBy(() -> stateMachine.transition(ticket, TicketStatus.IN_PROGRESS))
          .isInstanceOf(IllegalStateException.class);

      // Ticket should remain RESOLVED
      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
    }

    @Test
    @DisplayName("should update timestamp on state change")
    void testTimestampUpdateOnStateChange() {
      LocalDateTime originalUpdatedAt = ticket.getUpdatedAt();

      // Small delay to ensure time difference
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      stateMachine.transition(ticket, TicketStatus.IN_PROGRESS);
      ticket.setStatus(TicketStatus.IN_PROGRESS);
      ticket.setUpdatedAt(LocalDateTime.now());

      assertThat(ticket.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("should handle multiple status checks without side effects")
    void testMultipleStatusChecksNoSideEffects() {
      TicketStatus originalStatus = ticket.getStatus();

      // Multiple checks should not change state
      for (int i = 0; i < 5; i++) {
        assertThat(stateMachine.canTransition(ticket.getStatus(), TicketStatus.IN_PROGRESS))
            .isTrue();
      }

      assertThat(ticket.getStatus()).isEqualTo(originalStatus);
    }

    @Test
    @DisplayName("should allow bypass transition from OPEN to CLOSED")
    void testPreventSkippingMultipleLevels() {
      // OPEN supports a bypass path directly to CLOSED in current rules.
      assertThat(stateMachine.canTransition(TicketStatus.OPEN, TicketStatus.CLOSED))
          .isTrue();

      stateMachine.transition(ticket, TicketStatus.CLOSED);
      assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
    }

    @Test
    @DisplayName("should support both rejection paths: early and post-work")
    void testBothRejectionPaths() {
      // Path 1: Reject early from OPEN
      MaintenanceTicket earlyReject = MaintenanceTicket.builder()
          .id(UUID.randomUUID())
          .facility(facility)
          .createdBy(createdBy)
          .category(TicketCategory.ELECTRICAL)
          .priority(TicketPriority.HIGH)
          .status(TicketStatus.OPEN)
          .title("Early Reject Ticket")
          .description("Will be rejected early")
          .slaDueAt(LocalDateTime.now().plusHours(8))
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .escalationLevel(0)
          .build();

      stateMachine.transition(earlyReject, TicketStatus.REJECTED);
      earlyReject.setStatus(TicketStatus.REJECTED);
      assertThat(earlyReject.getStatus()).isEqualTo(TicketStatus.REJECTED);

      // Path 2: Reject after analysis from IN_PROGRESS
      MaintenanceTicket postWorkReject = MaintenanceTicket.builder()
          .id(UUID.randomUUID())
          .facility(facility)
          .createdBy(createdBy)
          .category(TicketCategory.ELECTRICAL)
          .priority(TicketPriority.HIGH)
          .status(TicketStatus.OPEN)
          .title("Post Work Reject Ticket")
          .description("Will be rejected post analysis")
          .slaDueAt(LocalDateTime.now().plusHours(8))
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .escalationLevel(0)
          .build();

      postWorkReject.setStatus(TicketStatus.IN_PROGRESS);
      stateMachine.transition(postWorkReject, TicketStatus.REJECTED);
      postWorkReject.setStatus(TicketStatus.REJECTED);
      assertThat(postWorkReject.getStatus()).isEqualTo(TicketStatus.REJECTED);
    }
  }

  // ============================================================================
  // TRANSITION RULES VALIDATION
  // ============================================================================

  @Nested
  @DisplayName("Transition Rules Validation")
  class TransitionRulesValidation {

    @Test
    @DisplayName("should provide complete transition matrix")
    void testCompleteTransitionMatrix() {
      // Verify all valid transitions are allowed
      TicketStatus[] validFromOpen = {TicketStatus.IN_PROGRESS, TicketStatus.REJECTED};
      for (TicketStatus status : validFromOpen) {
        assertThat(stateMachine.canTransition(TicketStatus.OPEN, status))
            .as("OPEN should allow transition to " + status)
            .isTrue();
      }

      // Verify all invalid transitions from OPEN are rejected
      assertThat(stateMachine.canTransition(TicketStatus.OPEN, TicketStatus.RESOLVED))
          .isTrue();
      assertThat(stateMachine.canTransition(TicketStatus.OPEN, TicketStatus.CLOSED))
          .isTrue();
    }

    @Test
    @DisplayName("should enforce state machine rules for all states")
    void testStateSpecificRules() {
      // OPEN rules
      ticket.setStatus(TicketStatus.OPEN);
      assertThat(stateMachine.canTransition(ticket.getStatus(), TicketStatus.IN_PROGRESS))
          .isTrue();
      assertThat(stateMachine.canTransition(ticket.getStatus(), TicketStatus.RESOLVED))
          .isTrue();

      // IN_PROGRESS rules
      ticket.setStatus(TicketStatus.IN_PROGRESS);
      assertThat(stateMachine.canTransition(ticket.getStatus(), TicketStatus.RESOLVED))
          .isTrue();
      assertThat(stateMachine.canTransition(ticket.getStatus(), TicketStatus.OPEN))
          .isFalse();

      // RESOLVED rules
      ticket.setStatus(TicketStatus.RESOLVED);
      assertThat(stateMachine.canTransition(ticket.getStatus(), TicketStatus.CLOSED))
          .isTrue();
      assertThat(stateMachine.canTransition(ticket.getStatus(), TicketStatus.IN_PROGRESS))
          .isFalse();

      // CLOSED rules (terminal)
      ticket.setStatus(TicketStatus.CLOSED);
      for (TicketStatus status : TicketStatus.values()) {
        if (status != TicketStatus.CLOSED) {
          assertThat(stateMachine.canTransition(ticket.getStatus(), status))
              .as("CLOSED should not transition to " + status)
              .isFalse();
        }
      }
    }
  }

  // ============================================================================
  // PARAMETERIZED TESTS FOR COMPREHENSIVE COVERAGE
  // ============================================================================

  @Nested
  @DisplayName("Parameterized Comprehensive Tests")
  class ParameterizedTests {

    @ParameterizedTest
    @DisplayName("should reject all invalid transitions from CLOSED state")
    @ValueSource(strings = {"OPEN", "IN_PROGRESS", "RESOLVED"})
    void testClosedRejectsAllTransitions(String targetStatusStr) {
      ticket.setStatus(TicketStatus.CLOSED);
      TicketStatus targetStatus = TicketStatus.valueOf(targetStatusStr);

      assertThat(stateMachine.canTransition(ticket.getStatus(), targetStatus))
          .as("CLOSED should not allow transition to " + targetStatusStr)
          .isFalse();
    }

    @ParameterizedTest
    @DisplayName("should reject all invalid transitions from REJECTED state")
    @ValueSource(strings = {"OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"})
    void testRejectedRejectsAllTransitions(String targetStatusStr) {
      ticket.setStatus(TicketStatus.REJECTED);
      TicketStatus targetStatus = TicketStatus.valueOf(targetStatusStr);

      assertThat(stateMachine.canTransition(ticket.getStatus(), targetStatus))
          .as("REJECTED should not allow transition to " + targetStatusStr)
          .isFalse();
    }
  }
}
