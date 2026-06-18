package main.java;

/*******************************************************************************
 * Copyright (C) 2025 Pablo C. Cañizares
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
import algorithms.moga.MultiObjectiveGeneticAlgorithm;

import java.util.Arrays;

import algorithms.moga.EGAAlgorithms;
import algorithms.moga.GAObjectives;
import algorithms.moga.MOGA;
import algorithms.moga.NSGAII;
import algorithms.moga.NSGAII2;
import algorithms.moga.PAES;
import algorithms.moga.PAES2;
import algorithms.moga.SPEA2;
import algorithms.moga.SPEA3;
import algorithms.moga.VEGA;
import algorithms.moga.VEGA2;
import core.MOCloudOrchestrator;
import entities.MOCloudChromosome;

public class Cloud_MO extends MOCloudOrchestrator {

	EGAAlgorithms eSelectedAlgorithm;
	public Cloud_MO(String[] args, EGAAlgorithms eSelectedAlgorithm) {		
		this.eSelectedAlgorithm = eSelectedAlgorithm;
		this.doConfigure(args,eSelectedAlgorithm.toString());
	}

	public static void main(String[] args) {
		EGAAlgorithms eSelectedAlgorithm;
		MOCloudOrchestrator moGeneticAlgorithm;
		
		int numReRuns;
		
		if (args.length >= 8)
			numReRuns = Integer.parseInt(args[7]);
		else			
		    numReRuns = 1;
		
		//Selection of the algorithm to use
		eSelectedAlgorithm = EGAAlgorithms.valueOf(args[0]);
		
		for (int i =0;i<numReRuns;i++)
		{
			//Instantiation and configuration
			moGeneticAlgorithm = new Cloud_MO(Arrays.copyOfRange(args, 1, args.length), eSelectedAlgorithm);
			
			//Start evolution!
			moGeneticAlgorithm.doEvolution();
		}

	}

	@Override
	public MultiObjectiveGeneticAlgorithm<MOCloudChromosome, GAObjectives> instanceAlgorithm() {
		
		MultiObjectiveGeneticAlgorithm<MOCloudChromosome, GAObjectives> instanceAlgorithm;
		switch(this.eSelectedAlgorithm)
		{
		case eVEGA:
			instanceAlgorithm = new VEGA<MOCloudChromosome, GAObjectives>(super.getPopulation(), super.getNObjectives(), super.getFitness());
			break;
		case eVEGA2:
			instanceAlgorithm = new VEGA2<MOCloudChromosome, GAObjectives>(super.getPopulation(), super.getNObjectives(), super.getFitness());
			break;			
		case eSPEA2:
			instanceAlgorithm = new SPEA2<MOCloudChromosome, GAObjectives>(super.getPopulation(), super.getFitness(), 230, 1.5);
			break;
		case eSPEA3:
			instanceAlgorithm = new SPEA3<MOCloudChromosome, GAObjectives>(super.getPopulation(), super.getFitness(), 230, 1.5);
			break;			
		case ePAES:
			instanceAlgorithm = new PAES<MOCloudChromosome, GAObjectives>(super.getPopulation(), super.getFitness(), 8, super.getEvolutionLoops());
			break;
		case ePAES2:
			instanceAlgorithm = new PAES2<MOCloudChromosome, GAObjectives>(super.getPopulation(), super.getFitness(), 8, super.getEvolutionLoops());
			break;			
		case eMOGA:
			instanceAlgorithm = new MOGA<MOCloudChromosome, GAObjectives>(super.getPopulation(), super.getFitness());
			break;		
		case eNSGAII:
			instanceAlgorithm = new NSGAII<MOCloudChromosome, GAObjectives>(super.getPopulation(), super.getFitness(),2);
			break;
		case eNSGAII2:
			instanceAlgorithm = new NSGAII2<MOCloudChromosome, GAObjectives>(super.getPopulation(), super.getFitness(),2);
			break;			
		default:
			instanceAlgorithm = new VEGA<MOCloudChromosome, GAObjectives>(super.getPopulation(), super.getNObjectives(), super.getFitness());
			break;
		}
		
		return instanceAlgorithm;
	}
}