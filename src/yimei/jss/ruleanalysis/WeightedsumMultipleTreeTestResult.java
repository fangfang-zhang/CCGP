package yimei.jss.ruleanalysis;

import java.io.File;

public class WeightedsumMultipleTreeTestResult extends TestResult{
	public static TestResult readFromFile(File file, RuleType ruleType, int numTrees) {

		// modified by fzhang 24.5.2018   for multiple trees of one individual
		return WeightedsumMultipleTreeResultFileReader.readTestResultFromFile(file, ruleType, ruleType.isMultiobjective(),
				numTrees);
	}
}
