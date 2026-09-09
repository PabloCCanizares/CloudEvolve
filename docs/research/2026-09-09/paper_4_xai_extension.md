# Paper 4 — XAI Extension: Reliability-Gated Pareto-Aware Acquisition

**Status:** GO as an independent extension only if XAI shows incremental causal value  
**Type:** XAI / active surrogate-assisted optimization paper  
**Working title:** *Reliability-Gated Pareto-Aware XAI Acquisition for Surrogate-Assisted Multiobjective Evolution*

Alternative title: *When Should Explanations Guide Expensive Optimization? Reliability-Gated XAI for Multiobjective Surrogate Search*.

## 1. Is this paper already included in Papers 1–3?

**No.**

Papers 1–3 are deliberately non-XAI:

- **Paper 1** studies selection fidelity and trajectory consequences;
- **Paper 2** studies structural generalization and inductive bias;
- **Paper 3** studies simulator allocation by decision utility.

The XAI extension asks a different scientific question:

> **Does explanation-guided restriction of the search space add measurable value beyond equivalent non-XAI controls, and is that value conditional on the surrogate being regionally reliable?**

This paper is therefore added as a separate research line.

## 2. Relation to the current XAI-v1 manuscript

The current CloudEvolve/TFM manuscript already includes:

- XAI-guided active sampling;
- driver selection;
- single-objective incumbent-centered exploration;
- Cartesian grid sampling;
- novelty/ratification;
- episodic retraining;
- validated archives.

It also shows that the current XAI mechanism does **not** dominate matched uninformed alternatives consistently: in the current tabular results, XAI emphasizes the low-energy extreme but does not improve overall hypervolume relative to the random control, and the graph branch does not show a robust XAI advantage.

Therefore an independent XAI extension cannot simply be:

> “same method with more seeds”

or:

> “replace the grid by Sobol and call the result XAI-v2.”

The new paper must isolate the **incremental causal value of XAI**.

## 3. Core scientific thesis

The strongest defensible thesis is conditional:

> **XAI should be treated as a hypothesis about locally relevant manipulable dimensions, not as an acquisition utility. Explanation-guided subspaces are useful only when the underlying surrogate is regionally reliable and the explanations are stable; under those conditions they can improve validated Pareto search relative to matched uninformed subspaces.**

This yields three distinct ideas:

1. **local multiobjective XAI** instead of one global/single-objective explanation;
2. **explanation reliability gating** instead of trusting explanations everywhere;
3. **matched acquisition controls** so any gain can actually be attributed to XAI.

## 4. Architecture: XAI-v2 Core

The recommended core is:

```text
validated simulator archive
        |
        v
validated Pareto archive
        |
        v
Pareto anchors / local regions
(energy / time / knee)
        |
        +-------------------------+
        |                         |
        v                         v
bootstrap surrogate ensemble   regional fidelity
        |                     (rank/pair/dom/support)
        v                         |
local Objective-XAI               |
energy + time                     |
        |                         |
        +------------+------------+
                     |
                     v
          explanation reliability gate
                     |
                     v
       stable manipulable groups/subspace
                     |
                     v
             scrambled Sobol pool
          (LHS/random matched controls)
                     |
                     v
        repair / canonicalize / dedup / refill
                     |
                     v
same Pareto-aware acquisition for every arm
(HVI + epistemic U + dominance/ranking U + diversity)
                     |
                     v
               real simulator
                     |
                     v
             validated archive
                     |
                     v
                  retrain
```

The key causal principle is:

> **XAI defines a candidate subspace/hypothesis; the same acquisition mechanism ranks candidates in every experimental arm.**

Do not put raw SHAP magnitude directly into the first acquisition score, because that makes it difficult to separate the value of XAI from the value of the acquisition policy.

## 5. Pareto-local explanation regions

Use the validated real Pareto archive to define deterministic shared regions/anchors:

- low-energy extreme;
- low-time/latency extreme;
- knee / compromise region.

For each region, build a neighborhood using **ratified real observations**, not predicted-only points.

Compute explanations separately for:

- energy;
- time/latency.

The purpose is to answer:

> Which manipulable variable groups are locally relevant to the two objectives in different parts of the real Pareto front?

This avoids the current bias of explaining only a single low-energy incumbent.

## 6. Bootstrap ensemble and explanation stability

For the tabular branch, use a bootstrap LightGBM ensemble initially.

For each region/objective/group, compute explanation statistics across ensemble members:

- median/mean absolute SHAP importance;
- rank stability;
- top-k stability;
- sign stability only where interpretation makes sense;
- variability/disagreement.

Explanation stability is part of the **reliability decision**, not an objective to maximize directly.

