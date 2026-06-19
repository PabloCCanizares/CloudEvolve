package algorithms.moga;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import algorithms.Chromosome;
import algorithms.Fitness;

/**
 * Smoke tests for the multi-objective evolutionary algorithms.
 *
 * <p>Each test wires a small population of lightweight {@link DummyMOChromosome}
 * individuals (two positive objectives, energy &amp; time) to a real algorithm
 * instance and runs a few evolution loops end-to-end. No external simulator is
 * involved: the dummy fitness derives the {@link GAObjectives} directly from the
 * chromosome's stored objective values, mirroring what {@code MOCloudFitness}
 * would return after a simulator run.</p>
 *
 * <p>The goal is to prove that the selection / crossover / mutation / dominance
 * pipeline of every algorithm executes without throwing and leaves a usable
 * population behind. The dummy {@code mutate()} and {@code crossover()} always
 * return non-null offspring, which is required by algorithms such as PAES whose
 * evolve loop spins until a mutation succeeds.</p>
 */
public class MOAlgorithmSmokeTest {

    private static final int POP_SIZE = 10;
    private static final int LOOPS = 3;

    private static PopulationMO<DummyMOChromosome> freshPopulation() {
        PopulationMO<DummyMOChromosome> pop = new PopulationMO<DummyMOChromosome>();
        for (int i = 0; i < POP_SIZE; i++) {
            // Spread the individuals over a Pareto-ish trade-off curve so the
            // dominance machinery has non-trivial fronts to sort.
            double energy = 100.0 - i * 5.0;   // decreasing energy ...
            double time = 10.0 + i * 5.0;      // ... increasing time
            pop.addChromosome(new DummyMOChromosome(i, energy, time));
        }
        return pop;
    }

    private static void assertUsable(MultiObjectiveGeneticAlgorithm<DummyMOChromosome, GAObjectives> alg) {
        assertNotNull("getBest() must return an individual", alg.getBest());
        assertTrue("population must not be empty after evolution",
                alg.getPopulation().getSize() > 0);
    }

    @Test
    public void smokeMOGA() {
        MOGA<DummyMOChromosome, GAObjectives> alg =
                new MOGA<DummyMOChromosome, GAObjectives>(freshPopulation(), new DummyMOFitness());
        alg.evolve(LOOPS);
        assertUsable(alg);
    }

    @Test
    public void smokeVEGA() {
        VEGA<DummyMOChromosome, GAObjectives> alg =
                new VEGA<DummyMOChromosome, GAObjectives>(freshPopulation(), 2, new DummyMOFitness());
        alg.evolve(LOOPS);
        assertUsable(alg);
    }

    @Test
    public void smokeVEGA2() {
        VEGA2<DummyMOChromosome, GAObjectives> alg =
                new VEGA2<DummyMOChromosome, GAObjectives>(freshPopulation(), 2, new DummyMOFitness());
        alg.evolve(LOOPS);
        assertUsable(alg);
    }

    @Test
    public void smokeNSGAII() {
        NSGAII<DummyMOChromosome, GAObjectives> alg =
                new NSGAII<DummyMOChromosome, GAObjectives>(freshPopulation(), new DummyMOFitness(), 2);
        alg.evolve(LOOPS);
        assertUsable(alg);
    }

    @Test
    public void smokeNSGAII2() {
        NSGAII2<DummyMOChromosome, GAObjectives> alg =
                new NSGAII2<DummyMOChromosome, GAObjectives>(freshPopulation(), new DummyMOFitness(), 2);
        alg.evolve(LOOPS);
        assertUsable(alg);
    }

    @Test
    public void smokeSPEA2() {
        SPEA2<DummyMOChromosome, GAObjectives> alg =
                new SPEA2<DummyMOChromosome, GAObjectives>(freshPopulation(), new DummyMOFitness(), 50, 1.5);
        alg.evolve(LOOPS);
        assertUsable(alg);
    }

    @Test
    public void smokeSPEA3() {
        SPEA3<DummyMOChromosome, GAObjectives> alg =
                new SPEA3<DummyMOChromosome, GAObjectives>(freshPopulation(), new DummyMOFitness(), 50, 1.5);
        alg.evolve(LOOPS);
        assertUsable(alg);
    }

