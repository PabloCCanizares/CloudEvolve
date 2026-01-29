package algorithms.moga;

import algorithms.Chromosome;

public interface IMOIterationListener<C extends Chromosome<C>, T extends Comparable<T>> {
	void update(MultiObjectiveGeneticAlgorithm<C, T> environment);
}