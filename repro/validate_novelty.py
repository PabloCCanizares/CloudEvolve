#!/usr/bin/env python3
"""Validate out-of-distribution / novelty detectors for the cloud surrogate.

A novelty detector is only useful if a high score predicts where the surrogate is
wrong. This script measures exactly that for three detectors:

  - leaf       : LightGBM leaf support — a config landing in low-count leaves is
                 in a sparse region (model-based uncertainty; free from the model).
  - distance   : importance-weighted kNN distance to the training set (density).
  - embedding  : PCA reconstruction error — the linear stand-in for an
                 autoencoder embedding (a full AE/GNN is the non-linear/graph
                 version; this keeps the validation dependency-light).

For a held-out test set (in-distribution rows sampled from the training set +
the hybrid's hard examples), it computes each detector's score and the surrogate's
real error, then reports:
  - Spearman(score, error)         — does novelty rank with error?
  - ROC-AUC for "error > T%"       — can it separate reliable from unreliable?
  - budget curve                   — ratifying the top-k% most novel, how much
                                     error is removed?  (the hybrid policy knob)

It saves testset.csv (frozen), report.txt and two figures so the finding is kept.

Usage:
  python validate_novelty.py --dataset <surrogate_dataset.parquet> \
      --increment <surrogate_increment.csv> --models-dir ../lib/surrogate \
      --out novelty_validation [--bad-threshold 10] [--insample 500]
"""
import argparse
import json
import os

import numpy as np
import pandas as pd


def prepare_numeric(df, feats, catc):
    """Feature matrix as floats (categoricals -> codes), for the distance/PCA detectors."""
    X = df.reindex(columns=feats).copy()
    for c in feats:
        if c in catc:
            X[c] = X[c].astype("string").astype("category").cat.codes.astype(float)
        else:
            X[c] = pd.to_numeric(X[c], errors="coerce").replace([np.inf, -np.inf], np.nan).fillna(0.0)
    return X.values.astype(float)


def prepare_for_model(df, feats, catc, catmap):
    """DataFrame with category dtype so the LightGBM booster predicts correctly."""
    X = df.reindex(columns=feats).copy()
    for c in catc:
        cats = catmap[c]
        s = X[c].astype("string")
        X[c] = pd.Categorical(s.where(s.isin(cats), "__UNK__"), categories=cats)
    for c in [f for f in feats if f not in catc]:
        X[c] = pd.to_numeric(X[c], errors="coerce").replace([np.inf, -np.inf], np.nan).fillna(0.0)
    return X[feats]


def leaf_support_score(booster, Xmodel):
    """mean over trees of 1/(leaf_count+1): high when the config lands in sparse leaves."""
    dumped = booster.dump_model()
    counts = []
    for tree in dumped["tree_info"]:
        m = {}
        stack = [tree["tree_structure"]]
        while stack:
            n = stack.pop()
            if "leaf_index" in n:
                m[n["leaf_index"]] = n.get("leaf_count", 0)
            else:
                stack.append(n["left_child"])
                stack.append(n["right_child"])
        counts.append(m)
    leaves = booster.predict(Xmodel, pred_leaf=True)  # (n_samples, n_trees)
    n, t = leaves.shape
    out = np.zeros(n)
    for i in range(n):
        out[i] = np.mean([1.0 / (counts[j].get(int(leaves[i, j]), 0) + 1.0) for j in range(t)])
    return out


