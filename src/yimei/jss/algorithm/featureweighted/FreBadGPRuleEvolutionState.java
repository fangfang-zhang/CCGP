package yimei.jss.algorithm.featureweighted;

import java.util.ArrayList;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPNode;
import ec.util.Checkpoint;
import ec.util.Parameter;
import ec.util.RandomChoice;
import yimei.jss.feature.FeatureIgnorable;
import yimei.jss.feature.FeatureUtil;
import yimei.jss.feature.ignore.Ignorer;
import yimei.jss.gp.GPRuleEvolutionState;
import yimei.jss.gp.TerminalsChangable;
import yimei.jss.ruleoptimisation.RuleOptimizationProblem;

/**
 * Created by fzhang on 22.5.2019.
 * 1. set the weights of featurs according to the frequency of features (based on top-k individuals)
 * 2. replace the bad individuals by generating new individuals with weihhted features
 */
public class FreBadGPRuleEvolutionState extends GPRuleEvolutionState implements TerminalsChangable, FeatureIgnorable {

	public static final String P_IGNORER = "ignorer";
	public static final String P_PRE_GENERATIONS = "pre-generations";
	public static final String P_POP_ADAPT_FRAC_ELITES = "pop-adapt-frac-elites";
	public static final String P_POP_ADAPT_FRAC_ADAPTED = "pop-adapt-frac-adapted";
	public static final String P_DO_ADAPT = "feature-selection-adapt-population";
	/*public static final String P_NUM_TOP_INDS = "num-topinds";*/

	private Ignorer ignorer;
	private int preGenerations;
	private double fracElites;
	private double fracAdapted;
	private boolean doAdapt;
	/*private int topInds;
*/
	@Override
	public Ignorer getIgnorer() {
		return ignorer;
	}

	@Override
	public void setIgnorer(Ignorer ignorer) {
		this.ignorer = ignorer;
	}

	@Override
	public void setup(EvolutionState state, Parameter base) {
		super.setup(state, base);

		ignorer = (Ignorer) (state.parameters.getInstanceForParameter(
				new Parameter(P_IGNORER), null, Ignorer.class)); //ignorer = yimei.jss.feature.ignore.SimpleIgnorer
		preGenerations = state.parameters.getIntWithDefault(
				new Parameter(P_PRE_GENERATIONS), null, -1);  //50
		fracElites = state.parameters.getDoubleWithDefault(
				new Parameter(P_POP_ADAPT_FRAC_ELITES), null, 0.0); //0.0
		fracAdapted = state.parameters.getDoubleWithDefault(
				new Parameter(P_POP_ADAPT_FRAC_ADAPTED), null, 1.0); //0.1
		doAdapt = state.parameters.getBoolean(new Parameter(P_DO_ADAPT),
				null, true);
		/*topInds = state.parameters.getInt(new Parameter(P_NUM_TOP_INDS), null);*/
	}


	private ArrayList<Individual> lastGenEliteIndividual = new ArrayList<>();

	@Override
	public GPNode pickTerminalRandom(int subPopNum) {
		int index = -1; //random[0].nextInt(terminals[subPopNum].lenth)
		//1. if no weights is set for features, then we will choose features uniformly.
		if (weights == null || weights[subPopNum] == null) {
			//output.warning("weights are null");
			index = random[0].nextInt(terminals[subPopNum].length);
		} else //2. otherwise, choose features based on their weighting power
		{
			//System.out.println(subPopNum);
			index = RandomChoice.pickFromDistribution(weights[subPopNum], random[0].nextDouble());
		}
		//System.out.println("The index of chosen features: " + index);
		return terminals[subPopNum][index];
	}

	@Override
	public int evolve() {
		output.message("Generation " + generation);

		// EVALUATION
		statistics.preEvaluationStatistics(this);
		evaluator.evaluatePopulation(this); //Feature selection, firstly, evaluate population as usual; then clear population

		//clearing, niching  MultiPopCoevolutionaryEvaluator
		statistics.postEvaluationStatistics(this);

		// SHOULD WE QUIT?
		if (evaluator.runComplete(this) && quitOnRunComplete) {
			output.message("Found Ideal Individual");
			return R_SUCCESS;
		}

		// SHOULD WE QUIT?
		if (generation == numGenerations - 1) {
			generation++;
			return R_FAILURE;
		}

		// PRE-BREEDING EXCHANGING
		statistics.prePreBreedingExchangeStatistics(this);
		population = exchanger.preBreedingExchangePopulation(this);
		statistics.postPreBreedingExchangeStatistics(this);

		String exchangerWantsToShutdown = exchanger.runComplete(this);
		if (exchangerWantsToShutdown != null) {
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

		population = breeder.breedPopulation(this);

		//2019.6.1 only applied at interval generation
		weights = null;

		// POST-BREEDING EXCHANGING
		statistics.postBreedingStatistics(this);

		// POST-BREEDING EXCHANGING
		statistics.prePostBreedingExchangeStatistics(this);
		population = exchanger.postBreedingExchangePopulation(this);
		statistics.postPostBreedingExchangeStatistics(this);

		// Generate new instances if needed
		RuleOptimizationProblem problem = (RuleOptimizationProblem) evaluator.p_problem;
		if (problem.getEvaluationModel().isRotatable()) {
			problem.rotateEvaluationModel();
		}

		// INCREMENT GENERATION AND CHECKPOINT
		generation++;
		if (checkpoint && generation % checkpointModulo == 0) {
			output.message("Checkpointing");
			statistics.preCheckpointStatistics(this);
			Checkpoint.setCheckpoint(this);
			statistics.postCheckpointStatistics(this);
		}

		return R_NOTDONE;
	}

	@Override
	public void adaptPopulation(int subPopNum) {
		FeatureUtil.adaptPopulationThreeParts(this, fracElites, fracAdapted, subPopNum);
	}
}

