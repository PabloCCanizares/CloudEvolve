package entities;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import algorithms.moga.EGAObjectives;
import configuration.EAController;
import configuration.LogLevel;
import core.Chromosome;
import dataParser.cloud.input.TcInput_cloud;
import dataParser.metadata.MetaTestCase;
import entities.cloud.Cloud;
import mutation.MutableCloud.MutableCloud;
import transformations.TestCase2Cloud;

public class CloudChromosome extends AbstractCloudChromosome<CloudChromosome> {

	private double dEnergyConsumption;

	@Override
	public CloudChromosome dup() {
		CloudChromosome dupChrom;

		dupChrom = new CloudChromosome();
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


	public void setEnergyConsumption(double dEnergyConsumption) {
		this.dEnergyConsumption = dEnergyConsumption;
	}
	
	private Cloud getCloudSystem() {
		Cloud cloudRet;

		cloudRet = null;
		if (this.cloudSystem != null)
			cloudRet = this.cloudSystem.getCloudSystem();

		return cloudRet;
	}

	@Override
	protected CloudChromosome clone() {
		CloudChromosome clone = new CloudChromosome();
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


	public double getEnergyConsumption() {
		// TODO Auto-generated method stub
		return 0;
	}
}