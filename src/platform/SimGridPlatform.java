package platform;

/** {@link SimulatorPlatform} for the SimGrid backend. */
public final class SimGridPlatform implements SimulatorPlatform {

    @Override
    public String evolutionaryBasePath() {
        return PlatformPaths.evolutionaryBase("simGrid");
    }
}
