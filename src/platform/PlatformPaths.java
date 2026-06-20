package platform;

/**
 * Resolves the configurable <b>workspace root</b> under which the simulator and
 * its data (evolutionary runs, initial populations, {@code .mtc} / {@code .tc})
 * live, so the paths no longer have to be hard-coded per machine.
 *
 * <p>Resolution order (first non-empty wins):</p>
 * <ol>
 *   <li>JVM system property {@code cloudevolve.workspace} (handy for {@code -D}
 *       overrides and tests),</li>
 *   <li>environment variable {@code CLOUDEVOLVE_WORKSPACE},</li>
 *   <li>the <b>deprecated</b> {@code cloudevolve.home} / {@code CLOUDEVOLVE_HOME},
 *       still honoured as a fallback,</li>
 *   <li>the default {@value #DEFAULT_WORKSPACE}.</li>
 * </ol>
 *
 * <p>Each simulator appends {@code <simulatorDir>/evolutionary}; e.g. with
 * {@code CLOUDEVOLVE_WORKSPACE=/Users/pablocc/cloudEvolution} the CloudSim-Storage
 * base becomes {@code /Users/pablocc/cloudEvolution/cloudsimStorage/evolutionary}.</p>
 */
public final class PlatformPaths {

    /** System property that overrides the workspace root. */
    public static final String WORKSPACE_PROPERTY = "cloudevolve.workspace";
    /** Environment variable that sets the workspace root. */
    public static final String WORKSPACE_ENV = "CLOUDEVOLVE_WORKSPACE";

    /** @deprecated renamed to {@link #WORKSPACE_PROPERTY}; still honoured as a fallback. */
    @Deprecated
    public static final String LEGACY_PROPERTY = "cloudevolve.home";
    /** @deprecated renamed to {@link #WORKSPACE_ENV}; still honoured as a fallback. */
    @Deprecated
    public static final String LEGACY_ENV = "CLOUDEVOLVE_HOME";

    /** Fallback root used when nothing else is set. */
    public static final String DEFAULT_WORKSPACE = "/localSpace/cloudEnergy";

    private PlatformPaths() {
    }

    /** The configurable workspace root, without a trailing slash. */
    public static String workspace() {
        String[] candidates = {
                System.getProperty(WORKSPACE_PROPERTY),
                System.getenv(WORKSPACE_ENV),
                System.getProperty(LEGACY_PROPERTY),
                System.getenv(LEGACY_ENV),
        };
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isEmpty()) {
                return candidate;
            }
        }
        return DEFAULT_WORKSPACE;
    }

    /** Builds {@code <workspace>/<simulatorDir>/evolutionary} for a simulator. */
    public static String evolutionaryBase(String simulatorDir) {
        return workspace() + "/" + simulatorDir + "/evolutionary";
    }

    /** Token, usable in stored paths (e.g. {@code .mtc} files), that expands to {@link #workspace()}. */
    public static final String WORKSPACE_TOKEN = "${workspace}";

    /**
     * Expands every {@link #WORKSPACE_TOKEN} in a stored path to the resolved
     * {@link #workspace()} root, so {@code .mtc} / {@code .tc} paths can be made
     * portable.
     *
     * <p>Paths <b>without</b> the token — absolute or relative — are returned
     * unchanged, so existing data keeps resolving exactly as it does today
     * (relative paths stay relative to the working directory). {@code null} is
     * returned unchanged.</p>
     */
    public static String resolveWorkspacePath(String stored) {
        if (stored == null || !stored.contains(WORKSPACE_TOKEN)) {
            return stored;
        }
        return stored.replace(WORKSPACE_TOKEN, workspace());
    }
}
