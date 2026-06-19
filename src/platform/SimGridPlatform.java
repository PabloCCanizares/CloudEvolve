package platform;

import java.util.LinkedList;
import java.util.List;

import configuration.EACrossoverOperator;
import configuration.EAMutationOperator;

/** {@link SimulatorPlatform} for the SimGrid backend. */
public final class SimGridPlatform implements SimulatorPlatform {

    @Override
    public String evolutionaryBasePath() {
        return PlatformPaths.evolutionaryBase("simGrid");
    }

    @Override
    public void registerMutationOperators(List<EAMutationOperator> operators, LinkedList<Double> probabilities) {
        operators.add(new EAMutationOperator(1, probabilities.removeFirst(), "Seed changes on a percentage of nodes of the system", true));
        operators.add(new EAMutationOperator(2, probabilities.removeFirst(), "Randomly modifies the bandwidth of a network link of the cloud", false));
        operators.add(new EAMutationOperator(3, probabilities.removeFirst(), "Randomly modifies the latency of a network link of the cloud", false));
        operators.add(new EAMutationOperator(4, probabilities.removeFirst(), "", false));
        operators.add(new EAMutationOperator(5, probabilities.removeFirst(), "", false));
        operators.add(new EAMutationOperator(6, probabilities.removeFirst(), "", false));
        operators.add(new EAMutationOperator(7, probabilities.removeFirst(), "Delete a rack and all the links connected to it", true));
        operators.add(new EAMutationOperator(8, probabilities.removeFirst(), "", false));
    }

    @Override
    public void registerCrossoverOperators(List<EACrossoverOperator> operators) {
        // SimGrid registers no crossover operators (legacy behaviour).
    }
}
