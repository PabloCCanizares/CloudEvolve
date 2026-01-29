package main.java;

import java.util.LinkedList;

import dataParser.TestCaseParser;
import dataParser.cloud.ECloudSimulator;
import dataParser.cloud.TestCaseParser_cloud;
import dataParser.cloud.input.TcInput_cloud;
import dataParser.cloud.output.TcOutput_cloud;
import dataParser.metadata.MetaParser;
import dataParser.metadata.MetaTestCase;

public class MT_Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MetaParser metaParser;
		MetaTestCase metaTc;
		TcInput_cloud tcInput;
		TcOutput_cloud tcOutput;
		String strMetaParser;
		LinkedList<MetaTestCase> metaList;
		TestCaseParser tcParser;
		
		strMetaParser = "/localSpace/cloudEnergy/cloudsimStorage/evolutionary/2019-01-16_18:01:44/0014/metaInfo/tc_00078.mtc";
		metaParser = new MetaParser();
		tcParser = new TestCaseParser_cloud(ECloudSimulator.eCLOUDSIMSTORAGE);
		
		//Load an specific TC and try to mutate it		
		if(metaParser.loadSingleMetaTC(strMetaParser))
		{
			metaList = metaParser.getTcList();
			
			for(int i=0;i<metaList.size();i++)
			{
				metaTc = metaList.get(i);
				tcInput = (TcInput_cloud) tcParser.doParseInput(metaTc.getTcInput());
				tcOutput = (TcOutput_cloud) tcParser.doParseOutput(metaTc.getTcOutput());
				
				System.out.println("Input->"+tcInput.ToString());
				System.out.println("Output->"+tcOutput.ToString());
			}
		}
	}

}
