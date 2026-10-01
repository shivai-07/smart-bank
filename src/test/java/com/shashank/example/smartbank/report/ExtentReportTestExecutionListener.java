package com.shashank.example.smartbank.report;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import org.junit.platform.engine.support.descriptor.MethodSource;

/** Records JUnit Jupiter test outcomes in an ExtentReports HTML report. */
public class ExtentReportTestExecutionListener implements TestExecutionListener {

	private ExtentReports extentReports;
	private final Map<String, ExtentTest> tests = new ConcurrentHashMap<>();
	private final Map<String, TestCaseDetails> testCaseDetails = new ConcurrentHashMap<>();

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		tests.clear();
		testCaseDetails.clear();
		testCaseDetails.putAll(TestDataWorkbookReader.read());
		extentReports = ExtentReportManager.createReport();
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		if (testIdentifier.isTest()) {
			String methodName = testIdentifier.getSource()
					.filter(MethodSource.class::isInstance)
					.map(MethodSource.class::cast)
					.map(MethodSource::getMethodName)
					.orElse("");
			String className = testIdentifier.getSource()
					.filter(org.junit.platform.engine.support.descriptor.MethodSource.class::isInstance)
					.map(org.junit.platform.engine.support.descriptor.MethodSource.class::cast)
					.map(source -> source.getClassName())
					.orElse("Unknown class");
			TestCaseDetails details = testCaseDetails.get(methodName);
			ExtentTest test = extentReports.createTest(testIdentifier.getDisplayName(),
					details == null ? "No matching test-data row in the Excel workbook." : details.toReportDescription())
					.assignCategory(className);
			tests.put(testIdentifier.getUniqueId(), test);
		}
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		if (testIdentifier.isTest()) {
			String className = testIdentifier.getSource()
					.filter(org.junit.platform.engine.support.descriptor.MethodSource.class::isInstance)
					.map(org.junit.platform.engine.support.descriptor.MethodSource.class::cast)
					.map(source -> source.getClassName())
					.orElse("Unknown class");
			ExtentTest test = extentReports.createTest(testIdentifier.getDisplayName())
					.assignCategory(className);
			test.skip("Skipped or aborted: " + reason);
		}
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
		if (!testIdentifier.isTest()) {
			return;
		}
		ExtentTest test = tests.get(testIdentifier.getUniqueId());
		if (test == null) {
			return;
		}

		switch (result.getStatus()) {
			case SUCCESSFUL -> test.pass("Test passed");
			case ABORTED -> test.skip("Test aborted: " + result.getThrowable()
					.map(Throwable::getMessage).orElse("No reason provided"));
			case FAILED -> test.log(Status.FAIL, result.getThrowable().orElseGet(
					() -> new AssertionError("Test failed without an exception")));
		}
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		if (extentReports != null) {
			extentReports.flush();
		}
	}
}
