# CloudEvolve – Multi-Objective Optimization Framework for Cloud Infrastructures

**CloudEvolve** is a modular framework for evolving cloud architectures through multi-objective genetic algorithms (MOGAs). It enables the simultaneous optimisation of energy consumption and performance, integrating seamlessly with simulators such as CloudSim.

---

### General Architecture

![High-level architecture](docs/architecture_v5_white.png)

The optimisation workflow follows six main stages:

- **Cloud-chromosome encoding** – each individual captures a *workload* and its target *cloud model* while tracking the objectives (energy consumption and response time).
- **Candidate model instantiation** – the chromosome is materialised into one or more *Cloud Configuration Models* (CCMs) that realise the logical architecture.
- **Simulation & evaluation** – every CCM is executed in the simulation backend to measure the objective metrics.
- **Selection** – the raw population is Pareto-sorted; the top individuals are chosen as parents.
- **Crossover** – selected parents exchange structural segments of their architectures to create offspring CCMs.
- **Mutation** – random modifications are applied to offspring models, introducing new configurations before they re-enter the evaluation loop.

The loop repeats until a stopping criterion is met, ultimately yielding the Pareto front of energy-/performance-optimal cloud architectures.

### Class Diagram

![Class diagram](docs/classDiagram_v6.png)

---

## Requirements

- **JDK 17+** (the build targets Java 17).
- **Maven 3.6+** to build and run the test suite.
- The metamorphic-testing layer (`lib/MT.jar`) is bundled, and JUnit / commons-io
  are resolved by Maven. The CloudSim-Storage simulator backend
  (`repro/cloudsimStorage.jar`) is also bundled for the smoke tests, and is only
  required for actual evolution runs.

---

## Quick Start

Build the project and run the full test suite — **no simulator required**:

```bash
mvn test
```

This runs the unit tests plus in-memory *smoke tests* that drive all nine MOEAs
through the complete selection / crossover / mutation / dominance pipeline. It
needs nothing beyond the JDK and Maven.

### Run against the real simulator (optional)

To additionally exercise the bundled CloudSim-Storage backend end-to-end, opt in:

```bash
mvn test -Dce.runSimulatorTests=true
```

This launches the real simulator on the ready-to-run fixture under `repro/smoke/`
(a downscaled `Al_w3` case): it evolves a small population per algorithm and
checks the resulting energy/time figures are sane.

### Run a full experiment (optional)

The launchers under `src/main_scico/` (`Launcher_VEGA`, `Launcher_MOGA`, …) run a
complete evolution for one algorithm against the simulator. They resolve their
paths from `CLOUDEVOLVE_WORKSPACE` (see [Configuration](#configuration)), so point
it at a directory holding `cloudsimStorage/cloudsimStorage.jar` and a
`cloudsimStorage/evolutionary/InitialPopulation/<case>` tree, then run e.g.
`main_scico.Launcher_VEGA`.

---

## Configuration

The simulator and its data (jar, evolutionary runs, initial populations) are
resolved from a single configurable **workspace root**, so you never have to edit
source:

| Source | Key | Default |
|---|---|---|
| Environment variable | `CLOUDEVOLVE_WORKSPACE` | `/localSpace/cloudEnergy` |
| JVM system property | `-Dcloudevolve.workspace` | (overrides the env var) |

Each simulator appends its own subtree. For example, with
`CLOUDEVOLVE_WORKSPACE=/Users/you/cloudEvolution`, CloudSim-Storage runs live
under `/Users/you/cloudEvolution/cloudsimStorage/evolutionary`:

```bash
export CLOUDEVOLVE_WORKSPACE=/Users/you/cloudEvolution
```

> The previous names `CLOUDEVOLVE_HOME` / `-Dcloudevolve.home` are **deprecated**
> but still honoured as a fallback.

---

## Supported Algorithms

* **MOGA** — Multi-Objective Genetic Algorithm
* **VEGA / VEGA2** — Vector Evaluated Genetic Algorithm
* **NSGA-II / NSGA-II2** — Non-Dominated Sorting Genetic Algorithm II
* **SPEA2 / SPEA3** — Strength Pareto Evolutionary Algorithm
* **PAES / PAES2** — Pareto Archived Evolution Strategy

---

## Project Structure

```
cloudevolve/
├── src/
│   ├── algorithms/        # MOEA implementations (MOGA, VEGA, NSGA-II, SPEA2, PAES, …)
│   ├── configuration/     # Experiment configuration, mutation/crossover operators
│   ├── core/              # Orchestration kernel (evolution loop, fitness)
│   ├── entities/          # Cloud-chromosome domain model
│   ├── executor/          # Simulator adapters & evaluation runners
│   ├── platform/          # Per-simulator strategy (paths, operators, execution, transforms)
│   ├── transformations/   # Test-case <-> cloud-model transformations
│   ├── main/              # CLI entry points
│   ├── main_scico/        # Experiment launchers (Launcher_VEGA, …)
│   └── auxiliars/         # Common utilities & helpers
├── lib/MT.jar             # Bundled metamorphic-testing layer
├── repro/                 # Bundled simulator jar + ready-to-run smoke fixtures
└── docs/                  # Guides and reference material
```

---

## License

Distributed under the **GNU General Public License v3.0**. See the `LICENSE`
file for the full text.

Some files under `src/algorithms/` are derived from Yuriy Lagodiuk's genetic
algorithm library and retain their original Apache License 2.0 headers, which is
compatible with GPLv3.

---

## Acknowledgements

* CloudSim-Storage for the underlying simulation engine.
* MOEA algorithms inspired by the work of Deb, Zitzler and Corne.
