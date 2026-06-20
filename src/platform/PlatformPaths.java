package platform;

import auxiliar.WorkspacePaths;

/**
 * CloudEvolve-side facade over {@link auxiliar.WorkspacePaths}, the single source
 * of truth for the configurable workspace root and the {@code ${workspace}} path
 * token. That logic lives in the metamorphic-testing layer (MT.jar) so it can
 * resolve and emit the token when reading/writing {@code .mtc} files; this class
 * just re-exposes it and adds the CloudEvolve-specific
 * {@code <root>/<simulator>/evolutionary} layout.
 */
public final class PlatformPaths {

    /** System property that overrides the workspace root. */
    public static final String WORKSPACE_PROPERTY = WorkspacePaths.WORKSPACE_PROPERTY;
    /** Environment variable that sets the workspace root. */
    public static final String WORKSPACE_ENV = WorkspacePaths.WORKSPACE_ENV;

    /** @deprecated renamed to {@link #WORKSPACE_PROPERTY}; still honoured as a fallback. */
    @Deprecated
    public static final String LEGACY_PROPERTY = WorkspacePaths.LEGACY_PROPERTY;
    /** @deprecated renamed to {@link #WORKSPACE_ENV}; still honoured as a fallback. */
    @Deprecated
    public static final String LEGACY_ENV = WorkspacePaths.LEGACY_ENV;

    /** Fallback root used when nothing else is set. */
    public static final String DEFAULT_WORKSPACE = WorkspacePaths.DEFAULT_WORKSPACE;

    /** Token, usable in stored paths, that expands to {@link #workspace()}. */
    public static final String WORKSPACE_TOKEN = WorkspacePaths.WORKSPACE_TOKEN;

    private PlatformPaths() {
    }

    /** The configurable workspace root, without a trailing slash. */
    public static String workspace() {
        return WorkspacePaths.workspace();
    }

    /** Builds {@code <workspace>/<simulatorDir>/evolutionary} for a simulator. */
    public static String evolutionaryBase(String simulatorDir) {
        return workspace() + "/" + simulatorDir + "/evolutionary";
    }

    /** Expands the {@code ${workspace}} token in a stored path; see {@link WorkspacePaths#resolve}. */
    public static String resolveWorkspacePath(String stored) {
        return WorkspacePaths.resolve(stored);
    }
}
