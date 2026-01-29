package main.java;

import java.util.LinkedList;

import transformations.TestCase2Cloud;
import auxiliar.AuxFunctions;
import configuration.EAController;
import dataParser.TestCase;
import dataParser.TestCaseInput;
import dataParser.TestCaseParser;
import dataParser.cloud.ECloudSimulator;
import dataParser.cloud.TestCaseParser_cloud;
import dataParser.cloud.input.TcInput_cloud;
import dataParser.metadata.MetaParser;
import dataParser.metadata.MetaTestCase;
import mutation.MutableCloud.MutableCloud;

public class TestXml {

	public static void main(String[] args) {
		MetaParser metaParser;
		String strPath;
		TestCaseInput tcInput;
		MetaTestCase mTc;
		TestCaseParser tcParser;
		MutableCloud mCloudSystem;
		TestCase2Cloud tcTransform;
		LinkedList<MetaTestCase> metaTcList;
		
		EAController.getInstance().setPlaftormInfo(ECloudSimulator.eSIMGRID);
		strPath ="/localSpace/cloudEnergy/simGrid/evolutionary/initialPopulation/sample/metaInfo";
		metaParser = new MetaParser();
		tcParser = new TestCaseParser_cloud(EAController.getInstance().getPlaftormInfo());		
		tcTransform = new TestCase2Cloud(EAController.getInstance().getPlaftormInfo());		
		metaTcList = metaParser.loadMetaTcFolder(strPath);
		
		
		//Execute first the 
		if(metaTcList != null)
		{
			
			for(int i=0;i<metaTcList.size();i++)
			{
				mTc = metaTcList.get(i);
				
				if(mTc != null)
				{
					//Load the TcInput
					tcInput = tcParser.doParseInput(mTc.getTcInput());
					
					if(tcInput != null)
					{
						//Set the object
						mTc.setTestCase(new TestCase(mTc.getTcId(), tcInput, null));
						
						//Convert to the programming model
						mCloudSystem = tcTransform.transformTestcase2Cloud(EAController.getInstance().getPlaftormInfo(), tcInput);

						mCloudSystem.printResume();
						if(mCloudSystem != null)
						{
							//Do mutations
							
							//Do crossovers
							
							//Transform to tc
							tcInput = tcTransform.transformCloud2Testcase(EAController.getInstance().getPlaftormInfo(), (TcInput_cloud)tcInput, mCloudSystem);
							
							//Save the tc
							AuxFunctions.saveCloudToXml(tcInput.getModel(), "/localSpace/cloudEnergy/simGrid/test.xml");
						}
						else
						{
							System.out.println("Error loading initial population!");
						}
					}
				}
			}			
		}
	}

}
