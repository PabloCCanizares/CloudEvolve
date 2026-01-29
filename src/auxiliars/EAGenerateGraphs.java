package auxiliars;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import auxiliar.CommandExecutor;
import configuration.EAController;
import dataParser.IPlatformInfo;
import dataParser.cloud.ECloudSimulator;
import graphs.GAGraph;
import graphs.GraphGenerator;
import graphs.MOGAGraph;

public class EAGenerateGraphs {

	final static String HISTOGRAM_DAT_TAG = "histogram.dat";
	final static String HISTOGRAM_GNU_TAG = "histogram.gnu";
	final static String EVOLUTION_DAT_TAG = "evolution.dat";
	final static String EVOLUTION_GNU_TAG = "evolution.gnu";
	
	public void generateGraphs(String strPath, String strSessionPath) {
		// Generate .dat info
		GraphGenerator graphGen;

		graphGen = new MOGAGraph();
		if (graphGen.generateGraph(strSessionPath, EAController.getInstance().getPlaftormInfo())) {
			doOperations(strPath, strSessionPath);
		}
	}

	private void doOperations(String strPath, String strSessionPath) {
		// CommandExecutor.exec("cp
		// "+strPath+File.separator+"gnuplot"+File.separator+HISTOGRAM_GNU_TAG+"
		// "+strSessionPath);
		CommandExecutor.exec("cp " + strPath + File.separator + HISTOGRAM_GNU_TAG + " " + strSessionPath);
		CommandExecutor.exec("cp " + removeLastDirectory(strPath) + File.separator + HISTOGRAM_GNU_TAG + " " + strSessionPath);
		
		//Copiamos el GNU
		CommandExecutor.exec("cp " + strPath + File.separator +  EVOLUTION_GNU_TAG + " "+ strSessionPath);
		
		CommandExecutor.exec("cp " + removeLastDirectory(strPath) + File.separator +  EVOLUTION_GNU_TAG + " "+ strSessionPath);		
		// Execute the command to generate the histogram
		// CommandExecutor.exec("cd "+strSessionPath+" && gnuplot
		// "+strSessionPath+File.separator+HISTOGRAM_GNU_TAG);
		CommandExecutor.exec(
				"cd " + strSessionPath + " && gnuplot " + strSessionPath + File.separator + HISTOGRAM_GNU_TAG);
		// Execute the command to generate the evolution graph
		// CommandExecutor.exec("cd "+strSessionPath+" && gnuplot
		// "+strSessionPath+File.separator+EVOLUTION_GNU_TAG);
		CommandExecutor.exec(
				"cd " + strSessionPath + " && gnuplot " + strSessionPath + File.separator + EVOLUTION_GNU_TAG);
	}
	
	 public  String removeLastDirectory(String raw) {
	        if (raw == null || raw.isBlank()) return raw;

	        Path p = Paths.get(raw).normalize();
	        Path parent = p.getParent();

	        if (parent == null) {
	            // Si no hay parent, pero hay raíz ("/" o "C:\") la devolvemos; si no, "".
	            Path root = p.getRoot();
	            return (root != null) ? root.toString() : "";
	        }
	        return parent.toString();
	    }
	 
	public void generateGraphs(String strPath, String strSessionPath, IPlatformInfo ePlatFormInfo) {
		// Generate .dat info
		GraphGenerator graphGen;

		graphGen = new MOGAGraph();
		if (graphGen.generateGraph(strSessionPath, ePlatFormInfo)){			
			doOperations(strPath, strSessionPath);
		}
	}
	
}
