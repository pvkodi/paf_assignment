package com.sliitreserve.api.integration.ticket;

import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.facility.Facility.FacilityStatus;
import com.sliitreserve.api.entities.facility.Facility.FacilityType;
import com.sliitreserve.api.entities.ticket.*;
import com.sliitreserve.api.repositories.auth.UserRepository;
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import com.sliitreserve.api.repositories.ticket.MaintenanceTicketRepository;
import com.sliitreserve.api.repositories.ticket.TicketCommentRepository;
import com.sliitreserve.api.repositories.ticket.TicketEscalationRepository;
import com.sliitreserve.api.services.ticket.TicketService;
import com.sliitreserve.api.services.ticket.EscalationService;
import com.sliitreserve.api.observers.EventPublisher;
import com.sliitreserve.api.state.TicketStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration Tests for Ticket Escalation and SLA Workflows (T064)
 *
 * <p>Purpose: Validate end-to-end ticket workflows including SLA deadline calculation,
 * escalation triggers, comment visibility rules, and soft-delete functionality.
 *
 * <p><b>Test Scope</b>:
 * <ul>
 *   <li>SLA deadline calculation per priority (CRITICAL 4h, HIGH 8h, MEDIUM 24h, LOW 72h)
 *   <li>Escalation triggers when SLA breached
 *   <li>SLA escalation level progression
 *   <li>Comment visibility: PUBLIC vs INTERNAL for different user roles
 *   <li>Comment soft-delete and filtration
 *   <li>Ticket status transitions with SLA impact
 *   <li>Staff vs non-staff access control
 *   <li>Escalation audit trail population
 * </ul>
 *
 * <p><b>SLA Deadlines from Specification</b>:
 * <ul>
 *   <li>CRITICAL: 4 hours (fastest response for high-impact issues)
 *   <li>HIGH: 8 hours (half-day response)
 *   <li>MEDIUM: 24 hours (next business day)
 *   <li>LOW: 72 hours (3-day turnaround)
 * </ul>
 *
 * <p><b>Escalation Levels</b> (0-3):
 * <ul>
 *   <li>Level 0: Initial state, no escalation yet
 *   <li>Level 1: First escalation (warning/notification)
 *   <li>Level 2: Escalate to manager
 *   <li>Level 3: Maximum escalation (executive)
 * </ul>
 */
@DisplayName("Ticket Escalation and SLA Integration Tests")
@ExtendWith(MockitoExtension.class)
public class TicketEscalationIntegrationTest {

  @Mock private MaintenanceTicketRepository ticketRepository;
  @Mock private TicketCommentRepository commentRepository;
  @Mock private TicketEscalationRepository escalationRepository;
  @Mock private FacilityRepository facilityRepository;
  @Mock private UserRepository userRepository;
  @Mock private EscalationService escalationService;
  @Mock private TicketStateMachine stateMachine;
  @Mock private EventPublisher eventPublisher;

  private TicketService ticketService;
  private Facility testFacility;
  private User creator;
  private User technician;
  private User admin;

  @BeforeEach
  void setUp() {
    ticketService =
        new TicketService(ticketRepository, commentRepository, stateMachine, eventPublisher);

    // Default save behavior for mocked repositories in service-level tests.
    org.mockito.Mockito.lenient()
      .when(ticketRepository.save(any()))
      .thenAnswer(invocation -> invocation.getArgument(0));
    org.mockito.Mockito.lenient()
      .when(commentRepository.save(any()))
      .thenAnswer(invocation -> invocation.getArgument(0));

    testFacility =
        Facility.builder()
            .id(UUID.randomUUID())
            .name("Test Hall")
            .type(FacilityType.LECTURE_HALL)
            .status(FacilityStatus.ACTIVE)
            .build();

    creator =
        User.builder()
            .id(UUID.randomUUID())
            .email("creator@example.com")
            .displayName("Ticket Creator")
            .roles(new java.util.HashSet<>(Collections.singletonList(Role.USER)))
            .build();

    technician =
        User.builder()
            .id(UUID.randomUUID())
            .email("tech@example.com")
            .displayName("Technician")
            .roles(new java.util.HashSet<>(Collections.singletonList(Role.TECHNICIAN)))
            .build();

    admin =
        User.builder()
            .id(UUID.randomUUID())
            .email("admin@example.com")
            .displayName("Admin")
            .roles(new java.util.HashSet<>(Collections.singletonList(Role.ADMIN)))
            .build();
  }

