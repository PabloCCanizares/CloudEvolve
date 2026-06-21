package platform.surrogate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Golden-master test for the pure-Java surrogate evaluator.
 *
 * <p>It pins the Java {@link SurrogateModel} (which reads the LightGBM models in
 * their native text format) against predictions produced by the original Python
 * LightGBM models: {@code golden_predictions.csv} holds 400 real cloud
 * configurations sampled from the training set together with the
 * {@code (energy, time)} the {@code .joblib} models return. The Java reader must
 * reproduce them to floating-point tolerance — this is the contract that lets the
 * surrogate stand in for the simulator.</p>
 *
 * <p>It is skipped (not failed) when the bundled models or the golden file are
 * absent, so a checkout without the large model files still builds and tests.</p>
 */
public class SurrogateModelGoldenTest {

    private static final String MODEL_DIR = "lib/surrogate";
    private static final String GOLDEN = "src/platform/surrogate/golden_predictions.csv";

    // Java vs LightGBM evaluate the identical trees in the same order, so the
    // agreement is essentially exact; these deltas only absorb float noise.
    private static final double ENERGY_TOL = 1e-4;
    private static final double TIME_TOL = 1e-2;

    private static SurrogateModel model;

    @BeforeClass
    public static void loadModel() throws Exception {
        Assume.assumeTrue("surrogate models not bundled at " + MODEL_DIR,
                new File(MODEL_DIR, SurrogateModel.ENERGY_FILE).isFile()
                        && new File(MODEL_DIR, SurrogateModel.TIME_FILE).isFile());
        // Pin the raw model fidelity, independent of the runtime plausibility guard.
        System.setProperty(SurrogateModel.GUARD_PROPERTY, "none");
        model = SurrogateModel.load(MODEL_DIR);
    }

    @AfterClass
    public static void clearGuard() {
        System.clearProperty(SurrogateModel.GUARD_PROPERTY);
    }

    @Test
    public void javaReaderReproducesPythonPredictions() throws Exception {
        File golden = new File(GOLDEN);
        Assume.assumeTrue("golden fixture missing: " + GOLDEN, golden.isFile());

        double maxEnergyDiff = 0.0;
        double maxTimeDiff = 0.0;
        int rows = 0;

        try (BufferedReader r = new BufferedReader(new FileReader(golden))) {
            String[] header = r.readLine().split(",");
            int energyCol = indexOf(header, "expected_energy");
            int timeCol = indexOf(header, "expected_time");
            assertTrue("golden header missing expected columns", energyCol >= 0 && timeCol >= 0);

            String line;
            while ((line = r.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] cells = line.split(",");
                Map<String, String> tc = new LinkedHashMap<>();
                for (int i = 0; i < header.length; i++) {
                    if (i != energyCol && i != timeCol) {
                        tc.put(header[i], cells[i]);
                    }
                }
                double expEnergy = Double.parseDouble(cells[energyCol]);
                double expTime = Double.parseDouble(cells[timeCol]);

                double[] pred = model.predict(tc);
                assertEquals("energy mismatch on row " + rows, expEnergy, pred[0], ENERGY_TOL);
                assertEquals("time mismatch on row " + rows, expTime, pred[1], TIME_TOL);

                maxEnergyDiff = Math.max(maxEnergyDiff, Math.abs(expEnergy - pred[0]));
                maxTimeDiff = Math.max(maxTimeDiff, Math.abs(expTime - pred[1]));
                rows++;
            }
        }

        assertTrue("golden file had no data rows", rows > 0);
        System.out.printf("[surrogate golden] %d rows; max|Δenergy|=%.2e kWh, max|Δtime|=%.2e s%n",
                rows, maxEnergyDiff, maxTimeDiff);
    }

    private static int indexOf(String[] arr, String key) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].trim().equals(key)) {
                return i;
            }
        }
        return -1;
    }
}
