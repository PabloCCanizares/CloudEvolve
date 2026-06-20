package transformations;

import auxiliar.cloud.CloudCreator;
import dataParser.TestCaseInput;
import dataParser.cloud.ECloudSimulator;
import dataParser.cloud.input.TcInput_cloud;
import entities.cloud.Cpu;
import entities.cloud.Network;
import entities.cloud.Ram;
import entities.cloud.Storage;
import mutation.MutableCloud.MutableCloud;
import mutation.MutableCloud.MutableNode;

/**
 * {@link TestCaseTransformations} for the CloudSim-Storage backend.
 *
 * <p>Holds the CloudSim test-case &lt;-&gt; cloud-model transforms that used to
 * live inline in {@link TestCase2Cloud} ({@code transformCloudSim} /
 * {@code transformCloudSim2TcInput}), now a sibling of {@link TC2Cloud_simGrid}.</p>
 */
public class TC2Cloud_cloudSim implements TestCaseTransformations {

    private final CloudCreator cloudCreator;
    private final ECloudSimulator eCloudSimulator;

    public TC2Cloud_cloudSim() {
        this.eCloudSimulator = ECloudSimulator.eCLOUDSIMSTORAGE;
        this.cloudCreator = new CloudCreator(eCloudSimulator);
    }

    @Override
    public MutableCloud transformTestCase2Cloud(TestCaseInput tcInput) {

        MutableCloud mutableCloud;
        MutableNode mutableNode;
        Cpu hostCpu;
        Storage hostStorage;
        Ram hostRam;
        Network hostNetwork;
        TcInput_cloud tcInputCloud;
        int nNumNodes, nNumRacks, nNumBlades;

        mutableCloud = null;
        nNumNodes = 0;
        nNumRacks = nNumBlades = 1;

        tcInputCloud = (TcInput_cloud) tcInput;
        if (tcInputCloud != null) {
            mutableCloud = new MutableCloud(this.eCloudSimulator);
            nNumNodes = tcInputCloud.getHostQuantity();
            // Lay the flat host count onto a balanced, roughly-square rack/blade
            // grid. createHomogeneousCloud distributes the nodes across
            // racks*blades cells but collapses to zero hosts when racks*blades
            // exceeds the node count, so the grid side is capped at
            // floor(sqrt(hostQuantity)) (>= 1). This preserves a real
            // multi-rack/multi-blade topology at scale (512 -> 22x22, the
            // canonical 100 -> 10x10) instead of flattening everything onto a
            // single blade.
            nNumRacks = nNumBlades = Math.max(1, (int) Math.sqrt(nNumNodes));
            hostCpu = cloudCreator.createCPU(tcInputCloud.getHostMips(), tcInputCloud.getHostPes());
            hostStorage = cloudCreator.createIO(tcInputCloud.getHostIoCapacity(), tcInputCloud.getIoPerformance(), tcInputCloud.getMaxIoTransferRate(), tcInputCloud.getIOLatency());
            hostRam = cloudCreator.createRAM(tcInputCloud.getHostRam(), tcInputCloud.getRamBandwidth());
            hostNetwork = cloudCreator.createNetwork(tcInputCloud.getNetId(), tcInputCloud.getNetBandwidth(), tcInputCloud.getNetLatency());
            mutableNode = cloudCreator.createMutableNode(hostCpu, hostStorage, hostRam, hostNetwork);
            mutableCloud = cloudCreator.createHomogeneousCloud(nNumRacks, nNumBlades, nNumNodes, mutableNode, hostNetwork);
        }

        return mutableCloud;
    }

    @Override
    public TestCaseInput transformCloud2Testcase(ECloudSimulator ePlatformInfo, TestCaseInput tcInputNew, MutableCloud mutCloud) {

        TcInput_cloud tcInput;

        tcInput = null;
        if (mutCloud != null && tcInputNew != null) {
            // The main idea is to clone the tcinput, and modify only the parts that the algorithm needs
            tcInput = (TcInput_cloud) tcInputNew;

            // Quantity
            tcInput.setHostQuantity(mutCloud.getNumHosts());

            // CPU
            tcInput.setHostMips((int) mutCloud.getMips());

            // RAM: N.A.

            // RAMSPEED: N.A.

            // STO
            tcInput.setIOMaxRate((int) mutCloud.getIORate());

            // NetBW
            tcInput.setNetBandwidth((int) mutCloud.getNetPerformance());

            // LATENCY
            tcInput.setNetLatency((int) mutCloud.getNetLatency());
        }

        return tcInput;
    }
}
