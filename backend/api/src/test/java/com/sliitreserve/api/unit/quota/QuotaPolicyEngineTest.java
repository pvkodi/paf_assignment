package com.sliitreserve.api.unit.quota;

import com.sliitreserve.api.entities.auth.Role;
import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.repositories.bookings.BookingRepository;
import com.sliitreserve.api.services.booking.QuotaPolicyEngine;
import com.sliitreserve.api.strategy.quota.AdminQuotaStrategy;
import com.sliitreserve.api.strategy.quota.LecturerQuotaStrategy;
import com.sliitreserve.api.strategy.quota.QuotaPolicyViolationException;
import com.sliitreserve.api.strategy.quota.QuotaStrategy;
import com.sliitreserve.api.strategy.quota.RolePolicyResolver;
import com.sliitreserve.api.strategy.quota.StudentQuotaStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for QuotaPolicyEngine - validates quota enforcement logic.
 * Tests cover:
 * - Weekly quota limits per role
 * - Monthly quota limits per role
 * - Peak-hour restrictions
 * - Advance booking window enforcement
 * - Multi-role policy resolution
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QuotaPolicyEngine Tests")
class QuotaPolicyEngineTest {

    @Mock
    private RolePolicyResolver rolePolicyResolver;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private QuotaPolicyEngine quotaPolicyEngine;

    private User studentUser;
    private User lecturerUser;
    private User adminUser;
    private User multiRoleUser;

    private static final ZoneId CAMPUS_TIMEZONE = ZoneId.of("Asia/Colombo");
    private static final int HIGH_CAPACITY_THRESHOLD = 200;

    @BeforeEach
    void setUp() {
        studentUser = User.builder()
                .id(UUID.randomUUID())
                .email("student@example.com")
                .roles(Set.of(Role.USER))
                .build();

        lecturerUser = User.builder()
                .id(UUID.randomUUID())
                .email("lecturer@example.com")
                .roles(Set.of(Role.LECTURER))
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin@example.com")
                .roles(Set.of(Role.ADMIN))
                .build();

        multiRoleUser = User.builder()
                .id(UUID.randomUUID())
                .email("multi@example.com")
                .roles(Set.of(Role.USER, Role.LECTURER))
                .build();
    }

    @Test
    @DisplayName("Student should not exceed weekly quota of 3")
    void testStudentWeeklyQuotaLimit() {
        // Setup: Student has already booked 3 times this week
        LocalDate today = LocalDate.now(CAMPUS_TIMEZONE);
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate weekEnd = weekStart.plusDays(6);

        when(rolePolicyResolver.resolveStrategy(anyCollection()))
                .thenReturn(new StudentQuotaStrategy());
        when(bookingRepository.countWeeklyBookings(studentUser.getId(), weekStart, weekEnd))
                .thenReturn(3L);

        // Action & Assert: Attempting to book 4th time should throw exception
        assertThrows(QuotaPolicyViolationException.class, () ->
                quotaPolicyEngine.validateBookingRequest(
                        studentUser,
                        today,
                        LocalTime.of(14, 0),
                        LocalTime.of(15, 0),
                        50,
                        HIGH_CAPACITY_THRESHOLD
                ));
    }

    @Test
    @DisplayName("Student should not exceed monthly quota of 10")
    void testStudentMonthlyQuotaLimit() {
        // Setup: Student has already booked 10 times this month
        LocalDate today = LocalDate.now(CAMPUS_TIMEZONE);

        when(rolePolicyResolver.resolveStrategy(anyCollection()))
                .thenReturn(new StudentQuotaStrategy());
        when(bookingRepository.countWeeklyBookings(eq(studentUser.getId()), 
                any(), any()))
                .thenReturn(2L); // Within weekly limit
        when(bookingRepository.countMonthlyBookings(eq(studentUser.getId()), 
                any(), any()))
                .thenReturn(10L); // At monthly limit

        // Action & Assert: Attempting to book should throw exception
        assertThrows(QuotaPolicyViolationException.class, () ->
                quotaPolicyEngine.validateBookingRequest(
                        studentUser,
                        today,
                        LocalTime.of(14, 0),
                        LocalTime.of(15, 0),
                        50,
                        HIGH_CAPACITY_THRESHOLD
                ));
    }

    @Test
    @DisplayName("Student should not book during peak hours (08:00-10:00)")
    void testStudentPeakHourRestriction() {
        LocalDate today = LocalDate.now(CAMPUS_TIMEZONE);

        when(rolePolicyResolver.resolveStrategy(anyCollection()))
                .thenReturn(new StudentQuotaStrategy());
        // Peak-hour validation happens before quota counting in engine

        // Action & Assert: Attempting to book during peak hours should fail
        assertThrows(QuotaPolicyViolationException.class, () ->
                quotaPolicyEngine.validateBookingRequest(
                        studentUser,
                        today,
                        LocalTime.of(8, 30),  // Within peak hours
                        LocalTime.of(9, 30),
                        50,
                        HIGH_CAPACITY_THRESHOLD
                ));
    }

