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

import algorithms.Fitness;
import core.Chromosome;

public class VEGA<C extends Chromosome<C>, T extends Comparable<T>> 
			extends MOGeneticAlgorithm <C,T>{

	private class ChromosomesComparatorMO implements Comparator<C> {

		private final Map<C, T> cache = new WeakHashMap<C, T>();
		private EGAObjectives objective;

		public ChromosomesComparatorMO(EGAObjectives objective) {
			this.objective = objective;
		}

		public ChromosomesComparatorMO() {
		}

		@Override
		public int compare(C chr1, C chr2) {
			T fit1 = this.fit(chr1);
			T fit2 = this.fit(chr2);
			int ret = fit1.compareTo(fit2);
			return ret;
		}

		public T fit(C chr) {
			T fit = this.cache.get(chr);
			if (fit == null) {
				if (objective != null)
					fit = VEGA.this.fitnessFunc.calculate(chr, objective);
				else
					fit = VEGA.this.fitnessFunc.calculate(chr);

				this.cache.put(chr, fit);
			}
			return fit;
		};

		public void clearCache() {
			this.cache.clear();
		}
	}
	private final ChromosomesComparatorMO chromosomesComparator;
	private CartesianDistanceComparator distanceComparator;
	

	public VEGA(PopulationMO<C> population, int nObjectives, Fitness<C, T> fitnessFunc) {
		this.population = population;
		this.fitnessFunc = fitnessFunc;
		this.nObjectives = nObjectives;
		this.chromosomesComparator = new ChromosomesComparatorMO();
		this.population.sortPopulationByFitness(this.chromosomesComparator);
		this.distanceComparator = new CartesianDistanceComparator<>();

	}

	public void calculatePopulationFitness() {
		C chr;
		for (int i = 0; i < this.population.getSize(); i++) {

			chr = population.getChromosomeByIndex(i);

			fitnessFunc.calculate(chr);
		}
	}

	public void evolve() {
		int parentPopulationSize = this.population.getSize();
		int childPopulationSize = this.population.getSize() / nObjectives + 1;
		List<PopulationMO<C>> populationPerObjective = new ArrayList<PopulationMO<C>>();
		PopulationMO<C> newPopulation = new PopulationMO<C>();

		for (int i = 0; i < nObjectives; i++) {
			populationPerObjective.add(new PopulationMO<C>());

			for (int j = 0; (j < childPopulationSize) && (i < this.parentChromosomesSurviveCount); j++) {
				populationPerObjective.get(i).addChromosome(this.population.getChromosomeByIndex(j));
			}
		}
		// Quitar por el random
		for (int n = 0; n < nObjectives; n++) {
			for (int i = 0; i < childPopulationSize; i++) {
				C chromosome = populationPerObjective.get(n).getChromosomeByIndex(i);
				C mutated = chromosome.mutate();

				C otherChromosome = populationPerObjective.get(n).getRandomChromosome();
				List<C> crossovered = chromosome.crossover(otherChromosome);

				if (mutated != null)
					populationPerObjective.get(n).addChromosome(mutated);

				if (crossovered != null) {
					for (C c : crossovered) {
						if (c != null)
							populationPerObjective.get(n).addChromosome(c);
					}
				}
			}
			populationPerObjective.get(n)
					.reverseSortPopulationByFitness(new ChromosomesComparatorMO(EGAObjectives.values()[n]));
		}
		// newPopulation.sortPopulationByFitness(this.chromosomesComparator);
		// Check if some of the individuals has negative or zero consumption.
		int minPopTam = Integer.MAX_VALUE;
		for (int i = 0; i < nObjectives; i++) {
			if (populationPerObjective.get(i).getSize() < minPopTam) {
				minPopTam = populationPerObjective.get(i).getSize();
			}
		}
		int i = 0;
		while (i < minPopTam) {
			for (int n = 0; n < nObjectives; n++) {

				newPopulation.addChromosome(populationPerObjective.get(n).getChromosomeByIndex(i));
			}
			i++;
		}

		for (i = 0; i < newPopulation.getSize(); i++) {
			C chrom = newPopulation.getChromosomeByIndex(i);

			if (!chrom.isFitnessValid()) {
				// Delete from
				newPopulation.deleteChromosome(chrom);
				i--;
			}
		}

		double minPoint = Double.MAX_VALUE;
		double maxPoint = 0.0;
		// newPopulation.trim(parentPopulationSize);
		for (EGAObjectives obj : EGAObjectives.values()) {
			for (int i1 = 0; i1 < newPopulation.getSize(); i1++) {
				double value = newPopulation.getChromosomeByIndex(i1).getObjective(obj);
				if (value < minPoint) {
					minPoint = value;
				}
				if (value > maxPoint) {
					maxPoint = value;
				}
			}
			distanceComparator.addValueMin(obj, minPoint);
			distanceComparator.addValueMax(obj, maxPoint);
			minPoint = Double.MAX_VALUE;
			maxPoint = 0.0;
		}

		newPopulation.sortPopulationByFitness(distanceComparator);
		populationTestPrettyPrint(newPopulation);
		// newPopulation.trim(parentPopulationSize);
		newPopulation.trim(POPULATION_MAX_SIZE);
		this.population = newPopulation;
	}
	public T fitness(C chromosome) {
		return this.chromosomesComparator.fit(chromosome);
	}

	public void clearCache() {
		this.chromosomesComparator.clearCache();
	}
}
