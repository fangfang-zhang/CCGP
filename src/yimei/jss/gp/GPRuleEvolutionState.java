package yimei.jss.gp;

import java.io.*;
import java.util.*;

import ec.Individual;
import yimei.jss.gp.terminal.AttributeGPNode;
import yimei.jss.gp.terminal.DoubleERC;
import yimei.jss.gp.terminal.JobShopAttribute;
import yimei.jss.helper.PopulationUtils;
import yimei.jss.niching.PhenoCharacterisation;
import yimei.jss.niching.RoutingPhenoCharacterisation;
import yimei.jss.niching.SequencingPhenoCharacterisation;
import yimei.jss.niching.phenotypicForSurrogate;
import yimei.jss.rule.AbstractRule;
import yimei.jss.rule.AbstractRuleHelper;
import ec.EvolutionState;
import ec.gp.GPNode;
import ec.simple.SimpleEvolutionState;
import ec.util.Checkpoint;
import ec.util.Parameter;
import yimei.jss.ruleoptimisation.RuleOptimizationProblem;

import static yimei.jss.algorithm.coevolutiongp.SimpleStatisticsSaveFitnessCCGP.bestFitnessSubpop0;
import static yimei.jss.algorithm.coevolutiongp.SimpleStatisticsSaveFitnessCCGP.bestFitnessSubpop1;
import static yimei.jss.gp.GPRun.out_dir;

/**
 * The evolution state of evolving dispatching rules with GP.
 *
 * @author yimei
 *
 */

public class GPRuleEvolutionState extends SimpleEvolutionState {

	/**
	 * Read the file to specify the terminals.
	 */
	public final static String P_TERMINALS_FROM = "terminals-from";
	public final static String P_INCLUDE_ERC = "include-erc";

	protected String[] terminalsFrom;
	protected boolean[] includeErc;
//	protected long jobSeed;
    public static long jobSeed;
	protected GPNode[][] terminals;
	//fzhang 2019.5.20 set weights to terminals in each subpop
	protected double[][] weights;

	public void setWeights(double[][] weights){
		this.weights = weights;
	}

	List<Double> genTimes = new ArrayList<>();

	public static ArrayList<Double> averageFitnessGensSubpop0 = new ArrayList<>();
	public static ArrayList<Double> averageFitnessGensSubpop1 = new ArrayList<>();

	public static ArrayList<Integer> numIndsSubpop0 = new ArrayList<>();
	public static ArrayList<Integer> numIndsSubpop1 = new ArrayList<>();

	ArrayList<Double> entropyDiversity0 = new ArrayList<>();
	ArrayList<Double> entropyDiversity1 = new ArrayList<>();

	public static PhenoCharacterisation[] phenoCharacterisation;


	public GPNode[][] getTerminals() {
		return terminals;
	}

    public GPNode[] getTerminals(int subPopNum) {
        return terminals[subPopNum];
    }

    public long getJobSeed() {
		return jobSeed;
	}

	public void setTerminals(GPNode[][] terminals) {
		this.terminals = terminals;
	}

    public void setTerminals(GPNode[] terminals, int subPopNum) {
        this.terminals[subPopNum] = terminals;
    }

    //fzhang 2019.5.20 set weights to each subpopulation
/*    public void setWeights(double[] weights, int subPopNum) {
        this.weights[subPopNum] = weights;
    }*/

    //fzhang 16.7.2018 in order to use selected features to initialize population
    public void setTreeTerminals(GPNode[] terminals, int numTrees) {
        this.terminals[numTrees] = terminals;
    }

