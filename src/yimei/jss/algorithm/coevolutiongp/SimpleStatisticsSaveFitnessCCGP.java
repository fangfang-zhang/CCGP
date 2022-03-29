/*
  Copyright 2006 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/


package yimei.jss.algorithm.coevolutiongp;
import ec.*;
import ec.gp.GPNode;
import ec.simple.SimpleProblemForm;
import ec.steadystate.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ec.util.*;
import yimei.jss.rule.operation.evolved.GPRule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import static yimei.jss.gp.GPRun.out_dir;

/*
 * SimpleStatistics.java
 *
 * Created: Tue Aug 10 21:10:48 1999
 * By: Sean Luke
 */

/**
 * A basic Statistics class suitable for simple problem applications.
 *
 * SimpleStatistics prints out the best individual, per subpopulation,
 * each generation.  At the end of a run, it also prints out the best
 * individual of the run.  SimpleStatistics outputs this data to a log
 * which may either be a provided file or stdout.  Compressed files will
 * be overridden on restart from checkpoint; uncompressed files will be
 * appended on restart.
 *
 * <p>SimpleStatistics implements a simple version of steady-state statistics:
 * if it quits before a generation boundary,
 * it will include the best individual discovered, even if the individual was discovered
 * after the last boundary.  This is done by using individualsEvaluatedStatistics(...)
 * to update best-individual-of-generation in addition to doing it in
 * postEvaluationStatistics(...).

 <p><b>Parameters</b><br>
 <table>
 <tr><td valign=top><i>base.</i><tt>gzip</tt><br>
 <font size=-1>boolean</font></td>
 <td valign=top>(whether or not to compress the file (.gz suffix added)</td></tr>
 <tr><td valign=top><i>base.</i><tt>file</tt><br>
 <font size=-1>String (a filename), or nonexistant (signifies stdout)</font></td>
 <td valign=top>(the log for statistics)</td></tr>
 </table>

 *
 * @author Sean Luke
 * @version 1.0
 */

