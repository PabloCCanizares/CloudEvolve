package platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import platform.RealEvaluationPolicy.Context;

/** Unit tests for the hybrid's ratification policies ({@link RealEvaluationPolicies}). */
public class RealEvaluationPolicyTest {

    private static final double[] ANY = { 10.0, 2800.0 };

    private static Context ctx(int gen, double novelty) {
        return new Context(gen, novelty);
    }

    @Test
    public void everyNFiresOnMultiples() {
        RealEvaluationPolicy p = RealEvaluationPolicies.everyN(3);
        assertTrue(p.shouldRatify(ANY, ctx(0, 0)));
        assertFalse(p.shouldRatify(ANY, ctx(1, 0)));
        assertTrue(p.shouldRatify(ANY, ctx(6, 0)));
    }

    @Test
    public void probabilityBoundsAreDeterministic() {
        assertFalse(RealEvaluationPolicies.probability(0.0).shouldRatify(ANY, ctx(1, 0)));
        assertTrue(RealEvaluationPolicies.probability(1.0).shouldRatify(ANY, ctx(1, 0)));
    }

    @Test
    public void implausibleFiresOutsideTheBounds() {
        RealEvaluationPolicy p = RealEvaluationPolicies.implausible(new double[] { 2.6, 24.7 },
                new double[] { 2770.0, 4074.0 });
        assertFalse(p.shouldRatify(new double[] { 18.0, 2800.0 }, ctx(1, 0)));   // in range
        assertTrue(p.shouldRatify(new double[] { 0.1, 2800.0 }, ctx(1, 0)));     // energy too low
        assertTrue(p.shouldRatify(new double[] { 18.0, 241.0 }, ctx(1, 0)));     // time too low
    }

    @Test
    public void noveltyFiresAboveThreshold() {
        RealEvaluationPolicy p = RealEvaluationPolicies.novelty(0.01);
        assertFalse(p.shouldRatify(ANY, ctx(1, 0.005)));
        assertTrue(p.shouldRatify(ANY, ctx(1, 0.02)));
    }

    @Test
    public void anyOfFiresIfEitherDoes() {
        RealEvaluationPolicy p = RealEvaluationPolicies.anyOf(Arrays.asList(
                RealEvaluationPolicies.everyN(100), RealEvaluationPolicies.novelty(0.01)));
        assertTrue(p.shouldRatify(ANY, ctx(1, 0.5)));    // novelty fires
        assertTrue(p.shouldRatify(ANY, ctx(0, 0.0)));    // everyN fires (gen 0)
        assertFalse(p.shouldRatify(ANY, ctx(1, 0.0)));   // neither
    }

    @Test
    public void cappedLimitsRatificationsPerGenerationAndResets() {
        RealEvaluationPolicy always = (pred, c) -> true;
        RealEvaluationPolicy p = RealEvaluationPolicies.capped(always, 2);
        int gen = 1;
        int fired = 0;
        for (int i = 0; i < 5; i++) {
            if (p.shouldRatify(ANY, ctx(gen, 0))) {
                fired++;
            }
        }
        assertEquals("at most 2 per generation", 2, fired);
        assertTrue("budget resets on a new generation", p.shouldRatify(ANY, ctx(gen + 1, 0)));
    }
}
