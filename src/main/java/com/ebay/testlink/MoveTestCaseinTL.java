/**
 * 
 */
package com.ebay.testlink;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import br.eti.kinoshita.testlinkjavaapi.TestLinkAPI;
import br.eti.kinoshita.testlinkjavaapi.constants.ActionOnDuplicate;
import br.eti.kinoshita.testlinkjavaapi.constants.TestCaseDetails;
import br.eti.kinoshita.testlinkjavaapi.model.Build;
import br.eti.kinoshita.testlinkjavaapi.model.Execution;
import br.eti.kinoshita.testlinkjavaapi.model.TestCase;
import br.eti.kinoshita.testlinkjavaapi.model.TestPlan;
import br.eti.kinoshita.testlinkjavaapi.model.TestProject;
import br.eti.kinoshita.testlinkjavaapi.model.TestSuite;
import br.eti.kinoshita.testlinkjavaapi.util.TestLinkAPIException;

/**
 * @author apannasa
 * 
 */
public class MoveTestCaseinTL {
	public PrintWriter testCaseMapping;
	public PrintWriter testCaseAutomationMapping;
	static TestLinkAPI api = null;
	String url = "http://localhost:90/TestLink/lib/api/xmlrpc.php";
	String devKey = "77065a3488d7f025ef8069d84f9147b2";
	TestCaseDetails testCaseDetails = TestCaseDetails.FULL;
	ActionOnDuplicate actionOnDuplicate = ActionOnDuplicate.BLOCK;
	String fileName = "testCaseMapping.properties";
	String autofileName = "automationMapping.properties";

	public MoveTestCaseinTL() throws FileNotFoundException {
		//testCaseMapping = new PrintWriter(new File(fileName));
		//testCaseAutomationMapping = new PrintWriter(new File(autofileName));
	}

	@Test
	public void moveTestCases() throws FileNotFoundException, IOException {

		TestProject baseProject = null;

		URL testlinkURL = null;

		try {
			testlinkURL = new URL(url);
		} catch (MalformedURLException mue) {
			mue.printStackTrace(System.err);
			Assert.fail(mue.getMessage());
		}

		try {
			api = new TestLinkAPI(testlinkURL, devKey);
		} catch (TestLinkAPIException te) {
			te.printStackTrace(System.err);
			Assert.fail(te.getMessage());
		}

		TestProject[] allProjects = api.getProjects();
		for (int i = 0; i < allProjects.length; i++) {
			if (allProjects[i].getName().equalsIgnoreCase("BuyerExp")) {
				baseProject = allProjects[i];
			}
		}
		for (int i = 0; i < allProjects.length; i++) {
			if (!allProjects[i].getName().equalsIgnoreCase("BuyerExp")) {
				moveTestCases(baseProject, allProjects[i]);
				moveTestPlans(baseProject, allProjects[i]);
			}
		}

	}

