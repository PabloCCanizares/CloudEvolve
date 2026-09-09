# Paper 3 — Simulation Allocation by Decision Utility

**Status:** CONDITIONAL GO  
**Type:** algorithm / model-management paper  
**Working title:** *Selective Simulation by Environmental-Selection Risk in Surrogate-Assisted Multiobjective Evolution*

Alternative title: *When Should the Simulator Override the Surrogate? Decision-Utility-Aware Evaluation in Multiobjective Evolution*.

## 1. Scientific thesis

The paper asks how to allocate a limited exact-simulator budget when surrogate predictions are used inside a multiobjective evolutionary optimizer.

The central idea is not to simulate points merely because they are statistically unusual or uncertain. The exact simulator should be used when trusting the surrogate is likely to change a **high-value optimizer decision**.

The intended thesis is:

> **Exact simulator evaluations should be allocated according to the expected utility of preventing harmful environmental-selection decisions, rather than according to OOD or predictive uncertainty alone.**

This paper is **conditional** because the relevant harmful decision event must first be identified empirically by Paper 1.

## 2. Dependency on Paper 1

Do not design the final `decision risk` score before the Selection Fidelity study answers:

- which selection errors occur;
- which are harmless;
- which produce measurable one-step regret;
- which propagate into trajectory damage;
- which observable signals predict those harmful errors.

Paper 1 provides the target; Paper 3 proposes a policy that acts on that target.

Without this dependency, Paper 3 risks becoming another heuristic infill criterion.

## 3. Why OOD is not the target

Distinguish:

\[
OOD(x)
\]

from:

\[
PredictionFailure(x)
\]

from:

\[
DecisionFailure(x).
\]

A candidate can be structurally/statistically OOD but easy to rank and harmless to trust.

Conversely, an apparently well-supported candidate can be decision-critical if a small objective error changes:

- non-dominated rank;
- partial-front ordering;
- crowding;
- survival;
- a Pareto extreme.

Therefore the desired quantity is closer to:

\[
P(\text{harmful optimizer decision if surrogate is trusted}\mid\mathcal I_t).
\]

## 4. Candidate decision targets

Do not freeze the final target until Paper 1 is complete.

Two candidate labels are:

### Selection-disagreement target

\[
E_{sel}=\mathbf 1[S_{\hat F}(C_t)\neq S_F(C_t)].
\]

This detects any different survivor set, but may over-penalize harmless alternatives.

### Harmful-selection target

Use a regret threshold:

\[
Y_{harm}=\mathbf 1[R_{sel}>\epsilon].
\]

where:

\[
R_{sel}=HV(F(S_F))-HV(F(S_{\hat F})).
\]

A future continuous target could estimate expected regret directly:

\[
\mathbb E[R_{sel}\mid\mathcal I_t].
\]

## 5. Connection with selective prediction

Selective prediction formalizes:

```text
predict
or
abstain
```

CloudEvolve has the analogous decision:

```text
use surrogate
or
execute simulator
```

This motivates a decision-risk / simulator-coverage view.

Let coverage represent the fraction of candidate decisions accepted without exact simulation. The analogue of selective risk is the rate or expected regret of harmful optimizer decisions among accepted surrogate evaluations.

Potential reporting:

\[
\text{selection risk}
\quad vs.\quad
\text{simulator coverage}.
\]

The selective-prediction analogy is a foundation, not by itself a novelty claim.

## 6. Main research questions

### RQ1 — Predictability

**Can harmful environmental-selection errors be predicted before exact simulation?**

### RQ2 — Incremental information

**Does decision-risk prediction identify harmful cases better than OOD, predictive uncertainty and dominance ambiguity?**

### RQ3 — Online value

**Does decision-aware selective simulation improve validated Pareto quality per exact simulator call under equal budgets?**

### RQ4 — Strong baselines

**Does the advantage remain against Pareto-/dominance-aware infill and contemporary model-management policies?**

### RQ5 — Retraining interaction

**Do decision-utility queries produce a more useful retraining stream than queries selected by generic uncertainty or novelty?**

## 7. Information available before simulation

Candidate features for a risk estimator may include:

- surrogate ensemble mean;
- predictive disagreement;
- ranking uncertainty;
- dominance uncertainty;
- predicted non-dominated rank;
- crowding margin / truncation status;
- distance to decision boundary between survive/discard;
- predicted HVI/Pareto relevance;
- support/OOD score;
- workload/episode/generation context;
- local fidelity diagnostics learned from previous audited batches.

