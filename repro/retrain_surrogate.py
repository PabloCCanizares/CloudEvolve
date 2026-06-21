#!/usr/bin/env python3
"""Re-train the cloud surrogate, emphasising the hard examples the hybrid found.

The HybridPlatform logs every real-simulator evaluation it makes into an
increment CSV (surrogate_increment.csv): the 25 features plus the real
energy_kwh / sim_time_sec. Because the hybrid spends its real budget where the
search converges (the region the surrogate is weakest), that increment is, by
construction, a set of hard/under-covered examples.

This script merges the original training set with the increment, optionally
up-weights the increment rows by how badly the *current* model predicts them
(active learning), re-trains the two LightGBM models, and re-exports them in the
native text format the Java side loads (plus .joblib and feature spec).

Usage:
  python retrain_surrogate.py \
      --dataset /path/surrogate_dataset.parquet \
      --increment surrogate_increment.csv \
      --models-dir ../lib/surrogate \
      --out ../lib/surrogate \
      --emphasize 3.0

Requires: lightgbm, scikit-learn, pandas, pyarrow, joblib (e.g. the
`surrogate-export` conda env).
"""
import argparse
import json
import os
import sys

import numpy as np
import pandas as pd


def load_spec(models_dir, dataset_dir):
    for cand in (os.path.join(models_dir, "surrogate_feature_spec.json"),
                 os.path.join(dataset_dir, "surrogate_feature_spec.json")):
        if os.path.exists(cand):
            with open(cand, encoding="utf-8") as f:
                return json.load(f)
    raise SystemExit("surrogate_feature_spec.json not found (looked in models dir and dataset dir)")


def prepare(df, feats, catc, catmap):
    X = df.reindex(columns=feats).copy()
    for c in catc:
        cats = catmap[c]
        s = X[c].astype("string")
        X[c] = pd.Categorical(s.where(s.isin(cats), "__UNK__"), categories=cats)
    num = [f for f in feats if f not in catc]
    for c in num:
        X[c] = pd.to_numeric(X[c], errors="coerce").replace([np.inf, -np.inf], np.nan).fillna(0.0)
    return X[feats]


def current_error(models_dir, target_file, X, y):
    """Abs error of the current model on (X, y), or None if it can't be loaded."""
    import lightgbm as lgb
    path = os.path.join(models_dir, target_file)
    if not os.path.exists(path):
        return None
    try:
        booster = lgb.Booster(model_file=path)
        return np.abs(booster.predict(X) - y)
    except Exception as e:  # noqa: BLE001
        print(f"  (could not score current model {target_file}: {e})")
        return None


def train_one(X, y, weights, params):
    import lightgbm as lgb
    model = lgb.LGBMRegressor(**params)
    model.fit(X, y, sample_weight=weights, categorical_feature="auto")
    return model


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dataset", required=True, help="original training set (.parquet)")
    ap.add_argument("--increment", default="surrogate_increment.csv", help="hybrid hard-examples CSV")
    ap.add_argument("--models-dir", default="../lib/surrogate", help="current models (for emphasis scoring + spec)")
    ap.add_argument("--out", default="../lib/surrogate", help="where to write the retrained models")
    ap.add_argument("--emphasize", type=float, default=3.0,
                    help="max extra weight for the worst increment rows (1 = uniform)")
    args = ap.parse_args()

    spec = load_spec(args.models_dir, os.path.dirname(os.path.abspath(args.dataset)))
    feats, catc, catmap = spec["features"], spec["categorical_feature"], spec["categories"]
    targets = spec["targets"]  # [energy_kwh, sim_time_sec]

    base = pd.read_parquet(args.dataset)
    if "is_valid" in base.columns:
        base = base[base["is_valid"] == 1]
    base = base.dropna(subset=targets)
    print(f"original training rows: {len(base)}")

    if os.path.exists(args.increment) and os.path.getsize(args.increment) > 0:
        inc = pd.read_csv(args.increment)
        inc = inc.dropna(subset=targets)
        # Drop failed simulator runs (energy/time = -1 sentinel, or non-positive).
        before = len(inc)
        inc = inc[(inc[targets] > 0).all(axis=1)]
        dropped = before - len(inc)
        print(f"increment (hard) rows : {len(inc)}" + (f"  ({dropped} invalid dropped)" if dropped else ""))
    else:
        inc = pd.DataFrame(columns=feats + targets)
        print("increment: none found — retraining on the original set only")

    Xb = prepare(base, feats, catc, catmap)
    Xi = prepare(inc, feats, catc, catmap) if len(inc) else Xb.iloc[0:0]
    X = pd.concat([Xb, Xi], ignore_index=True)

    for tgt, mfile, jfile in [(targets[0], "surrogate_energy_lgbm.txt", "surrogate_energy_lgbm.joblib"),
                              (targets[1], "surrogate_time_lgbm.txt", "surrogate_time_lgbm.joblib")]:
        yb = base[tgt].astype(float).values
        yi = inc[tgt].astype(float).values if len(inc) else np.array([])
        y = np.concatenate([yb, yi])

        w = np.ones(len(X))
        if len(inc) and args.emphasize > 1.0:
            err = current_error(args.models_dir, mfile, Xi, yi)
            if err is not None and err.max() > 0:
                scaled = err / err.max()                      # 0..1, worst = 1
                w[len(Xb):] = 1.0 + (args.emphasize - 1.0) * scaled
                print(f"[{tgt}] emphasising {len(inc)} hard rows "
                      f"(weight {w[len(Xb):].min():.2f}..{w[len(Xb):].max():.2f}); "
                      f"current MAE on them = {err.mean():.3f}")

        params = dict(n_estimators=1500, learning_rate=0.03, num_leaves=64,
                      subsample=0.8, colsample_bytree=0.8, random_state=42, n_jobs=-1)
        if tgt == targets[0]:
            params.update(reg_alpha=1e-3, reg_lambda=1e-2)

        model = train_one(X, y, w, params)
        os.makedirs(args.out, exist_ok=True)
        model.booster_.save_model(os.path.join(args.out, mfile))
        try:
            from joblib import dump
            dump(model, os.path.join(args.out, jfile))
        except Exception:  # noqa: BLE001
            pass
        print(f"[{tgt}] retrained on {len(X)} rows -> {os.path.join(args.out, mfile)}")

    with open(os.path.join(args.out, "surrogate_feature_spec.json"), "w", encoding="utf-8") as f:
        json.dump(spec, f, ensure_ascii=False, indent=2)
    print("done. Re-run the comparison to see the effect.")


if __name__ == "__main__":
    sys.exit(main())
