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

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import algorithms.Fitness;
import core.Chromosome;
import core.ChromosomeComparator;

public class SPEA2<C extends Chromosome<C>, T extends Comparable<T>> extends MOGeneticAlgorithm <C,T>{

	private static final int POPULATION_MAX_SIZE = 10;

	private class ChromosomesComparatorSPEA implements ChromosomeComparator<C> {

		private final Map<C, T> cache = new WeakHashMap<C, T>();
		public int sort(List<C> chromosomes) {

			LinkedList<LinkedList<MOSolution<C, T>>> dominationFronts;
			LinkedList<MOSolution<C, T>> solutionList; // List for calculating dominations
			MOSolution<C, T> moSolIndex1, moSolIndex2;
			solutionList = new LinkedList<MOSolution<C, T>>();
			int nIndex1, nIndex2;

			nIndex1 = 0;

			dominationFronts = new LinkedList<LinkedList<MOSolution<C, T>>>();

			// Forma facil, rellenamos la lista de soluciones para facilitar el proceso:
			for (C c1 : chromosomes) {
				moSolIndex1 = new MOSolution<C, T>(c1, this.fit(c1));
				solutionList.add(nIndex1, moSolIndex1);
				nIndex1++;
			}
			nIndex1 = nIndex2 = 0;
			// Aqui iria el Pareto Frontier
			for (C c1 : chromosomes) {
				moSolIndex1 = solutionList.get(nIndex1);
				// Comparar con todos los cromosomas
				for (C c2 : chromosomes) {
					// Si C1 !=1 c2
					if (nIndex1 == nIndex2) {
						nIndex2++;
						continue;
					}
					// Ver si se dominan
					if (this.dominates(c1, c2)) {
						// Incluir c2, como dominado por c1
						moSolIndex2 = solutionList.get(nIndex2);
						moSolIndex1.insertDominatedSolutions(moSolIndex2);
					} else if (this.dominates(c2, c1)) {
						// Incluir c1 como dominado por c2

						// Sumar 1, a c1 veces dominado
						moSolIndex1.incrementDominations();
					}

					nIndex2++;
				}
				nIndex2 = 0;

				// Si C1 ha sido dominado 0 veces, forma parte del pareto
				if (moSolIndex1.getDominations() == 0) {
					moSolIndex1.setRank(1);
					if (dominationFronts.isEmpty()) {
						LinkedList<MOSolution<C, T>> firstDominationFront = new LinkedList<MOSolution<C, T>>();
						firstDominationFront.add(moSolIndex1);
						dominationFronts.add(firstDominationFront);
					} else {
						LinkedList<MOSolution<C, T>> firstDominationFront = dominationFronts.getFirst();
						firstDominationFront.add(moSolIndex1);
					}

				}
				moSolIndex1.getIndividual().setDominated(moSolIndex1.getDominations());

				nIndex1++;
			}

			double dist = 0;
			for (C chrom : chromosomes) {
				double density = 0.0;
				for (C neigh : chromosomes) {
					if (chrom.equals(neigh))
						continue;

					dist = calculateManhattan(chrom, neigh);

					if (dist <= radius) {
						density += 1.0 - Math.pow(dist / radius, densityPow);
					}
				}
				chrom.setnCrowdDensity(density);
				chrom.setFitness();
			}


			// Finalmente, tomamos los cromosomas con rank=1;
			chromosomes.clear();

			int i = 1;
			while (dominationFronts.size() == i) {
				LinkedList<MOSolution<C, T>> nextDominationFront = new LinkedList<MOSolution<C, T>>();
				for (MOSolution<C, T> individualP : dominationFronts.get(i - 1)) {
					LinkedList<MOSolution<C, T>> dominatedIndividualList = individualP.getDominatedIndividuals();
					for (MOSolution<C, T> individualQ : dominatedIndividualList) {
						// Restamos 1 al número de dominaciones
						// individual2NumberOfDominatingIndividuals.put(individualQ,
						// individual2NumberOfDominatingIndividuals.get(individualQ) - 1);
						individualQ.decrementDominations();

						if (individualQ.getDominations() == 0) {
							individualQ.setRank(i + 1);
							nextDominationFront.add(individualQ);
						}
					}
				}
				i++;
				if (!nextDominationFront.isEmpty()) {
					dominationFronts.add(nextDominationFront);
				}
			}

			// Una vez tenemos el pareto construido, aplanarlo y añadirlo a soluciones.
			// new LinkedList<LinkedList<MOSolution<C, T>>>();
			for (LinkedList<MOSolution<C, T>> domFrontI : dominationFronts) {
				for (MOSolution<C, T> domIndiv : domFrontI) {
					chromosomes.add(domIndiv.getIndividual());
				}
			}

			return 1;
		}

