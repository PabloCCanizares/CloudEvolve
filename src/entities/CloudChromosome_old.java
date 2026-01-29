package entities;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import algorithms.Chromosome;
import algorithms.moga.EGAObjectives;
import configuration.EAController;
import configuration.LogLevel;
import dataParser.cloud.input.TcInput_cloud;
import dataParser.metadata.MetaTestCase;
import entities.cloud.Cloud;
import mutation.MutableCloud.MutableCloud;
import transformations.TestCase2Cloud;

public class CloudChromosome_old implements Chromosome<CloudChromosome_old>, Cloneable {

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
	public CloudChromosome_old mutate() {
		CloudChromosome_old result;
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
			result = (CloudChromosome_old) this.dup();

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
	public CloudChromosome_old dup() {
		CloudChromosome_old dupChrom;

		dupChrom = new CloudChromosome_old();
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
	public List<CloudChromosome_old> crossover(CloudChromosome_old other) {

		CloudChromosome_old chrom1, chrom2;
		MetaTestCase mTc1, mTc2, mTcNew1, mTcNew2;
		TcInput_cloud tcInput1, tcInput2, tcInputNew1, tcInputNew2;
		MutableCloud mCloudSystem1, mCloudSystem2;
		TestCase2Cloud tcTransform;
		List<CloudChromosome_old> resultList;
		LinkedList<MutableCloud> cloudCross;
		int nTcId, nIteration, nCrossoverOperator;
		boolean bRet, bPerformCrossover, bErrorMutating;

		nCrossoverOperator = 0;
		// Cuando mutas, tienes que analizar si cumple o no las MRs seleccionadas

		bRet = bPerformCrossover = bErrorMutating = false;
		chrom1 = chrom2 = null;
		tcTransform = new TestCase2Cloud(EAController.getInstance().getPlaftormInfo());
		resultList = new LinkedList<CloudChromosome_old>();

		try {
			// Duplicate
			chrom1 = (CloudChromosome_old) this.dup();
			chrom2 = (CloudChromosome_old) other.dup();

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
	protected CloudChromosome_old clone() {
		CloudChromosome_old clone = new CloudChromosome_old();
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