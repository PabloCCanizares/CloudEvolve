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
package algorithms;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import core.Chromosome;

public class GeneticAlgorithm<C extends Chromosome<C>, T extends Comparable<T>> {

	private static final int ALL_PARENTAL_CHROMOSOMES = Integer.MAX_VALUE;
	private static final int TOP10_PARENTAL_CHROMOSOMES = Integer.MAX_VALUE;
	private static final int POPULATION_MAX_SIZE = 10; 
	
	private class ChromosomesComparator implements Comparator<C> {

		private final Map<C, T> cache = new WeakHashMap<C, T>();

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
				fit = GeneticAlgorithm.this.fitnessFunc.calculate(chr);
				this.cache.put(chr, fit);
			}
			return fit;
		};

		public void clearCache() {
			this.cache.clear();
		}
	}

	private final ChromosomesComparator chromosomesComparator;

	private final Fitness<C, T> fitnessFunc;

	private Population<C> population;

	// listeners of genetic algorithm iterations (handle callback afterwards)
	private final List<IterationListener<C, T>> iterationListeners = new LinkedList<IterationListener<C, T>>();

	private boolean terminate = false;

	// number of parental chromosomes, which survive (and move to new
	// population)
	private int parentChromosomesSurviveCount = TOP10_PARENTAL_CHROMOSOMES;

	private int iteration = 0;

	public GeneticAlgorithm(Population<C> population, Fitness<C, T> fitnessFunc) {
		this.population = population;
		this.fitnessFunc = fitnessFunc;
		this.chromosomesComparator = new ChromosomesComparator();
		this.population.sortPopulationByFitness(this.chromosomesComparator);
	}

	public void evolve() {
		int parentPopulationSize = this.population.getSize();

		Population<C> newPopulation = new Population<C>();

		for (int i = 0; (i < parentPopulationSize) && (i < this.parentChromosomesSurviveCount); i++) {
			newPopulation.addChromosome(this.population.getChromosomeByIndex(i));
		}
		
		//Quitar por el random
		for (int i = 0; i < parentPopulationSize; i++) {
			C chromosome = this.population.getChromosomeByIndex(i);
			C mutated = chromosome.mutate();

			C otherChromosome = this.population.getRandomChromosome();
			List<C> crossovered = chromosome.crossover(otherChromosome);

			if(mutated != null)
				newPopulation.addChromosome(mutated);
			
			if(crossovered != null)
			{
				for (C c : crossovered) {
					if(c != null)
						newPopulation.addChromosome(c);
				}
			}
		}

		//newPopulation.sortPopulationByFitness(this.chromosomesComparator);
		//Check if some of the individuals has negative or zero consumption.
		
		newPopulation.reverseSortPopulationByFitness(this.chromosomesComparator);
		
		for(int i=0;i<newPopulation.getSize();i++)
		{
			C chrom = newPopulation.getChromosomeByIndex(i);
			
			if(!chrom.isFitnessValid())
			{
				//Delete from 
				newPopulation.deleteChromosome(chrom);
				i--;
			}
		}
		
		populationTestPrettyPrint(newPopulation);
		//newPopulation.trim(parentPopulationSize);
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
			this.iteration = i;
			for (IterationListener<C, T> l : this.iterationListeners) {
				//l.update(this);
			}
		}
	}

	public int getIteration() {
		return this.iteration;
	}

	public void terminate() {
		this.terminate = true;
	}

	public Population<C> getPopulation() {
		return this.population;
	}

	public C getBest() {
		
		if(this.population.getSize()>0)
			return this.population.getChromosomeByIndex(0);
		else
			return null;
	}

	public C getWorst() {
		return this.population.getChromosomeByIndex(this.population.getSize() - 1);
	}

	public void setParentChromosomesSurviveCount(int parentChromosomesCount) {
		this.parentChromosomesSurviveCount = parentChromosomesCount;
	}

	public int getParentChromosomesSurviveCount() {
		return this.parentChromosomesSurviveCount;
	}

	public void addIterationListener(IterationListener<C, T> listener) {
		this.iterationListeners.add(listener);
	}

	public void removeIterationListener(IterationListener<C, T> listener) {
		this.iterationListeners.remove(listener);
	}

	public T fitness(C chromosome) {
		return this.chromosomesComparator.fit(chromosome);
	}

	public void clearCache() {
		this.chromosomesComparator.clearCache();
	}
	
	public String populationPrettyPrint()
	{
		C chrom;
		String strRet;
		
		strRet = "[ "+this.population.getSize()+"| ";
		
		for(int i = 0; i<this.population.getSize();i++)
		{
			chrom = this.population.getChromosomeByIndex(i);
			strRet += chrom.toString();
		}
		strRet += "] ";
		
		return strRet;
	}
	private void populationTestPrettyPrint(Population<C> populationIn)
	{
		C chrom;
		String strRet;
		
		strRet = "[ "+populationIn.getSize()+"| ";
		
		for(int i = 0; i<populationIn.getSize();i++)
		{
			chrom = populationIn.getChromosomeByIndex(i);
			strRet += chrom.toString();
		}
		strRet += "] ";
		System.out.println("Printing previous population: "+strRet);
	}

	public LinkedList<Integer> getPopulationIds() {
		LinkedList<Integer> retList;
		int nIdIndex;
		C chrom;
		
		retList = new LinkedList<Integer>();
		for(int i = 0; i<this.population.getSize();i++)
		{
			chrom = this.population.getChromosomeByIndex(i);
			nIdIndex = chrom.getId();
			retList.add(nIdIndex);
		}
		
		return retList;
	}
}
