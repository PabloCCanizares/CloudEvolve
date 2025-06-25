package core;

import java.io.File;
import java.util.LinkedList;

import algorithms.Fitness;
import algorithms.moga.MOGeneticAlgorithm;
import algorithms.moga.PopulationMO;
import algorithms.moga.GAObjectives;
import algorithms.moga.IMOIterationListener;
import auxiliars.EAGenerateGraphs;
import configuration.EAController;
import dataParser.TestCase;
import dataParser.TestCaseInput;
import dataParser.TestCaseParser;
import dataParser.cloud.ECloudSimulator;
import dataParser.cloud.TestCaseParser_cloud;
import dataParser.metadata.MetaParser;
import dataParser.metadata.MetaTestCase;
import entities.MOCloudChromosome;
import executor.MT_Handler;
import mutation.MutableCloud.MutableCloud;
import transformations.TestCase2Cloud;

public abstract class MOCloudOrchestrator {

	EAGenerateGraphs graphGen;
	MOGeneticAlgorithm<MOCloudChromosome, GAObjectives> moAlgorithm;	
	PopulationMO<MOCloudChromosome> moPopulation;
	Fitness<MOCloudChromosome, GAObjectives> moFitness;
	
	ECloudSimulator eSimulator;
	int nProbBase, nRuleBase, nEvolutionLoops;
	int nObjectives;
	String strPathBase, strInitialPopulationPath, strSim;
	
	public void doConfigure(String[] args)
	{
		//TODO: Hay que mejorar esto que esta hecho un horror
		graphGen = new EAGenerateGraphs();
		strPathBase = null;
		nProbBase = nRuleBase = 0;
		nEvolutionLoops = 8;
		moAlgorithm = null;
		
		if (args.length >= 2) {
			strSim = args[0];
			strInitialPopulationPath = args[1] + File.separator + "metaInfo";

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
						strPathBase = args[5].concat("/VEGA/");
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
		} else
			System.out.printf("Base yet selected: %s", strPathBase);

	}
	public void setAlgorithm(MOGeneticAlgorithm<MOCloudChromosome, GAObjectives> moAlgorithm)
	{
		this.moAlgorithm = moAlgorithm;
	}
	public void doEvolution()
	{
		for (int nProbLevel = nProbBase; nProbLevel < 3; nProbLevel++) {
			int nRuleLevel = 1;
			doEvolution(eSimulator, strPathBase, strInitialPopulationPath, nEvolutionLoops, nProbLevel, nRuleLevel);
		}
	}
	public void doEvolution(ECloudSimulator eSimulator, String strPathBase, String strInitialPopulationPath,
			int nEvolutionLoops, int nProbLevel, int nRuleLevel) {
		PopulationMO<MOCloudChromosome> basePopulation;
		double dInitialConsumption, dInitialSimTime;
		double[] dConsumptionSimTime;
		LinkedList<Double> mutationProbList;
		LinkedList<Boolean> ruleCombList;
		
		
		try
		{
			validateConfiguration();
			System.out.printf(
					"doEvolution - Evolving in pathBase [%s], population [%s],\n loops [%d], prob [%d], ruleLevel [%d]\n",
					strPathBase, strInitialPopulationPath, nEvolutionLoops, nProbLevel, nRuleLevel);
	
			// Initial configuration
			EAController.getInstance().setPathBase(strPathBase);
	
			// Initial population path
			EAController.getInstance().setInitialPopulationPath(strInitialPopulationPath);
	
			// Selects the cloudsimstorage
			EAController.getInstance().setPlaftormInfo(eSimulator);
	
			// Generate the mutation probability
			mutationProbList = EAController.getInstance().getMutationProbability(nProbLevel);
	
			// Initialize the mutation operators
			EAController.getInstance().initializeMutationOperators(mutationProbList);
	
			// Generate the rule activation list
			mutationProbList = EAController.getInstance().getMutationProbability(nProbLevel);
	
			// Generate the rule list
			ruleCombList = EAController.getInstance().getCombinationRuleList(nProbLevel);
	
			// Selects the MRs
			EAController.getInstance().createDetailedMRList(ruleCombList);
	
			// Loads the seed model from disk
			basePopulation = Initialisation(strInitialPopulationPath);
	
			// Calculates the initial consumption of the population
			// dInitialConsumption = calculateInitialConsumption(basePopulation,
			EAController.getInstance().getPlaftormInfo();
			dConsumptionSimTime = calculateInitialConsumption(basePopulation, EAController.getInstance().getPlaftormInfo());
			dInitialConsumption = dConsumptionSimTime[0];
			dInitialSimTime = dConsumptionSimTime[1];
			if (dInitialConsumption != -1 && basePopulation.getSize() > 0) {
				// Generates the initial population
				EAController.getInstance().activateAlwaysMutate();
				moPopulation = createInitialPopulation(basePopulation, 10);
				EAController.getInstance().deactivateAlwaysMutate();
	
				// Selects the initial consumption to calculate the best individual
				moFitness = new MOCloudFitness(dInitialConsumption, dInitialSimTime);
				this.nObjectives = 2;
	
				moAlgorithm = instanceAlgorithm();
				
				// Adds a listener
				addListener(moAlgorithm);
				
				// Evolves the initial population N times
				moAlgorithm.evolve(nEvolutionLoops);
				
				// Save the evolution list in the path base
				saveIterationList();
	
				// Create graphs
				graphGen.generateGraphs(EAController.getInstance().getPathBase(),
						EAController.getInstance().getInstanceSessionPath());
	
				// Reset the engine to
				EAController.getInstance().resetEngine();
			} else {
				System.out.println("Error parsing initial population or issues on the model");
			}
		}
		catch(Exception e)
		{
			
		}
	}
	
