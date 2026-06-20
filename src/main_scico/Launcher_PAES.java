package main_scico;

import platform.PlatformPaths;

import main.java.Cloud_MO;

public class Launcher_PAES {

    public static String arrayToString(String[] array, String delimiter) {
        if (array == null || array.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }
    
	public static void main(String [] args)
	{
		String[] aux = new String[9];
		String strInitialPath;
		
		strInitialPath = PlatformPaths.evolutionaryBase("cloudsimStorage") + "/InitialPopulation/";
		//Algorithm
		aux[0] = "ePAES2";
		
		//Simulator
		aux[1] = "eCloudSimStorage";		
		
		//Path of the experiment
		aux[2] = strInitialPath+"Al_w3";
		
		//Number of iterations
		aux[3] = "100";
		
		//Mutation probability: 0->high, 1->mid, 2->low
		aux[4] = "0";
		
		//Rule base (fixed to 1)
		aux[5] = "1";
		
		//Path base (to save the experiments)
		aux[6] = PlatformPaths.evolutionaryBase("cloudsimStorage");
		
		//Number of re-runs
		aux[7] = "1";
		
		//Simulator .jar path
		aux[8] = PlatformPaths.workspace() + "/cloudsimStorage/cloudsimStorage.jar";
		
		System.out.println("Executing the experiment with config: "+arrayToString(aux, "\n"));
		
		/*for (int i=0;i<3;i++)
		{
			aux[2] = strInitialPath+"Al_w1";
			Cloud_MO.main(aux);
			
			aux[2] = strInitialPath+"Al_w3";
			Cloud_MO.main(aux);
			
			aux[2] = strInitialPath+"Bl_w1";
			Cloud_MO.main(aux);
			
			aux[2] = strInitialPath+"Bl_w3";
			Cloud_MO.main(aux);
		}*/
		
		for (int i=0;i<2;i++)
		{
			
			aux[2] = strInitialPath+"Al_w1";
			aux[4] = String.format("%d", i +1);
			Cloud_MO.main(aux);
			
			aux[2] = strInitialPath+"Al_w3";
			aux[4] = String.format("%d", i +1);
			Cloud_MO.main(aux);
			
			aux[2] = strInitialPath+"Bl_w1";
			aux[4] = String.format("%d", i +1);
			Cloud_MO.main(aux);
			
			aux[2] = strInitialPath+"Bl_w3";
			aux[4] = String.format("%d", i +1);
			Cloud_MO.main(aux);
		}
		
	}
}
