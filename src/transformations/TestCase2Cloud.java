package transformations;

import java.io.File;
import java.io.IOException;

import java.io.File;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.xml.sax.SAXException;

import auxiliar.CloudModel2Xml;
import auxiliar.cloud.CloudCreator;
import configuration.EAController;
import dataParser.TestCaseInput;
import dataParser.cloud.ECloudSimulator;
import dataParser.cloud.input.TcInput_cloud;
import entities.cloud.Cpu;
import entities.cloud.Network;
import entities.cloud.Ram;
import entities.cloud.Storage;
import mutation.MutableCloud.MutableCloud;
import mutation.MutableCloud.MutableNode;
import transformations.IModel;
import transformations.IModel2Xml;

public class TestCase2Cloud {

	CloudCreator cloudCreator;
	ECloudSimulator eCloudSimulator;
	
	public TestCase2Cloud(ECloudSimulator eCloudSimulator)
	{		
		cloudCreator = new CloudCreator(eCloudSimulator);
		this.eCloudSimulator = eCloudSimulator;
	}
	public MutableCloud transformTestcase2Cloud(ECloudSimulator ePlatformInfo, TestCaseInput tcInput)
	{
		MutableCloud mutableCloudSystem;
		
		mutableCloudSystem = null;
		if(tcInput != null)
		{
			
			if(ePlatformInfo == ECloudSimulator.eCLOUDSIMSTORAGE)
			{
				mutableCloudSystem = transformCloudSim(tcInput);			
			}
			else if(ePlatformInfo == ECloudSimulator.eSIMGRID)
			{
				TestCaseTransformations tcSimgrid = new TC2Cloud_simGrid();
				mutableCloudSystem = tcSimgrid.transformTestCase2Cloud(tcInput);	
			}
		}
		return mutableCloudSystem;		
	}


	public TestCaseInput transformCloud2Testcase(ECloudSimulator ePlatformInfo, TcInput_cloud tcInputNew, MutableCloud mutCloud)
	{
		TestCaseInput tcInput;
		tcInput = null;
		if(mutCloud != null)
		{
			
			if(ePlatformInfo == ECloudSimulator.eCLOUDSIMSTORAGE)
			{
				tcInput = transformCloudSim2TcInput(tcInputNew, mutCloud);			
			}
			else if (ePlatformInfo == ECloudSimulator.eSIMGRID)
			{
				tcInput = transformSimgrid2TcInput(tcInputNew, mutCloud);		
			}
		}		
		return tcInput;
	}
	
	private TestCaseInput transformSimgrid2TcInput(TcInput_cloud tcInputNew, MutableCloud mutCloud) {
		TcInput_cloud tcInput;
		
		tcInput = null;
		
		if(mutCloud != null && tcInputNew != null)
		{
			//The main idea is to clone the tcinput, and modify only the parts that the algorithm needs
			tcInput = tcInputNew;

			//Save XML model
			//saveCloudToXml(mutCloud, "/localSpace/cloudEnergy/simgrid/test.xml");
			tcInput.setCloudModel((IModel)mutCloud.getCloudSystem());
			
			//Quantity
			tcInput.setHostQuantity(mutCloud.getNumHosts());
				
			//CPU
			tcInput.setHostMips((int)mutCloud.getMips());
			
			//RAM: N.A.
			
			//RAMSPEED: N.A.
				
			//STO
			tcInput.setIOMaxRate((int)mutCloud.getIORate());
						
			//NetBW			
			tcInput.setNetBandwidth((int)mutCloud.getNetPerformance());
			
			//LATENCY
			tcInput.setNetLatency((int)mutCloud.getNetLatency());
		}
		
		return tcInput;
	}
	
	private TestCaseInput transformCloudSim2TcInput(TcInput_cloud tcInputNew, MutableCloud mutCloud) {
		
		TcInput_cloud tcInput;
		
		tcInput = null;
		if(mutCloud != null && tcInputNew != null)
		{
			//The main idea is to clone the tcinput, and modify only the parts that the algorithm needs
			tcInput = tcInputNew;
								
			//Quantity
			tcInput.setHostQuantity(mutCloud.getNumHosts());
				
			//CPU
			tcInput.setHostMips((int)mutCloud.getMips());
			
			//RAM: N.A.
			
			//RAMSPEED: N.A.
				
			//STO
			tcInput.setIOMaxRate((int)mutCloud.getIORate());
						
			//NetBW			
			tcInput.setNetBandwidth((int)mutCloud.getNetPerformance());
			
			//LATENCY
			tcInput.setNetLatency((int)mutCloud.getNetLatency());
		}

		
		return tcInput;
	}
	private MutableCloud transformCloudSim(TestCaseInput tcInput) {
		
		MutableCloud mutableCloud;
		MutableNode mutableNode;		
		Cpu hostCpu;
		Storage hostStorage;
		Ram hostRam;
		Network hostNetwork;
		TcInput_cloud tcInputCloud;
		int nNumNodes, nNumRacks, nNumBlades;
		
		mutableCloud = null;
		nNumNodes = 0;
		nNumRacks = nNumBlades = 10;
		
		tcInputCloud = (TcInput_cloud)tcInput;
		if(tcInputCloud != null)
		{
			mutableCloud = new MutableCloud(this.eCloudSimulator);
			nNumNodes = tcInputCloud.getHostQuantity();
			hostCpu = cloudCreator.createCPU(tcInputCloud.getHostMips(), tcInputCloud.getHostPes());
			hostStorage = cloudCreator.createIO(tcInputCloud.getHostIoCapacity(), tcInputCloud.getIoPerformance(),tcInputCloud.getMaxIoTransferRate(), tcInputCloud.getIOLatency());			
			hostRam = cloudCreator.createRAM(tcInputCloud.getHostRam(), tcInputCloud.getRamBandwidth());
			hostNetwork = cloudCreator.createNetwork(tcInputCloud.getNetId(),tcInputCloud.getNetBandwidth(), tcInputCloud.getNetLatency());
			mutableNode = cloudCreator.createMutableNode(hostCpu, hostStorage, hostRam, hostNetwork);			
			mutableCloud = cloudCreator.createHomogeneousCloud(nNumRacks, nNumBlades, nNumNodes, mutableNode,hostNetwork);
		}
		
		return mutableCloud;
	}
	
}
