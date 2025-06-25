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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import algorithms.Fitness;
import core.Chromosome;
import core.ChromosomeComparator;

public class NSGAII<C extends Chromosome<C>, T extends Comparable<T>>  extends MOGeneticAlgorithm <C,T> {


	private class ChromosomesComparatorMO implements ChromosomeComparator<C> {

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
					if (nIndex1 != nIndex2) {
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
				nIndex1++;
			}

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
			int j = 0;
			while (j < dominationFronts.size()) {
				calculateCrowdingDistance(dominationFronts.get(j));
				j++;
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

		private void calculateCrowdingDistance(LinkedList<MOSolution<C, T>> dominationFront) {
			MOSolution<C, T>[] sortedSols = new MOSolution[dominationFront.size()];
			for (int k = 0; k < dominationFront.size(); k++) {
				dominationFront.get(k).setCrowdingDistance(0);
				sortedSols[k] = dominationFront.get(k);
			}

			int numobjectives = EGAObjectives.values().length;
			for (int m = 0; m < numobjectives; m++) {
				EGAObjectives objective = EGAObjectives.values()[m];

				Arrays.sort(sortedSols, Comparator.comparingDouble(s -> s.getObjective(objective)));
				sortedSols[0].setCrowdingDistance(Double.POSITIVE_INFINITY);
				sortedSols[sortedSols.length - 1].setCrowdingDistance(Double.POSITIVE_INFINITY);

				if (sortedSols[0].getObjective(objective) != sortedSols[sortedSols.length - 1]
						.getObjective(objective)) {
					for (int i = 1; i < sortedSols.length - 1; i++) {
						double newCrowdingDistance = sortedSols[i].getCrowdingDistance();
						newCrowdingDistance += (sortedSols[i + 1].getObjective(objective)
								- sortedSols[i - 1].getObjective(objective))
								/ (sortedSols[sortedSols.length - 1].getObjective(objective)
										- sortedSols[0].getObjective(objective));
						sortedSols[i].setCrowdingDistance(newCrowdingDistance);
					}
				}
			}
			// System.out.println("Crowding Calculate");
		}

		private boolean dominates(C c1, C c2) {

			T fit1 = this.fit(c1);
			T fit2 = this.fit(c2);
			if (c1.getObjective(EGAObjectives.eENERGY) < 0)
				return false;
			if (c2.getObjective(EGAObjectives.eENERGY) < 0)
				return true;
			int ret = fit1.compareTo(fit2);
			int ret2 = fit2.compareTo(fit1);

			return ret > ret2;
		}

		public T fit(C chr) {
			T fit = this.cache.get(chr);
			if (fit == null) {
				fit = NSGAII.this.fitnessFunc.calculate(chr);
				this.cache.put(chr, fit);
			}
			return fit;
		};

		public void clearCache() {
			this.cache.clear();
		}
	}

	private int numObjectives;
	private final ChromosomesComparatorMO chromosomesComparator;	
	private CartesianDistanceComparator<C> distanceComparator;

	public NSGAII(PopulationMO<C> population, Fitness<C, T> fitnessFunc, int numObjectives) {
		this.population = population;
		this.fitnessFunc = fitnessFunc;
		this.chromosomesComparator = new ChromosomesComparatorMO();
		this.numObjectives = numObjectives;
		// primero se ordena por frentes de dominación
		this.population.sortPopulationByFitness(this.chromosomesComparator);
		this.distanceComparator = new CartesianDistanceComparator<C>();
	}


	/**
	 * Generate a random number between [min, max) always if min < max
	 */
	private List<Integer> randomNumberGenerator(int min, int max) {
		List<Integer> num = new ArrayList<Integer>();
		for (int i = min; i < max; i++) {
			num.add(i);
		}
		Collections.shuffle(num, new Random());
		if (num.size() < 4)
			System.out.println("Problemas");
		return num.subList(0, 2);
	}

	public void evolve() {
		int parentPopulationSize = this.population.getSize();
		PopulationMO<C> newPopulation = new PopulationMO<C>();

		List<C> p1 = new ArrayList<C>(parentPopulationSize);
		List<C> p2 = new ArrayList<C>(parentPopulationSize);

		for (int i = 0; (i < parentPopulationSize) && (i < this.parentChromosomesSurviveCount); i++) {
			p1.add(i, this.population.getChromosomeByIndex(i));
			p2.add(i, this.population.getChromosomeByIndex(i));
		}

		// Quitar por el random
		// shuffle both populations
		for (int i = 0; i < parentPopulationSize; i++) {
			int randomIndex = randomNumberGenerator(0, p1.size()).get(0);
			if (randomIndex != -1) {
				C temp = p1.get(randomIndex);
				p1.set(randomIndex, p1.get(i));
				p1.set(i, temp);
			}
			randomIndex = randomNumberGenerator(0, p2.size()).get(0);
			if (randomIndex != -1) {
				C temp = p2.get(randomIndex);
				p2.set(randomIndex, p2.get(i));
				p2.set(i, temp);
			}
		}
		for (int i = 0; i < parentPopulationSize; i++) {
			List<Integer> randomNumber = randomNumberGenerator(0, p1.size());
			C parent1 = binaryTournament(p1.get(i), p1.get(randomNumber.get(0)));
			C parent2 = binaryTournament(p1.get(i), p1.get(randomNumber.get(1)));

			C child1 = parent1.dup();
			C child2 = parent2.dup();
			C mutate1 = child1.mutate();
			C mutate2 = child2.mutate();

			if (mutate1 != null)
				newPopulation.addChromosome(mutate1);
			if (mutate2 != null)
				newPopulation.addChromosome(mutate2);
			List<C> crossovered = child1.crossover(child2);
			if (crossovered != null) {
				for (C c : crossovered) {
					if (c != null)
						newPopulation.addChromosome(c);
				}
			}

			randomNumber = randomNumberGenerator(0, p2.size());

			parent1 = binaryTournament(p2.get(i), p2.get(randomNumber.get(0)));
			parent2 = binaryTournament(p2.get(i), p2.get(randomNumber.get(1)));

			child1 = parent1.dup();
			child2 = parent2.dup();
			mutate1 = child1.mutate();
			mutate2 = child2.mutate();

			if (mutate1 != null)
				newPopulation.addChromosome(mutate1);
			if (mutate2 != null)
				newPopulation.addChromosome(mutate2);

			crossovered = child1.crossover(child2);
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
			if (!chrom.isFitnessValid() || chrom.getRank() > 2) {
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

		newPopulation.trim(POPULATION_MAX_SIZE);

		this.population = newPopulation;
	}

	private C binaryTournament(C c, C c2) {
		// First individual is better than the second one
		if (c.getRank() < c2.getRank()) {
			return c;
		}
		if (c.getRank() == c2.getRank() && c.getCrowdingDistance() > c2.getCrowdingDistance()) {
			return c;
		}
		// Second individual is better than the second one

		if (c2.getRank() < c.getRank()) {
			return c2;
		}
		if (c2.getRank() == c.getRank() && c2.getCrowdingDistance() > c.getCrowdingDistance()) {
			return c2;
		}
		if (Math.random() < 0.5)
			return c;
		else
			return c2;
	}

	public T fitness(C chromosome) {
		return this.chromosomesComparator.fit(chromosome);
	}

	public void clearCache() {
		this.chromosomesComparator.clearCache();
	}

}
