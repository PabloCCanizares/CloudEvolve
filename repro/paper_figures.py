#!/usr/bin/env python3
"""Paper-style comparison of the surrogate against an all-real execution.

Produces the two figures typically reported for a multi-objective study:

  1. hypervolume vs generation (convergence)
  2. the final Pareto front in objective space (energy x time)

The surrogate run's final front is re-evaluated with the real simulator
(AuditFront), so the Pareto figure compares the surrogate's solutions on their
*true* objectives — the honest "is the surrogate-guided search as good?" question.
Energy is the GA's fitness energy (raw total x simtime/3600), as the algorithm
optimises it.

Usage:
  python paper_figures.py --real <realRunDir> --surrogate <surrRunDir> \
      --launcher repro/CloudEvolve-launcher.jar --sim repro/cloudsimStorage.jar \
      --workspace repro --out repro/paper_figures
"""
import argparse
import glob
import os
import re
import subprocess

import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

ID = re.compile(r"output_(\d+)\.tc$")
ITER = re.compile(r"^\s*(\d+)\s*-\s*\[([^\]]+)\]\s*$")


def points(run):
    """id -> (fitness_energy_kWh, time_s) from every output_*.tc under the run."""
    out = {}
    for f in glob.glob(os.path.join(run, "*", "TcOutput", "output_*.tc")):
        m = ID.search(os.path.basename(f))
        if not m:
            continue
        e = t = None
        for line in open(f, errors="ignore"):
            s = line.strip().lower()
            if s.startswith("total energy consumption (cpu+storage):"):
                e = num(line)
            elif s.startswith("total simulation time:"):
                t = num(line)
        if e is not None and t is not None and e > 0 and t > 0:
            out[int(m.group(1))] = (e * t / 3600.0, t)   # fitness energy x time
    return out


def num(line):
    m = re.search(r"([0-9]+(?:[.,][0-9]+)?)", line.split(":", 1)[1])
    return float(m.group(1).replace(",", ".")) if m else None


def generations(run):
    gens = []
    for line in open(os.path.join(run, "iterationlist.txt"), errors="ignore"):
        m = ITER.match(line)
        if m:
            ids = [int(x) for x in m.group(2).split() if x.lstrip("-").isdigit()]
            gens.append((int(m.group(1)), ids))
    return sorted(gens)


def non_dominated(pts):
    nd = []
    for i, p in enumerate(pts):
        if not any(j != i and q[0] <= p[0] and q[1] <= p[1] and (q[0] < p[0] or q[1] < p[1])
                   for j, q in enumerate(pts)):
            nd.append(p)
    return nd


def hv2d(pts, ref):
    s = sorted([p for p in non_dominated(pts) if p[0] < ref[0] and p[1] < ref[1]])
    hv, prev_x = 0.0, None
    best_y = float("inf")
    sky = []
    for p in s:                       # decreasing-y skyline
        if p[1] < best_y:
            sky.append(p)
            best_y = p[1]
    for i, p in enumerate(sky):
        x_right = sky[i + 1][0] if i + 1 < len(sky) else ref[0]
        hv += (x_right - p[0]) * (ref[1] - p[1])
    return hv


def hv_curve(run, pts, ref):
    return [(g, hv2d([pts[i] for i in ids if i in pts], ref)) for g, ids in generations(run)]


def final_front(run, pts):
    last = generations(run)[-1][1]
    return non_dominated([pts[i] for i in last if i in pts])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--real", required=True)
    ap.add_argument("--surrogate", required=True)
    ap.add_argument("--launcher", default="repro/CloudEvolve-launcher.jar")
    ap.add_argument("--sim", default="repro/cloudsimStorage.jar")
    ap.add_argument("--workspace", default="repro")
    ap.add_argument("--out", default="repro/paper_figures")
    args = ap.parse_args()
    os.makedirs(args.out, exist_ok=True)

    real = points(args.real)
    surr = points(args.surrogate)                       # surrogate-predicted (pre-audit)
    allpts = list(real.values()) + list(surr.values())
    ref = (max(p[0] for p in allpts) * 1.1, max(p[1] for p in allpts) * 1.1)

    # ---- figure 1: hypervolume vs generation ----
    hr, hs = hv_curve(args.real, real, ref), hv_curve(args.surrogate, surr, ref)
    plt.figure(figsize=(7, 5))
    plt.plot([g for g, _ in hr], [v for _, v in hr], "-o", lw=2, color="#534AB7", label="real simulator")
    plt.plot([g for g, _ in hs], [v for _, v in hs], "-s", lw=2, color="#1D9E75",
             label="surrogate (predicted)")
    plt.xlabel("generation")
    plt.ylabel("hypervolume")
    plt.title("Hypervolume convergence: surrogate vs all-real")
    plt.grid(alpha=0.3)
    plt.legend()
    plt.tight_layout()
    plt.savefig(os.path.join(args.out, "hv_evolution.png"), dpi=120)

    # ---- re-evaluate the surrogate's final front with the real simulator ----
    subprocess.run(["java", "-cp", args.launcher, "main.java.AuditFront",
                    "-r", args.surrogate, "-S", args.sim, "-w", args.workspace,
                    "-c", os.path.join(args.out, "_audit.csv")], check=False)
    surr_audited = points(args.surrogate)               # final front now holds real objectives

    # ---- figure 2: final Pareto fronts ----
    fr = sorted(final_front(args.real, real))
    fs = sorted(final_front(args.surrogate, surr_audited))
    plt.figure(figsize=(7, 5))
    plt.plot([p[1] for p in fr], [p[0] for p in fr], "-o", color="#534AB7", label="real simulator")
    plt.plot([p[1] for p in fs], [p[0] for p in fs], "-s", color="#D85A30",
             label="surrogate (re-evaluated on the simulator)")
    plt.xlabel("simulation time (s)")
    plt.ylabel("energy (kWh)")
    plt.title("Final Pareto front: surrogate vs all-real")
    plt.grid(alpha=0.3)
    plt.legend()
    plt.tight_layout()
    plt.savefig(os.path.join(args.out, "pareto_front.png"), dpi=120)

    print(f"real  final HV={hr[-1][1]:.1f}  front size={len(fr)}")
    print(f"surr  final HV(pred)={hs[-1][1]:.1f}  audited front size={len(fs)}")
    print(f"saved: hv_evolution.png, pareto_front.png  (in {args.out})")


if __name__ == "__main__":
    main()
