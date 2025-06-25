package algorithms.moga;

import java.util.LinkedList;

import core.Chromosome;

public class MOSolution<C extends Chromosome, T extends Comparable<T>> {

	C chromosome;
	T fit;

	int nRank;
	int nDominations;

	LinkedList<MOSolution<C, T>> dominatedSolutions;

	public MOSolution(C chromosome, T fit) {
		dominatedSolutions = new LinkedList<MOSolution<C, T>>();
		this.chromosome = chromosome;
		this.fit = fit;
	}

	public void incrementDominations() {
		nDominations++;
	}

	public void insertDominatedSolutions(MOSolution<C, T> dominated) {
		if (!dominatedSolutions.contains(dominated)) 
			dominatedSolutions.add(dominated);

	}

	public int getDominations() {
		return this.nDominations;
	}

	public void setRank(int nRank) {
		chromosome.setRank(nRank);
		this.nRank = nRank;
	}

	public int getRank() {
		return this.nRank;
	}

	public C getIndividual() {
		return this.chromosome;
	}

	public LinkedList<MOSolution<C, T>> getDominatedIndividuals() {
		return dominatedSolutions;
	}

	public void decrementDominations() {
		this.nDominations--;
	}

	public double getObjective(EGAObjectives egaObjectives) {
		// TODO Auto-generated method stub
		return chromosome.getObjective(egaObjectives);
	}

	public void setCrowdingDistance(double distance) {
		// TODO Auto-generated method stub
		chromosome.setCrowdingDistance(distance);
	}

	public void addToCrowdingDistance(double normalizedDistance) {
		// TODO Auto-generated method stub
		chromosome.addToCrowdingDistance(normalizedDistance);
	}

	public double getCrowdingDistance() {
		// TODO Auto-generated method stub
		return chromosome.getCrowdingDistance();
	}

	public C getChromosome() {
		// TODO Auto-generated method stub
		return chromosome;
	}

	public T getFit() {
		// TODO Auto-generated method stub
		return fit;
	}

}
