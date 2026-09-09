# Revisión estratégica de papers de CloudEvolve

**Fecha:** 2026-09-09  
**Estado:** roadmap científico consolidado  
**Ámbito:** nuevas publicaciones derivadas de CloudEvolve, separadas del manuscrito actual siempre que exista una contribución científica independiente.

## 1. Conclusión ejecutiva

Después de revisar el estado del arte, el manuscrito actual, la evidencia experimental y los riesgos de solapamiento, la cartera no debe describirse como una colección de 6–8 papers independientes.

La estrategia realista es:

\[
\boxed{
2\ \text{papers con hipótesis fuertes}
+
1\ \text{paper algorítmico condicionado}
+
1\ \text{extensión XAI condicionada a valor causal}
}
\]

Los cuatro candidatos son:

1. **Selection Fidelity and Trajectory Consequences** — prioridad científica principal.
2. **Structural Generalization of Cloud/Fog Surrogates** — segunda prioridad, después de reconciliar la rama Fog.
3. **Simulation Allocation by Decision Utility** — condicionado a los resultados del Paper 1.
4. **Reliability-Gated Pareto-Aware XAI Acquisition** — extensión independiente de XAI-v2, condicionada a demostrar valor incremental frente a controles pareados.

El paper de extensión XAI **no estaba incluido** en los Papers 1–3 porque esos tres se diseñaron deliberadamente como líneas no-XAI. Se incorpora ahora como **Paper 4**.

---

# 2. Papers

## Paper 1 — Selection Fidelity and Trajectory Consequences

[Descripción completa del Paper 1](paper_1_selection_fidelity.md)

**Working title:** *From Prediction Fidelity to Selection Fidelity: How Surrogate Errors Alter Multiobjective Evolutionary Search*

### Pregunta principal

> ¿Qué errores del surrogate cambian realmente las decisiones de environmental selection de NSGA-II y cuáles de esos cambios perjudican la trayectoria posterior de búsqueda?

### Idea central

La unidad de fidelity debe desplazarse desde la predicción hacia la decisión real del optimizador:

\[
prediction
\neq
ranking
\neq
dominance
\neq
selection
\neq
trajectory.
\]

El paper debe utilizar counterfactual checkpoints y branching trajectories para medir:

- front-assignment fidelity;
- crowding fidelity;
- selection overlap;
- false eliminations;
- one-step selection regret;
- trajectory damage.

### Estado

**GO — prioridad 1.**

### Go/no-go crítico

Debe demostrarse que selection fidelity/regret aporta información real más allá de Spearman/dominance y que una parte de los errores de decisión tiene consecuencias downstream.

### Venue orientativo

- Best fit: *Swarm and Evolutionary Computation* / *Information Sciences*;
- Stretch: *IEEE Transactions on Evolutionary Computation*;
- Safe: *Applied Soft Computing*.

---

## Paper 2 — Structural Generalization of Cloud/Fog Surrogates

[Descripción completa del Paper 2](paper_2_structural_generalization.md)

**Working title:** *Structural Generalization of Cloud/Fog Surrogates: Flat, Graph and Physics-Guided Models under Topology Shift*

### Pregunta principal

> ¿Qué inductive biases —flat, graph/compositional o physics/analytical— conservan mejor predictive y decision fidelity cuando las topologías Cloud/Fog salen de la distribución observada en entrenamiento?

### Idea central

El paper no debe ser `GraphSAGE vs LightGBM`. Debe estudiar generalización estructural bajo splits controlados y baselines más fuertes:

- flat statistical models;
- graph/compositional models;
- physics/analytical residual models.

### Prerrequisito

Reconciliar completamente:

- datasets;
- commits;
- preprocessing;
- targets;
- splits;
- seeds;
- resultados históricos.

Especialmente:

\[
sim\_time\_sec
\quad vs.\quad
loop\_latency\_ms.
\]

### Estado

**GO tras reconciliación — prioridad 2.**

### Go/no-go crítico

Debe aparecer una ventaja estructural reproducible o una caracterización científica clara de cuándo cada inductive bias generaliza/falla.

### Venue orientativo

- Best fit: *Future Generation Computer Systems*;
- Alternatives: *Engineering Applications of Artificial Intelligence* / *Applied Soft Computing*;
- Stretch según framing: *IEEE Transactions on Cloud Computing*.

---

## Paper 3 — Simulation Allocation by Decision Utility

[Descripción completa del Paper 3](paper_3_decision_utility.md)

**Working title:** *Selective Simulation by Environmental-Selection Risk in Surrogate-Assisted Multiobjective Evolution*

### Pregunta principal

> ¿Qué evaluaciones exactas merece la pena comprar para evitar decisiones evolutivas perjudiciales?

### Idea central

No seleccionar simulaciones solo por:

- OOD;
- uncertainty;
- novelty;
- predicted Pareto relevance.

La nueva policy debería estimar directamente la utilidad de evitar errores de environmental selection, por ejemplo:

\[
P(R_{sel}>\epsilon\mid\mathcal I_t)
\]

