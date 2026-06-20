package platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Assume;
import org.junit.Test;

import dataParser.cloud.ECloudSimulator;
import dataParser.cloud.TestCaseParser_cloud;
import dataParser.cloud.output.TcOutput_cloud;
import dataParser.metadata.MetaTestCase;
import platform.surrogate.SurrogateModel;

/**
 * End-to-end test of the surrogate as a drop-in simulator: {@link SurrogatePlatform#execute}
 * reads a real {@code .tc}, predicts with the bundled models, and writes the
 * simulator's <b>native</b> output format. The test then parses that output with
 * the very same {@link TestCaseParser_cloud} the production code uses and checks
 * the figures survive the round-trip, including MT_Handler's post-processing
 * ({@code energy = raw * simTime / 3600}). This is what guarantees the surrogate
 * plugs into the existing fitness pipeline unchanged.
 *
 * <p>Skipped when the models or the smoke fixture are absent.</p>
 */
public class SurrogatePlatformExecuteTest {

    private static final String MODEL_DIR = "lib/surrogate";
    private static final String INPUT_TC = "repro/smoke/Al_w3/input.tc";

    @Test
    public void executeWritesNativeOutputThatTheParserReadsBack() throws Exception {
        Assume.assumeTrue("surrogate models not bundled at " + MODEL_DIR,
                new File(MODEL_DIR, SurrogateModel.ENERGY_FILE).isFile());
        File input = new File(INPUT_TC);
        Assume.assumeTrue("smoke input.tc missing: " + INPUT_TC, input.isFile());

        File out = File.createTempFile("surrogate_out", ".tc");
        out.deleteOnExit();

        MetaTestCase metaTC = new MetaTestCase();
        metaTC.setTcInput(input.getPath());
        metaTC.setTcOutput(out.getPath());

        boolean ok = SimulatorPlatforms.of(ECloudSimulator.eSURROGATE)
                .execute(new FakeExec(MODEL_DIR), metaTC);
        assertTrue("surrogate execute() should succeed", ok);

        // What the model predicts directly (raw energy_kwh, sim_time_sec)...
        SurrogateModel model = SurrogateModel.load(MODEL_DIR);
        double[] pred = model.predict(SurrogatePlatform.readTestCase(input.getPath()));
        double energy = pred[0];
        double time = pred[1];

        // ...must come back through the simulator's own output parser.
        TcOutput_cloud parsed = (TcOutput_cloud) new TestCaseParser_cloud(ECloudSimulator.eSURROGATE)
                .doParseOutput(out.getPath());
        assertNotNull("parser must read the surrogate output", parsed);

        assertEquals("simulation time round-trips", time, parsed.getSimTime(), 0.01);
        assertEquals("post-processed energy matches raw * time / 3600",
                energy * parsed.getSimTime() / 3600.0, parsed.getTotalEnergyCons(), 1e-3);
        assertTrue("fitness energy must be positive", parsed.getTotalEnergyCons() > 0.0);

        System.out.printf(
                "[surrogate drop-in] energy(raw)=%.5f kWh, time=%.2f s -> fitness energy=%.5f kWh%n",
                energy, time, parsed.getTotalEnergyCons());
    }

    /** Minimal {@link SimulatorExecution}: the surrogate only needs {@code simulatorPath()}. */
    private static final class FakeExec implements SimulatorExecution {
        private final String path;

        FakeExec(String path) {
            this.path = path;
        }

        @Override
        public boolean executeCommand(String command) {
            return false;
        }

        @Override
        public boolean executeCommandSimGrid(String command) {
            return false;
        }

        @Override
        public String timeoutHeader() {
            return "";
        }

        @Override
        public String simulatorPath() {
            return path;
        }
    }
}
