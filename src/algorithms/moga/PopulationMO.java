/*******************************************************************************
 * Copyright (C) 2022 Pablo C. Cañizares
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import algorithms.Chromosome;
import algorithms.ChromosomeComparator;

public class PopulationMO<C extends Chromosome<C>> implements Iterable<C> {

	private static final int DEFAULT_NUMBER_OF_CHROMOSOMES = 32;

	private List<C> chromosomes = new ArrayList<C>(DEFAULT_NUMBER_OF_CHROMOSOMES);

	private final Random random = new Random();

	public void addChromosome(C chromosome) {
		this.chromosomes.add(chromosome);
	}

	public void deleteChromosome(C chromosome) {
		this.chromosomes.remove(chromosome);
	}

	public int getSize() {
		return this.chromosomes.size();
	}

	public C getRandomChromosome() {
		int numOfChromosomes = this.chromosomes.size();
		// TODO improve random generator
		// maybe use pattern strategy ?
		int indx = this.random.nextInt(numOfChromosomes);
		return this.chromosomes.get(indx);
	}

	public C getChromosomeByIndex(int indx) {		
		return this.chromosomes.size() > indx ? this.chromosomes.get(indx) : null;
	}

	public void sortPopulationByFitness(ChromosomeComparator<C> chromosomesComparator) {

		C chr;
		// TODO: Pareto frontier

		// First of all, run all the
		Collections.shuffle(this.chromosomes);
		chromosomesComparator.sort(this.chromosomes);
	}

	public void reverseSortPopulationByFitness(Comparator<C> chromosomesComparator) {
		Collections.shuffle(this.chromosomes);
		Collections.sort(this.chromosomes, chromosomesComparator);
		Collections.reverse(this.chromosomes);
	}

	/**
	 * shortening population till specific number
	 */
	public void trim(int len) {

		if (this.chromosomes.size() > len)
			this.chromosomes = this.chromosomes.subList(0, len);
	}

	@Override
	public Iterator<C> iterator() {
		return this.chromosomes.iterator();
	}

	public void sortPopulationByFitness(Comparator<C> chromosomesComparator) {
		// TODO Auto-generated method stub
		Collections.shuffle(this.chromosomes);
		Collections.sort(this.chromosomes, chromosomesComparator);
	}

	public void replace(C chrom, C mutated) {
		// TODO Auto-generated method stub
		int index = this.chromosomes.indexOf(chrom);
		this.chromosomes.remove(chrom);
		this.chromosomes.add(index, mutated);
	}
}
