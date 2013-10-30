package com.ebay.testlink;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.testng.ITestResult;
import org.testng.annotations.Test;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import br.eti.kinoshita.testlinkjavaapi.constants.ActionOnDuplicate;
import br.eti.kinoshita.testlinkjavaapi.constants.ExecutionStatus;
import br.eti.kinoshita.testlinkjavaapi.constants.ExecutionType;
import br.eti.kinoshita.testlinkjavaapi.constants.TestImportance;
import br.eti.kinoshita.testlinkjavaapi.model.Build;
import br.eti.kinoshita.testlinkjavaapi.model.TestCase;
import br.eti.kinoshita.testlinkjavaapi.model.TestPlan;
import br.eti.kinoshita.testlinkjavaapi.model.TestSuite;
import br.eti.kinoshita.testlinkjavaapi.util.TestLinkAPIException;


public class TestLinkReportWriter {

	private static Object buildLock = new Object();

	private static Object suiteLock = new Object();
	private TestLinkData data;

	public TestLinkReportWriter(TestLinkData data)
			throws TransformerConfigurationException {
		this.data = data;
	}

	private synchronized void assignTestCaseToPlan(TestCase testCase,
			TestPlan testPlan) {
		List<TestCase> testCases = TestLinkData.planAndCases.get(testPlan);
		if (testCases == null) {
			TestCase[] planCases = TestLinkData.api.getTestCasesForTestPlan(
					testPlan.getId(), null, null, null, null, null, null, null,
					null, null, null);
			testCases = castArrayToList(planCases);
			TestLinkData.planAndCases.put(testPlan, testCases);
		}
		for (int i = 0; i < testCases.size(); i++) {
			if (testCase.getId().equals(testCases.get(i).getId())) {
				return;
			}
		}
		TestLinkData.api.addTestCaseToTestPlan(
				TestLinkData.testProject.getId(), testPlan.getId(),
				testCase.getId(), getVersion(testCase), null, null, 0);
		testCases.add(testCase);
	}

	private TestSuite createNewTestSuite(String suiteName) {
		return TestLinkData.api.createTestSuite(
				TestLinkData.testProject.getId(), suiteName, "", null, null,
				null, ActionOnDuplicate.GENERATE_NEW);
	}

	private TestSuite createNewTestSuite(TestSuite fatherSuite, String suiteName) {
		return TestLinkData.api
				.createTestSuite(TestLinkData.testProject.getId(), suiteName,
						"", fatherSuite.getId(), null, null,
						ActionOnDuplicate.GENERATE_NEW);
	}

	private synchronized TestCase createOrGetTestcase(String testCaseName,
			TestSuite testSuite) {
		TestCase testCase = null;
		List<TestCase> testCases = TestLinkData.suiteAndCases.get(testSuite);
		if (testCases == null) {
			TestCase[] suiteCases = TestLinkData.api.getTestCasesForTestSuite(
					testSuite.getId(), false, null);
			testCases = castArrayToList(suiteCases);
			TestLinkData.suiteAndCases.put(testSuite, testCases);
		}
		for (int i = 0; i < testCases.size(); i++) {
			if (testCaseName.equals(testCases.get(i).getName())) {
				return testCases.get(i);
			}
		}
		testCase = TestLinkData.api.createTestCase(testCaseName,
				testSuite.getId(), TestLinkData.testProject.getId(),
				data.username,
				"Created by MAUI automatically in " + new Date(), null, "",
				TestImportance.MEDIUM, ExecutionType.AUTOMATED, null, null,
				true, ActionOnDuplicate.BLOCK);
		testCases.add(testCase);
		return testCase;
	}

	private List<TestCase> castArrayToList(TestCase[] suiteCases) {
		List<TestCase> list = new ArrayList<TestCase>();

		if (suiteCases == null)
			return list;

		for (int i = 0; i < suiteCases.length; i++)
			list.add(suiteCases[i]);

		return list;
	}

