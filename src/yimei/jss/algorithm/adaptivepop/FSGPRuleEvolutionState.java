package yimei.jss.algorithm.adaptivepop;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.util.Checkpoint;
import ec.util.Parameter;
import yimei.jss.feature.FeatureIgnorable;
import yimei.jss.feature.FeatureUtil;
import yimei.jss.feature.ignore.Ignorer;
import yimei.jss.gp.GPRuleEvolutionState;
import yimei.jss.gp.TerminalsChangable;
import yimei.jss.helper.PopulationUtils;
import yimei.jss.ruleoptimisation.RuleOptimizationProblem;
import yimei.jss.surrogate.Surrogate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by YiMei on 5/10/16.
 */
public class FSGPRuleEvolutionState extends GPRuleEvolutionState implements TerminalsChangable, FeatureIgnorable {

    public static final String P_IGNORER = "ignorer";
    public static final String P_PRE_GENERATIONS = "pre-generations";
    public static final String P_POP_ADAPT_FRAC_ELITES = "pop-adapt-frac-elites";
    public static final String P_POP_ADAPT_FRAC_ADAPTED = "pop-adapt-frac-adapted";
    public static final String P_DO_ADAPT = "feature-selection-adapt-population";

    private Ignorer ignorer;
    private int preGenerations;
    private double fracElites;
    private double fracAdapted;
    private boolean doAdapt;

    private double fitUB = Double.NEGATIVE_INFINITY;
    private double fitLB = Double.POSITIVE_INFINITY;

    ArrayList<Double> saveOldFitSubPop0 = new ArrayList<>();
    ArrayList<Double> saveOldFitSubPop1 = new ArrayList<>();
    ArrayList<Double> saveFitDistanceSubPop0 = new ArrayList<>();
    ArrayList<Double> saveFitDistanceSubPop1 = new ArrayList<>();

    ArrayList<Double> PheDistanceSubPop0 = new ArrayList<>();
    ArrayList<Double> PheDistanceSubPop1 = new ArrayList<>();

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

