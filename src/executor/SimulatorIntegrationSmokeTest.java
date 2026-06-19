package executor;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assume;
import org.junit.Test;

/**
 * Integration smoke test that runs the <b>real</b> CloudSim-Storage simulator
 * (repro/cloudsimStorage.jar) on the bundled {@code Al_w3} initial-population
 * case and checks the reported energy / time are sane.
 *
 * <p>It uses the ready-to-run fixture under {@code repro/smoke/}, which is
 * pre-decompressed and uses paths relative to that folder, so the test launches
 * the simulator instantly — no archive extraction, no path rewriting. The
 * simulator is started with {@code repro/smoke} as its working directory, a
 * pinned {@code en_US} locale (its output parser expects a dot decimal
 * separator) and {@code -Xmx2g} (the full-size 2048-VM / 512-host case exceeds
 * the default heap).</p>
 *
 * <p>It is <b>opt-in</b>: it only runs with {@code -Dce.runSimulatorTests=true}
 * (and when the simulator jar + fixture are present), keeping the default
 * {@code mvn test} fast and free of external-process dependencies.</p>
 */
public class SimulatorIntegrationSmokeTest {

    private static final Pattern ENERGY =
            Pattern.compile("total Energy consumption \\(CPU\\+storage\\):\\s*([0-9.]+)");
    private static final Pattern TIME =
            Pattern.compile("Total simulation time:\\s*([0-9.]+)");

    @Test
    public void runsRealSimulatorOnInitialPopulationAndReportsSaneEnergyAndTime() throws Exception {
        Assume.assumeTrue(
                "Set -Dce.runSimulatorTests=true to run the simulator integration test",
                Boolean.getBoolean("ce.runSimulatorTests"));

        File fixture = new File("repro/smoke");
        File simulatorJar = new File("repro/cloudsimStorage.jar");
        File metaTc = new File(fixture, "Al_w3/meta.mtc");
        Assume.assumeTrue("simulator jar missing", simulatorJar.isFile());
        Assume.assumeTrue("smoke fixture missing", metaTc.isFile());

        // The simulator writes its run log (incl. the energy/time figures) here.
        File output = new File(fixture, "Al_w3/output.tc");
        output.delete();

        // Launch the real simulator with the fixture as working directory so the
        // relative paths inside meta.mtc / input.tc resolve. Locale and heap are
        // pinned (see class doc).
        Process p = new ProcessBuilder(
                "java", "-Xmx2g", "-Duser.language=en", "-Duser.country=US",
                "-jar", simulatorJar.getAbsolutePath(),
                "--standalone", "Al_w3/meta.mtc")
                .directory(fixture)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();
        assertTrue("simulator did not finish within 120s", p.waitFor(120, TimeUnit.SECONDS));
        assertEquals("simulator exited abnormally", 0, p.exitValue());

        assertTrue("the simulator must produce an output file", output.isFile());
        String report = new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8);

        double energy = extract(ENERGY, report, "energy consumption");
        double time = extract(TIME, report, "simulation time");

        // Approximate sanity: positive, finite and within a plausible range for
        // the bundled Al_w3 workload (its reference run reports ~24 kWh / ~2770 s).
        assertTrue("energy should be positive, was " + energy, energy > 0.0);
        assertTrue("energy out of plausible range: " + energy + " kWh", energy < 10_000.0);
        assertTrue("simulation time should be positive, was " + time, time > 0.0);

        System.out.printf("[simulator IT] Al_w3 -> energy=%.4f kWh, time=%.2f s%n", energy, time);
    }

    private static double extract(Pattern pattern, String text, String what) {
        Matcher m = pattern.matcher(text);
        assertTrue("could not find " + what + " in the simulator output", m.find());
        return Double.parseDouble(m.group(1));
    }
}