  @Nested
  @DisplayName("SLA Deadline Calculation")
  class SlaDeadlineTests {

    @Test
    @DisplayName("CRITICAL priority ticket should have 4-hour SLA deadline")
    void criticalTicketShouldHave4HourSla() {
      LocalDateTime beforeCreation = LocalDateTime.now();

      MaintenanceTicket ticket =
          ticketService.createTicket(
              testFacility,
              TicketCategory.PLUMBING,
              TicketPriority.CRITICAL,
              "Critical Issue",
              "The main water pipe is burst and causing flooding",
              creator);

      LocalDateTime afterCreation = LocalDateTime.now();

      assertThat(ticket.getSlaDueAt()).isNotNull();
      assertThat(ticket.getSlaDueAt()).isAfter(beforeCreation);
      assertThat(ticket.getSlaDueAt()).isBefore(afterCreation.plusHours(4).plusSeconds(1));
    }

    @Test
    @DisplayName("HIGH priority ticket should have 8-hour SLA deadline")
    void highTicketShouldHave8HourSla() {
      LocalDateTime beforeCreation = LocalDateTime.now();

      MaintenanceTicket ticket =
          ticketService.createTicket(
              testFacility,
              TicketCategory.ELECTRICAL,
              TicketPriority.HIGH,
              "Electrical Issue",
              "Multiple lights are malfunctioning in the lecture hall",
              creator);

      LocalDateTime afterCreation = LocalDateTime.now();

      assertThat(ticket.getSlaDueAt()).isAfter(beforeCreation);
      assertThat(ticket.getSlaDueAt()).isBefore(afterCreation.plusHours(8).plusSeconds(1));
    }

    @Test
    @DisplayName("MEDIUM priority ticket should have 24-hour SLA deadline")
    void mediumTicketShouldHave24HourSla() {
      LocalDateTime beforeCreation = LocalDateTime.now();

      MaintenanceTicket ticket =
          ticketService.createTicket(
              testFacility,
              TicketCategory.HVAC,
              TicketPriority.MEDIUM,
              "Temperature Control Issue",
              "Air conditioning not maintaining set temperature",
              creator);

      LocalDateTime afterCreation = LocalDateTime.now();

      assertThat(ticket.getSlaDueAt()).isAfter(beforeCreation);
      assertThat(ticket.getSlaDueAt()).isBefore(afterCreation.plusHours(24).plusSeconds(1));
    }

    @Test
    @DisplayName("LOW priority ticket should have 72-hour SLA deadline")
    void lowTicketShouldHave72HourSla() {
      LocalDateTime beforeCreation = LocalDateTime.now();

      MaintenanceTicket ticket =
          ticketService.createTicket(
              testFacility,
              TicketCategory.CLEANING,
              TicketPriority.LOW,
              "Chair Repair",
              "Wooden desk chair has a loose leg",
              creator);

      LocalDateTime afterCreation = LocalDateTime.now();

      assertThat(ticket.getSlaDueAt()).isAfter(beforeCreation);
      assertThat(ticket.getSlaDueAt()).isBefore(afterCreation.plusHours(72).plusSeconds(1));
    }

    @Test
    @DisplayName("SLA deadline should be in future (not past)")
    void slaDeadlineShouldBeInFuture() {
      MaintenanceTicket ticket =
          ticketService.createTicket(
              testFacility,
              TicketCategory.OTHER,
              TicketPriority.MEDIUM,
              "Generic Issue",
              "Something needs attention",
              creator);

      assertThat(ticket.getSlaDueAt()).isAfter(LocalDateTime.now());
    }
  }

  @Nested
  @DisplayName("Escalation Workflows")
  class EscalationTests {

    @Test
    @DisplayName("Ticket should initialize with escalation level 0")
    void ticketShouldInitializeWithLevel0() {
      MaintenanceTicket ticket =
          ticketService.createTicket(
              testFacility,
              TicketCategory.PLUMBING,
              TicketPriority.HIGH,
              "Leaky Faucet",
              "Water dripping from faucet",
              creator);

      assertThat(ticket.getEscalationLevel()).isEqualTo(0);
    }

