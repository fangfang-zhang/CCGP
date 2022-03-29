package yimei.jss.ruleevaluation;

import ec.EvolutionState;
import ec.Fitness;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Parameter;
import org.apache.commons.math3.analysis.function.Abs;
import yimei.jss.jobshop.SchedulingSet;
import yimei.jss.jobshop.WorkCenter;
import yimei.jss.rule.AbstractRule;
import yimei.jss.simulation.DynamicSimulation;
import yimei.jss.simulation.Simulation;
import yimei.jss.surrogate.Surrogate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

//fzhang 4.7.2018    use surrogate or not  use different surrogates in different several generations
//even generations use original shop, odd generations use half shop

public class DynamicGenHalfShopMultipleRuleEvaluationModel extends MultipleRuleEvaluationModel implements Surrogate {

	 //defined by fzhang, 26.4.2018
    int countBadrun = 0;
    int countInd = 0;
    List<Integer> genNumBadRun = new ArrayList<>();
    protected long jobSeed;
    
    protected boolean clear = true;
    
    protected SchedulingSet surrogateSet;
    protected boolean useSurrogate;

    public SchedulingSet getSurrogateSet() {
        return surrogateSet;
    }

    @Override
    public void useSurrogate() {
        useSurrogate = true;
    }

    @Override
    public void useOriginal() {
        useSurrogate = false;
    }

    @Override
    public void setup(final EvolutionState state, final Parameter base) {
        super.setup(state, base);
        
        //original: must be set the surrogateset in details
        //surrogateSet = schedulingSet.surrogate(5, 500, 100, objectives);
       //number of workcenter, job recorded, warmup jobs, objectives.
        useSurrogate();
    }

