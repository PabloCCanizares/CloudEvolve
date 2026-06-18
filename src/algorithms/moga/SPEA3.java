/*******************************************************************************
 * Copyright (C) 2022 Miguel Pérez
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 ******************************************************************************/
package algorithms.moga;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import algorithms.Chromosome;
import algorithms.ChromosomeComparator;
import algorithms.Fitness;

public class SPEA3<C extends Chromosome<C>, T extends Comparable<T>>
        extends MultiObjectiveGeneticAlgorithm<C, T> {

    private static final int POPULATION_MAX_SIZE = 10;
    private static final double EPS = 1e-12;

    private final ChromosomesComparatorSPEA chromosomesComparator;
    private final FitnessComparator fc;
    private PopulationMO<C> archive;
    // Se mantienen por compatibilidad con tu firma original
    private double radius;
    private double densityPow;

    private class ChromosomesComparatorSPEA implements ChromosomeComparator<C> {

        private final Map<C, T> cache = new WeakHashMap<>();

        @Override
        public int sort(List<C> chromosomes) {

            LinkedList<LinkedList<MOSolution<C, T>>> dominationFronts = new LinkedList<>();
            LinkedList<MOSolution<C, T>> solutionList = new LinkedList<>();
            MOSolution<C, T> moSolIndex1, moSolIndex2;

            int nIndex1 = 0, nIndex2 = 0;

            // Envolver individuos y calcular "fitnessFunc" si aplica
            for (C c1 : chromosomes) {
                moSolIndex1 = new MOSolution<>(c1, this.fit(c1));
                solutionList.add(nIndex1, moSolIndex1);
                nIndex1++;
            }

            nIndex1 = nIndex2 = 0;

            // Dominancias (vectoriales) y primer frente
            for (C c1 : chromosomes) {
                moSolIndex1 = solutionList.get(nIndex1);

                for (C c2 : chromosomes) {
                    if (nIndex1 == nIndex2) { // saltar self
                        nIndex2++;
                        continue;
                    }
                    if (this.dominates(c1, c2)) {
                        moSolIndex2 = solutionList.get(nIndex2);
                        moSolIndex1.insertDominatedSolutions(moSolIndex2);
                    } else if (this.dominates(c2, c1)) {
                        moSolIndex1.incrementDominations();
                    }
                    nIndex2++;
                }
                nIndex2 = 0;

                if (moSolIndex1.getDominations() == 0) {
                    moSolIndex1.setRank(1);
                    if (dominationFronts.isEmpty()) {
                        LinkedList<MOSolution<C, T>> firstDom = new LinkedList<>();
                        firstDom.add(moSolIndex1);
                        dominationFronts.add(firstDom);
                    } else {
                        dominationFronts.getFirst().add(moSolIndex1);
                    }
                }
                // guardar #veces dominado en el individuo
                moSolIndex1.getIndividual().setDominated(moSolIndex1.getDominations());

                nIndex1++;
            }

            // Densidad SPEA (k-NN) sobre la población actual
            final int n = chromosomes.size();
            final int m = EGAObjectives.values().length;
            final int kNN = Math.max(1, (int) Math.round(Math.sqrt(Math.max(1, n))));

            // normalización [0,1] (menor es mejor)
            double[] min = new double[m], max = new double[m];
            for (int k = 0; k < m; k++) {
                min[k] = Double.POSITIVE_INFINITY;
                max[k] = Double.NEGATIVE_INFINITY;
            }
            for (C c : chromosomes) {
                int k = 0;
                for (EGAObjectives obj : EGAObjectives.values()) {
                    double v = c.getObjective(obj);
                    if (v < min[k]) min[k] = v;
                    if (v > max[k]) max[k] = v;
                    k++;
                }
            }

            double[][] Z = new double[n][m];
            for (int i = 0; i < n; i++) {
                C c = chromosomes.get(i);
                int k = 0;
                for (EGAObjectives obj : EGAObjectives.values()) {
                    double v = c.getObjective(obj);
                    double denom = Math.max(max[k] - min[k], EPS);
                    Z[i][k] = (v - min[k]) / denom; // 0..1
                    k++;
                }
            }

            // distancias euclídeas
            double[][] dist = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    double s = 0.0;
                    for (int k = 0; k < m; k++) {
                        double d = Z[i][k] - Z[j][k];
                        s += d * d;
                    }
                    double d = Math.sqrt(s);
                    dist[i][j] = d;
                    dist[j][i] = d;
                }
            }

            // D(i) = 1 / (sigma_k + 2)  (se escribe en el cromosoma)
            for (int i = 0; i < n; i++) {
                double[] row = java.util.Arrays.copyOf(dist[i], n);
                java.util.Arrays.sort(row); // row[0] = 0 (self)
                double sigma = row[Math.min(kNN, n - 1)];
                double density = 1.0 / (sigma + 2.0);
                chromosomes.get(i).setnCrowdDensity(density);
            }

            //Propagación de frentes (condición robusta)
            int i = 1;
            while (dominationFronts.size() >= i) {
                LinkedList<MOSolution<C, T>> nextDominationFront = new LinkedList<>();
                for (MOSolution<C, T> p : dominationFronts.get(i - 1)) {
                    LinkedList<MOSolution<C, T>> dominatedList = p.getDominatedIndividuals();
                    for (MOSolution<C, T> q : dominatedList) {
                        q.decrementDominations();
                        if (q.getDominations() == 0) {
                            q.setRank(i + 1);
                            nextDominationFront.add(q);
                        }
                    }
                }
                i++;
                if (!nextDominationFront.isEmpty()) {
                    dominationFronts.add(nextDominationFront);
                } else {
                    break;
                }
            }

            // aplanar frentes en orden
            chromosomes.clear();
            for (LinkedList<MOSolution<C, T>> front : dominationFronts) {
                for (MOSolution<C, T> s : front) {
                    chromosomes.add(s.getIndividual());
                }
            }

            // ---- Fitness final: usa tu API sin argumentos ----
            // (ya tenemos setDominated(...) y setnCrowdDensity(...) puestos)
            for (C chrom : chromosomes) {
                chrom.setFitness(); // recalcula interno a partir de los componentes previos
            }

            // ordenar por fitness ascendente
            chromosomes.sort((a, b) -> Double.compare(a.getFitness(), b.getFitness()));

            return 1;
        }

        // Dominancia vectorial (minimización por defecto)
        private boolean dominates(C a, C b) {
            boolean better = false, worse = false;
            for (EGAObjectives obj : EGAObjectives.values()) {
                double va = a.getObjective(obj);
                double vb = b.getObjective(obj);
                if (va < vb - EPS) better = true;
                else if (va > vb + EPS) worse = true;
            }
            return better && !worse;
        }

        public T fit(C chr) {
            T fit = this.cache.get(chr);
            if (fit == null) {
                fit = SPEA3.this.fitnessFunc.calculate(chr);
                this.cache.put(chr, fit);
            }
            return fit;
        }

        public void clearCache() {
            this.cache.clear();
        }
    }

    private class FitnessComparator implements Comparator<C> {
        @Override
        public int compare(C o1, C o2) {
            return Double.compare(o1.getFitness(), o2.getFitness());
        }
    }

    /**
     * @param population  población inicial
     * @param fitnessFunc función de fitness (si aplica a tu modelo)
     * @param radius      (no usado por k-NN; mantenido por compatibilidad)
     * @param densityPow  (no usado por k-NN; mantenido por compatibilidad)
     */
    public SPEA3(PopulationMO<C> population, Fitness<C, T> fitnessFunc, double radius, double densityPow) {
        this.population = population;
        this.archive = new PopulationMO<>();
        this.fc = new FitnessComparator();
        this.fitnessFunc = fitnessFunc;
        this.chromosomesComparator = new ChromosomesComparatorSPEA(); // inicializado aquí
        this.radius = radius;
        this.densityPow = densityPow;

        // evaluación/ordenación inicial
        this.population.sortPopulationByFitness(this.chromosomesComparator);
        for (C chrom : this.population) {
            if (chrom.getNumDom() == 0.0)
                this.archive.addChromosome(chrom);
        }
        this.archive.trim(POPULATION_MAX_SIZE);
    }

    /**
     * Evolución con cambios mínimos: mantener flujo, evaluar con el comparator
     * (dominancia+kNN+setFitness()), limpiar inválidos, actualizar archivo y recortar.
     */
    public void evolve() {
        int parentPopulationSize = this.population.getSize();

        PopulationMO<C> newPopulation = new PopulationMO<>();

        // elitismo
        for (int i = 0; (i < parentPopulationSize) && (i < this.parentChromosomesSurviveCount); i++) {
            newPopulation.addChromosome(this.population.getChromosomeByIndex(i));
        }

        // descendencia desde el archivo
        for (int i = 0; i < archive.getSize(); i++) {
            C chromosome = this.archive.getChromosomeByIndex(i);
            C mutated = chromosome.mutate();

            C otherChromosome = this.archive.getRandomChromosome();
            List<C> crossovered = chromosome.crossover(otherChromosome);

            if (mutated != null) newPopulation.addChromosome(mutated);
            if (crossovered != null) {
                for (C c : crossovered) if (c != null) newPopulation.addChromosome(c);
            }
        }

        // evaluar (dominancia + densidad kNN + setFitness())
        populationTestPrettyPrint(newPopulation);
        newPopulation.sortPopulationByFitness(this.chromosomesComparator);
        populationTestPrettyPrint(newPopulation);

        // limpiar inválidos
        for (int i = 0; i < newPopulation.getSize(); i++) {
            C chrom = newPopulation.getChromosomeByIndex(i);
            if (!chrom.isFitnessValid()) {
                newPopulation.deleteChromosome(chrom);
                i--;
            }
        }

        // actualizar archivo
        updateArchive(newPopulation);
        populationTestPrettyPrint(newPopulation);

        // recorta población al tamaño objetivo y fija
        newPopulation.trim(parentPopulationSize);
        this.population = newPopulation;

        populationTestPrettyPrint(newPopulation);
    }

    public void updateArchive(PopulationMO<C> newPop) {
        for (C chrom : newPop)
            if (chrom.getNumDom() == 0.0)
                this.archive.addChromosome(chrom);

        archive.sortPopulationByFitness(fc);

        if (this.archive.getSize() > POPULATION_MAX_SIZE)
            archive.trim(POPULATION_MAX_SIZE);

        System.out.println("Sorted");
    }

    public T fitness(C chromosome) {
        return this.chromosomesComparator.fit(chromosome);
    }

    public void clearCache() {
        this.chromosomesComparator.clearCache();
    }
}
