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
package core;

import java.util.List;

import algorithms.moga.EGAObjectives;


public interface Chromosome<C extends Chromosome<C>> {

	List<C> crossover(C anotherChromosome);

	C mutate();

	boolean isFitnessValid();

	int getId();

	double getObjective(EGAObjectives egaObjectives);

	void setDominated(int dominations);

	void setnCrowdDensity(double density);

	void setFitness();

	double getNumDom();

	double getFitness();

	void setCrowdingDistance(double distance);

	void addToCrowdingDistance(double normalizedDistance);

	double getCrowdingDistance();

	int getRank();

	void setRank(int nRank);

	C dup();

	int[] getObjectivesIndex();
}