 // ========================rule evaluatation============================
 	@Override
 	public void evaluate(List<Fitness> currentFitnesses, List<AbstractRule> rules, EvolutionState state) {
 		// expecting 2 rules here - one routing rule and one sequencing rule
 		if (rules.size() != currentFitnesses.size() || rules.size() != 2) {
 			System.out.println("Rule evaluation failed!");
 			System.out.println("Expecting 2 rules, only 1 found.");
 			return;
 		}
 		// System.out.println(rules.size()); //2 repeat
 		countInd++;

 		AbstractRule sequencingRule = rules.get(0); // for each arraylist in list, they have two elements, the first one
 													// is sequencing rule and the second one is routing rule
 		AbstractRule routingRule = rules.get(1);
 		// System.out.println(objectives.size()); //1 repeat
 		// code taken from Abstract Rule
 		double[] fitnesses = new double[objectives.size()];
 		
 		//1---original 
		List<Simulation> simulations;
		
 	/*	if(useSurrogate)
 		    simulations = surrogateSet.getSimulations();
 	    else
 			simulations = schedulingSet.getSimulations();*/
 		
 		
 		//2---fzhang 3.7.2018  use different surrogates in different several generations
		if (useSurrogate) {
			// fzhang 3.7.2018 use different surrogates in different several generations
			if (state.generation % 2 == 0)
				surrogateSet = schedulingSet.surrogate(10, 5000, 1000, objectives);
			else
				surrogateSet = schedulingSet.surrogate(5, 500, 100, objectives);
			simulations = surrogateSet.getSimulations();
		} else
			simulations = schedulingSet.getSimulations();
 		
 		int col = 0;
 		// System.out.println(simulations.size()); // 1 repeat
 		// System.out.println(schedulingSet.getReplications().get(0)); //1 repeat
 		
 		for (int j = 0; j < simulations.size(); j++) {
 			Simulation simulation = simulations.get(j);

 			// ========================change here======================================
 			simulation.setSequencingRule(sequencingRule); // indicate different individuals
 			simulation.setRoutingRule(routingRule);
 			// System.out.println(simulation);
 			simulation.run();

 			for (int i = 0; i < objectives.size(); i++) {
 				double normObjValue = simulation.objectiveValue(objectives.get(i)) // this line: the value of makespan
 						/ schedulingSet.getObjectiveLowerBound(i, col);

 				// in essence, here is useless. because if w.numOpsInQueue() > 100, the
 				// simulation has been canceled in run(). here is a double check
 				for (WorkCenter w : simulation.getSystemState().getWorkCenters()) {
 					if (w.numOpsInQueue() > 100) {
 						// this was a bad run
 						normObjValue = Double.MAX_VALUE;
 						// System.out.println(systemState.getJobsInSystem().size());
 						// System.out.println(systemState.getJobsCompleted().size());

 						// normObjValue =
 						// normObjValue*(systemState.getJobsInSystem().size()/systemState.getJobsCompleted().size());
 						countBadrun++;
 					}
 				}

 				fitnesses[i] += normObjValue; // the value of fitness is the normalization of the objective value
 			}
 			col++;

 			// schedulingSet.getReplications().get(j) = 1, only calculate once, skip this
 			// part here
 			for (int k = 1; k < schedulingSet.getReplications().get(j); k++) {
 				simulation.rerun();

 				for (int i = 0; i < objectives.size(); i++) {
 					double normObjValue = simulation.objectiveValue(objectives.get(i))
 							/ schedulingSet.getObjectiveLowerBound(i, col);
 					fitnesses[i] += normObjValue;
 				}

 				col++;
 			}

 			simulation.reset();
 		}

 		// modified by fzhang 18.04.2018 in order to check this loop works or not after
 		// add filter part: does not work
 		// if(countBadrun>0) {
 		// System.out.println(state.generation);
 		// System.out.println("The number of badrun grasped in model: "+ countBadrun);
 		// }

 		for (int i = 0; i < fitnesses.length; i++) {
 			fitnesses[i] /= col;
 		}

 		for (Fitness fitness : currentFitnesses) {
 			MultiObjectiveFitness f = (MultiObjectiveFitness) fitness;
 			f.setObjectives(state, fitnesses);
 		}

 		// modified by fzhang, write bad run times to *.csv
 		// if(countInd % 512 == 0) {
 		if (countInd % state.population.subpops[0].individuals.length == 0) {
 			genNumBadRun.add(countBadrun);
 			countBadrun = 0;
 		}

 		// if(countInd == 1024*512)
 		if (countInd == state.population.subpops[0].individuals.length * state.population.subpops.length
 				* state.numGenerations)
 			WriteCountBadrun(state, null);
 	}

 	// modified by fzhang 26.4.2018 write bad run times to *.csv
 	public void WriteCountBadrun(EvolutionState state, final Parameter base) {

 		Parameter p;
 		// Get the job seed.
 		p = new Parameter("seed").push("" + 0);
 		jobSeed = state.parameters.getLongWithDefault(p, null, 0);
 		File countBadRunFile = new File("job." + jobSeed + ".BadRun.csv");

 		try {
 			BufferedWriter writer = new BufferedWriter(new FileWriter(countBadRunFile));
 			writer.write("generation,numBadRunSequening, numBadRunRouting,numTotalBadRun");
 			writer.newLine();
 			for (int cutPoint = 0; cutPoint < genNumBadRun.size() / 2; cutPoint++) {
 				writer.write(cutPoint + "," + genNumBadRun.get(2 * cutPoint) + "," + genNumBadRun.get(2 * cutPoint + 1)
 						+ "," + (genNumBadRun.get(2 * cutPoint) + genNumBadRun.get(2 * cutPoint + 1)));
 				writer.newLine();
 			}
 			writer.close();
 		} catch (IOException e) {
 			e.printStackTrace();
 		}
 	}
 

    @Override
    public void rotate() {
        super.rotate();
        surrogateSet.rotateSeed(objectives);
    }
    
    public void setClear(boolean clear) {
        this.clear = clear;
    }
}
