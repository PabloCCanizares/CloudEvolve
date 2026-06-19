package platform;

import java.util.LinkedList;
import java.util.List;

import configuration.EACrossoverOperator;
import configuration.EAMutationOperator;

/** {@link SimulatorPlatform} for the CloudSim-Storage backend. */
public final class CloudSimStoragePlatform implements SimulatorPlatform {

    @Override
    public String evolutionaryBasePath() {
        return PlatformPaths.evolutionaryBase("cloudsimStorage");
    }

    @Override
    public void registerMutationOperators(List<EAMutationOperator> operators, LinkedList<Double> probabilities) {
        if (probabilities.size() >= 4) {
            operators.add(new EAMutationOperator(1, probabilities.removeFirst(), "Seed changes on a percentage of nodes of the system", true));
            operators.add(new EAMutationOperator(2, probabilities.removeFirst(), "Randomly modifies the bandwidth of a network link of the cloud", true));
            operators.add(new EAMutationOperator(3, probabilities.removeFirst(), "Randomly modifies the latency of a network link of the cloud", true));
            operators.add(new EAMutationOperator(7, probabilities.removeFirst(), "Delete a rack and all the links connected to it", true));
        }
    }

    @Override
    public void registerCrossoverOperators(List<EACrossoverOperator> operators) {
        operators.add(new EACrossoverOperator(1, 0.0,
                "Seed changes on a percentage of nodes of the system", true));
        operators.add(new EACrossoverOperator(2, 100.0,
                "Given two parents, a random rack is selected from the first one and is exchanged "
              + "from a random rack from the second one to generate a new individual.", true));
    }
}
