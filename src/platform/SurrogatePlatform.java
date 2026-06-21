package platform;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import configuration.EACrossoverOperator;
import configuration.EAMutationOperator;
import dataParser.metadata.MetaTestCase;
import platform.surrogate.SurrogateModel;
import transformations.TestCaseTransformations;

/**
 * {@link SimulatorPlatform} that replaces the external simulator with a trained
 * surrogate ML model. Instead of launching CloudSim-Storage, it reads the test
 * case's cloud configuration, predicts {@code (energy, time)} with two LightGBM
 * models, and writes the simulator's native output format — so MT_Handler's
 * output parser and the downstream fitness are unchanged. It is a drop-in
 * replacement that trades simulator accuracy for orders-of-magnitude speed.
 *
 * <p>The cloud topology, mutation/crossover operators and test-case transforms
 * are identical to {@link CloudSimStoragePlatform}, so those are delegated; only
 * {@link #execute} differs.</p>
 *
 * <p>The model directory is taken from the configured "simulator path" (the last
 * launcher argument), reusing the existing plumbing; it must contain
 * {@code surrogate_energy_lgbm.txt} and {@code surrogate_time_lgbm.txt}. As a
 * fallback the {@code cloudevolve.surrogate.dir} system property is honoured.</p>
 */
public final class SurrogatePlatform implements SimulatorPlatform {

    /** System property pointing at the surrogate model directory (fallback). */
    public static final String SURROGATE_DIR_PROPERTY = "cloudevolve.surrogate.dir";

    private final SimulatorPlatform delegate = new CloudSimStoragePlatform();
    private final Map<String, SurrogateModel> cache = new ConcurrentHashMap<>();

    @Override
    public String evolutionaryBasePath() {
        return PlatformPaths.evolutionaryBase("surrogate");
    }

    @Override
    public void registerMutationOperators(List<EAMutationOperator> operators, LinkedList<Double> probabilities) {
        delegate.registerMutationOperators(operators, probabilities);
    }

    @Override
    public void registerCrossoverOperators(List<EACrossoverOperator> operators) {
        delegate.registerCrossoverOperators(operators);
    }

    @Override
    public TestCaseTransformations transformations() {
        return delegate.transformations();
    }

    @Override
    public boolean execute(SimulatorExecution exec, MetaTestCase metaTC) {
        try {
            Map<String, String> testCase =
                    SurrogateModel.readTestCase(PlatformPaths.resolveWorkspacePath(metaTC.getTcInput()));
            SurrogateModel model = modelFor(exec.simulatorPath());
            double[] prediction = model.predict(testCase);     // {energy_kwh, sim_time_sec}
            writeOutput(PlatformPaths.resolveWorkspacePath(metaTC.getTcOutput()),
                    testCase, prediction[0], prediction[1]);
            return true;
        } catch (Exception ex) {
            System.out.println("Surrogate execution failed: " + ex.getMessage());
            return false;
        }
    }

    private SurrogateModel modelFor(String simulatorPath) throws IOException {
        String dir = simulatorPath;
        if (dir == null || dir.isEmpty() || !hasModels(dir)) {
            String prop = System.getProperty(SURROGATE_DIR_PROPERTY);
            if (prop != null && !prop.isEmpty()) {
                dir = prop;
            }
        }
        if (dir == null || dir.isEmpty()) {
            throw new IOException("No surrogate model directory configured (pass it as the simulator "
                    + "path argument or set -D" + SURROGATE_DIR_PROPERTY + ").");
        }
        SurrogateModel model = cache.get(dir);
        if (model == null) {
            model = SurrogateModel.load(dir);
            cache.put(dir, model);
        }
        return model;
    }

    private static boolean hasModels(String dir) {
        File d = new File(dir);
        if (d.isFile()) {
            d = d.getParentFile();
        }
        return d != null
                && new File(d, SurrogateModel.ENERGY_FILE).isFile()
                && new File(d, SurrogateModel.TIME_FILE).isFile();
    }

    /**
     * Writes the simulator's native output format with the predicted raw values.
     * MT_Handler's parser then post-processes {@code energy = raw * simTime/3600}
     * exactly as it does for the real backend, keeping the surrogate a drop-in.
     */
    private static void writeOutput(String path, Map<String, String> testCase,
            double energyKwh, double simTimeSec) throws IOException {
        long hosts = asLong(testCase.get("host.quantity"));
        long vms = asLong(testCase.get("vm.quantity"));
        StringBuilder sb = new StringBuilder();
        sb.append("Experiment name: surrogate\n");
        sb.append("Number of hosts: ").append(hosts).append("\n");
        sb.append("Number of VMs: ").append(vms).append("\n");
        sb.append(String.format(Locale.US, "Total simulation time: %.2f sec\n", simTimeSec));
        sb.append(String.format(Locale.US, "Energy consumption: %.5f kWh\n", energyKwh));
        sb.append("Storage Energy consumption: 0.00000 kWh\n");
        sb.append(String.format(Locale.US, "total Energy consumption (CPU+storage): %.5f kWh\n", energyKwh));
        try (BufferedWriter w = new BufferedWriter(new FileWriter(path))) {
            w.write(sb.toString());
        }
    }

    private static long asLong(String s) {
        if (s == null) {
            return 0L;
        }
        try {
            return (long) Double.parseDouble(s.trim());
        } catch (NumberFormatException nfe) {
            return 0L;
        }
    }
}
