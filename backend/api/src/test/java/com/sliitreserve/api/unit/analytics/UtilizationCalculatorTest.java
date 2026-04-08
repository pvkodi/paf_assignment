package com.sliitreserve.api.unit.analytics;

import com.sliitreserve.api.services.analytics.UtilizationCalculator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UtilizationCalculatorTest {

    @Test
    public void calculatesPercent_correctly() {
        double p = UtilizationCalculator.calculateUtilizationPercent(8.0, 2.0);
        assertEquals(25.0, p);
    }

    @Test
    public void handles_zero_availableHours() {
        double p = UtilizationCalculator.calculateUtilizationPercent(0.0, 3.0);
        assertEquals(0.0, p);
    }

    @Test
    public void caps_at_100_percent() {
        double p = UtilizationCalculator.calculateUtilizationPercent(4.0, 10.0);
        assertEquals(100.0, p);
    }

    @Test
    public void negative_booked_results_in_zero() {
        double p = UtilizationCalculator.calculateUtilizationPercent(8.0, -2.0);
        assertEquals(0.0, p);
    }

    @Test
    public void underutilized_threshold_detection() {
        double p = UtilizationCalculator.calculateUtilizationPercent(8.0, 2.0);
        assertTrue(UtilizationCalculator.isUnderutilized(p, 30.0));
        assertFalse(UtilizationCalculator.isUnderutilized(p, 20.0));
    }
}
