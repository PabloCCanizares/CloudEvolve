package core;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import algorithms.moga.EGAAlgorithms;
import algorithms.moga.PopulationMO;
import entities.MOCloudChromosome;
import main.java.Cloud_MO;

/**
 * Long, opt-in integration smoke test that runs a <b>complete evolution</b>
 * end to end against the real CloudSim-Storage simulator — once per MOEA in
 * {@link EGAAlgorithms} (MOGA, VEGA, VEGA2, NSGA-II, NSGA-II2, SPEA2, SPEA3,
 * PAES, PAES2).
 *
 * <p>Each case drives the production orchestrator ({@link Cloud_MO#doEvolution()}),
 * exercising the whole pipeline — cloud-model transformation, mutation, simulator
 * execution, the algorithm's selection / Pareto sorting and the graph/IO steps —
 * and asserts the resulting population holds simulatable solutions with sane
 * energy/time figures.</p>
 *
 * <p>To stay fast and within the default heap it uses a <b>reduced-scale</b>
 * variant of the bundled {@code Al_w3} case (128 VMs / 32 hosts) and reuses the
 * pre-decompressed traces under {@code repro/smoke}; mutation probability is set
 * to "low" so most offspring stay simulatable.</p>
 *
 * <p>Opt-in via {@code -Dce.runSimulatorTests=true}. Requires
 * {@code repro/cloudsimStorage.jar}, the {@code repro/smoke} workload and, on the
 * runtime classpath, {@code commons-io} (declared in {@code pom.xml}).</p>
 */
@RunWith(Parameterized.class)
public class AlgorithmEvolutionSmokeTest {

    private static final int EVOLUTION_LOOPS = 1;

    @Parameters(name = "{0}")
    public static Collection<Object[]> algorithms() {
        List<Object[]> params = new ArrayList<>();
        for (EGAAlgorithms algorithm : EGAAlgorithms.values()) {
            params.add(new Object[] { algorithm });
        }
        return params;
    }

    @Parameter
    public EGAAlgorithms algorithm;

    @Test
    public void evolvesAgainstRealSimulatorAndYieldsSaneSolutions() throws Exception {
        Assume.assumeTrue(
                "Set -Dce.runSimulatorTests=true to run the evolution integration tests",
                Boolean.getBoolean("ce.runSimulatorTests"));

        File simulatorJar = new File("repro/cloudsimStorage.jar");
        File seedTc = new File("repro/smoke/Al_w3/input.tc");
        File traces = new File("repro/smoke/workload/io_mix/cpu/mix_vm");
        Assume.assumeTrue("simulator jar missing", simulatorJar.isFile());
        Assume.assumeTrue("seed test case missing", seedTc.isFile());
        Assume.assumeTrue("workload traces missing", traces.isDirectory());

        Path sandbox = Files.createTempDirectory("ce-evo-it-" + algorithm.name());
        Path expDir = sandbox.resolve("exp/Al_w3");
        Path inDir = Files.createDirectories(expDir.resolve("tcInput"));
        Files.createDirectories(expDir.resolve("tcOutput"));
        Path metaDir = Files.createDirectories(expDir.resolve("metaInfo"));
        Path pathBase = Files.createDirectories(sandbox.resolve("out"));

        String workPath = new File("repro/smoke/workload/io_mix/cpu").getAbsolutePath();
        Path tcInput = inDir.resolve("input_00000.tc");
        rewriteSeed(seedTc.toPath(), tcInput, workPath);

        Path mtc = metaDir.resolve("tc_00000.mtc");
        List<String> meta = new ArrayList<>();
        meta.add("Id: 0");
        meta.add("GroupId: 0");
        meta.add("MutantId: 0");
        meta.add("InputSrc: " + tcInput.toAbsolutePath());
        meta.add("OutputSrc: " + expDir.resolve("tcOutput/output_00000.tc").toAbsolutePath());
        meta.add("ExecCommands: ");
        meta.add("isSourceTC: true");
        meta.add("followUpChilds: ");
        meta.add("Description: Source test case");
        Files.write(mtc, meta, StandardCharsets.UTF_8);

        String[] args = {
                "eCloudSimStorage",
                expDir.toAbsolutePath().toString(),
                Integer.toString(EVOLUTION_LOOPS),
                "2",                                   // mutation probability: low
                "1",                                   // rule base
                pathBase.toAbsolutePath().toString(),
                "1",                                   // re-runs
                simulatorJar.getAbsolutePath()
        };

        Cloud_MO orchestrator = new Cloud_MO(args, algorithm);
        orchestrator.doEvolution();

        PopulationMO<MOCloudChromosome> population = orchestrator.getPopulation();
        assertNotNull(algorithm + ": evolution produced no population (seed failed to simulate?)", population);
        assertTrue(algorithm + ": population should not be empty after evolution", population.getSize() > 0);

        int valid = 0;
        double bestEnergy = Double.MAX_VALUE;
        for (int i = 0; i < population.getSize(); i++) {
            MOCloudChromosome c = population.getChromosomeByIndex(i);
            if (c.getEnergyConsumption() > 0 && c.getSimTime() > 0) {
                valid++;
                bestEnergy = Math.min(bestEnergy, c.getEnergyConsumption());
            }
        }

        assertTrue(algorithm + ": expected at least one simulatable solution, found none", valid > 0);
        assertTrue(algorithm + ": best energy out of plausible range: " + bestEnergy + " kWh",
                bestEnergy > 0 && bestEnergy < 100.0);

        System.out.printf("[evolution IT] %-7s -> %d/%d valid, best=%.4f kWh%n",
                algorithm, valid, population.getSize(), bestEnergy);
    }

    /** Copy the seed .tc, scaling it down and repointing its workload path. */
    private static void rewriteSeed(Path src, Path dst, String workPath) throws Exception {
        List<String> out = new ArrayList<>();
        for (String line : Files.readAllLines(src, StandardCharsets.UTF_8)) {
            if (line.startsWith("work.path")) {
                out.add("work.path = " + workPath);
            } else if (line.startsWith("vm.quantity")) {
                out.add("vm.quantity = 128");
            } else if (line.startsWith("host.quantity")) {
                out.add("host.quantity = 32");
            } else {
                out.add(line);
            }
        }
        Files.write(dst, out, StandardCharsets.UTF_8);
    }
}