u otra definición que Paper 1 demuestre que tiene significado downstream.

### Dependencia

**No diseñar el método final antes de Paper 1.**

Paper 1 debe identificar:

- qué decision errors importan;
- qué regret producen;
- qué señales pueden predecirlos.

### Estado

**CONDITIONAL GO — prioridad 3.**

### Go/no-go crítico

Debe superar, a igual coste real:

- OOD;
- predictive uncertainty;
- dominance/ranking uncertainty;
- Pareto/HVI relevance;
- strong contemporary infill/model-management baselines.

Si no existe valor incremental, el paper debe fusionarse o descartarse.

### Venue orientativo

- *Swarm and Evolutionary Computation* si el resultado es algorítmico;
- *Simulation Modelling Practice and Theory* si el foco es simulator allocation;
- *Applied Soft Computing* / *Expert Systems with Applications* como alternativas;
- TEVC solo con método general y validación externa fuerte.

---

## Paper 4 — XAI Extension: Reliability-Gated Pareto-Aware Acquisition

[Descripción completa del Paper 4 — XAI extension](paper_4_xai_extension.md)

**Working title:** *Reliability-Gated Pareto-Aware XAI Acquisition for Surrogate-Assisted Multiobjective Evolution*

### ¿Estaba incluido en Papers 1–3?

**No.**

Los Papers 1–3 son líneas no-XAI. El Paper 4 es la extensión científica del bloque XAI actual y debe demostrar algo distinto del manuscrito XAI-v1.

### Pregunta principal

> ¿Aporta XAI valor incremental cuando se utiliza como hipótesis local de subespacio/grupos manipulables, frente a controles no-XAI realmente pareados, y depende ese valor de la fiabilidad regional del surrogate y de la estabilidad de las explicaciones?

### Diferencia frente a XAI-v1

La extensión debe introducir una nueva unidad experimental:

- Pareto anchors: energía / tiempo / knee;
- Objective-XAI local biobjetivo;
- bootstrap ensembles;
- explanation stability;
- reliability gate;
- shared Sobol/LHS/random sampler;
- repair/dedup/refill con presupuesto exacto;
- **same acquisition in XAI and non-XAI arms**;
- causal matched controls.

XAI debe definir el subespacio/hypothesis. No debe recibir automáticamente un término de utility propio en la acquisition del experimento causal principal.

### Controles mínimos

- A: local XAI;
- B: attribution mapping permutado dentro de variables/grupos compatibles;
- C: sensitivity control no-XAI;
- posteriormente U-only vs X+U vs GX+U.

### Estado

**GO como línea de investigación, pero el paper XAI solo existe si XAI demuestra valor causal incremental.**

### Criterio para mantener XAI en el título

Al menos una comparación fuerte debe sobrevivir:

\[
XAI+same\ acquisition
>
matched\ nonXAI+same\ acquisition
\]

or:

\[
Gated\ XAI>Ungated\ XAI
\]

bajo una condición de reliability/shift predeclarada.

Si las mejoras proceden fundamentalmente de ensemble uncertainty, HVI, rank/dominance acquisition o adaptive regions, el artículo debe **reformularse** y XAI pasar a ser secundario.

### Venue orientativo

- *Information Sciences*;
- *Applied Soft Computing*;
- *Swarm and Evolutionary Computation* si la contribución algorítmica es general;
- *Engineering Applications of Artificial Intelligence*;
- TEVC solo con validación general fuerte.

---

# 3. Relación con el manuscrito actual

El manuscrito actual / XAI-v1 ya contiene evidencia sobre:

- global vs regional fidelity;
- MAPE/Spearman divergence;
- surrogate-assisted simulator savings;
- novelty/ratification;
- episodic retraining;
- XAI-guided sampling;
- GraphSAGE y structural holdout.

Por tanto, **eliminar XAI del título no basta para crear un paper nuevo**.

Cada paper debe tener una unidad científica claramente nueva:

| Paper | Unidad científica que justifica independencia |
|---|---|
| P1 | environmental-selection fidelity + counterfactual checkpoints + trajectory consequences |
| P2 | reconciled Fog targets + controlled structural shifts + flat/graph/physics inductive biases |
| P3 | explicit utility/risk of harmful environmental-selection errors for simulator allocation |
| P4 | matched causal evidence of incremental XAI value + explanation reliability gating |

---

# 4. Roadmap científico

