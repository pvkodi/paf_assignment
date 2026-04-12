package com.sliitreserve.api.services.analytics;

/**
 * Small utility for computing utilization percentages and underutilization checks.
 * Kept intentionally simple for unit testing and reuse in analytics services.
 */
public final class UtilizationCalculator {

    private UtilizationCalculator() {
    }

    /**
     * Calculate utilization percent (bookedHours / availableHours) * 100.
     * Returns 0 when availableHours <= 0 to avoid division by zero.
     * Result is rounded to two decimal places and clamped between 0 and 100.
     */
    public static double calculateUtilizationPercent(double availableHours, double bookedHours) {
        if (availableHours <= 0) return 0.0;
        double percent = (bookedHours / availableHours) * 100.0;
        if (Double.isNaN(percent) || Double.isInfinite(percent)) return 0.0;
        if (percent < 0) percent = 0.0;
        if (percent > 100.0) percent = 100.0;
        // round to 2 decimals
        return Math.round(percent * 100.0) / 100.0;
    }

    /**
     * Simple underutilized test: true if utilizationPercent < threshold.
     */
    public static boolean isUnderutilized(double utilizationPercent, double thresholdPercent) {
        return utilizationPercent < thresholdPercent;
    }
}
