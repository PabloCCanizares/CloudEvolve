#!/usr/bin/env python3
"""Pool-based active learning: refine the surrogate with the out-of-distribution
configurations an all-real run already labelled.

An all-real evolution is a pool of configs that come *with their true objectives*
— including the low-energy region the surrogate never covers. We score every pool
config with the novelty detector (leaf support), keep the out-of-distribution ones
(novelty above a threshold, optionally capped at top-N), add them to the training
set and re-train. No new simulation is needed — the real run already paid for the
labels; novelty is the acquisition function.

To prove it learns the target region honestly, the selected OOD configs are split
into a training part and a held-out part; we report the surrogate's energy MAPE on
the held-out OOD set (and on the fixed hard test set) before vs after.

Usage:
  python active_learning_pool.py --dataset <parquet> --pool <realRunDir> \
      --models-dir lib/surrogate --out /tmp/al_model --threshold <t> [--top-n N]
"""
import argparse
import glob
import json
import os
import re
import subprocess
import sys

import numpy as np
import pandas as pd

OUT_ID = re.compile(r"output_(\d+)\.tc$")


def num(line):
    m = re.search(r"([0-9]+(?:[.,][0-9]+)?)", line.split(":", 1)[1])
    return float(m.group(1).replace(",", ".")) if m else None


def read_tc(path):
    m = {}
    for line in open(path, errors="ignore"):
        line = line.strip()
        if "=" in line and not line.startswith("#"):
            k, v = line.split("=", 1)
            m[k.strip()] = v.strip()
    return m


def pool_from_run(run, feats):
    """Configs of an all-real run with their true (energy_kwh=cpu line, sim_time_sec)."""
    rows = []
    for inp in glob.glob(os.path.join(run, "*", "TcInput", "input_*.tc")):
        rid = re.search(r"input_(\d+)\.tc", inp).group(1)
        outp = os.path.join(os.path.dirname(os.path.dirname(inp)), "TcOutput", f"output_{rid}.tc")
        if not os.path.exists(outp):
            continue
        e = t = None
        for line in open(outp, errors="ignore"):
            s = line.strip().lower()
            if s.startswith("energy consumption:"):
                e = num(line)
            elif s.startswith("total simulation time:"):
                t = num(line)
        if e and t and e > 0 and t > 0:
            row = read_tc(inp)
            row["energy_kwh"] = e
            row["sim_time_sec"] = t
            rows.append(row)
    return pd.DataFrame(rows)


def model_frame(df, feats, catc, catmap):
    X = df.reindex(columns=feats).copy()
    for c in catc:
        s = X[c].astype("string")
        X[c] = pd.Categorical(s.where(s.isin(catmap[c]), "__UNK__"), categories=catmap[c])
    for c in [f for f in feats if f not in catc]:
        X[c] = pd.to_numeric(X[c], errors="coerce").fillna(0.0)
    return X[feats]


def leaf_novelty(booster, X):
    dumped = booster.dump_model()["tree_info"]
    maps = []
    for tr in dumped:
        m, st = {}, [tr["tree_structure"]]
        while st:
            n = st.pop()
            if "leaf_index" in n:
                m[n["leaf_index"]] = n.get("leaf_count", 0)
            else:
                st += [n["left_child"], n["right_child"]]
        maps.append(m)
    leaves = booster.predict(X, pred_leaf=True)
    return np.array([np.mean([1.0 / (maps[j].get(int(leaves[i, j]), 0) + 1.0) for j in range(leaves.shape[1])])
                     for i in range(leaves.shape[0])])


def mape(booster, X, y):
    p = booster.predict(X)
    return float(np.mean(np.abs(p - y) / np.where(y != 0, np.abs(y), 1) * 100))


