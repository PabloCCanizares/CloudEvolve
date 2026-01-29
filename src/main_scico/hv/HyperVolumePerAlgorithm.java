package main_scico.hv;

import java.nio.file.Path;
import java.util.List;

import main_scico.aux.FindBigFolders;

public class HyperVolumePerAlgorithm {

	public static void main(String args[])
	{
		String strAlgorithmBase;

    	if (args.length == 0) {
            //strAlgorithmBase = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/grid_hv/VEGA_grid_hv/";
    		strAlgorithmBase = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/PAES2/";
        }
        else
        	strAlgorithmBase = args[0];
    	
    	System.out.println("Exploring folder: "+strAlgorithmBase);
    	
		try {
			//strAlgorithmBase = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/PAES2/";
			//Tomamos la base, y de esta base extraemos las carpetas más densas (100 iteraciones debajo)
			calcHV(strAlgorithmBase, "Al_w1", "18.826120, 6075.100000");
			calcHV(strAlgorithmBase, "Al_w3", "24.149430, 6618.890000");
			calcHV(strAlgorithmBase, "Bl_w1", "17.088820, 6942.100000");
			calcHV(strAlgorithmBase, "Bl_w3", "18.103740, 10254.510400");
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void calcHV(String strAlgorithmBase, String strWorkload, String strReference)
			throws Exception {
		
		String aux[] = new String[4];
		String auxFolders[] = new String[2];
		
		auxFolders[0] = strAlgorithmBase+strWorkload;
		auxFolders[1] = "100";
		List<Path> paths = FindBigFolders.main(auxFolders);
		
		for(Path path: paths)
		{
			aux[0]=(String)path.toAbsolutePath().toString();
			aux[1]="--virtual-archive";
			aux[2]="--ref";			
			aux[3]=strReference;
			ParetoAndHVFromLogs.main(aux);
		}
		//ParetoAndHVFromLogs.calcNadir(strAlgorithmBase+strWorkload);
	}
}