The paper should determine which signals add real incremental value rather than combine all signals by default.

## 8. Baselines

At minimum:

1. random exact evaluation;
2. periodic evaluation;
3. current OOD/novelty policy;
4. predictive uncertainty;
5. dominance/ranking uncertainty;
6. predicted Pareto/HVI relevance;
7. a strong contemporary infill/model-management baseline;
8. proposed decision-utility policy.

All online comparisons must use:

- equal exact-simulation budgets;
- matched seeds;
- identical population/operators;
- consistent duplicate handling;
- identical accounting of invalid simulations;
- validated objectives for final comparison.

## 9. Experiment A — Offline audited-checkpoint prediction

Use the audited checkpoint dataset created for Paper 1.

For each candidate/checkpoint, learn or compute predictors for:

- any selection disagreement;
- false elimination;
- high one-step regret;
- possibly downstream trajectory damage for the smaller subset with branching evidence.

Evaluate:

- AUROC/AUPRC where appropriate;
- calibration;
- precision at fixed simulator budget;
- risk–coverage curve;
- expected prevented regret.

This is only a **pilot**. It is not enough for the final paper because an online policy changes future candidate distributions and retraining data.

## 10. Experiment B — Online matched-budget campaign

Run the full SAEA with each simulation-allocation policy.

Primary endpoint:

\[
HV_{validated}\quad\text{as a function of exact simulator calls}.
\]

Report:

- final validated HV;
- AUC of HV/call trajectory;
- Calls@HV;
- extremes;
- harmful selection errors prevented;
- one-step regret prevented;
- wall-clock overhead;
- retraining-data characteristics.

## 11. Relation to XAI

Paper 3 is **not an XAI paper**.

The core policy should work without SHAP or other explanations. If Failure-XAI later helps explain or target the learned decision-risk model, that belongs to the XAI extension paper unless the contribution is essential and independently justified.

This separation avoids mixing two scientific questions:

- which cases are worth simulating? (Paper 3)
- whether explanations add incremental value for acquisition/repair? (XAI extension)

## 12. Relation to the current CloudEvolve/XAI manuscript

The current manuscript already contains novelty-triggered/selective ratification and episodic retraining. Therefore this paper is independent only if it introduces and validates a genuinely new allocation target:

\[
\boxed{
\text{expected utility of preventing harmful environmental-selection errors}
}
\]

A new threshold or uncertainty score over the existing policy is insufficient.

## 13. Novelty boundary

Do not claim novelty for:

- selective exact evaluation;
- uncertainty-based infill;
- Pareto/HVI-based acquisition;
- model-management strategies;
- dominance-aware sampling;
- simulator-vs-surrogate dispatch.

The intended novelty must be the explicit mapping:

\[
\text{surrogate signals}
\rightarrow
\text{predicted environmental-selection harm}
\rightarrow
\text{selective simulator deferral}.
\]

## 14. Positive, negative and killing results

### Positive

Decision utility predicts high-regret decisions better than standard baselines and yields higher validated HV/call online.

### Useful negative

A simple dominance-uncertainty or crowding-margin signal explains nearly all decision risk. This may still be a useful result, but it weakens the need for a complex new method and may turn Paper 3 into a smaller empirical study.

### Kill result

If OOD/uncertainty/Pareto-aware baselines match the proposed policy under equal online budgets, do not force an independent Paper 3.

## 15. Go / merge / drop

### GO

Only if decision utility provides incremental predictive and online optimization value.

### MERGE

If decision risk is essentially reducible to a simple signal already studied in Paper 1 or to standard uncertainty/dominance ambiguity.

### DROP

If it does not improve the real quality/cost frontier.

## 16. Venue strategy

- **Best fit if algorithmic:** *Swarm and Evolutionary Computation*;
- **Best fit if simulation-allocation centric:** *Simulation Modelling Practice and Theory*;
- **Safer:** *Applied Soft Computing* / *Expert Systems with Applications*;
- **Stretch:** *IEEE Transactions on Evolutionary Computation* only with a general algorithm and external benchmarks.

## 17. Dependency in the research roadmap

```text
PAPER 1 audited decisions
          |
          v
 identify harmful decision target
          |
          v
 offline risk/utility prediction
          |
  incremental vs baselines?
       /          \
     yes           no
      |             |
      v             v
 online matched    DROP/MERGE
 budget campaign
      |
      v
    PAPER 3
```
