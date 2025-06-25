package auxiliars;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import auxiliar.AuxFunctions;
import auxiliar.Iteration;
import auxiliar.Iterations;
import dataParser.TestCase;
import dataParser.cloud.input.TcInput_cloud;
import dataParser.metadata.MetaTestCase;
import transformations.IModel;


public class EAFileHandler {
	private String INPUT_TAG = "TcInput";
	private String OUTPUT_TAG = "TcOutput";
	private String META_TAG = "metaInfo";
	
	
	String strBasePath;		
	String strInstanceSessionPath;
	
	public EAFileHandler(String strBasePath)
	{
		this.strBasePath = strBasePath;
	}
	
	public boolean createNewEAPathBase(String strFriendlyName)
	{
		boolean bRet;
		Date date;
		SimpleDateFormat dateFormat;
		File file;
		
		date = new Date();
		dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss") ;
		
		if(!strFriendlyName.isEmpty())
			strInstanceSessionPath = strBasePath+File.separatorChar+dateFormat.format(date)+"_"+strFriendlyName;
		else
			strInstanceSessionPath = strBasePath+File.separatorChar+dateFormat.format(date);
		
		file = new File(strInstanceSessionPath);
		
		bRet = file.mkdir();
		
		return bRet;
	}
	public boolean createNewIterationPath(int nIteration)
	{
		boolean bRet;
		File file;
		
		file = new File(strInstanceSessionPath+File.separatorChar+String.format("%04d", nIteration));
		
		if(!file.exists())
		{			
			bRet = file.mkdir();
			createIOFolders(nIteration);
		}
		else
			bRet = true;
		
		return bRet;
	}
	public void createIOFolders(int nIteration)
	{
		File fileInput, fileOutput, fileMetaTC;
		String strPathBase;
		
		strPathBase = strInstanceSessionPath+File.separatorChar+String.format("%04d", nIteration);
		fileInput = new File(strPathBase+File.separatorChar+INPUT_TAG);
		fileOutput = new File(strPathBase+File.separatorChar+OUTPUT_TAG);
		fileMetaTC = new File(strPathBase+File.separatorChar+META_TAG);
		if(!fileInput.exists())
			fileInput.mkdir();
		
		if(!fileOutput.exists())
			fileOutput.mkdir();		
		
		if(!fileMetaTC.exists())
			fileMetaTC.mkdir();
	}
	
	public MetaTestCase createNewIndividualFiles(int nIteration, int nTcId, TcInput_cloud tcInputNew) {

		File inputTcFile, outputTcFile, metaTcFile, modelPathFile;
		String strPath, strModelPath, strModelPathFile;
		MetaTestCase mTcNew;
		BufferedWriter metaWriter, tcInputWriter;
		IModel xmlModel;
		
		mTcNew = null;
		//Create the path and the subfolders
		if(createNewIterationPath(nIteration))
		{						
			//Create the path
			strPath = strInstanceSessionPath+File.separatorChar+String.format("%04d", nIteration);
			inputTcFile = new File(strPath+File.separatorChar+INPUT_TAG+File.separatorChar+String.format("input_%05d.tc", nTcId));
			outputTcFile = new File(strPath+File.separatorChar+OUTPUT_TAG+File.separatorChar+String.format("output_%05d.tc", nTcId));
			metaTcFile = new File(strPath+File.separatorChar+META_TAG+File.separatorChar+String.format("tc_%05d.mtc", nTcId));
			
			//Construct and Write the MetaTC
			mTcNew = new MetaTestCase(metaTcFile.getAbsolutePath());
			mTcNew.setTcId(nTcId);
			mTcNew.setTcInput(inputTcFile.getAbsolutePath());
			mTcNew.setTcOutput(outputTcFile.getAbsolutePath());
			mTcNew.setDescription("");
			mTcNew.setTestCase(new TestCase(nTcId, tcInputNew, null));
			
			xmlModel = tcInputNew.getModel();
			//Now, we must recreate the model path, and save it
			if(xmlModel != null)
			{
				strModelPath = strPath+File.separatorChar+INPUT_TAG+File.separatorChar+"cloudModels";
				modelPathFile = new File(strModelPath);
				if(!modelPathFile.exists())
					modelPathFile.mkdirs();
				
				strModelPathFile = strModelPath+File.separatorChar+String.format("cloud_%05d.xml", nTcId);
				tcInputNew.setCloudModelPath(strModelPathFile);
				AuxFunctions.saveCloudToXml(xmlModel, strModelPathFile);
			}
				
			try {
				metaWriter = new BufferedWriter(new FileWriter(metaTcFile.getAbsolutePath()));
				metaWriter.write(mTcNew.ToString());			     
				metaWriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			
			//Write the TcInput
			try {
				tcInputWriter = new BufferedWriter(new FileWriter(inputTcFile.getAbsolutePath()));
				tcInputWriter.write(tcInputNew.ToString());			     
				tcInputWriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}					
			
		}	
		
		return mTcNew;
	}

	public String getSessionPath() {		
		return strInstanceSessionPath;
	}
}
