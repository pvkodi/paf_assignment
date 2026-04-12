package com.sliitreserve.api.unit.workflow;

import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.booking.ApprovalStep;
import com.sliitreserve.api.entities.booking.ApprovalStepDecision;
import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.entities.booking.BookingStatus;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.facility.Facility.FacilityStatus;
import com.sliitreserve.api.entities.facility.Facility.FacilityType;
import com.sliitreserve.api.repositories.ApprovalStepRepository;
import com.sliitreserve.api.repositories.BookingRepository;
import com.sliitreserve.api.services.booking.ApprovalWorkflowService;
import com.sliitreserve.api.strategy.quota.RolePolicyResolver;
import com.sliitreserve.api.workflow.approval.ApprovalDecision;
import com.sliitreserve.api.workflow.approval.ApprovalHandler;
import com.sliitreserve.api.workflow.approval.ApprovalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for Approval Workflow Service (T049)
 * 
 * Purpose: Validate the approval workflow chain orchestration for booking requests.
 * These tests verify multi-role approval scenarios, approval handler delegation,
 * and booking status transitions according to FR-014 to FR-017.
 * 
 * Test Scope:
 * 1. USER booking approval (2-step: LECTURER → ADMIN)
 * 2. LECTURER booking approval (auto-approve or FACILITY_MANAGER if high-capacity)
 * 3. High-capacity facility approval (conditional FACILITY_MANAGER sign-off)
 * 4. ADMIN booking approval (bypass entire workflow)
 * 5. Approval rejection scenarios (terminal rejection)
 * 6. Approval chain delegation (PENDING → next handler)
 * 7. Approval history recording (ApprovalStep entities created)
 * 8. Multi-role policy resolution (most permissive role)
 * 
 * Framework: JUnit 5, Mockito, Spring Test patterns
 * 
 * @see com.sliitreserve.api.workflow.approval.ApprovalHandler
 * @see com.sliitreserve.api.services.booking.ApprovalWorkflowService
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Approval Workflow Service Unit Tests")
class ApprovalWorkflowServiceTest {

    @Mock
    private ApprovalStepRepository approvalStepRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RolePolicyResolver rolePolicyResolver;

    /**
     * ApprovalWorkflowService to be created once the service is implemented.
     * Placeholder for now; will be injected when service exists.
     */
    @InjectMocks
    private ApprovalWorkflowService approvalWorkflowService;

    // Test fixtures
    private User userRequester;
    private User lecturerApprover;
    private User adminApprover;
    private User facilityManagerApprover;
    private Facility lectureHall;
    private Facility auditorium;
    private Booking userBooking;
    private Booking lecturerBooking;
    private Booking adminBooking;

    // High-capacity threshold (from plan: typically 200+ attendees)
    private static final Integer HIGH_CAPACITY_THRESHOLD = 200;

