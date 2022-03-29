package yimei.jss.ruleanalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import ec.Fitness;
import ec.gp.koza.KozaFitness;
import ec.multiobjective.MultiObjectiveFitness;
import yimei.jss.rule.operation.evolved.GPRule;
import yimei.util.lisp.LispSimplifier;

public class SimpleResultFileReader extends ResultFileReader {

	public static TestResult readTestResultFromFile(File file, RuleType ruleType, boolean isMultiObjective) {
		TestResult result = new TestResult();

		String line;
		Fitness fitnesses;

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			while (!(line = br.readLine()).equals("Best Individual of Run:")) {
				if (line.startsWith("Generation")) {
					br.readLine(); // Best individual:

					GPRule sequencingRule;
					GPRule routingRule;

					br.readLine(); // Subpopulation 0:
					br.readLine(); // Evaluated: true

					line = br.readLine(); // read in fitness on following line
					fitnesses = readFitnessFromLine(line, isMultiObjective);

					br.readLine(); // tree 0
					line = br.readLine(); // this is a sequencing rule

					// sequencing rule
					line = LispSimplifier.simplifyExpression(line);
					sequencingRule = GPRule.readFromLispExpression(yimei.jss.rule.RuleType.SEQUENCING, line);

					line = "WIQ";
					routingRule = GPRule.readFromLispExpression(yimei.jss.rule.RuleType.ROUTING, line);

					Fitness fitness = fitnesses;
					GPRule[] bestRules = new GPRule[2];

					bestRules[0] = sequencingRule; // sequencing rule
					bestRules[1] = routingRule; // routing rule

					result.setBestRules(bestRules);
					result.setBestTrainingFitness(fitness);

					result.addGenerationalRules(bestRules);
					result.addGenerationalTrainFitness(fitness);
					result.addGenerationalValidationFitnesses((Fitness) fitness.clone());
					result.addGenerationalTestFitnesses((Fitness) fitness.clone());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	private static Fitness readFitnessFromLine(String line, boolean isMultiobjective) {
		if (isMultiobjective) {
			// TODO read multi-objective fitness line
			String[] spaceSegments = line.split("\\s+");
			String[] equation = spaceSegments[1].split("=");
			double fitness = Double.valueOf(equation[1]);
			KozaFitness f = new KozaFitness();
			f.setStandardizedFitness(null, fitness);

			return f;
		} else {
			String[] spaceSegments = line.split("\\s+");
			String[] fitVec = spaceSegments[1].split("\\[|\\]");
			double fitness = Double.valueOf(fitVec[1]);
			MultiObjectiveFitness f = new MultiObjectiveFitness();
			f.objectives = new double[1];
			f.objectives[0] = fitness;

			return f;
		}
	}

}
