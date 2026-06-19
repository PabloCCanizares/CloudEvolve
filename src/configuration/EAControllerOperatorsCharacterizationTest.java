package configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;

import org.junit.Test;

import dataParser.cloud.ECloudSimulator;
import mutation.MutationOperator;

/**
 * Characterization test pinning the <b>current</b> per-simulator operator
 * registration in {@link EAController#initializeMutationOperators} and
 * {@link EAController#initializeCrossoverOperators}.
 *
 * <p>Both methods branch on {@code switch (ePlatformType)}; this locks in exactly
 * which operators (numbers, enabled flags and the probability each consumes from
 * the supplied list) each simulator produces, so the planned strategy refactor
 * can prove behavioural equivalence. The {@code mutationOperatorList} /
 * {@code crossoverOperatorList} fields are package-private, so this test lives in
 * the {@code configuration} package and reads them directly.</p>
 */
public class EAControllerOperatorsCharacterizationTest {

    /** Distinct probabilities so we can assert which operator consumed which. */
    private static LinkedList<Double> probs() {
        LinkedList<Double> p = new LinkedList<Double>();
        for (int i = 1; i <= 8; i++) {
            p.add(i / 10.0); // 0.1, 0.2, ... 0.8
        }
        return p;
    }

    private static EAController controllerFor(ECloudSimulator sim) {
        EAController c = new EAController(); // fresh instance: avoid singleton state
        c.setPlaftormInfo(sim);
        return c;
    }

    // ── Mutation operators ───────────────────────────────────────────────────

    @Test
    public void cloudSimStorageMutationOperators() {
        EAController c = controllerFor(ECloudSimulator.eCLOUDSIMSTORAGE);
        c.initializeMutationOperators(probs());

        assertEquals(4, c.mutationOperatorList.size());
        assertOperator(c.mutationOperatorList.get(0), 1, 0.1, true);
        assertOperator(c.mutationOperatorList.get(1), 2, 0.2, true);
        assertOperator(c.mutationOperatorList.get(2), 3, 0.3, true);
        assertOperator(c.mutationOperatorList.get(3), 7, 0.4, true);
    }

    @Test
    public void simGridMutationOperators() {
        EAController c = controllerFor(ECloudSimulator.eSIMGRID);
        c.initializeMutationOperators(probs());

        assertEquals(8, c.mutationOperatorList.size());
        assertOperator(c.mutationOperatorList.get(0), 1, 0.1, true);
        assertOperator(c.mutationOperatorList.get(1), 2, 0.2, false);
        assertOperator(c.mutationOperatorList.get(2), 3, 0.3, false);
        assertOperator(c.mutationOperatorList.get(3), 4, 0.4, false);
        assertOperator(c.mutationOperatorList.get(4), 5, 0.5, false);
        assertOperator(c.mutationOperatorList.get(5), 6, 0.6, false);
        assertOperator(c.mutationOperatorList.get(6), 7, 0.7, true);
        assertOperator(c.mutationOperatorList.get(7), 8, 0.8, false);
    }

    /** With fewer than 4 probabilities the CloudSim branch registers nothing. */
    @Test
    public void cloudSimStorageMutationNeedsAtLeastFourProbabilities() {
        EAController c = controllerFor(ECloudSimulator.eCLOUDSIMSTORAGE);
        LinkedList<Double> few = new LinkedList<Double>();
        few.add(0.1);
        few.add(0.2);
        few.add(0.3);
        c.initializeMutationOperators(few);

        assertTrue(c.mutationOperatorList.isEmpty());
    }

    // ── Crossover operators ──────────────────────────────────────────────────

    @Test
    public void cloudSimStorageCrossoverOperators() {
        EAController c = controllerFor(ECloudSimulator.eCLOUDSIMSTORAGE);
        c.initializeCrossoverOperators();

        assertEquals(2, c.crossoverOperatorList.size());
        assertOperator(c.crossoverOperatorList.get(0), 1, 0.0, true);
        assertOperator(c.crossoverOperatorList.get(1), 2, 100.0, true);
    }

    @Test
    public void simGridRegistersNoCrossoverOperators() {
        EAController c = controllerFor(ECloudSimulator.eSIMGRID);
        c.initializeCrossoverOperators();

        assertTrue(c.crossoverOperatorList.isEmpty());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static void assertOperator(MutationOperator op, int number, double prob, boolean enabled) {
        assertEquals("operator number", number, op.getnOperator());
        assertEquals("enabled flag", enabled, op.isEnabled());
        assertEquals("probability", prob, probabilityOf(op), 1e-9);
    }

    private static double probabilityOf(MutationOperator op) {
        if (op instanceof EAMutationOperator) {
            return ((EAMutationOperator) op).getProbability();
        }
        return ((EACrossoverOperator) op).getProbability();
    }
}
