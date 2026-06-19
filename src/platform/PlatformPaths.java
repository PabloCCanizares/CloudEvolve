package platform;

/**
 * Resolves the configurable root directory under which each simulator backend
 * stores its evolutionary runs, so the base path no longer has to be hard-coded
 * per machine.
 *
 * <p>Resolution order (first non-empty wins):</p>
 * <ol>
 *   <li>JVM system property {@code cloudevolve.home} (handy for {@code -D}
 *       overrides and tests),</li>
 *   <li>environment variable {@code CLOUDEVOLVE_HOME},</li>
 *   <li>the legacy default {@value #DEFAULT_HOME}.</li>
 * </ol>
 *
 * <p>Each platform appends {@code <simulatorDir>/evolutionary}; e.g. with
 * {@code CLOUDEVOLVE_HOME=/Users/pablocc/cloudEvolution} the CloudSim-Storage
 * base becomes {@code /Users/pablocc/cloudEvolution/cloudsimStorage/evolutionary}.</p>
 */
public final class PlatformPaths {

    /** System property that overrides the evolutionary-runs root. */
    public static final String HOME_PROPERTY = "cloudevolve.home";
    /** Environment variable that sets the evolutionary-runs root. */
    public static final String HOME_ENV = "CLOUDEVOLVE_HOME";
    /** Fallback root used when neither the property nor the env var is set. */
    public static final String DEFAULT_HOME = "/localSpace/cloudEnergy";

    private PlatformPaths() {
    }

    /** The configurable evolutionary-runs root, without a trailing slash. */
    public static String home() {
        String property = System.getProperty(HOME_PROPERTY);
        if (property != null && !property.isEmpty()) {
            return property;
        }
        String env = System.getenv(HOME_ENV);
        if (env != null && !env.isEmpty()) {
            return env;
        }
        return DEFAULT_HOME;
    }

    /** Builds {@code <home>/<simulatorDir>/evolutionary} for a simulator. */
    public static String evolutionaryBase(String simulatorDir) {
        return home() + "/" + simulatorDir + "/evolutionary";
    }
}
