package entities;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import configuration.EAController;
import configuration.LogLevel;
import core.Chromosome;
import dataParser.cloud.input.TcInput_cloud;
import dataParser.metadata.MetaTestCase;
import mutation.MutableCloud.MutableCloud;
import transformations.TestCase2Cloud;

/**
 * Clase base que extrae la lógica común de CloudChromosome y MOCloudChromosome.
 *
 * @param <C> Tipo de cromosoma concreto que extiende esta clase.
 */
public abstract class AbstractCloudChromosome<
        C extends AbstractCloudChromosome<C>
    > implements Chromosome<C>, Cloneable {

    protected static final Random random = new Random();
    protected final int[] vector = new int[5];
    protected MutableCloud cloudSystem;
    protected MetaTestCase metaTestCase;
    protected int nId;

    // Identificador del individuo. 
    public void setId(int nId) {
        this.nId = nId;
    }

    @Override
    public int getId() {
        return this.nId;
    }

    // Asociar la simulación mutable. 
    public void setMutableCloudSystem(MutableCloud cloudSystem) {
        this.cloudSystem = cloudSystem;
    }

    // Obtener la simulación mutable. 
    public MutableCloud getMutableCloudSystem() {
        return this.cloudSystem;
    }

    // Set/get del metadato del test-case. 
    public void setMetaTC(MetaTestCase mTc) {
        this.metaTestCase = mTc;
    }
    public MetaTestCase getMetaTC() {
        return this.metaTestCase;
    }

    // Vector auxiliar fijo de tamaño 5. 
    public int[] getVector() {
        return this.vector;
    }

    /**
     * Lógica genérica de mutación extraída de ambas implementaciones.
     */
    @Override
    public C mutate() {
        C result;
        int nTcId, nIteration, nMutationOperator;
        boolean bRet, bPerformMutation, bErrorMutating;
        MetaTestCase mTc, mTcNew;
        TcInput_cloud tcInput, tcInputNew;
        MutableCloud mCloud;
        TestCase2Cloud tcTransform = new TestCase2Cloud(
            EAController.getInstance().getPlaftormInfo()
        );

        bRet = bPerformMutation = bErrorMutating = false;
        result = null;

        try {
            // 1. Duplica el cromosoma concreto
            result = dup();

            // 2. Prepara IDs y decide mutar
            nTcId = EAController.getInstance().getTcIndex();
            nIteration = EAController.getInstance().getIteration();
            bPerformMutation = EAController.getInstance().calculateMutation();

            if (result != null && bPerformMutation) {
                mTc = result.getMetaTC();
                result.setId(nTcId);

                if (mTc != null) {
                    tcInput = (TcInput_cloud) mTc.getTestCaseInput();
                    if (tcInput != null) {
                        // Duplica input y transforma
                        tcInputNew = tcInput.dupTc();
                        mCloud = tcTransform.transformTestcase2Cloud(
                            EAController.getInstance().getPlaftormInfo(), tcInput
                        );

                        // Mutación en el sistema
                        nMutationOperator = EAController.getInstance().getLastSelectedMutationOperator();
                        try {
                            mCloud.mutate(nMutationOperator,
                                EAController.getInstance().getMRList()
                            );
                            mCloud.printShortResume();
                        } catch (Exception e) {
                            System.out.printf(
                                "mutate - Error mutating with operator %d%n",
                                nMutationOperator
                            );
                            bErrorMutating = true;
                        }

                        if (!bErrorMutating) {
                            // Reconstruye el test case y guarda archivos
                            tcInputNew = (TcInput_cloud) tcTransform
                                .transformCloud2Testcase(
                                    EAController.getInstance().getPlaftormInfo(),
                                    tcInputNew, mCloud
                                );

                            EAController.getInstance().createNewIterationPath(nIteration);
                            mTcNew = EAController.getInstance()
                                .createNewIndividualFiles(nIteration, nTcId, tcInputNew);

                            if (EAController.getInstance().getLogLevel().getValue()
                                >= LogLevel.eLOG.getValue()) {
                                System.out.printf(
                                    "mutate - Mutant individual %d created successfully | MOP: %d%n",
                                    nTcId, nMutationOperator
                                );
                            }

                            EAController.getInstance().incCreatedIndIndex();
                            result.setMetaTC(mTcNew);
                            bRet = true;
                        } else {
                            // fallo mutación
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
        } catch (NullPointerException ex) {
            System.out.println("mutate - NullPointerException");
        }

        if (bPerformMutation && (result == null || !bRet)) {
            System.out.println(
                "mutate - Returning chromosome incorrect or mutation not applied"
            );
        }
        return result;
    }

    /**
     * Lógica genérica de crossover extraída de ambas implementaciones (CloudChromosome y MOCloudChromosome).
     */
    @Override
    public List<C> crossover(C other) {
        C chrom1, chrom2;
        MetaTestCase mTc1, mTc2, mTcNew1, mTcNew2;
        TcInput_cloud tcInput1, tcInput2, tcInputNew1, tcInputNew2;
        MutableCloud mCloud1, mCloud2;
        TestCase2Cloud tcTransform;
        List<C> resultList;
        LinkedList<MutableCloud> cloudCross;
        int nTcId, nIteration, nCrossoverOperator;
        boolean bRet, bPerformCrossover, bError;

        bRet = bPerformCrossover = bError = false;
        chrom1 = chrom2 = null;
        tcTransform = new TestCase2Cloud(
            EAController.getInstance().getPlaftormInfo()
        );
        resultList = new LinkedList<>();

        try {
            // 1. Duplica ambos
            chrom1 = this.dup();
            chrom2 = other.dup();

            // 2. IDs y decision crossover
            nTcId         = EAController.getInstance().getTcIndex();
            nIteration    = EAController.getInstance().getIteration();
            bPerformCrossover = EAController.getInstance().calculateCrossover();

            if (chrom1 != null && chrom2 != null && bPerformCrossover) {
                mTc1 = chrom1.getMetaTC();
                mTc2 = chrom2.getMetaTC();
                chrom1.setId(nTcId);
                chrom2.setId(nTcId + 1);

                if (mTc1 != null && mTc2 != null) {
                    tcInput1 = (TcInput_cloud) mTc1.getTestCaseInput();
                    tcInput2 = (TcInput_cloud) mTc2.getTestCaseInput();

                    if (tcInput1 != null && tcInput2 != null) {
                        // Duplica inputs y transforma
                        tcInputNew1 = tcInput1.dupTc();
                        tcInputNew2 = tcInput2.dupTc();
                        mCloud1 = tcTransform.transformTestcase2Cloud(
                            EAController.getInstance().getPlaftormInfo(), tcInput1
                        );
                        mCloud2 = tcTransform.transformTestcase2Cloud(
                            EAController.getInstance().getPlaftormInfo(), tcInput2
                        );

                        // Crossover en sistema
                        nCrossoverOperator = EAController.getInstance()
                            .getLastSelectedCrossoverOperator();
                        try {
                            cloudCross = mCloud1.crossover(
                                nCrossoverOperator, mCloud2,
                                EAController.getInstance().getMRList()
                            );
                            if (cloudCross != null && cloudCross.size() >= 2) {
                                mCloud1 = cloudCross.get(0);
                                mCloud2 = cloudCross.get(1);
                                mCloud1.printShortResume();
                                mCloud2.printShortResume();
                            }
                        } catch (Exception e) {
                            System.out.printf(
                                "crossover - Error with operator %d%n",
                                nCrossoverOperator
                            );
                            bError = true;
                        }

                        if (!bError) {
                            // Reconstruye y guarda
                            tcInputNew1 = (TcInput_cloud) tcTransform
                                .transformCloud2Testcase(
                                    EAController.getInstance().getPlaftormInfo(),
                                    tcInputNew1, mCloud1
                                );
                            tcInputNew2 = (TcInput_cloud) tcTransform
                                .transformCloud2Testcase(
                                    EAController.getInstance().getPlaftormInfo(),
                                    tcInputNew2, mCloud2
                                );

                            EAController.getInstance().createNewIterationPath(nIteration);
                            mTcNew1 = EAController.getInstance()
                                .createNewIndividualFiles(nIteration, nTcId, tcInputNew1);
                            mTcNew2 = EAController.getInstance()
                                .createNewIndividualFiles(nIteration, nTcId+1, tcInputNew2);

                            if (EAController.getInstance().getLogLevel().getValue()
                                >= LogLevel.eLOG.getValue()) {
                                System.out.printf(
                                    "crossover - Individuals %d & %d created | OP: %d%n",
                                    nTcId, nTcId+1, nCrossoverOperator
                                );
                            }

                            chrom1.setMetaTC(mTcNew1);
                            chrom2.setMetaTC(mTcNew2);
                            resultList.add(chrom1);
                            resultList.add(chrom2);
                            EAController.getInstance().incCreatedIndIndex();
                            EAController.getInstance().incCreatedIndIndex();
                            bRet = true;
                        }
                    }
                }
            }
        } catch (NullPointerException ex) {
            System.out.println("crossover - NullPointerException");
        }

        if (!bRet) {
            System.out.println(
                "crossover - No valid offspring generated"
            );
        }
        return resultList;
    }

    @Override
    public abstract C dup();

    @Override
    protected abstract C clone();
}