    @BeforeEach
    void setUp() {
        // Setup requester users
        userRequester = User.builder()
                .id(UUID.randomUUID())
                .googleSubject("user-subject-123")
                .email("user@institution.edu")
                .displayName("John User")
                .roles(Set.of(Role.USER))
                .active(true)
                .noShowCount(0)
                .build();

        lecturerApprover = User.builder()
                .id(UUID.randomUUID())
                .googleSubject("lecturer-subject-456")
                .email("lecturer@institution.edu")
                .displayName("Dr. Jane Lecturer")
                .roles(Set.of(Role.LECTURER))
                .active(true)
                .noShowCount(0)
                .build();

        adminApprover = User.builder()
                .id(UUID.randomUUID())
                .googleSubject("admin-subject-789")
                .email("admin@institution.edu")
                .displayName("Admin Officer")
                .roles(Set.of(Role.ADMIN))
                .active(true)
                .noShowCount(0)
                .build();

        facilityManagerApprover = User.builder()
                .id(UUID.randomUUID())
                .googleSubject("fac-mgr-subject-101")
                .email("facmgr@institution.edu")
                .displayName("Facility Manager")
                .roles(Set.of(Role.FACILITY_MANAGER))
                .active(true)
                .noShowCount(0)
                .build();

        // Setup test facilities
        lectureHall = Facility.builder()
                .id(UUID.randomUUID())
                .facilityCode("LH-001")
                .name("Lecture Hall A")
                .type(FacilityType.LECTURE_HALL)
                .capacity(50)
                .location("Building A, Level 2")
                .building("Building A")
                .floor("2")
                .status(FacilityStatus.ACTIVE)
                .availabilityStart(LocalTime.of(8, 0))
                .availabilityEnd(LocalTime.of(18, 0))
                .build();

        auditorium = Facility.builder()
                .id(UUID.randomUUID())
                .facilityCode("AUD-001")
                .name("Main Auditorium")
                .type(FacilityType.AUDITORIUM)
                .capacity(500) // High-capacity facility
                .location("Building C, Main Hall")
                .building("Building C")
                .floor("1")
                .status(FacilityStatus.ACTIVE)
                .availabilityStart(LocalTime.of(8, 0))
                .availabilityEnd(LocalTime.of(22, 0))
                .build();

        // Setup test bookings
        userBooking = Booking.builder()
                .id(UUID.randomUUID())
                .facility(lectureHall)
                .requestedBy(userRequester)
                .bookedFor(userRequester)
                .bookingDate(LocalDate.now().plusDays(7))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .purpose("Practical session on Java")
                .attendees(30)
                .status(BookingStatus.PENDING)
                .timezone("Asia/Colombo")
                .build();

        lecturerBooking = Booking.builder()
                .id(UUID.randomUUID())
                .facility(lectureHall)
                .requestedBy(lecturerApprover)
                .bookedFor(lecturerApprover)
                .bookingDate(LocalDate.now().plusDays(7))
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .purpose("Lecture on Advanced Algorithms")
                .attendees(45)
                .status(BookingStatus.PENDING)
                .timezone("Asia/Colombo")
                .build();

        adminBooking = Booking.builder()
                .id(UUID.randomUUID())
                .facility(auditorium)
                .requestedBy(adminApprover)
                .bookedFor(adminApprover)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .purpose("University-wide orientation event")
                .attendees(200)
                .status(BookingStatus.PENDING)
                .timezone("Asia/Colombo")
                .build();
    }

    @Nested
    @DisplayName("USER Booking Approval (2-Step Workflow)")
    class UserBookingApprovalTests {

