package transformations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import dataParser.cloud.ECloudSimulator;
import dataParser.cloud.input.TcInput_cloud;
import mutation.MutableCloud.MutableCloud;

/**
 * Golden-master characterization of the CloudSim-Storage branch of
 * {@link TestCase2Cloud#transformTestcase2Cloud}.
 *
 * <p>The transform dispatches on {@code ePlatformInfo}; this pins the observable
 * output ({@code MutableCloud}) for a representative input so the planned
 * strategy refactor of that dispatch can prove it produces an identical cloud.
 * It also guards the rack/blade-grid fix: a flat {@code host.quantity} must end
 * up as exactly that many hosts (it used to silently collapse to zero whenever
 * {@code racks*blades} exceeded the node count).</p>
 *
 * <p>Only the CloudSim-Storage branch is characterized here; the SimGrid branch
 * delegates to {@link TC2Cloud_simGrid} and needs its own input fixture — a
 * follow-up once that strategy is extracted.</p>
 */
public class TestCase2CloudCharacterizationTest {

    /** A representative homogeneous host spec; {@code hostQuantity} varies per test. */
    private static TcInput_cloud input(int hostQuantity) {
        TcInput_cloud in = new TcInput_cloud();
        in.setHostQuantity(hostQuantity);
        in.setHostMips(1000);
        in.setHostPes(4);
        in.setHostRam(8192L);
        in.setHostRamSpeed(1000);
        in.setHostBw(1000L);
        in.setNetBandwidth(1000L);
        in.setNetLatency(1);
        in.setIOCapacity(1000000L);
        in.setIOMaxRate(1000L);
        return in;
    }

    private static MutableCloud transform(int hostQuantity) {
        TestCase2Cloud t = new TestCase2Cloud(ECloudSimulator.eCLOUDSIMSTORAGE);
        return t.transformTestcase2Cloud(ECloudSimulator.eCLOUDSIMSTORAGE, input(hostQuantity));
    }

    /** Production-scale seed: 512 hosts -> a real grid carrying all 512 hosts. */
    @Test
    public void cloudSimStorageTransformPreservesHostsAndSpecs() {
        MutableCloud cloud = transform(512);

        assertNotNull("CloudSim-Storage transform must produce a cloud", cloud);
        assertEquals(512, cloud.getNumHosts());
        assertEquals(1000L, cloud.getMips());
        assertEquals(1000, cloud.getIORate());
        assertEquals(1000L, cloud.getNetPerformance());
        assertEquals(1L, cloud.getNetLatency());
    }

    /**
     * Regression guard for the grid fix: a small host count (where the old
     * hard-coded 10x10 grid, and later the 1x1 flatten, were both problematic)
     * must still yield exactly that many hosts.
     */
    @Test
    public void cloudSimStorageTransformHandlesSmallHostCount() {
        MutableCloud cloud = transform(32);

        assertNotNull(cloud);
        assertEquals(32, cloud.getNumHosts());
    }

    /** A single host is the degenerate lower bound and must not collapse to zero. */
    @Test
    public void cloudSimStorageTransformHandlesSingleHost() {
        MutableCloud cloud = transform(1);

        assertNotNull(cloud);
        assertEquals(1, cloud.getNumHosts());
    }
}