	public abstract MOGeneticAlgorithm<MOCloudChromosome, GAObjectives> instanceAlgorithm();
	
	private static double[] calculateInitialConsumption(PopulationMO<MOCloudChromosome> basePopulation,
			ECloudSimulator platformInfo) {
		double dRet, dEnergy, dSimTime;
		MT_Handler mExecutor;
		MetaTestCase metaTC;
		MOCloudChromosome chromosome;

		double[] dEnergySimTime = new double[2];
		double[] dRetESim = new double[2];

		System.out.println("calculateInitialConsumption - Analysing the energy consumption of the basePopulation");

		mExecutor = new MT_Handler();
		dRet = dEnergy = dSimTime = 999999999999999999999.0;

		if (basePopulation.getSize() > 0) {
			for (int i = 0; i < basePopulation.getSize(); i++) {
				chromosome = basePopulation.getChromosomeByIndex(i);
				metaTC = chromosome.getMetaTC();
				if (metaTC != null) {
					System.out.printf("calculateInitialConsumption - Simulating the individual (%d: %d (mTcId)) \n", i,
							metaTC.getTcId());

					// dEnergy = mExecutor.executeSingleTC(metaTC,platformInfo);
					dEnergy = mExecutor.executeSingleTC(metaTC, platformInfo);
					dSimTime = mExecutor.getdTime();
					chromosome.setEnergyConsumption(dEnergy);
					dSimTime = dEnergySimTime[1];
					chromosome.setTime(dSimTime);

					System.out.printf(
							"calculateInitialConsumption - The energy consumption of the individual %d is %f kWh\n", i,
							dEnergy);

					if (dEnergy < dRet)
						dRetESim[0] = dEnergy;
					if (dSimTime < dRet)
						dRetESim[1] = dSimTime;
				} else {
					System.out.println("calculateInitialConsumption - ERROR!! The meta test case is NULL!");

				}
			}
		} else {
			dRet = -1.0;
			dRetESim[0] = -1.0;
			dRetESim[1] = -1.0;

		}

		return dRetESim;// dRet;
	}
	
	/**
	 * The simplest strategy for creating initial population <br/>
	 * in real life it could be more complex
	 * 
	 * @param basePopulation
	 */
	private static PopulationMO<MOCloudChromosome> createInitialPopulation(PopulationMO<MOCloudChromosome> basePopulation,
			int populationSize) {

		PopulationMO<MOCloudChromosome> population = new PopulationMO<>();
		for (int i = 0; i < basePopulation.getSize(); i++) {
			MOCloudChromosome base = basePopulation.getChromosomeByIndex(i);

			for (int j = 0; j < populationSize; j++) {
				// each member of initial population
				// is mutated clone of base chromosome
				MOCloudChromosome chr = base.mutate();
				if (chr != null)
					population.addChromosome(chr);
				else
					System.out.println("createInitialPopulation - null individual removed");
			}
		}

		return population;
	}
	private static void saveIterationList() {
		EAController.getInstance().saveIterationList();
	}
	

