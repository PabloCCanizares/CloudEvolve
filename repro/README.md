# Reproducibility package

This folder lets a reviewer reproduce the study in:

> M. Pérez, P. C. Cañizares, A. Núñez. **Multi-objective optimization of cloud systems.**
> *Science of Computer Programming* 252 (2026) 103447.
> https://doi.org/10.1016/j.scico.2026.103447

The paper combines **multi-objective genetic algorithms (MOGAs)** with **metamorphic
testing (MT)** and the **CloudSim-Storage** simulator to optimise cloud
configurations for two conflicting objectives — **energy consumption** and
**execution time**. It evaluates five MOGAs (VEGA, MOGA, PAES, NSGA-II, SPEA2)
and reports its results as **hypervolume (HV)**.

**Headline claims to reproduce**
1. The **high** mutation configuration gives the best results (Fig. 4).
2. **NSGA-II** is the best algorithm overall (Fig. 5, Table 6).
3. The **metamorphic relations (MRs) help** (ablation, Fig. 7, Table 7).

---

## Self-contained: nothing to install or edit

Everything is bundled and path-independent. The launcher scripts root themselves
at this folder and export `CLOUDEVOLVE_WORKSPACE` automatically, so paths inside
the `.mtc` seeds (`${workspace}/…`) and the relative `work.path` in the `.tc`
files resolve here. **You do not edit any source or data file.**

Bundled here:

```
repro/
├── CloudEvolve-launcher.jar   # the framework (main.java.Cloud_MO), built from the repo
├── cloudsimStorage.jar        # the CloudSim-Storage simulator backend
├── InitialPopulation/<cfg>/   # the four seed configurations
├── workload/io_mix/cpu/mix_vm/# workload traces (20-trace subset, see note)
├── smoke/                     # instant simulator smoke fixture (Tier 0)
├── launcherSingleConf.sh      # one algorithm × one configuration (Tier 1)
├── launcherFullAlgorithm.sh   # one algorithm × 4 configs × N runs
├── launcherAllExperiments.sh  # all 5 algorithms (Tier 2 / Tier 3)
├── boxplot_all_algorithms.gnu # Fig. 5 boxplots
├── boxplot_ablation.gnu       # Fig. 7 ablation boxplots
└── redraw_30times.sh          # regenerate the figures
```

### The four configurations

| Folder  | Cloud  | Workload | Paper label |
|---------|--------|----------|-------------|
| `Al_w1` | CloudA (low-perf)  | small (ωˢ) | CloudA-ωˢ |
| `Al_w3` | CloudA (low-perf)  | large (ωˡ) | CloudA-ωˡ |
| `Bl_w1` | CloudB (high-perf) | small (ωˢ) | CloudB-ωˢ |
| `Bl_w3` | CloudB (high-perf) | large (ωˡ) | CloudB-ωˡ |

Mutation configuration: `-m 0` = **high**, `-m 1` = mid, `-m 2` = low. The paper's
main results use **high** (the default).

### ⚠️ Workload subset — read this

To keep the package small, `repro/workload/` ships a **20-trace subset** of the
full PlanetLab workload (the complete set, 14 649 files, is in `workload.zip`).
The simulator samples `work.numTraces` (=10) traces from it, so runs are
reproducible **but the absolute kWh/seconds will differ from the paper**, which
used the full workload. What reproduces with the subset is the **methodology and
the trends** (NSGA-II best, high mutation best). To match the paper's absolute
numbers, unzip `workload.zip` over `repro/workload/` first:

```bash
unzip -o workload.zip -d .
```

---

## Requirements

- **JDK 17+** (`java -version`).
- **bash** (the launcher scripts).
- **gnuplot** (only to regenerate the figures).

No build step: the jars are prebuilt. (To rebuild the launcher from source:
`mvn -q -DskipTests package` at the repo root, or see the project README.)

---

## Tier 0 — Smoke (instant)

Proves the simulator runs end-to-end on one case:

