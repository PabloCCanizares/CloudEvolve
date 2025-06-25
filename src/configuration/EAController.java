package configuration;

import java.util.LinkedList;

import auxiliar.Iteration;
import auxiliar.Iterations;
import auxiliars.EAFileHandler;
import dataParser.cloud.ECloudSimulator;
import dataParser.cloud.input.TcInput_cloud;
import dataParser.metadata.MetaTestCase;
import entities.cloud.EnumComputingElement;
import followUpTcGeneration.MRulesModel;
import main.ConfigMT;
import metamorphicRelation.expression.Characteristic;
import metamorphicRelation.expression.EnumModelValues;
import metamorphicRelation.expression.EnumRelationalOperator;
import metamorphicRelation.expression.LiteralExpression;
import metamorphicRelation.expression.MetamorphicRelation;
import metamorphicRelation.expression.RelationalExpression;
import metamorphicRelation.expression.cloud.CloudComponents;
import metamorphicRelation.expression.cloud.Energy;
import metamorphicRelation.expression.cloud.Nmachines;
import metamorphicRelation.expression.cloud.Performance;

public class EAController {

	int nMaxInterations;
	int nIterationIndex;
	int nNumTcCreated;
	String strPathBase;
	LinkedList<EAMutationOperator> mutationOperatorList;
	LinkedList<EACrossoverOperator> crossoverOperatorList;
	private static EAController controller;
	
	EAFileHandler fileHandler;
	EAProbabilityModule probModule;
	ECloudSimulator ePlatformType;
	LinkedList<MRulesModel> mrList;
	LogLevel eLogLevel;
	
	Iterations iterController;
	public EAController()
	{		
		initialise();
	}

	public void initialise()
	{
		iterController = new Iterations();
		
		mutationOperatorList = new LinkedList<EAMutationOperator>();
		crossoverOperatorList = new LinkedList<EACrossoverOperator>();
		nMaxInterations = nIterationIndex = nNumTcCreated = 0;
		mrList = null;
		eLogLevel = LogLevel.eVERBOSE;
		
	}
    public static EAController getInstance() {
        if (controller == null){
        	controller = new EAController();
        }        
        return controller;
    }
    
    public LogLevel getLogLevel()
    {
    	return this.eLogLevel;    		
    }
	public void setPathBase(String strPath)
	{
		this.strPathBase = strPath;
		fileHandler = new EAFileHandler(strPath);
	}

	public void setInitialPopulationPath(String strInitialPopulationPath) {
	}
	public void initializeMutationOperators(LinkedList<Double> probList)
	{
		switch(this.ePlatformType)
		{
		case eCLOUDSIMSTORAGE:
			if(probList.size() >= 4)
			{
				
				mutationOperatorList.add(new EAMutationOperator(1, probList.removeFirst(), "Seed changes on a percentage of nodes of the system", true));
				mutationOperatorList.add(new EAMutationOperator(2, probList.removeFirst(), "Randomly modifies the bandwidth of a network link of the cloud", true));
				mutationOperatorList.add(new EAMutationOperator(3, probList.removeFirst(), "Randomly modifies the latency of a network link of the cloud", true));
				mutationOperatorList.add(new EAMutationOperator(7, probList.removeFirst(), "Delete a rack and all the links connected to it", true));
			}

			break;
		case eSIMGRID:
			mutationOperatorList.add(new EAMutationOperator(1, probList.removeFirst(), "Seed changes on a percentage of nodes of the system", true));
			mutationOperatorList.add(new EAMutationOperator(2, probList.removeFirst(), "Randomly modifies the bandwidth of a network link of the cloud", false));
			mutationOperatorList.add(new EAMutationOperator(3, probList.removeFirst(), "Randomly modifies the latency of a network link of the cloud", false));
			mutationOperatorList.add(new EAMutationOperator(4, probList.removeFirst(), "", false));
			mutationOperatorList.add(new EAMutationOperator(5, probList.removeFirst(), "", false));
			mutationOperatorList.add(new EAMutationOperator(6, probList.removeFirst(), "", false));
			mutationOperatorList.add(new EAMutationOperator(7, probList.removeFirst(), "Delete a rack and all the links connected to it", true));			
			mutationOperatorList.add(new EAMutationOperator(8, probList.removeFirst(), "", false));
			break;
		default:
			break;
		}
		if(probModule == null)
		{
			//Initialises the probability module
			probModule = new EAProbabilityModule();
		}
		
		probModule.addMutationOperatorList(mutationOperatorList);

	}

