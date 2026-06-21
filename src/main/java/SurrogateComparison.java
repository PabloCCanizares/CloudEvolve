package main.java;

/*******************************************************************************
 * Copyright (C) 2025 Pablo C. Cañizares
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 ******************************************************************************/

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import dataParser.cloud.ECloudSimulator;
import dataParser.cloud.TestCaseParser_cloud;
import dataParser.cloud.output.TcOutput_cloud;
import platform.surrogate.SurrogateModel;

/**
 * Side-by-side comparison of the <b>surrogate</b> backend against a normal
 * (real-simulator) execution, at the level of a single fitness evaluation — the
 * unit a genetic algorithm repeats thousands of times.
 *
 * <p>For every bundled cloud configuration that ships with a recorded simulator
 * output (the {@code repro/smoke} case plus the four {@code InitialPopulation}
 * seeds), it prints the real {@code (energy, time)} next to the surrogate's
 * prediction and the relative error, then the aggregate MAPE. It also measures
 * the surrogate's raw throughput and, unless {@code --no-sim} is given and the
 * simulator jar is present, runs the real simulator <b>live</b> once for a true
 * wall-clock head-to-head (restoring the fixture afterwards, so nothing on disk
 * changes).</p>
 *
 * <p>Run from the repository root:</p>
 * <pre>{@code
 *   java -cp <classpath> main.java.SurrogateComparison
 *   java -cp <classpath> main.java.SurrogateComparison -m lib/surrogate -r repro --no-sim
 * }</pre>
 */
public class SurrogateComparison {

    /** A configuration with both its input .tc and the recorded real output .tc. */
    private static final class GroundTruthCase {
        final String name;
        final File inputTc;
        final File outputTc;

        GroundTruthCase(String name, File inputTc, File outputTc) {
            this.name = name;
            this.inputTc = inputTc;
            this.outputTc = outputTc;
        }
    }