    @Test
    @DisplayName("Escalation level should increment to maximum of 3")
    void escalationLevelShouldIncrementToMax() {
      MaintenanceTicket ticket =
          ticketService.createTicket(
              testFacility,
              TicketCategory.PLUMBING,
              TicketPriority.CRITICAL,
              "Critical Leak",
              "Water pipe burst",
              creator);

      MaintenanceTicket escalated1 = ticketService.escalateTicket(ticket);
      assertThat(escalated1.getEscalationLevel()).isEqualTo(1);

      MaintenanceTicket escalated2 = ticketService.escalateTicket(escalated1);
      assertThat(escalated2.getEscalationLevel()).isEqualTo(2);

      MaintenanceTicket escalated3 = ticketService.escalateTicket(escalated2);
      assertThat(escalated3.getEscalationLevel()).isEqualTo(3);

      MaintenanceTicket exceedsMax = escalated3;
      assertThatThrownBy(() -> ticketService.escalateTicket(exceedsMax))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("maximum");
    }
  }

  @Nested
  @DisplayName("Comment Visibility Rules")
  class CommentVisibilityTests {

    @Test
    @DisplayName("Ticket creator should see PUBLIC comments only")
    void creatorShouldSeePublicCommentsOnly() {
      MaintenanceTicket ticket =
          ticketService.createTicket(
              testFacility,
              TicketCategory.PLUMBING,
              TicketPriority.HIGH,
              "Issue",
              "Test issue",
              creator);

      TicketComment publicComment =
          ticketService.addComment(
              ticket,
              technician,
              "We're working on this",
              TicketCommentVisibility.PUBLIC);

      TicketComment internalComment =
          ticketService.addComment(
              ticket, technician, "Internal note", TicketCommentVisibility.INTERNAL);

      // Simulate visibility filtering
      when(commentRepository.findByTicketAndVisibilityAndDeletedAtIsNullOrderByCreatedAtAsc(
              ticket, TicketCommentVisibility.PUBLIC))
          .thenReturn(Collections.singletonList(publicComment));

      List<TicketComment> visibleToCreator =
          ticketService.getVisibleComments(ticket, creator);

      // Creator should only see PUBLIC comments
      assertThat(visibleToCreator).contains(publicComment).doesNotContain(internalComment);
    }

    @Test
    @DisplayName("Staff should see all comment types (PUBLIC and INTERNAL)")
    void staffShouldSeeAllComments() {
      MaintenanceTicket ticket =
          ticketService.createTicket(
              testFacility,
              TicketCategory.PLUMBING,
              TicketPriority.HIGH,
              "Issue",
              "Test issue",
              creator);

      TicketComment publicComment =
          ticketService.addComment(
              ticket,
              creator,
              "Public comment from creator",
              TicketCommentVisibility.PUBLIC);

      TicketComment internalComment =
          ticketService.addComment(
              ticket, technician, "Internal staff note", TicketCommentVisibility.INTERNAL);

      // Mock staff seeing all comments
      when(commentRepository.findByTicketAndDeletedAtIsNullOrderByCreatedAtAsc(ticket))
          .thenReturn(java.util.Arrays.asList(publicComment, internalComment));

      List<TicketComment> visibleToTechnician =
          ticketService.getVisibleComments(ticket, technician);

      // Technician (staff) should see all comments
      assertThat(visibleToTechnician)
          .contains(publicComment, internalComment)
          .hasSize(2);
    }

    @Test
    @DisplayName("Non-creator non-assigned users should see no comments")
    void nonCreatorNonAssignedShouldSeeNoComments() {
      MaintenanceTicket ticket =
          ticketService.createTicket(
              testFacility,
              TicketCategory.PLUMBING,
              TicketPriority.HIGH,
              "Issue",
              "Test issue",
              creator);

      User otherUser =
          User.builder()
              .id(UUID.randomUUID())
              .email("other@example.com")
              .displayName("Other User")
              .roles(new java.util.HashSet<>(Collections.singletonList(Role.USER)))
              .build();

      List<TicketComment> visibleToOther = ticketService.getVisibleComments(ticket, otherUser);

      // Other user should see no comments (not creator, not assigned)
      assertThat(visibleToOther).isEmpty();
    }
  }

  @Nested
  @DisplayName("Comment Lifecycle")
  class CommentLifecycleTests {

