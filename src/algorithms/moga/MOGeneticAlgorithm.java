package algorithms.moga;

import java.util.LinkedList;
import java.util.List;

import algorithms.Fitness;
import core.Chromosome;

//
public abstract class MOGeneticAlgorithm<C extends Chromosome<C>, T extends Comparable<T>> {
	protected PopulationMO<C> population;
	protected int iteration = 0;
	protected boolean terminate = false;
	protected int nObjectives;

	protected static final int POPULATION_MAX_SIZE = 10;
	protected static final int ALL_PARENTAL_CHROMOSOMES = Integer.MAX_VALUE;
	private static final int TOP10_PARENTAL_CHROMOSOMES = Integer.MAX_VALUE;
	protected Fitness<C, T> fitnessFunc;
	protected int parentChromosomesSurviveCount = TOP10_PARENTAL_CHROMOSOMES;
	protected final List<IMOIterationListener<C, T>> iterationListeners = new LinkedList<IMOIterationListener<C, T>>();

	
	public LinkedList<Integer> getPopulationIds() {
		LinkedList<Integer> retList;
		int nIdIndex;
		C chrom;

		retList = new LinkedList<Integer>();
		for (int i = 0; i < this.population.getSize(); i++) {
			chrom = this.population.getChromosomeByIndex(i);
			nIdIndex = chrom.getId();
			retList.add(nIdIndex);
		}

		return retList;
	}
	
	public String populationPrettyPrint() {
		C chrom;
		String strRet;

		strRet = "[ " + this.population.getSize() + "| ";

		for (int i = 0; i < this.population.getSize(); i++) {
			chrom = this.population.getChromosomeByIndex(i);
			strRet += chrom.toString();
		}
		strRet += "] ";

		return strRet;
	}

	public void populationTestPrettyPrint(PopulationMO<C> populationIn) {
		C chrom;
		String strRet;

		strRet = "[ " + populationIn.getSize() + "| ";

		for (int i = 0; i < populationIn.getSize(); i++) {
			chrom = populationIn.getChromosomeByIndex(i);
			strRet += chrom.toString();
		}
		strRet += "] ";
		System.out.println("Printing previous population: " + strRet);
	}
	

	public C getBest() {

		if (this.population.getSize() > 0)
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
	
	public int getIteration() {
		return this.iteration;
	}

	public void terminate() {
		this.terminate = true;
	}

	public PopulationMO<C> getPopulation() {
		return this.population;
	}

	public abstract void evolve();
	
	public void evolve(int count) {
		this.terminate = false;

		for (int i = 0; i < count; i++) {
			if (this.terminate) {
				break;
			}
			this.evolve();
			this.iteration = i;
			for (IMOIterationListener<C, T> l : this.iterationListeners) {
				l.update(this);
			}
		}
	}
	public double calculateManhattan(C o1, C o2) {
		double dist = 0.0;
		// Compare the objectives of two individuals
		Double dValue1, dValue2;
		for (EGAObjectives obj : EGAObjectives.values()) {

			// Printing all elements of a Map
			dValue1 = o1.getObjective(obj);
			System.out.println("C1: " + obj.toString() + " = " + dValue1);

			// Get objective
			dValue2 = o2.getObjective(obj);
			System.out.println("C2: " + obj.toString() + " = " + dValue2);
			dist += Math.abs(dValue1 - dValue2);
		}

		return dist;
	}
	public boolean isInPopulation(C chrom, PopulationMO<C> population) {
		boolean equal = true;

		for (int i = 0; i < population.getSize() && equal; i++) {
			C ind = population.getChromosomeByIndex(i);
			if (ind.equals(chrom)) {
				break;
			}
			for (EGAObjectives obj : EGAObjectives.values()) {
				if (ind.getObjective(obj) != chrom.getObjective(obj)) {
					equal = false;
				}
			}
		}
		return equal;
	}
	public void removeIterationListener(IMOIterationListener<C, T> listener) {
		this.iterationListeners.remove(listener);
	}


	public void addIterationListener(IMOIterationListener<C, T> listener) {
		this.iterationListeners.add(listener);		
	}


	public abstract T fitness(C best);
}