    @Test
    @DisplayName("Lecturer should not exceed advance booking window of 90 days")
    void testAdvanceBookingWindowLimit() {
        when(rolePolicyResolver.resolveStrategy(anyCollection()))
                .thenReturn(new LecturerQuotaStrategy());
        // No need to mock count queries since window validation fails first

        // Try to book 91 days in advance
        LocalDate futureDate = LocalDate.now(CAMPUS_TIMEZONE).plusDays(91);

        // Action & Assert: Should fail (exceeds 90-day window)
        assertThrows(QuotaPolicyViolationException.class, () ->
                quotaPolicyEngine.validateBookingRequest(
                        lecturerUser,
                        futureDate,
                        LocalTime.of(14, 0),
                        LocalTime.of(15, 0),
                        50,
                        HIGH_CAPACITY_THRESHOLD
                ));
    }

    @Test
    @DisplayName("Admin should can book up to 180 days in advance")
    void testAdminAdvanceBookingWindow() {
        when(rolePolicyResolver.resolveStrategy(anyCollection()))
                .thenReturn(new AdminQuotaStrategy());
        // Admin has unlimited quota, so no mocking needed for quota counts

        // Try to book 180 days in advance
        LocalDate futureDate = LocalDate.now(CAMPUS_TIMEZONE).plusDays(180);

        // Action & Assert: Should NOT throw (within 180-day window)
        assertDoesNotThrow(() ->
                quotaPolicyEngine.validateBookingRequest(
                        adminUser,
                        futureDate,
                        LocalTime.of(14, 0),
                        LocalTime.of(15, 0),
                        50,
                        HIGH_CAPACITY_THRESHOLD
                ));
    }

    @Test
    @DisplayName("Lecturer should be able to book during peak hours")
    void testLecturerCanBookPeakHours() {
        LocalDate today = LocalDate.now(CAMPUS_TIMEZONE);

        when(rolePolicyResolver.resolveStrategy(anyCollection()))
                .thenReturn(new LecturerQuotaStrategy());
        when(bookingRepository.countWeeklyBookings(eq(lecturerUser.getId()), 
                any(), any()))
                .thenReturn(0L);
        when(bookingRepository.countMonthlyBookings(eq(lecturerUser.getId()), 
                any(), any()))
                .thenReturn(0L);

        // Action & Assert: Should NOT throw for peak hour booking
        assertDoesNotThrow(() ->
                quotaPolicyEngine.validateBookingRequest(
                        lecturerUser,
                        today,
                        LocalTime.of(8, 30),  // Within peak hours
                        LocalTime.of(9, 30),
                        50,
                        HIGH_CAPACITY_THRESHOLD
                ));
    }

    @Test
    @DisplayName("Multi-role user should use most permissive (Lecturer) quota")
    void testMultiRolePolicyResolution() {
        LocalDate today = LocalDate.now(CAMPUS_TIMEZONE);

        // Most permissive strategy is Lecturer (priority 2 > Student priority 1)
        when(rolePolicyResolver.resolveStrategy(anyCollection()))
                .thenReturn(new LecturerQuotaStrategy());
        when(bookingRepository.countWeeklyBookings(eq(multiRoleUser.getId()), 
                any(), any()))
                .thenReturn(0L);
        when(bookingRepository.countMonthlyBookings(eq(multiRoleUser.getId()), 
                any(), any()))
                .thenReturn(0L);

        // Action & Assert: Multi-role user should be able to book during peak hours (Lecturer policy)
        assertDoesNotThrow(() ->
                quotaPolicyEngine.validateBookingRequest(
                        multiRoleUser,
                        today,
                        LocalTime.of(8, 30),  // Peak hours (would fail for Student)
                        LocalTime.of(9, 30),
                        50,
                        HIGH_CAPACITY_THRESHOLD
                ));
    }

    @Test
    @DisplayName("High-capacity facility approval requirement check for Lecturer")
    void testHighCapacityApprovalRequirement() {
        when(rolePolicyResolver.resolveStrategy(anyCollection()))
                .thenReturn(new LecturerQuotaStrategy());
        when(bookingRepository.countWeeklyBookings(eq(lecturerUser.getId()), 
                any(), any()))
                .thenReturn(0L);
        when(bookingRepository.countMonthlyBookings(eq(lecturerUser.getId()), 
                any(), any()))
                .thenReturn(0L);

        LocalDate today = LocalDate.now(CAMPUS_TIMEZONE);

        // High-capacity facility (300 > 200 threshold)
        int facilityCapacity = 300;

        // Should NOT throw - just logs high-capacity requirement
        // (The actual approval handling is in ApprovalWorkflowService)
        assertDoesNotThrow(() ->
                quotaPolicyEngine.validateBookingRequest(
                        lecturerUser,
                        today,
                        LocalTime.of(14, 0),
                        LocalTime.of(15, 0),
                        facilityCapacity,
                        HIGH_CAPACITY_THRESHOLD
                ));
    }

    @Test
    @DisplayName("Admin should bypass quota limits (effectively unlimited)")
    void testAdminBypassQuotaLimits() {
        when(rolePolicyResolver.resolveStrategy(anyCollection()))
                .thenReturn(new AdminQuotaStrategy());
        // Admin has unlimited quota, no mocking needed

        LocalDate today = LocalDate.now(CAMPUS_TIMEZONE);

        // Admin should still be able to book (unlimited)
        assertDoesNotThrow(() ->
                quotaPolicyEngine.validateBookingRequest(
                        adminUser,
                        today,
                        LocalTime.of(14, 0),
                        LocalTime.of(15, 0),
                        50,
                        HIGH_CAPACITY_THRESHOLD
                ));
    }
}
