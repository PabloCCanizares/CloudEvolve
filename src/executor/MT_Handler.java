package executor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import configuration.EAController;
import configuration.LogLevel;
import dataParser.TestCaseInput;
import dataParser.cloud.ECloudSimulator;
import dataParser.cloud.TestCaseParser_cloud;
import dataParser.cloud.input.TcInput_cloud;
import dataParser.cloud.output.TcOutput_cloud;
import dataParser.metadata.MetaTestCase;
import main.ConfigMT;
import platform.SimulatorExecution;
import platform.SimulatorPlatforms;

public class MT_Handler implements SimulatorExecution {

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

					bRet = SimulatorPlatforms.of(platformInfo).execute(this, metaTC);

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

	// ── SimulatorExecution: the seam the platform strategies launch through ──

	@Override
	public String timeoutHeader() {
		return getTimeoutHeader();
	}

	@Override
	public String simulatorPath() {
		return ConfigMT.getSingletonInstance().getSimulatorPath();
	}

	@Override
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

	@Override
	public boolean executeCommand(String strCommand) {
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

	private static volatile String CMD_PREFIX = null;
	private static final Object CMD_LOCK = new Object();

	/**
	 * Detecta el SO una única vez, construye y cachea el prefijo del comando
	 * (timeout + "java -jar <jar-SO> "). Las siguientes llamadas devuelven
	 * el valor cacheado e ignoran los parámetros.
	 */
	public  String getTimeoutHeader() {
	    String cached = CMD_PREFIX;
	    if (cached != null) return cached;

	    synchronized (CMD_LOCK) {
	        if (CMD_PREFIX != null) return CMD_PREFIX;

	        String os = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT);
	        StringBuilder head = new StringBuilder();

	        if (os.contains("mac")) {
	            String gtimeout = firstExecutable(
	                "/opt/homebrew/bin/gtimeout",    // Apple Silicon
	                "/usr/local/bin/gtimeout",       // Intel
	                "gtimeout"                       // PATH (fallback)
	            );
	            head.append(gtimeout);

	        } else if (os.contains("win")) {
	            head.append("cmd /c ");

	        } else {
	            head.append("timeout ");
	        }

	        CMD_PREFIX = head.toString();
	        return CMD_PREFIX;
	    }
	}

    /** Devuelve el primer candidato que existe y es ejecutable; si ninguno, devuelve el último literal. */
    private  String firstExecutable(String... candidates) {
        for (int i = 0; i < candidates.length - 1; i++) {
            Path p = Paths.get(candidates[i]);
            if (Files.exists(p) && Files.isExecutable(p)) return candidates[i];
        }
        return candidates[candidates.length - 1];
    }
}
