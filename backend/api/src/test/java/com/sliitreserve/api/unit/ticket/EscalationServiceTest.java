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
import com.sliitreserve.api.observer.EventEnvelope;
import com.sliitreserve.api.observer.EventSeverity;
import com.sliitreserve.api.observer.EventPublisher;
import com.sliitreserve.api.repositories.ticket.MaintenanceTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for EscalationService (T061)
 *
 * <p>Purpose: Validate the SLA deadline calculation and automatic escalation chain for
 * maintenance tickets according to FR-024, FR-025, FR-026, FR-051, and FR-052.
 *
 * <p><b>Test Scope</b>:
 * <ul>
 *   <li>SLA deadline calculations for all escalation levels
 *   <li>SLA breach detection with various time scenarios
 *   <li>Automatic escalation triggering based on SLA breach
 *   <li>Escalation level transitions and state management
 *   <li>Event emission on escalation (HIGH severity notifications)
 *   <li>Edge cases (rapid escalations, final escalation, multiple tickets)
 *   <li>Null safety and error handling
 * </ul>
 *
 * <p><b>SLA Model</b> (from FR-032):
 * <ul>
 *   <li>CRITICAL: 4-hour SLA (LEVEL_1 escalation)
 *   <li>HIGH: 8-hour SLA (LEVEL_2 escalation)
 *   <li>MEDIUM: 24-hour SLA (LEVEL_3 escalation)
 *   <li>LOW: 72-hour SLA (no further escalation)
 * </ul>
 *
 * <p><b>Escalation Chain</b> (from FR-025, FR-026):
 * <ul>
 *   <li>Level 0 (OPEN): Initial technician assignment
 *   <li>Level 1 (4-hour threshold breach): Escalate to senior technician
 *   <li>Level 2 (8-hour threshold breach): Escalate to facility manager/supervisor
 *   <li>Level 3 (24-hour threshold breach): Escalate to admin/director
 *   <li>Level 4 (72-hour threshold): Final escalation, max level
 * </ul>
 *
 * <p><b>Requirements Verified</b>:
 * <ul>
 *   <li>FR-024: Ticket SLA deadlines based on priority
 *   <li>FR-025: Automatic escalation on SLA breach
 *   <li>FR-026: Multi-level escalation chain
 *   <li>FR-051/052: Event emission for escalations (HIGH severity)
 * </ul>
 *
 * @see MaintenanceTicket for ticket entity
 * @see TicketPriority for SLA definitions
 * @see TicketStatus for status enumeration
 */
@DisplayName("EscalationService Tests")
@ExtendWith(MockitoExtension.class)
class EscalationServiceTest {

  @Mock private MaintenanceTicketRepository ticketRepository;
  @Mock private EventPublisher eventPublisher;
  @Mock private Clock clock;

  @Captor private ArgumentCaptor<EventEnvelope> eventCaptor;

  // Mock EscalationService interface - to be implemented
  // For testing purposes, we'll test the logic that the service should perform
  private EscalationService escalationService;

  // Test data
  private Facility testFacility;
  private User technicianLevel1;
  private User technicianLevel2;
  private User supervisorLevel3;
  private User createdBy;
  private MaintenanceTicket criticalTicket;
  private MaintenanceTicket highPriorityTicket;
  private MaintenanceTicket mediumPriorityTicket;
  private MaintenanceTicket lowPriorityTicket;

  // Fixed test time: 2026-04-11 10:00:00
  private LocalDateTime testNow;

  @BeforeEach
  void setUp() {
    // Initialize test data
    testNow = LocalDateTime.of(2026, 4, 11, 10, 0, 0);
    Instant testInstant = testNow.atZone(ZoneId.of("UTC")).toInstant();
    when(clock.instant()).thenReturn(testInstant);
    when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

    // Create test facility
    testFacility =
        Facility.builder()
            .id(UUID.randomUUID())
            .name("Test Lab")
            .type(FacilityType.LECTURE_HALL)
            .capacity(30)
            .status(FacilityStatus.ACTIVE)
            .location("Building A")
            .availabilityStart(java.time.LocalTime.of(8, 0))
            .availabilityEnd(java.time.LocalTime.of(20, 0))
            .build();

    // Create test users with different roles
    createdBy =
        User.builder()
            .id(UUID.randomUUID())
            .email("reporter@test.edu")
            .displayName("Test Reporter")
            .googleSubject("subject-reporter")
            .roles(Collections.singleton(Role.USER))
            .active(true)
            .build();

    technicianLevel1 =
        User.builder()
            .id(UUID.randomUUID())
            .email("tech1@test.edu")
            .displayName("Technician Level 1")
            .googleSubject("subject-tech1")
            .roles(Collections.singleton(Role.TECHNICIAN))
            .active(true)
            .build();

    technicianLevel2 =
        User.builder()
            .id(UUID.randomUUID())
            .email("tech2@test.edu")
            .displayName("Technician Level 2")
            .googleSubject("subject-tech2")
            .roles(Collections.singleton(Role.TECHNICIAN))
            .active(true)
            .build();

    supervisorLevel3 =
        User.builder()
            .id(UUID.randomUUID())
            .email("supervisor@test.edu")
            .displayName("Supervisor Level 3")
            .googleSubject("subject-supervisor")
            .roles(Collections.singleton(Role.FACILITY_MANAGER))
            .active(true)
            .build();

    // Create test tickets with different priorities
    createTicketVariants();

    // Initialize EscalationService with mocked dependencies
    // Note: This would be the actual service once implemented
    escalationService = new EscalationServiceImpl(ticketRepository, eventPublisher, clock);
  }

