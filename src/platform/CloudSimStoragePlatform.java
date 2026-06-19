package platform;

/** {@link SimulatorPlatform} for the CloudSim-Storage backend. */
public final class CloudSimStoragePlatform implements SimulatorPlatform {

    @Override
    public String evolutionaryBasePath() {
        return PlatformPaths.evolutionaryBase("cloudsimStorage");
    }
}
