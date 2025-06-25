package algorithms.moga;

import core.Chromosome;

public interface IMOIterationListener<C extends Chromosome<C>, T extends Comparable<T>> {
	void update(MOGeneticAlgorithm<C, T> environment);
}