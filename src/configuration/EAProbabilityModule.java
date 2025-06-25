package configuration;

import java.util.LinkedList;
import java.util.Random;

import auxiliar.AuxFunctions;
import dataParser.cloud.ECloudSimulator;
import mutation.MutationOperator;

public class EAProbabilityModule {

	LinkedList<EAMutationOperator> mutationOpList;
	LinkedList<EACrossoverOperator> crossoverOpList;
	int nLastSelected;
	int nLastCrossoverSelected;
	boolean bAlwaysMutate;
	
	public EAProbabilityModule()
	{
		nLastSelected=-1;
		this.mutationOpList = null;
		this.mutationOpList = null;
		bAlwaysMutate=false;
	}

	public void activateAlwaysMutate()
	{
		bAlwaysMutate = true;
	}
	public void deactivateAlwaysMutate()
	{
		bAlwaysMutate = false;
	}	
	public boolean calculateMutation()
	{
		EAMutationOperator mutationOp;
		boolean bRet, bFound;
		double dFactor, dFactorAcc, dFactorIndex;
		int nIndex;
		
		bRet = bAlwaysMutate;
		dFactorAcc = 0.0;
		nIndex = 0;
		bFound = false;
		
		dFactor = AuxFunctions.generateFactor(0.0, 100.0);
		
		if(!bAlwaysMutate && mutationOpList != null && mutationOpList.size()>0)
		{
			while(nIndex < mutationOpList.size() && !bFound)
			{
				mutationOp = mutationOpList.get(nIndex);
				if(mutationOp != null && mutationOp.isEnabled())
				{
					dFactorIndex = mutationOp.getProbability();
					if(dFactorAcc + dFactorIndex >= dFactor)
					{
						bFound = bRet = true;
						nLastSelected = mutationOp.getnOperator();
					}
					else 
						nIndex++;
					dFactorAcc+=dFactorIndex;
				}
				else
					nIndex++;
			}
		}
		else if(bAlwaysMutate)
		{
			//TODO: Diferenciar entre simgrid y cloudsim
			//Hay que cohexionar las MRs seleccionadas con los operadores,
			//de tal forma que solo se activen cuando las MRs esten de entrada.
			if(EAController.getInstance().getPlaftormInfo() == ECloudSimulator.eCLOUDSIMSTORAGE)
			{
				if(dFactor >=0 && dFactor <25)
					nLastSelected = 1;
				else if (dFactor >=25 && dFactor <50)
					nLastSelected = 2;
				else if (dFactor >=50 && dFactor <75)
					nLastSelected = 3;
				else
					nLastSelected = 4;
			}
			else
			{
				if(dFactor >=0 && dFactor <25)
					nLastSelected = 1;
				else if (dFactor >=25 && dFactor <50)
					nLastSelected = 2;
				else if (dFactor >=50 && dFactor <75)
					nLastSelected = 3;
				else
					nLastSelected = 7;			
			}
			
			
			bRet = true;
		}

		return bRet;
	}

	public int getLastMOperator() {
		
		return nLastSelected;
	}

	public int getLastCrossOperator() {
		
		return nLastSelected;
	}
	
	public boolean calculateCrossover() {
		double dFactor;
		boolean bRet;
		
		bRet = false;		
		
		dFactor = AuxFunctions.generateFactor(0.0, 100.0);
		
		if(dFactor <= 100.0)
		{
			if(dFactor >=0 && dFactor <25)
				nLastSelected = 1;
			else if (dFactor >=25 && dFactor <50)
				nLastSelected = 1;
			
			nLastSelected=2;
			bRet = true;
		}

		
		return bRet;
	}

	public void addMutationOperatorList(LinkedList<EAMutationOperator> mutationOperatorList) {
		this.mutationOpList = mutationOperatorList;
		
	}

	public void addCrossoverOperatorList(LinkedList<EACrossoverOperator> crossoverOperatorList) {
		this.crossoverOpList = crossoverOperatorList;
		
	}

}