## 7. Reliability gate

Start with an interpretable hard or tri-state gate rather than a complex continuous formula.

XAI is enabled in a region only if there is sufficient evidence that the surrogate is reliable enough there.

Candidate criteria include:

- minimum number of local real observations;
- lower confidence bound on regional ranking fidelity;
- pairwise/dominance accuracy threshold;
- minimum support depth / maximum OOD severity;
- explanation stability threshold.

Conceptually:

```text
if insufficient real evidence:
    XAI status = UNKNOWN / disabled
elif regional surrogate or explanation unreliable:
    XAI status = UNRELIABLE / disabled
else:
    XAI status = RELIABLE / enabled
```

Important principle:

> Unknown reliability is not equivalent to high reliability.

Continuous gating can be tested later, after the discrete gate is understood.

## 8. Candidate generation

The current fixed Cartesian grid should not be carried forward as the defining XAI mechanism.

Use a common candidate-generation interface:

- scrambled Sobol as default;
- maximin LHS as strong control;
- uniform random as baseline.

Pipeline:

```text
generate
→ map to legal domains
→ repair
→ canonicalize
→ deduplicate
→ refill
→ exact logical quota
```

A repaired duplicate should be regenerated/refilled so that matched arms execute the same intended logical quota when enough unique valid configurations exist.

Simulator-invalid executions consume cost and should not be selectively replaced unless a predeclared common rule is used in every arm.

## 9. Main research questions

### RQ1 — Local XAI value

**Do XAI-selected local manipulable groups improve validated Pareto gain compared with matched uninformed subspaces under the same sampler, candidate pool size, acquisition function and simulator budget?**

### RQ2 — Reliability interaction

**Is the effect of XAI larger in regions where the surrogate and explanations are regionally reliable?**

### RQ3 — Reliability gating

**Does disabling XAI in unreliable/unsupported regions improve validated outcomes relative to ungated XAI under distribution shift?**

### RQ4 — Multi-region behavior

**Does Pareto-local biobjective XAI improve coverage of energy, time and knee regions compared with the current single-objective incumbent-centered approach?**

### RQ5 — Interaction-aware extension

**Do stable feature interactions/groups add incremental value over main-effect local XAI?**

### Optional RQ6 — Failure-XAI

**If a decision/failure-risk model exists, does explaining its failures improve targeted surrogate repair beyond risk-only acquisition?**

This RQ is conditional and should not be required for the core paper.

## 10. Causal experimental controls

The most important design requirement is to isolate XAI from all other improvements.

### Core A/B/C pilot

Use the same:

- validated anchors;
- local domain bounds;
- number/type of groups;
- sampler;
- pool size;
- repair;
- acquisition;
- simulator budget;
- seeds.

Arms:

### A — Local XAI

Select local groups from SHAP for energy/time.

### B — Permuted-attribution control

Keep attribution magnitudes/distribution but permute the feature/group mapping within compatible strata.

Compatibility should preserve, where relevant:

- variable type;
- mutability;
- legal range/cardinality;
- constraints;
- group size.

This tests whether the **correct feature-to-importance mapping** matters.

### C — Non-XAI sensitivity control

Use a cheap local sensitivity method on the same surrogate and region, e.g. legal OAT/ALE-style sensitivity where appropriate.

This tests whether SHAP adds value beyond a simpler sensitivity mechanism.

## 11. Strong acquisition ablation after the pilot

If the A/B/C pilot shows an XAI signal, move to a stronger matched acquisition experiment.

Suggested arms:

- **U-only:** common Pareto/uncertainty/ranking acquisition without XAI subspace restriction;
- **X+U:** same acquisition inside XAI-selected subspaces;
- **GX+U:** same but with reliability gate;
- **XInt+U:** interaction-aware XAI groups if justified.

The acquisition itself should be identical across comparable arms, for example combining normalized:

\[
A(x|B)=
 w_H\widetilde{HVI}(x)
 +w_U\widetilde U(x)
 +w_R\widetilde U_{dom}(x)
 +w_D\widetilde D(x,B).
\]

Weights/mechanism must be predeclared or handled with a fair multi-criterion policy. XAI should not receive an extra raw score term in the core causal experiment.

## 12. Main hypotheses

### H1 — Local XAI incremental value

\[
XAI\text{-selected groups}
>
matched\ controls
\]

in validated Pareto gain at equal cost.

### H2 — Conditional value

\[
Effect(XAI\mid reliable)
>
Effect(XAI\mid unreliable).
\]

### H3 — Gate value

\[
Gated\ XAI
>
Ungated\ XAI
\]

