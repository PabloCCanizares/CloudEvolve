package executor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import configuration.EAController;
import configuration.LogLevel;
import dataParser.TestCaseInput;
import dataParser.cloud.ECloudSimulator;
import dataParser.cloud.TestCaseParser_cloud;
import dataParser.cloud.input.TcInput_cloud;
import dataParser.cloud.output.TcOutput_cloud;
import dataParser.metadata.MetaTestCase;
import main.ConfigMT;

public class MT_Handler {

	double dEnergy;
	double dTime;

	public double getEnergy() {
		return dEnergy;
	}

	public void setEnergy(double dEnergy) {
		this.dEnergy = dEnergy;
	}

	public double getdTime() {
		return dTime;
	}

	public void setdTime(double dTime) {
		this.dTime = dTime;
	}

	public double executeSingleTC(MetaTestCase metaTC, ECloudSimulator platformInfo) {

		boolean bRet;
		int nHosts, nVMs;
		String strPathInput, strPathOutput, strCloudXml;
		TestCaseInput tcInput;
		TcOutput_cloud tcOutput;
		TestCaseParser_cloud oTestCaseParser;

		int nTestcaseId;
		long startTime;
		long estimatedTime;

		nTestcaseId = -1;
		bRet = false;

		try {
			if (EAController.getInstance().getLogLevel().getValue() >= LogLevel.eLOG.getValue())
				System.out.println("executeSingleTC - Init");

			if (metaTC != null) {
				oTestCaseParser = new TestCaseParser_cloud(platformInfo);

				// Test case id
				nTestcaseId = metaTC.getTcId();

				// Parse the input
				strPathInput = metaTC.getTcInput();
				strPathOutput = metaTC.getTcOutput();

				tcInput = oTestCaseParser.doParseInput(strPathInput);

				if (tcInput != null) {
					ConfigMT.getSingletonInstance().tcInput = (TcInput_cloud) tcInput;
					ConfigMT.getSingletonInstance().setOutputFilename(metaTC.getTcOutput());

					if (EAController.getInstance().getLogLevel().getValue() >= LogLevel.eVERBOSE.getValue())
						System.out.printf("=====> Executing the test case: %d\n", nTestcaseId);

					startTime = System.currentTimeMillis();

					strCloudXml = ConfigMT.getSingletonInstance().tcInput.getCloudModelPath();
					nHosts = ConfigMT.getSingletonInstance().tcInput.getHostQuantity();
					nVMs = ConfigMT.getSingletonInstance().tcInput.getNumVMs();

					// It is neccesary in order to launch a quantity of VMs lesser than the number
					// of hosts
					if (nVMs < nHosts) {
						nHosts = nVMs;
						ConfigMT.getSingletonInstance().tcInput.setHostQuantity(nHosts);
					}

					switch (platformInfo) {
					case eSIMGRID:
						bRet = executeSimGrid(metaTC, strCloudXml, strPathOutput, nHosts);
						break;
					case eCLOUDSIMSTORAGE:
						bRet = executeCloudSim(metaTC, strCloudXml, strPathOutput, nHosts);
						break;
					default:
						bRet = false;
						break;
					}

					if (bRet) {
						// Parse the tc output
						tcOutput = (TcOutput_cloud) oTestCaseParser.doParseOutput(strPathOutput);

						if (tcOutput != null) {
							// load the energy
							dEnergy = (double) tcOutput.getTotalEnergyCons();
							dTime = (double) tcOutput.getSimTime();
						} else {
							System.out.printf("Test case output is empty, check TC-%d\n", nTestcaseId);
						}
					}

					estimatedTime = System.currentTimeMillis() - startTime;
					if (EAController.getInstance().getLogLevel().getValue() >= LogLevel.eVERBOSE.getValue())
						System.out.printf("Total execution time: %d\n", estimatedTime);
				} else
					throw new Exception("Error parsing test cases");
			}
		} catch (Exception ex) {
			System.out.printf("Exception catched while executing a test case. Check TC-%d\n", nTestcaseId);
		}

		return dEnergy;
	}

	private boolean executeCloudSim(MetaTestCase metaTC, String strCloudXml, String strPathOutput, int nHosts) {

		return executeCommand("timeout 60 java -jar /localSpace/cloudEnergy/cloudsimStorage/cloudsimStorage.jar --standalone "
				+ metaTC.getFilePath() /* +" &>"+metaTC.getTcOutput() */);
	}

	private boolean executeSimGrid(MetaTestCase metaTC, String strCloudXml, String strPathOutput, int nHosts) {

		executeCommandSimGrid("rm -r /tmp/simgrid*");
		return executeCommandSimGrid("timeout 60 java -jar /localSpace/cloudEnergy/simGrid/simGrid.jar --standalone "
				+ metaTC.getFilePath() + " &>" + metaTC.getTcOutput());
	}

	public boolean executeCommandSimGrid(String strCommand) {
		ProcessBuilder processBuilder = new ProcessBuilder();
		boolean bRet;
		// -- Linux --

		// Run a shell command
		processBuilder.command("bash", "-c", strCommand);

		// Run a shell script
		// processBuilder.command("path/to/hello.sh");

		// -- Windows --

		// Run a command
		// processBuilder.command("cmd.exe", "/c", "dir C:\\Users\\mkyong");

		// Run a bat file
		// processBuilder.command("C:\\Users\\mkyong\\hello.bat");

		bRet = false;
		try {

			Process process = processBuilder.start();

			StringBuilder output = new StringBuilder();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line + "\n");
			}

			int exitVal = process.waitFor();
			if (exitVal == 0) {
				bRet = true;
			} else {
				bRet = false;
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return bRet;
	}

	private boolean executeCommand(String strCommand) {
		Process p;
		boolean bRet;
		try {

			bRet = true;
			p = Runtime.getRuntime().exec(strCommand); // Redirect the output to null

			new Thread(new Runnable() {
				public void run() {
					BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
					String line = null;

					try {
						while ((line = input.readLine()) != null)
							System.out.println(line);
					} catch (IOException e) {
						System.out.println("Error executing command");
					}
				}
			}).start();

			try {
				p.waitFor();
			} catch (InterruptedException e) {
				System.out.println("Interruption catched while executing a command");
				bRet = false;
			}
		} catch (IOException e1) {
			bRet = false;
		}
		return bRet;
	}
}
