package main_scico.aux;

/**
 * Given a folder, recalculates the energy consumption, performance, hipervolume and aggregations.
 * Antes de este ejecutor, hay que lanzar el HyperVolumePerAlgorithm (ajustando los puntos )
 * @author Pablo C. Cañizares
 *
 */
public class MetricsCalculator {

	public static void main(String[] args)
	{
		String[] aux = new String[1];
		aux[0] = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/PAES2-Ablation";
		EvolutionStats.main(aux);
	}
}

