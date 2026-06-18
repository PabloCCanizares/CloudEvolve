package algorithms.moga;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import algorithms.Chromosome;

/**
 * Unit tests for the MOSolution class.
 */
public class MOSolutionTest {

    private MOSolution<DummyChromosome, Integer> solution;
    private DummyChromosome chromosome;

    @Before
    public void setUp() {
        chromosome = new DummyChromosome(42);
        solution = new MOSolution<DummyChromosome, Integer>(chromosome, 10);
    }

    /** Verify that the constructor stores the individual and fitness. */
    @Test
    public void testConstructorSetsIndividualAndFitness() {
        assertSame(chromosome, solution.getIndividual());
        assertEquals(Integer.valueOf(10), solution.getFit());
    }

    /** Verify that incrementDominations() increments the domination counter. */
    @Test
    public void testIncrementDominations() {
        assertEquals(0, solution.getDominations());
        solution.incrementDominations();
        assertEquals(1, solution.getDominations());
        solution.incrementDominations();
        assertEquals(2, solution.getDominations());
    }

    /** Verify that decrementDominations() decrements the domination counter. */
    @Test
    public void testDecrementDominations() {
        solution.incrementDominations();
        solution.incrementDominations();
        assertEquals(2, solution.getDominations());
        solution.decrementDominations();
        assertEquals(1, solution.getDominations());
    }

    /** Verify that dominated solutions can be inserted and are stored. */
    @Test
    public void testInsertDominatedSolutions() {
        DummyChromosome c2 = new DummyChromosome(5);
        MOSolution<DummyChromosome, Integer> dominated =
                new MOSolution<DummyChromosome, Integer>(c2, 5);
        solution.insertDominatedSolutions(dominated);
        assertEquals(1, solution.getDominatedIndividuals().size());
        assertSame(dominated, solution.getDominatedIndividuals().get(0));
    }

    /** Verify that the rank can be set and retrieved. */
    @Test
    public void testSetAndGetRank() {
        solution.setRank(3);
        assertEquals(3, solution.getRank());
    }

    /** Verify that inserting the same dominated solution twice keeps only one copy. */
    @Test
    public void testInsertDominatedSolutionsIgnoresDuplicates() {
        MOSolution<DummyChromosome, Integer> dominated =
                new MOSolution<DummyChromosome, Integer>(new DummyChromosome(1), 1);
        solution.insertDominatedSolutions(dominated);
        solution.insertDominatedSolutions(dominated);
        assertEquals(1, solution.getDominatedIndividuals().size());
    }

    /** Verify the remaining accessors expose / delegate to the wrapped chromosome. */
    @Test
    public void testAccessorsDelegateToChromosome() {
        assertSame(chromosome, solution.getChromosome());
        assertEquals(Integer.valueOf(10), solution.getFit());
        // getObjective and the crowding-distance helpers delegate to the chromosome.
        assertEquals(42.0, solution.getObjective(EGAObjectives.eENERGY), 1e-9);
        solution.setCrowdingDistance(2.0);
        assertEquals(2.0, solution.getCrowdingDistance(), 1e-9);
        solution.addToCrowdingDistance(0.5);
        assertEquals(2.5, solution.getCrowdingDistance(), 1e-9);
    }

    // ── Helper class ──────────────────────────────────────────────────────────

    /** Minimal Chromosome implementation used only for MOSolution tests. */
    @SuppressWarnings("rawtypes")
    static class DummyChromosome implements Chromosome<DummyChromosome> {

        private int value;
        private int rank;
        private double crowding;

        DummyChromosome(int value) {
            this.value = value;
        }

        @Override public List<DummyChromosome> crossover(DummyChromosome other) {
            return new ArrayList<DummyChromosome>();
        }
        @Override public DummyChromosome mutate() { return null; }
        @Override public boolean isFitnessValid() { return true; }
        @Override public int getId() { return value; }
        @Override public double getObjective(EGAObjectives obj) { return value; }
        @Override public void setDominated(int d) {}
        @Override public void setnCrowdDensity(double d) {}
        @Override public void setFitness() {}
        @Override public double getNumDom() { return 0; }
        @Override public double getFitness() { return value; }
        @Override public void setCrowdingDistance(double d) { this.crowding = d; }
        @Override public void addToCrowdingDistance(double d) { this.crowding += d; }
        @Override public double getCrowdingDistance() { return crowding; }
        @Override public int getRank() { return rank; }
        @Override public void setRank(int r) { this.rank = r; }
        @Override public DummyChromosome dup() { return new DummyChromosome(value); }
        @Override public int[] getObjectivesIndex() { return null; }
        @Override public String toString() { return "DC(" + value + ")"; }
    }
}