        ignorer = (Ignorer)(state.parameters.getInstanceForParameter(
                new Parameter(P_IGNORER), null, Ignorer.class)); //ignorer = yimei.jss.feature.ignore.SimpleIgnorer
        preGenerations = state.parameters.getIntWithDefault(
                new Parameter(P_PRE_GENERATIONS), null, -1);  //50
        fracElites = state.parameters.getDoubleWithDefault(
                new Parameter(P_POP_ADAPT_FRAC_ELITES), null, 0.0); //0.0
        fracAdapted = state.parameters.getDoubleWithDefault(
                new Parameter(P_POP_ADAPT_FRAC_ADAPTED), null, 1.0); //0.1
        doAdapt = state.parameters.getBoolean(new Parameter(P_DO_ADAPT),  
                null, true);
    }

    @Override
    public int evolve() {
        //if (generation > 0)
            output.message("Generation " + generation);
        //fzhang   when get the predefined preGeneration, do this.  That is to say, before this phase, do other things (feature selection).
        //this part only can be used once.
        if (generation == preGenerations) {

        	//fzhang 10.7.2018  all the generation use original evaluator, except for in the pre-generation
        	 //((MultiPopCoevolutionaryEvaluator)evaluator).setClear(true);

        	//debug: in---different from classical version
            evaluator.evaluatePopulation(this); //evaluate each individual, and then clear poor individuals
            PopulationUtils.sort(population); //old population

            //fzhang 2019.6.5 in order to measure the consistence of pgenotype strategy
            saveOldFitSubPop0 = ((multiPopCoevolutionaryClearingEvaluator)(this.evaluator)).getSaveOldFitSubPop0();
            saveOldFitSubPop1 = ((multiPopCoevolutionaryClearingEvaluator)(this.evaluator)).getSaveOldFitSubPop1();
            
           for (int i = 0; i < population.subpops.length; i++) { //before here is ++i
                Individual[] individuals = population.subpops[i].individuals;

                List<GPIndividual> selIndis =
                        FeatureUtil.selectDiverseIndis(this, individuals, i, 10); // i: which subpopulation  30: the number of individuals in the diverse set.
                //change 30->50, not difference
                //individuals archive: the archive from which the set will be chosen.
                fitUB = selIndis.get(0).fitness.fitness();  //the upper bound of fitness   //the best one, the smallest fitness in the individuals  1475.0335218025866
                fitLB = 1 - fitUB;  //the lower bound of fitness  -1474.0335218025866
                
               /* System.out.println("fitUB "+fitUB);
                System.out.println("fitLB "+ fitLB);*/ //fitUB 0.5503209083350227 + fitLB 0.4496790916649773, fitUB 0.5464790771699619 + fitLB 0.453520922830038

                //fitUB 1420.8931806818873   fitLB -1419.8931806818873   fitUB 1444.3724935270438    fitLB -1443.3724935270438
                //====================feture selection, after this we can get features=======================
                GPNode[] selFeatures =
                        FeatureUtil.featureSelection(this, selIndis,
                                FeatureUtil.ruleTypes[i], fitUB, fitLB);
  
                //if we do not want to use doAdapt and only use this for mutation, we only do it in this way
                setTerminals(selFeatures,i);
                
                if (doAdapt) {
	                adaptPopulation(i); //three part
                   }

               //FeatureUtil.savePheDistance(this);

               if(i == 0)
                   PheDistanceSubPop0 = FeatureUtil.getPheDistance(i);
               else
                   PheDistanceSubPop1 = FeatureUtil.getPheDistance(i);
                }

//            //for niching
            ((multiPopCoevolutionaryClearingEvaluator)evaluator).setClear(false);
             //((ClearingEvaluator)evaluator).setClear(false); //this is for simpleGP
             ((Surrogate)((RuleOptimizationProblem)evaluator.p_problem)
                     .getEvaluationModel()).useOriginal(); //use original simulation, 5000 jobs...   surrogate only use 500
        }
        
        // EVALUATION
        statistics.preEvaluationStatistics(this); //after adaptive population, here still the old population, maybe because we change the evalutor and model
        evaluator.evaluatePopulation(this); //Feature selection, firstly, evaluate population as usual; then clear population
        //clearing, niching  MultiPopCoevolutionaryEvaluator
        //PopulationUtils.sort(population); //adapted population  maybe in order to check others

        //before the populaiton is sorted
        //check which individual is a good individual
        /*int index = PopulationUtils.getIndexOfbestInds(this.population,0);
        System.out.println(index);*/

        //fzhang 2019.6.5 save the fitness information
        //===================================start===================================================
        if(generation == preGenerations) {
            for (int i = 0; i < (int) (fracElites * this.population.subpops[0].individuals.length); i++) {
                saveFitDistanceSubPop0.add(this.population.subpops[0].individuals[i].fitness.fitness() - saveOldFitSubPop0.get(i));
                saveFitDistanceSubPop1.add(this.population.subpops[1].individuals[i].fitness.fitness() - saveOldFitSubPop1.get(i));
            }
            savePheFitDistance();
        }
    //================================================end===============================================

        statistics.postEvaluationStatistics(this); //here, the best individual is print out to out.stat file

        // SHOULD WE QUIT?
        if (evaluator.runComplete(this) && quitOnRunComplete)
        {
            output.message("Found Ideal Individual");
            return R_SUCCESS;
        }

        // SHOULD WE QUIT?
        if (generation == numGenerations-1)
        {
        	generation++;
            return R_FAILURE;
        }

        // PRE-BREEDING EXCHANGING
        statistics.prePreBreedingExchangeStatistics(this);
        population = exchanger.preBreedingExchangePopulation(this);  //get current population
        statistics.postPreBreedingExchangeStatistics(this);

        String exchangerWantsToShutdown = exchanger.runComplete(this);
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

        population = breeder.breedPopulation(this);

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
        if (checkpoint && generation%checkpointModulo == 0)
        {
            output.message("Checkpointing");
            statistics.preCheckpointStatistics(this);
            Checkpoint.setCheckpoint(this);
            statistics.postCheckpointStatistics(this);
        }

        return R_NOTDONE;
    }

    @Override
    public void adaptPopulation(int subPopNum) {
        //FeatureUtil.adaptPopulationBasedOnPhenotype(this,fracElites,subPopNum);
        //FeatureUtil.adaptPopulationBasedOnPhenotypeWhole(this,fracElites,subPopNum);//after get the whole generated population, evaluate them together
        FeatureUtil.adaptPopulationBasedOnPhenotypeGeneticOperator(this,fracElites,subPopNum); //mimic top k% individuals and generate others based on these top k% with generate operators
    }

    public void savePheFitDistance(){
        //fzhang 2019.5.21 save the weight values
        long jobSeed = this.getJobSeed();
        File pheFitDistanceFile = new File("job." + jobSeed + ".pheFitDistance.csv"); // jobSeed = 0
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(pheFitDistanceFile));
            writer.write("PheDistanceSubPop0, FitDistanceSubPop0, PheDistanceSubPop1, FitDistanceSubPop1");
            writer.newLine();

            for (int i = 0; i < saveFitDistanceSubPop0.size(); i++) { //every two into one generation
                //writer.newLine();
                writer.write(PheDistanceSubPop0.get(i) + ", " + saveFitDistanceSubPop0.get(i) + ", ");
                writer.write(PheDistanceSubPop1.get(i) + ", " + saveFitDistanceSubPop1.get(i) + "\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
