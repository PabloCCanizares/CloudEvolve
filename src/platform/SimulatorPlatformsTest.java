package platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dataParser.cloud.ECloudSimulator;

/**
 * Tests for the per-simulator path strategy and its factory.
 *
 * <p>The factory only supports the simulators with a real backend
 * (CloudSim-Storage and SimGrid); any other value fails fast with an
 * {@link IllegalArgumentException} instead of silently falling back.
 * {@link #supportedSimulatorsKeepTheLegacyBasePath()} pins the two supported
 * paths and {@link #unsupportedSimulatorThrows()} pins the fail-fast contract.</p>
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

    @Test
    public void supportedSimulatorsKeepTheLegacyBasePath() {
        assertEquals("/localSpace/cloudEnergy/cloudsimStorage/evolutionary",
                SimulatorPlatforms.of(ECloudSimulator.eCLOUDSIMSTORAGE).evolutionaryBasePath());
        assertEquals("/localSpace/cloudEnergy/simGrid/evolutionary",
                SimulatorPlatforms.of(ECloudSimulator.eSIMGRID).evolutionaryBasePath());
    }

    @Test
    public void factorySelectsExpectedConcreteStrategies() {
        assertTrue(SimulatorPlatforms.of(ECloudSimulator.eCLOUDSIMSTORAGE) instanceof CloudSimStoragePlatform);
        assertTrue(SimulatorPlatforms.of(ECloudSimulator.eSIMGRID) instanceof SimGridPlatform);
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

    /** Every simulator without a backend, and {@code null}, fails fast. */
    @Test
    public void unsupportedSimulatorThrows() {
        for (ECloudSimulator s : ECloudSimulator.values()) {
            if (s == ECloudSimulator.eCLOUDSIMSTORAGE || s == ECloudSimulator.eSIMGRID) {
                continue;
            }
            assertThrows("expected fail-fast for " + s,
                    IllegalArgumentException.class, () -> SimulatorPlatforms.of(s));
        }
        assertThrows(IllegalArgumentException.class, () -> SimulatorPlatforms.of(null));
    }
}
