package algorithms;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import algorithms.moga.EGAObjectives;

/**
 * Unit tests for the Population class.
 */
public class PopulationTest {

    private Population<DummyChromosome> population;

    @Before
    public void setUp() {
        population = new Population<DummyChromosome>();
    }

    /** Verify that addChromosome increments the population size. */
    @Test
    public void testAddAndGetSize() {
        assertEquals(0, population.getSize());
        population.addChromosome(new DummyChromosome(1));
        assertEquals(1, population.getSize());
        population.addChromosome(new DummyChromosome(2));
        assertEquals(2, population.getSize());
    }

    /** Verify that deleteChromosome reduces the size. */
    @Test
    public void testDeleteChromosome() {
        DummyChromosome c1 = new DummyChromosome(1);
        DummyChromosome c2 = new DummyChromosome(2);
        population.addChromosome(c1);
        population.addChromosome(c2);
        population.deleteChromosome(c1);
        assertEquals(1, population.getSize());
        assertSame(c2, population.getChromosomeByIndex(0));
    }

    /** Verify that the correct chromosome is retrieved by index. */
    @Test
    public void testGetChromosomeByIndex() {
        DummyChromosome c0 = new DummyChromosome(10);
        DummyChromosome c1 = new DummyChromosome(20);
        population.addChromosome(c0);
        population.addChromosome(c1);
        assertSame(c0, population.getChromosomeByIndex(0));
        assertSame(c1, population.getChromosomeByIndex(1));
    }

    /** Verify that getRandomChromosome returns a non-null chromosome. */
    @Test
    public void testGetRandomChromosome() {
        population.addChromosome(new DummyChromosome(5));
        population.addChromosome(new DummyChromosome(10));
        DummyChromosome random = population.getRandomChromosome();
        assertNotNull(random);
    }

    /** Verify that trim(n) reduces the population to exactly n elements. */
    @Test
    public void testTrimReducesSize() {
        for (int i = 0; i < 5; i++) {
            population.addChromosome(new DummyChromosome(i));
        }
        population.trim(3);
        assertEquals(3, population.getSize());
    }

    /** Verify that trim(n) is a no-op when the population already has <= n elements. */
    @Test
    public void testTrimNoOpWhenSmaller() {
        population.addChromosome(new DummyChromosome(1));
        population.addChromosome(new DummyChromosome(2));
        population.trim(5);
        assertEquals(2, population.getSize());
    }

    /** Verify that sortPopulationByFitness produces an ascending-ordered population. */
    @Test
    public void testSortPopulationByFitness() {
        population.addChromosome(new DummyChromosome(3));
        population.addChromosome(new DummyChromosome(1));
        population.addChromosome(new DummyChromosome(2));

        Comparator<DummyChromosome> comparator = new Comparator<DummyChromosome>() {
            @Override
            public int compare(DummyChromosome a, DummyChromosome b) {
                return Integer.compare(a.getFitnessValue(), b.getFitnessValue());
            }
        };
        population.sortPopulationByFitness(comparator);

        for (int i = 0; i < population.getSize() - 1; i++) {
            int curr = population.getChromosomeByIndex(i).getFitnessValue();
            int next = population.getChromosomeByIndex(i + 1).getFitnessValue();
            assertTrue("Population should be sorted ascending", curr <= next);
        }
    }

    /** Verify that reverseSortPopulationByFitness produces a descending-ordered population. */
    @Test
    public void testReverseSortPopulationByFitness() {
        population.addChromosome(new DummyChromosome(1));
        population.addChromosome(new DummyChromosome(3));
        population.addChromosome(new DummyChromosome(2));

        Comparator<DummyChromosome> comparator = new Comparator<DummyChromosome>() {
            @Override
            public int compare(DummyChromosome a, DummyChromosome b) {
                return Integer.compare(a.getFitnessValue(), b.getFitnessValue());
            }
        };
        population.reverseSortPopulationByFitness(comparator);

        for (int i = 0; i < population.getSize() - 1; i++) {
            int curr = population.getChromosomeByIndex(i).getFitnessValue();
            int next = population.getChromosomeByIndex(i + 1).getFitnessValue();
            assertTrue("Population should be sorted descending", curr >= next);
        }
    }

    /** Verify that the iterator visits all chromosomes exactly once. */
    @Test
    public void testIterator() {
        DummyChromosome c1 = new DummyChromosome(1);
        DummyChromosome c2 = new DummyChromosome(2);
        DummyChromosome c3 = new DummyChromosome(3);
        population.addChromosome(c1);
        population.addChromosome(c2);
        population.addChromosome(c3);

        int count = 0;
        for (DummyChromosome c : population) {
            assertNotNull(c);
            count++;
        }
        assertEquals(3, count);
    }

    // ── Helper class ──────────────────────────────────────────────────────────

    /** Minimal Chromosome implementation used only for testing Population. */
    static class DummyChromosome implements Chromosome<DummyChromosome> {

        private int fitnessValue;

        DummyChromosome(int fitnessValue) {
            this.fitnessValue = fitnessValue;
        }

        int getFitnessValue() {
            return fitnessValue;
        }

        @Override public List<DummyChromosome> crossover(DummyChromosome other) {
            return new ArrayList<DummyChromosome>();
        }
        @Override public DummyChromosome mutate() { return null; }
        @Override public boolean isFitnessValid() { return true; }
        @Override public int getId() { return fitnessValue; }
        @Override public double getObjective(EGAObjectives obj) { return fitnessValue; }
        @Override public void setDominated(int d) {}
        @Override public void setnCrowdDensity(double d) {}
        @Override public void setFitness() {}
        @Override public double getNumDom() { return 0; }
        @Override public double getFitness() { return fitnessValue; }
        @Override public void setCrowdingDistance(double d) {}
        @Override public void addToCrowdingDistance(double d) {}
        @Override public double getCrowdingDistance() { return 0; }
        @Override public int getRank() { return 0; }
        @Override public void setRank(int r) {}
        @Override public DummyChromosome dup() { return new DummyChromosome(fitnessValue); }
        @Override public int[] getObjectivesIndex() { return null; }
        @Override public String toString() { return "DC(" + fitnessValue + ")"; }
    }
}
