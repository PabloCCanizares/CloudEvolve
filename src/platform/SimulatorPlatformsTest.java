package platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dataParser.cloud.ECloudSimulator;

/**
 * Tests for the per-simulator path strategy and its factory.
 *
 * <p>{@link #factoryReproducesLegacyBasePathForEverySimulator()} is the
 * behaviour-preservation gate: with the root pinned to the legacy default it
 * compares the factory against a verbatim copy of the original
 * {@code switch (eSimulator)} for every enum value (including the silent default
 * fallback). {@link #respectsConfiguredHome()} covers the new configurable root.</p>
 *
 * <p>The root is driven through the {@code cloudevolve.home} system property so
 * the assertions are deterministic regardless of any ambient
 * {@code CLOUDEVOLVE_HOME} on the developer's machine.</p>
 */
public class SimulatorPlatformsTest {

    private String savedHome;

    @Before
    public void pinHomeToLegacyDefault() {
        savedHome = System.getProperty(PlatformPaths.HOME_PROPERTY);
        System.setProperty(PlatformPaths.HOME_PROPERTY, PlatformPaths.DEFAULT_HOME);
    }

    @After
    public void restoreHome() {
        if (savedHome == null) {
            System.clearProperty(PlatformPaths.HOME_PROPERTY);
        } else {
            System.setProperty(PlatformPaths.HOME_PROPERTY, savedHome);
        }
    }

    /**
     * Verbatim copy of the base-path switch as it stood before the extraction
     * (in MOCloudOrchestrator.doConfigure and Cloud_GA), kept as the oracle.
     */
    private static String legacyBasePath(ECloudSimulator eSimulator) {
        switch (eSimulator) {
        case eCLOUDSIMSTORAGE:
            return "/localSpace/cloudEnergy/cloudsimStorage/evolutionary";
        case eSIMGRID:
            return "/localSpace/cloudEnergy/simGrid/evolutionary";
        default:
            return "/localSpace/cloudEnergy/cloudsimStorage/evolutionary";
        }
    }

    @Test
    public void factoryReproducesLegacyBasePathForEverySimulator() {
        for (ECloudSimulator s : ECloudSimulator.values()) {
            assertEquals("base path for " + s,
                    legacyBasePath(s), SimulatorPlatforms.of(s).evolutionaryBasePath());
        }
    }

    /** A configured root is honoured, with each simulator keeping its subtree. */
    @Test
    public void respectsConfiguredHome() {
        System.setProperty(PlatformPaths.HOME_PROPERTY, "/Users/pablocc/cloudEvolution");

        assertEquals("/Users/pablocc/cloudEvolution/cloudsimStorage/evolutionary",
                SimulatorPlatforms.of(ECloudSimulator.eCLOUDSIMSTORAGE).evolutionaryBasePath());
        assertEquals("/Users/pablocc/cloudEvolution/simGrid/evolutionary",
                SimulatorPlatforms.of(ECloudSimulator.eSIMGRID).evolutionaryBasePath());
    }

    @Test
    public void factoryReturnsAStrategyForEverySimulator() {
        for (ECloudSimulator s : ECloudSimulator.values()) {
            assertNotNull("no strategy for " + s, SimulatorPlatforms.of(s));
        }
    }

    @Test
    public void factorySelectsExpectedConcreteStrategies() {
        assertTrue(SimulatorPlatforms.of(ECloudSimulator.eCLOUDSIMSTORAGE) instanceof CloudSimStoragePlatform);
        assertTrue(SimulatorPlatforms.of(ECloudSimulator.eSIMGRID) instanceof SimGridPlatform);
    }

    /** Any non-SimGrid simulator falls back to CloudSim-Storage (legacy default). */
    @Test
    public void unknownSimulatorFallsBackToCloudSimStorage() {
        assertTrue(SimulatorPlatforms.of(ECloudSimulator.eCLOUDSIMPLUS) instanceof CloudSimStoragePlatform);
    }
}
