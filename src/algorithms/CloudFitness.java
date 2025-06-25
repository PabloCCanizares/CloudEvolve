package algorithms;

import algorithms.moga.EGAObjectives;
import configuration.EAController;
import dataParser.cloud.ECloudSimulator;
import dataParser.metadata.MetaTestCase;
import entities.CloudChromosome;
import executor.MT_Handler;

/**
 * Fitness function, which calculates difference between chromosomes vector and
 * target vector
 */
public class CloudFitness implements Fitness<CloudChromosome, Double> {

	private double targetConsumption;

	public CloudFitness() {
		targetConsumption = 0.0;
	}

	public CloudFitness(double dTargetConsumption) {
		this.targetConsumption = dTargetConsumption;
	}

	public void setTargetConsumption(double dTargetConsumption) {
		this.targetConsumption = dTargetConsumption;
	}

	@Override
	public Double calculate(CloudChromosome chromosome) {
		double delta, dEnergy;
		ECloudSimulator platformInfo;
		MT_Handler mExecutor;
		MetaTestCase metaTC;

		mExecutor = new MT_Handler();
		delta = dEnergy = 0.0;
		try {
			if (chromosome.getEnergyConsumption() == -1.0) {
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
				delta = targetConsumption - chromosome.getEnergyConsumption();
			}

		} catch (NullPointerException nil) {
			System.out.println("ERROR!! Nullpointer exception while calculating the energy consumption!");
		}

		return delta;
	}

	@Override
	public Double calculate(CloudChromosome chormosome, EGAObjectives obj) {
		// TODO Auto-generated method stub
		return null;
	}
}
