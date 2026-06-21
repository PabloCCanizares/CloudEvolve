package platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import dataParser.cloud.ECloudSimulator;

/**
 * Tests for the per-simulator path strategy and its factory.
 *
 * <p>The factory only supports the simulators with a backend strategy
 * (CloudSim-Storage, SimGrid and the surrogate ML model); any other value fails
 * fast with an {@link IllegalArgumentException} instead of silently falling back.
 * {@link #supportedSimulatorsKeepTheLegacyBasePath()} pins the two supported
 * paths and {@link #unsupportedSimulatorThrows()} pins the fail-fast contract.</p>
 *
 * <p>The root is driven through the {@code cloudevolve.workspace} system property so
 * the assertions are deterministic regardless of any ambient
 * {@code CLOUDEVOLVE_WORKSPACE} on the developer's machine.</p>
 */
public class SimulatorPlatformsTest {

    private String savedHome;

    @Before
    public void pinHomeToLegacyDefault() {
        savedHome = System.getProperty(PlatformPaths.WORKSPACE_PROPERTY);
        System.setProperty(PlatformPaths.WORKSPACE_PROPERTY, PlatformPaths.DEFAULT_WORKSPACE);
    }

    @After
    public void restoreHome() {
        if (savedHome == null) {
            System.clearProperty(PlatformPaths.WORKSPACE_PROPERTY);
        } else {
            System.setProperty(PlatformPaths.WORKSPACE_PROPERTY, savedHome);
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
        assertTrue(SimulatorPlatforms.of(ECloudSimulator.eSURROGATE) instanceof SurrogatePlatform);
        assertTrue(SimulatorPlatforms.of(ECloudSimulator.eHYBRID) instanceof HybridPlatform);
    }

    /** The surrogate and hybrid backends keep their own evolutionary subtrees. */
    @Test
    public void surrogateAndHybridHaveTheirOwnBasePath() {
        assertEquals("/localSpace/cloudEnergy/surrogate/evolutionary",
                SimulatorPlatforms.of(ECloudSimulator.eSURROGATE).evolutionaryBasePath());
        assertEquals("/localSpace/cloudEnergy/hybrid/evolutionary",
                SimulatorPlatforms.of(ECloudSimulator.eHYBRID).evolutionaryBasePath());
    }

    /** A configured root is honoured, with each simulator keeping its subtree. */
    @Test
    public void respectsConfiguredHome() {
        System.setProperty(PlatformPaths.WORKSPACE_PROPERTY, "/Users/pablocc/cloudEvolution");

        assertEquals("/Users/pablocc/cloudEvolution/cloudsimStorage/evolutionary",
                SimulatorPlatforms.of(ECloudSimulator.eCLOUDSIMSTORAGE).evolutionaryBasePath());
        assertEquals("/Users/pablocc/cloudEvolution/simGrid/evolutionary",
                SimulatorPlatforms.of(ECloudSimulator.eSIMGRID).evolutionaryBasePath());
    }

    /** Every simulator without a backend, and {@code null}, fails fast. */
    @Test
    public void unsupportedSimulatorThrows() {
        for (ECloudSimulator s : ECloudSimulator.values()) {
            if (s == ECloudSimulator.eCLOUDSIMSTORAGE || s == ECloudSimulator.eSIMGRID
                    || s == ECloudSimulator.eSURROGATE || s == ECloudSimulator.eHYBRID) {
                continue;
            }
            assertThrows("expected fail-fast for " + s,
                    IllegalArgumentException.class, () -> SimulatorPlatforms.of(s));
        }
        assertThrows(IllegalArgumentException.class, () -> SimulatorPlatforms.of(null));
    }

    /**
     * Back-compat: when the new {@code cloudevolve.workspace} is absent, the
     * deprecated {@code cloudevolve.home} / {@code CLOUDEVOLVE_HOME} is still
     * honoured as a fallback.
     */
    @Test
    public void deprecatedHomePropertyIsHonouredAsFallback() {
        Assume.assumeTrue("ambient CLOUDEVOLVE_WORKSPACE would take precedence",
                System.getenv(PlatformPaths.WORKSPACE_ENV) == null);
        System.clearProperty(PlatformPaths.WORKSPACE_PROPERTY);
        System.setProperty(PlatformPaths.LEGACY_PROPERTY, "/legacy/root");
        try {
            assertEquals("/legacy/root/cloudsimStorage/evolutionary",
                    SimulatorPlatforms.of(ECloudSimulator.eCLOUDSIMSTORAGE).evolutionaryBasePath());
        } finally {
            System.clearProperty(PlatformPaths.LEGACY_PROPERTY);
        }
    }
}