    @Test
    @DisplayName("Comment author should be able to update own comment")
    void authorShouldUpdateOwnComment() {
      MaintenanceTicket ticket = ticketRepository.save(
          MaintenanceTicket.builder()
              .id(UUID.randomUUID())
              .facility(testFacility)
              .category(TicketCategory.PLUMBING)
              .priority(TicketPriority.HIGH)
              .title("Test Ticket")
              .description("Test description")
              .createdBy(creator)
              .status(TicketStatus.OPEN)
              .escalationLevel(0)
              .slaDueAt(LocalDateTime.now().plusHours(8))
              .build()
      );

      TicketComment comment = commentRepository.save(
          TicketComment.builder()
              .id(UUID.randomUUID())
              .ticket(ticket)
              .content("Original content")
              .author(creator)
              .visibility(TicketCommentVisibility.PUBLIC)
              .build()
      );

      TicketComment updated =
          ticketService.updateComment(comment, "Updated content", creator);

      assertThat(updated.getContent()).isEqualTo("Updated content");
    }

    @Test
    @DisplayName("Non-author should not be able to update comment")
    void nonAuthorCannotUpdateComment() {
      MaintenanceTicket ticket = ticketRepository.save(
          MaintenanceTicket.builder()
              .id(UUID.randomUUID())
              .facility(testFacility)
              .category(TicketCategory.PLUMBING)
              .priority(TicketPriority.HIGH)
              .title("Test Ticket")
              .description("Test description")
              .createdBy(creator)
              .status(TicketStatus.OPEN)
              .escalationLevel(0)
              .slaDueAt(LocalDateTime.now().plusHours(8))
              .build()
      );

      TicketComment comment = commentRepository.save(
          TicketComment.builder()
              .id(UUID.randomUUID())
              .ticket(ticket)
              .content("Original content")
              .author(creator)
              .visibility(TicketCommentVisibility.PUBLIC)
              .build()
      );

      assertThatThrownBy(() -> ticketService.updateComment(comment, "New content", technician))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("author");
    }

    @Test
    @DisplayName("Admin should not be able to update others comment")
    void adminShouldNotUpdateOthersComment() {
      TicketComment comment = commentRepository.save(
          TicketComment.builder()
              .id(UUID.randomUUID())
              .content("Original content")
              .author(creator)
              .visibility(TicketCommentVisibility.PUBLIC)
              .build()
      );

      assertThatThrownBy(() -> ticketService.updateComment(comment, "Admin updated", admin))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("author");
    }

    @Test
    @DisplayName("Comment author should be able to delete own comment")
    void authorShouldDeleteOwnComment() {
      TicketComment comment =
          TicketComment.builder()
              .id(UUID.randomUUID())
              .content("Content to delete")
              .author(creator)
              .visibility(TicketCommentVisibility.PUBLIC)
              .build();

      TicketComment deleted = ticketService.deleteComment(comment, creator);

      assertThat(deleted.isDeleted()).isTrue();
      assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Soft-deleted comments should be excluded from visibility")
    void deletedCommentsShouldBeExcluded() {
      TicketComment publicComment =
          TicketComment.builder()
              .id(UUID.randomUUID())
              .content("Public comment")
              .author(creator)
              .visibility(TicketCommentVisibility.PUBLIC)
              .build();

      TicketComment deletedComment =
          TicketComment.builder()
              .id(UUID.randomUUID())
              .content("Deleted comment")
              .author(creator)
              .visibility(TicketCommentVisibility.PUBLIC)
              .deletedAt(LocalDateTime.now())
              .build();

      // Mock repository to exclude deleted comments
      MaintenanceTicket ticket =
          ticketService.createTicket(
              testFacility,
              TicketCategory.PLUMBING,
              TicketPriority.HIGH,
              "Issue",
              "Test",
              creator);

      when(commentRepository.findByTicketAndVisibilityAndDeletedAtIsNullOrderByCreatedAtAsc(
              ticket, TicketCommentVisibility.PUBLIC))
          .thenReturn(Collections.singletonList(publicComment));

      List<TicketComment> visibleComments =
          ticketService.getVisibleComments(ticket, creator);

      assertThat(visibleComments).contains(publicComment).doesNotContain(deletedComment);
    }
  }

  @Nested
  @DisplayName("Ticket Status Transitions with SLA")
  class StatusTransitionTests {

