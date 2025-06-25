package algorithms.moga;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import algorithms.Fitness;
import core.Chromosome;
import core.ChromosomeComparator;

public class MOGA<C extends Chromosome<C>, T extends Comparable<T>>  extends MOGeneticAlgorithm <C,T>{


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
			// TODO: Ojo aqui.
			// Aqui hay que empezar a codificar el Pareto Frontier, siguiendo el allgoritmo
			// de
			// http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.542.385&rep=rep1&type=pdf

			// Podemos tomar por referencia:
			// https://github.com/jrivera777/NSGAII/blob/master/src/NSGAII/NSGA2.java
			/*
			 * T fit1 = this.fit(chr1); T fit2 = this.fit(chr2); int ret =
			 * fit1.compareTo(fit2);
			 */

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
			// TODO: Construir el frente de pareto del todo, y ordenarlos por el frente
			// TODO: Cuidado con los -1 de las soluciones inválidas!
			/*
			 * for(MOSolution<C, T> moSol: solutionList) { if(moSol.getRank() == 1) {
			 * chromosomes.add(moSol.getIndividual()); } }
			 */

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
				fit = MOGA.this.fitnessFunc.calculate(chr);
				this.cache.put(chr, fit);
			}
			return fit;
		};

		public void clearCache() {
			this.cache.clear();
		}
	}

	private final ChromosomesComparatorMO chromosomesComparator;
	private CartesianDistanceComparator<C> distanceComparator;
	private final Fitness<C, T> fitnessFunc;
	private PopulationMO<C> population;

	public MOGA(PopulationMO<C> population, Fitness<C, T> fitnessFunc) {
		this.population = population;
		this.fitnessFunc = fitnessFunc;
		this.chromosomesComparator = new ChromosomesComparatorMO();
		this.distanceComparator = new CartesianDistanceComparator<>();
		this.population.sortPopulationByFitness(this.chromosomesComparator);
	}


	public void evolve() {
		int parentPopulationSize = this.population.getSize();

		PopulationMO<C> newPopulation = new PopulationMO<C>();

		for (int i = 0; (i < parentPopulationSize) && (i < this.parentChromosomesSurviveCount); i++) {
			newPopulation.addChromosome(this.population.getChromosomeByIndex(i));
		}

		// Quitar por el random
		for (int i = 0; i < parentPopulationSize; i++) {
			C chromosome = this.population.getChromosomeByIndex(i);
			C mutated = chromosome.mutate();

			C otherChromosome = this.population.getRandomChromosome();
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
		// Check if some of the individuals has negative or zero consumption.

		// newPopulation.reverseSortPopulationByFitness(this.chromosomesComparator);

		for (int i = 0; i < newPopulation.getSize(); i++) {
			C chrom = newPopulation.getChromosomeByIndex(i);

			if (!chrom.isFitnessValid()) {
				// Delete from
				newPopulation.deleteChromosome(chrom);
				i--;
			}
		}
	
		populationTestPrettyPrint(newPopulation);

		// newPopulation.trim(parentPopulationSize);
		newPopulation.trim(POPULATION_MAX_SIZE);
		this.population = newPopulation;
	}

	public void evolve(int count) {
		this.terminate = false;

		for (int i = 0; i < count; i++) {
			if (this.terminate) {
				break;
			}
			this.evolve();
			double minPoint = Double.MAX_VALUE;
			double maxPoint = 0.0;
			// newPopulation.trim(parentPopulationSize);
			for (EGAObjectives obj : EGAObjectives.values()) {
				for (int i1 = 0; i1 < this.population.getSize(); i1++) {
					double value = this.population.getChromosomeByIndex(i1).getObjective(obj);
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

			this.population.sortPopulationByFitness(distanceComparator);
			this.iteration = i;
			for (IMOIterationListener<C, T> l : this.iterationListeners) {
				l.update(this);
			}
		}
	}
	@Override
	public T fitness(C chromosome) {
		return this.chromosomesComparator.fit(chromosome);
	}

	public void clearCache() {
		this.chromosomesComparator.clearCache();
	}
}