	private Build getBuild(TestPlan testPlan, ITestResult result) {
		if (TestLinkData.planAndBuild.get(testPlan) == null) {
			synchronized (buildLock) {
				if (TestLinkData.planAndBuild.get(testPlan) != null)
					return TestLinkData.planAndBuild.get(testPlan);
				Build build = null;
				Build[] builds = TestLinkData.api.getBuildsForTestPlan(testPlan
						.getId());
				if (builds != null)
					for (int i = 0; i < builds.length; i++)
						if (builds[i].getName().equals(TestLinkData.buildName))
							build = builds[i];

				build = TestLinkData.api.createBuild(testPlan.getId(),
						TestLinkData.buildName, "");
				TestLinkData.planAndBuild.put(testPlan, build);
				return build;
			}
		}

		return TestLinkData.planAndBuild.get(testPlan);
	}

	private TestSuite getFistLevelSuite(String suiteName) {
		try {
			TestSuite[] testSuites = TestLinkData.api
					.getFirstLevelTestSuitesForTestProject(TestLinkData.testProject
							.getId());

			for (int i = 0; i < testSuites.length; i++)
				if (testSuites[i].getName().equals(suiteName))
					return testSuites[i];

		} catch (Exception e) {
			return createNewTestSuite(suiteName);
		}

		return createNewTestSuite(suiteName);
	}

	private List<String> getPackageDirs(String packageString) {
		List<String> packageDirs = new ArrayList<String>();

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < packageString.length(); i++) {
			if (packageString.charAt(i) != '.') {
				sb.append(packageString.charAt(i));
			} else {
				packageDirs.add(sb.toString());
				sb = new StringBuilder();
			}
		}
		packageDirs.add(sb.toString());

