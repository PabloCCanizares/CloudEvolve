package platform;

import dataParser.cloud.ECloudSimulator;

/**
 * Factory that resolves the {@link SimulatorPlatform} strategy for a given
 * {@link ECloudSimulator}. This is the single point of per-simulator dispatch
 * that replaces the duplicated {@code switch (eSimulator)} blocks.
 */
public final class SimulatorPlatforms {

    private static final SimulatorPlatform CLOUDSIM_STORAGE = new CloudSimStoragePlatform();
    private static final SimulatorPlatform SIMGRID = new SimGridPlatform();

    private SimulatorPlatforms() {
    }

    /**
     * Resolves the strategy for a simulator.
     *
     * <p>Mirrors the legacy {@code switch (eSimulator)} default exactly: SimGrid
     * maps to its own platform, and <em>everything else</em> (including a null
     * simulator) falls back to CloudSim-Storage.</p>
     */
    public static SimulatorPlatform of(ECloudSimulator simulator) {
        if (simulator == ECloudSimulator.eSIMGRID) {
            return SIMGRID;
        }
        return CLOUDSIM_STORAGE;
    }
}