	public void initializeCrossoverOperators()
	{
		switch(this.ePlatformType)
		{
		case eCLOUDSIMSTORAGE:
			//Operator 1
			crossoverOperatorList.add(new EACrossoverOperator(1, 0.0, 
					"Seed changes on a percentage of nodes of the system", true));
			//Operator 2
			crossoverOperatorList.add(new EACrossoverOperator(2, 100.0, 
					"Given two parents, a random rack is selected from the first one and is exchanged "
				  + "from a random rack from the second one to generate a new individual.", true));			
			break;
		case eSIMGRID:
				
			break;
		default:
			break;
		}
		if(probModule == null)
		{
			//Initialises the probability module
			probModule = new EAProbabilityModule();			
		}
		probModule.addCrossoverOperatorList(crossoverOperatorList);
		
	}
	public String getPathBase() {		
		return strPathBase;
	}

	public void incIterationIndex()
	{
		nIterationIndex++;
	}
	public void incCreatedIndIndex()
	{
		nNumTcCreated++;
	}

	public int getTcIndex() {
		
		return nNumTcCreated;
	}

	public int getIteration() {
		
		return nIterationIndex;
	}

	public void createNewIterationPath(int nIteration) {
		
		if(fileHandler != null)
			fileHandler.createNewIterationPath(nIteration);
	}

	public void createNewInstancePath(String strFriendlyName) {
		if(fileHandler != null)
			fileHandler.createNewEAPathBase(strFriendlyName);
	}

	public MetaTestCase createNewIndividualFiles(int nIteration, int nTcId, TcInput_cloud tcInputNew) {
		MetaTestCase metaTcRet;
		
		metaTcRet = null;
		if(fileHandler!= null)
			metaTcRet = fileHandler.createNewIndividualFiles(nIteration,nTcId, tcInputNew);
		
		return metaTcRet;
	}

	public void setPlaftormInfo(ECloudSimulator ePlatformType) {
		this.ePlatformType = ePlatformType;		
	}
	
	public ECloudSimulator getPlaftormInfo() {
		return this.ePlatformType;		
	}

	public LinkedList<MRulesModel> getMRList() {		
		return mrList;
	}
	
	public void setMRList(LinkedList<MRulesModel> mrList) {		
		this.mrList = mrList;
	}	
	public void createMRList(int ... numbers)
	{
		if(mrList == null)
		{
			mrList = new LinkedList<MRulesModel>();
		}

		
		for (int number : numbers) 
		{
			System.out.printf("createMRList - Adding relation #%d\n", number);
			mrList.add(new MRulesModel(number, true));
	    }
	}
	public void activateAlwaysMutate() {
		if(probModule != null)
			probModule.activateAlwaysMutate();		
	}

	public void deactivateAlwaysMutate() {
		if(probModule != null)
			probModule.deactivateAlwaysMutate();		
	}
	
