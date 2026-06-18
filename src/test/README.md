# CloudEvolve – Unit & Smoke Tests

## Overview

This document explains how to build, run and measure the coverage of the JUnit 4
test suite for the **CloudEvolve** project.

The suite has two layers:

* **Unit tests** for the pure logic classes (population containers, objective
  model, comparators, configuration value objects, the entity chromosome and the
  2D hypervolume math).
* **Smoke tests** (`MOAlgorithmSmokeTest`) that wire a small population of
  lightweight dummy individuals to every evolutionary algorithm (MOGA, VEGA,
  VEGA2, NSGA-II, NSGA-II2, SPEA2, SPEA3, PAES, PAES2) and run a few evolution
  loops end-to-end, proving the selection / crossover / mutation / dominance
  pipeline executes without the external simulator.

## Prerequisites

| Tool    | Minimum version |
|---------|-----------------|
| Java    | 17              |
| Maven   | 3.6             |

The required runtime library (`lib/MT.jar`) is already bundled with the
repository and is configured as a system dependency in `pom.xml`. It is the only
external library needed to build and run the tests; the simulator backend
(`repro/cloudsimStorage.jar`) is **not** required for `mvn test`.

## Running the Tests

```bash
mvn test
```

Maven compiles all sources under `src/` and runs every class whose name ends in
`Test` (excluding the `MT_Test` scratch class). At the time of writing:

```
Tests run: 120, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Coverage

The JaCoCo plugin produces a coverage report during the `test` phase:

```bash
mvn test
open target/site/jacoco/index.html
```

The report scopes coverage to the **testable core** by excluding the
environment-coupled layer (see *Excluded from coverage* below).

## Scope: what is tested to 100% and what is not

`mvn test` runs against the whole tree, but only a well-defined **testable core**
is targeted for full coverage. The following classes are at **100% line
coverage**:

| Class | Package |
|---|---|
| `Population` | `algorithms` |
| `GeneticAlgorithm` (+ inner comparator) | `algorithms` |
| `PopulationMO` | `algorithms.moga` |
| `MOSolution` | `algorithms.moga` |
| `GAObjectives` / `EGAObjectives` | `algorithms.moga` |
| `CartesianDistanceComparator` | `algorithms.moga` |
| `MOCloudChromosome` | `entities` |
| `EAConfig`, `LogLevel`, `EAMutationOperator`, `EACrossoverOperator` | `configuration` |
| `HV2D`, `Extreme` | `main_scico.hv` |

The nine evolutionary algorithms and `AdaptiveCellGrid` are covered by the smoke
tests at **75–96%** line coverage (the uncovered remainder is mostly defensive
branches and the unused PAES comparator `sort()`).

### Excluded from coverage

These are not unit-testable in isolation and are excluded from the JaCoCo report:

* **Simulator-coupled:** `executor.MT_Handler`, `core.MOCloudOrchestrator`,
  `core.MOCloudFitness` (they shell out to the CloudSim/SimGrid `.jar`).
* **Filesystem / singleton-coupled:** `configuration.EAController`,
  `transformations.*`, `auxiliars.*`, `entities.AbstractCloudChromosome`'s
  `mutate()`/`crossover()` (all route through `EAController` + absolute
  `/localSpace` paths).
* **Launchers / scratch:** `main.java.*`, `main_scico.Launcher*`,
  `main_scico.aux.*`, `main_scico.experiments.*`, `main_scico.hv.refpoint.*` and
  the log-reading hypervolume utilities (`ParetoAndHVFromLogs`, …) — these have
  `main()` methods and read experiment outputs from disk.
* **Dead code:** the `*_old` chromosome classes.

## Notes

* **Locale independence** – `MOCloudChromosomeTest.testToString` builds its
  expected string with the same `String.format` spec as the production code, so
  it passes on both dot- and comma-decimal locales.
* **Smoke-test dummies** – `MOAlgorithmSmokeTest` uses a `DummyMOChromosome`
  whose `mutate()`/`crossover()` always return non-null offspring; this is
  required by PAES, whose evolve loop spins until a mutation succeeds.
* **Known issue surfaced by the smoke tests** – `AdaptiveCellGrid` indexes its
  fixed `divisions²` cell array directly from raw objective values, so it throws
  `IndexOutOfBoundsException` when the objective range exceeds the grid
  resolution. The PAES smoke tests size the grid (`bisections = 6`) to avoid it;
  the underlying class would benefit from a fix.
* **`EAConfig` singleton** – `EAConfigTest` resets the private static instance via
  reflection in `@Before` so tests stay independent.
* **Dummy helpers** are minimal `Chromosome`/`Fitness` implementations declared
  as static inner classes of each test; they are **not** production code.
