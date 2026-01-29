package main.java;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
	
public class TestExec {


    public static void main(String args[]) {
    	
    	
    	Test1();
    	//Test2();
    }
    
    public static void Test1()
    {
    	ProcessBuilder processBuilder = new ProcessBuilder();

    	// -- Linux --

    	// Run a shell command
    	processBuilder.command("bash", "-c", "timeout 10 java -jar /localSpace/cloudEnergy/simGrid/simGrid.jar --standalone /localSpace/cloudEnergy/simGrid/evolutionary/initialPopulation/sample/metaInfo/tc_00000.mtc &> /localSpace/cloudEnergy/simGrid/evolutionary/initialPopulation/sample/tcOutput/output_00000.tc");

    	// Run a shell script
    	//processBuilder.command("path/to/hello.sh");

    	// -- Windows --

    	// Run a command
    	//processBuilder.command("cmd.exe", "/c", "dir C:\\Users\\mkyong");

    	// Run a bat file
    	//processBuilder.command("C:\\Users\\mkyong\\hello.bat");

    	try {

    		Process process = processBuilder.start();

    		StringBuilder output = new StringBuilder();

    		BufferedReader reader = new BufferedReader(
    				new InputStreamReader(process.getInputStream()));

    		String line;
    		while ((line = reader.readLine()) != null) {
    			output.append(line + "\n");
    		}

    		int exitVal = process.waitFor();
    		if (exitVal == 0) {
    			System.out.println("Success!");
    			System.out.println(output);
    			//System.exit(0);
    		} else {
    			//abnormal...
    		}

    	} catch (IOException e) {
    		e.printStackTrace();
    	} catch (InterruptedException e) {
    		e.printStackTrace();
    	}
    }
    public static void Test2()
    {
        String s;
        Process p;
        try {
            p = Runtime.getRuntime().exec("java -jar /localSpace/cloudEnergy/simGrid/simGrid.jar --standalone /localSpace/cloudEnergy/simGrid/evolutionary/initialPopulation/sample/metaInfo/tc_00000.mtc &> test.txt");
            BufferedReader br = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
            while ((s = br.readLine()) != null)
                System.out.println("line: " + s);
            p.waitFor();
            System.out.println ("exit: " + p.exitValue());
            p.destroy();
        } catch (Exception e) {}
    }
	
}