    /**
	 * Initialize the terminal set with all the job shop attributes.
	 */
	public void initTerminalSet() {
        int numSubPops = parameters.getInt(new Parameter("pop.subpops"),null); //numSubPops = 2
        //terminals is a double array [][], type: GPNode
        this.terminals = new GPNode[numSubPops][];

        for (int subPopNum = 0; subPopNum < numSubPops; subPopNum++) {
         //for (int subPopNum = 0; subPopNum < numSubPops; ++subPopNum) {
        	//terminals From should have two elements, terminalsFrom[0] and terminalsFrom[1]
        	//terminals-from.0 = relative            terminalsFrom[0] = relative
        	//terminals-from.1 = systemstate         terminalsFrom[1] = systemstate   Yes, it is right.
            String terminalFrom = terminalsFrom[subPopNum];
            boolean includeErc = this.includeErc[subPopNum];
            if (terminalFrom.equals("basic")) {
                initBasicTerminalSet(subPopNum);
            }
            else if (terminalFrom.equals("relative")) {
            	//that is to say, put the terminals we defined into terminals[]
                initRelativeTerminalSet(subPopNum);
            }
            //modified by fzhang 27.5.2018   create some new features mainly about systemstate
            else if(terminalFrom.equals("systemstate")) {
            	initSystemstateTerminalSet(subPopNum);
            }
          //modified by fzhang 6.6.2018   create some new features from relativeWithoutWeight
            else if(terminalFrom.equals("relativeWithoutWeight")) {
            	initRelativeWithoutWeightTerminalSet(subPopNum);
            }
            //fzhang 19.7.2018
            else if(terminalFrom.equals("current")) {
            	initRelativeCurrentAttributesTerminalSet(subPopNum);
            }
            else if(terminalFrom.equals("future")) {
            	initRelativeFutureAttributesTerminalSet(subPopNum);
            }
            else if(terminalFrom.equals("history")) {
            	initRelativeHistoryAttributesTerminalSet(subPopNum);
            }
            else {
            	String terminalFile = terminalFrom;
                //String terminalFile = "terminals/" + terminalFrom;  //set directly in parameter file fzhang 21.6.2018
                initTerminalSetFromCsv(new File(terminalFile), subPopNum);
            }

            if (includeErc) {
                //terminals.add(new DoubleERC());
                //TODO: Implement this
                System.out.println("INCLUDE ERC NOT IMPLEMENTED");
            }
        }
	}

	public void initBasicTerminalSet(int subPopNum) {
		LinkedList<GPNode> terminals = new LinkedList<GPNode>();
		for (JobShopAttribute a : JobShopAttribute.basicAttributes()) {
            GPNode attribute = new AttributeGPNode(a);
            terminals.add(attribute);
		}
		this.terminals[subPopNum] = terminals.toArray(new GPNode[0]);

	}

	public void initRelativeTerminalSet(int subPopNum) {
        LinkedList<GPNode> terminals = new LinkedList<GPNode>();
		for (JobShopAttribute a : JobShopAttribute.relativeAttributes()) {
		    GPNode attribute = new AttributeGPNode(a);
			terminals.add(attribute); //read all the terminals we defined to keep them into terminals[]
		}
        this.terminals[subPopNum] = terminals.toArray(new GPNode[0]);
    }

	//modified by fzhang  27.5.2018  create some new features
	public void initSystemstateTerminalSet(int subPopNum) {
        LinkedList<GPNode> terminals = new LinkedList<GPNode>();
		for (JobShopAttribute a : JobShopAttribute.systemstateAttributes()) {
		    GPNode attribute = new AttributeGPNode(a);
			terminals.add(attribute);  //read terminals into terminals[]  rigth
		}
        this.terminals[subPopNum] = terminals.toArray(new GPNode[0]);  //terminals[subPopNum] value is right
    }

	//modified by fzhang  6.6.2018  do not use weight as terminal in non-weighted-objective
		public void initRelativeWithoutWeightTerminalSet(int subPopNum) {
	        LinkedList<GPNode> terminals = new LinkedList<GPNode>();
			for (JobShopAttribute a : JobShopAttribute.relativeWithoutWeightAttributes()) {
			    GPNode attribute = new AttributeGPNode(a);
				terminals.add(attribute);
			}
	        this.terminals[subPopNum] = terminals.toArray(new GPNode[0]);
	    }

	// modified by fzhang 19.7.2018  relativeCurrentAttributes
	public void initRelativeCurrentAttributesTerminalSet(int subPopNum) {
		LinkedList<GPNode> terminals = new LinkedList<GPNode>();
		for (JobShopAttribute a : JobShopAttribute.relativeCurrentAttributes()) {
			GPNode attribute = new AttributeGPNode(a);
			terminals.add(attribute);
		}
		this.terminals[subPopNum] = terminals.toArray(new GPNode[0]);
	}
	//relativeFutureAttributes
	public void initRelativeFutureAttributesTerminalSet(int subPopNum) {
		LinkedList<GPNode> terminals = new LinkedList<GPNode>();
		for (JobShopAttribute a : JobShopAttribute.relativeFutureAttributes()) {
			GPNode attribute = new AttributeGPNode(a);
			terminals.add(attribute);
		}
		this.terminals[subPopNum] = terminals.toArray(new GPNode[0]);
	}