		private boolean dominates(C c1, C c2) {

			T fit1 = this.fit(c1);
			T fit2 = this.fit(c2);

			int ret = fit1.compareTo(fit2);
			int ret2 = fit2.compareTo(fit1);
			return ret > ret2;
		}

		public T fit(C chr) {
			T fit = this.cache.get(chr);
			if (fit == null) {
				fit = SPEA2.this.fitnessFunc.calculate(chr);
				this.cache.put(chr, fit);
			}
			return fit;
		};

		public void clearCache() {
			this.cache.clear();
		}
	}
	private CartesianDistanceComparator<C> distanceComparator;
	private final ChromosomesComparatorSPEA chromosomesComparator;
	private final FitnessComparator fc;
	private PopulationMO<C> archive;
	private double radius;
	private double densityPow;
	private class FitnessComparator implements Comparator<C> {

		@Override
		public int compare(C o1, C o2) {
			if (o1.getFitness() < o2.getFitness())
				return -1;
			else if (o1.getFitness() == o2.getFitness())
				return 0;
			else
				return 1;
		}

	}	

	/**
	 * 
	 * @param population
	 * @param fitnessFunc
	 * @param radius:     represents de radius of influence to calculate the
	 *                    crowding density of each solution.
	 * @param densityPow: represents de pow used to calculate de density of
	 *                    solutions. This value represents the importance of the
	 *                    distance when calculating the density. Value between 1 or
	 *                    2 means distance is more lineal. Value between 3 or 4
	 *                    means distance has a non lineal effect when calculating
	 *                    the density.
	 * 
	 */
	public SPEA2(PopulationMO<C> population, Fitness<C, T> fitnessFunc, double radius, double densityPow) {
		this.population = population;
		this.archive = new PopulationMO<C>();
		this.fc = new FitnessComparator();
		this.fitnessFunc = fitnessFunc;
		this.chromosomesComparator = new ChromosomesComparatorSPEA();
		this.radius = radius;
		this.densityPow = densityPow;
		this.population.sortPopulationByFitness(this.chromosomesComparator);
		for (C chrom : this.population)
			if (chrom.getNumDom() == 0.0)
				this.archive.addChromosome(chrom);
		this.archive.trim(POPULATION_MAX_SIZE);
		this.distanceComparator = new CartesianDistanceComparator<>();

	}
	/**
	 * Para la primera iteración los individuos ya tienen la dominancia calculada.
	 * Por lo que el paso de evaluación ya está hecho.
	 * 
	 * A continuación hay que calcular la aptitud para cada solución que se suma la
	 * densidad y el número de soluciones que dominan a cada solución
	 * 
	 * Del archivo externo se seleccionan padres que tengan una aptitud superior a
	 * la media para llevar a cabo la mutación y el cruce.
	 * 
	 * Operadores genéticos...
	 * 
	 * Cálculo del frente de pareto de nuevo para unir ambas poblaciones.
	 * 
	 * Se seleccionan las mejores soluciones del frente de pareto para actualizar el
	 * archivo externo
	 * 
	 */
	public void evolve() {

		int parentPopulationSize = this.population.getSize();

		PopulationMO<C> newPopulation = new PopulationMO<C>();

		for (int i = 0; (i < parentPopulationSize) && (i < this.parentChromosomesSurviveCount); i++) {
			newPopulation.addChromosome(this.population.getChromosomeByIndex(i));
		}

		// Quitar por el random
		/**
		 * Aptitud, se realiza el cálculo de la densidad de cada solución además de
		 * identificar el número de soluciones dominadas y que dominan a cada solución.
		 */
		double dist = 0;

		for (int i = 0; i < archive.getSize(); i++) {
			C chromosome = this.archive.getChromosomeByIndex(i);
			C mutated = chromosome.mutate();

			C otherChromosome = this.archive.getRandomChromosome();
			List<C> crossovered = chromosome.crossover(otherChromosome);

			if (mutated != null)
				newPopulation.addChromosome(mutated);

			if (crossovered != null) {
				for (C c : crossovered) {
					if (c != null)
						newPopulation.addChromosome(c);
				}
			}
		}

		newPopulation.sortPopulationByFitness(this.chromosomesComparator);

		for (int i = 0; i < newPopulation.getSize(); i++) {
			C chrom = newPopulation.getChromosomeByIndex(i);

			if (!chrom.isFitnessValid()) {
				// Delete from
				newPopulation.deleteChromosome(chrom);
				i--;
			}
		}

		updateArchive(newPopulation);
		populationTestPrettyPrint(newPopulation);

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

		newPopulation.trim(POPULATION_MAX_SIZE * 2);
		this.population = newPopulation;
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