def budget_curve(score, error):
    """Fraction of total error removed by ratifying the top-k% most-novel configs."""
    order = np.argsort(-score)            # most novel first
    cum = np.cumsum(error[order])
    total = error.sum()
    frac_ratified = np.arange(1, len(error) + 1) / len(error)
    frac_error_removed = cum / total if total > 0 else cum * 0
    return frac_ratified, frac_error_removed


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dataset", required=True)
    ap.add_argument("--increment", default="surrogate_increment.csv")
    ap.add_argument("--models-dir", default="../lib/surrogate")
    ap.add_argument("--out", default="novelty_validation")
    ap.add_argument("--bad-threshold", type=float, default=10.0, help="error%% above which a prediction is 'bad'")
    ap.add_argument("--insample", type=int, default=500, help="in-distribution rows sampled from the training set")
    ap.add_argument("--target", default="energy_kwh")
    args = ap.parse_args()

    import lightgbm as lgb
    from sklearn.decomposition import PCA
    from sklearn.neighbors import NearestNeighbors
    from sklearn.preprocessing import StandardScaler
    from sklearn.metrics import roc_auc_score
    from scipy.stats import spearmanr
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt

    os.makedirs(args.out, exist_ok=True)
    spec = json.load(open(os.path.join(args.models_dir, "surrogate_feature_spec.json"), encoding="utf-8"))
    feats, catc, catmap = spec["features"], spec["categorical_feature"], spec["categories"]
    target = args.target

    base = pd.read_parquet(args.dataset)
    if "is_valid" in base.columns:
        base = base[base["is_valid"] == 1]
    base = base.dropna(subset=[target]).reset_index(drop=True)

    # ---- build (and freeze) the test set: in-distribution sample + hard examples ----
    testset_path = os.path.join(args.out, "testset.csv")
    if os.path.exists(testset_path):
        test = pd.read_csv(testset_path)
        print(f"using frozen test set: {testset_path} ({len(test)} rows)")
    else:
        ind = base.sample(n=min(args.insample, len(base)), random_state=7).copy()
        ind["source"] = "in_dist"
        parts = [ind[feats + [target, "source"]]]
        if os.path.exists(args.increment) and os.path.getsize(args.increment) > 0:
            inc = pd.read_csv(args.increment).dropna(subset=[target]).copy()
            inc["source"] = "hard"
            parts.append(inc[feats + [target, "source"]])
        test = pd.concat(parts, ignore_index=True)
        test.to_csv(testset_path, index=False)
        print(f"froze test set -> {testset_path} ({len(test)} rows: "
              f"{(test['source']=='in_dist').sum()} in-dist + {(test['source']=='hard').sum()} hard)")

    # ---- surrogate error on the test set ----
    booster = lgb.Booster(model_file=os.path.join(args.models_dir, f"surrogate_{'energy' if target=='energy_kwh' else 'time'}_lgbm.txt"))
    Xmodel_test = prepare_for_model(test, feats, catc, catmap)
    pred = booster.predict(Xmodel_test)
    real = test[target].astype(float).values
    error = np.abs(pred - real) / np.where(real != 0, np.abs(real), 1.0) * 100.0  # APE %

    # ---- detectors: fit on the training distribution, score the test set ----
    Xtr = prepare_numeric(base, feats, catc)
    Xte = prepare_numeric(test, feats, catc)
    keep = Xtr.std(axis=0) > 1e-9            # drop constant features (singular otherwise)
    Xtr, Xte = Xtr[:, keep], Xte[:, keep]
    scaler = StandardScaler().fit(Xtr)
    Ztr, Zte = scaler.transform(Xtr), scaler.transform(Xte)

    imp = booster.feature_importance(importance_type="gain").astype(float)[keep]
    w = np.sqrt(imp / imp.sum()) if imp.sum() > 0 else np.ones(Ztr.shape[1])

    scores = {}
    scores["leaf"] = leaf_support_score(booster, Xmodel_test)
    nn = NearestNeighbors(n_neighbors=10).fit(Ztr * w)
    scores["distance"] = nn.kneighbors(Zte * w)[0].mean(axis=1)
    pca = PCA(n_components=0.95, svd_solver="full").fit(Ztr)
    recon = pca.inverse_transform(pca.transform(Zte))
    scores["embedding"] = np.sqrt(((Zte - recon) ** 2).sum(axis=1))

    # ---- metrics ----
    bad = (error > args.bad_threshold).astype(int)
    lines = []
    lines.append(f"Novelty-detector validation  (target={target}, n={len(test)}, "
                 f"bad = error>{args.bad_threshold:.0f}% -> {bad.sum()} of {len(bad)})")
    lines.append(f"surrogate error on test set: mean {error.mean():.2f}%  median {np.median(error):.2f}%")
    lines.append("")
    lines.append(f"{'detector':<10} {'Spearman':>9} {'ROC-AUC':>8} {'err@top10%':>10} {'err@top20%':>10}")
    fr, _ = budget_curve(scores["leaf"], error)
    for name in ["leaf", "distance", "embedding"]:
        s = scores[name]
        rho = spearmanr(s, error).correlation
        auc = roc_auc_score(bad, s) if 0 < bad.sum() < len(bad) else float("nan")
        _, removed = budget_curve(s, error)
        at10 = removed[int(0.10 * len(removed)) - 1] * 100
        at20 = removed[int(0.20 * len(removed)) - 1] * 100
        lines.append(f"{name:<10} {rho:>9.3f} {auc:>8.3f} {at10:>9.1f}% {at20:>9.1f}%")
    report = "\n".join(lines)
    print("\n" + report + "\n")
    with open(os.path.join(args.out, "report.txt"), "w", encoding="utf-8") as f:
        f.write(report + "\n")

    # ---- figure 1: novelty vs error scatter ----
    fig, axs = plt.subplots(1, 3, figsize=(15, 4.2))
    for ax, name in zip(axs, ["leaf", "distance", "embedding"]):
        ax.scatter(scores[name], error, s=12, alpha=0.4)
        ax.set_title(f"{name}  (Spearman {spearmanr(scores[name], error).correlation:.2f})")
        ax.set_xlabel("novelty score")
        ax.set_ylabel("surrogate error (%)")
        ax.grid(alpha=0.3)
    fig.suptitle("Novelty score vs surrogate error")
    fig.tight_layout()
    fig.savefig(os.path.join(args.out, "novelty_scatter.png"), dpi=110)

    # ---- figure 2: budget curve ----
    plt.figure(figsize=(7, 5))
    for name in ["leaf", "distance", "embedding"]:
        fr, removed = budget_curve(scores[name], error)
        plt.plot(fr * 100, removed * 100, lw=2, label=name)
    plt.plot([0, 100], [0, 100], "k--", lw=1, label="random")
    plt.xlabel("% configs ratified with the simulator (most novel first)")
    plt.ylabel("% of total surrogate error removed")
    plt.title("Budget curve: ratify-under-suspicion")
    plt.grid(alpha=0.3)
    plt.legend()
    plt.tight_layout()
    plt.savefig(os.path.join(args.out, "budget_curve.png"), dpi=110)

    print(f"saved: {testset_path}, report.txt, novelty_scatter.png, budget_curve.png  (in {args.out})")


if __name__ == "__main__":
    main()