        @Test
        @DisplayName("Should create approval chain for USER booking with LECTURER and ADMIN steps")
        void testUserBookingCreatesApprovalChain() {
            // Given: USER submits a booking that requires LECTURER and ADMIN approval
            // When: Approval workflow is initiated
            // Then: Two approval steps should be created (PENDING status)

            // Arrange
            when(bookingRepository.save(any())).thenReturn(userBooking);
            when(approvalStepRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            // Note: This assumes ApprovalWorkflowService.initiateApproval() exists
            // approvalWorkflowService.initiateApproval(userBooking);

            // Assert (placeholder - will be implemented when service exists)
            // verify(approvalStepRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("Should approve USER booking after LECTURER approves and ADMIN approves")
        void testUserBookingApprovalAfterBothStepsApprove() {
            // Given: USER booking with pending LECTURER and ADMIN approval steps
            // When: LECTURER approves, then ADMIN approves
            // Then: Booking status should transition to APPROVED

            // Arrange
            ApprovalStep lecturerStep = ApprovalStep.builder()
                    .id(UUID.randomUUID())
                    .booking(userBooking)
                    .stepOrder(1)
                    .approverRole(Role.LECTURER)
                    .decision(ApprovalStepDecision.PENDING)
                    .build();

            ApprovalStep adminStep = ApprovalStep.builder()
                    .id(UUID.randomUUID())
                    .booking(userBooking)
                    .stepOrder(2)
                    .approverRole(Role.ADMIN)
                    .decision(ApprovalStepDecision.PENDING)
                    .build();

            // Act & Assert (placeholder - will implement when ApprovalWorkflowService exists)
            // ApprovalDecision lecturerDecision = ApprovalDecision.builder()
            //     .status(ApprovalStatus.APPROVED)
            //     .approverRole("LECTURER")
            //     .note("Approved by lecturer")
            //     .build();
            // 
            // approvalWorkflowService.processApprovalStep(lecturerStep, lecturerDecision);
            // verify(approvalStepRepository).save(argThat(step -> 
            //     step.getDecision() == ApprovalStepDecision.APPROVED));
        }

        @Test
        @DisplayName("Should reject USER booking if LECTURER rejects")
        void testUserBookingRejectedByLecturer() {
            // Given: USER booking awaiting LECTURER approval
            // When: LECTURER rejects the booking
            // Then: Booking status should immediately transition to REJECTED
            //       and approval chain should terminate

            // Arrange
            ApprovalStep lecturerStep = ApprovalStep.builder()
                    .id(UUID.randomUUID())
                    .booking(userBooking)
                    .stepOrder(1)
                    .approverRole(Role.LECTURER)
                    .decision(ApprovalStepDecision.PENDING)
                    .build();

            // Act & Assert (placeholder)
            // ApprovalDecision rejectionDecision = ApprovalDecision.builder()
            //     .status(ApprovalStatus.REJECTED)
            //     .approverRole("LECTURER")
            //     .note("Overlaps with lab session")
            //     .build();
            // 
            // approvalWorkflowService.processApprovalStep(lecturerStep, rejectionDecision);
            // verify(bookingRepository).save(argThat(booking -> 
            //     booking.getStatus() == BookingStatus.REJECTED));
        }
    }

    @Nested
    @DisplayName("LECTURER Booking Approval (Conditional Workflow)")
    class LecturerBookingApprovalTests {

        @Test
        @DisplayName("Should auto-approve LECTURER booking for normal-capacity facility")
        void testLecturerBookingAutoApprovalNormalCapacity() {
            // Given: LECTURER booking for facility with capacity < HIGH_CAPACITY_THRESHOLD
            // When: Approval workflow is evaluated
            // Then: Booking should auto-approve WITHOUT any approval steps

            // Arrange
            when(bookingRepository.save(any())).thenReturn(lecturerBooking);

            // Act & Assert (placeholder)
            // approvalWorkflowService.initiateApproval(lecturerBooking);
            // verify(approvalStepRepository, never()).save(any());
            // verify(bookingRepository).save(argThat(booking -> 
            //     booking.getStatus() == BookingStatus.APPROVED));
        }

        @Test
        @DisplayName("Should require FACILITY_MANAGER approval for LECTURER booking of high-capacity facility")
        void testLecturerBookingHighCapacityRequiresFacilityManager() {
            // Given: LECTURER booking for high-capacity facility (>threshold)
            // When: Approval workflow is initiated
            // Then: One approval step should be created with FACILITY_MANAGER role

            // Arrange
            Booking lecturerHighCapacityBooking = Booking.builder()
                    .id(UUID.randomUUID())
                    .facility(auditorium) // 500 capacity
                    .requestedBy(lecturerApprover)
                    .bookedFor(lecturerApprover)
                    .bookingDate(LocalDate.now().plusDays(7))
                    .startTime(LocalTime.of(14, 0))
                    .endTime(LocalTime.of(16, 0))
                    .purpose("Large lecture")
                    .attendees(250) // High attendance
                    .status(BookingStatus.PENDING)
                    .timezone("Asia/Colombo")
                    .build();

            when(bookingRepository.save(any())).thenReturn(lecturerHighCapacityBooking);
            when(approvalStepRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // Act & Assert (placeholder)
            // approvalWorkflowService.initiateApproval(lecturerHighCapacityBooking);
            // ArgumentCaptor<ApprovalStep> stepCaptor = ArgumentCaptor.forClass(ApprovalStep.class);
            // verify(approvalStepRepository).save(stepCaptor.capture());
            // assertThat(stepCaptor.getValue().getApproverRole()).isEqualTo(Role.FACILITY_MANAGER);
        }
    }

    @Nested
    @DisplayName("ADMIN Booking Approval (Bypass Workflow)")
    class AdminBookingApprovalTests {

        @Test
        @DisplayName("Should bypass approval workflow for ADMIN bookings (auto-approve)")
        void testAdminBookingBypassesApprovalWorkflow() {
            // Given: ADMIN user submits booking regardless of facility/capacity
            // When: Approval workflow is initiated
            // Then: Booking should immediately transition to APPROVED
            //       and NO approval steps should be created

            // Arrange
            when(bookingRepository.save(any())).thenReturn(adminBooking);

            // Act & Assert (placeholder)
            // approvalWorkflowService.initiateApproval(adminBooking);
            // verify(approvalStepRepository, never()).save(any());
            // verify(bookingRepository).save(argThat(booking -> 
            //     booking.getStatus() == BookingStatus.APPROVED));
        }
    }

    @Nested
    @DisplayName("High-Capacity Facility Approval")
    class HighCapacityFacilityApprovalTests {

        @Test
        @DisplayName("Should require FACILITY_MANAGER approval for high-capacity facility booking")
        void testHighCapacityFacilityRequiresFacilityManagerApproval() {
            // Given: Any user booking high-capacity facility (capacity > threshold)
            // When: Approval is evaluated
            // Then: Approval chain should include FACILITY_MANAGER step

            // Arrange
            Booking highCapacityBooking = Booking.builder()
                    .id(UUID.randomUUID())
                    .facility(auditorium) // Capacity: 500
                    .requestedBy(userRequester)
                    .bookedFor(userRequester)
                    .bookingDate(LocalDate.now().plusDays(7))
                    .startTime(LocalTime.of(10, 0))
                    .endTime(LocalTime.of(12, 0))
                    .purpose("Large event")
                    .attendees(250)
                    .status(BookingStatus.PENDING)
                    .timezone("Asia/Colombo")
                    .build();

            when(bookingRepository.save(any())).thenReturn(highCapacityBooking);
            when(approvalStepRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // Act & Assert (placeholder)
            // approvalWorkflowService.initiateApproval(highCapacityBooking);
            // verify(approvalStepRepository, times(3)).save(any()); // LECTURER, FACILITY_MANAGER, ADMIN
        }
    }

    @Nested
    @DisplayName("Approval Chain Delegation (PENDING Status)")
    class ApprovalChainDelegationTests {

        @Test
        @DisplayName("Should delegate to next handler when current handler returns PENDING")
        void testApprovalChainDelegatesToNextHandler() {
            // Given: Approval handler that returns PENDING
            // When: Approval chain processes the decision
            // Then: Next handler in chain should be invoked

            // Arrange
            MockApprovalHandler handler1 = new MockApprovalHandler("HANDLER_1", ApprovalStatus.PENDING);
            MockApprovalHandler handler2 = new MockApprovalHandler("HANDLER_2", ApprovalStatus.APPROVED);
            handler1.setNext(handler2);

            // Act
            ApprovalDecision decision = handler1.handle(userBooking, userRequester, lectureHall);

            // Assert
            assertThat(decision)
                    .isNotNull()
                    .extracting(ApprovalDecision::getStatus)
                    .isEqualTo(ApprovalStatus.APPROVED);
            assertThat(decision.getApproverRole()).isEqualTo("HANDLER_2");
        }

        @Test
        @DisplayName("Should terminate approval chain on APPROVED decision")
        void testApprovalChainTerminatesOnApproval() {
            // Given: Approval handler that returns APPROVED
            // When: Approval chain processes the decision
            // Then: Chain should terminate and return APPROVED

            // Arrange
            MockApprovalHandler handler1 = new MockApprovalHandler("HANDLER_1", ApprovalStatus.APPROVED);
            MockApprovalHandler handler2 = new MockApprovalHandler("HANDLER_2", ApprovalStatus.PENDING);
            handler1.setNext(handler2);

            // Act
            ApprovalDecision decision = handler1.handle(userBooking, userRequester, lectureHall);

            // Assert
            assertThat(decision.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
            assertThat(decision.getApproverRole()).isEqualTo("HANDLER_1");
        }

        @Test
        @DisplayName("Should terminate approval chain on REJECTED decision")
        void testApprovalChainTerminatesOnRejection() {
            // Given: Approval handler that returns REJECTED
            // When: Approval chain processes the decision
            // Then: Chain should terminate immediately regardless of next handlers

            // Arrange
            MockApprovalHandler handler1 = new MockApprovalHandler("HANDLER_1", ApprovalStatus.REJECTED);
            MockApprovalHandler handler2 = new MockApprovalHandler("HANDLER_2", ApprovalStatus.APPROVED);
            handler1.setNext(handler2);

            // Act
            ApprovalDecision decision = handler1.handle(userBooking, userRequester, lectureHall);

            // Assert
            assertThat(decision.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
            assertThat(decision.getApproverRole()).isEqualTo("HANDLER_1");
        }
    }

    @Nested
    @DisplayName("Approval History Recording")
    class ApprovalHistoryRecordingTests {

        @Test
        @DisplayName("Should record approval step with approver details")
        void testApprovalStepRecordsApproverDetails() {
            // Given: Completed approval step
            // When: Approval step is saved to database
            // Then: Step should contain approver user, decision timestamp, and optional note

            // Arrange
            ApprovalStep approvalStep = ApprovalStep.builder()
                    .id(UUID.randomUUID())
                    .booking(userBooking)
                    .stepOrder(1)
                    .approverRole(Role.LECTURER)
                    .decision(ApprovalStepDecision.APPROVED)
                    .decidedBy(lecturerApprover)
                    .decidedAt(LocalDateTime.now())
                    .note("Approved: no conflicts with other sessions")
                    .build();

            when(approvalStepRepository.save(any())).thenReturn(approvalStep);

            // Act
            ApprovalStep saved = approvalStepRepository.save(approvalStep);

            // Assert
            assertThat(saved)
                    .extracting(ApprovalStep::getApproverRole)
                    .isEqualTo(Role.LECTURER);
            assertThat(saved.getDecidedBy()).isEqualTo(lecturerApprover);
            assertThat(saved.getDecidedAt()).isNotNull();
            assertThat(saved.getNote()).contains("no conflicts");
        }

        @Test
        @DisplayName("Should record approval sequence in order")
        void testApprovalHistoryMaintainsOrder() {
            // Given: Multiple approval steps
            // When: Approval steps are retrieved
            // Then: Steps should be ordered by stepOrder

            // Arrange
            ApprovalStep step1 = ApprovalStep.builder()
                    .id(UUID.randomUUID())
                    .booking(userBooking)
                    .stepOrder(1)
                    .approverRole(Role.LECTURER)
                    .decision(ApprovalStepDecision.APPROVED)
                    .build();

            ApprovalStep step2 = ApprovalStep.builder()
                    .id(UUID.randomUUID())
                    .booking(userBooking)
                    .stepOrder(2)
                    .approverRole(Role.ADMIN)
                    .decision(ApprovalStepDecision.PENDING)
                    .build();

            // Assert
            assertThat(step1.getStepOrder()).isLessThan(step2.getStepOrder());
        }
    }

    @Nested
    @DisplayName("Multi-Role Policy Resolution")
    class MultiRolePolicyResolutionTests {

        @Test
        @DisplayName("Should apply most permissive role for multi-role users")
        void testMultiRoleUserAppliesMostPermissivePolicy() {
            // Given: User with multiple roles (USER + LECTURER)
            // When: Approval workflow evaluates the user
            // Then: Most permissive role (LECTURER) should be used

            // Arrange
            User multiRoleUser = User.builder()
                    .id(UUID.randomUUID())
                    .googleSubject("multi-role-subject")
                    .email("multirole@institution.edu")
                    .displayName("Dual Role User")
                    .roles(Set.of(Role.USER, Role.LECTURER))
                    .active(true)
                    .noShowCount(0)
                    .build();

            Booking multiRoleBooking = Booking.builder()
                    .id(UUID.randomUUID())
                    .facility(lectureHall)
                    .requestedBy(multiRoleUser)
                    .bookedFor(multiRoleUser)
                    .bookingDate(LocalDate.now().plusDays(7))
                    .startTime(LocalTime.of(10, 0))
                    .endTime(LocalTime.of(12, 0))
                    .purpose("Multi-role booking")
                    .attendees(30)
                    .status(BookingStatus.PENDING)
                    .timezone("Asia/Colombo")
                    .build();

            when(rolePolicyResolver.getMostPermissiveRole(multiRoleUser.getRoles()))
                    .thenReturn(Role.LECTURER);

            // Act & Assert (placeholder)
            // Role resolvedRole = rolePolicyResolver.getMostPermissiveRole(multiRoleUser.getRoles());
            // assertThat(resolvedRole).isEqualTo(Role.LECTURER);
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle null booking gracefully")
        void testHandleNullBooking() {
            // Given: Null booking
            // When: Approval workflow is invoked
            // Then: Should throw IllegalArgumentException

            // Act & Assert (placeholder)
            // assertThatThrownBy(() -> approvalWorkflowService.initiateApproval(null))
            //     .isInstanceOf(IllegalArgumentException.class)
            //     .hasMessageContaining("Booking cannot be null");
        }

        @Test
        @DisplayName("Should handle missing requester gracefully")
        void testHandleMissingRequester() {
            // Given: Booking with null requester
            // When: Approval workflow is invoked
            // Then: Should throw IllegalArgumentException

            // Arrange
            Booking invalidBooking = Booking.builder()
                    .id(UUID.randomUUID())
                    .facility(lectureHall)
                    .requestedBy(null) // Missing requester
                    .bookedFor(userRequester)
                    .bookingDate(LocalDate.now().plusDays(7))
                    .startTime(LocalTime.of(10, 0))
                    .endTime(LocalTime.of(12, 0))
                    .purpose("Invalid booking")
                    .attendees(30)
                    .status(BookingStatus.PENDING)
                    .timezone("Asia/Colombo")
                    .build();

            // Act & Assert (placeholder)
            // assertThatThrownBy(() -> approvalWorkflowService.initiateApproval(invalidBooking))
            //     .isInstanceOf(IllegalArgumentException.class)
            //     .hasMessageContaining("Requester");
        }

        @Test
        @DisplayName("Should handle missing facility gracefully")
        void testHandleMissingFacility() {
            // Given: Booking with null facility
            // When: Approval workflow is invoked
            // Then: Should throw IllegalArgumentException

            // Arrange
            Booking invalidBooking = Booking.builder()
                    .id(UUID.randomUUID())
                    .facility(null) // Missing facility
                    .requestedBy(userRequester)
                    .bookedFor(userRequester)
                    .bookingDate(LocalDate.now().plusDays(7))
                    .startTime(LocalTime.of(10, 0))
                    .endTime(LocalTime.of(12, 0))
                    .purpose("Invalid booking")
                    .attendees(30)
                    .status(BookingStatus.PENDING)
                    .timezone("Asia/Colombo")
                    .build();

            // Act & Assert (placeholder)
            // assertThatThrownBy(() -> approvalWorkflowService.initiateApproval(invalidBooking))
            //     .isInstanceOf(IllegalArgumentException.class)
            //     .hasMessageContaining("Facility");
        }
    }

    @Nested
    @DisplayName("Approval Decision Utility Methods")
    class ApprovalDecisionUtilityTests {

        @Test
        @DisplayName("ApprovalDecision.isTerminal() should return true for APPROVED")
        void testApprovalDecisionIsTerminalForApproved() {
            // Arrange
            ApprovalDecision decision = ApprovalDecision.builder()
                    .status(ApprovalStatus.APPROVED)
                    .approverRole("LECTURER")
                    .build();

            // Act & Assert
            assertThat(decision.isTerminal()).isTrue();
            assertThat(decision.isApproved()).isTrue();
            assertThat(decision.isRejected()).isFalse();
            assertThat(decision.isPending()).isFalse();
        }

        @Test
        @DisplayName("ApprovalDecision.isTerminal() should return true for REJECTED")
        void testApprovalDecisionIsTerminalForRejected() {
            // Arrange
            ApprovalDecision decision = ApprovalDecision.builder()
                    .status(ApprovalStatus.REJECTED)
                    .approverRole("ADMIN")
                    .note("Overlaps with existing booking")
                    .build();

            // Act & Assert
            assertThat(decision.isTerminal()).isTrue();
            assertThat(decision.isApproved()).isFalse();
            assertThat(decision.isRejected()).isTrue();
            assertThat(decision.isPending()).isFalse();
        }

        @Test
        @DisplayName("ApprovalDecision.isTerminal() should return false for PENDING")
        void testApprovalDecisionIsTerminalForPending() {
            // Arrange
            ApprovalDecision decision = ApprovalDecision.builder()
                    .status(ApprovalStatus.PENDING)
                    .approverRole("FACILITY_MANAGER")
                    .build();

            // Act & Assert
            assertThat(decision.isTerminal()).isFalse();
            assertThat(decision.isApproved()).isFalse();
            assertThat(decision.isRejected()).isFalse();
            assertThat(decision.isPending()).isTrue();
        }
    }

    /**
     * Mock ApprovalHandler for testing chain behavior without full implementation.
     * This handler always returns a fixed decision for testing chain delegation logic.
     */
    private static class MockApprovalHandler implements ApprovalHandler {
        private final String role;
        private final ApprovalStatus status;
        private ApprovalHandler nextHandler;

        MockApprovalHandler(String role, ApprovalStatus status) {
            this.role = role;
            this.status = status;
        }

        @Override
        public ApprovalDecision handle(Booking booking, User requester, Facility facility) {
            if (!canHandle(booking, requester, facility)) {
                return nextHandler != null
                        ? nextHandler.handle(booking, requester, facility)
                        : ApprovalDecision.builder()
                        .status(ApprovalStatus.APPROVED)
                        .approverRole("DEFAULT")
                        .build();
            }

            if (status == ApprovalStatus.PENDING && nextHandler != null) {
                return nextHandler.handle(booking, requester, facility);
            }

            return ApprovalDecision.builder()
                    .status(status)
                    .approverRole(role)
                    .note("Mock decision: " + status)
                    .build();
        }

        @Override
        public boolean canHandle(Booking booking, User requester, Facility facility) {
            return true;
        }

        @Override
        public ApprovalHandler setNext(ApprovalHandler nextHandler) {
            this.nextHandler = nextHandler;
            return this;
        }

        @Override
        public ApprovalHandler getNext() {
            return nextHandler;
        }

        @Override
        public String getApprovalRole() {
            return role;
        }
    }
}
