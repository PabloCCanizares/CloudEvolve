package platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import dataParser.cloud.ECloudSimulator;
import dataParser.metadata.MetaTestCase;

/**
 * Verifies the per-simulator launch dispatch through the {@link SimulatorExecution}
 * seam, without starting any real simulator: a spy records which execution path
 * and command each platform uses. This is the unit-level replacement for the old
 * {@code switch (platformInfo)} in {@code MT_Handler.executeSingleTC}.
 */
public class SimulatorExecutionDispatchTest {

    /** Records the commands each execution path receives. */
    private static final class SpyExecution implements SimulatorExecution {
        final List<String> cloudSimCommands = new ArrayList<>();
        final List<String> simGridCommands = new ArrayList<>();

        @Override public boolean executeCommand(String command) { cloudSimCommands.add(command); return true; }
        @Override public boolean executeCommandSimGrid(String command) { simGridCommands.add(command); return true; }
        @Override public String timeoutHeader() { return "TIMEOUT"; }
        @Override public String simulatorPath() { return "/path/to/sim.jar"; }
    }

    @Test
    public void cloudSimStorageLaunchesViaExecuteCommandWithLocalePinned() {
        SpyExecution spy = new SpyExecution();
        MetaTestCase tc = new MetaTestCase();
        tc.setFilePath("/tmp/tc_00000.mtc");

        boolean ok = SimulatorPlatforms.of(ECloudSimulator.eCLOUDSIMSTORAGE).execute(spy, tc);

        assertTrue(ok);
        assertTrue("must not use the SimGrid path", spy.simGridCommands.isEmpty());
        assertEquals(1, spy.cloudSimCommands.size());

        String cmd = spy.cloudSimCommands.get(0);
        assertTrue(cmd.startsWith("TIMEOUT"));
        assertTrue("locale must be pinned", cmd.contains("-Duser.language=en -Duser.country=US"));
        assertTrue(cmd.contains("/path/to/sim.jar"));
        assertTrue(cmd.contains("/tmp/tc_00000.mtc"));
    }

    @Test
    public void simGridCleansTmpThenLaunchesViaSimGridJar() {
        SpyExecution spy = new SpyExecution();
        MetaTestCase tc = new MetaTestCase();
        tc.setFilePath("/tmp/tc_00000.mtc");
        tc.setTcOutput("/tmp/output_00000.tc");

        boolean ok = SimulatorPlatforms.of(ECloudSimulator.eSIMGRID).execute(spy, tc);

        assertTrue(ok);
        assertTrue("must not use the CloudSim path", spy.cloudSimCommands.isEmpty());
        assertEquals(2, spy.simGridCommands.size());
        assertEquals("rm -r /tmp/simgrid*", spy.simGridCommands.get(0));

        String launch = spy.simGridCommands.get(1);
        assertTrue(launch.contains("simGrid.jar"));
        assertTrue(launch.contains("/tmp/tc_00000.mtc"));
        assertTrue(launch.contains("&>/tmp/output_00000.tc"));
    }
}
