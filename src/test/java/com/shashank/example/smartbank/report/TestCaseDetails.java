package com.shashank.example.smartbank.report;

/** Test steps and expectations maintained in the test-data workbook. */
record TestCaseDetails(String steps, String testData, String expectedMessage) {

	String toReportDescription() {
		return "Steps: " + steps + "<br>Test data: " + testData + "<br>Expected message/result: " + expectedMessage;
	}
}
