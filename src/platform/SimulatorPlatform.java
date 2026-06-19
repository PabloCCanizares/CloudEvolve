package platform;

import java.util.LinkedList;
import java.util.List;

import configuration.EACrossoverOperator;
import configuration.EAMutationOperator;

/**
 * Strategy that captures the behaviour which varies per cloud-simulator backend.
 *
 * <p>It replaces the {@code switch (eSimulator)} blocks that were scattered
 * across the orchestrators and launchers. Obtain the implementation for a given
 * {@link dataParser.cloud.ECloudSimulator} through {@link SimulatorPlatforms#of}.</p>
 *
 * <p>The interface starts by owning just the evolutionary base path; it is meant
 * to grow as further per-simulator decisions (mutation/crossover operator
 * registration, test-case execution, cloud transformation) are migrated off
 * their own switches.</p>
 */
public interface SimulatorPlatform {

    /** Default root path under which this simulator's evolutionary runs are stored. */
    String evolutionaryBasePath();

    /**
     * Appends this simulator's mutation operators to {@code operators}, consuming
     * one probability per operator (in declaration order) from {@code probabilities}.
     */
    void registerMutationOperators(List<EAMutationOperator> operators, LinkedList<Double> probabilities);

    /**
     * Appends this simulator's crossover operators (with their fixed probabilities)
     * to {@code operators}.
     */
    void registerCrossoverOperators(List<EACrossoverOperator> operators);
}
