package executor;

import static org.junit.Assert.*;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Assume;
import org.junit.Test;

/**
 * Integration smoke test that runs the <b>real</b> CloudSim-Storage simulator
 * (repro/cloudsimStorage.jar) on one of the bundled initial-population test
 * cases and checks the reported energy / time are sane.
 *
 * <p>The bundled {@code .tc}/{@code .mtc} files reference absolute
 * {@code /localSpace/...} paths from the original research machine, so the test
 * provisions a self-contained sandbox: it extracts the workload traces the test
 * case needs from {@code repro/workload.zip}, copies the test case into a temp
 * dir and rewrites the workload / input / output paths to point there.</p>
 *
 * <p>The simulator is launched directly (rather than via {@link MT_Handler}) so
 * the test can pin the two things the framework is fragile about:</p>
 * <ul>
 *   <li><b>locale</b> — the simulator prints decimals with the default locale
 *       and its own output parser expects a dot, so the run is forced to
 *       {@code en_US} to stay reproducible on any host;</li>
 *   <li><b>heap</b> — the full-size initial population (2048 VMs / 512 hosts)
 *       needs more than the default heap, so {@code -Xmx2g} is supplied.</li>
 * </ul>
 *
 * <p>It is <b>opt-in</b>: it only runs with {@code -Dce.runSimulatorTests=true}
 * (and when the simulator jar + workload archive are present), keeping the
 * default {@code mvn test} fast and free of external-process dependencies.</p>
 */
public class SimulatorIntegrationSmokeTest {

    private static final String CPU_PREFIX = "workload/io_mix/cpu/";
    private static final String TRACES_PREFIX = CPU_PREFIX + "mix_vm/";

    private static final Pattern ENERGY =
            Pattern.compile("total Energy consumption \\(CPU\\+storage\\):\\s*([0-9.]+)");
    private static final Pattern TIME =
            Pattern.compile("Total simulation time:\\s*([0-9.]+)");

    @Test
    public void runsRealSimulatorOnInitialPopulationAndReportsSaneEnergyAndTime() throws Exception {
        Assume.assumeTrue(
                "Set -Dce.runSimulatorTests=true to run the simulator integration test",
                Boolean.getBoolean("ce.runSimulatorTests"));

        File simulatorJar = new File("repro/cloudsimStorage.jar");
        File workloadZip = new File("repro/workload.zip");
        File caseDir = new File("repro/InitialPopulation/Al_w3");
        Assume.assumeTrue("simulator jar missing", simulatorJar.isFile());
        Assume.assumeTrue("workload archive missing", workloadZip.isFile());
        Assume.assumeTrue("initial population missing", caseDir.isDirectory());

        Path sandbox = Files.createTempDirectory("ce-sim-it");

        // 1. Extract the workload trace folder (work.path/work.name) the case needs.
        Path cpuDir = sandbox.resolve(CPU_PREFIX);
        int traces = extractTree(workloadZip, TRACES_PREFIX, sandbox);
        assertTrue("no workload traces extracted", traces > 0);

        // 2. Copy the .tc input, repointing work.path into the sandbox.
        Path inDir = Files.createDirectories(sandbox.resolve("env/tcInput"));
        Path outDir = Files.createDirectories(sandbox.resolve("env/tcOutput"));
        Path metaDir = Files.createDirectories(sandbox.resolve("env/metaInfo"));
        Path tcInput = inDir.resolve("input_00000.tc");
        Path tcOutput = outDir.resolve("output_00000.tc");
        Path mtc = metaDir.resolve("tc_00000.mtc");

        final String workPath = cpuDir.toAbsolutePath().toString();
        rewriteLines(caseDir.toPath().resolve("tcInput/input_00000.tc"), tcInput, line ->
                line.startsWith("work.path") ? "work.path = " + workPath : line);

        // 3. Write a .mtc pointing at the sandbox input/output.
        List<String> meta = new ArrayList<>();
        meta.add("Id: 0");
        meta.add("GroupId: 0");
        meta.add("MutantId: 0");
        meta.add("InputSrc: " + tcInput.toAbsolutePath());
        meta.add("OutputSrc: " + tcOutput.toAbsolutePath());
        meta.add("ExecCommands: ");
        meta.add("isSourceTC: true");
        meta.add("followUpChilds: ");
        meta.add("Description: Source test case");
        Files.write(mtc, meta, StandardCharsets.UTF_8);

        // 4. Launch the real simulator (pinned locale + heap) and wait for it.
        Process p = new ProcessBuilder(
                "java", "-Xmx2g", "-Duser.language=en", "-Duser.country=US",
                "-jar", simulatorJar.getAbsolutePath(),
                "--standalone", mtc.toAbsolutePath().toString())
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();
        assertTrue("simulator did not finish within 120s", p.waitFor(120, TimeUnit.SECONDS));
        assertEquals("simulator exited abnormally", 0, p.exitValue());

        // 5. The simulator writes its run log (incl. the energy/time figures) to OutputSrc.
        assertTrue("the simulator must produce an output file", Files.exists(tcOutput));
        String report = new String(Files.readAllBytes(tcOutput), StandardCharsets.UTF_8);

        double energy = extract(ENERGY, report, "energy consumption");
        double time = extract(TIME, report, "simulation time");

        // Approximate sanity: positive, finite and within a plausible range for
        // the bundled Al_w3 workload (its reference run reports ~24 kWh / ~2770 s).
        assertTrue("energy should be positive, was " + energy, energy > 0.0);
        assertTrue("energy out of plausible range: " + energy + " kWh", energy < 10_000.0);
        assertTrue("simulation time should be positive, was " + time, time > 0.0);

        System.out.printf("[simulator IT] Al_w3 -> energy=%.4f kWh, time=%.2f s%n", energy, time);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static double extract(Pattern pattern, String text, String what) {
        Matcher m = pattern.matcher(text);
        assertTrue("could not find " + what + " in the simulator output", m.find());
        return Double.parseDouble(m.group(1));
    }

    private static int extractTree(File zip, String prefix, Path destRoot) throws Exception {
        int count = 0;
        try (ZipFile zf = new ZipFile(zip)) {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().startsWith(prefix)) {
                    continue;
                }
                Path dest = destRoot.resolve(entry.getName());
                Files.createDirectories(dest.getParent());
                try (InputStream in = zf.getInputStream(entry);
                     OutputStream out = Files.newOutputStream(dest)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                    }
                }
                count++;
            }
        }
        return count;
    }

    private interface LineMapper {
        String map(String line);
    }

    private static void rewriteLines(Path src, Path dst, LineMapper mapper) throws Exception {
        List<String> out = new ArrayList<>();
        for (String line : Files.readAllLines(src, StandardCharsets.UTF_8)) {
            out.add(mapper.map(line));
        }
        Files.write(dst, out, StandardCharsets.UTF_8);
    }
}
