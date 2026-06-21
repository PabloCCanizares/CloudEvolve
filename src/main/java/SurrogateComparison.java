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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final String DECIMAL = "([0-9]+(?:[\\.,][0-9]+)?)";
    private static final Pattern P_ENERGY = Pattern.compile(
            "^total\\s+energy\\s+consumption\\s*\\(CPU\\+storage\\):\\s*" + DECIMAL + "\\s*kWh\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern P_TIME = Pattern.compile(
            "^total\\s+simulation\\s+time:\\s*" + DECIMAL + "\\s*sec\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern P_ITERLINE = Pattern.compile("^\\s*(\\d+)\\s*-\\s*\\[([^\\]]+)\\]\\s*$");
    private static final Pattern P_OUTPUT_NAME = Pattern.compile("^output_(\\d+)\\.tc$");

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
        boolean evolve = false;
        String algo = "eNSGAII";
        String config = "Al_w1";   // the lightest case -> fastest real-simulator run
        int iterations = 6;
        String outDir = "repro/out_compare";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-m": modelDir = args[++i]; break;
                case "-r": reproDir = args[++i]; break;
                case "--no-sim": runSimulator = false; break;
                case "--evolve": evolve = true; break;
                case "-a": algo = args[++i]; break;
                case "-n": config = args[++i]; break;
                case "-i": iterations = Integer.parseInt(args[++i]); break;
                case "-o": outDir = args[++i]; break;
                case "-h": case "--help":
                    System.out.println("Usage: SurrogateComparison [-m modelDir] [-r reproDir] [--no-sim]");
                    System.out.println("       [--evolve [-a ALGO] [-n CONFIG] [-i ITERS] [-o OUT_DIR]]");
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
        if (evolve) {
            // The end-to-end evolution already runs the real simulator extensively,
            // so the single-case head-to-head is redundant here.
            runEvolutionComparison(reproDir, modelDir, algo, config, iterations, outDir);
        } else if (runSimulator && simulatorJar.isFile()) {
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

    /* ===================== End-to-end evolution comparison ===================== */

    /**
     * Runs the same short evolution twice — once with the real simulator, once
     * with the surrogate — extracts the best-so-far energy and time per
     * generation, and renders two comparison graphs.
     */
    private static void runEvolutionComparison(String reproDir, String modelDir, String algo,
            String config, int iterations, String outDir) throws Exception {
        System.out.println("=========================================================================");
        System.out.println(" End-to-end evolution: real simulator vs surrogate");
        System.out.printf(Locale.US, " %s on %s, %d generations%n", algo, config, iterations);
        System.out.println("=========================================================================");

        File out = new File(outDir);
        File realOut = new File(out, "real");
        File surrOut = new File(out, "surrogate");
        realOut.mkdirs();
        surrOut.mkdirs();

        System.out.println(" Running REAL-simulator evolution (the slow one)...");
        long realMs = runLauncher(new String[] { "bash", reproDir + "/launcherSingleConf.sh",
                "-a", algo, "-n", config, "-i", String.valueOf(iterations), "-o", realOut.getAbsolutePath() },
                new File(out, "real.log"));

        System.out.println(" Running SURROGATE evolution...");
        // The launcher cd's into repro/, so the model dir must be absolute.
        String modelDirAbs = new File(modelDir).getAbsolutePath();
        long surrMs = runLauncher(new String[] { "bash", reproDir + "/launcherSurrogate.sh",
                "-a", algo, "-n", config, "-i", String.valueOf(iterations), "-o", surrOut.getAbsolutePath(),
                "-s", modelDirAbs }, new File(out, "surrogate.log"));

        File realRun = findRunDir(realOut);
        File surrRun = findRunDir(surrOut);
        if (realRun == null || surrRun == null) {
            System.out.println(" Could not locate run directories; see the .log files under " + out);
            return;
        }

        TreeMap<Integer, double[]> realTraj = extractTrajectory(realRun); // gen -> {energy, time}
        TreeMap<Integer, double[]> surrTraj = extractTrajectory(surrRun);

        File energyDat = new File(out, "energy_evolution.dat");
        File timeDat = new File(out, "time_evolution.dat");
        writeMerged(energyDat, realTraj, surrTraj, 0, "# generation  real_energy_kWh  surrogate_energy_kWh");
        writeMerged(timeDat, realTraj, surrTraj, 1, "# generation  real_time_s  surrogate_time_s");

        File energyGnu = writeGnuplot(out, "energy_evolution", energyDat.getName(),
                "Energy: real simulator vs surrogate", "Best energy so far (kWh)");
        File timeGnu = writeGnuplot(out, "time_evolution", timeDat.getName(),
                "Time: real simulator vs surrogate", "Best simulation time so far (s)");
        boolean plotted = runGnuplot(out, energyGnu) & runGnuplot(out, timeGnu);

        int lastGen = realTraj.isEmpty() ? -1 : realTraj.lastKey();
        System.out.println("-------------------------------------------------------------------------");
        if (lastGen >= 0 && surrTraj.containsKey(lastGen)) {
            double[] r = realTraj.get(lastGen);
            double[] s = surrTraj.get(lastGen);
            System.out.printf(Locale.US, " final best energy: real %.3f kWh | surrogate %.3f kWh (%.1f%% off)%n",
                    r[0], s[0], ape(r[0], s[0]));
            System.out.printf(Locale.US, " final best time  : real %.1f s   | surrogate %.1f s   (%.1f%% off)%n",
                    r[1], s[1], ape(r[1], s[1]));
        }
        System.out.printf(Locale.US, " wall time        : real %.1f s | surrogate %.1f s  (~%.0fx faster)%n",
                realMs / 1000.0, surrMs / 1000.0, surrMs > 0 ? (double) realMs / surrMs : 0.0);
        System.out.println(" data : " + energyDat.getPath() + ", " + timeDat.getPath());
        if (plotted) {
            System.out.println(" plots: " + new File(out, "energy_evolution.png").getPath()
                    + ", " + new File(out, "time_evolution.png").getPath());
        } else {
            System.out.println(" gnuplot unavailable — .dat and .gnu written; render with: "
                    + "(cd " + out + " && gnuplot energy_evolution.gnu time_evolution.gnu)");
        }
    }

    /** Runs a launcher subprocess, redirecting its (verbose) output to {@code log}; returns wall ms. */
    private static long runLauncher(String[] cmd, File log) throws Exception {
        long t0 = System.nanoTime();
        Process p = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .redirectOutput(log)
                .start();
        int code = p.waitFor();
        if (code != 0) {
            System.out.println("   WARNING: launcher exited with code " + code + " (see " + log + ")");
        }
        return (long) ((System.nanoTime() - t0) / 1_000_000.0);
    }

    /** Finds the run directory (the one holding iterationlist.txt) under a launcher output dir. */
    private static File findRunDir(File outDir) throws Exception {
        try (java.util.stream.Stream<Path> walk = Files.walk(outDir.toPath())) {
            return walk.filter(pp -> pp.getFileName().toString().equals("iterationlist.txt"))
                    .map(pp -> pp.getParent().toFile())
                    .findFirst().orElse(null);
        }
    }

    /**
     * Best-so-far (cumulative minimum) energy and time per generation. Energy is
     * the fitness energy (raw total × time/3600), matching what the GA optimises.
     */
    private static TreeMap<Integer, double[]> extractTrajectory(File runDir) throws Exception {
        Map<Integer, double[]> idToPoint = new HashMap<>();   // id -> {rawEnergy, time}
        File[] subs = runDir.listFiles(File::isDirectory);
        if (subs != null) {
            for (File sub : subs) {
                File tcOut = new File(sub, "TcOutput");
                File[] outs = tcOut.listFiles();
                if (outs == null) {
                    continue;
                }
                for (File f : outs) {
                    Matcher nm = P_OUTPUT_NAME.matcher(f.getName());
                    if (nm.matches()) {
                        double[] pt = parsePoint(f);
                        if (pt != null) {
                            idToPoint.put(Integer.parseInt(nm.group(1)), pt);
                        }
                    }
                }
            }
        }

        TreeMap<Integer, double[]> traj = new TreeMap<>();
        double cumEnergy = Double.POSITIVE_INFINITY;
        double cumTime = Double.POSITIVE_INFINITY;
        try (BufferedReader br = new BufferedReader(new FileReader(new File(runDir, "iterationlist.txt")))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = P_ITERLINE.matcher(line);
                if (!m.matches()) {
                    continue;
                }
                int gen = Integer.parseInt(m.group(1));
                for (String tok : m.group(2).trim().split("\\s+")) {
                    int id;
                    try {
                        id = Integer.parseInt(tok);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    double[] pt = idToPoint.get(id);
                    if (pt == null || pt[0] <= 0 || pt[1] <= 0) {
                        continue;
                    }
                    double fitnessEnergy = pt[0] * pt[1] / 3600.0;
                    cumEnergy = Math.min(cumEnergy, fitnessEnergy);
                    cumTime = Math.min(cumTime, pt[1]);
                }
                if (cumEnergy != Double.POSITIVE_INFINITY) {
                    traj.put(gen, new double[] { cumEnergy, cumTime });
                }
            }
        }
        return traj;
    }

    /** Parses {raw total energy, time} from a simulator/surrogate output .tc. */
    private static double[] parsePoint(File f) {
        Double energy = null;
        Double time = null;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String s = line.trim();
                Matcher me = P_ENERGY.matcher(s);
                if (me.matches()) {
                    energy = Double.parseDouble(me.group(1).replace(',', '.'));
                    continue;
                }
                Matcher mt = P_TIME.matcher(s);
                if (mt.matches()) {
                    time = Double.parseDouble(mt.group(1).replace(',', '.'));
                }
                if (energy != null && time != null) {
                    break;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return (energy != null && time != null) ? new double[] { energy, time } : null;
    }

    private static void writeMerged(File dat, TreeMap<Integer, double[]> real,
            TreeMap<Integer, double[]> surr, int objective, String header) throws Exception {
        try (BufferedWriter w = new BufferedWriter(new java.io.FileWriter(dat))) {
            w.write(header + "\n");
            for (Integer gen : real.keySet()) {
                if (!surr.containsKey(gen)) {
                    continue;
                }
                w.write(String.format(Locale.US, "%d  %.6f  %.6f%n",
                        gen, real.get(gen)[objective], surr.get(gen)[objective]));
            }
        }
    }

    private static File writeGnuplot(File out, String base, String datName, String title, String ylabel)
            throws Exception {
        File gnu = new File(out, base + ".gnu");
        try (BufferedWriter w = new BufferedWriter(new java.io.FileWriter(gnu))) {
            w.write("# Auto-generated by SurrogateComparison\n");
            w.write("set term pngcairo size 1100,700 enhanced font 'Arial,12'\n");
            w.write("set output '" + base + ".png'\n");
            w.write("set title '" + title + "'\n");
            w.write("set xlabel 'Generation'\n");
            w.write("set ylabel '" + ylabel + "'\n");
            w.write("set grid\n");
            w.write("set key right top\n");
            w.write("plot \\\n");
            w.write("  '" + datName + "' using 1:2 with linespoints lw 2 pt 7 title 'real simulator', \\\n");
            w.write("  '" + datName + "' using 1:3 with linespoints lw 2 pt 5 title 'surrogate'\n");
        }
        return gnu;
    }

    private static boolean runGnuplot(File workDir, File script) {
        try {
            Process p = new ProcessBuilder("gnuplot", script.getName())
                    .directory(workDir)
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
