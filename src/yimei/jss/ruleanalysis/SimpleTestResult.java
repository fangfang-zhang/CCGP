package yimei.jss.ruleanalysis;

import java.io.File;

public class SimpleTestResult extends TestResult{
	public static TestResult readFromFile(File file, RuleType ruleType) {

		// modified by fzhang 24.5.2018   for multiple trees of one individual
		return SimpleResultFileReader.readTestResultFromFile(file, ruleType, ruleType.isMultiobjective());
	}
}