```bash
./smoke/run.sh
# -> total Energy consumption (CPU+storage): ~23.89 kWh ; Total simulation time: ~2770 s
```

## Tier 1 — One evolution (minutes)

Runs the full MOGA + MT + simulator pipeline for one algorithm on one
configuration, writing the Pareto front and per-iteration logs under `out/`:

```bash
./launcherSingleConf.sh -a eNSGAII -n Al_w3 -i 100
# output: out/NSGAII/<timestamp>_Al_w3/   (evolution.dat, histogram.dat, Pareto logs)
```

Use a small `-i` (e.g. `-i 5`) for a quick check of the pipeline.

### Surrogate backend (fast variant)

`launcherSurrogate.sh` runs the exact same pipeline but evaluates fitness with a
trained **LightGBM surrogate** instead of launching the simulator, so a full
evolution finishes in seconds. It is a drop-in replacement (same algorithms,
metamorphic testing and cloud model); only the `(energy, time)` evaluation is
predicted rather than simulated.

```bash
./launcherSurrogate.sh -a eNSGAII -n Al_w3 -i 100
# output: out_surrogate/NSGAII/<timestamp>_Al_w3/
```

The models are read from `../lib/surrogate` by default (this folder lives inside
the repo). If you run `repro/` standalone, copy `surrogate_energy_lgbm.txt` and
`surrogate_time_lgbm.txt` somewhere and pass the directory with `-s`. Being a
surrogate, the figures approximate the simulator; use the real backend for the
paper's exact numbers.

## Tier 2 — Reduced study (hours)

The full design at reduced scale — enough to see the trend without the full cost:

```bash
./launcherAllExperiments.sh -r 2 -i 20      # 5 algos × 4 configs × 2 runs × 20 iters, high mutation
```

Then generate the boxplots (see **Figures**). You should already see NSGA-II
leading on most configurations.

## Tier 3 — Full study (days)

The paper's exact design (5 algos × 4 configs × **30 runs** × **100 iters**,
high mutation ≈ millions of simulations):

```bash
unzip -o workload.zip -d .                  # full workload, for exact numbers
./launcherAllExperiments.sh                 # defaults: -r 30 -i 100 -m 0
```

For the mutation-rate study (Fig. 4) repeat with `-m 1` and `-m 2`.

**Hardware reference (paper):** MacBook, Apple M3 (8-core), 16 GiB RAM, macOS
Sequoia 15, Java 17. The full study takes on the order of days at this scale.

---

## Figures

After running an experiment (Tier 2/3), generate the hypervolume (HV) boxplots
(Fig. 5) in one step:

```bash
./computeHV.sh
# -> out/<algo>/hv__<config>.dat   and   out/boxplot_hv_all_algos.eps
```

`computeHV.sh` runs `main_scico.hv.ParetoAndHVFromLogs` on every run under `out/`
(HV per iteration), aggregates the **final** HV of each run into
`out/<algo>/hv__<config>.dat` (one value per line), and draws
`boxplot_all_algorithms.gnu`. Requires **gnuplot**.

**Reference points.** By default the HV uses an *empirical* reference (max
objective ×1.1 per run), which is the right choice for the bundled 20-trace
subset. To use the paper's fixed reference points, add `--paper-ref` — but only
with the full workload (`unzip workload.zip`), otherwise some fronts fall outside
the fixed reference and the HV collapses to 0.

The boxplot expects the groups `Al_w1 Al_w3 Bl_w1 Bl_w3` and the algorithms
`PAES SPEA2 NSGAII VEGA MOGA` (i.e. the `out/<algo>/` folders the launchers
create).

---

## Expected results

- **Trend (subset or full):** NSGA-II reaches the best HV fastest and is the best
  overall; the **high** mutation configuration dominates low/mid; PAES with MRs
  beats PAES without MRs (ablation).
- **Exact values (full workload only):** see **Table 6** of the paper for the
  per-algorithm aggregated HV with 95% confidence intervals.
