package entities;

import algorithms.moga.EGAObjectives;
import dataParser.metadata.MetaTestCase;
import entities.cloud.Cloud;
import mutation.MutableCloud.MutableCloud;

public class MOCloudChromosome  extends AbstractCloudChromosome<MOCloudChromosome> {

		private double dEnergyConsumption;
	private double dSimTime;
	
	public MOCloudChromosome(MutableCloud cloudSystem2, double dEnergyConsumption2, double dSimTime2,
			MetaTestCase metaTestCase2) {
		this.cloudSystem = cloudSystem2;
		this.setEnergyConsumption(dEnergyConsumption2);
		this.dSimTime = dSimTime2;
		this.setMetaTestCase(metaTestCase2);
	}

	public MOCloudChromosome() {
	}


	public void setEnergyConsumption(double dEnergyConsumption) {
		this.dEnergyConsumption = dEnergyConsumption;
	}


	// Metodo añadido por Miguel Pérez
	public void setTime(double dTime) {
		this.dSimTime = dTime;
	}

	@Override
	public double getObjective(EGAObjectives obj) {
		double ret = -1;
		switch (obj) {
		case eENERGY:
			ret = getEnergyConsumption();
		case eTIME:
			ret = getSimTime();
		}
		return ret;
	}

	public boolean dominates(MOCloudChromosome solution2) {
		if (this.getEnergyConsumption() > solution2.getEnergyConsumption() && this.getSimTime() >= solution2.getSimTime())
			return true;
		if (this.getSimTime() > solution2.getSimTime() && this.getEnergyConsumption() >= solution2.getEnergyConsumption())
			return true;
		return false;
	}


	@Override
	public MOCloudChromosome dup() {
		MOCloudChromosome dupChrom;

		dupChrom = new MOCloudChromosome();
		dupChrom.setEnergyConsumption(-1.0);

		if (this.getMetaTestCase() != null)
			dupChrom.setMetaTC(this.getMetaTestCase().dup());
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


	public Cloud getCloudSystem() {
		Cloud cloudRet;

		cloudRet = null;
		if (this.cloudSystem != null)
			cloudRet = this.cloudSystem.getCloudSystem();

		return cloudRet;
	}

	@Override
	protected MOCloudChromosome clone() {
		MOCloudChromosome clone = new MOCloudChromosome();
		System.arraycopy(this.vector, 0, clone.vector, 0, this.vector.length);
		return clone;
	}

	public int[] getVector() {
		return this.vector;
	}

	@Override
	public String toString() {
		String strFormat;

		strFormat = String.format("(%d, %.6f, %f)", this.nId, getEnergyConsumption(), getSimTime());
		return strFormat;
	}

	public MetaTestCase getMetaTC() {
		return getMetaTestCase();
	}

	@Override
	public int getId() {
		return this.nId;
	}

	@Override
	public boolean isFitnessValid() {
		boolean bRet;
		bRet = false;

		if (this.getEnergyConsumption() > 0.0 && this.getSimTime() > 0.0)
			bRet = true;

		return bRet;
	}

	public double getObjective(int index) {
		if (index == 0)
			return getEnergyConsumption();
		else if (index == 1) {
			return getSimTime();
		} else
			return -1;
	}

	@Override
	public void setDominated(int dominations) {
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

	public double getEnergyConsumption() {
		return dEnergyConsumption;
	}

	public MetaTestCase getMetaTestCase() {
		return metaTestCase;
	}

	public void setMetaTestCase(MetaTestCase metaTestCase) {
		this.metaTestCase = metaTestCase;
	}

	public double getSimTime() {
		return dSimTime;
	}

	public MutableCloud getMutableCloudSystem() {
		return cloudSystem;
	}

}
