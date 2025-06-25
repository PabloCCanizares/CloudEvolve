package main.java;


/*******************************************************************************
 * Copyright 2018 Pablo C. Cañizares
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
import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import transformations.TestCase2Cloud;
import algorithms.Fitness;
import algorithms.GeneticAlgorithm;
import algorithms.IterationListener;
import algorithms.Population;
import algorithms.moga.EGAObjectives;
import auxiliar.CommandExecutor;
import configuration.EAController;
import configuration.LogLevel;
import core.Chromosome;
import dataParser.TestCase;
import dataParser.TestCaseInput;
import dataParser.TestCaseParser;
import dataParser.cloud.ECloudSimulator;
import dataParser.cloud.TestCaseParser_cloud;
import dataParser.cloud.input.TcInput_cloud;
import dataParser.metadata.MetaParser;
import dataParser.metadata.MetaTestCase;
import entities.cloud.Cloud;
import graphs.GAGraph;
import graphs.GraphGenerator;
import executor.MT_Handler;
import mutation.MutableCloud.MutableCloud;

public class Cloud_GA extends Thread {

	final static String HISTOGRAM_DAT_TAG = "histogram.dat";
	final static String HISTOGRAM_GNU_TAG = "histogram.gnu";
	final static String EVOLUTION_DAT_TAG = "evolution.dat";
	final static String EVOLUTION_GNU_TAG = "evolution.gnu";
	private String[] args;

	public Cloud_GA(String[] arg) {
		this.args = arg;
	}

	@Override
	public void run() {
		start(this.args);
	}

	public static void main(String[] args) {

		String strPathBase, strInitialPopulationPath, strSim;
		ECloudSimulator eSimulator;
		int nProbBase, nRuleBase, nEvolutionLoops;

		nProbBase = nRuleBase = 0;
		strPathBase = null;
		nEvolutionLoops = 250;
		if (args.length >= 2) {
			strSim = args[0];
			strInitialPopulationPath = args[1] + File.separator + "metaInfo";

			// TODO: Comprobar que el simulador existe o salimos
			if (strSim.indexOf("cloudsimstorage") != -1)
				eSimulator = ECloudSimulator.eCLOUDSIMSTORAGE;
			else if (strSim.indexOf("simgrid") != -1)
				eSimulator = ECloudSimulator.eSIMGRID;
			else
				eSimulator = ECloudSimulator.eCLOUDSIMSTORAGE;

			if (args.length >= 3) {
				nEvolutionLoops = Integer.parseInt(args[2]);
				System.out.printf("Selected evolution loops: %d\n", nEvolutionLoops);
				if (args.length >= 5) {
					nProbBase = Integer.parseInt(args[3]);
					nRuleBase = Integer.parseInt(args[4]);
					System.out.printf("Selected probability & rule base: %d - %d\n", nProbBase, nRuleBase);
					if (args.length >= 6)
						strPathBase = args[5].concat("/GA/");
				}
			}

		} else {
			strPathBase = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary";
			// Siguientes pasos, añadir el tipo
			eSimulator = ECloudSimulator.eCLOUDSIMSTORAGE;
			// Esto es lo que tenemos que cambiar con el input del programa.
			strInitialPopulationPath = strPathBase + File.separator + "InitialPopulation/sample" + File.separator
					+ "metaInfo";
		}
		if (strPathBase == null) {
			switch (eSimulator) {
			case eCLOUDSIMSTORAGE:
				strPathBase = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary";
				break;
			case eSIMGRID:
				strPathBase = "/localSpace/cloudEnergy/simGrid/evolutionary";
				break;
			default:
				strPathBase = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary";
			}
		}

		/**
		 * TODO: Descomentando esto se prueba el mismo caso de prueba con distintos
		 * niveles de probabilidad
		 */

		for (int nProbLevel = nProbBase; nProbLevel < 3; nProbLevel++) {
			// for (int nRuleLevel = nRuleBase; nRuleLevel < 2; nRuleLevel++) {
			int nRuleLevel = 1;
			doEvolution(eSimulator, strPathBase, strInitialPopulationPath, nEvolutionLoops, nProbLevel, nRuleLevel);
			
		}

		// TODO: DANGER! Cambio para puebas de la segunda revision
		// for(int i=0;i<30;i++)
