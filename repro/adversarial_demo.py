#!/usr/bin/env python3
"""Show the adversarial loop improving the surrogate round after round.

Each round runs a HYBRID evolution (surrogate + novelty-triggered ratification),
which explores the surrogate's blind spots and harvests their true labels, then
re-trains the surrogate on the accumulated hard examples. After every round we
measure two things that should improve:

  * accuracy  — the surrogate's energy MAPE on a fixed hard test set;
  * quality   — the true best energy (re-evaluated on the simulator) of the
                round's Pareto front, vs the all-real reference.

Produces improvement.png (two panels) and saves the per-round numbers.

Usage:
  python adversarial_demo.py --dataset <parquet> --reference <realRunDir> \
      --algo eNSGAII --config Al_w1 --iters 5 --rounds 4 --out repro/adversarial_demo
"""
import argparse
import glob
import os
import re
import shutil
import subprocess
import sys

import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

OUT_ID = re.compile(r"output_(\d+)\.tc$")
ITER = re.compile(r"^\s*(\d+)\s*-\s*\[([^\]]+)\]\s*$")


def points(run):
    out = {}
    for f in glob.glob(os.path.join(run, "*", "TcOutput", "output_*.tc")):
        m = OUT_ID.search(os.path.basename(f))
        if not m:
            continue
        e = t = None
        for line in open(f, errors="ignore"):
            s = line.strip().lower()
            if s.startswith("total energy consumption (cpu+storage):"):
                e = num(line)
            elif s.startswith("total simulation time:"):
                t = num(line)
        if e and t and e > 0 and t > 0:
            out[int(m.group(1))] = (e * t / 3600.0, t)
    return out


def num(line):
    m = re.search(r"([0-9]+(?:[.,][0-9]+)?)", line.split(":", 1)[1])
    return float(m.group(1).replace(",", ".")) if m else None


def last_gen_best(run):
    last = []
    for line in open(os.path.join(run, "iterationlist.txt"), errors="ignore"):
        m = ITER.match(line)
        if m:
            last = [int(x) for x in m.group(2).split() if x.lstrip("-").isdigit()]
    pts = points(run)
    vals = [pts[i][0] for i in last if i in pts]
    return min(vals) if vals else float("nan")


def find_run(out_dir):
    for f in glob.glob(os.path.join(out_dir, "**", "iterationlist.txt"), recursive=True):
        return os.path.dirname(f)
    return None


def energy_mape(model_dir, test, feats, catc, catmap):
    import lightgbm as lgb
    import pandas as pd
    b = lgb.Booster(model_file=os.path.join(model_dir, "surrogate_energy_lgbm.txt"))
    X = test.reindex(columns=feats).copy()
    for c in catc:
        s = X[c].astype("string")
        X[c] = pd.Categorical(s.where(s.isin(catmap[c]), "__UNK__"), categories=catmap[c])
    for c in [f for f in feats if f not in catc]:
        X[c] = pd.to_numeric(X[c], errors="coerce").fillna(0.0)
    pred = b.predict(X[feats])
    real = test["energy_kwh"].astype(float).values
    return float(np.mean(np.abs(pred - real) / np.where(real != 0, np.abs(real), 1) * 100))