def main():
    import lightgbm as lgb
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt

    ap = argparse.ArgumentParser()
    ap.add_argument("--dataset", required=True)
    ap.add_argument("--pool", required=True, help="an all-real run dir (labelled pool)")
    ap.add_argument("--models-dir", default="lib/surrogate")
    ap.add_argument("--out", default="/tmp/al_model")
    ap.add_argument("--testset", default="repro/novelty_validation/testset.csv")
    ap.add_argument("--threshold", type=float, default=None, help="novelty threshold (default: spec)")
    ap.add_argument("--top-n", type=int, default=0, help="cap selection to the N most novel (0 = no cap)")
    ap.add_argument("--holdout", type=float, default=0.3, help="fraction of selected OOD held out for testing")
    ap.add_argument("--emphasize", type=float, default=1.0)
    ap.add_argument("--figdir", default="repro/active_learning")
    args = ap.parse_args()
    os.makedirs(args.figdir, exist_ok=True)

    spec = json.load(open(os.path.join(args.models_dir, "surrogate_feature_spec.json"), encoding="utf-8"))
    feats, catc, catmap = spec["features"], spec["categorical_feature"], spec["categories"]
    thr = args.threshold if args.threshold is not None else spec.get("novelty_threshold", 0.0)

    energy_txt = os.path.join(args.models_dir, "surrogate_energy_lgbm.txt")
    booster = lgb.Booster(model_file=energy_txt)

    pool = pool_from_run(args.pool, feats)
    Xpool = model_frame(pool, feats, catc, catmap)
    nov = leaf_novelty(booster, Xpool)
    pool = pool.assign(_novelty=nov)

    sel = pool[pool["_novelty"] > thr].copy()
    if args.top_n and len(sel) > args.top_n:
        sel = sel.nlargest(args.top_n, "_novelty")
    print(f"pool={len(pool)}  selected OOD (novelty>{thr:.4f})={len(sel)}"
          + (f"  capped to {args.top_n}" if args.top_n else ""))
    if len(sel) < 5:
        print("too few OOD configs selected; lower the threshold.")
        return

    rng = np.random.RandomState(7)
    mask = rng.rand(len(sel)) < (1 - args.holdout)
    train_sel, hold_sel = sel[mask], sel[~mask]
    print(f"  -> {len(train_sel)} added to training, {len(hold_sel)} held out for the OOD test")

    inc = os.path.join(args.figdir, "selected_increment.csv")
    train_sel[feats + ["energy_kwh", "sim_time_sec"]].to_csv(inc, index=False)

    subprocess.run([sys.executable, os.path.join("repro", "retrain_surrogate.py"),
                    "--dataset", args.dataset, "--increment", inc,
                    "--models-dir", args.models_dir, "--out", args.out, "--emphasize", str(args.emphasize)],
                   check=False)

    new = lgb.Booster(model_file=os.path.join(args.out, "surrogate_energy_lgbm.txt"))
    Xhold, yhold = model_frame(hold_sel, feats, catc, catmap), hold_sel["energy_kwh"].astype(float).values
    test = pd.read_csv(args.testset)
    Xtest, ytest = model_frame(test, feats, catc, catmap), test["energy_kwh"].astype(float).values

    res = {
        "held-out OOD": (mape(booster, Xhold, yhold), mape(new, Xhold, yhold)),
        "hard test set": (mape(booster, Xtest, ytest), mape(new, Xtest, ytest)),
    }
    print(f"\n{'set':<14} {'MAPE before':>12} {'MAPE after':>12}")
    for k, (b, a) in res.items():
        print(f"{k:<14} {b:>11.2f}% {a:>11.2f}%")

    labels = list(res.keys())
    before = [res[k][0] for k in labels]
    after = [res[k][1] for k in labels]
    x = np.arange(len(labels))
    plt.figure(figsize=(7, 5))
    plt.bar(x - 0.2, before, 0.4, label="before (original surrogate)", color="#D85A30")
    plt.bar(x + 0.2, after, 0.4, label="after (pool active learning)", color="#1D9E75")
    plt.xticks(x, labels)
    plt.ylabel("energy MAPE (%)")
    plt.title(f"Pool active learning: {len(train_sel)} OOD configs added → surrogate accuracy")
    plt.grid(alpha=0.3, axis="y")
    plt.legend()
    plt.tight_layout()
    plt.savefig(os.path.join(args.figdir, "active_learning_mape.png"), dpi=120)
    print(f"saved: active_learning_mape.png  (in {args.figdir})")


if __name__ == "__main__":
    main()
