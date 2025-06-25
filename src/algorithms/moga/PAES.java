/*******************************************************************************
 * Copyright 2012 Yuriy Lagodiuk
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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import algorithms.Fitness;
import core.Chromosome;
import core.ChromosomeComparator;

/**
 * TODO: definir el tamaño del archivo desde los parámetros de entrada, también
 * se podría ver el número de celdas del archivo, aunque esto sería más
 * complicado...
 * 
 * La implementación del algoritmo se basa en el paper de Joshua D. Knowels
 * "Approximating the Nondominated Front Using the Pareto Archived Evolution
 * Strategy" DOI: 10.1162/106365600568167
 *
 * @param <C>
 * @param <T>
 */
public class PAES<C extends Chromosome<C>, T extends Comparable<T>>  extends MOGeneticAlgorithm <C,T>{

	protected class ChromosomesComparatorPAES implements ChromosomeComparator<C>{

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

		public boolean dominate(C c1, C c2) {
			return dominates(c1, c2);
		}

		private boolean dominates(C c1, C c2) {

			T fit1 = this.fit(c1);
			T fit2 = this.fit(c2);

			int ret = fit1.compareTo(fit2);
			return ret > 0;
		}

		public T fit(C chr) {
			T fit = this.cache.get(chr);
			if (fit == null) {
				fit = PAES.this.fitnessFunc.calculate(chr);
				this.cache.put(chr, fit);
			}
			return fit;
		};

		public void clearCache() {
			this.cache.clear();
		}

		public int compare(C solution, C oldSolution) {
			// TODO Auto-generated method stub
			T fit1 = this.fit(solution);
			T fit2 = this.fit(oldSolution);

			int ret = fit1.compareTo(fit2);
			return 0;
		}

	}
	private final ChromosomesComparatorPAES chromosomesComparator;
	private final CartesianDistanceComparator<C> distanceComparator;
	private final Fitness<C, T> fitnessFunc;

	// archivo con las soluciones no dominadas en ninguna generación
	// private AdaptiveGrid<C> archive;
	private AdaptiveCellGrid<C> archive;

	private int bisections;

	private PopulationMO<C> basePopulation;

	// size of the maximum solution that can be archives
	private int archiveSize;

	private boolean terminate = false;

	/**
	 * @param population
	 * @param fitnessFunc
	 */
	public PAES(PopulationMO<C> population, Fitness<C, T> fitnessFunc, int bisections, int archiveSize) {
		this.basePopulation = population;
		this.fitnessFunc = fitnessFunc;
		this.archiveSize = archiveSize;
		this.bisections = bisections;
		this.chromosomesComparator = new ChromosomesComparatorPAES();
		this.distanceComparator = new CartesianDistanceComparator<>();
		try {
			// this.archive = new AdaptiveGrid<C>(archiveSize, (int) Math.pow(2,
			// bisections), 2);
			this.archive = new AdaptiveCellGrid<C>(archiveSize, (int) Math.pow(2, bisections), 2);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (C chrom : this.basePopulation) {
			calculateSingleChromosome(chrom);
		}

	}

	// TODO: Esto esta encorsetado de tal manera que se llama a un comparador
	// Cosa que esta bien siempre que se necesite comparar de 1 a 1.
	// Para hacer MOGA o sustituir algoritmos, no me convence la verdad
	public void calculatePopulationFitness() {
		C chr;
		for (int i = 0; i < this.population.getSize(); i++) {
			chr = population.getChromosomeByIndex(i);
			fitnessFunc.calculate(chr);
		}
	}

	public void calculateSingleChromosome(C chrom) {
		fitnessFunc.calculate(chrom);
	}
	/**
	 * Se recibe la población actualizada y evaluada. En el archivo se encuentran
	 * las soluciones que no han sido dominadas hasta el momento.
	 * 
	 * El archivo de soluciones se hace en todo el dominio de los números reales
	 * 
	 */
	public void evolve() {

		//utilizar un bucle while y sólo cambia de indice cuando corresponda
		C mutated = null;
		C chrom = basePopulation.getChromosomeByIndex(this.iteration);
		archive.add(chrom);
		do {

			mutated = chrom.mutate();

			if (mutated != null) {
				calculateSingleChromosome(mutated);
				if (mutated.getObjective(EGAObjectives.eENERGY) > 0) {
					int dominated = isDominated(mutated, chrom);

					// The new chromosome is dominated by its parent
					if (dominated < 0) {
						continue;
					}
					// the parent is dominated by the mutated chromosome
					if (dominated > 0) {
						basePopulation.replace(chrom, mutated);
						// addToPopulation(mutated);
						archive.add(mutated);
						archive.remove(chrom);
						System.out.println("hasta aquí hemos llegado");
					} else { // Verify if the new chromosome is dominated by any solution in the archive
						addToArchive(chrom, mutated);
					}
				}
			}
		} while (mutated == null);

	}

	private void addToArchive(C chrom, C mutated) {
		// TODO Auto-generated method stub
		if (archive.getSize() < archiveSize) {
			if (archive.add(mutated))
				if (archive.getDensity(mutated) < archive.getDensity(chrom)) {
					basePopulation.deleteChromosome(chrom);
					basePopulation.addChromosome(mutated);
				}
		} else {
			if (archive.isInLessCrowdedRegion(chrom, mutated)) {
				if (archive.add(mutated))
					if (archive.getDensity(mutated) < archive.getDensity(chrom)) {
						basePopulation.deleteChromosome(chrom);
						basePopulation.addChromosome(mutated);
					}
			} else {
				if (archive.getDensity(mutated) < archive.getDensity(chrom)) {
					basePopulation.deleteChromosome(chrom);
					basePopulation.addChromosome(mutated);
				}
			}
		}
	}

	public void evolve(int count) {
		this.terminate = false;

		for (int i = 0; i < count; i++) {
			if (this.terminate) {
				break;
			}
			this.evolve();
			this.iteration = i;
			this.population = this.archive.getPopulation();

			double minimPoint = Double.MAX_VALUE;

			// newPopulation.trim(parentPopulationSize);
			double minPoint = Double.MAX_VALUE;
			double maxPoint = 0.0;
			// newPopulation.trim(parentPopulationSize);
			for (EGAObjectives obj : EGAObjectives.values()) {
				for (int i1 = 0; i1 < population.getSize(); i1++) {
					double value = population.getChromosomeByIndex(i1).getObjective(obj);
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
			this.population = this.archive.getPopulation();

			this.population.sortPopulationByFitness(distanceComparator);
			// this.population.sortPopulationByFitness(chromosomesComparator);
			for (IMOIterationListener<C, T> l : this.iterationListeners) {
				l.update(this);
			}
		}

	}

	public int isDominated(C c1, C c2) {
		int dominated = 0;
		for (EGAObjectives obj : EGAObjectives.values()) {
			if (c2.getObjective(obj) < c1.getObjective(obj)) {
				if (dominated > 0) {
					return 0;
				} else {
					dominated--;
				}
			} else if (c1.getObjective(obj) < c2.getObjective(obj)) {
				if (dominated < 0)
					dominated = 0;
				else
					dominated++;

			}
		}

		return dominated;

	}


	public T fitness(C chromosome) {
		return this.chromosomesComparator.fit(chromosome);
	}

	public void clearCache() {
		this.chromosomesComparator.clearCache();
	}

}
