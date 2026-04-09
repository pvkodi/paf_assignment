# Quota Strategy Package

## Overview

This package implements the Strategy pattern for quota and booking policy enforcement. It provides a clean, extensible mechanism to encapsulate role-specific policies without hard-coded conditionals.

## Architecture

### Core Components

1. **QuotaStrategy** (interface)
   - Defines contract for role-specific quota policies
   - Methods: canBookDuringPeakHours(), getWeeklyQuota(), getMonthlyQuota(), getMaxAdvanceBookingDays(), etc.
   - Implemented by StudentQuotaStrategy, LecturerQuotaStrategy, AdminQuotaStrategy

2. **RolePolicyResolver** (@Component)
   - Registry for mapping roles to strategies
   - Resolves most permissive applicable strategy when user has multiple roles
   - Key method: resolveStrategy(Collection<String> roleNames) → QuotaStrategy

3. **AbstractQuotaStrategy** (abstract)
   - Base class providing common utilities
   - Timezone-aware helpers (CAMPUS_TIMEZONE, isWithinPeakHours(), isWithinAdvanceWindow())
   - Peak hour constants: PEAK_HOUR_START (08:00), PEAK_HOUR_END (10:00)

4. **QuotaStrategyBootstrap** (@Component)
   - Initializes strategies at application startup
   - Registers StudentQuotaStrategy, LecturerQuotaStrategy, AdminQuotaStrategy with resolver
   - Graceful degradation if strategies not yet implemented

5. **QuotaPolicyViolationException** (RuntimeException)
   - Thrown when booking violates quota constraints
   - Includes violatedPolicy and userRole for error context
   - Caught by GlobalExceptionHandler for HTTP 400 responses

### Marker Interfaces (for T053 implementation)

- **StudentQuotaStrategy**: Placeholder for student-specific policies
- **LecturerQuotaStrategy**: Placeholder for lecturer-specific policies
- **AdminQuotaStrategy**: Placeholder for admin-specific policies

## Usage Pattern

```java
// Inject resolver
@Autowired
private RolePolicyResolver policyResolver;

// When processing a booking request
public void createBooking(BookingRequest request, User user) {
    // Get applicable policy (most permissive if multi-role)
    QuotaStrategy policy = policyResolver.resolveStrategy(user.getRoles());

    // Check policies
    if (!policy.canBookDuringPeakHours(request.getStartTime())) {
        throw new QuotaPolicyViolationException(
            "Peak hour booking not allowed for " + policy.getRoleName(),
            "PEAK_HOURS",
            policy.getRoleName()
        );
    }

    if (!policy.canBookWithinAdvanceWindow(request.getBookingDate())) {
        throw new QuotaPolicyViolationException(
            "Booking exceeds advance booking window of " + policy.getMaxAdvanceBookingDays() + " days",
            "ADVANCE_WINDOW",
            policy.getRoleName()
        );
    }

    // Proceed with booking
    saveBooking(request, user);
}
```

## Multi-Role Policy Selection

When a user has multiple roles (e.g., STUDENT and LECTURER), the resolver selects the most permissive:

```
User roles: {STUDENT, LECTURER}
  ↓
Resolve strategies for each role:
  - STUDENT strategy (priority 1)
  - LECTURER strategy (priority 2)
  ↓
Select highest priority: LECTURER (priority 2)
  ↓
Return LECTURER strategy for policy checks
```

This ensures users benefit from all available permissions without administrative burden.

## Time Semantics

All time-based calculations use **campus local timezone** (configured in T018):

- Peak hours: 08:00-10:00 in campus timezone
- Advance booking window: Days from today in campus timezone
- Week/month boundaries: Based on campus timezone

## Integration Points

- **T018**: TimePolicyConfig provides campus timezone configuration
- **T027**: User entities with roles
- **T045**: BookingService uses policy checks
- **T053**: Concrete strategy implementations (StudentQuotaStrategy, etc.)
- **T054**: QuotaPolicyEngine orchestrates quota validation
- **GlobalExceptionHandler**: Maps QuotaPolicyViolationException to HTTP responses

## Testing Considerations

Unit tests (T048) should verify:

- Strategy registration by role
- Most-permissive policy selection for multi-role users
- Peak hour detection with timezone awareness
- Advance booking window validation
- Exception handling for unknown roles
- Quota limit comparisons (weekly, monthly)
