package com.sliitreserve.api.strategy.quota;

/**
 * Marker interface for Lecturer quota strategy.
 * 
 * Concrete implementation will be created in T053.
 * 
 * Lecturer policies (typical constraints):
 * - Weekly quota: 10 bookings max
 * - Monthly quota: 30 bookings max
 * - Advance booking window: 180 days
 * - Peak hours (08:00-10:00): May be allowed (depends on configuration)
 * - High-capacity approval: May be required depending on facility size
 * - Permissiveness priority: 2 (moderate)
 */
public interface LecturerQuotaStrategy extends QuotaStrategy {
}
