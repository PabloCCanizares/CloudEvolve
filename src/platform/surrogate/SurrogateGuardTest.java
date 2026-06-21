package platform.surrogate;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

/**
 * Unit test for the plausibility guard on surrogate predictions
 * ({@link SurrogateModel#guard}).
 */
public class SurrogateGuardTest {

    private static final double[] ENERGY_BOUNDS = { 2.64, 24.74 };
    private static final double[] TIME_BOUNDS = { 2770.0, 4074.0 };
    private static final double EPS = 1e-9;

    @Test
    public void noneLeavesPredictionUntouched() {
        double[] raw = { -1.21, 241.0 };
        assertArrayEquals(raw, SurrogateModel.guard("none", raw, ENERGY_BOUNDS, TIME_BOUNDS), EPS);
    }

    @Test
    public void nonnegClipsImpossibleNegatives() {
        assertArrayEquals(new double[] { 0.0, 0.0 },
                SurrogateModel.guard("nonneg", new double[] { -1.21, -5.0 }, ENERGY_BOUNDS, TIME_BOUNDS), EPS);
        // valid predictions are left untouched
        assertArrayEquals(new double[] { 18.7, 2770.1 },
                SurrogateModel.guard("nonneg", new double[] { 18.7, 2770.1 }, ENERGY_BOUNDS, TIME_BOUNDS), EPS);
    }

    @Test
    public void clampPullsPredictionsIntoTheTrainingRange() {
        // the phantom (0.1 kWh, 241 s) is dragged up to the training floor
        assertArrayEquals(new double[] { 2.64, 2770.0 },
                SurrogateModel.guard("clamp", new double[] { 0.1, 241.0 }, ENERGY_BOUNDS, TIME_BOUNDS), EPS);
        // and absurdly high values down to the ceiling
        assertArrayEquals(new double[] { 24.74, 4074.0 },
                SurrogateModel.guard("clamp", new double[] { 999.0, 99999.0 }, ENERGY_BOUNDS, TIME_BOUNDS), EPS);
    }

    @Test
    public void clampWithoutBoundsFallsBackToNonneg() {
        assertArrayEquals(new double[] { 0.0, 241.0 },
                SurrogateModel.guard("clamp", new double[] { -1.0, 241.0 }, null, null), EPS);
    }
}
