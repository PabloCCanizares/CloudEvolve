package core;

import algorithms.Fitness;
import algorithms.moga.EGAObjectives;
import algorithms.moga.GAObjectives;
import configuration.EAController;
import dataParser.cloud.ECloudSimulator;
import dataParser.metadata.MetaTestCase;
import entities.MOCloudChromosome;
import executor.MT_Handler;

public class MOCloudFitness implements Fitness<MOCloudChromosome, GAObjectives> {

	private double targetConsumption;
	private double targetTime;

	public MOCloudFitness() {
		targetConsumption = 0.0;
		targetTime = 0.0;
	}

	public MOCloudFitness(double dTargetConsumption, double dTargetTime) {
		this.targetConsumption = dTargetConsumption;
		this.targetTime = dTargetTime;
	}

	public void setTargetConsumption(double dTargetConsumption) {
		this.targetConsumption = dTargetConsumption;
	}

	@Override
	public GAObjectives calculate(MOCloudChromosome chromosome) {
		double delta, dEnergy, dTime;
		ECloudSimulator platformInfo;
		MT_Handler mExecutor;
		MetaTestCase metaTC;
		GAObjectives gaObjectives;

		gaObjectives = new GAObjectives();
		mExecutor = new MT_Handler();
		delta = dEnergy = 0.0;
		try {
			if (chromosome.getEnergyConsumption() == -1.0) {
				// Check first if the model has been already simulated
				metaTC = chromosome.getMetaTC();

				if (metaTC != null) {
					dEnergy = mExecutor.executeSingleTC(metaTC, EAController.getInstance().getPlaftormInfo());
					dTime = mExecutor.getdTime();
					delta = targetConsumption - dEnergy;
					chromosome.setEnergyConsumption(dEnergy);
					chromosome.setTime(dTime);
					gaObjectives.addObjective(EGAObjectives.eENERGY, dEnergy);
					gaObjectives.addObjective(EGAObjectives.eTIME, dTime);
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

		return gaObjectives;
	}

	@Override
	public GAObjectives calculate(MOCloudChromosome chromosome, EGAObjectives obj) {
		double delta, dEnergy, dSimTime;
		MT_Handler mExecutor;
		MetaTestCase metaTC;
		GAObjectives gaObjectives;

		gaObjectives = new GAObjectives();
		mExecutor = new MT_Handler();
		delta = dEnergy = dSimTime = 0.0;
		try {
			if (chromosome.getEnergyConsumption() == -1.0) {
				// Check first if the model has been already simulated
				metaTC = chromosome.getMetaTC();

				if (metaTC != null) {
					dEnergy = mExecutor.executeSingleTC(metaTC, EAController.getInstance().getPlaftormInfo());
					dSimTime = mExecutor.getdTime();
					switch (obj) {
					case eENERGY: {
						delta = targetConsumption - dEnergy;
						gaObjectives.addObjective(EGAObjectives.eENERGY, delta);
					}
					case eTIME: {
						delta = targetTime - dSimTime;
						gaObjectives.addObjective(EGAObjectives.eTIME, delta);
					}
					}
					chromosome.setEnergyConsumption(dEnergy);
					chromosome.setTime(dSimTime);
					
				} else {
					delta = 0.0;
					System.out.printf("calculate - ERROR!! The meta test case of individual %d is NULL! \n",
							chromosome.getId());
				}
			} else {
				switch (obj) {
				case eENERGY: {
					delta = targetConsumption - chromosome.getEnergyConsumption();
					gaObjectives.addObjective(EGAObjectives.eENERGY, delta);
				}
				case eTIME: {
					delta = targetTime - chromosome.getSimTime();
					gaObjectives.addObjective(EGAObjectives.eTIME, delta);
				}
				}
			}

		} catch (NullPointerException nil) {
			System.out.println("ERROR!! Nullpointer exception while calculating the energy consumption!");
		}

		return gaObjectives;
	}
}