//		for (int i = 0; i < 1; i++) {
//			doEvolution(eSimulator, strPathBase, strInitialPopulationPath, nEvolutionLoops, nProbBase, nRuleBase);
//		}

	}

	public void start(String[] args) {

		String strPathBase, strInitialPopulationPath, strSim;
		ECloudSimulator eSimulator;
		int nProbBase, nRuleBase, nEvolutionLoops;

		nProbBase = nRuleBase = 0;
		strPathBase = null;
		nEvolutionLoops = 250;
		if (args.length >= 2) {
			strSim = args[0];
			strInitialPopulationPath = args[1] + File.separator + "metaInfo";

			// TODO: Comprobar que el simulador existe o saliendo
			if (strSim.indexOf("cloudsimstorage") != -1)
				eSimulator = ECloudSimulator.eCLOUDSIMSTORAGE;
			else if (strSim.indexOf("simgrid") != -1)
				eSimulator = ECloudSimulator.eSIMGRID;
			else
				eSimulator = ECloudSimulator.eCLOUDSIMSTORAGE;

			if (args.length >= 3) {
				nEvolutionLoops = Integer.parseInt(args[2]);
				System.out.printf("Selected evolution loops: %d\n", nEvolutionLoops);
				if (args.length >= 5) {
					nProbBase = Integer.parseInt(args[3]);
					nRuleBase = Integer.parseInt(args[4]);
					System.out.printf("Selected probability & rule base: %d - %d\n", nProbBase, nRuleBase);
					if (args.length >= 6)
						strPathBase = args[5];
				}
			}

		} else {
			strPathBase = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary";
			// Siguientes pasos, añadir el tipo
			eSimulator = ECloudSimulator.eCLOUDSIMSTORAGE;
			// Esto es lo que tenemos que cambiar con el input del programa.
			strInitialPopulationPath = strPathBase + File.separator + "InitialPopulation/sample" + File.separator
					+ "metaInfo";
		}
		if (strPathBase == null) {
			switch (eSimulator) {
			case eCLOUDSIMSTORAGE:
				strPathBase = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary";
				break;
			case eSIMGRID:
				strPathBase = "/localSpace/cloudEnergy/simGrid/evolutionary";
				break;
			default:
				strPathBase = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary";
			}
		}

		for (int nProbLevel = nProbBase; nProbLevel < 3; nProbLevel++) {
			// for (int nRuleLevel = nRuleBase; nRuleLevel < 2; nRuleLevel++) {
			int nRuleLevel = 1;
				doEvolution(eSimulator, strPathBase, strInitialPopulationPath, nEvolutionLoops, nProbLevel, nRuleLevel);
			
		}
		// TODO: DANGER! Cambio para puebas de la segunda revision
		// for(int i=0;i<30;i++)
//		for (int i = 0; i < 1; i++) {
//			doEvolution(eSimulator, strPathBase, strInitialPopulationPath, nEvolutionLoops, nProbBase, nRuleBase);
//		}

	}

	private static void doEvolution(ECloudSimulator eSimulator, String strPathBase, String strInitialPopulationPath,
			int nEvolutionLoops, int nProbLevel, int nRuleLevel) {
		Population<CloudChrom> basePopulation;
		double dInitialConsumption;
		LinkedList<Double> mutationProbList;
		LinkedList<Boolean> ruleCombList;

		// Initial configuration
		EAController.getInstance().setPathBase(strPathBase);

		// Initial population path
		EAController.getInstance().setInitialPopulationPath(strInitialPopulationPath);

		// Selects the simulation platform
		EAController.getInstance().setPlaftormInfo(eSimulator);

		// Generate the mutation probability
		mutationProbList = EAController.getInstance().getMutationProbability(nProbLevel);

		// Initialize the mutation operators
		EAController.getInstance().initializeMutationOperators(mutationProbList);

		// Generate the rule activation list
		mutationProbList = EAController.getInstance().getMutationProbability(nProbLevel);

		// Generate the rule list
		ruleCombList = EAController.getInstance().getCombinationRuleList(nRuleLevel);

		// Selects the MRs
		EAController.getInstance().createDetailedMRList(ruleCombList);

		// Loads the seed model from disk
		basePopulation = Initialisation(strInitialPopulationPath);

		// Calculates the initial consumption of the population
		dInitialConsumption = calculateInitialConsumption(basePopulation, EAController.getInstance().getPlaftormInfo());

		if (dInitialConsumption != -1 && basePopulation.getSize() > 0) {
			// Generates the initial population
			EAController.getInstance().activateAlwaysMutate();
			Population<CloudChrom> population = createInitialPopulation(basePopulation, 10);
			EAController.getInstance().deactivateAlwaysMutate();

			// Selects the initial consumption to calculate the best individual
			Fitness<CloudChrom, Double> fitness = new MyVectorFitness(dInitialConsumption);

			// Creates the genetic algorithm object
			GeneticAlgorithm<CloudChrom, Double> ga = new GeneticAlgorithm<CloudChrom, Double>(population, fitness);

			// Adds a listener
			addListener(ga);

			// Evolves the initial population N times
			ga.evolve(nEvolutionLoops);

			// Save the evolution list in the path base
			saveIterationList();

			// Create graphs
			generateGraphs(EAController.getInstance().getPathBase(),
					EAController.getInstance().getInstanceSessionPath());

			// Reset the engine to
			EAController.getInstance().resetEngine();
		} else {
			System.out.println("Error parsing initial population or issues on the model");
		}
	}

	private static void generateGraphs(String strPath, String strSessionPath) {
		// Generate .dat info
		GraphGenerator graphGen;

		graphGen = new GAGraph();
		if (graphGen.generateGraph(strSessionPath, EAController.getInstance().getPlaftormInfo())) {
			CommandExecutor.exec("cp " + strPath + File.separator + "gnuplot" + File.separator + HISTOGRAM_GNU_TAG + " "
					+ strSessionPath);

			CommandExecutor.exec("cp " + strPath + File.separator + "gnuplot" + File.separator + EVOLUTION_GNU_TAG + " "
					+ strSessionPath);
			// Execute the command to generate the histogram
			CommandExecutor.exec(
					"cd " + strSessionPath + " && gnuplot " + strSessionPath + File.separator + HISTOGRAM_GNU_TAG);
			// Execute the command to generate the evolution graph
			CommandExecutor.exec(
					"cd " + strSessionPath + " && gnuplot " + strSessionPath + File.separator + EVOLUTION_GNU_TAG);
		}
	}

	private static void saveIterationList() {
		EAController.getInstance().saveIterationList();

	}

	private static double calculateInitialConsumption(Population<CloudChrom> basePopulation,
			ECloudSimulator platformInfo) {
		double dRet, dEnergy;
		MT_Handler mExecutor;
		MetaTestCase metaTC;
		CloudChrom chromosome;

		System.out.println("calculateInitialConsumption - Analysing the energy consumption of the basePopulation");

		mExecutor = new MT_Handler();
		dRet = dEnergy = 999999999999999999999.0;

		if (basePopulation.getSize() > 0) {
			for (int i = 0; i < basePopulation.getSize(); i++) {
				chromosome = basePopulation.getChromosomeByIndex(i);
				metaTC = chromosome.getMetaTC();
				if (metaTC != null) {
					System.out.printf("calculateInitialConsumption - Simulating the individual (%d: %d (mTcId)) \n", i,
							metaTC.getTcId());

					dEnergy = mExecutor.executeSingleTC(metaTC, platformInfo);
					chromosome.setEnergyConsumption(dEnergy);

					System.out.printf(
							"calculateInitialConsumption - The energy consumption of the individual %d is %f kWh\n", i,
							dEnergy);

					if (dEnergy < dRet)
						dRet = dEnergy;
				} else {
					System.out.println("calculateInitialConsumption - ERROR!! The meta test case is NULL!");

				}
			}
		} else {
			dRet = -1.0;
		}

		return dRet;
	}

	private static Population<CloudChrom> Initialisation(String strPath) {

		Population<CloudChrom> initialPopulation;
		MetaParser metaParser;
		LinkedList<MetaTestCase> metaTcList;
		MetaTestCase mTc;
		TestCaseParser tcParser;
		TestCaseInput tcInput;
		CloudChrom cloudIndividual;
		MutableCloud mCloudSystem;
		TestCase2Cloud tcTransform;

		initialPopulation = new Population<CloudChrom>();
		metaParser = new MetaParser();
		tcParser = new TestCaseParser_cloud(EAController.getInstance().getPlaftormInfo());
		tcTransform = new TestCase2Cloud(EAController.getInstance().getPlaftormInfo());

		// Read the initial population, from an specific path.
		metaTcList = metaParser.loadMetaTcFolder(strPath);
		if (metaTcList != null) {
			// TODO:
			int nIndex;
			String strFriendlyName;
			strFriendlyName = strPath.replace("/metaInfo", "");
			nIndex = strFriendlyName.lastIndexOf("/");

			if (nIndex != -1)
				strFriendlyName = strFriendlyName.substring(nIndex + 1);
			else
				strFriendlyName = "";

			// Create a new path to stora all the data neccesary to carry out the EA
			// algorithm
			EAController.getInstance().createNewInstancePath(strFriendlyName);

			for (int i = 0; i < metaTcList.size(); i++) {
				mTc = metaTcList.get(i);

				if (mTc != null) {
					// Load the TcInput
					tcInput = tcParser.doParseInput(mTc.getTcInput());

					if (tcInput != null) {
						// Set the object
						mTc.setTestCase(new TestCase(mTc.getTcId(), tcInput, null));

						// Convert to the programming model
						mCloudSystem = tcTransform.transformTestcase2Cloud(EAController.getInstance().getPlaftormInfo(),
								tcInput);

						System.out.printf("PARENT: %d: ", i);
						mCloudSystem.printShortResume();
						// Increment the number of individuals created.
						EAController.getInstance().incCreatedIndIndex();

						if (mCloudSystem != null) {
							// Also associate the meta TC to handle the output.
							cloudIndividual = new CloudChrom();
							cloudIndividual.setMutableCloudSystem(mCloudSystem);
							cloudIndividual.setMetaTC(mTc);
							cloudIndividual.setId(i);

							// Add individual to population
							initialPopulation.addChromosome(cloudIndividual);
						} else {
							System.out.println("Error loading initial population!");
						}
					}
				}
			}
		}

		return initialPopulation;
	}

	/**
	 * The simplest strategy for creating initial population <br/>
	 * in real life it could be more complex
	 * 
	 * @param basePopulation
	 */
	private static Population<CloudChrom> createInitialPopulation(Population<CloudChrom> basePopulation,
			int populationSize) {

		Population<CloudChrom> population = new Population<CloudChrom>();
		for (int i = 0; i < basePopulation.getSize(); i++) {
			CloudChrom base = basePopulation.getChromosomeByIndex(i);

			for (int j = 0; j < populationSize; j++) {
				// each member of initial population
				// is mutated clone of base chromosome
				CloudChrom chr = base.mutate();
				if (chr != null)
					population.addChromosome(chr);
				else
					System.out.println("createInitialPopulation - null individual removed");
			}
		}

		return population;
	}

	/**
	 * After each iteration Genetic algorithm notifies listener
	 */
	private static void addListener(GeneticAlgorithm<CloudChrom, Double> ga) {
		// just for pretty print
		System.out.println(String.format("%s\t%s\t%s", "iter", "fit", "chromosome"));

		// Lets add listener, which prints best chromosome after each iteration
		ga.addIterationListener(new IterationListener<CloudChrom, Double>() {

			@Override
			public void update(GeneticAlgorithm<CloudChrom, Double> ga) {

				CloudChrom best = ga.getBest();
				int iteration = ga.getIteration();
				if (best != null) {
					// CloudChrom best = ga.getWorst();
					double bestFit = ga.fitness(best);

					// Listener prints best achieved solution

					System.out.println(String.format("%s\t%s\t%s | Id: %d", iteration, bestFit, best, best.getId()));
					System.out.println(ga.populationPrettyPrint());
					System.out.println("============================================");
					// If fitness is satisfying - we can stop Genetic algorithm
					// if (bestFit < this.threshold) {
					// ga.terminate();
					// }
				} else {
					System.out.println("WARNING! The population vector is empty!!");
					System.out.println("============================================");
				}

				EAController.getInstance().createNewIteration(iteration, ga.getPopulationIds());
				EAController.getInstance().incIterationIndex();
			}
		});
	}

	/**
	 * Chromosome
	 */
	public static class CloudChrom implements Chromosome<CloudChrom>, Cloneable {

		private static final Random random = new Random();
		private final int[] vector = new int[5];
		private MutableCloud cloudSystem;
		private MetaTestCase metaTestCase;
		private double dEnergyConsumption;
		private int nId;

		public void setId(int nId) {
			this.nId = nId;
		}

		public void setMutableCloudSystem(MutableCloud cloudSystem) {
			this.cloudSystem = cloudSystem;
		}

		public void setEnergyConsumption(double dEnergyConsumption) {
			this.dEnergyConsumption = dEnergyConsumption;
		}

		public void setMetaTC(MetaTestCase mTc) {
			this.metaTestCase = mTc;
		}

		/**
		 * Returns clone of current chromosome, which is mutated a bit
		 */
		@Override
		public CloudChrom mutate() {
			CloudChrom result;
			MetaTestCase mTc, mTcNew;
			TcInput_cloud tcInput, tcInputNew;
			int nTcId, nIteration, nMutationOperator;
			boolean bRet, bPerformMutation, bErrorMutating;
			MutableCloud mCloudSystem;
			TestCase2Cloud tcTransform;

			nMutationOperator = 0;
			// Cuando mutas, tienes que analizar si cumple o no las MRs seleccionadas

			bRet = bPerformMutation = bErrorMutating = false;
			result = null;
			tcTransform = new TestCase2Cloud(EAController.getInstance().getPlaftormInfo());

			try {
				// Duplicate
				result = (CloudChrom) this.dup();

				// Constuir el metaTestCase y el tcInput
				nTcId = EAController.getInstance().getTcIndex();
				nIteration = EAController.getInstance().getIteration();
				bPerformMutation = EAController.getInstance().calculateMutation();

				if (result != null && bPerformMutation) {
					mTc = result.getMetaTC();
					result.setId(nTcId);
					if (mTc != null) {
						tcInput = (TcInput_cloud) mTc.getTestCaseInput();

						if (tcInput != null) {
							// Duplicate the test case
							tcInputNew = tcInput.dupTc();

							// Perform the transformation
							mCloudSystem = tcTransform
									.transformTestcase2Cloud(EAController.getInstance().getPlaftormInfo(), tcInput);

							// and mutation!
							nMutationOperator = EAController.getInstance().getLastSelectedMutationOperator();

							try {
								mCloudSystem.mutate(nMutationOperator, EAController.getInstance().getMRList());
								mCloudSystem.printShortResume();
							} catch (Exception e) {

								// Error mutating!
								System.out.printf("Cloud_GA::mutate - Error mutating with operator %d\n",
										nMutationOperator);
								bErrorMutating = true;
							}

							if (!bErrorMutating) {
								// Retransform!
								tcInputNew = (TcInput_cloud) tcTransform.transformCloud2Testcase(
										EAController.getInstance().getPlaftormInfo(), tcInputNew, mCloudSystem);

								// And finally, create the folders and files neccesaries to perform the process.
								EAController.getInstance().createNewIterationPath(nIteration);
								mTcNew = EAController.getInstance().createNewIndividualFiles(nIteration, nTcId,
										tcInputNew);

								if (EAController.getInstance().getLogLevel().getValue() >= LogLevel.eLOG.getValue())
									System.out.printf("mutate - Mutant individual %d created sucessfully | MOP: %d\n",
											nTcId, nMutationOperator);

								EAController.getInstance().incCreatedIndIndex();

								result.setMetaTC(mTcNew);

								bRet = true;
							} else {
								// Error mutating, returning null
								result = null;
							}

						} else {
							System.out.println("mutate - TcInput null");
						}
					} else {
						System.out.println("mutate - MetaTestCase null");
					}
				} else {
					result = null;
					if (bPerformMutation)
						System.out.println("mutate - Chromosome null");
				}
			} catch (NullPointerException nil) {
				System.out.println("mutate - Error mutating the individual: Nullpointer exception.");
			}

			if (bPerformMutation && (result == null || bRet == false))
				System.out.println(
						"mutate - The returning chromosome is empty, incorrect or the mutation has not been seeded");

			return result;
		}

		@Override
		public CloudChrom dup() {
			CloudChrom dupChrom;

			dupChrom = new CloudChrom();
			dupChrom.setEnergyConsumption(-1.0);

			if (this.metaTestCase != null)
				dupChrom.setMetaTC(this.metaTestCase.dup());
			else {
				dupChrom.setMetaTC(null);
				System.out.println("dup() - ERROR! The metaTestCase is null");
			}

			if (this.cloudSystem != null) {
				dupChrom.setMutableCloudSystem((MutableCloud) this.cloudSystem.clone());
			} else {
				dupChrom.setMutableCloudSystem((MutableCloud) null);
				System.out.println("dup() - ERROR! The cloudSystem is null");
			}

			return dupChrom;
		}

		/**
		 * Returns list of siblings <br/>
		 * Siblings are actually new chromosomes, <br/>
		 * created using any of crossover strategy
		 */
		@Override
		public List<CloudChrom> crossover(CloudChrom other) {

			CloudChrom chrom1, chrom2;
			MetaTestCase mTc1, mTc2, mTcNew1, mTcNew2;
			TcInput_cloud tcInput1, tcInput2, tcInputNew1, tcInputNew2;
			MutableCloud mCloudSystem1, mCloudSystem2;
			TestCase2Cloud tcTransform;
			List<CloudChrom> resultList;
			LinkedList<MutableCloud> cloudCross;
			int nTcId, nIteration, nCrossoverOperator;
			boolean bRet, bPerformCrossover, bErrorMutating;

			nCrossoverOperator = 0;
			// Cuando mutas, tienes que analizar si cumple o no las MRs seleccionadas

			bRet = bPerformCrossover = bErrorMutating = false;
			chrom1 = chrom2 = null;
			tcTransform = new TestCase2Cloud(EAController.getInstance().getPlaftormInfo());
			resultList = new LinkedList<CloudChrom>();

			try {
				// Duplicate
				chrom1 = (CloudChrom) this.dup();
				chrom2 = (CloudChrom) other.dup();

				// Constuir el metaTestCase y el tcInput
				nTcId = EAController.getInstance().getTcIndex();
				nIteration = EAController.getInstance().getIteration();
				bPerformCrossover = EAController.getInstance().calculateCrossover();

				if (chrom1 != null && chrom2 != null && bPerformCrossover) {
					mTc1 = chrom1.getMetaTC();
					mTc2 = chrom2.getMetaTC();

					// Set the IDs
					chrom1.setId(nTcId);
					chrom2.setId(nTcId + 1);

					if (mTc1 != null && mTc2 != null) {
						tcInput1 = (TcInput_cloud) mTc1.getTestCaseInput();
						tcInput2 = (TcInput_cloud) mTc2.getTestCaseInput();

						if (tcInput1 != null && tcInput2 != null) {
							// Duplicate the test case
							tcInputNew1 = tcInput1.dupTc();
							tcInputNew2 = tcInput1.dupTc();

							// Perform the transformation
							mCloudSystem1 = tcTransform
									.transformTestcase2Cloud(EAController.getInstance().getPlaftormInfo(), tcInput1);
							mCloudSystem2 = tcTransform
									.transformTestcase2Cloud(EAController.getInstance().getPlaftormInfo(), tcInput2);

							// and mutation!
							nCrossoverOperator = EAController.getInstance().getLastSelectedCrossoverOperator();

							try {
								cloudCross = mCloudSystem1.crossover(nCrossoverOperator, mCloudSystem2,
										EAController.getInstance().getMRList());

								if (cloudCross != null && cloudCross.size() >= 2) {
									mCloudSystem1 = cloudCross.get(0);
									mCloudSystem2 = cloudCross.get(1);

									mCloudSystem1.printShortResume();
									mCloudSystem2.printShortResume();
								}

							} catch (Exception e) {

								// Error mutating!
								System.out.printf("Cloud_GA::crossover - Error performing crossover with operator %d\n",
										nCrossoverOperator);
								bErrorMutating = true;
							}

							if (!bErrorMutating) {
								// Retransform!
								tcInputNew1 = (TcInput_cloud) tcTransform.transformCloud2Testcase(
										EAController.getInstance().getPlaftormInfo(), tcInputNew1, mCloudSystem1);
								tcInputNew2 = (TcInput_cloud) tcTransform.transformCloud2Testcase(
										EAController.getInstance().getPlaftormInfo(), tcInputNew2, mCloudSystem2);

								// And finally, create the folders and files neccesaries to perform the process.
								EAController.getInstance().createNewIterationPath(nIteration);
								mTcNew1 = EAController.getInstance().createNewIndividualFiles(nIteration, nTcId,
										tcInputNew1);
								mTcNew2 = EAController.getInstance().createNewIndividualFiles(nIteration, nTcId + 1,
										tcInputNew2);

								if (EAController.getInstance().getLogLevel().getValue() >= LogLevel.eLOG.getValue())
									System.out.printf(
											"crossover - Crossover individuals %d and %d created sucessfully | MOP: %d\n",
											nTcId, nTcId + 1, nCrossoverOperator);

								chrom1.setMetaTC(mTcNew1);
								chrom2.setMetaTC(mTcNew2);

								resultList.add(chrom1);
								resultList.add(chrom2);

								EAController.getInstance().incCreatedIndIndex();
								EAController.getInstance().incCreatedIndIndex();
								bRet = true;
							} else {
								// Error mutating, returning null
								chrom1 = chrom2 = null;
							}

						} else {
							System.out.println("crossover - TcInput null");
						}
					} else {
						System.out.println("crossover - MetaTestCase null");
					}
				} else {
					System.out.println("crossover - Chromosome null");
				}
			} catch (NullPointerException nil) {
				System.out.println("crossover - Error mutating the individual: Nullpointer exception.");
			}

			if (resultList == null || resultList.size() == 0 || bRet == false)
				System.out.println(
						"crossover - The returning chromosome is empty, incorrect or the crossover has not been seeded");

			return resultList;
		}

		private Cloud getCloudSystem() {
			Cloud cloudRet;

			cloudRet = null;
			if (this.cloudSystem != null)
				cloudRet = this.cloudSystem.getCloudSystem();

			return cloudRet;
		}

		@Override
		protected CloudChrom clone() {
			CloudChrom clone = new CloudChrom();
			System.arraycopy(this.vector, 0, clone.vector, 0, this.vector.length);
			return clone;
		}

		public int[] getVector() {
			return this.vector;
		}

		@Override
		public String toString() {
			String strFormat;

			strFormat = String.format("(%d, %.6f)", this.nId, dEnergyConsumption);
			return strFormat;
		}

		public MetaTestCase getMetaTC() {
			return metaTestCase;
		}

		@Override
		public int getId() {
			return this.nId;
		}

		@Override
		public boolean isFitnessValid() {
			boolean bRet;
			bRet = false;

			if (this.dEnergyConsumption > 0.0)
				bRet = true;

			return bRet;
		}

		@Override
		public double getObjective(EGAObjectives egaObjectives) {
			// TODO Auto-generated method stub
			return -1;
		}

		@Override
		public void setDominated(int dom) {
			// TODO Auto-generated method stub

		}

		@Override
		public void setnCrowdDensity(double density) {
			// TODO Auto-generated method stub

		}

		@Override
		public void setFitness() {
			// TODO Auto-generated method stub

		}

		@Override
		public double getNumDom() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getFitness() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void setCrowdingDistance(double distance) {
			// TODO Auto-generated method stub

		}

		@Override
		public void addToCrowdingDistance(double normalizedDistance) {
			// TODO Auto-generated method stub

		}

		@Override
		public double getCrowdingDistance() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getRank() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void setRank(int nRank) {
			// TODO Auto-generated method stub

		}

		@Override
		public int[] getObjectivesIndex() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	/**
	 * Fitness function, which calculates difference between chromosomes vector and
	 * target vector
	 */
	public static class MyVectorFitness implements Fitness<CloudChrom, Double> {

		private double targetConsumption;

		public MyVectorFitness() {
			targetConsumption = 0.0;
		}

		public MyVectorFitness(double dTargetConsumption) {
			this.targetConsumption = dTargetConsumption;
		}

		public void setTargetConsumption(double dTargetConsumption) {
			this.targetConsumption = dTargetConsumption;
		}

		@Override
		public Double calculate(CloudChrom chromosome) {
			double delta, dEnergy;
			ECloudSimulator platformInfo;
			MT_Handler mExecutor;
			MetaTestCase metaTC;

			mExecutor = new MT_Handler();
			delta = dEnergy = 0.0;
			try {
				if (chromosome.dEnergyConsumption == -1.0) {
					// Check first if the model has been already simulated
					metaTC = chromosome.getMetaTC();

					if (metaTC != null) {
						dEnergy = mExecutor.executeSingleTC(metaTC, EAController.getInstance().getPlaftormInfo());
						delta = targetConsumption - dEnergy;
						chromosome.setEnergyConsumption(dEnergy);
					} else {
						delta = 0.0;
						System.out.printf("calculate - ERROR!! The meta test case of individual %d is NULL! \n",
								chromosome.getId());
					}
				} else {
					delta = targetConsumption - chromosome.dEnergyConsumption;
				}

			} catch (NullPointerException nil) {
				System.out.println("ERROR!! Nullpointer exception while calculating the energy consumption!");
			}

			return delta;
		}

		@Override
		public Double calculate(Cloud_GA.CloudChrom chormosome, EGAObjectives obj) {
			// TODO Auto-generated method stub
			return null;
		}
	}
}