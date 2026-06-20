package core;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import algorithms.moga.GAObjectives;
import algorithms.moga.MultiObjectiveGeneticAlgorithm;
import dataParser.cloud.ECloudSimulator;
import entities.MOCloudChromosome;

/**
 * Characterization test that pins the <b>current</b> behaviour of the
 * "simulator -&gt; base path" decision in {@link MOCloudOrchestrator#doConfigure}.
 *
 * <p>This is a safety net for the planned refactor that replaces the scattered
 * {@code switch (eSimulator)} blocks with a per-simulator strategy: it locks in
 * both the {@code args[0] -> ECloudSimulator} mapping and the
 * {@code ECloudSimulator -> strPathBase} mapping exactly as they behave today
 * (including the case-sensitive {@code indexOf} quirk and the silent
 * "anything unknown falls back to CloudSim-Storage" default), so the refactor
 * can prove it preserves them.</p>
 */
public class SimulatorPathCharacterizationTest {

    private static final String CLOUDSIM_PATH = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary";
    private static final String SIMGRID_PATH  = "/localSpace/cloudEnergy/simGrid/evolutionary";

    // The base path is now resolved from a configurable root. Pin it to the
    // legacy default so these assertions stay deterministic regardless of any
    // CLOUDEVOLVE_WORKSPACE set on the machine running the tests.
    private static final String HOME_PROPERTY = "cloudevolve.workspace";
    private static final String LEGACY_HOME = "/localSpace/cloudEnergy";
    private String savedHome;

    @Before
    public void pinHome() {
        savedHome = System.getProperty(HOME_PROPERTY);
        System.setProperty(HOME_PROPERTY, LEGACY_HOME);
    }

    @After
    public void restoreHome() {
        if (savedHome == null) {
            System.clearProperty(HOME_PROPERTY);
        } else {
            System.setProperty(HOME_PROPERTY, savedHome);
        }
    }

    /** Minimal concrete orchestrator: we only exercise {@code doConfigure}. */
    private static final class ProbeOrchestrator extends MOCloudOrchestrator {
        @Override
        public MultiObjectiveGeneticAlgorithm<MOCloudChromosome, GAObjectives> instanceAlgorithm() {
            return null;
        }
    }

    /** Runs doConfigure with the given simulator token and an (unused) population dir. */
    private static ProbeOrchestrator configure(String simulatorToken) {
        ProbeOrchestrator orch = new ProbeOrchestrator();
        // Two args only: no explicit path (args[5]) -> strPathBase stays null ->
        // the simulator switch under test runs.
        orch.doConfigure(new String[] { simulatorToken, "popDir" }, "MOGA");
        return orch;
    }

    @Test
    public void cloudsimstorageTokenMapsToCloudSimStorage() {
        ProbeOrchestrator orch = configure("cloudsimstorage");
        assertEquals(ECloudSimulator.eCLOUDSIMSTORAGE, orch.eSimulator);
        assertEquals(CLOUDSIM_PATH, orch.strPathBase);
    }

    @Test
    public void simgridTokenMapsToSimGrid() {
        ProbeOrchestrator orch = configure("simgrid");
        assertEquals(ECloudSimulator.eSIMGRID, orch.eSimulator);
        assertEquals(SIMGRID_PATH, orch.strPathBase);
    }

    @Test
    public void unknownTokenFallsBackToCloudSimStorage() {
        ProbeOrchestrator orch = configure("totally-unknown");
        assertEquals(ECloudSimulator.eCLOUDSIMSTORAGE, orch.eSimulator);
        assertEquals(CLOUDSIM_PATH, orch.strPathBase);
    }

    /**
     * Documents the current case-sensitivity quirk: the production launchers pass
     * "eCloudSimStorage", but {@code indexOf("cloudsimstorage")} is case-sensitive
     * and misses, so the token is classified via the default branch. The end
     * result still resolves to CloudSim-Storage, but for the "wrong" reason — the
     * refactor must keep this observable outcome.
     */
    @Test
    public void mixedCaseProductionTokenResolvesViaDefaultBranch() {
        ProbeOrchestrator orch = configure("eCloudSimStorage");
        assertEquals(ECloudSimulator.eCLOUDSIMSTORAGE, orch.eSimulator);
        assertEquals(CLOUDSIM_PATH, orch.strPathBase);
    }
}
