package algorithms.moga;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import algorithms.Chromosome;

/**
 * Unit tests for {@link CartesianDistanceComparator}, which orders chromosomes
 * by their normalized Euclidean distance to the (0,0) reference point.
 */
public class CartesianDistanceComparatorTest {

    private CartesianDistanceComparator<DummyChromosome> comparator;

    @Before
    public void setUp() {
        comparator = new CartesianDistanceComparator<DummyChromosome>();
        comparator.addValueMin(EGAObjectives.eENERGY, 0.0);
        comparator.addValueMax(EGAObjectives.eENERGY, 10.0);
        comparator.addValueMin(EGAObjectives.eTIME, 0.0);
        comparator.addValueMax(EGAObjectives.eTIME, 10.0);
    }

    @Test
    public void testCloserToOriginIsSmaller() {
        DummyChromosome near = new DummyChromosome(1.0, 1.0);
        DummyChromosome far = new DummyChromosome(9.0, 9.0);
        assertTrue("nearer the origin must compare as smaller", comparator.compare(near, far) < 0);
        assertTrue(comparator.compare(far, near) > 0);
    }

    @Test
    public void testEqualPointsCompareEqual() {
        DummyChromosome a = new DummyChromosome(5.0, 5.0);
        DummyChromosome b = new DummyChromosome(5.0, 5.0);
        assertEquals(0, comparator.compare(a, b));
    }

    // ── Test double ──────────────────────────────────────────────────────────

    static class DummyChromosome implements Chromosome<DummyChromosome> {
        final double energy;
        final double time;
        DummyChromosome(double energy, double time) { this.energy = energy; this.time = time; }

        @Override public List<DummyChromosome> crossover(DummyChromosome other) { return new ArrayList<DummyChromosome>(); }
        @Override public DummyChromosome mutate() { return null; }
        @Override public boolean isFitnessValid() { return true; }
        @Override public int getId() { return 0; }
        @Override public double getObjective(EGAObjectives obj) {
            return obj == EGAObjectives.eENERGY ? energy : time;
        }
        @Override public void setDominated(int d) {}
        @Override public void setnCrowdDensity(double d) {}
        @Override public void setFitness() {}
        @Override public double getNumDom() { return 0; }
        @Override public double getFitness() { return 0; }
        @Override public void setCrowdingDistance(double d) {}
        @Override public void addToCrowdingDistance(double d) {}
        @Override public double getCrowdingDistance() { return 0; }
        @Override public int getRank() { return 0; }
        @Override public void setRank(int r) {}
        @Override public DummyChromosome dup() { return new DummyChromosome(energy, time); }
        @Override public int[] getObjectivesIndex() { return null; }
    }
}