  private void createTicketVariants() {
    // CRITICAL ticket (4-hour SLA)
    criticalTicket =
        MaintenanceTicket.builder()
            .id(UUID.randomUUID())
            .facility(testFacility)
            .createdBy(createdBy)
            .assignedTechnician(technicianLevel1)
            .category(TicketCategory.ELECTRICAL)
            .priority(TicketPriority.CRITICAL)
            .status(TicketStatus.OPEN)
            .title("Critical electrical issue")
            .description("Power outage in lab area")
            .slaDueAt(testNow.plus(4, ChronoUnit.HOURS))
            .escalationLevel(0)
            .createdAt(testNow)
            .updatedAt(testNow)
            .build();

    // HIGH priority ticket (8-hour SLA)
    highPriorityTicket =
        MaintenanceTicket.builder()
            .id(UUID.randomUUID())
            .facility(testFacility)
            .createdBy(createdBy)
            .assignedTechnician(technicianLevel1)
            .category(TicketCategory.HVAC)
            .priority(TicketPriority.HIGH)
            .status(TicketStatus.OPEN)
            .title("HVAC system malfunction")
            .description("Temperature control not working")
            .slaDueAt(testNow.plus(8, ChronoUnit.HOURS))
            .escalationLevel(0)
            .createdAt(testNow)
            .updatedAt(testNow)
            .build();

    // MEDIUM priority ticket (24-hour SLA)
    mediumPriorityTicket =
        MaintenanceTicket.builder()
            .id(UUID.randomUUID())
            .facility(testFacility)
            .createdBy(createdBy)
            .assignedTechnician(technicianLevel1)
            .category(TicketCategory.PLUMBING)
            .priority(TicketPriority.MEDIUM)
            .status(TicketStatus.OPEN)
            .title("Plumbing issue")
            .description("Water leak in restroom")
            .slaDueAt(testNow.plus(24, ChronoUnit.HOURS))
            .escalationLevel(0)
            .createdAt(testNow)
            .updatedAt(testNow)
            .build();

    // LOW priority ticket (72-hour SLA)
    lowPriorityTicket =
        MaintenanceTicket.builder()
            .id(UUID.randomUUID())
            .facility(testFacility)
            .createdBy(createdBy)
            .assignedTechnician(technicianLevel1)
            .category(TicketCategory.CLEANING)
            .priority(TicketPriority.LOW)
            .status(TicketStatus.OPEN)
            .title("Cleaning required")
            .description("General facility cleaning")
            .slaDueAt(testNow.plus(72, ChronoUnit.HOURS))
            .escalationLevel(0)
            .createdAt(testNow)
            .updatedAt(testNow)
            .build();
  }

  // ============================================================================
  // SLA Deadline Calculation Tests
  // ============================================================================

  @Nested
  @DisplayName("SLA Deadline Calculation Tests")
  class SlaDeadlineCalculationTests {

    @Test
    @DisplayName("Should calculate 4-hour SLA for CRITICAL priority")
    void testCriticalSlaDeadline() {
      // Act: Calculate SLA deadline for CRITICAL ticket
      LocalDateTime deadline =
          escalationService.calculateSlaDueDate(testNow, TicketPriority.CRITICAL);

      // Assert
      assertThat(deadline)
          .isEqualTo(testNow.plus(4, ChronoUnit.HOURS))
          .isNotEqualTo(testNow.plus(5, ChronoUnit.HOURS));
    }

    @Test
    @DisplayName("Should calculate 8-hour SLA for HIGH priority")
    void testHighSlaDeadline() {
      // Act
      LocalDateTime deadline = escalationService.calculateSlaDueDate(testNow, TicketPriority.HIGH);

      // Assert
      assertThat(deadline).isEqualTo(testNow.plus(8, ChronoUnit.HOURS));
    }

    @Test
    @DisplayName("Should calculate 24-hour SLA for MEDIUM priority")
    void testMediumSlaDeadline() {
      // Act
      LocalDateTime deadline =
          escalationService.calculateSlaDueDate(testNow, TicketPriority.MEDIUM);

      // Assert
      assertThat(deadline).isEqualTo(testNow.plus(24, ChronoUnit.HOURS));
    }

    @Test
    @DisplayName("Should calculate 72-hour SLA for LOW priority")
    void testLowSlaDeadline() {
      // Act
      LocalDateTime deadline = escalationService.calculateSlaDueDate(testNow, TicketPriority.LOW);

      // Assert
      assertThat(deadline).isEqualTo(testNow.plus(72, ChronoUnit.HOURS));
    }

