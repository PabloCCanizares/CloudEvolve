package transformations;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

import auxiliar.AuxFunctions;
import auxiliar.cloud.CloudCreator;
import dataParser.TestCaseInput;
import dataParser.cloud.ECloudSimulator;
import dataParser.cloud.input.TcInput_cloud;
import entities.cloud.CloudCluster;
import entities.cloud.CloudLink;
import entities.cloud.CloudProperty;
import entities.cloud.CloudRoute;
import entities.cloud.CloudRouter;
import entities.cloud.CloudZone;
import entities.cloud.CloudZoneRoute;
import entities.cloud.Network;

import mutation.MutableCloud.MutableCloud;


public class TC2Cloud_simGrid implements TestCaseTransformations {

	private final String ZONE_TAG = "zone";
	private final String ZONEROUTE_TAG = "zoneRoute";
	private final String ROUTER_TAG = "router";
	private final String ROUTE_TAG = "route";
	private final String LINK_TAG = "link";
	private final String LINK_CTN = "link_ctn";
	private final String PROP_TAG = "prop";	
	private final String CLUSTER_TAG = "cluster";
	
	ECloudSimulator eCloudSimulator;
	CloudCreator cloudCreator;
	MutableCloud cloudRet;
	
	public TC2Cloud_simGrid()
	{
		eCloudSimulator = ECloudSimulator.eSIMGRID;
		cloudCreator = new CloudCreator(eCloudSimulator);
		cloudRet = new MutableCloud(eCloudSimulator);
	}
	@Override
	public MutableCloud transformTestCase2Cloud(TestCaseInput tcInput) {
		// TODO Auto-generated method stub
		return transformSimGrid(tcInput);
	}
	@Override
	public TestCaseInput transformCloud2Testcase(ECloudSimulator ePlatformInfo, TestCaseInput tcInputNew,
			MutableCloud mutCloud) {
		// TODO Auto-generated method stub
		return null;
	}
	private MutableCloud transformSimGrid(TestCaseInput tcInput) {
		MutableCloud mutableCloud;
		TcInput_cloud tcInputCloud;
		String strCloudModel;
		Document xmlFile;
		
		mutableCloud = null;
		
		tcInputCloud = (TcInput_cloud)tcInput;
		if(tcInputCloud != null)
		{		
			//The main idea is to load the XML and extract the different zones and computing elements
			strCloudModel = tcInputCloud.getCloudModelPath();
			
			xmlFile = AuxFunctions.openXmlFile(strCloudModel);
			
			if(xmlFile != null)
			{
				mutableCloud = parseXmlCloud(xmlFile.getDocumentElement());		
				
			}
		}
		
		return mutableCloud;
	}
	public MutableCloud parseXmlCloud(Node node) {
		
		MutableCloud mutRet;
		String strNodeType;
		CloudZone cloudZone;
		CloudZoneRoute cloudZoneRoute;
		
		mutRet = null;
		if(node != null)
		{
			mutRet = new MutableCloud(eCloudSimulator);
	
		    NodeList nodeList = node.getChildNodes();
		    for (int i = 0; i < nodeList.getLength(); i++) {
		        Node currentNode = nodeList.item(i);
		        if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
		        	
		        	strNodeType = currentNode.getNodeName();
		        	if(strNodeType.indexOf(ZONE_TAG) != -1)
		        	{
		        		cloudZone = parseCloudZone(currentNode);
		        		mutRet.insertCloudZone(cloudZone);
		        	}
		        	/*else if(strNodeType.indexOf(ZONEROUTE_TAG) != -1)
		        	{
		        		cloudZoneRoute = parseCloudZoneRoute(currentNode);		
		        		mutRet.insertCloudZoneRoute(cloudZoneRoute);
		        	}*/
		        }
		    }
		    mutRet.toString();
		}
		return mutRet;
	}	
	private CloudZone parseCloudZone(Node currentNode) {
		CloudZone cloudRet, cloudZoneChild;
		CloudZoneRoute cloudZoneRouteChild;
		CloudRouter cloudRouter;
		CloudRoute cloudRoute;
		CloudCluster cloudCluster;
		Network cloudNetCon;
		String strZoneId, strRouting, strNodeType;
		NamedNodeMap nodeAttr;
		
		cloudRet = null;
		if(currentNode != null)
		{
			cloudRet = new CloudZone();
			
			//Get attributes
			nodeAttr = currentNode.getAttributes();
			
			if(nodeAttr != null)
			{
				if(nodeAttr.getNamedItem("id") != null)
				{
					strZoneId = nodeAttr.getNamedItem("id").getNodeValue();
				}
				else
				{
					strZoneId = "unknown";
				}
				
				if(nodeAttr.getNamedItem("routing") != null)
				{
					strRouting = nodeAttr.getNamedItem("routing").getNodeValue();
				}
				else
					strRouting = "unknown";
						
				cloudRet.setZoneId(strZoneId);
				cloudRet.setRoutingType(strRouting);
				
				//Process childs				
				NodeList nodeList = currentNode.getChildNodes();
			    for (int i = 0; i < nodeList.getLength(); i++) {
			        Node currentChild = nodeList.item(i);
			        if (currentChild.getNodeType() == Node.ELEMENT_NODE) {
			        	
			        	strNodeType = currentChild.getNodeName();
			        	if(strNodeType.equals(ZONE_TAG))
			        	{
			        		cloudZoneChild = parseCloudZone(currentChild);
			        		cloudRet.addZone(cloudZoneChild);
			        	}
			        	else if(strNodeType.equals(ZONEROUTE_TAG))
			        	{
			        		//Create a zoneroute object
			        		cloudZoneRouteChild = parseCloudZoneRoute(currentChild);
			        		
			        		if(cloudZoneRouteChild != null)
			        			cloudRet.addZoneRoute(cloudZoneRouteChild);			        		
			        	}
			        	else if(strNodeType.equals(ROUTER_TAG))
			        	{
			        		//Create a router object
			        		cloudRouter = parseCloudRouter(currentChild);
			        		
			        		if(cloudRouter != null)
			        		{
			        			cloudRouter.setParentZoneId(strZoneId);
			        			cloudRet.addRouter(cloudRouter);				        		
			        		}
			        	}	
			        	else if(strNodeType.equals(ROUTE_TAG))
			        	{
			        		//Create a route object
			        		cloudRoute = parseCloudRoute(currentChild);
			        		
			        		if(cloudRoute != null)
			        		{
			        			cloudRoute.setParentZoneId(strZoneId);
			        			cloudRet.addRoute(cloudRoute);				        		
			        		}
			        	}
			        	else if(strNodeType.equals(LINK_TAG))
			        	{
			        		//Create a route object
			        		cloudNetCon = parseNetConnLink(currentChild);
			        		
			        		if(cloudNetCon != null)
			        		{
			        			cloudNetCon.setParentZoneId(strZoneId);
			        			cloudRet.addNetworkConnection(cloudNetCon);					        		
			        		}
			        	}			
			        	else if(strNodeType.equals(CLUSTER_TAG))
			        	{
			        		//Create a route object
			        		cloudCluster = parseCloudCluster(strZoneId, currentChild);
			        		
			        		if(cloudCluster != null)
			        			cloudRet.addCluster(cloudCluster);			        		
			        	}
			        }
			    }				
			}
		}
		return cloudRet;
	}
	private CloudCluster parseCloudCluster(String strZoneId, Node currentChild) {
		String strId, strPrefix, strSuffix, strRadical, strBw, strSpeed;
		String strLat, strBb_bw, strBb_lat;
		NodeList nodeList;	
		NamedNodeMap nodeAttr;
		CloudCluster cloudCluster;
		long lLatency, lBandwidth, lLatencyBB, lBandwidthBB, lSpeed;
		LinkedList<CloudProperty> propList;
		
		propList = null;
		cloudCluster = null;
		nodeAttr = currentChild.getAttributes();
		if(nodeAttr != null)
		{
			strId = nodeAttr.getNamedItem("id").getNodeValue();
			strPrefix = nodeAttr.getNamedItem("prefix").getNodeValue();
			strSuffix = nodeAttr.getNamedItem("suffix").getNodeValue();
			strRadical = nodeAttr.getNamedItem("radical").getNodeValue();
			strSpeed = nodeAttr.getNamedItem("speed").getNodeValue();
			strBw = nodeAttr.getNamedItem("bw").getNodeValue();
			strLat = nodeAttr.getNamedItem("lat").getNodeValue();
			strBb_bw = nodeAttr.getNamedItem("bb_bw").getNodeValue();
			strBb_lat = nodeAttr.getNamedItem("bb_lat").getNodeValue();
						
			lSpeed = parseLong(strSpeed); 
			lLatency = parseLatency(strLat);
			lLatencyBB = parseLatency(strBb_lat);
			lBandwidth = parseLong(strBw);
			lBandwidthBB = parseLong(strBb_bw);

			//Extract properties
			nodeList = currentChild.getChildNodes();
			propList = extractProperties(nodeList, nodeAttr);
			
			cloudCluster = new CloudCluster(strZoneId, strId, strPrefix, strSuffix, strRadical, lSpeed, lLatency, lLatencyBB, lBandwidth, lBandwidthBB);

			for(CloudProperty cloudIndex: propList)
			{
				cloudCluster.addProperty(cloudIndex);
			}			
		}
			
		return cloudCluster;
	}
	private LinkedList<CloudProperty> extractProperties(NodeList nodeList, NamedNodeMap nodeAttr) {
		String strNodeType;
		String strPropId;
		String strPropValue;
		
		NamedNodeMap nodeAttrChild;
		LinkedList<CloudProperty> propList;
		
		propList = null;
		if(nodeList != null)
		{
			propList = new LinkedList<CloudProperty>();
			for (int i = 0; i < nodeList.getLength(); i++) 
			{
		        Node indexNode = nodeList.item(i);
		        if (indexNode.getNodeType() == Node.ELEMENT_NODE) 
		        {
		        	strNodeType = indexNode.getNodeName();

		        	if(strNodeType.equals(PROP_TAG))
		        	{
		    			//Get attributes
		    			nodeAttrChild = indexNode.getAttributes();
		    			
		    			if(nodeAttr != null)
		    			{
		    				strPropId = nodeAttrChild.getNamedItem("id").getNodeValue();
		    				strPropValue = nodeAttrChild.getNamedItem("value").getNodeValue();
		    				propList.add(new CloudProperty(strPropId, strPropValue));
		    			}
		        	}
		        }
			}
		}
		
		return propList;
	}
	private Network parseNetConnLink(Node currentChild) {
		String strId, strBandwidth, strLatency;
		NamedNodeMap nodeAttr;
		Network netConnRet;
		long lLatency, lBandwidth;
		
		netConnRet = null;
		nodeAttr = currentChild.getAttributes();
		if(nodeAttr != null)
		{
			strId = nodeAttr.getNamedItem("id").getNodeValue();
			strBandwidth = nodeAttr.getNamedItem("bandwidth").getNodeValue();
			strLatency = nodeAttr.getNamedItem("latency").getNodeValue();
			
			lLatency = parseLatency(strLatency);
			lBandwidth = parseLong(strBandwidth);
			
			//Aqui falta
			netConnRet = new Network(strId, lLatency, lBandwidth);
		}
			
		return netConnRet;
	}
	private long parseLatency(String strLatency) {
		long lLat;
		int nExpIndex, nExponential, nMult;
		Matcher m;
		Pattern p;
		Double dValue;
		String strAux;
		
		lLat = 0;
		nMult = 0;
		if(!strLatency.isEmpty())
		{
			nExpIndex = strLatency.indexOf("E");
			if(nExpIndex != -1)
			{				
				//m = Pattern.compile(".*E-{0,1}\\d*\\D\\Z").matcher(strLatency);
				strAux = strLatency.substring(0,nExpIndex);
				
				if(!strAux.isEmpty())
				{
					dValue = Double.parseDouble(strAux);
					
					strAux = strLatency.substring(nExpIndex+1); 
					
					p = Pattern.compile("\\p{Alpha}");
					m = p.matcher(strAux);
					if (m.find()) {
					    nExpIndex = m.start();
					    strAux = strAux.substring(0,nExpIndex);
					    
					    if(!strAux.isEmpty())
					    {
					    	nExponential = Integer.parseInt(strAux);
				    		//Transform to usecs
					    	if(m.group().equals("s"))
					    		nMult = (int) 1E6;
					    	else
					    		nMult=1;
					    	
					    	lLat = (long)(nMult*(Math.pow(10,nExponential)*dValue));
					    }
					}
					else
					{
						lLat = Long.parseLong(strLatency);
					}					
				}
			}
			else
			{
				p = Pattern.compile("\\p{Alpha}");
				m = p.matcher(strLatency);
				if (m.find()) {
				    System.out.println(m.group());
				    System.out.println("At: " + m.start());
				}
				else
				{
					lLat = Long.parseLong(strLatency);
				}
				
			}
		}
		
		return lLat;
	}
	private long parseLong(String strLine) {
		long lLongRet;
		int nExpIndex, nExponential;
		Matcher m;
		Pattern p;
		Double dValue;
		String strAux;
		
		lLongRet = 0;
		if(!strLine.isEmpty())
		{
			nExpIndex = strLine.indexOf("E");
			if(nExpIndex != -1)
			{				
				//m = Pattern.compile(".*E-{0,1}\\d*\\D\\Z").matcher(strLatency);
				strAux = strLine.substring(0,nExpIndex);
				
				if(!strAux.isEmpty())
				{
					dValue = Double.parseDouble(strAux);
					
					strAux = strLine.substring(nExpIndex+1); 
					
					p = Pattern.compile("\\p{Alpha}");
					m = p.matcher(strAux);
					if (m.find()) {
					    nExpIndex = m.start();
					    strAux = strAux.substring(0,nExpIndex);
					    
					    if(!strAux.isEmpty())
					    {
					    	nExponential = Integer.parseInt(strAux);
					    	lLongRet = (long)((Math.pow(10,nExponential)*dValue));
					    }
					    else
					    	lLongRet = 0;
					}
					else
					{
						lLongRet = (long) Double.parseDouble(strLine);
					}					
				}
			}
			else
			{
				p = Pattern.compile("\\p{Alpha}");
				m = p.matcher(strLine);
				if (m.find()) {
				    //System.out.println(m.group());
				    //System.out.println("At: " + m.start());
				    
				    strAux = strLine.substring(0,m.start());
				    if(strAux != null)
				    	lLongRet = (long) Double.parseDouble(strAux);				    
				}
				else
				{
					lLongRet = (long) Double.parseDouble(strLine);
				}
				
			}
		}
		
		return lLongRet;
	}	
	private CloudRoute parseCloudRoute(Node currentChild) {
		NamedNodeMap nodeAttr, nodeAttrChild;
		String strSource, strDst, strNodeType, strId;
		CloudRoute cloudRouteRet;
		NodeList nodeList;
		
		//Initialize
		cloudRouteRet = null;
		//Get attributes
		nodeAttr = currentChild.getAttributes();
		
		if(nodeAttr != null)
		{
			strSource = nodeAttr.getNamedItem("src").getNodeValue();
			strDst = nodeAttr.getNamedItem("dst").getNodeValue();
			cloudRouteRet = new CloudRoute(strSource, strDst);
			
			nodeList = currentChild.getChildNodes();
			
			if(nodeList != null)
			{
				for (int i = 0; i < nodeList.getLength(); i++) 
				{
			        Node indexNode = nodeList.item(i);
			        if (indexNode.getNodeType() == Node.ELEMENT_NODE) 
			        {
			        	strNodeType = indexNode.getNodeName();

			        	if(strNodeType.equals(LINK_CTN))
			        	{
			    			//Get attributes
			    			nodeAttrChild = indexNode.getAttributes();
			    			
			    			if(nodeAttr != null)
			    			{
			    				strId = nodeAttrChild.getNamedItem("id").getNodeValue();
			    				cloudRouteRet.addLink(new CloudLink(strId));
			    			}
			        	}
			        }
				}
			}
		}
			
		return cloudRouteRet;
	}
	private CloudRouter parseCloudRouter(Node currentChild) {
		NamedNodeMap nodeAttr;
		String strName;
		CloudRouter cloudRouterRet;
		
		//Initialize
		cloudRouterRet = null;
		//Get attributes
		nodeAttr = currentChild.getAttributes();
		
		if(nodeAttr != null)
		{
			strName = nodeAttr.getNamedItem("id").getNodeValue();
			cloudRouterRet = new CloudRouter(strName);
		}
			
		return cloudRouterRet;
	}
	private CloudZoneRoute parseCloudZoneRoute(Node currentChild) {
		NamedNodeMap nodeAttr, nodeAttrChild;
		String strSource, strDst, strNodeType, strId, strSourceGW, strDstGW;
		CloudZoneRoute cloudRouteRet;
		NodeList nodeList;
		
		//Initialize
		cloudRouteRet = null;
		//Get attributes
		nodeAttr = currentChild.getAttributes();
		
		if(nodeAttr != null)
		{
			strSource = nodeAttr.getNamedItem("src").getNodeValue();
			strDst = nodeAttr.getNamedItem("dst").getNodeValue();
			strSourceGW = nodeAttr.getNamedItem("gw_src").getNodeValue();
			strDstGW = nodeAttr.getNamedItem("gw_dst").getNodeValue();			
			cloudRouteRet = new CloudZoneRoute(strSource, strDst, strSourceGW, strDstGW);
			
			nodeList = currentChild.getChildNodes();
			
			if(nodeList != null)
			{
				for (int i = 0; i < nodeList.getLength(); i++) 
				{
			        Node indexNode = nodeList.item(i);
			        if (indexNode.getNodeType() == Node.ELEMENT_NODE) 
			        {
			        	strNodeType = indexNode.getNodeName();

			        	if(strNodeType.equals(LINK_CTN))
			        	{
			    			//Get attributes
			    			nodeAttrChild = indexNode.getAttributes();
			    			
			    			if(nodeAttr != null)
			    			{
			    				strId = nodeAttrChild.getNamedItem("id").getNodeValue();
			    				cloudRouteRet.addLink(new CloudLink(strId));
			    			}
			        	}
			        }
				}
			}
		}
			
		return cloudRouteRet;
	}
	
	private void saveDocToXmlFile(String strXmlOutputPath, Document xmlCloned) {
		// 
		try
		{
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMImplementation domImpl = xmlCloned.getImplementation(); //Ojo que aqui he cambiardo xmkFile por lo q hay
			DocumentType doctype = domImpl.createDocumentType("doctype",
				    "",
				    "http://simgrid.gforge.inria.fr/simgrid/simgrid.dtd");
			
			transformer.setOutputProperty(OutputKeys.ENCODING, "");
			//transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			
			DOMSource source = new DOMSource(xmlCloned);
			StreamResult result = new StreamResult(new File(strXmlOutputPath));
			transformer.transform(source, result);
			
		   } catch (TransformerException tfe) {
			tfe.printStackTrace();
		   }
	}	

}
