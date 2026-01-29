package main_scico.experiments;

import main_scico.aux.EvolutionStats;
import main_scico.hv.HyperVolumePerAlgorithm;

public class launcherHV{

	public static void main(String [] args)
	{
		String[] aux = new String[1];
		
		System.out.println("Initialising HV");
		//Recalculamos hipervolumenes de todos los algoritmos (Con los puntos de referencia ajustados internamente)
		aux[0] = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/VEGA/";
		HyperVolumePerAlgorithm.main(aux); //eVEGA
		aux[0] = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/MOGA/";
		HyperVolumePerAlgorithm.main(aux); //eMOGA
		aux[0] = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/PAES2/";
		HyperVolumePerAlgorithm.main(aux); //ePAES
		aux[0] = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/NSGAII/";
		HyperVolumePerAlgorithm.main(aux); //eNSGAII
		aux[0] = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/SPEA2/";
		HyperVolumePerAlgorithm.main(aux); //eSPEA2
		
		//Calculamos las métricas: para ello es necesario haber obtenido los HVs y métricas por iteracion
		aux[0] = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/PAES2";
		EvolutionStats.main(aux);
		aux[0] = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/MOGA";
		EvolutionStats.main(aux);
		aux[0] = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/VEGA";
		EvolutionStats.main(aux);
		aux[0] = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/NSGAII";
		EvolutionStats.main(aux);
		aux[0] = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/SPEA2";
		EvolutionStats.main(aux);
		
		
		System.out.println("End of experiments");
		//Y de aqui, debería salir una nueva carpeta, con todos los ficheros de hipervolumenes para las gráficas
	}
}
