package configuration;

import mutation.MutationOperator;
public class EAMutationOperator extends MutationOperator {

	
	double dProbability;
	
	public EAMutationOperator(int nOperatorNumber, double fProbability, String strDescription, boolean bEnabled) {
		super(nOperatorNumber, strDescription, bEnabled);
		this.dProbability = fProbability;
		
	}

	public double getProbability() {
		return dProbability;
	}

	public void setProbability(double dProbability) {
		this.dProbability = dProbability;
	}
}
