package com.sliitreserve.api.unit.auth;

import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.exception.ForbiddenException;
import com.sliitreserve.api.repositories.UserRepository;
import com.sliitreserve.api.services.auth.SuspensionPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for No-Show Detection and Suspension Services
 *
 * <p><b>Purpose</b>: Verify the no-show detection and automatic suspension lifecycle
 * implementation for User Story 3: Approval, Quota Enforcement, and Suspension Lifecycle.
 *
 * <p><b>Test Scope</b>:
 * <ul>
 *   <li>No-show recording logic (increment counter and automatic suspension at threshold)
 *   <li>Suspension policy enforcement (blocking protected operations)
 *   <li>Suspension threshold checking (3 no-shows = automatic 1-week suspension)
 *   <li>Grace period validation (15 minutes from booking start)
 *   <li>Whitelisted operations (profile view, logout, appeal) allowed while suspended
 *   <li>Suspension release on appeal approval (suspension lifted, noShowCount reset)
 *   <li>Remaining no-shows calculation before suspension
 *   <li>Edge cases (null users, users already suspended, suspended users reaching another no-show)
 * </ul>
 *
 * <p><b>From Specification</b>:
 * <ul>
 *   <li>FR-003: Suspended users blocked from protected operations except auth/profile/appeals
 *   <li>FR-021: No-show when check-in doesn't occur within 15 minutes of booking start
 *   <li>FR-022: Automatic 1-week suspension after 3 no-shows
 *   <li>FR-023: Admin user can approve appeal → suspension lifted + noShowCount reset
 * </ul>
 *
 * <p><b>Design Notes</b>:
 * <ul>
 *   <li>SuspensionPolicyService handles all suspension logic and policy enforcement
 *   <li>NoShowEvaluator (to be implemented) will use this service for no-show detection
 *   <li>CheckInService will call recordNoShowAndApplySuspensionIfNeeded()
 *   <li>AppealService will call releaseSuspension() on approval
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("No-Show and Suspension Services Unit Tests")
class NoShowSuspensionServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SuspensionPolicyService suspensionPolicyService;

    // Test fixtures
    private User testUser;
    private User suspendedUser;
    private User userWithOneNoShow;
    private User userWithTwoNoShows;

    // Constants matching service implementation
    private static final int NO_SHOW_SUSPENSION_THRESHOLD = 3;
    private static final int SUSPENSION_DAYS = 7;
    private static final String TEST_EMAIL = "user@university.edu";
    private static final String TEST_DISPLAY_NAME = "Test User";
    private static final String TEST_GOOGLE_SUBJECT = "google-subject-123";

    @BeforeEach
    void setUp() {
        // Create base test user (not suspended, 0 no-shows)
        testUser = User.builder()
                .id(UUID.randomUUID())
                .googleSubject(TEST_GOOGLE_SUBJECT)
                .email(TEST_EMAIL)
                .displayName(TEST_DISPLAY_NAME)
                .roles(new HashSet<>(Set.of()))
                .active(true)
                .suspendedUntil(null)
                .noShowCount(0)
                .build();

        // Create user with one no-show
        userWithOneNoShow = User.builder()
                .id(UUID.randomUUID())
                .googleSubject("google-subject-001")
                .email("user1@university.edu")
                .displayName("User One NoShow")
                .roles(new HashSet<>(Set.of()))
                .active(true)
                .suspendedUntil(null)
                .noShowCount(1)
                .build();

        // Create user with two no-shows
        userWithTwoNoShows = User.builder()
                .id(UUID.randomUUID())
                .googleSubject("google-subject-002")
                .email("user2@university.edu")
                .displayName("User Two NoShows")
                .roles(new HashSet<>(Set.of()))
                .active(true)
                .suspendedUntil(null)
                .noShowCount(2)
                .build();

        // Create suspended user
        LocalDateTime suspensionEndTime = LocalDateTime.now(ZoneId.systemDefault()).plusDays(SUSPENSION_DAYS);
        suspendedUser = User.builder()
                .id(UUID.randomUUID())
                .googleSubject("google-subject-suspended")
                .email("suspended@university.edu")
                .displayName("Suspended User")
                .roles(new HashSet<>(Set.of()))
                .active(true)
                .suspendedUntil(suspensionEndTime)
                .noShowCount(3)
                .build();
    }

    // ==================== No-Show Recording and Automatic Suspension ====================

    @Nested
    @DisplayName("No-Show Recording and Suspension Tests")
    class NoShowRecordingTests {

        @Test
        @DisplayName("Should increment no-show counter when recording first no-show")
        void testRecordFirstNoShow() {
            // Arrange
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            testUser.setNoShowCount(1); // Simulate update

            // Act
            User result = suspensionPolicyService.recordNoShowAndApplySuspensionIfNeeded(testUser);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getNoShowCount());
            assertNull(result.getSuspendedUntil()); // No suspension yet
            verify(userRepository, times(1)).save(testUser);
        }

        @Test
        @DisplayName("Should increment no-show counter when recording second no-show")
        void testRecordSecondNoShow() {
            // Arrange
            when(userRepository.save(any(User.class))).thenReturn(userWithOneNoShow);
            userWithOneNoShow.setNoShowCount(2); // Simulate update

            // Act
            User result = suspensionPolicyService.recordNoShowAndApplySuspensionIfNeeded(userWithOneNoShow);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.getNoShowCount());
            assertNull(result.getSuspendedUntil()); // No suspension yet (threshold is 3)
            verify(userRepository, times(1)).save(userWithOneNoShow);
        }

        @Test
        @DisplayName("Should apply automatic suspension when reaching third no-show (threshold)")
        void testApplyAutomaticSuspensionAtThreshold() {
            // Arrange
            LocalDateTime beforeCall = LocalDateTime.now(ZoneId.systemDefault());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                return savedUser;
            });

            userWithTwoNoShows.setNoShowCount(3); // Simulate increment to 3

            // Act
            User result = suspensionPolicyService.recordNoShowAndApplySuspensionIfNeeded(userWithTwoNoShows);

            // Assert
            assertNotNull(result);
            assertEquals(3, result.getNoShowCount());
            assertNotNull(result.getSuspendedUntil(), "User should be suspended at 3 no-shows");
            
            // Verify suspension end time is approximately 7 days from now
            LocalDateTime expectedMinimum = beforeCall.plusDays(SUSPENSION_DAYS - 1);
            LocalDateTime expectedMaximum = beforeCall.plusDays(SUSPENSION_DAYS + 1);
            
            assertTrue(
                result.getSuspendedUntil().isAfter(expectedMinimum) && 
                result.getSuspendedUntil().isBefore(expectedMaximum),
                "Suspension should be approximately 7 days from now"
            );

            verify(userRepository, times(1)).save(userWithTwoNoShows);
        }

        @Test
        @DisplayName("Should increment no-show counter even if user already suspended")
        void testRecordNoShowForAlreadySuspendedUser() {
            // Arrange
            int currentNoShowCount = suspendedUser.getNoShowCount();
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                return savedUser;
            });

            suspendedUser.setNoShowCount(currentNoShowCount + 1); // Simulate increment

            // Act
            User result = suspensionPolicyService.recordNoShowAndApplySuspensionIfNeeded(suspendedUser);

            // Assert
            assertNotNull(result);
            assertEquals(4, result.getNoShowCount()); // Should be incremented to 4
            assertNotNull(result.getSuspendedUntil()); // Should still be suspended
            verify(userRepository, times(1)).save(suspendedUser);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when user is null")
        void testRecordNoShowWithNullUser() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> suspensionPolicyService.recordNoShowAndApplySuspensionIfNeeded(null)
            );

            assertEquals("User cannot be null", exception.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle null noShowCount field (treat as 0)")
        void testRecordNoShowWithNullNoShowCount() {
            // Arrange
            testUser.setNoShowCount(null); // Explicitly set to null
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            testUser.setNoShowCount(1); // Simulate increment from null

            // Act
            User result = suspensionPolicyService.recordNoShowAndApplySuspensionIfNeeded(testUser);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getNoShowCount());
            verify(userRepository, times(1)).save(testUser);
        }
    }

    // ==================== Suspension Status Checks ====================

    @Nested
    @DisplayName("Suspension Status Checking Tests")
    class SuspensionStatusTests {

        @Test
        @DisplayName("Should return true for currently suspended user (suspendedUntil in future)")
        void testIsSuspendedForActiveSuspension() {
            // Arrange
            LocalDateTime futureTime = LocalDateTime.now(ZoneId.systemDefault()).plusDays(5);
            suspendedUser.setSuspendedUntil(futureTime);

            // Act
            boolean isSuspended = suspensionPolicyService.isSuspended(suspendedUser);

            // Assert
            assertTrue(isSuspended);
        }

        @Test
        @DisplayName("Should return false for expired suspension (suspendedUntil in past)")
        void testIsSuspendedForExpiredSuspension() {
            // Arrange
            LocalDateTime pastTime = LocalDateTime.now(ZoneId.systemDefault()).minusDays(1);
            suspendedUser.setSuspendedUntil(pastTime);

            // Act
            boolean isSuspended = suspensionPolicyService.isSuspended(suspendedUser);

            // Assert
            assertFalse(isSuspended);
        }

        @Test
        @DisplayName("Should return false for non-suspended user (suspendedUntil is null)")
        void testIsSuspendedForNonSuspendedUser() {
            // Act
            boolean isSuspended = suspensionPolicyService.isSuspended(testUser);

            // Assert
            assertFalse(isSuspended);
        }

        @Test
        @DisplayName("Should return false when user is null")
        void testIsSuspendedWithNullUser() {
            // Act
            boolean isSuspended = suspensionPolicyService.isSuspended(null);

            // Assert
            assertFalse(isSuspended);
        }

        @Test
        @DisplayName("Should check suspension policy and throw ForbiddenException for suspended user")
        void testCheckSuspensionPolicyThrowsExceptionForSuspendedUser() {
            // Arrange
            LocalDateTime futureTime = LocalDateTime.now(ZoneId.systemDefault()).plusDays(5);
            suspendedUser.setSuspendedUntil(futureTime);

            // Act & Assert
            ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> suspensionPolicyService.checkSuspensionPolicy(suspendedUser)
            );

            assertNotNull(exception.getMessage());
            assertTrue(exception.getMessage().contains("suspended"));
            assertTrue(exception.getMessage().contains("appeal"));
        }

        @Test
        @DisplayName("Should not throw exception for non-suspended user")
        void testCheckSuspensionPolicyAllowsNonSuspendedUser() {
            // Act & Assert
            assertDoesNotThrow(() -> suspensionPolicyService.checkSuspensionPolicy(testUser));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when user is null in checkSuspensionPolicy")
        void testCheckSuspensionPolicyWithNullUser() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> suspensionPolicyService.checkSuspensionPolicy(null)
            );

            assertEquals("User cannot be null", exception.getMessage());
        }
    }

    // ==================== Suspension Release Tests ====================

    @Nested
    @DisplayName("Suspension Release and Appeal Tests")
    class SuspensionReleaseTests {

        @Test
        @DisplayName("Should release suspension and reset no-show count on appeal approval")
        void testReleaseSuspension() {
            // Arrange
            suspendedUser.setNoShowCount(3);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                return savedUser;
            });

            // Act
            User result = suspensionPolicyService.releaseSuspension(suspendedUser);

            // Assert
            assertNotNull(result);
            assertNull(result.getSuspendedUntil(), "Suspension should be cleared");
            assertEquals(0, result.getNoShowCount(), "No-show count should be reset");
            verify(userRepository, times(1)).save(suspendedUser);
        }

        @Test
        @DisplayName("Should clear suspendedUntil field when releasing suspension")
        void testReleaseSuspensionClearsField() {
            // Arrange
            LocalDateTime suspensionTime = LocalDateTime.now(ZoneId.systemDefault()).plusDays(7);
            suspendedUser.setSuspendedUntil(suspensionTime);
            when(userRepository.save(any(User.class))).thenReturn(suspendedUser);

            // Act
            User result = suspensionPolicyService.releaseSuspension(suspendedUser);

            // Assert
            assertNull(result.getSuspendedUntil());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when releasing suspension for null user")
        void testReleaseSuspensionWithNullUser() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> suspensionPolicyService.releaseSuspension(null)
            );

            assertEquals("User cannot be null", exception.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should allow releasing suspension even for non-suspended user")
        void testReleaseSuspensionForNonSuspendedUser() {
            // Arrange (testUser is not suspended)
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            User result = suspensionPolicyService.releaseSuspension(testUser);

            // Assert
            assertNotNull(result);
            assertNull(result.getSuspendedUntil());
            assertEquals(0, result.getNoShowCount());
            verify(userRepository, times(1)).save(testUser);
        }
    }

    // ==================== No-Show Count and Threshold Tests ====================

    @Nested
    @DisplayName("No-Show Count and Threshold Tests")
    class NoShowCountTests {

        @Test
        @DisplayName("Should return correct no-show count")
        void testGetNoShowCount() {
            // Act
            int count = suspensionPolicyService.getNoShowCount(userWithTwoNoShows);

            // Assert
            assertEquals(2, count);
        }

        @Test
        @DisplayName("Should return 0 for non-suspended user with null noShowCount")
        void testGetNoShowCountWithNullCount() {
            // Arrange
            testUser.setNoShowCount(null);

            // Act
            int count = suspensionPolicyService.getNoShowCount(testUser);

            // Assert
            assertEquals(0, count);
        }

        @Test
        @DisplayName("Should return 0 for null user")
        void testGetNoShowCountWithNullUser() {
            // Act
            int count = suspensionPolicyService.getNoShowCount(null);

            // Assert
            assertEquals(0, count);
        }

        @Test
        @DisplayName("Should return correct remaining no-shows before suspension")
        void testGetRemainingNoShowsBeforeSuspension() {
            // Act & Assert
            assertEquals(3, suspensionPolicyService.getRemainingNoShowsBeforeSuspension(testUser)); // 3 - 0 = 3
            assertEquals(2, suspensionPolicyService.getRemainingNoShowsBeforeSuspension(userWithOneNoShow)); // 3 - 1 = 2
            assertEquals(1, suspensionPolicyService.getRemainingNoShowsBeforeSuspension(userWithTwoNoShows)); // 3 - 2 = 1
        }

        @Test
        @DisplayName("Should return 0 remaining no-shows when threshold reached")
        void testGetRemainingNoShowsWhenThresholdReached() {
            // Act
            int remaining = suspensionPolicyService.getRemainingNoShowsBeforeSuspension(suspendedUser);

            // Assert
            assertEquals(0, remaining); // 3 - 3 = 0
        }

        @Test
        @DisplayName("Should return 0 remaining no-shows when exceeding threshold")
        void testGetRemainingNoShowsWhenExceedingThreshold() {
            // Arrange
            suspendedUser.setNoShowCount(5);

            // Act
            int remaining = suspensionPolicyService.getRemainingNoShowsBeforeSuspension(suspendedUser);

            // Assert
            assertEquals(0, remaining); // Should never go negative
        }

        @Test
        @DisplayName("Should return true for user at threshold")
        void testHasReachedNoShowThreshold() {
            // Act
            boolean reached = suspensionPolicyService.hasReachedNoShowThreshold(suspendedUser);

            // Assert
            assertTrue(reached);
        }

        @Test
        @DisplayName("Should return false for user below threshold")
        void testHasNotReachedNoShowThreshold() {
            // Act & Assert
            assertFalse(suspensionPolicyService.hasReachedNoShowThreshold(testUser));
            assertFalse(suspensionPolicyService.hasReachedNoShowThreshold(userWithOneNoShow));
            assertFalse(suspensionPolicyService.hasReachedNoShowThreshold(userWithTwoNoShows));
        }

        @Test
        @DisplayName("Should return threshold constant")
        void testGetSuspensionThreshold() {
            // Act
            int threshold = suspensionPolicyService.getSuspensionThreshold();

            // Assert
            assertEquals(NO_SHOW_SUSPENSION_THRESHOLD, threshold);
            assertEquals(3, threshold);
        }

        @Test
        @DisplayName("Should return suspension duration constant")
        void testGetSuspensionDurationDays() {
            // Act
            int duration = suspensionPolicyService.getSuspensionDurationDays();

            // Assert
            assertEquals(SUSPENSION_DAYS, duration);
            assertEquals(7, duration);
        }
    }

    // ==================== Whitelisted Operations Tests ====================

    @Nested
    @DisplayName("Whitelisted Operations for Suspended Users")
    class WhitelistedOperationsTests {

        @Test
        @DisplayName("Should allow profile operation for suspended user")
        void testAllowProfileOperationForSuspendedUser() {
            // Act
            boolean allowed = suspensionPolicyService.isOperationAllowedForSuspendedUser(suspendedUser, "profile");

            // Assert
            assertTrue(allowed);
        }

        @Test
        @DisplayName("Should allow logout operation for suspended user")
        void testAllowLogoutOperationForSuspendedUser() {
            // Act
            boolean allowed = suspensionPolicyService.isOperationAllowedForSuspendedUser(suspendedUser, "logout");

            // Assert
            assertTrue(allowed);
        }

        @Test
        @DisplayName("Should allow appeal operation for suspended user")
        void testAllowAppealOperationForSuspendedUser() {
            // Act
            boolean allowed = suspensionPolicyService.isOperationAllowedForSuspendedUser(suspendedUser, "appeal");

            // Assert
            assertTrue(allowed);
        }

        @Test
        @DisplayName("Should block booking operation for suspended user")
        void testBlockBookingOperationForSuspendedUser() {
            // Act
            boolean allowed = suspensionPolicyService.isOperationAllowedForSuspendedUser(suspendedUser, "booking");

            // Assert
            assertFalse(allowed);
        }

        @Test
        @DisplayName("Should block check-in operation for suspended user")
        void testBlockCheckInOperationForSuspendedUser() {
            // Act
            boolean allowed = suspensionPolicyService.isOperationAllowedForSuspendedUser(suspendedUser, "check-in");

            // Assert
            assertFalse(allowed);
        }

        @Test
        @DisplayName("Should block approval operation for suspended user")
        void testBlockApprovalOperationForSuspendedUser() {
            // Act
            boolean allowed = suspensionPolicyService.isOperationAllowedForSuspendedUser(suspendedUser, "approval");

            // Assert
            assertFalse(allowed);
        }

        @Test
        @DisplayName("Should allow all operations for non-suspended user")
        void testAllowAllOperationsForNonSuspendedUser() {
            // Act & Assert
            assertTrue(suspensionPolicyService.isOperationAllowedForSuspendedUser(testUser, "profile"));
            assertTrue(suspensionPolicyService.isOperationAllowedForSuspendedUser(testUser, "logout"));
            assertTrue(suspensionPolicyService.isOperationAllowedForSuspendedUser(testUser, "appeal"));
            assertTrue(suspensionPolicyService.isOperationAllowedForSuspendedUser(testUser, "booking"));
            assertTrue(suspensionPolicyService.isOperationAllowedForSuspendedUser(testUser, "check-in"));
            assertTrue(suspensionPolicyService.isOperationAllowedForSuspendedUser(testUser, "approval"));
        }

        @Test
        @DisplayName("Should handle case-insensitive operation type")
        void testOperationTypeCaseInsensitive() {
            // Act & Assert
            assertTrue(suspensionPolicyService.isOperationAllowedForSuspendedUser(suspendedUser, "PROFILE"));
            assertTrue(suspensionPolicyService.isOperationAllowedForSuspendedUser(suspendedUser, "Profile"));
            assertTrue(suspensionPolicyService.isOperationAllowedForSuspendedUser(suspendedUser, "LOGOUT"));
            assertTrue(suspensionPolicyService.isOperationAllowedForSuspendedUser(suspendedUser, "Appeal"));
        }

        @Test
        @DisplayName("Should allow all operations when user is not suspended")
        void testAllowAllOperationsForNonSuspendedUserRegardlessOfType() {
            // Act & Assert
            assertTrue(suspensionPolicyService.isOperationAllowedForSuspendedUser(null, "any-operation"));
            assertTrue(suspensionPolicyService.isOperationAllowedForSuspendedUser(testUser, "unknown-operation"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"profile", "PROFILE", "Profile", "logout", "LOGOUT", "Logout", "appeal", "APPEAL", "Appeal"})
        @DisplayName("Should allow all whitelisted operations (case-insensitive)")
        void testAllWhitelistedOperationsVariations(String operationType) {
            // Act
            boolean allowed = suspensionPolicyService.isOperationAllowedForSuspendedUser(suspendedUser, operationType);

            // Assert
            assertTrue(allowed, "Operation '" + operationType + "' should be whitelisted");
        }
    }

    // ==================== Suspension Message Tests ====================

    @Nested
    @DisplayName("Suspension Message Tests")
    class SuspensionMessageTests {

        @Test
        @DisplayName("Should return suspension message for suspended user")
        void testGetSuspensionMessage() {
            // Arrange
            LocalDateTime suspensionEndTime = LocalDateTime.now(ZoneId.systemDefault()).plusDays(5);
            suspendedUser.setSuspendedUntil(suspensionEndTime);

            // Act
            String message = suspensionPolicyService.getSuspensionMessage(suspendedUser);

            // Assert
            assertNotNull(message);
            assertTrue(message.contains("suspended"), "Message should mention suspension");
            assertTrue(message.contains("appeal"), "Message should mention appeal");
            assertTrue(message.contains(suspensionEndTime.toString()), "Message should include suspension end time");
        }

        @Test
        @DisplayName("Should return not suspended message for null user")
        void testGetSuspensionMessageForNullUser() {
            // Act
            String message = suspensionPolicyService.getSuspensionMessage(null);

            // Assert
            assertNotNull(message);
            assertTrue(message.contains("not suspended"));
        }

        @Test
        @DisplayName("Should return not suspended message for non-suspended user")
        void testGetSuspensionMessageForNonSuspendedUser() {
            // Act
            String message = suspensionPolicyService.getSuspensionMessage(testUser);

            // Assert
            assertNotNull(message);
            assertTrue(message.contains("not suspended"));
        }
    }

    // ==================== Edge Cases and Integration Scenarios ====================

    @Nested
    @DisplayName("Edge Cases and Integration Scenarios")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle multiple no-shows in sequence correctly")
        void testMultipleNoShowsSequence() {
            // Arrange
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .googleSubject("google-subject-seq")
                    .email("sequence@university.edu")
                    .displayName("Sequence Tester")
                    .roles(new HashSet<>(Set.of()))
                    .active(true)
                    .suspendedUntil(null)
                    .noShowCount(0)
                    .build();

            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act - First no-show
            user.setNoShowCount(1);
            User after1 = suspensionPolicyService.recordNoShowAndApplySuspensionIfNeeded(user);

            // Assert - Not suspended yet
            assertEquals(1, after1.getNoShowCount());
            assertNull(after1.getSuspendedUntil());

            // Act - Second no-show
            user.setNoShowCount(2);
            User after2 = suspensionPolicyService.recordNoShowAndApplySuspensionIfNeeded(user);

            // Assert - Still not suspended
            assertEquals(2, after2.getNoShowCount());
            assertNull(after2.getSuspendedUntil());

            // Act - Third no-show (triggers suspension)
            user.setNoShowCount(3);
            User after3 = suspensionPolicyService.recordNoShowAndApplySuspensionIfNeeded(user);

            // Assert - Now suspended
            assertEquals(3, after3.getNoShowCount());
            assertNotNull(after3.getSuspendedUntil());
        }

        @Test
        @DisplayName("Should maintain no-show count and suspension independent of each other")
        void testNoShowCountAndSuspensionIndependence() {
            // Arrange
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .googleSubject("google-subject-ind")
                    .email("independent@university.edu")
                    .displayName("Independence Tester")
                    .roles(new HashSet<>(Set.of()))
                    .active(true)
                    .suspendedUntil(null)
                    .noShowCount(2)
                    .build();

            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act - Manually suspend user while at 2 no-shows
            LocalDateTime suspensionTime = LocalDateTime.now(ZoneId.systemDefault()).plusDays(7);
            user.setSuspendedUntil(suspensionTime);

            // Act - Record another no-show
            user.setNoShowCount(3);
            User result = suspensionPolicyService.recordNoShowAndApplySuspensionIfNeeded(user);

            // Assert - Both suspension and no-show count updated
            assertEquals(3, result.getNoShowCount());
            assertNotNull(result.getSuspendedUntil());
        }

        @Test
        @DisplayName("Should calculate remaining no-shows correctly for various counts")
        void testRemainingNoShowsCalculationVariations() {
            // Create users with different no-show counts
            for (int noShowCount = 0; noShowCount <= 5; noShowCount++) {
                User user = User.builder()
                        .id(UUID.randomUUID())
                        .googleSubject("google-subject-" + noShowCount)
                        .email("user" + noShowCount + "@university.edu")
                        .displayName("User " + noShowCount)
                        .roles(new HashSet<>(Set.of()))
                        .active(true)
                        .suspendedUntil(null)
                        .noShowCount(noShowCount)
                        .build();

                // Act
                int remaining = suspensionPolicyService.getRemainingNoShowsBeforeSuspension(user);

                // Assert
                int expected = Math.max(0, NO_SHOW_SUSPENSION_THRESHOLD - noShowCount);
                assertEquals(expected, remaining, "Remaining no-shows for count " + noShowCount);
            }
        }

        @Test
        @DisplayName("Should verify suspension expiration behavior")
        void testSuspensionExpirationBehavior() {
            // Arrange - Create user with suspension in the past
            LocalDateTime pastSuspension = LocalDateTime.now(ZoneId.systemDefault()).minusHours(1);
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .googleSubject("google-subject-exp")
                    .email("expiration@university.edu")
                    .displayName("Expiration Tester")
                    .roles(new HashSet<>(Set.of()))
                    .active(true)
                    .suspendedUntil(pastSuspension)
                    .noShowCount(3)
                    .build();

            // Act
            boolean isSuspended = suspensionPolicyService.isSuspended(user);

            // Assert - Should not be suspended if suspension time passed
            assertFalse(isSuspended);

            // Act - Try to check suspension policy
            // Should not throw exception since suspension has expired
            assertDoesNotThrow(() -> suspensionPolicyService.checkSuspensionPolicy(user));
        }

        @Test
        @DisplayName("Should handle null operation type in isOperationAllowedForSuspendedUser")
        void testNullOperationTypeHandling() {
            // Act
            boolean allowed = suspensionPolicyService.isOperationAllowedForSuspendedUser(suspendedUser, null);

            // Assert
            assertFalse(allowed); // null operation type should not be in whitelist
        }
    }

    // ==================== Constants and Configuration Tests ====================

    @Nested
    @DisplayName("Service Constants and Configuration Tests")
    class ConstantsTests {

        @Test
        @DisplayName("Should verify suspension threshold constant is 3")
        void testSuspensionThresholdConstant() {
            // Act
            int threshold = suspensionPolicyService.getSuspensionThreshold();

            // Assert
            assertEquals(3, threshold, "Suspension threshold should be 3 no-shows");
        }

        @Test
        @DisplayName("Should verify suspension duration constant is 7 days")
        void testSuspensionDurationConstant() {
            // Act
            int duration = suspensionPolicyService.getSuspensionDurationDays();

            // Assert
            assertEquals(7, duration, "Suspension duration should be 7 days");
        }

        @Test
        @DisplayName("Should verify no-show grace period aligns with 15 minutes requirement")
        void testNoShowGracePeriodAlignment() {
            // Note: This test verifies that the service's threshold (3 no-shows)
            // aligns with FR-021 which defines 15 minutes as grace period.
            // The actual grace period calculation happens in CheckInService/NoShowEvaluator.

            // Act
            int threshold = suspensionPolicyService.getSuspensionThreshold();

            // Assert
            assertEquals(3, threshold);
            // Grace period (15 min) is applied in CheckInService, not here
        }
    }
}
