package yimei.jss.algorithm.featureselection;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.util.Checkpoint;
import ec.util.Parameter;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import yimei.jss.algorithm.onlyselectedfeatures.KMeansPlusPlusClustererTemp;
import yimei.jss.algorithm.onlyselectedfeatures.Point;
import yimei.jss.feature.FeatureUtil;
import yimei.jss.helper.PopulationUtils;
import yimei.jss.niching.MultiPopCoevolutionaryClearingEvaluator;
import yimei.jss.surrogate.Surrogate;
import yimei.jss.feature.FeatureIgnorable;
import yimei.jss.feature.ignore.Ignorer;
import yimei.jss.gp.GPRuleEvolutionState;
import yimei.jss.gp.TerminalsChangable;
import yimei.jss.ruleoptimisation.RuleOptimizationProblem;

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
    public static final String P_NUMCLUSTERS = "numClusters";

    private Ignorer ignorer;
    private int preGenerations;
    private double fracElites;
    private double fracAdapted;
    private boolean doAdapt;
    private int numClusters;

    private double fitUB = Double.NEGATIVE_INFINITY;
    private double fitLB = Double.POSITIVE_INFINITY;

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
        numClusters = state.parameters.getIntWithDefault(new Parameter(P_NUMCLUSTERS), null, 1);
    }

    @Override
    public int evolve() {
        //if (generation > 0)
            output.message("Generation " + generation);
        //fzhang   when get the predefined preGeneration, do this.  That is to say, before this phase, do other things (feature selection).
        //this part only can be used once.
        if (generation == preGenerations) {
            evaluator.evaluatePopulation(this); //evaluate each individual, and then clear poor individuals
            PopulationUtils.sort(population); //old population

           for (int i = 0; i < population.subpops.length; i++) { //before here is ++i
                Individual[] individuals = population.subpops[i].individuals;

                List<GPIndividual> selIndis =
                        FeatureUtil.selectDiverseIndis(this, individuals, i, 10); // i: which subpopulation  30: the number of individuals in the diverse set.

               //fzhang 2019.6.26 calculate voting weights
               //=======================start====================
              /* List<GPIndividual> selIndis = saveBestCenInds(individuals, numClusters);
               double maxFit = selIndis.get(0).fitness.fitness();
               double minFit = selIndis.get(0).fitness.fitness();

               for(int ind = 1; ind < selIndis.size(); ind ++)
               {
                   if (selIndis.get(ind).fitness.fitness() < minFit){
                       minFit = selIndis.get(ind).fitness.fitness();
                   }

                   if(selIndis.get(ind).fitness.fitness() > maxFit){
                       maxFit = selIndis.get(ind).fitness.fitness();
                   }
               }

               fitLB = 1 / (1 + maxFit);
               fitUB = 1 / (1 + minFit);*/
               //========================end=======================

               //fzhang 2019.6.25 original normalisation method
               //===============================start=========================
                fitUB = selIndis.get(0).fitness.fitness();  //the upper bound of fitness   //the best one, the smallest fitness in the individuals  1475.0335218025866
                fitLB = 1 - fitUB;  //the lower bound of fitness  -1474.0335218025866
              //==========================end=============================

                //====================feture selection, after this we can get features=======================
                GPNode[] selFeatures =
                        FeatureUtil.featureSelection(this, selIndis,
                                FeatureUtil.ruleTypes[i], fitUB, fitLB);

                //1. get the baseline, in stage 2, still use the original features, should commment this
                //=====================start==================
               setTerminals(selFeatures,i);
               //actually, also skip this part
               if (doAdapt) {
                   adaptPopulation(i); //three part
               }
               //=========================end==================

               //2. only apply selected features in mutation in stage 2
               //3. adapt new population and also use selected features for mutation --- need set new features
               //==================start============
            /*   setTerminals(selFeatures,i);
               //2. skip this part
               //3. set the doAdapt to true
               if (doAdapt) {
                   adaptPopulation(i); //three part
               }*/
               //==============end==================
           }
            
            //for niching
            ((MultiPopCoevolutionaryClearingEvaluator)evaluator).setClear(false);
             //((ClearingEvaluator)evaluator).setClear(false); //this is for simpleGP
             ((Surrogate)((RuleOptimizationProblem)evaluator.p_problem)
                     .getEvaluationModel()).useOriginal(); //use original simulation, 5000 jobs...   surrogate only use 500                         		 
        }

        //System.out.println("evaluator: "+evaluator.toString());
       // System.out.println("Model: "+ ((Surrogate)((RuleOptimizationProblem)evaluator.p_problem)
        //        .getEvaluationModel()).toString());
        
        // EVALUATION
        statistics.preEvaluationStatistics(this);
        evaluator.evaluatePopulation(this); //Feature selection, firstly, evaluate population as usual; then clear population
        //clearing, niching  MultiPopCoevolutionaryEvaluator
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
        	generation++;
            return R_FAILURE;
        }

        // PRE-BREEDING EXCHANGING
        statistics.prePreBreedingExchangeStatistics(this);
        population = exchanger.preBreedingExchangePopulation(this);
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
        FeatureUtil.adaptPopulationThreeParts(this, fracElites, fracAdapted, subPopNum);
    }

    public List<GPIndividual> saveBestCenInds(Individual[] individuals, int k){
        //clustering
        KMeansPlusPlusClustererTemp<Point> kMeansPlusPlusClusterer = new KMeansPlusPlusClustererTemp<Point>(k);
        //create points for clustering
        List<Point> points = new ArrayList<Point>();
        for(Individual ind: individuals){
            double[] pos = new double[]{ind.fitness.fitness()};
            points.add(new Point(pos));
        }

        // now perform clustering
        List<CentroidCluster<Point>> centroids = kMeansPlusPlusClusterer.cluster(this, points);

        //find the smallest centroid
        CentroidCluster<Point> minCentroid = null;
        for(int c = 0; c < centroids.size(); c++){
            if(minCentroid == null ||
                    centroids.get(c).getCenter().getPoint()[0] < minCentroid.getCenter().getPoint()[0])
                minCentroid = centroids.get(c);
        }
        int minIndex = centroids.indexOf(minCentroid);

        List<GPIndividual> saveBestCenInds = new ArrayList<GPIndividual>();
        for(int index=0; index<points.size(); index++){
            Point point = points.get(index);
            int centroidIndex = kMeansPlusPlusClusterer.getNearestCluster(centroids, point);
            if (centroidIndex == minIndex)
                saveBestCenInds.add((GPIndividual) individuals[index]);
        }

        return saveBestCenInds;
    }
}