	private static PopulationMO<MOCloudChromosome> Initialisation(String strPath) {

		PopulationMO<MOCloudChromosome> initialPopulation;
		MetaParser metaParser;
		LinkedList<MetaTestCase> metaTcList;
		MetaTestCase mTc;
		TestCaseParser tcParser;
		TestCaseInput tcInput;
		MOCloudChromosome cloudIndividual;
		MutableCloud mCloudSystem;
		TestCase2Cloud tcTransform;
		String strFriendlyName;

		initialPopulation = new PopulationMO<>();
		metaParser = new MetaParser();
		tcParser = new TestCaseParser_cloud(EAController.getInstance().getPlaftormInfo());
		tcTransform = new TestCase2Cloud(EAController.getInstance().getPlaftormInfo());

		// Read the initial population, from an specific path.
		metaTcList = metaParser.loadMetaTcFolder(strPath);
		if (metaTcList != null) {
			// TODO:Analizar
			// Adding to the folder a friendly identificator
			int nIndex;
			strFriendlyName = strPath.replace("/metaInfo", "");
			nIndex = strFriendlyName.lastIndexOf("/");

			if (nIndex != -1)
				strFriendlyName = strFriendlyName.substring(nIndex + 1);
			else
				strFriendlyName = "";

			// Create a new path to store all the data neccesary to carry out the EA
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
							cloudIndividual = new MOCloudChromosome();
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
	 * Comprueba que la configuración previa (doConfigure) ha dejado
	 * los campos en un estado válido. 
	 * Lanza IllegalStateException si hay algún error.
	 */
	private void validateConfiguration() {
	    StringBuilder sb = new StringBuilder();
	    // Comprobar simulador
	    if (eSimulator == null) {
	        sb.append("– eSimulator no asignado.\n");
	    }
	    // Nº de iteraciones
	    if (nEvolutionLoops <= 0) {
	        sb.append("– nEvolutionLoops debe ser > 0 (actual: ")
	          .append(nEvolutionLoops).append(").\n");
	    }
	    // Probabilidad y regla deben estar en rango 0–2 y 0–1
	    if (nProbBase < 0 || nProbBase > 2) {
	        sb.append("– nProbBase debe estar entre 0 y 2 (actual: ")
	          .append(nProbBase).append(").\n");
	    }
	    if (nRuleBase < 0 || nRuleBase > 1) {
	        sb.append("– nRuleBase debe estar entre 0 y 1 (actual: ")
	          .append(nRuleBase).append(").\n");
	    }
	    // Paths no nulos ni vacíos
	    if (strPathBase == null || strPathBase.isEmpty()) {
	        sb.append("– strPathBase no definido.\n");
	    }
	    if (strInitialPopulationPath == null || strInitialPopulationPath.isEmpty()) {
	        sb.append("– strInitialPopulationPath no definido.\n");
	    }
	    // strSim tampoco puede faltar
	    if (eSimulator == null && (strSim == null || strSim.isEmpty())) {
	        sb.append("– strSim no definido.\n");
	    }
	    // Si hay errores, abortar
	    if (sb.length() > 0) {
	        throw new IllegalStateException("Error en configuración:\n" + sb.toString());
	    }
	}
	private void addListener(MOGeneticAlgorithm<MOCloudChromosome, GAObjectives> algorithm) {
		// just for pretty print
		System.out.println(String.format("%s\t%s\t%s", "iter", "fit", "chromosome"));

		// Lets add listener, which prints best chromosome after each iteration
		algorithm.addIterationListener(new IMOIterationListener<MOCloudChromosome, GAObjectives>() {

			@Override
			public void update(MOGeneticAlgorithm<MOCloudChromosome, GAObjectives> algorithm) {

				MOCloudChromosome best = algorithm.getBest();
				int iteration = algorithm.getIteration();
				if (best != null) {
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

		});
	}
	public PopulationMO<MOCloudChromosome> getPopulation() {
		return moPopulation;
	}
	public Fitness<MOCloudChromosome, GAObjectives> getFitness() {
		return moFitness;
	}
	public int getNObjectives() {
		return nObjectives;
	}
	protected int getEvolutionLoops() 
	{
		return nEvolutionLoops;
	}
}
