package main.java;

/*******************************************************************************
 * Copyright (C) 2025 Pablo C. Cañizares
 *
 * Distributed under the GNU General Public License v3.0. See the LICENSE file in
 * the project root for the full license text.
 ******************************************************************************/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dataParser.cloud.ECloudSimulator;
import dataParser.cloud.TestCaseParser_cloud;
import dataParser.cloud.output.TcOutput_cloud;
import platform.HybridPlatform;
import platform.surrogate.SampleLogger;
import platform.surrogate.SurrogateModel;

/**
 * Offline audit of a surrogate evolution: re-evaluates the final Pareto front
 * with the <b>real</b> simulator. This is the heart of the offline adversarial
 * loop and needs no engine changes.
 *
 * <p>For each non-dominated solution of the last generation it (1) re-runs the
 * real simulator on its {@code .mtc} — which overwrites the run's
 * {@code output_*.tc} with the true values, so the reported front becomes real —
 * and (2) appends {@code (features, real energy_kwh, real sim_time_sec)} to the
 * re-training increment. Because the front is exactly where the optimiser
 * converged (and where it exploited the surrogate's errors), these are the most
 * valuable hard examples.</p>
 *
 * <pre>{@code
 *   java main.java.AuditFront -r <runDir> -S repro/cloudsimStorage.jar \
 *        -w repro -c repro/surrogate_increment.csv [--all]
 * }</pre>
 */
public class AuditFront {

    private static final Pattern P_ITERLINE = Pattern.compile("^\\s*(\\d+)\\s*-\\s*\\[([^\\]]+)\\]\\s*$");
    private static final Pattern P_ID = Pattern.compile("_(\\d+)\\.(tc|mtc)$");

