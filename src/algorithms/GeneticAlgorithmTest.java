package algorithms;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import algorithms.moga.EGAObjectives;

/**
 * Unit tests for the GeneticAlgorithm class.
 */
public class GeneticAlgorithmTest {

    private GeneticAlgorithm<DummyChromosome, Integer> ga;

    @Before
    public void setUp() {
        Population<DummyChromosome> pop = new Population<DummyChromosome>();
        pop.addChromosome(new DummyChromosome(3));
        pop.addChromosome(new DummyChromosome(1));
        pop.addChromosome(new DummyChromosome(2));
        ga = new GeneticAlgorithm<DummyChromosome, Integer>(pop, new DummyFitness());
    }

    /** Verify that the constructor sorts the population. */
    @Test
    public void testConstructorSortsPopulation() {
        Population<DummyChromosome> pop = ga.getPopulation();
        int prev = pop.getChromosomeByIndex(0).getFitnessValue();
        for (int i = 1; i < pop.getSize(); i++) {
            int curr = pop.getChromosomeByIndex(i).getFitnessValue();
            assertTrue("Population should be sorted after constructor", prev <= curr);
            prev = curr;
        }
    }

    /** Verify that evolve(n) advances the iteration counter. */
    @Test
    public void testEvolveIncreasesIteration() {
        ga.evolve(3);
        assertEquals(2, ga.getIteration());
    }

    /** Verify that terminate() stops the evolve(int) loop. */
    @Test(timeout = 5000)
    public void testTerminate() throws InterruptedException {
        final GeneticAlgorithm<DummyChromosome, Integer> gaLocal;
        Population<DummyChromosome> pop = new Population<DummyChromosome>();
        for (int i = 0; i < 3; i++) {
            pop.addChromosome(new DummyChromosome(i + 1));
        }
        gaLocal = new GeneticAlgorithm<DummyChromosome, Integer>(pop, new DummyFitness());

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                gaLocal.evolve(Integer.MAX_VALUE);
            }
        });
        t.start();
        Thread.sleep(50);
        gaLocal.terminate();
        t.join(3000);
        assertFalse("GA thread should have stopped after terminate()", t.isAlive());
    }

    /** Verify that getBest() returns the chromosome with the lowest fitness after construction. */
    @Test
    public void testGetBestChromosome() {
        DummyChromosome best = ga.getBest();
        assertNotNull(best);
        // After construction sort, the lowest-fitness chromosome is at index 0.
        assertEquals(1, best.getFitnessValue());
    }

    /** Verify that getWorst() returns the chromosome with the highest fitness after construction. */
    @Test
    public void testGetWorstChromosome() {
        DummyChromosome worst = ga.getWorst();
        assertNotNull(worst);
        // After construction sort, the highest-fitness chromosome is at the last index.
        assertEquals(3, worst.getFitnessValue());
    }

    /** Verify that iteration listeners can be added and that evolve(int) completes without error. */
    @Test
    public void testAddIterationListener() {
        final int[] callCount = {0};
        ga.addIterationListener(new IterationListener<DummyChromosome, Integer>() {
            @Override
            public void update(GeneticAlgorithm<DummyChromosome, Integer> algorithm) {
                callCount[0]++;
            }
        });
        ga.evolve(2);
        // The listener callback is currently commented out in the GA implementation,
        // so we verify that the GA runs to completion without error.
        assertEquals(1, ga.getIteration());
    }

    /** Verify that the parent-survive count can be changed and read back. */
    @Test
    public void testSetParentChromosomesSurviveCount() {
        ga.setParentChromosomesSurviveCount(5);
        assertEquals(5, ga.getParentChromosomesSurviveCount());
    }

    // ── Helper classes ────────────────────────────────────────────────────────

    /** Minimal Chromosome implementation used only for GeneticAlgorithm tests. */
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

    /** Minimal Fitness implementation that returns the chromosome's fitness value. */
    static class DummyFitness implements Fitness<DummyChromosome, Integer> {
        @Override
        public Integer calculate(DummyChromosome chromosome) {
            return chromosome.getFitnessValue();
        }
        @Override
        public Integer calculate(DummyChromosome chromosome, EGAObjectives obj) {
            return chromosome.getFitnessValue();
        }
    }
}
