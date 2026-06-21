# Paper figures: surrogate vs all-real

`paper_figures.py` produces the two figures usually reported for a multi-objective
study, comparing an evolution driven by the **surrogate** against one driven by
the **real simulator**:

- `hv_evolution.png` — hypervolume vs generation (convergence).
- `pareto_front.png` — the final Pareto front in objective space (energy × time).

Energy is the GA's fitness energy (raw total × simtime/3600). To be honest about
quality, the surrogate run's final front is **re-evaluated on the real simulator**
(via `main.java.AuditFront`) before plotting, so the Pareto figure compares the
surrogate's solutions on their *true* objectives.

## Regenerate

```bash
# one real and one surrogate run (same algorithm / configuration)
repro/launcherSingleConf.sh -a eNSGAII -n Al_w1 -i 12 -o /tmp/real
repro/launcherSurrogate.sh  -a eNSGAII -n Al_w1 -i 12 -s lib/surrogate -g clamp -o /tmp/surr

python repro/paper_figures.py --real <realRunDir> --surrogate <surrRunDir> \
    --launcher repro/CloudEvolve-launcher.jar --sim repro/cloudsimStorage.jar \
    --workspace repro --out repro/paper_figures
```

Needs the `surrogate-export` env (lightgbm, matplotlib, …). For the real Pareto
trade-off, unzip the full workload first (`unzip -o repro/workload.zip -d repro`);
the 20-trace subset leaves the simulation time nearly constant.

## Reading the committed example (NSGA-II, Al_w1, full workload)

The surrogate-guided search is orders of magnitude faster but here reaches a
worse true front than the all-real search: the real run converges to ~8 kWh while
the surrogate's solutions, re-evaluated, sit higher. Two caveats for paper-final
figures:

1. **Scale** — this is a single run of 12 generations. The paper's Fig. 5 is
   30-run hypervolume **boxplots** (`launcherAllExperiments.sh`); use those for
   the headline claim, with confidence intervals.
2. **Train/eval consistency** — the surrogate must be trained on the same workload
   distribution it is evaluated on. The example uses a model refined on the
   hybrid's hard examples; for a clean comparison, retrain on the full-workload
   dataset (`retrain_surrogate.py`) before plotting.

These figures show the **method and the honest trade-off** (speed vs accuracy),
which is what motivates the hybrid backend and the adversarial re-training loop.