    // PAES variants archive solutions in an AdaptiveCellGrid sized divisions^2,
    // where divisions = 2^bisections. The grid now normalizes objective values
    // onto its bisection grid using the adaptive min/max bounds, so a coarse
    // resolution is safe regardless of the objective scale (~50 here).
    // bisections=2 -> 4 divisions -> 16 cells, the canonical small PAES grid.
    private static final int PAES_BISECTIONS = 2;

    @Test
    public void smokePAES() {
        PAES<DummyMOChromosome, GAObjectives> alg =
                new PAES<DummyMOChromosome, GAObjectives>(freshPopulation(), new DummyMOFitness(), PAES_BISECTIONS, POP_SIZE);
        alg.evolve(LOOPS);
        assertUsable(alg);
    }

    @Test
    public void smokePAES2() {
        PAES2<DummyMOChromosome, GAObjectives> alg =
                new PAES2<DummyMOChromosome, GAObjectives>(freshPopulation(), new DummyMOFitness(), PAES_BISECTIONS, POP_SIZE);
        alg.evolve(LOOPS);
        assertUsable(alg);
    }

    // ── Test doubles ───────────────────────────────────────────────────────────

    /**
     * Lightweight chromosome carrying two positive objectives. {@code mutate()}
     * and {@code crossover()} always yield valid, non-null offspring so that no
     * algorithm's evolve loop can spin forever or dereference null.
     */
    static class DummyMOChromosome implements Chromosome<DummyMOChromosome> {

        private static final AtomicInteger SEQ = new AtomicInteger(1000);

        private int id;
        private double energy;
        private double time;
        private int rank;
        private double crowding;

        DummyMOChromosome(int id, double energy, double time) {
            this.id = id;
            this.energy = Math.max(1.0, energy);
            this.time = Math.max(1.0, time);
        }

        @Override
        public List<DummyMOChromosome> crossover(DummyMOChromosome other) {
            List<DummyMOChromosome> kids = new ArrayList<DummyMOChromosome>();
            double e = Math.max(1.0, (this.energy + other.energy) / 2.0);
            double t = Math.max(1.0, (this.time + other.time) / 2.0);
            kids.add(new DummyMOChromosome(SEQ.getAndIncrement(), e + 1, t - 0.5));
            kids.add(new DummyMOChromosome(SEQ.getAndIncrement(), e - 0.5, t + 1));
            return kids;
        }

        @Override
        public DummyMOChromosome mutate() {
            // Always non-null: required by PAES' do/while(mutated == null) loop.
            return new DummyMOChromosome(SEQ.getAndIncrement(),
                    Math.max(1.0, energy + (id % 3) - 1),
                    Math.max(1.0, time + (id % 2)));
        }

        @Override public boolean isFitnessValid() { return energy > 0 && time > 0; }
        @Override public int getId() { return id; }

        @Override
        public double getObjective(EGAObjectives obj) {
            return obj == EGAObjectives.eENERGY ? energy : time;
        }

        @Override public void setDominated(int d) {}
        @Override public void setnCrowdDensity(double d) {}
        @Override public void setFitness() {}
        @Override public double getNumDom() { return 0; }
        @Override public double getFitness() { return 0; }
        @Override public void setCrowdingDistance(double d) { this.crowding = d; }
        @Override public void addToCrowdingDistance(double d) { this.crowding += d; }
        @Override public double getCrowdingDistance() { return crowding; }
        @Override public int getRank() { return rank; }
        @Override public void setRank(int r) { this.rank = r; }
        @Override public DummyMOChromosome dup() { return new DummyMOChromosome(id, energy, time); }
        @Override public int[] getObjectivesIndex() { return new int[] {0, 1}; }
        @Override public String toString() { return "(" + id + ", " + energy + ", " + time + ")"; }
    }

    /** Fitness double that turns the chromosome's objectives into {@link GAObjectives}. */
    static class DummyMOFitness implements Fitness<DummyMOChromosome, GAObjectives> {
        @Override
        public GAObjectives calculate(DummyMOChromosome c) {
            GAObjectives o = new GAObjectives();
            o.addObjective(EGAObjectives.eENERGY, c.energy);
            o.addObjective(EGAObjectives.eTIME, c.time);
            return o;
        }
        @Override
        public GAObjectives calculate(DummyMOChromosome c, EGAObjectives obj) {
            GAObjectives o = new GAObjectives();
            o.addObjective(obj, c.getObjective(obj));
            return o;
        }
    }
}
