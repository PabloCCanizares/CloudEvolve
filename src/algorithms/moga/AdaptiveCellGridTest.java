package algorithms.moga;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import algorithms.Chromosome;

/**
 * Unit tests for the public API of {@link AdaptiveCellGrid}, the adaptive grid
 * archive used by the PAES algorithms.
 *
 * <p>Most tests use small integer objective values for readability, but the
 * grid normalizes objective values onto its bisection grid against the adaptive
 * min/max bounds, so cell indexing stays in range whatever the objective scale.
 * {@link #testWidelySpreadObjectivesDoNotOverflowSmallGrid()} and
 * {@link #testWideSpreadProbesStayInRange()} cover that explicitly on a coarse
 * grid. Note that the archive over-/underflow guard in the constructor is
 * unreachable with valid inputs, so this class is intentionally not pursued to
 * 100% line coverage.</p>
 */
public class AdaptiveCellGridTest {

    private AdaptiveCellGrid<DummyChromosome> grid;

    @Before
    public void setUp() throws Exception {
        grid = new AdaptiveCellGrid<DummyChromosome>(10, 8, 2);
    }

    @Test
    public void testAddIncreasesSize() {
        assertEquals(0, grid.getSize());
        // Two mutually non-dominated points (a trade-off) are both archived.
        assertTrue(grid.add(new DummyChromosome(1, 4)));
        assertTrue(grid.add(new DummyChromosome(4, 1)));
        assertEquals(2, grid.getSize());
    }

    @Test
    public void testDominatedCandidateIsRejected() {
        grid.add(new DummyChromosome(1, 1)); // dominates everything below
        // (2,2) is dominated by (1,1) for minimization -> not archived.
        assertFalse(grid.add(new DummyChromosome(2, 2)));
        assertEquals(1, grid.getSize());
    }

    @Test
    public void testGetAndDensityReturnCellContents() {
        DummyChromosome a = new DummyChromosome(2, 3);
        grid.add(a);
        List<DummyChromosome> cell = grid.get(a);
        assertTrue(cell.contains(a));
        assertEquals(1, grid.getDensity(a));
    }

    @Test
    public void testRemoveTakesChromosomeOutOfArchive() {
        DummyChromosome a = new DummyChromosome(2, 3);
        DummyChromosome b = new DummyChromosome(3, 2);
        grid.add(a);
        grid.add(b);
        grid.remove(a);
        assertFalse(grid.get(a).contains(a));
    }

    @Test
    public void testGetPopulationReflectsArchive() {
        grid.add(new DummyChromosome(2, 3));
        grid.add(new DummyChromosome(3, 2));
        PopulationMO<DummyChromosome> pop = grid.getPopulation();
        assertEquals(2, pop.getSize());
    }

    /**
     * Regression test for the latent IndexOutOfBoundsException: a coarse grid
     * (2 bisections -> 4 divisions -> 16 cells) fed objective values spanning
     * ~50-100 (far wider than the grid resolution) used to index past the end
     * of its backing list in {@code add()}. The grid must now absorb the spread
     * by normalizing onto its bisection grid.
     */
    @Test
    public void testWidelySpreadObjectivesDoNotOverflowSmallGrid() throws Exception {
        AdaptiveCellGrid<DummyChromosome> coarse =
                new AdaptiveCellGrid<DummyChromosome>(100, 4, 2);

        // A Pareto-style trade-off front: decreasing energy, increasing time, so
        // every point is mutually non-dominated and gets archived.
        for (int i = 0; i < 10; i++) {
            double energy = 100.0 - i * 5.0;
            double time = 10.0 + i * 5.0;
            assertTrue(coarse.add(new DummyChromosome(energy, time)));
        }

        assertEquals(10, coarse.getSize());
        assertEquals(10, coarse.getPopulation().getSize());
    }

    /**
     * The other public probes — {@code get()}, {@code getDensity()} and
     * {@code remove()} — also derive a cell index from the objective values, so
     * they must stay in range on a coarse grid with widely spread objectives.
     */
    @Test
    public void testWideSpreadProbesStayInRange() throws Exception {
        AdaptiveCellGrid<DummyChromosome> coarse =
                new AdaptiveCellGrid<DummyChromosome>(100, 4, 2);

        DummyChromosome low = new DummyChromosome(5, 95);
        DummyChromosome high = new DummyChromosome(95, 5);
        coarse.add(low);
        coarse.add(high);

        assertTrue(coarse.get(low).contains(low));
        assertEquals(1, coarse.getDensity(low));
        assertEquals(1, coarse.getDensity(high));

        coarse.remove(low);
        assertFalse(coarse.get(low).contains(low));
        assertEquals(0, coarse.getDensity(low));
        // The other solution is untouched and still reachable in range.
        assertEquals(1, coarse.getDensity(high));
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
