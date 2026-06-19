package core;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assume;
import org.junit.Test;

import algorithms.moga.EGAAlgorithms;
import algorithms.moga.PopulationMO;
import entities.MOCloudChromosome;
import main.java.Cloud_MO;

/**
 * Long, opt-in integration smoke test that runs a <b>complete MOGA evolution</b>
 * end to end against the real CloudSim-Storage simulator: it loads a seed test
 * case, mutates it into an initial population, simulates every individual,
 * evolves for a couple of generations and checks the resulting population holds
 * solutions with sane energy/time figures.
 *
 * <p>It drives the production orchestrator ({@link Cloud_MO#doEvolution()}), so
 * it exercises the whole pipeline — cloud-model transformation, mutation,
 * simulator execution, Pareto sorting and the graph/IO steps.</p>
 *
 * <p>To stay fast and within the default heap it uses a <b>reduced-scale</b>
 * variant of the bundled {@code Al_w3} case (128 VMs / 32 hosts instead of
 * 2048 / 512) and reuses the pre-decompressed traces under {@code repro/smoke}.
 * Mutation probability is set to "low" so most offspring stay simulatable.</p>
 *
 * <p>Opt-in via {@code -Dce.runSimulatorTests=true}. Requires
 * {@code repro/cloudsimStorage.jar}, the {@code repro/smoke} workload and, on
 * the classpath at runtime, {@code commons-io} (declared in {@code pom.xml}).</p>
 */
public class MOGAEvolutionSmokeTest {

    private static final int EVOLUTION_LOOPS = 2;

    @Test
    public void evolvesMOGAAgainstRealSimulatorAndYieldsSaneSolutions() throws Exception {
        Assume.assumeTrue(
                "Set -Dce.runSimulatorTests=true to run the evolution integration test",
                Boolean.getBoolean("ce.runSimulatorTests"));

        File simulatorJar = new File("repro/cloudsimStorage.jar");
        File seedTc = new File("repro/smoke/Al_w3/input.tc");
        File traces = new File("repro/smoke/workload/io_mix/cpu/mix_vm");
        Assume.assumeTrue("simulator jar missing", simulatorJar.isFile());
        Assume.assumeTrue("seed test case missing", seedTc.isFile());
        Assume.assumeTrue("workload traces missing", traces.isDirectory());

        Path sandbox = Files.createTempDirectory("ce-moga-it");
        Path expDir = sandbox.resolve("exp/Al_w3");
        Path inDir = Files.createDirectories(expDir.resolve("tcInput"));
        Files.createDirectories(expDir.resolve("tcOutput"));
        Path metaDir = Files.createDirectories(expDir.resolve("metaInfo"));
        Path pathBase = Files.createDirectories(sandbox.resolve("out"));

        // Reduced-scale seed pointing at the committed workload (absolute path so
        // the simulator subprocess, whose cwd is the project root, resolves it).
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

        // Drive the full orchestrator: algorithm=MOGA, low mutation, N loops.
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

        Cloud_MO orchestrator = new Cloud_MO(args, EGAAlgorithms.eMOGA);
        orchestrator.doEvolution();

        PopulationMO<MOCloudChromosome> population = orchestrator.getPopulation();
        assertNotNull("evolution did not produce a population (seed failed to simulate?)", population);
        assertTrue("population should not be empty after evolution", population.getSize() > 0);

        int valid = 0;
        double bestEnergy = Double.MAX_VALUE;
        for (int i = 0; i < population.getSize(); i++) {
            MOCloudChromosome c = population.getChromosomeByIndex(i);
            double energy = c.getEnergyConsumption();
            double time = c.getSimTime();
            if (energy > 0 && time > 0) {
                valid++;
                bestEnergy = Math.min(bestEnergy, energy);
            }
        }

        assertTrue("expected at least one simulatable (valid) solution, found none", valid > 0);
        // Sanity-check the best solution for the reduced-scale Al_w3 (reference ~1.15 kWh).
        assertTrue("best energy out of plausible range: " + bestEnergy + " kWh",
                bestEnergy > 0 && bestEnergy < 100.0);

        System.out.printf("[MOGA evolution IT] %d generations -> %d/%d valid solutions, best=%.4f kWh%n",
                EVOLUTION_LOOPS, valid, population.getSize(), bestEnergy);
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