	public int getLastSelectedMutationOperator() 
	{
		
		return probModule == null ? 0 : probModule.getLastMOperator();
	}
	public int getLastSelectedCrossoverOperator() {
		return probModule == null ? 0 : probModule.getLastCrossOperator();
	}
	public boolean calculateMutation() 
	{
		return probModule == null ? false : probModule.calculateMutation();
	}
	public boolean calculateCrossover() {
		return probModule == null ? false : probModule.calculateCrossover();
	}
	public void createDetailedMRList(LinkedList<Boolean> ruleCombList) {
		MRulesModel rule1, rule2, rule3, rule4, rule5,  rule6, rule7, rule8;
		MetamorphicRelation expressionRule, expressionRule2, expressionRule4, expressionRule5;
		
		expressionRule = new MetamorphicRelation(
		//Left expression
	    new RelationalExpression(new LiteralExpression(new Performance(EnumModelValues.eMODEL_M1,CloudComponents.eCOMPONENT_CPU)), //Perf(m1,cpu)
								 EnumRelationalOperator.eRELATIONAL_GREATER,													   // >		 		
								 new LiteralExpression(new Performance(EnumModelValues.eMODEL_M2,CloudComponents.eCOMPONENT_CPU))),//Perf(m2,cpu)
	    
	    //Right Expression
	    new RelationalExpression(new LiteralExpression(new Energy(EnumModelValues.eMODEL_M1)),	//Energy (m1)  
				 EnumRelationalOperator.eRELATIONAL_LESS,										// <
				 new LiteralExpression(new Energy(EnumModelValues.eMODEL_M2))));				//Energy (m2)
		
		/////////////////////////////////////////////////////////////////////////////////////////////////////
		expressionRule2 = new MetamorphicRelation(	
		//Left expression		
		new RelationalExpression(new LiteralExpression(new Nmachines(EnumModelValues.eMODEL_M1)), 
										 EnumRelationalOperator.eRELATIONAL_GREATEREQUAL,													   		 		
										 new LiteralExpression(new Nmachines(EnumModelValues.eMODEL_M2))),
	    //Right Expression
	    new RelationalExpression(new LiteralExpression(new Energy(EnumModelValues.eMODEL_M1)),	//Energy (m1)  
				 EnumRelationalOperator.eRELATIONAL_LESS,										// <
				 new LiteralExpression(new Energy(EnumModelValues.eMODEL_M2))));				//Energy (m2));
				
		///////////////////////////////////////////////////////////////////////////////////////////////////
		expressionRule4 = new MetamorphicRelation(		
	    new RelationalExpression(new LiteralExpression(new Performance(EnumModelValues.eMODEL_M1,CloudComponents.eCOMPONENT_HD)), 
								 EnumRelationalOperator.eRELATIONAL_GREATER,													   		 		
								 new LiteralExpression(new Performance(EnumModelValues.eMODEL_M2,CloudComponents.eCOMPONENT_HD))),null);
		
		///////////////////////////////////////////////////////////////////////////////////////////////////
		expressionRule5 = new MetamorphicRelation(		
			    new RelationalExpression(new LiteralExpression(new Performance(EnumModelValues.eMODEL_M1,CloudComponents.eCOMPONENT_NET)), 
										 EnumRelationalOperator.eRELATIONAL_GREATER,													   		 		
										 new LiteralExpression(new Performance(EnumModelValues.eMODEL_M2,CloudComponents.eCOMPONENT_NET))),null);
				
		if(mrList == null)
		{
			mrList = new LinkedList<MRulesModel>();
		}
		
		if(ruleCombList != null && ruleCombList.size()>=8)
		{
			//Creating rules
			rule1 = new MRulesModel(1, ruleCombList.removeFirst());	
			rule2 = new MRulesModel(2, ruleCombList.removeFirst());	
			rule3 = new MRulesModel(3, ruleCombList.removeFirst());	
			rule4 = new MRulesModel(4, ruleCombList.removeFirst());	
			rule5 = new MRulesModel(5, ruleCombList.removeFirst());
			rule6 = new MRulesModel(6, ruleCombList.removeFirst());
			rule7 = new MRulesModel(7, ruleCombList.removeFirst());
			rule8 = new MRulesModel(8, ruleCombList.removeFirst());
		}
		else
		{
			//Creating rules
			rule1 = new MRulesModel(1, true);	
			rule2 = new MRulesModel(2, true);	
			rule3 = new MRulesModel(3, true);	
			rule4 = new MRulesModel(4, true);	
			rule5 = new MRulesModel(5, true);
			rule6 = new MRulesModel(6, true);
			rule7 = new MRulesModel(7, true);
			rule8 = new MRulesModel(8, true);
		}

		
		//Adding the expressions
		rule1.addExpression(expressionRule);
		rule2.addExpression(expressionRule2);	
		rule4.addExpression(expressionRule4);
		rule5.addExpression(expressionRule5);
		
		mrList.add(rule1);
		mrList.add(rule2);
		mrList.add(rule3);
		mrList.add(rule4);
		mrList.add(rule5);
		mrList.add(rule6);
		mrList.add(rule7);
		mrList.add(rule8);
		
	}