```text
                         ┌────────────────────────────┐
                         │ PHASE 0                    │
                         │ correctness + provenance   │
                         └─────────────┬──────────────┘
                                       │
               ┌───────────────────────┼─────────────────────────┐
               │                       │                         │
               ▼                       ▼                         ▼
     Selection-fidelity pilot   Fog reconciliation      Freeze XAI-v1 +
               │                       │                matched accounting
               ▼                       ▼                         │
    One-step counterfactual    Structural pilot                 ▼
        selection audit        flat/graph/physics        Pareto-local XAI
               │                       │                    A/B/C pilot
       signal beyond rank?       structural signal?              │
          /          \              /        \               XAI signal?
        yes           no           yes        no              /        \
         │             │            │          │            yes        no
         ▼             ▼            ▼          ▼             │          │
 branching        reduce/merge   full       reformulate       ▼          ▼
 trajectories                    benchmark   /drop        ensemble +   reframe/
         │                         │                      reliability     drop
         ▼                         ▼                         gate
      PAPER 1                   PAPER 2                       │
         │                                                    ▼
         │                                          U-only / X+U / GX+U
         │                                                    │
         └──────► identify harmful decision target            ▼
                         │                                  PAPER 4
                         ▼
                 offline decision-risk
                         │
                 incremental value?
                    /          \
                  yes           no
                   │             │
                   ▼             ▼
           online matched     DROP/MERGE
           budget campaign
                   │
                   ▼
                 PAPER 3
```

---

# 5. Prioridad operativa

## Priority 0 — correctness and provenance

Antes de nuevas campañas:

- freeze NSGA-II correctness;
- regression tests for rank/crowding/selection/state;
- reconcile Fog datasets and temporal targets;
- establish checkpoint provenance;
- freeze XAI-v1 baseline and experimental accounting.

## Priority 1 — Paper 1 pilot

Run one-step counterfactual environmental selection.

Key question:

\[
\boxed{
Does\ selection\ fidelity/regret\ add\ information\ beyond\ ranking/dominance?
}
\]

## Priority 2 — Paper 2 pilot

After Fog reconciliation, compare only:

1. strong flat baseline;
2. GraphSAGE;
3. compositional/physics baseline.

Do not expand the GNN zoo before the pilot shows a real structural signal.

## Priority 3 — XAI Paper 4 causal pilot

In parallel after the XAI-v1 baseline is frozen:

- multi-region anchors;
- local biobjective explanations;
- matched A/B/C arms;
- exact accounting;
- common sampler.

This can proceed without waiting for Paper 3.

## Priority 4 — Branching trajectories / full P1

Only branch enough checkpoints to characterize which decision disagreements create persistent harm.

## Priority 5 — Paper 3

Design only after Paper 1 identifies a meaningful decision-risk target.

---

# 6. Shared artifacts and later papers

## Audited Decision Benchmark

Potential artifact generated by Paper 1 containing, for checkpoint \(t\):

\[
(C_t,\hat F,F,\hat r,r,\widehat{CD},CD,\hat S,S,R_{sel}).
\]

Treat as **artifact first, independent paper later only if community value is demonstrated**.

## CloudEvolve software paper

Possible parallel SoftwareX/JOSS track if the software becomes sufficiently stable, documented, tested and reusable/open-source as required by the target venue.

## Cross-workload optimization transfer

Later line using Paper 1 metrics to distinguish:

\[
prediction\ transfer
\neq
selection\ transfer
\neq
optimization\ transfer.
\]

## Robustness under workload/simulator variation

Independent later branch on robust Pareto outcomes under environment/simulator uncertainty. Requires its own SOTA and experimental plan.

---

# 7. Final portfolio

| Priority | Paper / output | Status | Main dependency |
|---:|---|---|---|
| **1** | [Paper 1 — Selection Fidelity and Trajectory Consequences](paper_1_selection_fidelity.md) | **GO** | correctness + audited checkpoints |
| **2** | [Paper 2 — Structural Generalization](paper_2_structural_generalization.md) | **GO after reconciliation** | Fog provenance / correct targets |
| **3** | [Paper 4 — XAI Extension](paper_4_xai_extension.md) | **GO conditional on XAI effect** | frozen XAI-v1 + matched controls |
| **4** | [Paper 3 — Simulation Allocation by Decision Utility](paper_3_decision_utility.md) | **CONDITIONAL GO** | Paper 1 decision-risk evidence |
| 5 | Audited Decision Benchmark | Artifact | Paper 1 |
| 6 | CloudEvolve SoftwareX/JOSS | Parallel | software maturity |
| 7 | Cross-workload transfer | Later | Paper 1 metrics |
| 8 | Robustness under workload/simulator variation | Later | new campaign/SOTA |

The numbering of Papers 1–4 is kept stable even though the operational order may execute the XAI Paper 4 pilot before the full Paper 3 campaign.

---

# 8. Final decision

The roadmap should not maximize the number of papers. It should maximize independent scientific claims that survive strong controls.

The core program is:

\[
\boxed{
P1:\ decision\ fidelity
}
\]

\[
\boxed{
P2:\ structural\ generalization
}
\]

\[
\boxed{
P3:\ decision\ utility\ for\ simulator\ allocation\quad(conditional)
}
\]

\[
\boxed{
P4:\ incremental\ causal\ value\ of\ reliable\ XAI\quad(conditional)
}
\]

The XAI extension is therefore explicitly part of the strategic publication roadmap, but it is kept separate from Papers 1–3 so that its scientific contribution can be tested independently rather than being attributed to general surrogate-reliability improvements.
