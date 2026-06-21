package platform.surrogate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The cloud surrogate: two LightGBM regressors (energy and time) sharing one
 * feature layout. Maps a parsed {@code .tc} cloud configuration onto the model's
 * feature vector and returns the predicted {@code (energy_kwh, sim_time_sec)}.
 *
 * <p>The feature order and the categorical category lists are read straight from
 * the model file (its {@code feature_names} header and {@code pandas_categorical}
 * trailer), so the contract stays in sync with whatever was trained — no
 * separate spec file is needed at runtime. The two {@code .tc} field names map
 * 1:1 onto the feature names, which is exactly how the training set was built.</p>
 */
public final class SurrogateModel {

    public static final String ENERGY_FILE = "surrogate_energy_lgbm.txt";
    public static final String TIME_FILE = "surrogate_time_lgbm.txt";
    public static final String SPEC_FILE = "surrogate_feature_spec.json";

    /** Plausibility guard on predictions; see {@link #applyGuard}. */
    public static final String GUARD_PROPERTY = "cloudevolve.surrogate.guard";

    private final LightGbmModel energy;
    private final LightGbmModel time;
    private final String[] featureNames;          // shared layout (energy model's)
    private final List<List<String>> categories;  // shared pandas_categorical
    private final double[] energyBounds;          // {min,max} from the spec, or null
    private final double[] timeBounds;
    private final String guard;

    private SurrogateModel(LightGbmModel energy, LightGbmModel time, double[][] bounds) {
        this.energy = energy;
        this.time = time;
        this.featureNames = energy.featureNames();
        this.categories = energy.pandasCategorical();
        this.energyBounds = bounds != null ? bounds[0] : null;
        this.timeBounds = bounds != null ? bounds[1] : null;
        this.guard = System.getProperty(GUARD_PROPERTY, "nonneg").toLowerCase();
    }

    /**
     * Loads both models from {@code dir} (or, if a model file path is given, from
     * its containing folder).
     */
    public static SurrogateModel load(String dir) throws IOException {
        File d = new File(dir);
        if (d.isFile()) {
            d = d.getParentFile();
        }
        File e = new File(d, ENERGY_FILE);
        File t = new File(d, TIME_FILE);
        if (!e.isFile() || !t.isFile()) {
            throw new IOException("Surrogate models not found in '" + d + "' (expected "
                    + ENERGY_FILE + " and " + TIME_FILE + ").");
        }
        return new SurrogateModel(LightGbmModel.load(e.getPath()), LightGbmModel.load(t.getPath()),
                readTargetBounds(new File(d, SPEC_FILE)));
    }

    /**
     * Reads {@code target_bounds} ([energyMin,energyMax],[timeMin,timeMax]) from the
     * feature spec, in {@code targets} order. Returns null if the spec is absent or
     * has no bounds (then the {@code clamp} guard degrades to {@code nonneg}).
     */
    private static double[][] readTargetBounds(File spec) {
        if (!spec.isFile()) {
            return null;
        }
        try {
            String s = new String(Files.readAllBytes(spec.toPath()), StandardCharsets.UTF_8);
            Matcher tm = Pattern.compile("\"targets\"\\s*:\\s*\\[\\s*\"([^\"]+)\"\\s*,\\s*\"([^\"]+)\"").matcher(s);
            if (!tm.find()) {
                return null;
            }
            String[] names = { tm.group(1), tm.group(2) };
            String num = "(-?[0-9.eE+]+)";
            double[][] bounds = new double[2][];
            for (int i = 0; i < 2; i++) {
                Matcher bm = Pattern.compile(Pattern.quote("\"" + names[i] + "\"")
                        + "\\s*:\\s*\\[\\s*" + num + "\\s*,\\s*" + num + "\\s*\\]").matcher(s);
                if (!bm.find()) {
                    return null;
                }
                bounds[i] = new double[] { Double.parseDouble(bm.group(1)), Double.parseDouble(bm.group(2)) };
            }
            return bounds;
        } catch (Exception e) {
            return null;
        }
    }

    /** Parses a {@code key = value} cloud test case into a {@code name -> value} map. */
    public static Map<String, String> readTestCase(String path) throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = r.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.charAt(0) == '#') {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq > 0) {
                    map.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
            }
        }
        return map;
    }

    /** Predicted {@code {energy_kwh, sim_time_sec}} for a {@code .tc} file. */
    public double[] predictFile(String tcPath) throws IOException {
        return predict(readTestCase(tcPath));
    }

    /** Predicted {@code {energy_kwh, sim_time_sec}} for a parsed {@code .tc}. */
    public double[] predict(Map<String, String> testCase) {
        double[] x = features(testCase);
        return applyGuard(new double[] { energy.predict(x), time.predict(x) });
    }

    /**
     * Plausibility guard (selected by {@code -Dcloudevolve.surrogate.guard}):
     * <ul>
     *   <li>{@code none} — raw model output;</li>
     *   <li>{@code nonneg} (default) — clip to &ge; 0 (no-op on valid predictions,
     *       but kills impossible negatives);</li>
     *   <li>{@code clamp} — clamp to the training target range from the spec
     *       (degrades to {@code nonneg} if no bounds), so the search cannot exploit
     *       out-of-range extrapolation artifacts.</li>
     * </ul>
     * It is a safety net against the worst phantoms, not a substitute for auditing
     * the final front with the real simulator.
     */
    double[] applyGuard(double[] energyTime) {
        return guard(this.guard, energyTime, energyBounds, timeBounds);
    }

    /** Pure guard logic (see {@link #applyGuard}); static so it is trivially testable. */
    static double[] guard(String mode, double[] energyTime, double[] energyBounds, double[] timeBounds) {
        if ("none".equals(mode)) {
            return energyTime;
        }
        if ("clamp".equals(mode) && energyBounds != null && timeBounds != null) {
            return new double[] { clamp(energyTime[0], energyBounds), clamp(energyTime[1], timeBounds) };
        }
        return new double[] { Math.max(0.0, energyTime[0]), Math.max(0.0, energyTime[1]) };
    }

    private static double clamp(double v, double[] lohi) {
        return Math.max(lohi[0], Math.min(lohi[1], v));
    }

    /** Builds the model feature vector from a {@code feature-name -> raw value} map. */
    double[] features(Map<String, String> testCase) {
        double[] x = new double[featureNames.length];
        for (int i = 0; i < featureNames.length; i++) {
            x[i] = encode(testCase.get(featureNames[i]));
        }
        return x;
    }

    /**
     * Numeric values pass through; a nominal value is mapped to its pandas
     * category code (0 = {@code __UNK__}), matching how LightGBM encodes
     * categories. Missing/unseen values become 0, exactly as in training
     * ({@code fillna(0)} for numerics, {@code __UNK__} for categories).
     */
    private double encode(String raw) {
        if (raw == null) {
            return 0.0;
        }
        raw = raw.trim();
        if (raw.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException nfe) {
            for (List<String> cats : categories) {
                int idx = cats.indexOf(raw);
                if (idx >= 0) {
                    return idx;
                }
            }
            return 0.0; // unseen category -> __UNK__ (never split on in the shipped models)
        }
    }
}