		return packageDirs;
	}

	private String getPlanName(ITestResult result) {
		XmlTest xmlTest = result.getTestClass().getXmlTest();
		XmlSuite xmlSuite = xmlTest.getSuite();

		if (xmlTest.getParameter("TestLinkPlanName") != null)
			return xmlTest.getParameter("TestLinkPlanName");
		else if (System.getenv("TestLinkPlanName") != null)
			return System.getenv("TestLinkPlanName");
		else if (xmlSuite.getParameter("TestLinkPlanName") != null)
			return xmlSuite.getParameter("TestLinkPlanName");

		String suiteName = xmlSuite.getName();
		if (suiteName == null || suiteName.trim().equals(""))
			suiteName = "maui";

		String testName = xmlTest.getName();
		if (testName == null || testName.trim().equals(""))
			testName = "mauitest";

		return suiteName + "-" + testName;
	}

	private TestSuite getSuite(String suiteName) {
		if (TestLinkData.testSuiteMap.get(suiteName) != null)
			return TestLinkData.testSuiteMap.get(suiteName);

		TestSuite fatherSuite = null;
		List<String> packageDirs = getPackageDirs(suiteName);
		synchronized (suiteLock) {
			if (TestLinkData.testSuiteMap.get(suiteName) != null)
				return TestLinkData.testSuiteMap.get(suiteName);

			for (int i = 0; i < packageDirs.size(); i++)
				if (i == 0)
					fatherSuite = getFistLevelSuite(packageDirs.get(i));
				else
					fatherSuite = getSuite(fatherSuite, packageDirs.get(i));

			TestLinkData.testSuiteMap.put(suiteName, fatherSuite);
		}
		return fatherSuite;
	}

	private TestSuite getSuite(TestSuite fatherSuite, String suiteName) {
		TestSuite[] testSuites = TestLinkData.api
				.getTestSuitesForTestSuite(fatherSuite.getId());

		for (int i = 0; i < testSuites.length; i++)
			if (testSuites[i].getName().equals(suiteName))
				return testSuites[i];

		return createNewTestSuite(fatherSuite, suiteName);
	}

	private String getTestCaseName(ITestResult result) {
		Method m = result.getMethod().getConstructorOrMethod().getMethod();
		if (m.isAnnotationPresent(Test.class)
				&& !m.getAnnotation(Test.class).dataProvider().equals("")) {
			Object[] objects = result.getParameters();
			for (int i = 0; i < objects.length; i++) {
				if (objects[i].getClass().getName().endsWith("TestObject")) {
					String testObject = objects[i].toString();
					String testTitle = testObject.split("TestTitle=")[1]
							.split("\\|")[0];
					return testTitle;
				}
			}
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] instanceof String
						&& ((String) objects[i]).contains("|TestCaseTitle=")) {
					String str = (String) objects[i];
					int index = str.indexOf("|TestCaseTitle=") + 15;
					return str.substring(index, str.length() - 1);
				}
			}
			System.out
					.println("Non standard Dataprovider -- without TestObject, ignore");
			return null;
		}
		
		return m.getName();
	}

	private TestCase getTestCaseFromExternalId(ITestResult result) {
		Method m = result.getMethod().getConstructorOrMethod().getMethod();
		String testCaseExternalId = "";
		if (m.getAnnotation(TestLinkId.class) != null) {
			TestCase testCase = TestLinkData.api.getTestCaseByExternalId(m
					.getAnnotation(TestLinkId.class).id().toString(), 1);
			int version = getVersion(testCase);
			return TestLinkData.api.getTestCaseByExternalId(
					m.getAnnotation(TestLinkId.class).id(), version);
		} else if (m.isAnnotationPresent(Test.class)
				&& !m.getAnnotation(Test.class).dataProvider().equals("")) {
			Object[] objects = result.getParameters();
			for (int i = 0; i < objects.length; i++) {
				if (objects[i].getClass().getName().endsWith("TestObject")) {
					String testObject = objects[i].toString();
					String testTitle = testObject.split("TestCaseId=")[1]
							.split("\\|")[0];
					return TestLinkData.api.getTestCaseByExternalId(testTitle,
							null);
				}
			}
			System.out
					.println("Non standard Dataprovider -- without TestObject, ignore");
			return null;
		}

		return null;
	}

	private TestPlan getTestPlan(ITestResult result) {
		String planName = getPlanName(result);
		return getTestPlan(planName);
	}

	private synchronized TestPlan getTestPlan(String planName) {
		TestPlan testPlan = null;

		if ((testPlan = TestLinkData.testPlanMap.get(planName)) != null)
			return testPlan;

		TestPlan[] testPlans = TestLinkData.api
				.getProjectTestPlans(TestLinkData.testProject.getId());

		for (int i = 0; i < testPlans.length; i++)
			if (planName.equals(testPlans[i].getName())) {
				testPlan = testPlans[i];
				TestLinkData.testPlanMap.put(planName, testPlan);
				return testPlan;
			}

		testPlan = TestLinkData.api.createTestPlan(planName,
				TestLinkData.testProject.getName(), "Created by maui in "
						+ new Date(), true, true);
		TestLinkData.testPlanMap.put(planName, testPlan);

		return testPlan;
	}

	private String getTestSuiteName(ITestResult result) {
		String className = result.getTestClass().getName();
		int position = className.lastIndexOf('.');
		return className.substring(0, position);
	}

	private Integer getVersion(TestCase testCase) {
		try {
			Integer version = TestLinkData.api.getTestCase(testCase.getId(),
					null, null).getVersion();
			return version == null ? 1 : version;
		} catch (TestLinkAPIException e) {
			System.out.println("Test Case Id(getVersion)£º" + testCase.getId());
			return 1;
		}
	}

	private TestCase handleTestCaseInSuite(ITestResult result) {
		TestCase testCase = null;
		if (result.getAttribute("tlIdSet").toString().equalsIgnoreCase("true")) {
			testCase = getTestCaseFromExternalId(result);
		} else {
			String testCaseName = getTestCaseName(result);
			if (testCaseName == null)
				return null;
			testCase = createOrGetTestcase(testCaseName,
					getSuite(getTestSuiteName(result)));

		}
		return testCase;

	}

	private void setTestCaseExecutionResult(TestCase testCase,
			ExecutionStatus status, TestPlan testPlan, Build build,
			ITestResult result, String url) {
		TestLinkData.api.setTestCaseExecutionResult(testCase.getId(),
				testCase.getExecutionOrder(), testPlan.getId(), status,
				build.getId(), "maui", getNote(result, url), null, null, null,
				null, null, null);
	}

	private String getNote(ITestResult result, String url) {
		StringBuilder sb = new StringBuilder();
		if (url != null) { // new logic
			sb.append("URL:" + url + "\n");
		}
		sb.append("test case run time:"
				+ (result.getEndMillis() - result.getStartMillis()) + "ms\n");
		if (result.getThrowable() != null) {
			if (result.getThrowable().getMessage() != null)
				sb.append(result.getThrowable().getMessage() + "\n");
			StackTraceElement[] elements = result.getThrowable()
					.getStackTrace();
			for (int i = 0; i < elements.length; i++)
				sb.append(elements[i].toString() + "\n");
		}

		return sb.toString();
	}

	private void writeResult(int id, TestCase testCase, TestPlan testPlan,
			Build build, ITestResult result, String url) {
		switch (id) {
		case TestLinkData.SUCCESS_RESULT:
			 setTestCaseExecutionResult(testCase, ExecutionStatus.PASSED,
			 testPlan, build, result, url);
			writeResultToFile(testCase, ExecutionStatus.PASSED, testPlan,
					build, result, url);
			System.out.println("success...");
			break;
		case TestLinkData.FAILURE_RESULT:
			 setTestCaseExecutionResult(testCase, ExecutionStatus.FAILED,
			 testPlan, build, result, url);
			writeResultToFile(testCase, ExecutionStatus.FAILED, testPlan,
					build, result, url);
			System.out.println("failure...");
			break;
		case TestLinkData.BLOCK_RESULT:
			 setTestCaseExecutionResult(testCase, ExecutionStatus.BLOCKED,
			 testPlan, build, result, url);
			writeResultToFile(testCase, ExecutionStatus.BLOCKED, testPlan,
					build, result, url);
			System.out.println("blocked...");
			break;
		}
	}

	synchronized public void writeResultToFile(TestCase testCase,
			ExecutionStatus status, TestPlan testPlan, Build build,
			ITestResult result, String url) {

		String testCaseId = testCase.getFullExternalId();
		String strTester = result.getAttribute("user").toString();
		String testresult = status.toString();
		String notes = getNote(result, url);

		String xmlFileName = "testlink_results.xml";

		try {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory
					.newDocumentBuilder();
			Document document = documentBuilder.parse(xmlFileName);

			Element testcase = document.createElement("testcase");
			testcase.setAttribute("external_id", testCaseId);

			Element tester = document.createElement("tester");
			tester.setTextContent(strTester);
			Element result_element = document.createElement("result");
			result_element.setTextContent(testresult);
			Element notes_element = document.createElement("notes");
			CDATASection data = document.createCDATASection(notes);
			notes_element.appendChild(data);
			testcase.appendChild(tester);
			testcase.appendChild(result_element);
			testcase.appendChild(notes_element);

			// Root Element
			Element rootElement = document.getDocumentElement();
			rootElement.appendChild(testcase);

			DOMSource source = new DOMSource(document);
			StreamResult output = new StreamResult(xmlFileName);
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty("indent", "yes");
			transformer.transform(source, output);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// public void writeResult(ITestResult result, int id) {
	// TestCase testCase = handleTestCaseInSuite(result);
	// if (testCase == null) {
	// return;
	// }
	// TestPlan testPlan = getTestPlan(result);
	// assignTestCaseToPlan(testCase, testPlan);
	// Build build = getBuild(testPlan, result);
	// writeResult(id, testCase, testPlan, build, result);
	// }

	public void writeResult(ITestResult result, int id, String url) {
		TestCase testCase = handleTestCaseInSuite(result);
		if (testCase == null) {
			return;
		}
		TestPlan testPlan = getTestPlan(result);
		assignTestCaseToPlan(testCase, testPlan);
		Build build = getBuild(testPlan, result);
		writeResult(id, testCase, testPlan, build, result, url);
	}
}