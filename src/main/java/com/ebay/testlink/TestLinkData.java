package com.ebay.testlink;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.testng.ISuite;

import br.eti.kinoshita.testlinkjavaapi.TestLinkAPI;
import br.eti.kinoshita.testlinkjavaapi.model.Build;
import br.eti.kinoshita.testlinkjavaapi.model.TestCase;
import br.eti.kinoshita.testlinkjavaapi.model.TestPlan;
import br.eti.kinoshita.testlinkjavaapi.model.TestProject;
import br.eti.kinoshita.testlinkjavaapi.model.TestSuite;

public class TestLinkData {
	String url;
	String key;
	String projectName;
	String prefix;
	String username;
	static TestLinkAPI api;
	static TestProject testProject;
	static String buildName;

	public static final int SUCCESS_RESULT = 1;
	public static final int FAILURE_RESULT = 2;
	public static final int BLOCK_RESULT = 3;

	static HashMap<String, TestSuite> testSuiteMap = new HashMap<String, TestSuite>();
	static HashMap<String, TestPlan> testPlanMap = new HashMap<String, TestPlan>();
	static HashMap<TestPlan, Build> planAndBuild = new HashMap<TestPlan, Build>();
	
	static HashMap<TestSuite, List<TestCase>> suiteAndCases = new HashMap<TestSuite, List<TestCase>>();
	static HashMap<TestPlan, List<TestCase>> planAndCases = new HashMap<TestPlan, List<TestCase>>();
	
	public TestLinkData(ISuite suite) {
		init(suite);
	}

	private TestProject createNewTestProject(TestLinkAPI api,
			String projectName, String prefix) {
		return api.createTestProject(projectName, prefix,
				"Created by MAUI automatically in " + new Date(), true, true,
				true, true, true, true);
	}

	private TestProject getTestProject() {
		TestProject[] testProjects = api.getProjects();

		for (int i = 0; i < testProjects.length; i++) {
			if (testProjects[i].getName().equals(projectName)) {
				return testProjects[i];
			}
		}

		return createNewTestProject(api, projectName, prefix);
	}

	private void init(ISuite suite) {
		initParameters(suite);
		testParameters();
		api = initAPIInstance();
		testProject = getTestProject();
	}

	private TestLinkAPI initAPIInstance() {
		try {
			return new TestLinkAPI(new URL(url), key);
		} catch (MalformedURLException mue) {
			throw new RuntimeException(
					"TestLinkURL format is not recorgnized, please correct it!");
		}
	}

	private void initParameters(ISuite suite) {
		url = System.getenv("TestLinkURL") == null ? suite.getXmlSuite()
				.getParameter("TestLinkURL") : System.getenv("TestLinkURL");
		key = System.getenv("TestLinkDevKey") == null ? suite.getXmlSuite()
				.getParameter("TestLinkDevKey") : System
				.getenv("TestLinkDevKey");
		projectName = System.getenv("TestLinkProjectName") == null ? suite
				.getXmlSuite().getParameter("TestLinkProjectName") : System
				.getenv("TestLinkProjectName");
		prefix = System.getenv("TestLinkProjectPrefix") == null ? suite
				.getXmlSuite().getParameter("TestLinkProjectPrefix") : System
				.getenv("TestLinkProjectPrefix");
		buildName = System.getenv("TestLinkBuildName") == null ? suite
				.getXmlSuite().getParameter("TestLinkBuildName") : System
				.getenv("TestLinkBuildName");
		username = System.getenv("TestLinkUser") == null ? suite.getXmlSuite()
				.getParameter("TestLinkUser") : System.getenv("TestLinkUser");
	}
	
	private void testParameters() {
		if (url == null || url.equals("") || key == null || key.equals("")
				|| projectName == null || projectName.equals("")) {
			throw new RuntimeException(
					"Must provide TestLinkURL,TestLinkDevKey,TestLinkProjectName in System Property or TestNG parameter!");
		}

		if (buildName == null || buildName.trim().equals("")) {
			System.out
					.println("TestLinkBuildName is not specified, use default build name: maui");
			buildName = "maui";
		}

		if (prefix == null || prefix.trim().equals("")) {
			System.out
					.println("TestLinkProjectPrefix is not specified, use default project name as project prefix");
			prefix = projectName;
		}

		if (username == null || username.trim().equals("")) {
			System.out
					.println("TestLinkUser is not specified, use admin as user name");
			username = "admin";
		}
	}
}
