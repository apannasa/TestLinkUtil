package com.ebay.testlink;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class TestLinkListener extends TestListenerAdapter implements
		ISuiteListener {
	private TestLinkReportWriter writer;
	private long start;
	private static boolean setTLProps = false;
	private static boolean isTLIdSet;
	private static String user;
	

	public void onFinish(ISuite suite) {
		System.out.println("TestLinkListener: Total execution time "
				+ (System.currentTimeMillis() - start) + "ms");
	}

	public void onStart(ISuite suite) {
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream("testlinkconfig.properties"));
		} catch (FileNotFoundException e) {
			System.err.println("Unable to Find testlinkconfig.properties file...... Data will not be updated to testlink");
			e.printStackTrace();
			throw new RuntimeException("Unable to Find testlinkconfig.properties file. Remove TestLinkListner from executing XML if you dont want to update TestLink");
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (prop.getProperty("updateTestLink").equalsIgnoreCase("true") && !setTLProps) {
							Map<String, String> parameters =suite.getXmlSuite().getParameters();
				parameters.put("TestLinkProjectName",
						prop.getProperty("TestLinkProjectName").trim());
				parameters.put("TestLinkURL", prop.getProperty("TestLinkURL").trim());
				parameters.put("TestLinkDevKey",
						prop.getProperty("TestLinkDevKey").trim());
				parameters.put("TestLinkProjectPrefix",
						prop.getProperty("TestLinkProjectPrefix").trim());
				parameters.put("TestLinkPlanName",
						prop.getProperty("TestLinkPlanName").trim());
				parameters.put("TestLinkBuildName",
						prop.getProperty("TestLinkBuildName").trim());
				parameters
						.put("TestLinkUser", prop.getProperty("TestLinkUser").trim());
				user= prop.getProperty("TestLinkUser");
				
				suite.getXmlSuite().setParameters(parameters);
				createResultsXmlFile();
				if(prop.getProperty("usingTLID").equalsIgnoreCase("true")){
					isTLIdSet=true;
					System.out.println("Updating testlink with data using Teslink Ids");
				}
				
				try {
					start = System.currentTimeMillis();
					if (writer == null) {
						System.out.println("initialize testlinkdata");
						TestLinkData data = new TestLinkData(suite);
						writer = new TestLinkReportWriter(data);
					}
				} catch (Exception ex) {
					System.out
							.println("TestLinkListener: met exception in onStart, skipped--"
									+ ex.getMessage());
					writer = null;
					// throw new RuntimeException(ex);
				}finally{
					setTLProps = true;
					
				}
			}
		
	}

	@Override
	public void onTestFailure(ITestResult result) {
		if (writer != null){
			addResultParams(result);
			writer.writeResult(result, TestLinkData.FAILURE_RESULT, null);
		}
	}

	@Override
	public void onTestSkipped(ITestResult result) {
		if (writer != null){
			addResultParams(result);
			writer.writeResult(result, TestLinkData.BLOCK_RESULT, null);
		}
	}

	@Override
	public void onTestSuccess(ITestResult result) {
		if (writer != null){
			addResultParams(result);
			writer.writeResult(result, TestLinkData.SUCCESS_RESULT, null);
		}
	}
	
	private void addResultParams(ITestResult result){
		result.setAttribute("tlIdSet", isTLIdSet);
		result.setAttribute("user", user);
		
	}
	
	public void createResultsXmlFile(){
		String xmlFileName = "testlink_results.xml";
		System.out
				.println("Creating Results XML for Uploading to TestLInk");
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
 
			// root elements
			Document doc = docBuilder.newDocument();
			doc.setXmlVersion("1.0");
			doc.setXmlStandalone(false);
			Element rootElement = doc.createElement("results");
			doc.appendChild(rootElement);
			
			DOMSource source = new DOMSource(doc);

			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			StreamResult output = new StreamResult(xmlFileName);
			transformer.transform(source, output);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
 
	}
}
