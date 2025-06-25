package configuration;

import mutation.MutationOperator;
public class EACrossoverOperator extends MutationOperator {

	
	double dProbability;
	
	public EACrossoverOperator(int nOperatorNumber, double fProbability, String strDescription, boolean bEnabled) {
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
