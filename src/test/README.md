# CloudEvolve – Unit Tests

## Overview

This document explains how to build and run the JUnit 4 test suite for the **CloudEvolve** project.

## Prerequisites

| Tool    | Minimum version |
|---------|-----------------|
| Java    | 8               |
| Maven   | 3.6             |

The required runtime library (`lib/MT.jar`) is already bundled with the repository and is configured as a system dependency in `pom.xml`.

## Running the Tests

Execute all tests from the repository root:

```bash
mvn test
```

Maven will compile all sources (which live directly under `src/`) and then run every class whose name ends in `Test`.

### Expected output

```
Tests run: 55, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Test Structure

Tests are placed alongside the production sources, mirroring the same package layout:

| Test class | Package | Classes under test |
|---|---|---|
| `src/algorithms/PopulationTest.java` | `algorithms` | `Population` |
| `src/algorithms/GeneticAlgorithmTest.java` | `algorithms` | `GeneticAlgorithm` |
| `src/algorithms/moga/GAObjectivesTest.java` | `algorithms.moga` | `GAObjectives` |
| `src/algorithms/moga/EGAObjectivesTest.java` | `algorithms.moga` | `EGAObjectives` |
| `src/algorithms/moga/MOSolutionTest.java` | `algorithms.moga` | `MOSolution` |
| `src/entities/MOCloudChromosomeTest.java` | `entities` | `MOCloudChromosome` |
| `src/configuration/EAConfigTest.java` | `configuration` | `EAConfig` |
| `src/configuration/LogLevelTest.java` | `configuration` | `LogLevel` |
| `src/configuration/EAMutationOperatorTest.java` | `configuration` | `EAMutationOperator` |

## Notes

* **External dependencies** – Classes that require a running simulator
  (`EAController`, `MT_Handler`, `MutableCloud`, etc.) are not exercised at
  runtime.  `MOCloudChromosomeTest` passes `null` for those references and
  the production code prints harmless error messages to stdout, which is the
  expected behaviour when those fields are absent.

* **`EAConfig` singleton** – `EAConfigTest` uses reflection to reset the
  private static `config` field to `null` in `@Before` so each test starts
  from a clean state.

* **`GeneticAlgorithm` listener** – The iteration-listener callback is
  currently commented out in `GeneticAlgorithm.evolve(int)`.
  `GeneticAlgorithmTest.testAddIterationListener` therefore only verifies
  that adding a listener and running evolve does not throw an exception.

* **`DummyChromosome` / `DummyFitness`** – Each test class that needs to
  instantiate `Population` or `GeneticAlgorithm` declares a minimal static
  inner helper class (`DummyChromosome`, `DummyFitness`) with no-op or
  simple implementations.  These helpers are **not** production code.
