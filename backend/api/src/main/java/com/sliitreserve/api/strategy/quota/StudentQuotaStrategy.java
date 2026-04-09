package com.sliitreserve.api.strategy.quota;

/**
 * Marker interface for Student quota strategy.
 * 
 * Concrete implementation will be created in T053.
 * 
 * Student policies (typical constraints):
 * - Weekly quota: 3 bookings max
 * - Monthly quota: 10 bookings max
 * - Advance booking window: 90 days
 * - Peak hours (08:00-10:00): NOT allowed
 * - High-capacity approval: NOT required for students
 * - Permissiveness priority: 1 (most restrictive)
 */
public interface StudentQuotaStrategy extends QuotaStrategy {
}
