/*******************************************************************************
 * Copyright 2022 Miguel Pérez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package algorithms.moga;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import algorithms.Chromosome;
import algorithms.Fitness;

public class VEGA2<C extends Chromosome<C>, T extends Comparable<T>>
        extends MultiObjectiveGeneticAlgorithm<C, T> {

    // Comparator que evalúa cada cromosoma con un fitness escalar
    // Si se indica un objetivo, calcula el fitness sesgado a ese objetivo
    private class ChromosomesComparatorMO implements Comparator<C> {
        private final Map<C, T> cache = new WeakHashMap<>();
        private final EGAObjectives objective;

        public ChromosomesComparatorMO(EGAObjectives objective) {
            this.objective = objective;
        }

        public ChromosomesComparatorMO() {
            this.objective = null;
        }

        @Override
        public int compare(C a, C b) {
            T fa = fit(a);
            T fb = fit(b);
            return fa.compareTo(fb);
        }

        public T fit(C chr) {
            T f = this.cache.get(chr);
            if (f == null) {
                // Si hay objetivo, usa la variante por objetivo; si no, la global
                f = (objective != null)
                        ? VEGA2.this.fitnessFunc.calculate(chr, objective)
                        : VEGA2.this.fitnessFunc.calculate(chr);
                this.cache.put(chr, f);
            }
            return f;
        }

        public void clearCache() {
            this.cache.clear();
        }
    }

    // Comparator de distancia al punto utópico para mezclar subpoblaciones
    private final CartesianDistanceComparator<C> distanceComparator;
    private final ChromosomesComparatorMO chromosomesComparator;

    public VEGA2(PopulationMO<C> population, int nObjectives, Fitness<C, T> fitnessFunc) {
        this.population = population;
        this.fitnessFunc = fitnessFunc;
        this.nObjectives = nObjectives;

        this.chromosomesComparator = new ChromosomesComparatorMO();
        this.distanceComparator = new CartesianDistanceComparator<>();

        // Ordenación inicial por fitness global si la función lo soporta
        this.population.sortPopulationByFitness(this.chromosomesComparator);
    }

    // Recalcula el fitness escalar para toda la población si procede
    public void calculatePopulationFitness() {
        for (int i = 0; i < this.population.getSize(); i++) {
            C chr = population.getChromosomeByIndex(i);
            fitnessFunc.calculate(chr);
        }
    }

    @Override
    public void evolve() {
        final int parentPopulationSize = this.population.getSize();
        if (parentPopulationSize == 0) return;

        // Tamaño objetivo por subpoblación
        final int childPopulationSize = (int) Math.ceil(parentPopulationSize / (double) nObjectives);

        // Subpoblaciones sesgadas por objetivo
        List<PopulationMO<C>> populationPerObjective = new ArrayList<>(nObjectives);
        PopulationMO<C> newPopulation = new PopulationMO<>();

        // Construcción de subpoblaciones: ordenar por cada objetivo y tomar los mejores
        for (int n = 0; n < nObjectives; n++) {
            populationPerObjective.add(new PopulationMO<>());

            PopulationMO<C> sorted = new PopulationMO<>();
            for (int idx = 0; idx < parentPopulationSize; idx++) {
                sorted.addChromosome(this.population.getChromosomeByIndex(idx));
            }
            // reverseSortPopulationByFitness debe dejar los mejores primero según el comparador dado
            sorted.reverseSortPopulationByFitness(new ChromosomesComparatorMO(EGAObjectives.values()[n]));

            // Copiar top-K al subgrupo n, respetando límites y el número de padres que sobreviven
            for (int j = 0; j < childPopulationSize
                    && j < sorted.getSize()
                    && j < this.parentChromosomesSurviveCount; j++) {
                populationPerObjective.get(n).addChromosome(sorted.getChromosomeByIndex(j));
            }
        }

        // Variación dentro de cada subpoblación y re-ordenación por su objetivo
        for (int n = 0; n < nObjectives; n++) {
            PopulationMO<C> sub = populationPerObjective.get(n);
            int baseSize = sub.getSize(); // Tamaño estable para evitar desbordes al ir añadiendo hijos
            if (baseSize == 0) continue;

            for (int i = 0; i < baseSize; i++) {
                C chromosome = sub.getChromosomeByIndex(i);
                C mutated = chromosome.mutate();

                C otherChromosome = sub.getRandomChromosome();
                List<C> crossovered = chromosome.crossover(otherChromosome);

                if (mutated != null) sub.addChromosome(mutated);
                if (crossovered != null) {
                    for (C c : crossovered) if (c != null) sub.addChromosome(c);
                }
            }

            // Mantener presión selectiva por el mismo objetivo tras generar descendencia
            sub.reverseSortPopulationByFitness(new ChromosomesComparatorMO(EGAObjectives.values()[n]));
        }

        // Mezcla intercalada de subpoblaciones para formar la nueva población
        int minPopTam = Integer.MAX_VALUE;
        for (int n = 0; n < nObjectives; n++) {
            minPopTam = Math.min(minPopTam, populationPerObjective.get(n).getSize());
        }
        if (minPopTam == Integer.MAX_VALUE) minPopTam = 0;

        for (int i = 0; i < minPopTam; i++) {
            for (int n = 0; n < nObjectives; n++) {
                newPopulation.addChromosome(populationPerObjective.get(n).getChromosomeByIndex(i));
            }
        }

        // Filtrar cromosomas inválidos
        for (int i = 0; i < newPopulation.getSize(); i++) {
            C chrom = newPopulation.getChromosomeByIndex(i);
            if (!chrom.isFitnessValid()) {
                newPopulation.deleteChromosome(chrom);
                i--;
            }
        }

        if (newPopulation.getSize() == 0) return;

        // Preparación de rangos para normalizar en el comparador de distancia
        double minPoint = Double.POSITIVE_INFINITY;
        double maxPoint = Double.NEGATIVE_INFINITY;

        for (EGAObjectives obj : EGAObjectives.values()) {
            for (int i1 = 0; i1 < newPopulation.getSize(); i1++) {
                double value = newPopulation.getChromosomeByIndex(i1).getObjective(obj);
                if (value < minPoint) minPoint = value;
                if (value > maxPoint) maxPoint = value;
            }
            distanceComparator.addValueMin(obj, minPoint);
            distanceComparator.addValueMax(obj, maxPoint);
            minPoint = Double.POSITIVE_INFINITY;
            maxPoint = Double.NEGATIVE_INFINITY;
        }

        // Orden final heurístico por distancia al utópico para mezclar soluciones de diferentes objetivos
        newPopulation.sortPopulationByFitness(distanceComparator);

        // Recorte al tamaño original para mantener la demografía
        newPopulation.trim(parentPopulationSize);
        this.population = newPopulation;
    }

    public T fitness(C chromosome) {
        return this.chromosomesComparator.fit(chromosome);
    }

    public void clearCache() {
        this.chromosomesComparator.clearCache();
    }
}
