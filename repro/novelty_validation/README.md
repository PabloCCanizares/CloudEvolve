# Novelty / OOD detector validation

Does a novelty score tell us *where the surrogate will be wrong*? This artifact
answers it empirically for three detectors, so we only ship one that actually
correlates with surrogate error.

## Detectors compared

| detector | what it measures | artifact / cost |
|---|---|---|
| `leaf` | LightGBM leaf support — landing in low-count leaves = sparse region | none (read from the model) — **free** |
| `distance` | importance-weighted kNN distance to the training set (density) | tiny (fit at runtime) |
| `embedding` | PCA reconstruction error — linear stand-in for an autoencoder | tiny (a full AE/GNN is the non-linear/graph version) |

## Method

Test set = an in-distribution sample of the training set + the hybrid's hard
examples (`surrogate_increment.csv`). For each config we compute the surrogate's
real error and each novelty score, then measure:

- **Spearman(score, error)** — does novelty rank with error?
- **ROC-AUC** for "error > 10%" — can it separate reliable from unreliable?
- **budget curve** — ratifying the top-k% most novel with the simulator, how much
  error is removed? (the hybrid's ratify-under-suspicion knob)

## Result (see `report.txt`, `novelty_scatter.png`, `budget_curve.png`)

All three work well; `distance` is marginally best, `leaf` is nearly as good and
free.

| detector | Spearman | ROC-AUC | err removed @top10% / @top20% |
|---|---|---|---|
| leaf | 0.879 | 0.915 | 48% / 51% |
| distance | 0.885 | 0.945 | 29% / 58% |
| embedding | 0.745 | 0.939 | 48% / 51% |

Ratifying just the ~30% most-novel configs removes essentially all of the
surrogate error (vs the random diagonal). **Takeaway:** ship `leaf` (free) or
`distance` (best); the learned `embedding` adds nothing for these 25 flat
features — it would earn its keep only on the cloud topology graph (a GNN).

## Regenerate

```bash
python repro/validate_novelty.py \
    --dataset <surrogate_dataset.parquet> \
    --increment repro/surrogate_increment.csv \
    --models-dir lib/surrogate --out repro/novelty_validation
```

`testset.csv` is frozen here; delete it to rebuild from a fresh increment.
Needs the `surrogate-export` env (lightgbm, scikit-learn, scipy, matplotlib).
