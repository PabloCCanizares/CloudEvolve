package platform.surrogate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Thread-safe CSV appender that records real-simulator evaluations gathered by
 * the {@link platform.HybridPlatform} for re-training the surrogate.
 *
 * <p>Each row is the cloud configuration (the surrogate's feature columns, taken
 * verbatim from the {@code .tc}) plus the simulator's real {@code energy_kwh} and
 * {@code sim_time_sec}. This is the exact schema of the training set's relevant
 * columns, so the file can be concatenated to it directly. Because the hybrid
 * spends its real-simulator budget where the search converges (the region the
 * surrogate is weakest), this increment naturally accumulates the hard examples
 * — active learning by construction.</p>
 */
public final class SampleLogger {

    private final File csv;
    private final List<String> featureColumns;
    private volatile boolean headerEnsured;

    public SampleLogger(String path, List<String> featureColumns) {
        this.csv = new File(path);
        this.featureColumns = featureColumns;
    }

    /** Appends one real evaluation: the {@code .tc} features plus the real labels. */
    public synchronized void record(Map<String, String> features, double energyKwh, double simTimeSec) {
        try {
            ensureHeader();
            StringBuilder sb = new StringBuilder();
            for (String col : featureColumns) {
                String v = features.get(col);
                sb.append(v == null ? "" : v).append(',');
            }
            sb.append(String.format(Locale.US, "%.6f,%.6f%n", energyKwh, simTimeSec));
            try (BufferedWriter w = new BufferedWriter(new FileWriter(csv, true))) {
                w.write(sb.toString());
            }
        } catch (IOException e) {
            System.out.println("SampleLogger: could not append to " + csv + ": " + e.getMessage());
        }
    }

    private void ensureHeader() throws IOException {
        if (headerEnsured) {
            return;
        }
        File parent = csv.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        if (!csv.exists() || csv.length() == 0) {
            try (BufferedWriter w = new BufferedWriter(new FileWriter(csv, true))) {
                w.write(String.join(",", featureColumns) + ",energy_kwh,sim_time_sec\n");
            }
        }
        headerEnsured = true;
    }
}
