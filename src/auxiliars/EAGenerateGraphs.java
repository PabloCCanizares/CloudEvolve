package auxiliars;

import java.io.File;

import auxiliar.CommandExecutor;
import configuration.EAController;
import graphs.GAGraph;
import graphs.GraphGenerator;

public class EAGenerateGraphs {

	final static String HISTOGRAM_DAT_TAG = "histogram.dat";
	final static String HISTOGRAM_GNU_TAG = "histogram.gnu";
	final static String EVOLUTION_DAT_TAG = "evolution.dat";
	final static String EVOLUTION_GNU_TAG = "evolution.gnu";
	
	public void generateGraphs(String strPath, String strSessionPath) {
		// Generate .dat info
		GraphGenerator graphGen;

		graphGen = new GAGraph();
		if (graphGen.generateGraph(strSessionPath, EAController.getInstance().getPlaftormInfo())) {
			// CommandExecutor.exec("cp
			// "+strPath+File.separator+"gnuplot"+File.separator+HISTOGRAM_GNU_TAG+"
			// "+strSessionPath);
			CommandExecutor.exec("cp " + strPath + File.separator + HISTOGRAM_DAT_TAG + " " + strSessionPath);

			// CommandExecutor.exec("cp
			// "+strPath+File.separator+"gnuplot"+File.separator+EVOLUTION_GNU_TAG+"
			// "+strSessionPath);
			CommandExecutor.exec("cp " + strPath + File.separator + "gnuplot" + File.separator + EVOLUTION_DAT_TAG + " "
					+ strSessionPath);
			// Execute the command to generate the histogram
			// CommandExecutor.exec("cd "+strSessionPath+" && gnuplot
			// "+strSessionPath+File.separator+HISTOGRAM_GNU_TAG);
			CommandExecutor.exec(
					"cd " + strSessionPath + " && gnuplot " + strSessionPath + File.separator + HISTOGRAM_DAT_TAG);
			// Execute the command to generate the evolution graph
			// CommandExecutor.exec("cd "+strSessionPath+" && gnuplot
			// "+strSessionPath+File.separator+EVOLUTION_GNU_TAG);
			CommandExecutor.exec(
					"cd " + strSessionPath + " && gnuplot " + strSessionPath + File.separator + EVOLUTION_DAT_TAG);
		}
	}
}
