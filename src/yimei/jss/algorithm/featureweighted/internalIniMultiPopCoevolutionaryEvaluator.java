package yimei.jss.algorithm.featureweighted;

import ec.EvolutionState;
import ec.Individual;
import ec.coevolve.GroupedProblemForm;
import ec.coevolve.MultiPopCoevolutionaryEvaluator;
import ec.gp.GPNode;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Parameter;
import yimei.jss.gp.GPRuleEvolutionState;
import yimei.jss.helper.Weights;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class internalIniMultiPopCoevolutionaryEvaluator extends MultiPopCoevolutionaryEvaluator {
    public static final String P_PRE_GENERATIONS = "pre-generations";
    public static final String P_NUM_TOP_INDS = "num-topinds";
    public final static String P_GENERATIONS = "generations";

    private int preGenerations;
    protected long jobSeed;
    private int generations;

    // individuals to evaluate together
    Individual[] inds = null;
    // which individual should have its fitness updated as a result
    boolean[] updates = null;

    ArrayList<double[]> saveWeights = new ArrayList<>();

    public void evaluatePopulation(final EvolutionState state) {
        // determine who needs to be evaluated
        boolean[] preAssessFitness = new boolean[state.population.subpops.length];
        boolean[] postAssessFitness = new boolean[state.population.subpops.length];
        for (int i = 0; i < state.population.subpops.length; i++) {
            postAssessFitness[i] = shouldEvaluateSubpop(state, i, 0);
            //System.out.println(shouldEvaluateSubpop(state, i, 0));  //true  true
            preAssessFitness[i] = postAssessFitness[i] || (state.generation == 0);  // always prepare (set up trials) on generation 0
        }

        // do evaluation
        beforeCoevolutionaryEvaluation(state, state.population, (GroupedProblemForm) p_problem);

        ((GroupedProblemForm) p_problem).preprocessPopulation(state, state.population, preAssessFitness, false);
        performCoevolutionaryEvaluation(state, state.population, (GroupedProblemForm) p_problem);
        ((GroupedProblemForm) p_problem).postprocessPopulation(state, state.population, postAssessFitness, false); //set the objective and set the flags of evaluated individuals as true

        int topInds = state.parameters.getInt(new Parameter(P_NUM_TOP_INDS), null);
        preGenerations = state.parameters.getIntWithDefault(new Parameter(P_PRE_GENERATIONS), null, 0);

        //fzhang 2019.5.25 remove the bad individuals
        if ((state.generation+1)%preGenerations == 0 && state.generation != 0) {
            //calculateWeights(state);//after this, the populaiton is sorted by increment
            double[][] weights = new double[state.population.subpops.length][];
            Weights.calculateWeights(state,topInds,weights,saveWeights);
            ((GPRuleEvolutionState)state).setWeights(weights);

            for (int i = 0; i < state.population.subpops.length; i++) {
                removeBadInds(state, fracElites, i);
            }
        }

        generations = state.parameters.getIntWithDefault(new Parameter(P_GENERATIONS),null,-1);

        if (state.generation == generations - 1) {
            jobSeed = ((GPRuleEvolutionState)state).getJobSeed();
            saveWeights(state,jobSeed);
        }

        //change the eliteIndividuals
        afterCoevolutionaryEvaluation(state, state.population, (GroupedProblemForm) p_problem);
    }

    /**
     * Adapt the current population into two parts based on a changed
     * terminal set.
     * @param state the current evolution state (new terminal set).
     * @param fracElites the fraction of elite (directly copy).
     *                    fzhang 2019.5.28
     */
    public void removeBadInds(final EvolutionState state,
                              double fracElites,
                              int subPopNum) {
        inds = new Individual[state.population.subpops.length];
        updates = new boolean[state.population.subpops.length];

        Individual[] newPop = state.population.subpops[subPopNum].individuals;

        int numElites = (int)(fracElites * state.population.subpops[subPopNum].individuals.length); //elites: how many individuals to copy directly
        //int numAdapted = (int)(fracAdapted * state.population.subpops[subPopNum].individuals.length); //how many individuals to replace old terminals to 1


        for (int i = numElites; i < newPop.length; i++) {
            newPop[i] = state.population.subpops[subPopNum].species.newIndividual(state, 0);
            //read eliteIndividuals for coevolution
            for (int k = 0; k < eliteIndividuals[subPopNum].length; k++) { //2
                for (int ind1 = 0; ind1 < inds.length; ind1++) { //2
                    if (ind1 == subPopNum) {   //j = 0, 1  (ind j) ---> (0 0) or (1 1) that is to say, this is the subpopulation1
                        inds[ind1] = newPop[i]; //inds[0] = individual = state.population.subpops[0].individuals[0];
                        //the individuals to evaluate together
                        updates[ind1] = true;   // updates[0] = true    updates[1] = true   evaluate
                    }
                    else {  // this is subpopulation2
                        inds[ind1] = eliteIndividuals[ind1][k];   // (ind j) ---> (0 1) or (1 0)
                        updates[ind1] = false;  // do not evaluate
                    }
                }
            }

            //evaluate new individuals
            ((MultiObjectiveFitness)(newPop[i].fitness)).trials = new ArrayList();//this is always make trials.size == 1, actually useless
            ((GroupedProblemForm)(this.p_problem)).evaluate(state, inds
                    , updates // Should the fitness of individuals be updated? Here it says yes and yes.
                    , false
                    , new int[]{0, 1} // Which subpopulation to use? Here we have two subpops and we want to use them both so it should be 0 and 1
                    , 0);// real evaluation
            newPop[i].evaluated = true;
        }
    }

  /*  public void saveWeights(final EvolutionState state, long jobSeed) {
        //fzhang 2019.5.21 save the weight values
        File weightFile = new File("job." + jobSeed + ".weight.csv"); // jobSeed = 0
        GPNode[][] terminals = ((FreBadGPRuleEvolutionState)state).getTerminals();
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(weightFile));
            writer.write("Gen, sNIQ, sWIQ, sMWT, sPT, sNPT, sOWT, sWKR, sNOR, sW, sTIS, "
                    + "rNIQ, rWIQ, rMWT, rPT, rNPT, rOWT, rWKR, rNOR, rW, rTIS");
            writer.newLine();
            double[] zero = new double[terminals[0].length];
//			zero

            for (int i = 0; i < preGenerations; i++) {
                //writer.newLine();
                writer.write(i + ", " + Arrays.toString(zero).replaceAll("\\[|\\]", ""));
                writer.write(", " + Arrays.toString(zero).replaceAll("\\[|\\]", "") + "\n");
                //Returns a string representation of the contents of the specified array.
            }
            for (int i = 0; i < saveWeights.size(); i += 2) { //every two into one generation
                //writer.newLine();
                writer.write(i / 2 + preGenerations + ", " + Arrays.toString(saveWeights.get(i)).replaceAll("\\[|\\]", ""));
                writer.write(", " + Arrays.toString(saveWeights.get(i + 1)).replaceAll("\\[|\\]", "") + "\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/


  //fzhang 2019.6.1 only apply weighted features on interval generations
    public void saveWeights(final EvolutionState state, long jobSeed) {
        //fzhang 2019.5.21 save the weight values
        File weightFile = new File("job." + jobSeed + ".weight.csv"); // jobSeed = 0
        GPNode[][] terminals = ((FreBadGPRuleEvolutionState)state).getTerminals();
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(weightFile));
            writer.write("Gen, sNIQ, sWIQ, sMWT, sPT, sNPT, sOWT, sWKR, sNOR, sW, sTIS, "
                    + "rNIQ, rWIQ, rMWT, rPT, rNPT, rOWT, rWKR, rNOR, rW, rTIS");
            writer.newLine();
            double[] zero = new double[terminals[0].length];
//			zero

            int j = 0;
            for(int i = 0; i < generations; i++){
                if(((i+1) % preGenerations) == 0 && i != 0)
                {
                    writer.write(i + ", " + Arrays.toString(saveWeights.get(j)).replaceAll("\\[|\\]", ""));
                    writer.write(", " + Arrays.toString(saveWeights.get(j + 1)).replaceAll("\\[|\\]", "") + "\n");
                    j++;
                }else
                {
                    writer.write(i + ", " + Arrays.toString(zero).replaceAll("\\[|\\]", ""));
                    writer.write(", " + Arrays.toString(zero).replaceAll("\\[|\\]", "") + "\n");
                }
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
   }