package platform.surrogate;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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

    private final LightGbmModel energy;
    private final LightGbmModel time;
    private final String[] featureNames;          // shared layout (energy model's)
    private final List<List<String>> categories;  // shared pandas_categorical

    private SurrogateModel(LightGbmModel energy, LightGbmModel time) {
        this.energy = energy;
        this.time = time;
        this.featureNames = energy.featureNames();
        this.categories = energy.pandasCategorical();
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
        return new SurrogateModel(LightGbmModel.load(e.getPath()), LightGbmModel.load(t.getPath()));
    }

    /** Predicted {@code {energy_kwh, sim_time_sec}} for a parsed {@code .tc}. */
    public double[] predict(Map<String, String> testCase) {
        double[] x = features(testCase);
        return new double[] { energy.predict(x), time.predict(x) };
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
