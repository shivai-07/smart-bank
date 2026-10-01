package com.shashank.example.smartbank.report;

import java.nio.file.Path;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

/** Creates the HTML report used by the JUnit 5 listener. */
public final class ExtentReportManager {

	private static final String REPORT_PATH = "target/extent-reports/ExtentReport.html";

	private ExtentReportManager() {
	}

	public static ExtentReports createReport() {
		ExtentSparkReporter sparkReporter = new ExtentSparkReporter(Path.of(REPORT_PATH).toString());
		sparkReporter.config().setDocumentTitle("Smart Bank Test Report");
		sparkReporter.config().setReportName("JUnit 5 Test Results");

		ExtentReports extentReports = new ExtentReports();
		extentReports.attachReporter(sparkReporter);
		extentReports.setSystemInfo("Project", "smart-bank");
		extentReports.setSystemInfo("Test data source", "testdata/SmartBankTestData.xlsx (TestCases sheet)");
		return extentReports;
	}
}