	public void moveTestPlans(TestProject destProject, TestProject sourceProject)
			throws FileNotFoundException, IOException {
		System.out.println("Moving Testplans from " + sourceProject.getName()
				+ " to " + destProject.getName());

		// Get All Test Plans
		TestPlan[] testPlans = api.getProjectTestPlans(sourceProject.getId());

		for (int i = 0; i < testPlans.length; i++) {
			if (testPlans[i].isActive()) {
				System.out.println("	Moving Testplan :"
						+ testPlans[i].getName());
				// Create Test Plans
				TestPlan createTestPlan = api.createTestPlan(
						testPlans[i].getName(), destProject.getName(),
						testPlans[i].getNotes(), testPlans[i].isActive(),
						testPlans[i].isPublic());

				// Get All Builds for a testPlan
				Build[] builds = api.getBuildsForTestPlan(testPlans[i].getId());
				for (int j = 0; j < builds.length; j++) {
					System.out.println("		Creating Build :"
							+ builds[j].getName());
					// Create Builds
					Build createBuild = api.createBuild(createTestPlan.getId(),
							builds[j].getName(), builds[j].getNotes());
					// Add Test Cases //
					TestCase[] testCases = api.getTestCasesForTestPlan(
							testPlans[i].getId(), null, builds[j].getId(), null, null, null,
							null, null, null, null, TestCaseDetails.FULL);
//					for (int testCaseCount = 0; testCaseCount < testCases.length; testCaseCount++) {
//						System.out.println("testCaseName: "+testCases[testCaseCount].getId());
//					}
					Properties props = new Properties();
					props.load(new FileInputStream(fileName));
					for (int k = 0; k < testCases.length; k++) {
						System.out.println("			Adding TestCase to TestPlan :"
								+ api.getTestCase(testCases[k].getId(), null,
										null).getName());
						int newTestCaseId = Integer.parseInt(props.getProperty(
								testCases[k].getId().toString()).toString());
						api.addTestCaseToTestPlan(destProject.getId(),
								createTestPlan.getId(), newTestCaseId,
								testCases[k].getVersion(), null,
								testCases[k].getExecutionOrder(), null);
						Execution oldResults = api.getLastExecutionResult(
								testPlans[i].getId(), testCases[k].getId(),
								Integer.parseInt(testCases[k]
										.getFullExternalId()));
						TestCase newTestCase = api.getTestCase(newTestCaseId,
								null, null);
						if (oldResults != null) {
							System.out.println("				Adding TestCase Results :"
									//+ testCases[k].getName() + "Results :"
									+ oldResults.getStatus());
							api.setTestCaseExecutionResult(newTestCase.getId(),
									Integer.parseInt(testCases[k]
											.getFullExternalId()),
									createTestPlan.getId(), oldResults
											.getStatus(), createBuild.getId(),
									createBuild.getName(), oldResults
											.getNotes(), null, null, null, "",
									null, null);
						}
					}
				}

			}
		}
	}

	public void moveTestCases(TestProject destProject, TestProject sourceProject) {
		System.out.println("Moving TestCases from " + sourceProject.getName()
				+ " to " + destProject.getName());

		// Create the Project as Suite
		TestSuite createTestSuite_Project = api.createTestSuite(
				destProject.getId(), sourceProject.getName(), "", null, null,
				null, ActionOnDuplicate.BLOCK);

		// Retrieve the TestSuites
		TestSuite[] testSuites = api
				.getFirstLevelTestSuitesForTestProject(sourceProject.getId());

		for (int firstLevel = 0; firstLevel < testSuites.length; firstLevel++) {
			System.out.println("	Moving " + testSuites[firstLevel].getName());

			// Create the First level Test Suite
			TestSuite createFirstLevelSubSuite = api.createTestSuite(
					destProject.getId(), testSuites[firstLevel].getName(),
					testSuites[firstLevel].getDetails(),
					createTestSuite_Project.getId(), null, true,
					actionOnDuplicate);

			// Check to see if there are any subSuites
			TestSuite firstLevelSubTestSuites[] = api
					.getTestSuitesForTestSuite(testSuites[firstLevel].getId());
			if (firstLevelSubTestSuites.length > 0) {
				for (int secondLevel = 0; secondLevel < firstLevelSubTestSuites.length; secondLevel++) {
					System.out.println("		Moving "
							+ firstLevelSubTestSuites[secondLevel].getName());
					// Create the Second level Test Suite
					TestSuite createSecondLevelSubSuite = api.createTestSuite(
							destProject.getId(),
							firstLevelSubTestSuites[secondLevel].getName(),
							firstLevelSubTestSuites[secondLevel].getDetails(),
							createFirstLevelSubSuite.getId(), null, true,
							actionOnDuplicate);

					// Check to see if there are any subSuites
					TestSuite secondLevelSubTestSuites[] = api
							.getTestSuitesForTestSuite(firstLevelSubTestSuites[secondLevel]
									.getId());
					if (secondLevelSubTestSuites.length > 0) {
						for (int thirdLevel = 0; thirdLevel < secondLevelSubTestSuites.length; thirdLevel++) {
							System.out.println("			Moving "
									+ secondLevelSubTestSuites[thirdLevel]
											.getName());
							// Create Third level Test Suite
							TestSuite createThirdLevelSubSuite = api
									.createTestSuite(
											destProject.getId(),
											secondLevelSubTestSuites[thirdLevel]
													.getName(),
											secondLevelSubTestSuites[thirdLevel]
													.getDetails(),
											createSecondLevelSubSuite.getId(),
											null, true, actionOnDuplicate);

							// Check to see if there are any subSuites
							TestSuite thirdLevelSubTestSuites[] = api
									.getTestSuitesForTestSuite(secondLevelSubTestSuites[thirdLevel]
											.getId());

							if (thirdLevelSubTestSuites.length > 0) {
								for (int fourthLevel = 0; fourthLevel < thirdLevelSubTestSuites.length; fourthLevel++) {
									System.out
											.println("				Moving "
													+ thirdLevelSubTestSuites[fourthLevel]
															.getName());
									// Create Fourth level Test Suite
									TestSuite createFourthLevelSubSuite = api
											.createTestSuite(
													destProject.getId(),
													thirdLevelSubTestSuites[fourthLevel]
															.getName(),
													thirdLevelSubTestSuites[fourthLevel]
															.getDetails(),
													createThirdLevelSubSuite
															.getId(), null,
													true, actionOnDuplicate);

									// No More checking :(
									// Move Test Cases
									createTestCases(
											destProject,
											thirdLevelSubTestSuites[fourthLevel],
											createFourthLevelSubSuite.getId());

								}
							} else {
								createTestCases(destProject,
										secondLevelSubTestSuites[thirdLevel],
										createThirdLevelSubSuite.getId());
							}
						}
					} else {
						createTestCases(destProject,
								firstLevelSubTestSuites[secondLevel],
								createSecondLevelSubSuite.getId());
					}
				}
			} else {
				createTestCases(destProject, testSuites[firstLevel],
						createFirstLevelSubSuite.getId());
			}

		}
	}

