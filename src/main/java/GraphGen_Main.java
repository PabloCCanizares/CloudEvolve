package main.java;

import auxiliars.EAGenerateGraphs;
import dataParser.IPlatformInfo;
import dataParser.cloud.ECloudSimulator;

/**
 * Este programa permite tomar una carpeta donde ya se hayan realizado ejecuciones, y ajustar los ficheros de graficas
 * @author j0hn
 *
 */
public class GraphGen_Main {

	public static void main(String[] args)
	{
		EAGenerateGraphs graphGen;
		graphGen = new EAGenerateGraphs();
		IPlatformInfo plat = ECloudSimulator.eCLOUDSIMSTORAGE;
		
		String[] paths = {
			    "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-18_03:14:52_Bl_w1",
			    "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-17_23:13:36_Bl_w1",
			    "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-18_06:43:04_Bl_w1",
			    "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-18_12:16:09_Al_w1",
			    "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-18_17:27:22_Al_w1",
			    "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-18_04:09:11_Al_w3",
			    "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-18_11:19:40_Al_w3",
			    "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-18_09:49:27_Al_w1",
			    "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-18_05:12:28_Al_w1",
			    "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-18_12:08:33_Bl_w1",
			    "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-18_17:26:53_Al_w1",
			    "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-17_23:13:42_Al_w3",
			    "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-18_02:09:31_Al_w1",
			    "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-18_13:13:42_Al_w3",
			    "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-18_06:02:51_Al_w3",
			    "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-18_08:10:34_Al_w3",
			    "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-18_01:51:46_Al_w3",
			    "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-18_09:12:35_Bl_w1",
			    "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-18_09:39:48_Al_w3",
			    "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-17_23:13:49_Al_w1",
			    "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-18_07:40:41_Al_w1"
			};

		for (String path : paths) {
		    System.out.println(path);
		    graphGen.generateGraphs("/localSpace/cloudEnergy/cloudsimStorage/evolutionary",path, plat);
		}
		
		//graphGen.generateGraphs("/localSpace/cloudEnergy/cloudsimStorage/evolutionary","/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2025-09-17_23:13:36_Bl_w1", plat);
		
	}
}
