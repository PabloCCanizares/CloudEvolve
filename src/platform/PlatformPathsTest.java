package platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link PlatformPaths#resolveWorkspacePath} — the {@code ${workspace}}
 * token expansion used to make stored {@code .mtc} / {@code .tc} paths portable.
 * The workspace root is pinned through the system property so the assertions are
 * deterministic.
 */
public class PlatformPathsTest {

    private String savedWorkspace;

    @Before
    public void pinWorkspace() {
        savedWorkspace = System.getProperty(PlatformPaths.WORKSPACE_PROPERTY);
        System.setProperty(PlatformPaths.WORKSPACE_PROPERTY, "/ws");
    }

    @After
    public void restoreWorkspace() {
        if (savedWorkspace == null) {
            System.clearProperty(PlatformPaths.WORKSPACE_PROPERTY);
        } else {
            System.setProperty(PlatformPaths.WORKSPACE_PROPERTY, savedWorkspace);
        }
    }

    @Test
    public void expandsTheWorkspaceToken() {
        assertEquals("/ws/cloudsimStorage/evolutionary/Al_w3/tcInput/input_00000.tc",
                PlatformPaths.resolveWorkspacePath("${workspace}/cloudsimStorage/evolutionary/Al_w3/tcInput/input_00000.tc"));
    }

    @Test
    public void expandsEveryOccurrence() {
        assertEquals("/ws/in.tc -> /ws/out.tc",
                PlatformPaths.resolveWorkspacePath("${workspace}/in.tc -> ${workspace}/out.tc"));
    }

    @Test
    public void absolutePathIsReturnedUnchanged() {
        // Existing absolute .mtc paths keep working verbatim.
        assertEquals("/localSpace/cloudEnergy/x/input.tc",
                PlatformPaths.resolveWorkspacePath("/localSpace/cloudEnergy/x/input.tc"));
    }

    @Test
    public void relativePathIsReturnedUnchanged() {
        // Token-less relative paths stay relative (they are CWD-relative today).
        assertEquals("Al_w3/input.tc", PlatformPaths.resolveWorkspacePath("Al_w3/input.tc"));
    }

    @Test
    public void nullIsReturnedUnchanged() {
        assertNull(PlatformPaths.resolveWorkspacePath(null));
    }
}