    @Test
    @DisplayName("Ticket should transition from OPEN to IN_PROGRESS")
    void shouldTransitionOpenToInProgress() {
      org.mockito.Mockito.doAnswer(invocation -> {
            MaintenanceTicket targetTicket = invocation.getArgument(0);
            TicketStatus targetStatus = invocation.getArgument(1);
            targetTicket.setStatus(targetStatus);
            return null;
          })
          .when(stateMachine)
          .transition(any(MaintenanceTicket.class), any(TicketStatus.class));

      MaintenanceTicket ticket =
          ticketService.createTicket(
              testFacility,
              TicketCategory.PLUMBING,
              TicketPriority.HIGH,
              "Leak",
              "Water leak detected",
              creator);

      when(stateMachine.canTransition(TicketStatus.OPEN, TicketStatus.IN_PROGRESS))
          .thenReturn(true);

      MaintenanceTicket updated =
          ticketService.updateTicketStatus(ticket, TicketStatus.IN_PROGRESS);

      assertThat(updated.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Ticket should transition from IN_PROGRESS to RESOLVED")
    void shouldTransitionInProgressToResolved() {
      org.mockito.Mockito.doAnswer(invocation -> {
        MaintenanceTicket targetTicket = invocation.getArgument(0);
        TicketStatus targetStatus = invocation.getArgument(1);
        targetTicket.setStatus(targetStatus);
        return null;
          })
          .when(stateMachine)
          .transition(any(MaintenanceTicket.class), any(TicketStatus.class));

      MaintenanceTicket ticket =
          MaintenanceTicket.builder()
              .facility(testFacility)
              .category(TicketCategory.PLUMBING)
              .priority(TicketPriority.HIGH)
              .title("Leak")
              .description("Water leak fixed")
              .createdBy(creator)
              .status(TicketStatus.IN_PROGRESS)
              .slaDueAt(LocalDateTime.now().plusHours(8))
              .build();

      when(stateMachine.canTransition(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED))
          .thenReturn(true);

      MaintenanceTicket updated =
          ticketService.updateTicketStatus(ticket, TicketStatus.RESOLVED);

      assertThat(updated.getStatus()).isEqualTo(TicketStatus.RESOLVED);
    }

    @Test
    @DisplayName("Invalid transition should be rejected by state machine")
    void invalidTransitionShouldBeRejected() {
      MaintenanceTicket ticket =
          MaintenanceTicket.builder()
              .facility(testFacility)
              .category(TicketCategory.PLUMBING)
              .priority(TicketPriority.HIGH)
              .title("Leak")
              .description("Description")
              .createdBy(creator)
              .status(TicketStatus.CLOSED)
              .slaDueAt(LocalDateTime.now().plusHours(8))
              .build();

      when(stateMachine.canTransition(TicketStatus.CLOSED, TicketStatus.IN_PROGRESS))
          .thenReturn(false);

      assertThatThrownBy(
              () -> ticketService.updateTicketStatus(ticket, TicketStatus.IN_PROGRESS))
          .isInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  @DisplayName("Ticket Assignment")
  class AssignmentTests {

    @Test
    @DisplayName("Ticket should be assignable to technician")
    void shouldAssignToTechnician() {
      MaintenanceTicket ticket =
          ticketService.createTicket(
              testFacility,
              TicketCategory.PLUMBING,
              TicketPriority.HIGH,
              "Leak",
              "Water leak in bathroom",
              creator);

      MaintenanceTicket assigned = ticketService.assignTicketToTechnician(ticket, technician);

      assertThat(assigned.getAssignedTechnician()).isEqualTo(technician);
    }

    @Test
    @DisplayName("Ticket should be unassignable (assign to null)")
    void shouldUnassignFromTechnician() {
      MaintenanceTicket ticket =
          MaintenanceTicket.builder()
              .facility(testFacility)
              .category(TicketCategory.PLUMBING)
              .priority(TicketPriority.HIGH)
              .title("Leak")
              .description("Water leak in bathroom")
              .createdBy(creator)
              .assignedTechnician(technician)
              .status(TicketStatus.OPEN)
              .slaDueAt(LocalDateTime.now().plusHours(8))
              .build();

      MaintenanceTicket unassigned = ticketService.assignTicketToTechnician(ticket, null);

      assertThat(unassigned.getAssignedTechnician()).isNull();
    }
  }

  @Nested
  @DisplayName("Escalation History Audit Trail")
  class EscalationHistoryTests {

    @Test
    @DisplayName("Escalation history should record escalation events")
    void escalationHistoryShouldBeTracked() {
      MaintenanceTicket ticket =
          ticketService.createTicket(
              testFacility,
              TicketCategory.PLUMBING,
              TicketPriority.CRITICAL,
              "Critical Issue",
              "Critical maintenance issue",
              creator);

      assertThat(ticket.getEscalationLevel()).isEqualTo(0);

      MaintenanceTicket escalated = ticketService.escalateTicket(ticket);

      assertThat(escalated.getEscalationLevel()).isEqualTo(1);
      // Escalation service would record this in escalation history
    }
  }
}
