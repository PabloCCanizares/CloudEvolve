package transformations;

import dataParser.TestCaseInput;
import dataParser.cloud.ECloudSimulator;
import dataParser.cloud.input.TcInput_cloud;
import mutation.MutableCloud.MutableCloud;
import platform.SimulatorPlatforms;

/**
 * Thin facade over the per-simulator {@link TestCaseTransformations}: it resolves
 * the strategy for the requested simulator (via {@link SimulatorPlatforms}) and
 * delegates. The actual transforms live in {@link TC2Cloud_cloudSim} and
 * {@link TC2Cloud_simGrid}.
 */
public class TestCase2Cloud {

    public TestCase2Cloud(ECloudSimulator eCloudSimulator) {
        // The simulator is resolved per call from the explicit ePlatformInfo
        // argument below; this constructor is kept for source compatibility.
    }

    public MutableCloud transformTestcase2Cloud(ECloudSimulator ePlatformInfo, TestCaseInput tcInput) {
        if (tcInput == null) {
            return null;
        }
        return SimulatorPlatforms.of(ePlatformInfo).transformations().transformTestCase2Cloud(tcInput);
    }

    public TestCaseInput transformCloud2Testcase(ECloudSimulator ePlatformInfo, TcInput_cloud tcInputNew, MutableCloud mutCloud) {
        if (mutCloud == null) {
            return null;
        }
        return SimulatorPlatforms.of(ePlatformInfo).transformations().transformCloud2Testcase(ePlatformInfo, tcInputNew, mutCloud);
    }
}
