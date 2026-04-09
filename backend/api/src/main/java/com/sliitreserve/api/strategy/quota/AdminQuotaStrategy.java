package com.sliitreserve.api.strategy.quota;

/**
 * Marker interface for Admin quota strategy.
 * 
 * Concrete implementation will be created in T053.
 * 
 * Admin policies (least restrictive):
 * - Weekly quota: Unlimited or very high (e.g., 100+)
 * - Monthly quota: Unlimited or very high (e.g., 300+)
 * - Advance booking window: 365 days or unlimited
 * - Peak hours (08:00-10:00): Allowed without restriction
 * - High-capacity approval: May be required for audit trail, but not enforced
 * - Permissiveness priority: 3 (most permissive)
 */
public interface AdminQuotaStrategy extends QuotaStrategy {
}
