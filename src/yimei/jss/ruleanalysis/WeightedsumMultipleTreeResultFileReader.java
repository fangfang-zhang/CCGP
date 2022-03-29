package yimei.jss.ruleanalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import ec.Fitness;
import ec.gp.koza.KozaFitness;
import ec.multiobjective.MultiObjectiveFitness;
import yimei.jss.rule.operation.evolved.GPRule;
import yimei.util.lisp.LispSimplifier;

public class WeightedsumMultipleTreeResultFileReader extends ResultFileReader {

	public static TestResult readTestResultFromFile(File file, RuleType ruleType, boolean isMultiObjective,
			int numTrees) {
		TestResult result = new TestResult();

		String line;
		Fitness fitnesses;

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			while (!(line = br.readLine()).equals("Best Individual of Run:")) {
//				while (!(line = br.readLine()).equals(" PARETO FRONTS")) {
				
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

					// routing rule
					br.readLine();
					line = br.readLine();
					routingRule = GPRule.readFromLispExpression(yimei.jss.rule.RuleType.ROUTING, line);

					Fitness fitness = fitnesses;
					GPRule[] bestRules = new GPRule[numTrees];

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
	
	
	private static MultiObjectiveFitness parseFitness(String line)
	{
		String[] spaceSegments = line.split("\\s+");//\\s表示   空格,回车,换行等空白符, +号表示一个或多个的意思
		MultiObjectiveFitness f = new MultiObjectiveFitness();
		f.objectives = new double[spaceSegments.length - 1];
		for(int i = 1; i < spaceSegments.length; i++)
		{
			String[] equation = spaceSegments[i].split("\\[|\\]");
			double fitness = Double.valueOf(equation[i == 1 ? 1 : 0]);
			f.objectives[i-1] = fitness;
		}
		
		return f;
//		String[] equation1 = spaceSegments[1].split("\\[|\\]");
//		String[] equation2 = spaceSegments[2].split("\\[|\\]");
//		double fitness1 = Double.valueOf(equation1[1]);
//		double fitness2 = Double.valueOf(equation2[0]);
////		MultiObjectiveFitness f = new MultiObjectiveFitness();
////		f.objectives = new double[2];
//		f.objectives[0] = fitness1;
//		f.objectives[1] = fitness2;
	}

	private static Fitness readFitnessFromLine(String line, boolean isMultiobjective) {
		if (isMultiobjective) {
			// TODO read multi-objective fitness line
			/*String[] spaceSegments = line.split("\\s+");//\\s表示   空格,回车,换行等空白符, +号表示一个或多个的意思
			String[] equation = spaceSegments[1].split("=");
			double fitness = Double.valueOf(equation[1]);
			KozaFitness f = new KozaFitness();
			f.setStandardizedFitness(null, fitness);*/
			
			//save objective from training fzhang 18.11.2018
			/*String[] spaceSegments = line.split("\\s+");//\\s表示   空格,回车,换行等空白符, +号表示一个或多个的意思
			String[] equation1 = spaceSegments[1].split("\\[|\\]");
			String[] equation2 = spaceSegments[2].split("\\[|\\]");
			double fitness1 = Double.valueOf(equation1[1]);
			double fitness2 = Double.valueOf(equation2[0]);
			MultiObjectiveFitness f = new MultiObjectiveFitness();
			f.objectives = new double[2];
			f.objectives[0] = fitness1;
			f.objectives[1] = fitness2;

			return f;*/
			return parseFitness(line);
			 
		} else {
			String[] spaceSegments = line.split("\\s+"); // . 、 | 和 * 等转义字符，必须得加 \\。
			String[] fitVec = spaceSegments[1].split("\\[|\\]");//多个分隔符，可以用 | 作为连字符。
			double fitness = Double.valueOf(fitVec[1]);
			MultiObjectiveFitness f = new MultiObjectiveFitness();
			f.objectives = new double[1];
			f.objectives[0] = fitness;

			return f;
		}
	}

	   //24.8.2018  fzhang read badrun from CSV
    public static DescriptiveStatistics readBadRunFromFile(File file) {
        DescriptiveStatistics generationalBadRunStat = new DescriptiveStatistics();

        String line;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            br.readLine();
            while(true) {
                line = br.readLine();

                if (line == null)
                    break;

                String[] commaSegments = line.split(",");
                generationalBadRunStat.addValue(Double.valueOf(commaSegments[1])); //read from excel, the first column is 0
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return generationalBadRunStat;
    }


    //fzhang 2019.1.14 read training fitness from CSV
	public static DescriptiveStatistics readTrainingFitness0FromFile(File file) {
		DescriptiveStatistics generationalTrainingFitnessStat0 = new DescriptiveStatistics();
		
        String line;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            br.readLine();
            while(true) {
                line = br.readLine();

                if (line == null)
                    break;

                String[] commaSegments = line.split(",");
                generationalTrainingFitnessStat0.addValue(Double.valueOf(commaSegments[1])); //read from excel, the first column is 0
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return generationalTrainingFitnessStat0;
	}
    
    //fzhang 2019.1.14 read training fitness from CSV
	public static DescriptiveStatistics readTrainingFitness1FromFile(File file) {
		DescriptiveStatistics generationalTrainingFitnessStat1 = new DescriptiveStatistics();
		
        String line;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            br.readLine();
            while(true) {
                line = br.readLine();

                if (line == null)
                    break;

                String[] commaSegments = line.split(",");
                generationalTrainingFitnessStat1.addValue(Double.valueOf(commaSegments[2])); //read from excel, the first column is 0
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return generationalTrainingFitnessStat1;
	}
}
