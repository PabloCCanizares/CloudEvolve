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
     * <p>Only the simulators with a real backend are supported; any other value
     * (or {@code null}) is rejected with an {@link IllegalArgumentException}
     * rather than silently falling back, so an unsupported simulator fails fast
     * instead of running with the wrong configuration.</p>
     *
     * @throws IllegalArgumentException if {@code simulator} has no platform strategy
     */
    public static SimulatorPlatform of(ECloudSimulator simulator) {
        if (simulator == ECloudSimulator.eCLOUDSIMSTORAGE) {
            return CLOUDSIM_STORAGE;
        }
        if (simulator == ECloudSimulator.eSIMGRID) {
            return SIMGRID;
        }
        throw new IllegalArgumentException("Unsupported simulator (no platform strategy): " + simulator);
    }
}