under sufficient distribution shift / reliability variation.

### H4 — Failure-XAI, conditional

\[
Risk+FailureXAI
>
RiskOnly
\]

for repair of rank/dominance/selection fidelity per exact call.

## 13. Success criteria for keeping “XAI” in the title

The final paper should retain `XAI-guided` / `XAI acquisition` in its title only if at least one strong causal comparison succeeds, such as:

1. XAI subspace + same acquisition > matched random/permuted/sensitivity subspace + same acquisition;
2. gated XAI > ungated XAI under a predeclared reliability/shift analysis;
3. Failure-XAI > risk-only if that optional branch is included.

The following are **not sufficient**:

- XAI-v2 full system > XAI-v1, because many components changed;
- Sobol-XAI > old Cartesian-grid XAI;
- lower best energy without a justified multiobjective claim;
- better HV obtained with more real simulator calls;
- ensemble/acquisition improvements that occur equally without XAI.

If XAI adds little while uncertainty/ranking/trust-region mechanisms explain the gains, the honest paper should be reframed around reliability-aware active surrogate evolution and XAI should become diagnostic rather than the headline contribution.

## 14. Relationship to Papers 1–3

### Paper 1 — Selection Fidelity

Provides better regional decision-fidelity diagnostics and may improve the reliability gate. Paper 4 should not reuse Paper 1's main results as its own contribution.

### Paper 2 — Structural Generalization

Largely independent. XAI-v2 should be demonstrated tabular-first. Do not add a new graph-XAI architecture merely to broaden scope.

### Paper 3 — Decision Utility

Paper 3 asks **which cases deserve exact simulation**. Paper 4 asks **whether explanations identify useful local search/repair dimensions**.

The optional Failure-XAI branch can use a decision-risk model from Paper 3, but the XAI-v2 Core should not depend on Paper 3 being successful.

## 15. Minimum publishable experiment

- XAI-v1 frozen baseline;
- Pareto anchors: energy/time/knee;
- local biobjective SHAP;
- exact matched sampler/accounting;
- A/B/C causal pilot;
- validated HV and extremes;
- multiple seeds;
- regional fidelity + explanation stability diagnostics.

## 16. Strong experiment

- bootstrap ensemble;
- reliability gate;
- same Pareto-aware acquisition in XAI and non-XAI arms;
- U-only / X+U / GX+U / XInt+U ablation;
- multiple workload regimes;
- enough seeds for confirmatory statistics;
- optional Failure-XAI branch after risk modeling is mature.

## 17. Extensions explicitly postponed

Do not make the first paper depend on:

- adaptive trust-region expand/shrink/restart;
- Active Subspaces;
- conformal calibration;
- DPP;
- exact qNEHVI;
- bandit portfolios;
- a new GNN architecture;
- high-order SHAP interactions;
- raw SHAP as a direct acquisition utility.

These are follow-up experiments, not prerequisites for demonstrating XAI value.

## 18. Go / reframe / drop

### GO as XAI paper

If XAI-selected groups or reliability-gated XAI outperform truly matched non-XAI controls at equal cost with reproducible effect.

### REFRAME

If most gains come from ensemble uncertainty, Pareto acquisition, exact accounting or adaptive regions, with negligible incremental XAI effect.

Possible reframing:

> *Reliability-Aware Active Surrogate-Assisted Multiobjective Evolution under Optimizer-Induced Distribution Shift*.

### DROP as independent XAI extension

If XAI cannot beat matched random/permuted/sensitivity controls and the gate does not reveal a meaningful conditional effect.

## 19. Venue strategy

Depending on the final contribution:

- **Best fit:** *Information Sciences* / *Applied Soft Computing*;
- **Algorithmic/XAI-optimization fit:** *Swarm and Evolutionary Computation* if the method is sufficiently general;
- **Engineering AI fit:** *Engineering Applications of Artificial Intelligence*;
- **Stretch:** *IEEE Transactions on Evolutionary Computation* only with strong general validation and a clearly generalizable method.

Avoid relying on FGCS solely because the current CloudEvolve manuscript targets Cloud/Fog: an independent XAI extension should differentiate itself scientifically, not only by additional experiments in the same application.

## 20. Dependency in the research roadmap

```text
freeze XAI-v1 + correctness
          |
          v
matched accounting / shared sampler
          |
          v
Pareto-local XAI A/B/C pilot
          |
      XAI signal?
       /       \
     yes        no
      |          |
      v          v
ensemble +     reframe/drop
stability gate
      |
      v
U-only vs X+U vs GX+U
      |
      v
   PAPER 4
      |
      +--> optional interactions / Failure-XAI / adaptive regions later
```
