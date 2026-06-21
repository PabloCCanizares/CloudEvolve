package platform;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import configuration.EACrossoverOperator;
import configuration.EAController;
import configuration.EAMutationOperator;
import dataParser.cloud.ECloudSimulator;
import dataParser.cloud.TestCaseParser_cloud;
import dataParser.cloud.output.TcOutput_cloud;
import dataParser.metadata.MetaTestCase;
import platform.surrogate.SampleLogger;
import platform.surrogate.SurrogateModel;
import transformations.TestCaseTransformations;

/**
 * {@link SimulatorPlatform} that combines the fast surrogate with a real
 * simulator backend. Most fitness evaluations use the surrogate; every
 * {@code N} generations they are routed to the real simulator instead, so the
 * accurate values flow back into NSGA-II's selection naturally (self-correction)
 * and are logged for re-training.
 *
 * <p>It is a <b>compositor</b>: it owns a {@link SurrogatePlatform} and an
 * <i>injectable</i> real backend ({@code cloudevolve.hybrid.real}, default
 * CloudSim-Storage — but any backend with a strategy works), and delegates the
 * cloud topology, operators and transforms to them (identical on both). Only the
 * per-evaluation routing in {@link #execute} is its own.</p>
 *
 * <p>The Pareto front is only known after a whole generation is evaluated, so
 * "real every N generations" is realised at the evaluation seam via the current
 * generation index ({@link EAController#getIteration()}): on generations that are
 * a multiple of N the offspring are evaluated for real, and being part of the
 * population they propagate into the front through selection.</p>
 *
 * <p>Configuration (system properties):</p>
 * <ul>
 *   <li>{@code cloudevolve.hybrid.real} — real backend enum (default {@code eCLOUDSIMSTORAGE}).</li>
 *   <li>{@code cloudevolve.hybrid.realEvery} — N; real simulator every N generations (default 5; 0 disables).</li>
 *   <li>{@code cloudevolve.hybrid.increment} — re-training CSV (default {@code <workspace>/surrogate_increment.csv}).</li>
 *   <li>{@code cloudevolve.surrogate.dir} — surrogate model directory (as for {@link SurrogatePlatform}).</li>
 * </ul>
 */
public final class HybridPlatform implements SimulatorPlatform {

    public static final String REAL_BACKEND_PROPERTY = "cloudevolve.hybrid.real";
    public static final String REAL_EVERY_PROPERTY = "cloudevolve.hybrid.realEvery";
    public static final String INCREMENT_PROPERTY = "cloudevolve.hybrid.increment";

    /** Surrogate feature columns, in training order (see surrogate_feature_spec.json). */
    public static final List<String> FEATURE_COLUMNS = Arrays.asList(
            "vm.quantity", "vm.mips", "vm.pes", "vm.ram", "vm.bw", "vm.size", "vm.priority",
            "vm.schedulingInterval", "host.quantity", "host.ram", "host.ramspeed", "host.bw",
            "host.sto", "host.mips", "host.pes", "sto.capacity", "sto.maxTransferRate", "sto.latency",
            "work.volume", "work.numTraces", "net.bandwidth", "net.latency", "net.switches",
            "sto.type", "work.name");

    private final SimulatorPlatform surrogate = new SurrogatePlatform();
    private final SimulatorPlatform real = SimulatorPlatforms.of(realBackend());
    private final int realEvery = intProperty(REAL_EVERY_PROPERTY, 5);
    private final SampleLogger logger = new SampleLogger(incrementPath(), FEATURE_COLUMNS);

    @Override
    public String evolutionaryBasePath() {
        return PlatformPaths.evolutionaryBase("hybrid");
    }

    @Override
    public void registerMutationOperators(List<EAMutationOperator> operators, LinkedList<Double> probabilities) {
        real.registerMutationOperators(operators, probabilities);
    }

    @Override
    public void registerCrossoverOperators(List<EACrossoverOperator> operators) {
        real.registerCrossoverOperators(operators);
    }

    @Override
    public TestCaseTransformations transformations() {
        return real.transformations();
    }

    @Override
    public boolean execute(SimulatorExecution exec, MetaTestCase metaTC) {
        if (useRealSimulator()) {
            boolean ok = real.execute(exec, metaTC);
            if (ok) {
                logSample(metaTC);
            }
            return ok;
        }
        return surrogate.execute(exec, metaTC);
    }

    /** True on generations that are a (non-zero) multiple of N. */
    private boolean useRealSimulator() {
        if (realEvery <= 0) {
            return false;
        }
        int generation = EAController.getInstance().getIteration();
        return generation % realEvery == 0;
    }

    /** Logs the just-produced real evaluation (features + real labels) for re-training. */
    private void logSample(MetaTestCase metaTC) {
        try {
            Map<String, String> features =
                    SurrogateModel.readTestCase(PlatformPaths.resolveWorkspacePath(metaTC.getTcInput()));
            String outPath = PlatformPaths.resolveWorkspacePath(metaTC.getTcOutput());
            TcOutput_cloud out = (TcOutput_cloud) new TestCaseParser_cloud(ECloudSimulator.eCLOUDSIMSTORAGE)
                    .doParseOutput(outPath);
            if (out == null) {
                return;
            }
            // energy_kwh = the raw "Energy consumption" line, the surrogate's training target.
            logger.record(features, out.getCpuEnergyCons(), out.getSimTime());
        } catch (Exception e) {
            System.out.println("HybridPlatform: could not log sample: " + e.getMessage());
        }
    }

    private static ECloudSimulator realBackend() {
        String v = System.getProperty(REAL_BACKEND_PROPERTY, "");
        if (v.toLowerCase().contains("simgrid")) {
            return ECloudSimulator.eSIMGRID;
        }
        return ECloudSimulator.eCLOUDSIMSTORAGE;
    }

    private static int intProperty(String key, int dflt) {
        try {
            String v = System.getProperty(key);
            return (v == null || v.isEmpty()) ? dflt : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return dflt;
        }
    }

    private static String incrementPath() {
        String v = System.getProperty(INCREMENT_PROPERTY);
        if (v != null && !v.isEmpty()) {
            return v;
        }
        return PlatformPaths.workspace() + "/surrogate_increment.csv";
    }
}
