package algorithms.moga;

import java.util.*;
import algorithms.Chromosome;
import algorithms.ChromosomeComparator;
import algorithms.Fitness;

public class NSGAII2<C extends Chromosome<C>, T extends Comparable<T>>
        extends MultiObjectiveGeneticAlgorithm<C, T> {

    private static final double EPS = 1e-9;
    private static final int MAX_MUT_TRIES = 32;

    private final Fitness<C, T> fitnessFunc;
    private final ChromosomesComparatorMO sorter;

    public NSGAII2(PopulationMO<C> population, Fitness<C, T> fitnessFunc, int ignoredNumObjectives) {
        this.population = population;
        this.fitnessFunc = fitnessFunc;
        this.sorter = new ChromosomesComparatorMO();

        // Evalúa y filtra población inicial (solo factibles y válidos)
        for (int i = 0; i < this.population.getSize(); ) {
            C chr = this.population.getChromosomeByIndex(i);
            fitnessFunc.calculate(chr);
            if (!isFeasible(chr) || !chr.isFitnessValid()) {
                this.population.deleteChromosome(chr);
            } else {
                i++;
            }
        }
        // Clasificación inicial
        this.population.sortPopulationByFitness(this.sorter);
    }

    @Override
    public void evolve() {
        final PopulationMO<C> parents = this.population;

        // Reproducción (offspring |Q_t| = |P_t|), filtrando infeasibles
        PopulationMO<C> offspring = reproduce(parents);

        // Unión R_t = P_t ∪ Q_t (solo factibles/validos)
        PopulationMO<C> union = new PopulationMO<>();
        for (C c : parents)   if (isFeasible(c) && c.isFitnessValid()) union.addChromosome(c);
        for (C c : offspring) if (isFeasible(c) && c.isFitnessValid()) union.addChromosome(c);
        if (union.getSize() == 0) { this.population = parents; return; }

        // Ordenar por frentes + crowding
        union.sortPopulationByFitness(this.sorter);

        // Selección elitista a N
        final int N = POPULATION_MAX_SIZE;
        PopulationMO<C> next = selectByFrontsAndCrowding(union, N);

        this.population = next;
    }

    @Override
    public void evolve(int count) {
        this.terminate = false;
        for (int i = 0; i < count; i++) {
            if (this.terminate) break;
            evolve();
            this.iteration = i;
            for (IMOIterationListener<C, T> l : this.iterationListeners) l.update(this);
        }
    }

    // -------------------- Reproducción --------------------

    private PopulationMO<C> reproduce(PopulationMO<C> parents) {
        int mu = parents.getSize();
        PopulationMO<C> off = new PopulationMO<>();
        if (mu == 0) return off;

        // Asegura rank/crowding para torneo
        parents.sortPopulationByFitness(this.sorter);

        List<C> pool = new ArrayList<>(mu);
        for (int i = 0; i < mu; i++) pool.add(parents.getChromosomeByIndex(i));
        Random rng = new Random();

        while (off.getSize() < mu) {
            C p1 = binaryTournament(pool.get(rng.nextInt(mu)), pool.get(rng.nextInt(mu)));
            C p2 = binaryTournament(pool.get(rng.nextInt(mu)), pool.get(rng.nextInt(mu)));

            // Mutaciones factibles desde clones
            C c1 = p1.dup();
            C m1 = tryMutateFeasible(c1);
            if (m1 != null) { fitnessFunc.calculate(m1); if (isFeasible(m1) && m1.isFitnessValid()) off.addChromosome(m1); }

            C c2 = p2.dup();
            C m2 = tryMutateFeasible(c2);
            if (m2 != null) { fitnessFunc.calculate(m2); if (isFeasible(m2) && m2.isFitnessValid()) off.addChromosome(m2); }

            // Cruce p1 x p2
            List<C> cross = p1.crossover(p2);
            if (cross != null) {
                for (C child : cross) {
                    if (child == null) continue;
                    fitnessFunc.calculate(child);
                    if (isFeasible(child) && child.isFitnessValid()) {
                        off.addChromosome(child);
                        if (off.getSize() >= mu) break;
                    }
                }
            }
        }
        off.trim(mu);
        return off;
    }

    private C tryMutateFeasible(C base) {
        C cur = null;
        for (int t = 0; t < MAX_MUT_TRIES; t++) {
            cur = base.mutate();
            if (cur == null) continue;
            fitnessFunc.calculate(cur);
            if (isFeasible(cur) && cur.isFitnessValid()) return cur;
        }
        return null;
    }

    private C binaryTournament(C a, C b) {
        if (a.getRank() < b.getRank()) return a;
        if (b.getRank() < a.getRank()) return b;
        if (a.getCrowdingDistance() > b.getCrowdingDistance()) return a;
        if (b.getCrowdingDistance() > a.getCrowdingDistance()) return b;
        return Math.random() < 0.5 ? a : b;
    }

    // --------- Selección por frentes + crowding ---------

    private PopulationMO<C> selectByFrontsAndCrowding(PopulationMO<C> union, int N) {
        List<List<C>> fronts = splitByFronts(union);
        PopulationMO<C> out = new PopulationMO<>();
        int f = 0;
        while (f < fronts.size() && out.getSize() + fronts.get(f).size() <= N) {
            for (C c : fronts.get(f)) out.addChromosome(c);
            f++;
        }
        if (out.getSize() < N && f < fronts.size()) {
            List<C> last = new ArrayList<>(fronts.get(f));
            last.sort((x, y) -> Double.compare(y.getCrowdingDistance(), x.getCrowdingDistance()));
            for (C c : last) { if (out.getSize() >= N) break; out.addChromosome(c); }
        }
        return out;
    }

    private List<List<C>> splitByFronts(PopulationMO<C> pop) {
        List<List<C>> fronts = new ArrayList<>();
        int currentRank = 1;
        List<C> cur = new ArrayList<>();
        for (C c : pop) {
            if (c.getRank() == currentRank) {
                cur.add(c);
            } else {
                if (!cur.isEmpty()) fronts.add(cur);
                cur = new ArrayList<>();
                currentRank = c.getRank();
                cur.add(c);
            }
        }
        if (!cur.isEmpty()) fronts.add(cur);
        return fronts;
    }

    // -------------------- Sorter/Comparador --------------------

    private class ChromosomesComparatorMO implements ChromosomeComparator<C> {
        private final Map<C, T> cache = new WeakHashMap<>();

        @Override
        public int sort(List<C> chromosomes) {
            // Construir MOSolutions solo con factibles/validos
            List<MOSolution<C, T>> sols = new ArrayList<>(chromosomes.size());
            for (C c : chromosomes) {
                if (!isFeasible(c) || !c.isFitnessValid()) continue;
                sols.add(new MOSolution<>(c, fit(c)));
            }
            if (sols.isEmpty()) {
                chromosomes.clear();
                return 1;
            }

            // Fast non-dominated sorting usando SOLO métodos existentes de MOSolution
            List<LinkedList<MOSolution<C, T>>> fronts = new ArrayList<>();

            // Inicializa estructuras: dominatedSolutions vacía y dominations=0 (por construcción)
            for (MOSolution<C, T> p : sols) {
                // nada que setear; por defecto lista vacía y dominations=0
            }

            // Emparejamientos p vs q
            for (int i = 0; i < sols.size(); i++) {
                MOSolution<C, T> p = sols.get(i);
                for (int j = 0; j < sols.size(); j++) {
                    if (i == j) continue;
                    MOSolution<C, T> q = sols.get(j);
                    if (dominates(p.getIndividual(), q.getIndividual())) {
                        p.insertDominatedSolutions(q);
                    } else if (dominates(q.getIndividual(), p.getIndividual())) {
                        p.incrementDominations();
                    }
                }
            }

            // Frente 1
            LinkedList<MOSolution<C, T>> F1 = new LinkedList<>();
            for (MOSolution<C, T> p : sols) {
                if (p.getDominations() == 0) {
                    p.setRank(1); // propaga a chromosome
                    F1.add(p);
                }
            }
            if (!F1.isEmpty()) fronts.add(F1);

            // Siguientes frentes
            int idx = 0;
            while (idx < fronts.size()) {
                LinkedList<MOSolution<C, T>> next = new LinkedList<>();
                for (MOSolution<C, T> p : fronts.get(idx)) {
                    for (MOSolution<C, T> q : p.getDominatedIndividuals()) {
                        q.decrementDominations();
                        if (q.getDominations() == 0) {
                            q.setRank(idx + 2);
                            next.add(q);
                        }
                    }
                }
                if (!next.isEmpty()) fronts.add(next);
                idx++;
            }

            // Crowding y aplanado (ordenando cada frente por crowding desc)
            List<C> flattened = new ArrayList<>(sols.size());
            for (LinkedList<MOSolution<C, T>> F : fronts) {
                calculateCrowdingDistance(F);
                F.sort(Comparator.comparingDouble(MOSolution<C, T>::getCrowdingDistance).reversed());
                for (MOSolution<C, T> s : F) flattened.add(s.getIndividual());
            }

            chromosomes.clear();
            chromosomes.addAll(flattened);
            return 1;
        }

        private void calculateCrowdingDistance(List<MOSolution<C, T>> front) {
            if (front == null || front.size() == 0) return;

            for (MOSolution<C, T> s : front) s.setCrowdingDistance(0.0);

            EGAObjectives[] objs = EGAObjectives.values();
            for (EGAObjectives obj : objs) {
                front.sort(Comparator.comparingDouble(s -> s.getObjective(obj)));
                // bordes
                front.get(0).setCrowdingDistance(Double.POSITIVE_INFINITY);
                front.get(front.size() - 1).setCrowdingDistance(Double.POSITIVE_INFINITY);

                double min = front.get(0).getObjective(obj);
                double max = front.get(front.size() - 1).getObjective(obj);
                if (Math.abs(max - min) < EPS) continue;

                for (int i = 1; i < front.size() - 1; i++) {
                    double prev = front.get(i - 1).getObjective(obj);
                    double next = front.get(i + 1).getObjective(obj);
                    double cd = front.get(i).getCrowdingDistance();
                    cd += (next - prev) / (max - min);
                    front.get(i).setCrowdingDistance(cd);
                }
            }
        }

        private boolean dominates(C a, C b) {
            // Regla de factibilidad: uno infeasible nunca domina a factible
            boolean aFeas = isFeasible(a) && a.isFitnessValid();
            boolean bFeas = isFeasible(b) && b.isFitnessValid();
            if (!aFeas && bFeas) return false;
            if (aFeas && !bFeas) return true;
            if (!aFeas && !bFeas) return false;

            boolean better = false;
            for (EGAObjectives obj : EGAObjectives.values()) {
                double va = a.getObjective(obj), vb = b.getObjective(obj);
                // Si maximizas algún objetivo, invierte aquí (va=-va; vb=-vb)
                if (va > vb + EPS) return false;    // peor en alguno
                if (va + EPS < vb) better = true;   // mejor en alguno
            }
            return better;
        }

        private T fit(C chr) {
            T f = cache.get(chr);
            if (f == null) {
                f = NSGAII2.this.fitnessFunc.calculate(chr);
                cache.put(chr, f);
            }
            return f;
        }

        public void clearCache() { cache.clear(); }
    }

    // -------------------- Utilidades --------------------

    /** Factibilidad estricta: TODOS los objetivos > 0. */
    private boolean isFeasible(C x) {
        if (x == null) return false;
        for (EGAObjectives obj : EGAObjectives.values()) {
            if (x.getObjective(obj) <= 0.0) return false;
        }
        return true;
    }

    @Override
    public T fitness(C chromosome) { return this.sorter.fit(chromosome); }

    public void clearCache() { this.sorter.clearCache(); }
}
