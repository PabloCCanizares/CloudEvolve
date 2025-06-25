package core;

import algorithms.moga.MOGeneticAlgorithm;
import algorithms.moga.GAObjectives;
import algorithms.moga.IMOIterationListener;
import configuration.EAController;
import entities.MOCloudChromosome;

public class MOIteratorListener<C extends Chromosome<C>, T extends Comparable<T>>{
	
	public void update(MOGeneticAlgorithm<MOCloudChromosome, GAObjectives> algorithm) {

		MOCloudChromosome best = algorithm.getBest();
		int iteration = algorithm.getIteration();
		if (best != null) {
			// CloudChromosome best = ga.getWorst();
			GAObjectives bestFit = algorithm.fitness(best);

			// Listener prints best achieved solution
			System.out.println(String.format("%s\t%s\t%s | Id: %d", iteration, bestFit, best, best.getId()));
			System.out.println(algorithm.populationPrettyPrint());
			System.out.println("============================================");
			// If fitness is satisfying - we can stop Genetic algorithm
			// if (bestFit < this.threshold) {
			// ga.terminate();
			// }
		} else {
			System.out.println("WARNING! The population vector is empty!!");
			System.out.println("============================================");
		}

		EAController.getInstance().createNewIteration(iteration, algorithm.getPopulationIds());
		EAController.getInstance().incIterationIndex();
	}

}