	public void createNewIteration(int nIteration, LinkedList<Integer> populationIds) {

		Iteration iteration;
		
		iterController.createNewIteration(nIteration, populationIds);

	}
	public void saveIterationList()
	{
		String strInstanceSessionPath;
		strInstanceSessionPath = fileHandler.getSessionPath();
		iterController.saveIterationList(strInstanceSessionPath);		
	}
	public String getInstanceSessionPath()
	{
		String strInstanceSessionPath;
		
		strInstanceSessionPath = "";
		if(fileHandler != null)
		{
			strInstanceSessionPath = fileHandler.getSessionPath();
		}
		
		return strInstanceSessionPath;
	}

	public LinkedList<Double> getMutationProbability(int nProbLevel) {
		
		LinkedList<Double> doubleList;
		
		doubleList = new LinkedList<Double>();
		
		if(this.ePlatformType == ECloudSimulator.eCLOUDSIMSTORAGE)
		{
			switch(nProbLevel)
			{
			case 0:
				doubleList.addLast(25.0);
				doubleList.addLast(25.0);
				doubleList.addLast(25.0);
				doubleList.addLast(25.0);
				break;
			case 1:
				doubleList.addLast(15.0);
				doubleList.addLast(10.0);
				doubleList.addLast(5.0);
				doubleList.addLast(1.0);
				break;			
			case 2:
				doubleList.addLast(1.5);
				doubleList.addLast(1.5);
				doubleList.addLast(1.0);
				doubleList.addLast(0.05);
				break;			
			}
		}
		else
		{
			switch(nProbLevel)
			{
			/*case 0:
				doubleList.addLast(5.0);doubleList.addLast(1.0);doubleList.addLast(1.0);doubleList.addLast(1.0);
				doubleList.addLast(1.0);doubleList.addLast(1.0);doubleList.addLast(1.0);doubleList.addLast(1.0);
				break;*/
			case 0:
				doubleList.addLast(25.0);	//#1
				doubleList.addLast(25.0);	//#2
				doubleList.addLast(25.0);	//#3
				doubleList.addLast(0.0);	//#4
				doubleList.addLast(0.0);	//#5
				doubleList.addLast(0.0);	//#6
				doubleList.addLast(25.0);	//#7
				doubleList.addLast(0.0);	//#8				
				break;
			case 1:
				doubleList.addLast(10.0);	//#1
				doubleList.addLast(10.0);	//#2
				doubleList.addLast(10.0);	//#3
				doubleList.addLast(0.0);	//#4
				doubleList.addLast(0.0);	//#5
				doubleList.addLast(0.0);	//#6
				doubleList.addLast(10.0);	//#7
				doubleList.addLast(0.0);	//#8
				break;			
			case 2:
				doubleList.addLast(5.0);	//#1
				doubleList.addLast(5.0);	//#2
				doubleList.addLast(5.0);	//#3
				doubleList.addLast(0.0);	//#4
				doubleList.addLast(0.0);	//#5
				doubleList.addLast(0.0);	//#6
				doubleList.addLast(5.0);	//#7
				doubleList.addLast(0.0);	//#8
				break;			
			}
		}
		
		
		return doubleList;
	}

	public LinkedList<Boolean> getCombinationRuleList(int nProbLevel) {
		LinkedList<Boolean> booleanList;
		
		booleanList = new LinkedList<Boolean>();
		switch(nProbLevel)
		{
		case 0:
			booleanList.addLast(true);	//#1
			booleanList.addLast(false); //#2
			booleanList.addLast(false); //#3
			booleanList.addLast(false); //#4
			booleanList.addLast(true);  //#5
			booleanList.addLast(false); //#6
			booleanList.addLast(false); //#7
			booleanList.addLast(false); //#8
			break;
		case 1:
			booleanList.addLast(true); //#1
			booleanList.addLast(true); //#2
			booleanList.addLast(true); //#3
			booleanList.addLast(true); //#4
			booleanList.addLast(true); //#5
			booleanList.addLast(true); //#6
			booleanList.addLast(true); //#7	
			booleanList.addLast(true); //#8	
			break;			
		default:
			break;
		}
		
		return booleanList;
	}

	public void resetEngine() {
		initialise();
	}

}
