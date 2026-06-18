package main_scico.hv;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * Unit tests for {@link HV2D}, the exact 2D hypervolume / non-dominated helper.
 */
public class HV2DTest {

    private static List<double[]> pts(double[]... ps) {
        return new ArrayList<double[]>(Arrays.asList(ps));
    }

    @Test
    public void testComputeEmptyWhenNoFeasiblePoints() {
        // All points are outside the reference box (>= ref) or non-positive.
        List<double[]> points = pts(new double[]{20, 20}, new double[]{-1, 5}, new double[]{5, 0});
        assertEquals(0.0, HV2D.compute(points, new double[]{10, 10}), 1e-9);
    }

    @Test
    public void testComputeKnownValueOnParetoFront() {
        // Three mutually non-dominated points under ref (10,10).
        List<double[]> points = pts(new double[]{2, 8}, new double[]{5, 5}, new double[]{8, 2});
        // Skyline rectangles: 3*2 + 3*5 + 2*8 = 6 + 15 + 16 = 37.
        assertEquals(37.0, HV2D.compute(points, new double[]{10, 10}), 1e-9);
    }

    @Test
    public void testDominatedPointDoesNotChangeHypervolume() {
        List<double[]> front = pts(new double[]{2, 8}, new double[]{5, 5}, new double[]{8, 2});
        List<double[]> withDominated = pts(new double[]{2, 8}, new double[]{5, 5}, new double[]{8, 2}, new double[]{9, 9});
        assertEquals(HV2D.compute(front, new double[]{10, 10}),
                     HV2D.compute(withDominated, new double[]{10, 10}), 1e-9);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testComputeRejectsNon2DPoint() {
        List<double[]> bad = pts(new double[]{1, 2, 3});
        HV2D.compute(bad, new double[]{10, 10});
    }

    @Test
    public void testNonDominatedFiltersDominatedPoints() {
        List<double[]> points = pts(new double[]{1, 1}, new double[]{2, 2}, new double[]{1, 2});
        List<double[]> nd = HV2D.nonDominated(points);
        assertEquals(1, nd.size());
        assertArrayEquals(new double[]{1, 1}, nd.get(0), 1e-9);
    }

    @Test
    public void testNonDominatedKeepsTradeOffs() {
        List<double[]> points = pts(new double[]{1, 5}, new double[]{5, 1});
        assertEquals(2, HV2D.nonDominated(points).size());
    }

    @Test
    public void testNonDominatedIndices() {
        List<double[]> points = pts(new double[]{1, 1}, new double[]{2, 2}, new double[]{3, 0.5});
        List<Integer> idx = HV2D.nonDominatedIndices(points);
        assertTrue(idx.contains(0)); // (1,1) non-dominated
        assertTrue(idx.contains(2)); // (3,0.5) trades x for y
        assertFalse(idx.contains(1)); // (2,2) dominated by (1,1)
    }
}
