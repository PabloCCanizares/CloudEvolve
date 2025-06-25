# CloudEvolve – Multi‑Objective Optimization Framework for Cloud Infrastructures

**CloudEvolve** is a modular framework for evolving cloud architectures through multi‑objective genetic algorithms (MOGAs). It enables the simultaneous optimisation of energy consumption and performance, integrating seamlessly with simulators such as CloudSim.

---

### General Architecture

![High‑level architecture](docs/architecture_v5.png)

The optimisation workflow follows six main stages:

- **Cloud‑chromosome encoding** – each individual captures a *workload* and its target *cloud model* while tracking the objectives (energy consumption and response time).
- **Candidate model instantiation** – the chromosome is materialised into one or more *Cloud Configuration Models* (CCMs) that realise the logical architecture.
- **Simulation & evaluation** – every CCM is executed in the simulation backend to measure the objective metrics.
- **Selection** – the raw population is Pareto‑sorted; the top individuals are chosen as parents.
- **Crossover** – selected parents exchange structural segments of their architectures to create offspring CCMs.
- **Mutation** – random modifications are applied to offspring models, introducing new configurations before they re‑enter the evaluation loop.

The loop repeats until a stopping criterion is met, ultimately yielding the Pareto front of energy‑/performance‑optimal cloud architectures.

### Class Diagram

![Class diagram](docs/classDiagram_v6.png)

---

## Supported Algorithms

* VEGA (Vector Evaluated Genetic Algorithm)
* MOGA (Multi‑Objective Genetic Algorithm)
* NSGA‑II (Non‑Dominated Sorting Genetic Algorithm II)
* SPEA2 (Strength Pareto Evolutionary Algorithm 2)
* PAES (Pareto Archived Evolution Strategy)

---

## Project Structure

```
cloudevolve/
├── src/
│   ├── algorithms/        # MOEA implementations
│   ├── configuration/     # Experiment configs (YAML)
│   ├── entities/          # Domain entity definitions
│   ├── main/              # Entry point (CLI / scripts)
│   ├── auxiliars/         # Common utilities & helpers
│   ├── core/              # Orchestration kernel
│   ├── executor/          # Simulator adapters & evaluation runners
│   └── transformations/   # Data transformations & post‑processing
└── docs/                  # Guides and reference material
```

---

## License

Distributed under the **MIT License**. See the `LICENSE` file for details.

---

## Acknowledgements

* CloudSim‑Storage for the underlying simulation engine.
* MOEA algorithms inspired by the work of Deb, Zitzler and Corne.