	public void createTestCases(TestProject destProject,
			TestSuite currentTestSuite, int parentTestSuiteId) {
		TestCase[] testCases = api.getTestCasesForTestSuite(
				currentTestSuite.getId(), true, testCaseDetails);
		for (int k = 0; k < testCases.length; k++) {
			if (validateTestCase(testCases[k])) {
				testCases[k] = setupTestCase(testCases[k]);
				System.out.println("					Moving TestCase :"
						+ testCases[k].getName().replaceAll("\n", ""));
				TestCase createTestCase = api.createTestCase(
						testCases[k].getName(), parentTestSuiteId,
						destProject.getId(), testCases[k].getAuthorLogin(),
						testCases[k].getSummary(), testCases[k].getSteps(),
						testCases[k].getPreconditions(),
						testCases[k].getTestImportance(),
						testCases[k].getExecutionType(),
						testCases[k].getExecutionOrder(), null, false,
						actionOnDuplicate);
				testCaseMapping.append(testCases[k].getId() + "="
						+ createTestCase.getId() + "\n");
				testCaseAutomationMapping.append(testCases[k]
						.getFullExternalId()
						+ "="
						+ api.getTestCase(createTestCase.getId(), null, null)
								.getFullExternalId() + "\n");
				testCaseMapping.flush();
				testCaseAutomationMapping.flush();
			}
		}
	}

	/**
	 * @param testCase
	 */
	private boolean validateTestCase(TestCase testCase) {
		if (testCase.getName().isEmpty()) {
			System.err.println("					Invalid Test Case:" + testCase.getName());
			return false;
		}
		return true;

	}

	public TestCase setupTestCase(TestCase testCase) {
		if (testCase.getAuthorLogin() == null) {
			testCase.setAuthorLogin("admin");
		}
		if (testCase.getSummary() == null || testCase.getSummary().isEmpty()) {
			testCase.setSummary("Summary");
		}
		// if(testCase.getAuthorLogin()==null){
		// testCase.setAuthorLogin("admin");
		// }
		// if(testCase.getAuthorLogin()==null){
		// testCase.setAuthorLogin("admin");
		// }
		// if(testCase.getAuthorLogin()==null){
		// testCase.setAuthorLogin("admin");
		// }
		// if(testCase.getAuthorLogin()==null){
		// testCase.setAuthorLogin("admin");
		// }
		// if(testCase.getAuthorLogin()==null){
		// testCase.setAuthorLogin("admin");
		// }
		// if(testCase.getAuthorLogin()==null){
		// testCase.setAuthorLogin("admin");
		// }
		return testCase;
	}

}
