# Paper 2 — Structural Generalization of Cloud/Fog Surrogates

**Status:** GO after provenance reconciliation  
**Type:** empirical systems / AI paper  
**Working title:** *Structural Generalization of Cloud/Fog Surrogates: Flat, Graph and Physics-Guided Models under Topology Shift*

Alternative title: *Beyond GraphSAGE: Which Inductive Biases Generalize across Unseen Cloud/Fog Topologies?*

## 1. Scientific thesis

The paper studies which model inductive biases preserve predictive and optimization-relevant fidelity when Cloud/Fog infrastructures move beyond the topology distribution observed during training.

The question is **not** whether GraphSAGE beats a flattened baseline in one split. Graph-size generalization is an established research area, and graph surrogates are not novel by themselves.

The stronger question is:

> **Which inductive biases — flat statistical, graph/compositional, or physics/analytical — preserve prediction and optimizer-decision quality under controlled structural shift?**

## 2. Why the current evidence is only motivating evidence

The existing CloudEvolve/Fog results are scientifically interesting because, under a larger-topology split, GraphSAGE has shown much stronger ordinal behavior than a flattened control even when absolute \(R^2\) degrades badly.

However, these historical results cannot yet be treated as one cumulative experimental chain because there are unresolved differences in:

- datasets;
- code versions;
- preprocessing;
- target definitions;
- topology splits;
- protocols;
- seeds.

The temporal target requires particular care:

\[
sim\_time\_sec
\quad\text{vs}\quad
loop\_latency\_ms.
\]

The new paper must therefore begin with a **provenance and reproducibility reconciliation**.

## 3. Mandatory provenance audit

Before any claim is carried into the new campaign, build a table with at least:

| Artifact | Dataset | Commit | Energy target | Time target | Split | Representation | Model | Seeds | Result |
|---|---|---|---|---|---|---|---|---|---|

The audit must answer:

1. Which dataset produced each result?
2. Which temporal target was used?
3. Which preprocessing pipeline was used?
4. Which topologies entered train/validation/test?
5. Which exact model/configuration produced the result?
6. Which commit and seed produced it?
7. Which results are reproducible today?

Until this is resolved, historical GraphSAGE numbers are **motivating evidence**, not the definitive baseline for the paper.

## 4. Main research questions

### RQ1 — Size generalization

**How do flat, graph-based and compositional/physics-guided surrogate families generalize from observed to larger unseen Cloud/Fog topologies?**

### RQ2 — Decision-relevant structural fidelity

**Does explicit structural representation preserve ranking, dominance and selection fidelity under topology shift even when absolute prediction accuracy deteriorates?**

### RQ3 — Failure modes

**Which forms of structural shift break each surrogate family?**

### RQ4 — Physics/compositional priors

**Can analytical or compositional priors improve extrapolation relative to learning the complete response function from data?**

### RQ5 — Downstream optimization

**Does better structural decision fidelity translate into better validated multiobjective optimization on unseen topology regimes?**

## 5. Model families

The paper should compare **inductive-bias families**, not merely model names.

### 5.1 Flat statistical models

At minimum:

- LightGBM;
- strong MLP baseline.

Potential later additions:

- XGBoost / Random Forest where useful.

### 5.2 Structural / compositional graph models

Pilot:

- GraphSAGE.

Full campaign only if the pilot justifies expansion:

- GAT;
- GCN or GIN;
- a simpler permutation-invariant compositional baseline if useful.

### 5.3 Physics / analytical hybrids

For latency/performance, use a defensible analytical or queueing/network baseline and learn the residual:

\[
\hat T(x)=T_{analytic}(x)+\Delta_\theta(G).
\]

For energy, use a component-wise/compositional baseline where justified:

\[
\hat E(x)=E_{component}(x)+\Delta_\theta(G).
\]

The exact analytical models must be justified from the simulator semantics. The objective is not to force a physics-guided winner, but to test whether prior structure improves extrapolation.

## 6. State-of-the-art anchors

The paper should explicitly position itself relative to:

- graph-size generalization literature;
- RouteNet-Fermi and other topology-aware network modeling;
- QT-RouteNet / analytical-plus-learned residual approaches;
- inductive graph regression;
- OOD/generalization in GNNs.

These references raise the baseline: `GraphSAGE vs flattened LightGBM` alone is not enough for a strong independent paper.

