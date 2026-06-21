# Pool-based active learning

`active_learning_pool.py` refines the surrogate using the configurations an
all-real run already labelled, without any new simulation.

Idea: an all-real evolution is a pool of configs that come **with their true
objectives**, including the low-energy region the surrogate never covers. Score
every pool config with the **novelty detector** (leaf support), keep the
out-of-distribution ones (novelty above the spec threshold, optionally capped at
top-N), add them to the training set and re-train. Novelty is the acquisition
function; the real run already paid for the labels.

To prove it honestly, the selected OOD configs are split into a training part and
a **held-out** part; the figure reports the surrogate's energy MAPE on the
held-out OOD set (and on the fixed hard test set) before vs after.

## Result (`active_learning_mape.png`)

Pool = a full-workload all-real NSGA-II/Al_w1 run (946 configs); 852 flagged OOD;
592 added to training, 260 held out.

| set | MAPE before | MAPE after |
|---|---|---|
| held-out OOD | 6.35% | **0.56%** |
| hard test set | 28.9% | 26.7% |

The surrogate learns the region it was blind to (≈11× lower error there). The
hard test set improves only modestly because it covers a different region — active
learning improves the surrogate exactly where you sample, which is the point.

## Regenerate

```bash
python repro/active_learning_pool.py --dataset <surrogate_dataset.parquet> \
    --pool <allRealRunDir> --models-dir lib/surrogate --out /tmp/al_model
# --threshold <t> (default: spec novelty_threshold) | --top-n N | --emphasize E
```
