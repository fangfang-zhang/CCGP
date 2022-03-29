package yimei.jss.algorithm.elitesbasedpop;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.coevolve.GroupedProblemForm;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Parameter;
import yimei.jss.helper.PopulationUtils;
import yimei.jss.niching.Clearing;
import yimei.jss.niching.MultiPopCoevolutionaryClearingEvaluator;
import yimei.jss.ruleoptimisation.RuleOptimizationProblem;
import yimei.jss.surrogate.Surrogate;

import java.util.ArrayList;

//fzhang 2019.6.14 this is for calculating the fitness distance, no effect for evaluate the individuals
public class multiPopCoevolutionaryClearingEvaluator extends MultiPopCoevolutionaryClearingEvaluator {
    public static final String P_PRE_GENERATIONS = "pre-generations";
    public static final String P_NUM_TOP_INDS = "num-topinds";
    public final static String P_GENERATIONS = "generations";
    public static final String P_POP_ADAPT_FRAC_ELITES = "pop-adapt-frac-elites";

    private int preGenerations;
    protected long jobSeed;
    Boolean flag = false;

    // individuals to evaluate together
    Individual[] inds = null;
    // which individual should have its fitness updated as a result
    boolean[] updates = null;

    ArrayList<Double> saveOldFitSubPop0 = new ArrayList<>();
    ArrayList<Double> saveOldFitSubPop1 = new ArrayList<>();

    @Override
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

        ////fzhang 2019.6.6 in order to get the real fitnesses of top 20% individuals in old population
        //==============================start===============================
        preGenerations = state.parameters.getIntWithDefault(new Parameter(P_PRE_GENERATIONS), null, 0);
        if(state.generation == preGenerations && flag == false){
            flag = true;
            PopulationUtils.sort(state.population); //old population
            ((MultiPopCoevolutionaryClearingEvaluator)state.evaluator).setClear(false);
            //((ClearingEvaluator)evaluator).setClear(false); //this is for simpleGP
            ((Surrogate)((RuleOptimizationProblem)state.evaluator.p_problem)
                    .getEvaluationModel()).useOriginal(); //use original simulation, 5000 jobs...   surrogate only use 500

            double fracElites = state.parameters.getDoubleWithDefault(
                    new Parameter(P_POP_ADAPT_FRAC_ELITES), null, 1.0); //0.1
            for (int i = 0; i < state.population.subpops.length; i++) {
                calculateOldFitness(state, fracElites, i);
            }

            ((MultiPopCoevolutionaryClearingEvaluator)state.evaluator).setClear(true);
            //((ClearingEvaluator)evaluator).setClear(false); //this is for simpleGP
            ((Surrogate)((RuleOptimizationProblem)state.evaluator.p_problem)
                    .getEvaluationModel()).useSurrogate(); //use original simulation, 5000 jobs...   surrogate only use 500
            evaluatePopulation(state);
        }
        //==========================================end=======================================

        //change the eliteIndividuals
        afterCoevolutionaryEvaluation(state, state.population, (GroupedProblemForm) p_problem);

        if (clear) {
            //  System.out.println("MultiPopCoevolution");
            Clearing.clearPopulation(state, radius, capacity,
                    phenoCharacterisation);
        }
    }


    //fzhang 2019.6.16 evaluate population directly
    public void evaluatePopulation(final EvolutionState state, Population pop) {
        // determine who needs to be evaluated
        boolean[] preAssessFitness = new boolean[pop.subpops.length];
        boolean[] postAssessFitness = new boolean[pop.subpops.length];
        for (int i = 0; i < pop.subpops.length; i++) {
            postAssessFitness[i] = shouldEvaluateSubpop(state, i, 0);
            //System.out.println(shouldEvaluateSubpop(state, i, 0));  //true  true
            preAssessFitness[i] = postAssessFitness[i] || (state.generation == 0);  // always prepare (set up trials) on generation 0
        }

        // do evaluation
        beforeCoevolutionaryEvaluation(state, pop, (GroupedProblemForm) p_problem);

        ((GroupedProblemForm) p_problem).preprocessPopulation(state, pop, preAssessFitness, false);
        performCoevolutionaryEvaluation(state, pop, (GroupedProblemForm) p_problem);
        ((GroupedProblemForm) p_problem).postprocessPopulation(state, pop, postAssessFitness, false); //set the objective and set the flags of evaluated individuals as true

        ////fzhang 2019.6.6 in order to get the real fitnesses of top 20% individuals in old population
        //==============================start===============================
        preGenerations = state.parameters.getIntWithDefault(new Parameter(P_PRE_GENERATIONS), null, 0);
        if(state.generation == preGenerations && flag == false){
            flag = true;
            PopulationUtils.sort(pop); //old population
            ((MultiPopCoevolutionaryClearingEvaluator)state.evaluator).setClear(false);
            //((ClearingEvaluator)evaluator).setClear(false); //this is for simpleGP
            ((Surrogate)((RuleOptimizationProblem)state.evaluator.p_problem)
                    .getEvaluationModel()).useOriginal(); //use original simulation, 5000 jobs...   surrogate only use 500

            double fracElites = state.parameters.getDoubleWithDefault(
                    new Parameter(P_POP_ADAPT_FRAC_ELITES), null, 1.0); //0.1
            for (int i = 0; i < pop.subpops.length; i++) {
                calculateOldFitness(state, fracElites, i);
            }

            ((MultiPopCoevolutionaryClearingEvaluator)state.evaluator).setClear(true);
            //((ClearingEvaluator)evaluator).setClear(false); //this is for simpleGP
            ((Surrogate)((RuleOptimizationProblem)state.evaluator.p_problem)
                    .getEvaluationModel()).useSurrogate(); //use original simulation, 5000 jobs...   surrogate only use 500
            evaluatePopulation(state);
        }
        //==========================================end=======================================

        //change the eliteIndividuals
        afterCoevolutionaryEvaluation(state, pop, (GroupedProblemForm) p_problem);

        if (clear) {
            //  System.out.println("MultiPopCoevolution");
            Clearing.clearPopulation(state, radius, capacity,
                    phenoCharacterisation);
        }
    }

    public void calculateOldFitness(final EvolutionState state,
                              double fracElites,
                              int subPopNum) {
        inds = new Individual[state.population.subpops.length];
        updates = new boolean[state.population.subpops.length];

        Individual[] newPop = state.population.subpops[subPopNum].individuals;
        int numElites = (int)(fracElites * state.population.subpops[subPopNum].individuals.length); //elites: how many individuals to copy directly

        for (int i = 0; i < numElites; i++) {
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
            //newPop[i].evaluated = true;
            if(subPopNum == 0)
               saveOldFitSubPop0.add(newPop[i].fitness.fitness());
            else
               saveOldFitSubPop1.add(newPop[i].fitness.fitness());
        }
    }

    public ArrayList<Double> getSaveOldFitSubPop0(){
        return saveOldFitSubPop0;
    }

    public ArrayList<Double> getSaveOldFitSubPop1(){
        return saveOldFitSubPop1;
    }
}