    public static void main(String[] args) throws Exception {
        String runDir = null;
        String simJar = "repro/cloudsimStorage.jar";
        String workspace = "repro";
        String increment = null;
        boolean frontOnly = true;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-r": runDir = args[++i]; break;
                case "-S": simJar = args[++i]; break;
                case "-w": workspace = args[++i]; break;
                case "-c": increment = args[++i]; break;
                case "--all": frontOnly = false; break;
                case "-h": case "--help":
                    System.out.println("Usage: AuditFront -r runDir [-S simJar] [-w workspace] "
                            + "[-c increment.csv] [--all]");
                    return;
                default: System.out.println("Unknown argument: " + args[i]); return;
            }
        }
        if (runDir == null) {
            System.out.println("Missing -r <runDir>");
            return;
        }
        if (increment == null) {
            increment = workspace + "/surrogate_increment.csv";
        }

        File run = new File(runDir);
        Map<Integer, File> outputs = index(run, "output_");
        Map<Integer, File> inputs = index(run, "input_");
        Map<Integer, File> mtcs = indexMtc(run);

        List<Integer> lastGen = lastGenerationIds(new File(run, "iterationlist.txt"));
        if (lastGen.isEmpty()) {
            System.out.println("No iterationlist / generations found under " + runDir);
            return;
        }

        // Surrogate points of the last generation (before re-evaluation).
        List<Integer> ids = new ArrayList<>();
        List<double[]> pts = new ArrayList<>();
        for (int id : lastGen) {
            double[] p = readPoint(outputs.get(id));
            if (p != null && p[0] > 0 && p[1] > 0) {
                ids.add(id);
                pts.add(p);
            }
        }
        List<Integer> selected = frontOnly ? nonDominated(ids, pts) : ids;
        System.out.printf(Locale.US, "Auditing %d %s solution(s) of the last generation (%d individuals)%n",
                selected.size(), frontOnly ? "non-dominated" : "last-gen", lastGen.size());

        SampleLogger logger = new SampleLogger(increment, HybridPlatform.FEATURE_COLUMNS);
        File workdir = new File(workspace);
        TestCaseParser_cloud parser = new TestCaseParser_cloud(ECloudSimulator.eCLOUDSIMSTORAGE);

        double sumAbsEnergy = 0.0;
        int audited = 0;
        for (int id : selected) {
            File mtc = mtcs.get(id);
            File in = inputs.get(id);
            if (mtc == null || in == null) {
                continue;
            }
            double surrEnergy = readPoint(outputs.get(id))[0];

            if (!runSimulator(simJar, mtc, workdir)) {
                System.out.printf(Locale.US, "  id %d: simulator run failed, skipped%n", id);
                continue;
            }
            TcOutput_cloud out = (TcOutput_cloud) parser.doParseOutput(outputs.get(id).getPath());
            if (out == null || out.getCpuEnergyCons() <= 0) {
                continue;
            }
            double realEnergy = out.getCpuEnergyCons();
            double realTime = out.getSimTime();
            logger.record(SurrogateModel.readTestCase(in.getPath()), realEnergy, realTime);
            sumAbsEnergy += Math.abs(realEnergy - surrEnergy);
            audited++;
            System.out.printf(Locale.US, "  id %d: surrogate %.3f -> real %.3f kWh%n", id, surrEnergy, realEnergy);
        }

        System.out.printf(Locale.US, "Audited %d solution(s); mean |Δenergy| = %.3f kWh; increment -> %s%n",
                audited, audited > 0 ? sumAbsEnergy / audited : 0.0, increment);
    }

    private static boolean runSimulator(String simJar, File mtc, File workdir) {
        try {
            Process p = new ProcessBuilder("java", "-Xmx2g", "-Duser.language=en", "-Duser.country=US",
                    "-jar", new File(simJar).getAbsolutePath(), "--standalone", mtc.getAbsolutePath())
                    .directory(workdir)
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!p.waitFor(180, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return false;
            }
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static Map<Integer, File> index(File run, String prefix) {
        Map<Integer, File> map = new HashMap<>();
        collect(run, prefix, ".tc", map);
        return map;
    }

    private static Map<Integer, File> indexMtc(File run) {
        Map<Integer, File> map = new HashMap<>();
        collect(run, "tc_", ".mtc", map);
        return map;
    }

    private static void collect(File dir, String prefix, String suffix, Map<Integer, File> map) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File f : children) {
            if (f.isDirectory()) {
                collect(f, prefix, suffix, map);
            } else if (f.getName().startsWith(prefix) && f.getName().endsWith(suffix)) {
                Matcher m = P_ID.matcher(f.getName());
                if (m.find()) {
                    map.put(Integer.parseInt(m.group(1)), f);
                }
            }
        }
    }

    private static List<Integer> lastGenerationIds(File iterList) throws Exception {
        List<Integer> last = new ArrayList<>();
        if (!iterList.isFile()) {
            return last;
        }
        try (BufferedReader r = new BufferedReader(new FileReader(iterList))) {
            String line;
            while ((line = r.readLine()) != null) {
                Matcher m = P_ITERLINE.matcher(line);
                if (m.matches()) {
                    List<Integer> ids = new ArrayList<>();
                    for (String tok : m.group(2).trim().split("\\s+")) {
                        try {
                            ids.add(Integer.parseInt(tok));
                        } catch (NumberFormatException ignored) {
                            // skip non-numeric tokens
                        }
                    }
                    last = ids; // keep overwriting; ends on the last generation
                }
            }
        }
        return last;
    }

    /** {@code {energy, time}} from an output .tc (raw total energy + sim time), or null. */
    private static double[] readPoint(File f) {
        if (f == null || !f.isFile()) {
            return null;
        }
        Double e = null;
        Double t = null;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) {
                String s = line.trim();
                if (s.toLowerCase(Locale.ROOT).startsWith("total energy consumption (cpu+storage):")) {
                    e = num(s);
                } else if (s.toLowerCase(Locale.ROOT).startsWith("total simulation time:")) {
                    t = num(s);
                }
            }
        } catch (Exception ex) {
            return null;
        }
        return (e != null && t != null) ? new double[] { e, t } : null;
    }

    private static Double num(String line) {
        Matcher m = Pattern.compile("([0-9]+(?:[.,][0-9]+)?)").matcher(line.substring(line.indexOf(':') + 1));
        return m.find() ? Double.parseDouble(m.group(1).replace(',', '.')) : null;
    }

    /** Non-dominated subset (2D minimisation). */
    private static List<Integer> nonDominated(List<Integer> ids, List<double[]> pts) {
        List<Integer> front = new ArrayList<>();
        for (int i = 0; i < pts.size(); i++) {
            boolean dominated = false;
            for (int j = 0; j < pts.size() && !dominated; j++) {
                if (i != j) {
                    double[] a = pts.get(j);
                    double[] b = pts.get(i);
                    if (a[0] <= b[0] && a[1] <= b[1] && (a[0] < b[0] || a[1] < b[1])) {
                        dominated = true;
                    }
                }
            }
            if (!dominated) {
                front.add(ids.get(i));
            }
        }
        return front;
    }
}
