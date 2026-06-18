package algorithms.moga;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import algorithms.Chromosome;
import algorithms.ChromosomeComparator;

/**
 * Unit tests for {@link PopulationMO}.
 */
public class PopulationMOTest {

    private PopulationMO<DummyChromosome> population;

    @Before
    public void setUp() {
        population = new PopulationMO<DummyChromosome>();
    }

    @Test
    public void testAddAndSize() {
        assertEquals(0, population.getSize());
        population.addChromosome(new DummyChromosome(1));
        population.addChromosome(new DummyChromosome(2));
        assertEquals(2, population.getSize());
    }

    @Test
    public void testDelete() {
        DummyChromosome c1 = new DummyChromosome(1);
        DummyChromosome c2 = new DummyChromosome(2);
        population.addChromosome(c1);
        population.addChromosome(c2);
        population.deleteChromosome(c1);
        assertEquals(1, population.getSize());
        assertSame(c2, population.getChromosomeByIndex(0));
    }

    @Test
    public void testGetByIndexReturnsNullWhenOutOfRange() {
        population.addChromosome(new DummyChromosome(1));
        assertNotNull(population.getChromosomeByIndex(0));
        assertNull("out-of-range index must return null", population.getChromosomeByIndex(5));
    }

    @Test
    public void testGetRandomChromosome() {
        population.addChromosome(new DummyChromosome(7));
        assertSame(population.getChromosomeByIndex(0), population.getRandomChromosome());
    }

    @Test
    public void testTrimReduces() {
        for (int i = 0; i < 5; i++) population.addChromosome(new DummyChromosome(i));
        population.trim(3);
        assertEquals(3, population.getSize());
    }

    @Test
    public void testTrimNoOpWhenSmaller() {
        population.addChromosome(new DummyChromosome(1));
        population.trim(5);
        assertEquals(1, population.getSize());
    }

    @Test
    public void testIterator() {
        population.addChromosome(new DummyChromosome(1));
        population.addChromosome(new DummyChromosome(2));
        int count = 0;
        for (DummyChromosome c : population) {
            assertNotNull(c);
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testReplace() {
        DummyChromosome a = new DummyChromosome(1);
        DummyChromosome b = new DummyChromosome(2);
        DummyChromosome replacement = new DummyChromosome(99);
        population.addChromosome(a);
        population.addChromosome(b);
        population.replace(a, replacement);
        assertEquals(2, population.getSize());
        assertSame(replacement, population.getChromosomeByIndex(0));
        assertSame(b, population.getChromosomeByIndex(1));
    }

    @Test
    public void testReverseSortByComparator() {
        population.addChromosome(new DummyChromosome(1));
        population.addChromosome(new DummyChromosome(3));
        population.addChromosome(new DummyChromosome(2));
        population.reverseSortPopulationByFitness(byValue());
        assertEquals(3, population.getChromosomeByIndex(0).value);
        assertEquals(2, population.getChromosomeByIndex(1).value);
        assertEquals(1, population.getChromosomeByIndex(2).value);
    }

    @Test
    public void testSortByComparator() {
        population.addChromosome(new DummyChromosome(3));
        population.addChromosome(new DummyChromosome(1));
        population.addChromosome(new DummyChromosome(2));
        population.sortPopulationByFitness(byValue());
        assertEquals(1, population.getChromosomeByIndex(0).value);
        assertEquals(2, population.getChromosomeByIndex(1).value);
        assertEquals(3, population.getChromosomeByIndex(2).value);
    }

    @Test
    public void testSortByChromosomeComparator() {
        population.addChromosome(new DummyChromosome(3));
        population.addChromosome(new DummyChromosome(1));
        population.addChromosome(new DummyChromosome(2));
        // ChromosomeComparator overload: implementation shuffles then delegates to sort().
        population.sortPopulationByFitness(new ChromosomeComparator<DummyChromosome>() {
            @Override
            public int sort(List<DummyChromosome> chromosomes) {
                chromosomes.sort(byValue());
                return 1;
            }
        });
        assertEquals(1, population.getChromosomeByIndex(0).value);
        assertEquals(3, population.getChromosomeByIndex(2).value);
    }

    private static Comparator<DummyChromosome> byValue() {
        return new Comparator<DummyChromosome>() {
            @Override
            public int compare(DummyChromosome a, DummyChromosome b) {
                return Integer.compare(a.value, b.value);
            }
        };
    }

    // ── Test double ──────────────────────────────────────────────────────────

    static class DummyChromosome implements Chromosome<DummyChromosome> {
        final int value;
        DummyChromosome(int value) { this.value = value; }

        @Override public List<DummyChromosome> crossover(DummyChromosome other) { return new ArrayList<DummyChromosome>(); }
        @Override public DummyChromosome mutate() { return null; }
        @Override public boolean isFitnessValid() { return true; }
        @Override public int getId() { return value; }
        @Override public double getObjective(EGAObjectives obj) { return value; }
        @Override public void setDominated(int d) {}
        @Override public void setnCrowdDensity(double d) {}
        @Override public void setFitness() {}
        @Override public double getNumDom() { return 0; }
        @Override public double getFitness() { return value; }
        @Override public void setCrowdingDistance(double d) {}
        @Override public void addToCrowdingDistance(double d) {}
        @Override public double getCrowdingDistance() { return 0; }
        @Override public int getRank() { return 0; }
        @Override public void setRank(int r) {}
        @Override public DummyChromosome dup() { return new DummyChromosome(value); }
        @Override public int[] getObjectivesIndex() { return null; }
        @Override public String toString() { return "DC(" + value + ")"; }
    }
}