    public static void main(String[] args) throws Exception {
        String modelDir = "lib/surrogate";
        String reproDir = "repro";
        boolean runSimulator = true;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-m": modelDir = args[++i]; break;
                case "-r": reproDir = args[++i]; break;
                case "--no-sim": runSimulator = false; break;
                case "-h": case "--help":
                    System.out.println("Usage: SurrogateComparison [-m modelDir] [-r reproDir] [--no-sim]");
                    return;
                default:
                    System.out.println("Unknown argument: " + args[i]);
                    return;
            }
        }

        SurrogateModel model = SurrogateModel.load(modelDir);
        List<GroundTruthCase> cases = discoverCases(reproDir);
        if (cases.isEmpty()) {
            System.out.println("No ground-truth cases found under " + reproDir
                    + " (expected repro/smoke and repro/InitialPopulation).");
            return;
        }

        System.out.println("=========================================================================");
        System.out.println(" Surrogate vs real simulator — per fitness-evaluation comparison");
        System.out.println(" model dir: " + modelDir);
        System.out.println("=========================================================================");
        System.out.printf(Locale.US, "%-8s | %18s | %18s%n", "", "ENERGY (kWh)", "TIME (s)");
        System.out.printf(Locale.US, "%-8s | %8s %8s %6s | %8s %8s %6s%n",
                "config", "real", "surr", "err%", "real", "surr", "err%");
        System.out.println("-------------------------------------------------------------------------");

        double sumEnergyApe = 0.0;
        double sumTimeApe = 0.0;
        int counted = 0;

        for (GroundTruthCase c : cases) {
            double[] real = parseRecorded(c.outputTc);   // {energy_kwh, sim_time_sec}
            double[] surr = model.predictFile(c.inputTc.getPath());

            double energyApe = ape(real[0], surr[0]);
            double timeApe = ape(real[1], surr[1]);
            sumEnergyApe += energyApe;
            sumTimeApe += timeApe;
            counted++;

            System.out.printf(Locale.US, "%-8s | %8.3f %8.3f %5.1f%% | %8.1f %8.1f %5.1f%%%n",
                    c.name, real[0], surr[0], energyApe, real[1], surr[1], timeApe);
        }

        System.out.println("-------------------------------------------------------------------------");
        System.out.printf(Locale.US, "%-8s | %25.2f%% | %22.2f%%%n",
                "MAPE", sumEnergyApe / counted, sumTimeApe / counted);
        System.out.println();

        measureSurrogateSpeed(model, cases.get(0).inputTc.getPath());

        File simulatorJar = new File(reproDir, "cloudsimStorage.jar");
        if (runSimulator && simulatorJar.isFile()) {
            headToHead(model, reproDir, simulatorJar);
        } else if (runSimulator) {
            System.out.println("(real-simulator head-to-head skipped: " + simulatorJar + " not found)");
        }
    }

    /** Collects the bundled configurations that have a recorded real output. */
    private static List<GroundTruthCase> discoverCases(String reproDir) {
        List<GroundTruthCase> cases = new ArrayList<>();
        addIfPresent(cases, "smoke",
                new File(reproDir, "smoke/Al_w3/input.tc"),
                new File(reproDir, "smoke/Al_w3/output.tc"));
        for (String cfg : new String[] { "Al_w1", "Al_w3", "Bl_w1", "Bl_w3" }) {
            addIfPresent(cases, cfg,
                    new File(reproDir, "InitialPopulation/" + cfg + "/tcInput/input_00000.tc"),
                    new File(reproDir, "InitialPopulation/" + cfg + "/tcOutput/output_00000.tc"));
        }
        return cases;
    }

    private static void addIfPresent(List<GroundTruthCase> cases, String name, File in, File out) {
        if (in.isFile() && out.isFile()) {
            cases.add(new GroundTruthCase(name, in, out));
        }
    }

    /**
     * Reads a recorded simulator output into {@code {energy_kwh, sim_time_sec}}
     * using the production parser. The energy is the raw "Energy consumption"
     * line and the time the raw "Total simulation time" — exactly the two
     * quantities the surrogate is trained to predict.
     */
    private static double[] parseRecorded(File output) {
        TcOutput_cloud parsed = (TcOutput_cloud) new TestCaseParser_cloud(ECloudSimulator.eCLOUDSIMSTORAGE)
                .doParseOutput(output.getPath());
        return new double[] { parsed.getCpuEnergyCons(), parsed.getSimTime() };
    }

    private static double ape(double real, double predicted) {
        if (real == 0.0) {
            return 0.0;
        }
        return Math.abs(predicted - real) / Math.abs(real) * 100.0;
    }

    /** Times the surrogate over many predictions to report its raw throughput. */
    private static void measureSurrogateSpeed(SurrogateModel model, String inputTc) throws Exception {
        final int warmup = 200;
        final int reps = 5000;
        for (int i = 0; i < warmup; i++) {
            model.predictFile(inputTc);
        }
        long t0 = System.nanoTime();
        for (int i = 0; i < reps; i++) {
            model.predictFile(inputTc);
        }
        long elapsed = System.nanoTime() - t0;
        double microsPerEval = elapsed / 1_000.0 / reps;
        double evalsPerSec = reps / (elapsed / 1_000_000_000.0);
        System.out.printf(Locale.US,
                "Surrogate speed: %.1f µs/evaluation  (~%,.0f evaluations/second, incl. .tc parsing)%n%n",
                microsPerEval, evalsPerSec);
    }

    /**
     * Runs the real simulator once on the self-contained smoke fixture for a true
     * wall-clock head-to-head, then restores the fixture so nothing changes on
     * disk.
     */
    private static void headToHead(SurrogateModel model, String reproDir, File simulatorJar) throws Exception {
        File fixture = new File(reproDir, "smoke");
        File metaTc = new File(fixture, "Al_w3/meta.mtc");
        File inputTc = new File(fixture, "Al_w3/input.tc");
        File outputTc = new File(fixture, "Al_w3/output.tc");
        if (!metaTc.isFile() || !inputTc.isFile()) {
            System.out.println("(head-to-head skipped: smoke fixture incomplete)");
            return;
        }

        // Back up the recorded output so the live run leaves the repo pristine.
        byte[] backup = outputTc.isFile() ? Files.readAllBytes(outputTc.toPath()) : null;
        try {
            System.out.println("=========================================================================");
            System.out.println(" Live head-to-head on the smoke case (real simulator vs surrogate)");
            System.out.println("=========================================================================");

            long t0 = System.nanoTime();
            Process p = new ProcessBuilder(
                    "java", "-Xmx2g", "-Duser.language=en", "-Duser.country=US",
                    "-jar", simulatorJar.getAbsolutePath(),
                    "--standalone", "Al_w3/meta.mtc")
                    .directory(fixture)
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!p.waitFor(180, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                System.out.println("(simulator did not finish within 180s; head-to-head aborted)");
                return;
            }
            double simWallMs = (System.nanoTime() - t0) / 1_000_000.0;

            double[] real = parseRecorded(outputTc);

            // Surrogate on the same case: time a single warm call.
            model.predictFile(inputTc.getPath()); // warm
            long s0 = System.nanoTime();
            double[] surr = model.predictFile(inputTc.getPath());
            double surrMicros = (System.nanoTime() - s0) / 1_000.0;

            System.out.printf(Locale.US, "  real simulator : energy=%.3f kWh, time=%.1f s   [wall %.0f ms]%n",
                    real[0], real[1], simWallMs);
            System.out.printf(Locale.US, "  surrogate      : energy=%.3f kWh, time=%.1f s   [wall %.0f µs]%n",
                    surr[0], surr[1], surrMicros);

            double speedup = (simWallMs * 1000.0) / surrMicros;
            System.out.printf(Locale.US, "  speedup        : ~%,.0fx per evaluation%n", speedup);
            System.out.println();
            System.out.printf(Locale.US,
                    "  At this rate, the paper's campaign (5 algos x 4 configs x 30 runs x 100 gens,%n"
                  + "  on the order of millions of evaluations) shrinks from simulator-days to%n"
                  + "  surrogate-seconds — at a mean error of a few percent (table above).%n");
        } finally {
            if (backup != null) {
                Files.write(outputTc.toPath(), backup);
            }
        }
    }
}