## 7. Structural shifts

Do not begin with too many dimensions of shift. The first campaign should use 2–3 orthogonal regimes.

### 7.1 Size shift

Train:

\[
|V|\le n.
\]

Test:

\[
|V|>n.
\]

### 7.2 Topology-family shift

Train and test contain structurally distinct families, with the split defined before training.

### 7.3 Resource/network regime shift

Examples:

- latency distribution;
- bandwidth regime;
- resource heterogeneity.

Later extensions may include:

- density;
- degree distribution;
- hierarchy depth;
- workload.

## 8. Metrics

### Predictive

- MAE;
- RMSE;
- MAPE;
- \(R^2\).

### Ordinal

- Spearman;
- Kendall;
- pairwise accuracy.

### Multiobjective decision fidelity

Where candidate pools are available:

- dominance fidelity;
- front-assignment fidelity;
- selection fidelity;
- one-step selection regret.

### Downstream

- validated hypervolume;
- extremes;
- Calls@HV;
- HV/call or AUC-HV under equal accounting.

## 9. Mechanistic structural analysis

A useful extension is to ask **why** a representation generalizes or fails.

Inspired by graph-size-generalization literature, measure whether the distribution of local structural neighborhoods/motifs changes between train and test.

The desired story is not:

> GraphSAGE won.

It is closer to:

> Structural models preserve decision-relevant relations when the local/compositional structure required by the target remains transferable; they fail when the structural shift changes those relations beyond the learned inductive bias.

A negative result can therefore remain scientifically valuable if failure is characterized reproducibly.

## 10. Pilot experiment

After provenance reconciliation, freeze one clean protocol:

- same dataset;
- same valid energy target;
- same valid temporal target;
- same train/test topology split;
- same preprocessing;
- same metrics.

Train only:

1. strong flat baseline;
2. GraphSAGE;
3. compositional/analytical or physics-guided baseline.

Do **not** add GAT/GCN/GIN until this pilot shows that structural inductive bias is worth expanding.

## 11. Strong experiment

If the pilot succeeds:

- multiple structural shifts;
- several structural models;
- repeated seeds;
- decision-fidelity analysis;
- downstream NSGA-II campaign on unseen topology regimes;
- local-structure/motif diagnostics;
- robust statistics.

## 12. Relation to the current CloudEvolve/XAI manuscript

The current manuscript already contains GraphSAGE and structural-holdout observations. The independent contribution of this paper must therefore be:

\[
\boxed{
\text{reconciled targets/provenance}
+
\text{controlled structural shifts}
+
\text{flat vs graph vs physics/compositional inductive biases}
+
\text{decision/downstream fidelity}
}
\]

Simply rerunning the existing GraphSAGE/flattened comparison with more seeds is not enough.

## 13. Expected outcomes that remain publishable

### Positive

Graph/compositional/physics models preserve materially more decision-relevant fidelity under structural shift and improve downstream validated optimization.

### Mixed

Graph models preserve ranking/selection but not absolute magnitudes. This can be a strong result if it is reproducible and explains optimization usefulness.

### Negative but useful

No structural family dominates universally, but failure can be predicted from the type/magnitude of structural shift.

### Kill result

Strong flat baselines match or beat structural models across all reconciled shifts and no generalizable structural failure/success pattern emerges.

## 14. Go / reformulate / drop

### GO

If at least one structural/compositional inductive bias provides reproducible value under structural OOD or the campaign reveals a clear generalizable failure boundary.

### REFORMULATE

If GraphSAGE itself is not superior, but the experiment identifies when graph structure helps or fails.

### DROP

If careful flat baselines eliminate the signal and no mechanistic structural finding remains.

## 15. Venue strategy

- **Best fit:** *Future Generation Computer Systems*;
- **Alternatives:** *Engineering Applications of Artificial Intelligence* / *Applied Soft Computing*;
- **Stretch depending on systems framing:** *IEEE Transactions on Cloud Computing*.

## 16. Dependency in the research roadmap

```text
Fog provenance + target reconciliation
                |
                v
 flat vs graph vs physics pilot
                |
        structural signal?
          /           \
        yes            no
         |              |
         v              v
 full structural    reformulate/drop
 benchmark
         |
         v
 downstream optimization
         |
         v
       PAPER 2
```