	//relativeHistoryAttributes
	public void initRelativeHistoryAttributesTerminalSet(int subPopNum) {
		LinkedList<GPNode> terminals = new LinkedList<GPNode>();
		for (JobShopAttribute a : JobShopAttribute.relativeHistoryAttributes()) {
			GPNode attribute = new AttributeGPNode(a);
			terminals.add(attribute);
		}
		this.terminals[subPopNum] = terminals.toArray(new GPNode[0]);
	}

	public void initTerminalSetFromCsv(File csvFile, int subPopNum) {
        LinkedList<GPNode> terminals = new LinkedList<GPNode>();

		BufferedReader br = null;
        String line = "";

        try {
            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {
            	JobShopAttribute a = JobShopAttribute.get(line);
                GPNode attribute = new AttributeGPNode(a);
                terminals.add(attribute);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        this.terminals[subPopNum] = terminals.toArray(new GPNode[0]);
    }

	/**
	 * Return the index of an attribute in the terminal set.
	 * @param attribute the attribute.
	 * @return the index of the attribute in the terminal set.
	 */
	public int indexOfAttribute(JobShopAttribute attribute, int subPopNum) {
	    GPNode[] terminals = this.terminals[subPopNum];
		for (int i = 0; i < terminals.length; i++) {
			JobShopAttribute terminalAttribute = ((AttributeGPNode)terminals[i]).getJobShopAttribute();
			if (terminalAttribute == attribute) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * Randomly pick a terminal from the terminal set.
	 * @return the selected terminal, which is a GPNode.
	 */
	//fzhang  random pick terminals will come to here
	public GPNode pickTerminalRandom(int subPopNum) {
    	int index = random[0].nextInt(terminals[subPopNum].length);
    	return terminals[subPopNum][index];
    }

    //fzhang 2019.5.27 another pickTerminalRandom with different parameters
	public GPNode pickTerminalRandom(EvolutionState state, int subPopNum) {
		int index = state.random[0].nextInt(terminals[subPopNum].length);
		return terminals[subPopNum][index];
	}

	// the best individual in subpopulation
	public Individual bestIndi(int subpop) {
		int best = 0;
		for(int x = 1; x < population.subpops[subpop].individuals.length; x++)
			if (population.subpops[subpop].individuals[x].fitness.betterThan(
			        population.subpops[subpop].individuals[best].fitness))
				best = x;

		return population.subpops[subpop].individuals[best];
	}

	@Override
	public void setup(EvolutionState state, Parameter base) {
		Parameter p;
		//fzhang 2018.11.8 I need to do this to be able to load seed values in the AbtractRule class.
		AbstractRuleHelper.state = this;

		// Get the job seed.
		p = new Parameter("seed").push(""+0);
		jobSeed = parameters.getLongWithDefault(p, null, 0);

		String filePath = this.parameters.getString(new Parameter("filePath"), null);
		phenoCharacterisation = new PhenoCharacterisation[2];
		//this is the baseline PhenoCharacterisation with baseline rule "SPT" "WIQ", it will be set again by the best rule, so it is useful here
		if (filePath == null) {
			//dynamic simulation
			phenoCharacterisation[0] =
					SequencingPhenoCharacterisation.defaultPhenoCharacterisation();
			phenoCharacterisation[1] =
					RoutingPhenoCharacterisation.defaultPhenoCharacterisation();
		} else {
			//static simulation
			phenoCharacterisation[0] =
					SequencingPhenoCharacterisation.defaultPhenoCharacterisation(filePath);
			phenoCharacterisation[1] =
					RoutingPhenoCharacterisation.defaultPhenoCharacterisation(filePath);
		}

		setupTerminals();

		super.setup(this, base);
	}

	@Override
	public void run(int condition)
    {
		double totalTime = 0;

		if (condition == C_STARTED_FRESH) {
			startFresh();
        }
		else {
			startFromCheckpoint();
        }

		int result = R_NOTDONE; //2, means not finished, continue to do
		while ( result == R_NOTDONE )
        {
			//fzhang 21.7.2018  after startFresh(), we will in this loop
			//here, to reFresh breeder only
			//startFreshResetOperatorProb();

			long start = System.currentTimeMillis();//yimei.util.Timer.getCpuTime();

			result = evolve();//System.out.println(result);

			long finish =System.currentTimeMillis();// yimei.util.Timer.getCpuTime();
			double duration = (finish - start) / 1000;//000000;
			genTimes.add(duration);
			totalTime += duration;

			output.message("Generation " + (generation-1) + " elapsed " + duration + " seconds.");// time used for each generation
        }

		output.message("The whole program elapsed " + totalTime + " seconds."); // time used for total program

		File timeFile = new File(out_dir + "/job." + jobSeed + ".time.csv"); //jobSeed = 0
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(timeFile));
			writer.write("Gen,Time");
			writer.newLine();
			for (int gen = 0; gen < genTimes.size(); gen++) {
				writer.write(gen + "," + genTimes.get(gen));
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		finish(result);
    }

    @Override
	public int evolve() {
	    if (generation > 0)
	        output.message("Generation " + generation);

	    //System.out.println("generation "+generation);
	    // EVALUATION
	    statistics.preEvaluationStatistics(this);

	    evaluator.evaluatePopulation(this);  //// here, after this we evaluate the population

		//2022.3.1 get the diversity of populations
		ArrayList<Individual> indsTask0Subpop0 = new ArrayList<>();
		ArrayList<Individual> indsTask1Subpop1 = new ArrayList<>();

		for(int i = 0; i < this.population.subpops[0].individuals.length; i++){
			indsTask0Subpop0.add(this.population.subpops[0].individuals[i]); //by the way, save the inds to arraylist
		}

		ArrayList<Double> averageFitnessCurrentGenTask10 = new ArrayList<>();
		ArrayList<Double> averageFitnessCurrentGenTask11 = new ArrayList<>();
		for(int i = 0; i < this.population.subpops[1].individuals.length; i++){
			indsTask1Subpop1.add(this.population.subpops[1].individuals[i]); //by the way, save the inds to arraylist
		}

		//2021.9.22 calculate the diversity of tasks
		//====================start===================
		int[][] indsCharListsMultiTree0 = phenotypicForSurrogate.muchBetterPhenotypicPopulation(indsTask0Subpop0, phenoCharacterisation); //3. calculate the phenotypic characteristic
		double diversityValue0 = PopulationUtils.entropy(indsCharListsMultiTree0);
		entropyDiversity0.add(diversityValue0);

		int[][] indsCharListsMultiTree1 = phenotypicForSurrogate.muchBetterPhenotypicPopulation(indsTask1Subpop1, phenoCharacterisation); //3. calculate the phenotypic characteristic
		double diversityValue1 = PopulationUtils.entropy(indsCharListsMultiTree1);
		entropyDiversity1.add(diversityValue1);
		//========================end================================

		//2021.9.20 save the number of inds in each subpop
		numIndsSubpop0.add(this.population.subpops[0].individuals.length);
		numIndsSubpop1.add(this.population.subpops[1].individuals.length);

		//2021.9.20 record the average fitness in the pop and average fitness value
		//=====================================start==============================
		ArrayList<Double> averageFitnessCurrentGenSubpop0 = new ArrayList<>();
		for (Individual ind : this.population.subpops[0].individuals) {
			if (ind.fitness.fitness() != Double.MAX_VALUE) {
				averageFitnessCurrentGenSubpop0.add(ind.fitness.fitness());
			}
		}
		averageFitnessGensSubpop0.add(averageFitnessCurrentGenSubpop0.stream().mapToDouble(i -> i).average().orElse(0));

		ArrayList<Double> averageFitnessCurrentGenSubpop1 = new ArrayList<>();
		for (Individual ind : this.population.subpops[1].individuals) {
			if (ind.fitness.fitness() != Double.MAX_VALUE) {
				averageFitnessCurrentGenSubpop1.add(ind.fitness.fitness());
			}
		}
		averageFitnessGensSubpop1.add(averageFitnessCurrentGenSubpop1.stream().mapToDouble(i -> i).average().orElse(0));
		//==========================end====================================

	    statistics.postEvaluationStatistics(this);

	    // SHOULD WE QUIT?
	    if (evaluator.runComplete(this) && quitOnRunComplete)
	        {
	        output.message("Found Ideal Individual");
	        return R_SUCCESS;
	        }
	    // SHOULD WE QUIT?
	    if (generation == numGenerations-1)
	        {
				generation++; // in this way, the last generation value will be printed properly.  fzhang 28.3.2018
				writeAverageFitnessGensToFile(averageFitnessGensSubpop0, averageFitnessGensSubpop1);
				writeNumberIndsSubpopGensToFile(numIndsSubpop0, numIndsSubpop1);
				writeBestFitnessSubpopsGensToFile(bestFitnessSubpop0,bestFitnessSubpop1);
				writeEntropyDiversityTasks(this, entropyDiversity0, entropyDiversity1);
				return R_FAILURE;
	        }

	    // PRE-BREEDING EXCHANGING
	    statistics.prePreBreedingExchangeStatistics(this);
	    population = exchanger.preBreedingExchangePopulation(this);  /** Simply returns state.population. */
	    statistics.postPreBreedingExchangeStatistics(this);

	    String exchangerWantsToShutdown = exchanger.runComplete(this);  /** Always returns null */
	    if (exchangerWantsToShutdown!=null)
	        {
	        output.message(exchangerWantsToShutdown);
	        /*
	         * Don't really know what to return here.  The only place I could
	         * find where runComplete ever returns non-null is
	         * IslandExchange.  However, that can return non-null whether or
	         * not the ideal individual was found (for example, if there was
	         * a communication error with the server).
	         *
	         * Since the original version of this code didn't care, and the
	         * result was initialized to R_SUCCESS before the while loop, I'm
	         * just going to return R_SUCCESS here.
	         */

	        return R_SUCCESS;
	        }

	    // BREEDING
	    statistics.preBreedingStatistics(this);

	    population = breeder.breedPopulation(this); //!!!!!!   return newpop;  if it is NSGA-II, the population here is 2N

	    // POST-BREEDING EXCHANGING
	    statistics.postBreedingStatistics(this);   //position 1  here, a new pop has been generated.

	    // POST-BREEDING EXCHANGING
	    statistics.prePostBreedingExchangeStatistics(this);
	    population = exchanger.postBreedingExchangePopulation(this);   /** Simply returns state.population. */
	    statistics.postPostBreedingExchangeStatistics(this);  //position 2

	    // Generate new instances if needed
		RuleOptimizationProblem problem = (RuleOptimizationProblem)evaluator.p_problem;
	    if (problem.getEvaluationModel().isRotatable()) {
			problem.rotateEvaluationModel();
		}

	    // INCREMENT GENERATION AND CHECKPOINT
	    generation++;
	    if (checkpoint && generation%checkpointModulo == 0)
	        {
	        output.message("Checkpointing");
	        statistics.preCheckpointStatistics(this);
	        Checkpoint.setCheckpoint(this);
	        statistics.postCheckpointStatistics(this);
	        }

	    return R_NOTDONE;
	}

	private void setupTerminals() {
        Parameter p;

        //Need to know how many populations we're expecting here, as will need
        //one terminal set per population
        int numSubPops = parameters.getInt(new Parameter("pop.subpops"),null);

        if (numSubPops == 1) {
            p = new Parameter(P_TERMINALS_FROM);

            terminalsFrom = new String[]{parameters.getStringWithDefault(p,
                    null, "relative")};

            p = new Parameter(P_INCLUDE_ERC);
            //includeErc seems like does not have influence.
            includeErc = new boolean[]{parameters.getBoolean(p, null, false)};
            initTerminalSet();
        } else if (numSubPops == 2) {
            terminalsFrom = new String[numSubPops];
            includeErc = new boolean[numSubPops];
            int subPopNum = 0;

            p = new Parameter(P_TERMINALS_FROM + "." + subPopNum);
            String subPop1TerminalSet = parameters.getStringWithDefault(p,
                    null, null);
            if (subPop1TerminalSet == null) {
                //might have provided other value by mistake, we should check for this
                p = new Parameter(P_TERMINALS_FROM);
                subPop1TerminalSet = parameters.getStringWithDefault(p,
                        null, "relative");
                output.warning("No terminal set for subpopulation 1 specified - using "+subPop1TerminalSet+".");

            }
            terminalsFrom[subPopNum] = subPop1TerminalSet;

            subPopNum++;
            p = new Parameter(P_TERMINALS_FROM + "." + subPopNum);
            String subPop2TerminalSet = parameters.getStringWithDefault(p,
                    null, null);
            if (subPop2TerminalSet == null) {
                //use whatever we settled on for first population
                subPop2TerminalSet = subPop1TerminalSet;
                output.warning("No terminal set for subpopulation 2 specified - using terminal set for subpopulation 1.");
            }
            terminalsFrom[subPopNum] = subPop2TerminalSet;
            //TODO: Add support for erc - will be false by default

            initTerminalSet(); //right
        }
    }

	//2021.9.20 write the average fitness value at each generation
	public static void writeAverageFitnessGensToFile(ArrayList<Double> averageFitnessGensSubpop0,ArrayList<Double> averageFitnessGensSubpop1) {
		File averageFitnessGensFile = new File(out_dir + "/job." + jobSeed + ".averageFitnessGens.csv"); // jobSeed = 0
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(averageFitnessGensFile));
			writer.write("Gen,averageFitnessSubpop0,averageFitnessSubpop1");
			writer.newLine();
			for (int i = 0; i < averageFitnessGensSubpop0.size(); i++) { //every two into one generation
				//writer.newLine();
				writer.write(i + "," + averageFitnessGensSubpop0.get(i) + "," + averageFitnessGensSubpop1.get(i) + "\n");
			}
//			writer.write(numGenerations -1 + ", " + 0 + "\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeNumberIndsSubpopGensToFile(ArrayList<Integer> numIndsSubpop0,ArrayList<Integer> numIndsSubpop1) {
		File numIndsSubpopGensFile = new File(out_dir + "/job." + jobSeed + ".numIndsSubpopGens.csv"); // jobSeed = 0
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(numIndsSubpopGensFile));
			writer.write("Gen,numIndsSubpop0,numIndsSubpop1");
			writer.newLine();
			for (int i = 0; i < numIndsSubpop0.size(); i++) { //every two into one generation
				//writer.newLine();
				writer.write(i + "," + numIndsSubpop0.get(i) + "," + numIndsSubpop1.get(i) + "\n");
			}
//			writer.write(numGenerations -1 + ", " + 0 + "\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static void writeBestFitnessSubpopsGensToFile(ArrayList<Double> bestFitnessSubpop0,ArrayList<Double> bestFitnessSubpop1) {
		File bestFitnessSubpopGensFile = new File(out_dir + "/job." + jobSeed + ".bestFitnessSubpopGens.csv"); // jobSeed = 0
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(bestFitnessSubpopGensFile));
			writer.write("Gen,bestFitnessSubpop0,bestFitnessSubpop1");
			writer.newLine();
			for (int i = 0; i < bestFitnessSubpop0.size(); i++) { //every two into one generation
				//writer.newLine();
				writer.write(i + "," + bestFitnessSubpop0.get(i) + "," + bestFitnessSubpop1.get(i) + "\n");
			}
//			writer.write(numGenerations -1 + ", " + 0 + "\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//2021.9.22 save the diversity of inds for each task at each generation
	public void writeEntropyDiversityTasks(EvolutionState state, ArrayList<Double> entropyDiversity0, ArrayList<Double> entropyDiversity1) {
		Parameter p;
		// Get the job seed.
		p = new Parameter("seed").push("" + 0);
		jobSeed = state.parameters.getLongWithDefault(p, null, 0);
		File indSizeForTask = new File(out_dir + "/job." + jobSeed + ".diversityTasks.csv");

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(indSizeForTask));
			writer.write("generation,diversityTask0,diversityTask1");
			writer.newLine();
			for (int i = 0; i < entropyDiversity0.size(); i++) {
				writer.write(i + "," + entropyDiversity0.get(i) + "," + entropyDiversity1.get(i));
				writer.newLine();
			}
//            writer.write(state.generation-1 + "," + 0 + "," + 0);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
