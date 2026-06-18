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
 * <p>Objective values are kept as small integers well within the grid
 * resolution (8 divisions -> 64 cells) so the archive's cell indexing stays in
 * range. Note that the archive over-/underflow guards in the constructor and a
 * couple of defensive branches are unreachable with valid inputs, so this class
 * is intentionally not pursued to 100% line coverage.</p>
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