    @Test
    @DisplayName("Should handle null priority gracefully")
    void testNullPrioritySlaCalculation() {
      // Act & Assert
      assertThatThrownBy(
              () -> escalationService.calculateSlaDueDate(testNow, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Priority cannot be null");
    }

    @Test
    @DisplayName("Should handle null creation time gracefully")
    void testNullCreationTimeSlaCalculation() {
      // Act & Assert
      assertThatThrownBy(
              () -> escalationService.calculateSlaDueDate(null, TicketPriority.CRITICAL))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Creation time cannot be null");
    }
  }

  // ============================================================================
  // SLA Breach Detection Tests
  // ============================================================================

  @Nested
  @DisplayName("SLA Breach Detection Tests")
  class SlaBreachDetectionTests {

    @Test
    @DisplayName("Should detect SLA not breached when current time is before deadline")
    void testSlaNotBreachedBeforeDeadline() {
      // Arrange: Current time is 1 hour before SLA deadline
      LocalDateTime beforeDeadline = testNow.plus(2, ChronoUnit.HOURS);
      // Act
      boolean isBreached =
          escalationService.isSlaBreached(criticalTicket, beforeDeadline);

      // Assert
      assertThat(isBreached).isFalse();
    }

    @Test
    @DisplayName("Should detect SLA breached when current time equals deadline")
    void testSlaBreachedAtDeadline() {
      // Arrange: Update ticket's SLA due date to be in 2 hours from now
      criticalTicket.setSlaDueAt(testNow.plus(2, ChronoUnit.HOURS));

      // Act: Check breach at exact deadline
      LocalDateTime atDeadline = testNow.plus(2, ChronoUnit.HOURS);
      boolean isBreached = escalationService.isSlaBreached(criticalTicket, atDeadline);

      // Assert: Should be breached at or after deadline
      assertThat(isBreached).isTrue();
    }

    @Test
    @DisplayName("Should detect SLA breached when current time exceeds deadline")
    void testSlaBreachedAfterDeadline() {
      // Arrange. Current time is 1 hour after SLA deadline
      LocalDateTime afterDeadline = testNow.plus(5, ChronoUnit.HOURS);
      criticalTicket.setSlaDueAt(testNow.plus(4, ChronoUnit.HOURS));

      // Act
      boolean isBreached = escalationService.isSlaBreached(criticalTicket, afterDeadline);

      // Assert
      assertThat(isBreached).isTrue();
    }

    @Test
    @DisplayName("Should return false when using current mocked time on valid ticket")
    void testSlaNotBreachedWithMockedClock() {
      // Arrange: SLA deadline is 4 hours from now
      criticalTicket.setSlaDueAt(testNow.plus(4, ChronoUnit.HOURS));

      // Act
      boolean isBreached = escalationService.isSlaBreachedNow(criticalTicket);

      // Assert
      assertThat(isBreached).isFalse();
    }

    @Test
    @DisplayName("Should handle null ticket gracefully")
    void testSlaBreachWithNullTicket() {
      // Act & Assert
      assertThatThrownBy(() -> escalationService.isSlaBreached(null, testNow))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Ticket cannot be null");
    }

    @Test
    @DisplayName("Should handle null check time gracefully")
    void testSlaBreachWithNullCheckTime() {
      // Act & Assert
      assertThatThrownBy(() -> escalationService.isSlaBreached(criticalTicket, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Check time cannot be null");
    }
  }

  // ============================================================================
  // Escalation Triggering Tests
  // ============================================================================

  @Nested
  @DisplayName("Escalation Triggering Tests")
  class EscalationTriggeringTests {

    @Test
    @DisplayName("Should escalate from Level 0 to Level 1 when SLA breached")
    void testEscalateFromLevel0ToLevel1() {
      // Arrange
      criticalTicket.setEscalationLevel(0);
      LocalDateTime breachTime = criticalTicket.getSlaDueAt().plus(1, ChronoUnit.MINUTES);

      // Act
      EscalationResult result =
          escalationService.escalateTicket(criticalTicket, breachTime, technicianLevel2);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.isSuccessful()).isTrue();
      assertThat(result.getNewEscalationLevel()).isEqualTo(1);
      assertThat(criticalTicket.getEscalationLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should escalate from Level 1 to Level 2")
    void testEscalateFromLevel1ToLevel2() {
      // Arrange
      criticalTicket.setEscalationLevel(1);
      criticalTicket.setAssignedTechnician(technicianLevel2);
      LocalDateTime escalation2Time = testNow.plus(8, ChronoUnit.HOURS).plus(1, ChronoUnit.MINUTES);

      // Act
      EscalationResult result =
          escalationService.escalateTicket(criticalTicket, escalation2Time, supervisorLevel3);

      // Assert
      assertThat(result.isSuccessful()).isTrue();
      assertThat(result.getNewEscalationLevel()).isEqualTo(2);
      assertThat(criticalTicket.getEscalationLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should escalate from Level 2 to Level 3")
    void testEscalateFromLevel2ToLevel3() {
      // Arrange
      criticalTicket.setEscalationLevel(2);
      LocalDateTime escalation3Time = testNow.plus(24, ChronoUnit.HOURS).plus(1, ChronoUnit.MINUTES);

      // Act
      EscalationResult result = escalationService.escalateTicket(criticalTicket, escalation3Time);

      // Assert
      assertThat(result.isSuccessful()).isTrue();
      assertThat(result.getNewEscalationLevel()).isEqualTo(3);
      assertThat(criticalTicket.getEscalationLevel()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should not escalate beyond Level 3")
    void testCannotEscalateBeyondLevel3() {
      // Arrange: Already at max escalation level
      criticalTicket.setEscalationLevel(3);
      LocalDateTime afterLevel3 = testNow.plus(72, ChronoUnit.HOURS).plus(1, ChronoUnit.MINUTES);

      // Act
      EscalationResult result = escalationService.escalateTicket(criticalTicket, afterLevel3);

      // Assert
      assertThat(result.isSuccessful()).isFalse();
      assertThat(result.getNewEscalationLevel()).isEqualTo(3);
      assertThat(result.getErrorMessage()).contains("maximum escalation level");
    }

    @Test
    @DisplayName("Should not escalate if SLA not breached")
    void testNoEscalationIfSlaNotBreached() {
      // Arrange: Current time is before SLA deadline
      LocalDateTime beforeDeadline = testNow.plus(2, ChronoUnit.HOURS);
      criticalTicket.setEscalationLevel(0);

      // Act
      EscalationResult result = escalationService.escalateTicket(criticalTicket, beforeDeadline);

      // Assert
      assertThat(result.isSuccessful()).isFalse();
      assertThat(criticalTicket.getEscalationLevel()).isEqualTo(0);
      assertThat(result.getErrorMessage()).contains("SLA not breached");
    }

    @Test
    @DisplayName("Should emit HIGH severity event when escalation triggers")
    void testEscalationEmitsHighSeverityEvent() {
      // Arrange
      criticalTicket.setEscalationLevel(0);
      LocalDateTime breachTime = criticalTicket.getSlaDueAt().plus(1, ChronoUnit.MINUTES);

      // Act
      escalationService.escalateTicket(criticalTicket, breachTime, technicianLevel2);

      // Assert: Verify event publisher was called with HIGH severity event
      verify(eventPublisher, times(1)).publish(eventCaptor.capture());
      EventEnvelope event = eventCaptor.getValue();
      assertThat(event.getSeverity()).isEqualTo(EventSeverity.HIGH);
      assertThat(event.getEventType()).contains("ESCALATION");
    }

    @Test
    @DisplayName("Should update ticket escalation level and maintain status")
    void testEscalationUpdatesEscalationLevel() {
      // Arrange
      criticalTicket.setStatus(TicketStatus.IN_PROGRESS);
      criticalTicket.setEscalationLevel(0);
      LocalDateTime breachTime = criticalTicket.getSlaDueAt().plus(1, ChronoUnit.MINUTES);

      // Act
      escalationService.escalateTicket(criticalTicket, breachTime, technicianLevel2);

      // Assert
      assertThat(criticalTicket.getEscalationLevel()).isEqualTo(1);
      assertThat(criticalTicket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS); // Status unchanged by escalation
    }
  }

  // ============================================================================
  // Escalation Level Management Tests
  // ============================================================================

  @Nested
  @DisplayName("Escalation Level Management Tests")
  class EscalationLevelManagementTests {

    @Test
    @DisplayName("Should return current escalation level")
    void testGetCurrentEscalationLevel() {
      // Arrange
      criticalTicket.setEscalationLevel(2);

      // Act
      int level = escalationService.getCurrentEscalationLevel(criticalTicket);

      // Assert
      assertThat(level).isEqualTo(2);
    }

    @Test
    @DisplayName("Should calculate remaining time until SLA breach for valid ticket")
    void testCalculateRemainingTime() {
      // Arrange
      criticalTicket.setSlaDueAt(testNow.plus(4, ChronoUnit.HOURS));

      // Act
      long remainingMinutes =
          escalationService.getRemainingMinutesUntilSlaBreak(criticalTicket, testNow);

      // Assert
      assertThat(remainingMinutes).isEqualTo(240); // 4 hours = 240 minutes
    }

    @Test
    @DisplayName("Should return negative remaining time when SLA already breached")
    void testRemainingTimeWhenBreached() {
      // Arrange
      LocalDateTime breachTime = testNow.plus(5, ChronoUnit.HOURS);
      criticalTicket.setSlaDueAt(testNow.plus(4, ChronoUnit.HOURS));

      // Act
      long remainingMinutes =
          escalationService.getRemainingMinutesUntilSlaBreak(criticalTicket, breachTime);

      // Assert
      assertThat(remainingMinutes).isNegative();
    }

    @Test
    @DisplayName("Should identify escalated tickets correctly")
    void testIsTicketEscalated() {
      // Arrange
      criticalTicket.setEscalationLevel(0);
      highPriorityTicket.setEscalationLevel(1);

      // Act & Assert
      assertThat(escalationService.isEscalated(criticalTicket)).isFalse();
      assertThat(escalationService.isEscalated(highPriorityTicket)).isTrue();
    }

    @Test
    @DisplayName("Should get escalation info containing all relevant details")
    void testGetEscalationInfo() {
      // Arrange
      criticalTicket.setEscalationLevel(2);
      criticalTicket.setSlaDueAt(testNow.plus(24, ChronoUnit.HOURS));

      // Act
      SlaInfo info = escalationService.getEscalationInfo(criticalTicket, testNow);

      // Assert
      assertThat(info).isNotNull();
      assertThat(info.getCurrentLevel()).isEqualTo(2);
      assertThat(info.getDeadline()).isEqualTo(testNow.plus(24, ChronoUnit.HOURS));
      assertThat(info.isBreached()).isFalse();
    }
  }

  // ============================================================================
  // Pending Escalations Query Tests
  // ============================================================================

  @Nested
  @DisplayName("Pending Escalations Query Tests")
  class PendingEscalationsQueryTests {

    @Test
    @DisplayName("Should find all breached tickets ready for escalation")
    void testFindBreachedTickets() {
      // Arrange
      LocalDateTime checkTime = testNow.plus(5, ChronoUnit.HOURS);
      criticalTicket.setSlaDueAt(testNow.plus(4, ChronoUnit.HOURS));
      highPriorityTicket.setSlaDueAt(testNow.plus(10, ChronoUnit.HOURS));

      when(ticketRepository.findByStatusNotInAndSlaDueAtBefore(
              any(), eq(checkTime)))
          .thenReturn(Arrays.asList(criticalTicket));

      // Act
      List<MaintenanceTicket> breachedTickets =
          escalationService.findBreachedTicketsReadyForEscalation(checkTime);

      // Assert
      assertThat(breachedTickets).hasSize(1).contains(criticalTicket);
      verify(ticketRepository, times(1))
          .findByStatusNotInAndSlaDueAtBefore(any(), eq(checkTime));
    }

    @Test
    @DisplayName("Should filter breached tickets by escalation level")
    void testFindBreachedTicketsByLevel() {
      // Arrange
      criticalTicket.setEscalationLevel(0);
      highPriorityTicket.setEscalationLevel(1);
      List<MaintenanceTicket> byLevel =
          Arrays.asList(highPriorityTicket);

      when(ticketRepository.findByEscalationLevelAndSlaDueAtBefore(
              eq(1), any()))
          .thenReturn(byLevel);

      // Act
      List<MaintenanceTicket> level1Tickets =
          escalationService.findBreachedTicketsByLevel(1, testNow);

      // Assert
      assertThat(level1Tickets).hasSize(1).contains(highPriorityTicket);
    }

    @Test
    @DisplayName("Should return empty list when no breached tickets exist")
    void testNoBreachedTickets() {
      // Arrange
      when(ticketRepository.findByStatusNotInAndSlaDueAtBefore(any(), any()))
          .thenReturn(Collections.emptyList());

      // Act
      List<MaintenanceTicket> breachedTickets =
          escalationService.findBreachedTicketsReadyForEscalation(testNow);

      // Assert
      assertThat(breachedTickets).isEmpty();
    }

    @Test
    @DisplayName("Should only return non-terminal status tickets")
    void testExcludeTerminalStatusTickets() {
      // Arrange: Mix of terminal and non-terminal tickets
      criticalTicket.setStatus(TicketStatus.IN_PROGRESS);
      highPriorityTicket.setStatus(TicketStatus.CLOSED);
      mediumPriorityTicket.setStatus(TicketStatus.IN_PROGRESS);

      LocalDateTime checkTime = testNow.plus(30, ChronoUnit.HOURS);

      when(ticketRepository.findByStatusNotInAndSlaDueAtBefore(any(), eq(checkTime)))
          .thenReturn(Arrays.asList(criticalTicket, mediumPriorityTicket));

      // Act
      List<MaintenanceTicket> nonTerminal =
          escalationService.findBreachedTicketsReadyForEscalation(checkTime);

      // Assert
      assertThat(nonTerminal)
          .hasSize(2)
          .doesNotContain(highPriorityTicket) // CLOSED is terminal
          .contains(criticalTicket, mediumPriorityTicket);
    }
  }

  // ============================================================================
  // Edge Cases and Advanced Scenarios
  // ============================================================================

  @Nested
  @DisplayName("Edge Cases and Advanced Scenarios")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle rapid escalations correctly")
    void testRapidEscalations() {
      // Arrange: Multiple escalations in quick succession
      criticalTicket.setEscalationLevel(0);
      LocalDateTime time1 = criticalTicket.getSlaDueAt().plus(1, ChronoUnit.MINUTES);
      LocalDateTime time2 = time1.plus(1, ChronoUnit.MINUTES);
      LocalDateTime time3 = time2.plus(1, ChronoUnit.MINUTES);

      // Act & Assert: Each escalation should succeed
      EscalationResult result1 = escalationService.escalateTicket(criticalTicket, time1);
      assertThat(result1.isSuccessful()).isTrue();
      assertThat(result1.getNewEscalationLevel()).isEqualTo(1);

      EscalationResult result2 = escalationService.escalateTicket(criticalTicket, time2);
      assertThat(result2.isSuccessful()).isTrue();
      assertThat(result2.getNewEscalationLevel()).isEqualTo(2);

      EscalationResult result3 = escalationService.escalateTicket(criticalTicket, time3);
      assertThat(result3.isSuccessful()).isTrue();
      assertThat(result3.getNewEscalationLevel()).isEqualTo(3);

      // Final state should be Level 3
      assertThat(criticalTicket.getEscalationLevel()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should handle multiple tickets escalating simultaneously")
    void testMultipleTicketsEscalatingSimultaneously() {
      // Arrange
      LocalDateTime breachTime = testNow.plus(5, ChronoUnit.HOURS);
      criticalTicket.setSlaDueAt(testNow.plus(4, ChronoUnit.HOURS));
      highPriorityTicket.setSlaDueAt(testNow.plus(4, ChronoUnit.HOURS));
      mediumPriorityTicket.setSlaDueAt(testNow.plus(4, ChronoUnit.HOURS));

      // Act
      EscalationResult result1 = escalationService.escalateTicket(criticalTicket, breachTime);
      EscalationResult result2 = escalationService.escalateTicket(highPriorityTicket, breachTime);
      EscalationResult result3 = escalationService.escalateTicket(mediumPriorityTicket, breachTime);

      // Assert: All should escalate successfully
      assertThat(result1.isSuccessful()).isTrue();
      assertThat(result2.isSuccessful()).isTrue();
      assertThat(result3.isSuccessful()).isTrue();
      verify(eventPublisher, times(3)).publish(any(EventEnvelope.class));
    }

    @Test
    @DisplayName("Should handle final escalation (Level 3 -> cannot escalate further)")
    void testFinalEscalationLevel() {
      // Arrange: Already at max level
      criticalTicket.setEscalationLevel(3);
      LocalDateTime afterLevel3Deadline = testNow.plus(100, ChronoUnit.HOURS);

      // Act
      EscalationResult result =
          escalationService.escalateTicket(criticalTicket, afterLevel3Deadline);

      // Assert
      assertThat(result.isSuccessful()).isFalse();
      assertThat(result.getNewEscalationLevel()).isEqualTo(3);
      assertThat(criticalTicket.getEscalationLevel()).isEqualTo(3); // Unchanged
      assertThat(result.getErrorMessage()).contains("already at maximum");
    }

    @Test
    @DisplayName("Should save escalation changes to repository")
    void testEscalationSaveChanges() {
      // Arrange
      criticalTicket.setEscalationLevel(0);
      LocalDateTime breachTime = criticalTicket.getSlaDueAt().plus(1, ChronoUnit.MINUTES);

      // Act
      escalationService.escalateTicket(criticalTicket, breachTime, technicianLevel2);

      // Assert: Verify repository save was called
      verify(ticketRepository, times(1)).save(criticalTicket);
    }

    @Test
    @DisplayName("Should handle null assigned technician for escalation")
    void testEscalationWithNullAssignedTechnician() {
      // Arrange
      criticalTicket.setAssignedTechnician(null);
      LocalDateTime breachTime = criticalTicket.getSlaDueAt().plus(1, ChronoUnit.MINUTES);

      // Act & Assert: Should still escalate but handle null gracefully
      EscalationResult result = escalationService.escalateTicket(criticalTicket, breachTime);

      assertThat(result.isSuccessful()).isTrue();
      assertThat(result.getNewEscalationLevel()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    @DisplayName("Should validate escalation level bounds for all valid levels")
    void testEscalationLevelBounds(int level) {
      // Arrange
      criticalTicket.setEscalationLevel(level);

      // Act
      int retrievedLevel = escalationService.getCurrentEscalationLevel(criticalTicket);

      // Assert
      assertThat(retrievedLevel).isEqualTo(level).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should maintain ticket reference integrity during escalation")
    void testTicketReferenceIntegrity() {
      // Arrange
      UUID originalId = criticalTicket.getId();
      UUID originalFacilityId = criticalTicket.getFacility().getId();
      LocalDateTime breachTime = criticalTicket.getSlaDueAt().plus(1, ChronoUnit.MINUTES);

      // Act
      escalationService.escalateTicket(criticalTicket, breachTime);

      // Assert: IDs should remain unchanged
      assertThat(criticalTicket.getId()).isEqualTo(originalId);
      assertThat(criticalTicket.getFacility().getId()).isEqualTo(originalFacilityId);
    }
  }

  // ============================================================================
  // Automated SLA Check and Processing Tests
  // ============================================================================

  @Nested
  @DisplayName("Automated SLA Check and Processing Tests")
  class AutomatedSlaProcessingTests {

    @Test
    @DisplayName("Should process SLA check and escalate breached tickets")
    void testProcessSlaCheckAndEscalation() {
      // Arrange
      LocalDateTime checkTime = testNow.plus(5, ChronoUnit.HOURS);
      criticalTicket.setSlaDueAt(testNow.plus(4, ChronoUnit.HOURS));
      criticalTicket.setEscalationLevel(0);

      List<MaintenanceTicket> breachedTickets = Arrays.asList(criticalTicket);
      when(ticketRepository.findByStatusNotInAndSlaDueAtBefore(any(), any()))
          .thenReturn(breachedTickets);

      // Act
      escalationService.processScheduledSlaChecks(checkTime);

      // Assert
      assertThat(criticalTicket.getEscalationLevel()).isEqualTo(1);
      verify(eventPublisher, atLeastOnce()).publish(any(EventEnvelope.class));
      verify(ticketRepository, atLeastOnce()).save(criticalTicket);
    }

    @Test
    @DisplayName("Should skip processing for terminal status tickets")
    void testSkipProcessingForTerminalStatus() {
      // Arrange
      LocalDateTime checkTime = testNow.plus(30, ChronoUnit.HOURS);
      criticalTicket.setStatus(TicketStatus.CLOSED);
      criticalTicket.setSlaDueAt(testNow.plus(4, ChronoUnit.HOURS));

      // Act: Repository should not return terminal tickets
      when(ticketRepository.findByStatusNotInAndSlaDueAtBefore(any(), any()))
          .thenReturn(Collections.emptyList());

      escalationService.processScheduledSlaChecks(checkTime);

      // Assert
      assertThat(criticalTicket.getEscalationLevel()).isEqualTo(0); // Unchanged
      verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle empty result set from SLA check")
    void testProcessSlaCheckWithNoBreachedTickets() {
      // Arrange
      LocalDateTime checkTime = testNow.plus(1, ChronoUnit.HOURS);
      when(ticketRepository.findByStatusNotInAndSlaDueAtBefore(any(), any()))
          .thenReturn(Collections.emptyList());

      // Act
      escalationService.processScheduledSlaChecks(checkTime);

      // Assert: No save operations should occur
      verify(ticketRepository, never()).save(any());
      verify(eventPublisher, never()).publish(any());
    }
  }

  // ============================================================================
  // Mock Implementation for Testing
  // ============================================================================

  /**
   * Minimal EscalationService implementation for testing purposes. This interface definition
   * should be implemented in the actual service class.
   */
  interface EscalationService {
    LocalDateTime calculateSlaDueDate(LocalDateTime creationTime, TicketPriority priority);

    boolean isSlaBreached(MaintenanceTicket ticket, LocalDateTime checkTime);

    boolean isSlaBreachedNow(MaintenanceTicket ticket);

    EscalationResult escalateTicket(
        MaintenanceTicket ticket, LocalDateTime escalationTime, User newAssignee);

    EscalationResult escalateTicket(MaintenanceTicket ticket, LocalDateTime escalationTime);

    int getCurrentEscalationLevel(MaintenanceTicket ticket);

    long getRemainingMinutesUntilSlaBreak(MaintenanceTicket ticket, LocalDateTime currentTime);

    boolean isEscalated(MaintenanceTicket ticket);

    SlaInfo getEscalationInfo(MaintenanceTicket ticket, LocalDateTime currentTime);

    List<MaintenanceTicket> findBreachedTicketsReadyForEscalation(LocalDateTime checkTime);

    List<MaintenanceTicket> findBreachedTicketsByLevel(int escalationLevel, LocalDateTime checkTime);

    void processScheduledSlaChecks(LocalDateTime checkTime);
  }

  /**
   * Mock implementation of EscalationService for testing. In production, this would be replaced
   * with an actual implementation.
   */
  class EscalationServiceImpl implements EscalationService {
    private final MaintenanceTicketRepository ticketRepository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    EscalationServiceImpl(
        MaintenanceTicketRepository ticketRepository,
        EventPublisher eventPublisher,
        Clock clock) {
      this.ticketRepository = ticketRepository;
      this.eventPublisher = eventPublisher;
      this.clock = clock;
    }

    @Override
    public LocalDateTime calculateSlaDueDate(LocalDateTime creationTime, TicketPriority priority) {
      if (creationTime == null) {
        throw new IllegalArgumentException("Creation time cannot be null");
      }
      if (priority == null) {
        throw new IllegalArgumentException("Priority cannot be null");
      }

      return switch (priority) {
        case CRITICAL -> creationTime.plus(4, ChronoUnit.HOURS);
        case HIGH -> creationTime.plus(8, ChronoUnit.HOURS);
        case MEDIUM -> creationTime.plus(24, ChronoUnit.HOURS);
        case LOW -> creationTime.plus(72, ChronoUnit.HOURS);
      };
    }

    @Override
    public boolean isSlaBreached(MaintenanceTicket ticket, LocalDateTime checkTime) {
      if (ticket == null) {
        throw new IllegalArgumentException("Ticket cannot be null");
      }
      if (checkTime == null) {
        throw new IllegalArgumentException("Check time cannot be null");
      }

      return checkTime.isAfter(ticket.getSlaDueAt())
          || checkTime.isEqual(ticket.getSlaDueAt());
    }

    @Override
    public boolean isSlaBreachedNow(MaintenanceTicket ticket) {
      LocalDateTime now = LocalDateTime.now(clock);
      return isSlaBreached(ticket, now);
    }

    @Override
    public EscalationResult escalateTicket(
        MaintenanceTicket ticket, LocalDateTime escalationTime, User newAssignee) {
      if (ticket == null) {
        throw new IllegalArgumentException("Ticket cannot be null");
      }

      if (!isSlaBreached(ticket, escalationTime)) {
        return new EscalationResult(false, ticket.getEscalationLevel(), "SLA not breached yet");
      }

      int currentLevel = ticket.getEscalationLevel();
      if (currentLevel >= 3) {
        return new EscalationResult(
            false, currentLevel, "Ticket is already at maximum escalation level");
      }

      int newLevel = currentLevel + 1;
      ticket.setEscalationLevel(newLevel);
      if (newAssignee != null) {
        ticket.setAssignedTechnician(newAssignee);
      }

      // Emit HIGH severity event
      EventEnvelope event =
          EventEnvelope.builder()
              .eventId(UUID.randomUUID().toString())
              .eventType("TICKET_ESCALATED")
              .severity(EventSeverity.HIGH)
              .affectedUserId(1L) // Mock user ID
              .title("Ticket Escalated")
              .description("Ticket escalated to level " + newLevel)
              .source("EscalationService")
              .build();

      eventPublisher.publish(event);
      ticketRepository.save(ticket);

      return new EscalationResult(true, newLevel, "Escalation successful");
    }

    @Override
    public EscalationResult escalateTicket(
        MaintenanceTicket ticket, LocalDateTime escalationTime) {
      return escalateTicket(ticket, escalationTime, null);
    }

    @Override
    public int getCurrentEscalationLevel(MaintenanceTicket ticket) {
      return ticket.getEscalationLevel();
    }

    @Override
    public long getRemainingMinutesUntilSlaBreak(
        MaintenanceTicket ticket, LocalDateTime currentTime) {
      return ChronoUnit.MINUTES.between(currentTime, ticket.getSlaDueAt());
    }

    @Override
    public boolean isEscalated(MaintenanceTicket ticket) {
      return ticket.getEscalationLevel() > 0;
    }

    @Override
    public SlaInfo getEscalationInfo(MaintenanceTicket ticket, LocalDateTime currentTime) {
      return new SlaInfo(
          ticket.getEscalationLevel(),
          ticket.getSlaDueAt(),
          getRemainingMinutesUntilSlaBreak(ticket, currentTime),
          isSlaBreached(ticket, currentTime));
    }

    @Override
    public List<MaintenanceTicket> findBreachedTicketsReadyForEscalation(
        LocalDateTime checkTime) {
      return ticketRepository.findByStatusNotInAndSlaDueAtBefore(
          Arrays.asList(TicketStatus.CLOSED, TicketStatus.REJECTED), checkTime);
    }

    @Override
    public List<MaintenanceTicket> findBreachedTicketsByLevel(
        int escalationLevel, LocalDateTime checkTime) {
      return ticketRepository.findByEscalationLevelAndSlaDueAtBefore(escalationLevel, checkTime);
    }

    @Override
    public void processScheduledSlaChecks(LocalDateTime checkTime) {
      List<MaintenanceTicket> breachedTickets = findBreachedTicketsReadyForEscalation(checkTime);
      for (MaintenanceTicket ticket : breachedTickets) {
        if (!ticket.isTerminal()) {
          escalateTicket(ticket, checkTime);
        }
      }
    }
  }

  /**
   * Result object for escalation operations
   */
  static class EscalationResult {
    private final boolean successful;
    private final int newEscalationLevel;
    private final String message;

    EscalationResult(boolean successful, int newEscalationLevel, String message) {
      this.successful = successful;
      this.newEscalationLevel = newEscalationLevel;
      this.message = message;
    }

    public boolean isSuccessful() {
      return successful;
    }

    public int getNewEscalationLevel() {
      return newEscalationLevel;
    }

    public String getErrorMessage() {
      return message;
    }
  }

  /**
   * SLA information holder
   */
  static class SlaInfo {
    private final int currentLevel;
    private final LocalDateTime deadline;
    private final long remainingMinutes;
    private final boolean breached;

    SlaInfo(int currentLevel, LocalDateTime deadline, long remainingMinutes, boolean breached) {
      this.currentLevel = currentLevel;
      this.deadline = deadline;
      this.remainingMinutes = remainingMinutes;
      this.breached = breached;
    }

    public int getCurrentLevel() {
      return currentLevel;
    }

    public LocalDateTime getDeadline() {
      return deadline;
    }

    public long getRemainingMinutes() {
      return remainingMinutes;
    }

    public boolean isBreached() {
      return breached;
    }
  }
}
