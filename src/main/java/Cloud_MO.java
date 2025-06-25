package main.java;

/*******************************************************************************
 * Copyright 2025 Pablo C. Cañizares
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
import algorithms.moga.MOGeneticAlgorithm;
import algorithms.moga.EGAAlgorithms;
import algorithms.moga.GAObjectives;
import algorithms.moga.MOGA;
import algorithms.moga.NSGAII;
import algorithms.moga.PAES;
import algorithms.moga.SPEA2;
import algorithms.moga.VEGA;
import core.MOCloudOrchestrator;
import entities.MOCloudChromosome;

public class Cloud_MO extends MOCloudOrchestrator {

	EGAAlgorithms eSelectedAlgorithm;
	public Cloud_MO(String[] args, EGAAlgorithms eSelectedAlgorithm) {		
		this.eSelectedAlgorithm = eSelectedAlgorithm;
		this.doConfigure(args);
	}

	public static void main(String[] args) {
		EGAAlgorithms eSelectedAlgorithm;
		MOCloudOrchestrator moGeneticAlgorithm;
		
		//Selection of the algorithm to use
		eSelectedAlgorithm = EGAAlgorithms.eNSGAII;
		
		//Instantiation and configuration
		moGeneticAlgorithm = new Cloud_MO(args, eSelectedAlgorithm);
		
		//Start evolution!
		moGeneticAlgorithm.doEvolution();
	}

	@Override
	public MOGeneticAlgorithm<MOCloudChromosome, GAObjectives> instanceAlgorithm() {
		
		MOGeneticAlgorithm<MOCloudChromosome, GAObjectives> instanceAlgorithm;
		switch(this.eSelectedAlgorithm)
		{
		case eVEGA:
			instanceAlgorithm = new VEGA<MOCloudChromosome, GAObjectives>(super.getPopulation(), super.getNObjectives(), super.getFitness());
			break;
		case eSPEA2:
			instanceAlgorithm = new SPEA2<MOCloudChromosome, GAObjectives>(super.getPopulation(), super.getFitness(), 230, 1.5);
			break;
		case ePAES:
			instanceAlgorithm = new PAES<MOCloudChromosome, GAObjectives>(super.getPopulation(), super.getFitness(), 8, super.getEvolutionLoops());
			break;
		case eMOGA:
			instanceAlgorithm = new MOGA<MOCloudChromosome, GAObjectives>(super.getPopulation(), super.getFitness());
			break;		
		case eNSGAII:
			instanceAlgorithm = new NSGAII<MOCloudChromosome, GAObjectives>(super.getPopulation(), super.getFitness(),2);
			break;
		default:
			instanceAlgorithm = new VEGA<MOCloudChromosome, GAObjectives>(super.getPopulation(), super.getNObjectives(), super.getFitness());
			break;
		}
		
		return instanceAlgorithm;
	}
}