def main():
    import json
    import pandas as pd

    ap = argparse.ArgumentParser()
    ap.add_argument("--dataset", required=True)
    ap.add_argument("--reference", required=True, help="an all-real run dir for the reference best")
    ap.add_argument("--testset", default="repro/novelty_validation/testset.csv")
    ap.add_argument("--algo", default="eNSGAII")
    ap.add_argument("--config", default="Al_w1")
    ap.add_argument("--iters", type=int, default=5)
    ap.add_argument("--rounds", type=int, default=4)
    ap.add_argument("--seed-models", default="lib/surrogate")
    ap.add_argument("--repro", default="repro")
    ap.add_argument("--out", default="repro/adversarial_demo")
    args = ap.parse_args()
    os.makedirs(args.out, exist_ok=True)

    spec = json.load(open(os.path.join(args.seed_models, "surrogate_feature_spec.json"), encoding="utf-8"))
    feats, catc, catmap = spec["features"], spec["categorical_feature"], spec["categories"]
    test = pd.read_csv(args.testset)

    best_real = last_gen_best(args.reference)
    launcher = os.path.join(args.repro, "CloudEvolve-launcher.jar")
    sim = os.path.join(args.repro, "cloudsimStorage.jar")

    # Absolute paths: the launcher cd's into repro/, so relative -o/-s/-c would break.
    work = os.path.abspath(os.path.join(args.out, "work"))
    models = os.path.join(work, "models")
    increment = os.path.join(work, "increment.csv")
    if os.path.exists(work):
        shutil.rmtree(work)
    os.makedirs(models)
    for f in glob.glob(os.path.join(args.seed_models, "*")):
        shutil.copy(f, models)

    mape, quality = [], []
    mape.append(energy_mape(models, test, feats, catc, catmap))     # round 0 = seed model
    for k in range(args.rounds):
        out_k = os.path.join(work, f"round_{k}")
        # 1) hybrid evolution: explore blind spots + harvest into the increment
        subprocess.run([os.path.join(args.repro, "launcherHybrid.sh"),
                        "-a", args.algo, "-n", args.config, "-i", str(args.iters),
                        "-s", models, "-o", out_k, "-c", increment,
                        "-P", "novelty,implausible", "-C", "10", "-g", "clamp"],
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=False)
        run = find_run(out_k)
        if run is None:
            print(f"round {k}: hybrid produced no run dir; stopping.")
            break
        # 2) re-evaluate the front on the simulator -> true best (quality) + more harvest
        subprocess.run(["java", "-cp", launcher, "main.java.AuditFront",
                        "-r", run, "-S", sim, "-w", args.repro, "-c", increment],
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=False)
        quality.append(last_gen_best(run))
        # 3) retrain on dataset + accumulated hard examples
        subprocess.run([sys.executable, os.path.join(args.repro, "retrain_surrogate.py"),
                        "--dataset", args.dataset, "--increment", increment,
                        "--models-dir", models, "--out", models, "--emphasize", "1.0"],
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=False)
        mape.append(energy_mape(models, test, feats, catc, catmap))
        print(f"round {k}: quality(true best energy)={quality[-1]:.2f} kWh   "
              f"accuracy(MAPE after retrain)={mape[-1]:.2f}%")

    # ---- figure ----
    fig, (a1, a2) = plt.subplots(1, 2, figsize=(13, 4.6))
    a1.plot(range(len(mape)), mape, "-o", color="#534AB7", lw=2)
    a1.set_xlabel("re-training round")
    a1.set_ylabel("surrogate energy MAPE (%)")
    a1.set_title("Accuracy improves each round")
    a1.grid(alpha=0.3)
    a1.set_xticks(range(len(mape)))

    a2.plot(range(1, len(quality) + 1), quality, "-s", color="#1D9E75", lw=2, label="surrogate (true best)")
    a2.axhline(best_real, ls="--", color="#534AB7", label=f"all-real best ({best_real:.1f})")
    a2.set_xlabel("round")
    a2.set_ylabel("true best energy found (kWh)")
    a2.set_title("Solution quality approaches all-real")
    a2.grid(alpha=0.3)
    a2.set_xticks(range(1, len(quality) + 1))
    a2.legend()
    fig.suptitle(f"Adversarial loop, round by round ({args.algo} {args.config})")
    fig.tight_layout()
    fig.savefig(os.path.join(args.out, "improvement.png"), dpi=120)

    with open(os.path.join(args.out, "rounds.txt"), "w") as fh:
        fh.write("round  MAPE(%)  true_best_energy(kWh)\n")
        for i, m in enumerate(mape):
            q = quality[i - 1] if 0 < i <= len(quality) else float("nan")
            fh.write(f"{i:5d}  {m:7.2f}  {q:8.2f}\n")
    print(f"all-real reference best = {best_real:.2f} kWh")
    print(f"saved: improvement.png, rounds.txt  (in {args.out})")


if __name__ == "__main__":
    main()