public class SimpleStatisticsSaveFitnessCCGP extends Statistics implements SteadyStateStatisticsForm //, ec.eval.ProvidesBestSoFar
    {
	/*//fzhang 20.7.2018 in order to get breed
	 public final static String P_BREEDER = "breed";
	 public Breeder breeder;
	 public ParameterDatabase parameters;*/
	 
    public Individual[] getBestSoFar() { return best_of_run; }

    /** log file parameter */
    public static final String P_STATISTICS_FILE = "file";

    /** compress? */
    public static final String P_COMPRESS = "gzip";

    public static final String P_DO_FINAL = "do-final";
    public static final String P_DO_GENERATION = "do-generation";
    public static final String P_DO_MESSAGE = "do-message";
    public static final String P_DO_DESCRIPTION = "do-description";
    public static final String P_DO_PER_GENERATION_DESCRIPTION = "do-per-generation-description";
    //get seed
    protected long jobSeed;
    
    //fzhang 25.6.2018 in order to save the rulesize in each generation
    List<Double> aveSeqRulesizeSubPop0 = new ArrayList<>();
    List<Double> aveRouRulesizeSubPop1 = new ArrayList<>();

    /** The Statistics' log */
    public int statisticslog = 0;  // stdout

    /** The best individual we've found so far */
    public Individual[] best_of_run = null;

    /** Should we compress the file? */
    public boolean compress;
    public boolean doFinal;
    public boolean doGeneration;
    public boolean doMessage;
    public boolean doDescription;
    public boolean doPerGenerationDescription;

    //fzhang  in order to save the fiteness in each generation
/*    List<String> fitnessSubPop0 = new ArrayList<>();
    List<String> fitnessSubPop1 = new ArrayList<>();*/

    //2021.9.20 save the fitness of the best individual in each subpopulation
    public static ArrayList<Double> bestFitnessSubpop0 = new ArrayList<>();
    public static ArrayList<Double> bestFitnessSubpop1 = new ArrayList<>();

    public void setup(final EvolutionState state, final Parameter base)
        {
        super.setup(state,base);

        compress = state.parameters.getBoolean(base.push(P_COMPRESS),null,false);

        File statisticsFile = state.parameters.getFile(
            base.push(P_STATISTICS_FILE),null);

        doFinal = state.parameters.getBoolean(base.push(P_DO_FINAL),null,true);
        doGeneration = state.parameters.getBoolean(base.push(P_DO_GENERATION),null,true);
        //System.out.println(doGeneration); //true
        doMessage = state.parameters.getBoolean(base.push(P_DO_MESSAGE),null,true);
        doDescription = state.parameters.getBoolean(base.push(P_DO_DESCRIPTION),null,true);
        doPerGenerationDescription = state.parameters.getBoolean(base.push(P_DO_PER_GENERATION_DESCRIPTION),null,false);

        Parameter p;
		// Get the job seed.
		p = new Parameter("seed").push(""+0);
        jobSeed = state.parameters.getLongWithDefault(p, null, 0);
        //System.out.println(jobSeed);

        //generation
        //int generation = state.parameters.getIntWithDefault(new Parameter("generations"), null, 0);
        //System.out.println(generation);

        if (silentFile)
            {
            statisticslog = Output.NO_LOGS;
            }
        else if (statisticsFile!=null)
            {
            try
                {
                statisticslog = state.output.addLog(statisticsFile, !compress, compress);
                }
            catch (IOException i)
                {
                state.output.fatal("An IOException occurred while trying to create the log " + statisticsFile + ":\n" + i);
                }
            }
        else state.output.warning("No statistics file specified, printing to stdout at end.", base.push(P_STATISTICS_FILE));
        }

	public void postInitializationStatistics(final EvolutionState state) {
		super.postInitializationStatistics(state);

		// set up our best_of_run array -- can't do this in setup, because
		// we don't know if the number of subpopulations has been determined yet
		best_of_run = new Individual[state.population.subpops.length]; 
	}

    /** Logs the best individual of the generation. */
    boolean warned = false;
    public void postEvaluationStatistics(final EvolutionState state)
        {
        super.postEvaluationStatistics(state);

        // for now we just print the best fitness per subpopulation.
        Individual[] best_i = new Individual[state.population.subpops.length];  // quiets compiler complaints
        for(int x=0;x<state.population.subpops.length;x++)
            {
            best_i[x] = state.population.subpops[x].individuals[0];
            for(int y=1;y<state.population.subpops[x].individuals.length;y++)
                {
                if (state.population.subpops[x].individuals[y] == null)
                    {
                    if (!warned)
                        {
                        state.output.warnOnce("Null individuals found in subpopulation");
                        warned = true;  // we do this rather than relying on warnOnce because it is much faster in a tight loop
                        }
                    }
                else if (best_i[x] == null || state.population.subpops[x].individuals[y].fitness.betterThan(best_i[x].fitness))
                     best_i[x] = state.population.subpops[x].individuals[y];

                if (best_i[x] == null)
                    {
                    if (!warned)
                        {
                        state.output.warnOnce("Null individuals found in subpopulation");
                        warned = true;  // we do this rather than relying on warnOnce because it is much faster in a tight loop
                        }
                    }
                }

            // now test to see if it's the new best_of_run
            if (best_of_run[x]==null || best_i[x].fitness.betterThan(best_of_run[x].fitness))
                best_of_run[x] = (Individual)(best_i[x].clone());
            }

//        //Only care about overall best fitness
//        //Collab and elite individuals will switch roles each generation, so which subpop is better is not
//        //important
//        Individual best = best_i[0];
//        for (int i = 1; i < state.population.subpops.length; ++i) {
//            if (best_i[i].fitness.betterThan(best.fitness)) {
//                best = best_i[i];
//            }
//        }
//        //easier just to copy than change code below
//        for (int i = 0; i < state.population.subpops.length; ++i) {
//            best_i[i] = best;
//        }

        // print the best-of-generation individual

        if (doGeneration) state.output.println("\nGeneration: " + state.generation,statisticslog);
        if (doGeneration) state.output.println("Best Individual:",statisticslog);

        for(int x=0;x<state.population.subpops.length;x++)
            {
            if (doGeneration) state.output.println("Subpopulation " + x + ":",statisticslog);
            if (doGeneration) best_i[x].printIndividualForHumans(state,statisticslog);
            if (doMessage && !silentPrint) state.output.message("Subpop " + x + " best fitness of generation" +
                (best_i[x].evaluated ? " " : " (evaluated flag not set): ") +
                best_i[x].fitness.fitnessToStringForHumans());

            //2019.9.20 save the best fitness for each subpop
                if (x == 0) {
                    bestFitnessSubpop0.add(best_i[x].fitness.fitness());
                } else {
                    bestFitnessSubpop1.add(best_i[x].fitness.fitness());
                }


            //save the fitness values to .csv  26/3/2018   fzhang
         /*   if(x==0)
               fitnessSubPop0.add(best_i[x].fitness.fitnessToStringForHumans());
               //fitnessSubPop0.add(best_i[x].fitness.fitnessToStringForHumans());   //[1.21444]
               // fitnessSubPop0.add(best_i[x].fitness.fitnessToString());
            if(x==1)
               fitnessSubPop1.add(best_i[x].fitness.fitnessToStringForHumans());
*/
            // describe the winner if there is a description
            if (doGeneration && doPerGenerationDescription)
                {
                if (state.evaluator.p_problem instanceof SimpleProblemForm)
                    ((SimpleProblemForm)(state.evaluator.p_problem.clone())).describe(state, best_i[x], x, 0, statisticslog);
                }
            }

		//System.out.println(fitnessSubPop0.size());
        //save fitness values into .csv

        //save the fitness values to .csv  26/3/2018   fzhang
     /*   File fitnessFile = new File("job." + jobSeed + ".fitness.csv"); //jobSeed = 0
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(fitnessFile));
			writer.write("Gen,SubPop1,SubPop2");
			writer.newLine();
			for (int gen = 0; gen < fitnessSubPop0.size(); gen++) {
				writer.write(gen + "," + extractFitness(fitnessSubPop0.get(gen)) + "," + extractFitness(fitnessSubPop1.get(gen)));
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
		}

    /** Allows MultiObjectiveStatistics etc. to call super.super.finalStatistics(...) without
        calling super.finalStatistics(...) */
    protected void bypassFinalStatistics(EvolutionState state, int result)
        { super.finalStatistics(state, result); }

    /** Logs the best individual of the run. */
    public void finalStatistics(final EvolutionState state, final int result)
        {
        super.finalStatistics(state,result);

        // for now we just print the best fitness

        if (doFinal) state.output.println("\nBest Individual of Run:",statisticslog);
        for(int x=0;x<state.population.subpops.length;x++ )
            {
            if (doFinal) state.output.println("Subpopulation " + x + ":",statisticslog);
            if (doFinal) best_of_run[x].printIndividualForHumans(state,statisticslog);
            if (doMessage && !silentPrint) state.output.message("Subpop " + x + " best fitness of run: " + best_of_run[x].fitness.fitnessToStringForHumans());

            // finally describe the winner if there is a description
            if (doFinal && doDescription)
                if (state.evaluator.p_problem instanceof SimpleProblemForm)
                    ((SimpleProblemForm)(state.evaluator.p_problem.clone())).describe(state, best_of_run[x], x, 0, statisticslog);
            }

        //save the best value in after last generation  fzhang 27/3/2018
        //File fitnessFile = new File("job." + jobSeed + ".bestfitness.csv"); //jobSeed = 0

        String bestfitnessSubpop0 = extractFitness(best_of_run[0].fitness.fitnessToStringForHumans());
        String bestfitnessSubpop1 = extractFitness(best_of_run[1].fitness.fitnessToStringForHumans());

        double bestfitnessFinalSub0 = Double.parseDouble(bestfitnessSubpop0);
        double bestfitnessFinalSub1 = Double.parseDouble(bestfitnessSubpop1);

        double selectedBestFitness = bestfitnessFinalSub0;
        if(Double.compare(selectedBestFitness, bestfitnessFinalSub1)>0) {
        	selectedBestFitness = bestfitnessFinalSub1;
        }

		/*try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(fitnessFile));
			writer.write("bestfitnessSubPop1,bestfitnessSubPop2,selectedBestfitness");
			writer.newLine();

	 	    writer.write(bestfitnessSubpop0 + "," +
	 	    	     	 bestfitnessSubpop1 + "," + selectedBestFitness);
		    writer.newLine();

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
        }

    //convert string including characters and numbers to numbers  26/3/2017 fzhang
    public String extractFitness(String s) {
    	String split =s.split("\\[")[1];
    	split = split.split("\\]")[0];
    	return split;
    }
    
    /** GENERATIONAL: Called immediately before evaluation occurs. */
    public void preEvaluationStatistics(final EvolutionState state)
        {
    	for(int x=0;x<children.length;x++)
            children[x].preEvaluationStatistics(state);
    	//==============================start==================================================================
        // fzhang 15.6.2018 1. save the individual size in population
 		// 2. calculate the average size of individuals in population
 		// check the average size of sequencing and routing rules in population
        //fzhang 15.6.2018  in order to check the average size of sequencing and routing rules in population
		int SeqSizePop0 = 0;
		int RouSizePop1 = 0;
        //int indSizePop = 0; // in order to check whether SeqSizePop1 and RouSizePop2 are calculated correctly
                        // should be the sum of SeqSizePop1 and RouSizePop2
		double aveSeqSizePop0 = 0;
		double aveRouSizePop1 = 0; //change this to double, in this way, 11+12=11.5
		for (int ind = 0; ind < state.population.subpops[0].individuals.length; ind++) {
			SeqSizePop0 += state.population.subpops[0].individuals[ind].size();
		}

		for (int ind = 0; ind < state.population.subpops[1].individuals.length; ind++) {
			RouSizePop1 += state.population.subpops[1].individuals[ind].size();
		}
		aveSeqSizePop0 = SeqSizePop0 / state.population.subpops[0].individuals.length;
		aveRouSizePop1 = RouSizePop1 / state.population.subpops[1].individuals.length;
		aveSeqRulesizeSubPop0.add(aveSeqSizePop0);
		aveRouRulesizeSubPop1.add(aveRouSizePop1);
		
		if(state.generation == state.numGenerations-1) {
			//fzhang  15.6.2018  save the size of rules in each generation
		    File rulesizeFile = new File(out_dir + "/job." + jobSeed + ".aveGenRulesize.csv"); // jobSeed = 0
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(rulesizeFile));
				writer.write("Gen,aveSeqRuleSize,aveRouRuleSize,avePairSize");
				writer.newLine();
				for (int gen = 0; gen < aveSeqRulesizeSubPop0.size(); gen++) {
					writer.write(gen + "," + aveSeqRulesizeSubPop0.get(gen) + "," + aveRouRulesizeSubPop1.get(gen) + ","
							+ (aveSeqRulesizeSubPop0.get(gen) + aveRouRulesizeSubPop1.get(gen))/2);
					writer.newLine();
				} 
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	 			
		/*	System.out.println(SeqSizePop0);
			System.out.println(RouSizePop1);
			System.out.println(aveSeqSizePop0);
			System.out.println(aveRouSizePop1);
	 		*/
	 	//fzhang 15.6.2018 in order to check whether SeqSizePop1 and RouSizePop2 are calculated correctly (YES)
	 	/*	for (int pop = 0; pop < state.population.subpops.length; pop++) {
	 			for (int ind = 0; ind < state.population.subpops[pop].individuals.length; ind++) {
	 				indSizePop += state.population.subpops[pop].individuals[ind].size();
	 			}
	 		}
	 		System.out.println(indSizePop);*/
		}
		//=======================================end====================================================================
        }
    
    }

