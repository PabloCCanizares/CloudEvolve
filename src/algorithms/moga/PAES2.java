package algorithms.moga;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import algorithms.Chromosome;
import algorithms.ChromosomeComparator;
import algorithms.Fitness;

public class PAES2<C extends Chromosome<C>, T extends Comparable<T>>
        extends MultiObjectiveGeneticAlgorithm<C, T> {

    private static final double EPS = 1e-9;
    private static final int MAX_MUTATION_TRIES = 64;
    private static final int MAX_PARENT_ADVANCES = 64; // para saltar padres infeasibles

    protected class ChromosomesComparatorPAES implements ChromosomeComparator<C> {
        private final Map<C, T> cache = new WeakHashMap<>();

        @Override
        public int sort(List<C> chromosomes) {
            LinkedList<LinkedList<MOSolution<C, T>>> dominationFronts = new LinkedList<>();
            LinkedList<MOSolution<C, T>> solutionList = new LinkedList<>();

            int nIndex1 = 0, nIndex2 = 0;

            for (C c1 : chromosomes) {
                // Ignora infeasibles al clasificar (no deberían llegar aquí, pero por si acaso)
                if (!isFeasible(c1)) continue;
                MOSolution<C, T> moSol = new MOSolution<>(c1, this.fit(c1));
                solutionList.add(nIndex1, moSol);
                nIndex1++;
            }

            nIndex1 = nIndex2 = 0;

            for (C c1 : chromosomes) {
                if (!isFeasible(c1)) { nIndex1++; continue; }
                MOSolution<C, T> moSolIndex1 = solutionList.get(nIndex1);

                for (C c2 : chromosomes) {
                    if (!isFeasible(c2)) { nIndex2++; continue; }
                    if (nIndex1 == nIndex2) { nIndex2++; continue; }

                    if (dominates(c1, c2)) {
                        MOSolution<C, T> moSolIndex2 = solutionList.get(nIndex2);
                        moSolIndex1.insertDominatedSolutions(moSolIndex2);
                    } else if (dominates(c2, c1)) {
                        moSolIndex1.incrementDominations();
                    }
                    nIndex2++;
                }
                nIndex2 = 0;

                if (moSolIndex1.getDominations() == 0) {
                    moSolIndex1.setRank(1);
                    if (dominationFronts.isEmpty()) {
                        LinkedList<MOSolution<C, T>> first = new LinkedList<>();
                        first.add(moSolIndex1);
                        dominationFronts.add(first);
                    } else {
                        dominationFronts.getFirst().add(moSolIndex1);
                    }
                }

                try { moSolIndex1.getIndividual().setDominated(moSolIndex1.getDominations()); } catch (Throwable ignore) {}
                nIndex1++;
            }

            int i = 1;
            while (i < dominationFronts.size()) {
                LinkedList<MOSolution<C, T>> nextFront = new LinkedList<>();
                for (MOSolution<C, T> p : dominationFronts.get(i - 1)) {
                    LinkedList<MOSolution<C, T>> dominatedList = p.getDominatedIndividuals();
                    for (MOSolution<C, T> q : dominatedList) {
                        q.decrementDominations();
                        if (q.getDominations() == 0) {
                            q.setRank(i + 1);
                            nextFront.add(q);
                        }
                    }
                }
                if (!nextFront.isEmpty()) dominationFronts.add(nextFront);
                i++;
            }

            chromosomes.clear();
            for (LinkedList<MOSolution<C, T>> front : dominationFronts) {
                for (MOSolution<C, T> s : front) chromosomes.add(s.getIndividual());
            }
            return 1;
        }

        private boolean dominates(C a, C b) {
            // Los infeasibles nunca dominan y siempre son dominados
            if (!isFeasible(a)) return false;
            if (!isFeasible(b)) return true;

            boolean better = false;
            for (EGAObjectives obj : EGAObjectives.values()) {
                double va = a.getObjective(obj), vb = b.getObjective(obj);
                if (va > vb + EPS) return false;
                if (va + EPS < vb) better = true;
            }
            return better;
        }

        public T fit(C chr) {
            T fit = cache.get(chr);
            if (fit == null) {
                fit = PAES2.this.fitnessFunc.calculate(chr);
                cache.put(chr, fit);
            }
            return fit;
        }
        public void clearCache() { cache.clear(); }

        public int compare(C a, C b) {
            T fa = this.fit(a);
            T fb = this.fit(b);
            return fa.compareTo(fb);
        }
    }

    private final ChromosomesComparatorPAES chromosomesComparator;
    private CartesianDistanceComparator<C> distanceComparator;
    private final Fitness<C, T> fitnessFunc;

    private AdaptiveCellGrid<C> archive;

    private final int bisections;
    private final int archiveSize;

    private final PopulationMO<C> basePopulation;

    public PAES2(PopulationMO<C> population,
                 Fitness<C, T> fitnessFunc,
                 int bisections,
                 int archiveSize) {
        this.basePopulation = population;
        this.fitnessFunc = fitnessFunc;
        this.bisections = bisections;
        this.archiveSize = archiveSize;

        this.chromosomesComparator = new ChromosomesComparatorPAES();
        this.distanceComparator = new CartesianDistanceComparator<>();

        try {
            this.archive = new AdaptiveCellGrid<>(archiveSize, (int) Math.pow(2, bisections), 2);
        } catch (Exception e) {
            throw new RuntimeException("Error creando AdaptiveCellGrid", e);
        }

        // Evalúa población inicial; conserva solo factibles
        for (int i = 0; i < this.basePopulation.getSize(); ) {
            C chrom = this.basePopulation.getChromosomeByIndex(i);
            calculateSingleChromosome(chrom);
            if (isFeasible(chrom) && chrom.isFitnessValid()) {
                this.archive.add(chrom);
                i++;
            } else {
                // elimina infeasible de la población de trabajo
                this.basePopulation.deleteChromosome(chrom);
                // no incrementamos i porque el tamaño ha decrecido
            }
        }

        // define población de trabajo
        this.population = this.basePopulation;
    }

    public void calculatePopulationFitness() {
        for (int i = 0; i < this.basePopulation.getSize(); i++) {
            C chr = this.basePopulation.getChromosomeByIndex(i);
            fitnessFunc.calculate(chr);
        }
    }

    public void calculateSingleChromosome(C chrom) {
        fitnessFunc.calculate(chrom);
    }

    public PopulationMO<C> getArchivePopulation() {
        return this.archive.getPopulation();
    }

    @Override
    public void evolve() {
        if (this.basePopulation.getSize() == 0) {
            System.out.println("PAES2: basePopulation vacía (tras filtrar infeasibles).");
            return;
        }

        // Selecciona un padre factible (intenta avanzar si el actual no lo es)
        int triesParent = 0;
        C parent = null;
        while (triesParent++ < MAX_PARENT_ADVANCES) {
            int idx = (this.iteration + triesParent - 1) % this.basePopulation.getSize();
            C cand = this.basePopulation.getChromosomeByIndex(idx);
            if (cand != null && isFeasible(cand) && cand.isFitnessValid()) { parent = cand; break; }
        }
        if (parent == null) return;

        this.archive.add(parent);

        // Mutación hasta encontrar un mutante factible (con límite)
        C mutated = null;
        int tries = 0;
        do {
            mutated = parent.mutate();
            if (mutated != null) {
                calculateSingleChromosome(mutated);
                if (!isFeasible(mutated) || !mutated.isFitnessValid()) {
                    mutated = null; // fuerza reintento
                }
            }
            tries++;
        } while (mutated == null && tries < MAX_MUTATION_TRIES);

        if (mutated == null) return; // no hay mutante factible en esta iteración

        // Comparación parent vs mutated (ambos factibles)
        int dominance = isDominated(mutated, parent);

        if (dominance < 0) {
            // mutated dominado -> descarta
            return;
        } else if (dominance > 0) {
            // mutated domina -> replace y actualizar archivo
            this.basePopulation.replace(parent, mutated);
            this.archive.add(mutated);
            this.archive.remove(parent);
        } else {
            // No-dominados -> decide por densidad del archivo
            addToArchive(parent, mutated);
        }
    }

    private void addToArchive(C parent, C mutated) {
        // Precondición: ambos factibles
        if (!isFeasible(mutated)) return;

        if (archive.getSize() < archiveSize) {
            if (archive.add(mutated)) {
                if (archive.getDensity(mutated) < archive.getDensity(parent)) {
                    basePopulation.deleteChromosome(parent);
                    basePopulation.addChromosome(mutated);
                }
            }
        } else {
            if (archive.isInLessCrowdedRegion(parent, mutated)) {
                if (archive.add(mutated)) {
                    if (archive.getDensity(mutated) < archive.getDensity(parent)) {
                        basePopulation.deleteChromosome(parent);
                        basePopulation.addChromosome(mutated);
                    }
                }
            } else {
                if (archive.getDensity(mutated) < archive.getDensity(parent)) {
                    basePopulation.deleteChromosome(parent);
                    basePopulation.addChromosome(mutated);
                }
            }
        }
    }

    @Override
    public void evolve(int count) {
        this.terminate = false;

        for (int i = 0; i < count; i++) {
            if (this.terminate) break;

            this.evolve();

            // Reinit comparator y normalización con SOLO factibles
            this.distanceComparator = new CartesianDistanceComparator<>();
            for (EGAObjectives obj : EGAObjectives.values()) {
                double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
                for (int k = 0; k < this.basePopulation.getSize(); k++) {
                    C c = this.basePopulation.getChromosomeByIndex(k);
                    if (!isFeasible(c)) continue;
                    double v = c.getObjective(obj);
                    if (v < min) min = v;
                    if (v > max) max = v;
                }
                // Si no hay factibles para este objetivo, evita añadir rangos degenerados
                if (min < Double.POSITIVE_INFINITY && max > Double.NEGATIVE_INFINITY) {
                    this.distanceComparator.addValueMin(obj, min);
                    this.distanceComparator.addValueMax(obj, max);
                }
            }

            // Orden solo para reporting (no altera archivo)
            this.basePopulation.sortPopulationByFitness(this.distanceComparator);

            this.iteration = i;
            for (IMOIterationListener<C, T> l : this.iterationListeners) {
                l.update(this);
            }
        }
    }

    // +1 si c1 domina a c2, -1 si c2 domina a c1, 0 si no-dominados/iguales. 
    public int isDominated(C c1, C c2) {
        // Asumimos ambos factibles: si no, el llamador debe filtrar antes
        boolean c1Better = false, c2Better = false;
        for (EGAObjectives obj : EGAObjectives.values()) {
            double v1 = c1.getObjective(obj);
            double v2 = c2.getObjective(obj);
            if (v1 + EPS < v2) c1Better = true;
            else if (v2 + EPS < v1) c2Better = true;

            if (c1Better && c2Better) return 0;
        }
        if (c1Better && !c2Better) return 1;
        if (c2Better && !c1Better) return -1;
        return 0;
    }

    @Override
    public T fitness(C chromosome) {
        return this.chromosomesComparator.fit(chromosome);
    }

    public void clearCache() {
        this.chromosomesComparator.clearCache();
    }

    // Feasibility: TODOS los objetivos deben ser estrictamente positivos.
    private boolean isFeasible(C x) {
        if (x == null) return false;
        for (EGAObjectives obj : EGAObjectives.values()) {
            if (x.getObjective(obj) <= 0.0) return false; // solo positivos
        }
        return true;
    }
}
