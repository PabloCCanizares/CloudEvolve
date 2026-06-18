package algorithms;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import algorithms.moga.EGAObjectives;

/**
 * Branch-coverage tests for {@link GeneticAlgorithm#evolve()} and the remaining
 * accessors. Unlike {@code GeneticAlgorithmTest} (whose dummy is inert), the
 * chromosome here produces real offspring: {@code mutate()} returns a fresh
 * valid child and {@code crossover()} returns a list containing a valid child, a
 * {@code null} (to exercise the null-skip branch), and an invalid child (to
 * exercise the {@code isFitnessValid() == false} removal branch). Starting from
 * 8 parents the post-evolution population exceeds {@code POPULATION_MAX_SIZE},
 * so the trim branch is covered too.
 */
public class GeneticAlgorithmEvolveTest {

    private GeneticAlgorithm<RichChromosome, Integer> ga;

    @Before
    public void setUp() {
        Population<RichChromosome> pop = new Population<RichChromosome>();
        for (int i = 0; i < 8; i++) {
            pop.addChromosome(new RichChromosome(i + 1, true));
        }
        ga = new GeneticAlgorithm<RichChromosome, Integer>(pop, new RichFitness());
    }

    @Test
    public void testEvolveExercisesMutateCrossoverFilterAndTrim() {
        ga.evolve();
        int size = ga.getPopulation().getSize();
        assertTrue("population should be trimmed to the max size", size <= 10);
        assertTrue("population should not be empty", size > 0);
        // No invalid chromosome may survive the validity filter.
        for (RichChromosome c : ga.getPopulation()) {
            assertTrue(c.isFitnessValid());
        }
    }

    @Test
    public void testGetBestReturnsNullOnEmptyPopulation() {
        GeneticAlgorithm<RichChromosome, Integer> empty =
                new GeneticAlgorithm<RichChromosome, Integer>(new Population<RichChromosome>(), new RichFitness());
        assertNull(empty.getBest());
    }

    @Test
    public void testGetWorstReturnsLastChromosome() {
        RichChromosome worst = ga.getWorst();
        assertNotNull(worst);
        assertSame(ga.getPopulation().getChromosomeByIndex(ga.getPopulation().getSize() - 1), worst);
    }

    @Test
    public void testFitnessAndClearCache() {
        RichChromosome c = ga.getBest();
        assertEquals(Integer.valueOf(c.fitnessValue), ga.fitness(c));
        ga.clearCache(); // must not throw and recompute lazily afterwards
        assertEquals(Integer.valueOf(c.fitnessValue), ga.fitness(c));
    }

    @Test
    public void testPopulationPrettyPrintAndIds() {
        String pretty = ga.populationPrettyPrint();
        assertTrue(pretty.startsWith("[ "));
        assertEquals(ga.getPopulation().getSize(), ga.getPopulationIds().size());
    }

    @Test
    public void testAddAndRemoveIterationListener() {
        IterationListener<RichChromosome, Integer> listener =
                new IterationListener<RichChromosome, Integer>() {
                    @Override
                    public void update(GeneticAlgorithm<RichChromosome, Integer> algorithm) {
                        // body intentionally empty
                    }
                };
        ga.addIterationListener(listener);
        ga.removeIterationListener(listener);
        ga.evolve(1); // iterates the (now empty) listener list without error
        assertEquals(0, ga.getIteration());
    }

    // ── Test doubles ───────────────────────────────────────────────────────────

    static class RichChromosome implements Chromosome<RichChromosome> {
        private static final AtomicInteger SEQ = new AtomicInteger(100);
        final int fitnessValue;
        final boolean valid;

        RichChromosome(int fitnessValue, boolean valid) {
            this.fitnessValue = fitnessValue;
            this.valid = valid;
        }

        @Override
        public List<RichChromosome> crossover(RichChromosome other) {
            // valid child, a null (null-skip branch), and an invalid child (filter branch).
            return new ArrayList<RichChromosome>(Arrays.asList(
                    new RichChromosome(SEQ.getAndIncrement(), true),
                    null,
                    new RichChromosome(SEQ.getAndIncrement(), false)));
        }

        @Override public RichChromosome mutate() { return new RichChromosome(SEQ.getAndIncrement(), true); }
        @Override public boolean isFitnessValid() { return valid; }
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
        @Override public RichChromosome dup() { return new RichChromosome(fitnessValue, valid); }
        @Override public int[] getObjectivesIndex() { return null; }
        @Override public String toString() { return "RC(" + fitnessValue + ")"; }
    }

    static class RichFitness implements Fitness<RichChromosome, Integer> {
        @Override public Integer calculate(RichChromosome c) { return c.fitnessValue; }
        @Override public Integer calculate(RichChromosome c, EGAObjectives obj) { return c.fitnessValue; }
    }
}
