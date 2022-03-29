package yimei.jss.algorithm.elbowSelectedFeatures;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.util.Checkpoint;
import ec.util.Parameter;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import yimei.jss.algorithm.onlyselectedfeatures.KMeansPlusPlusClustererTemp;
import yimei.jss.algorithm.onlyselectedfeatures.Point;
import yimei.jss.feature.FeatureIgnorable;
import yimei.jss.feature.FeatureUtil;
import yimei.jss.feature.ignore.Ignorer;
import yimei.jss.gp.GPRuleEvolutionState;
import yimei.jss.gp.TerminalsChangable;
import yimei.jss.helper.PopulationUtils;
import yimei.jss.niching.ClearingEvaluator;
import yimei.jss.niching.MultiPopCoevolutionaryClearingEvaluator;
import yimei.jss.niching.PhenoCharacterisation;
import yimei.jss.rule.RuleType;
import yimei.jss.rule.operation.evolved.GPRule;
import yimei.jss.ruleoptimisation.RuleOptimizationProblem;
import yimei.jss.surrogate.Surrogate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static yimei.jss.feature.FeatureUtil.ruleTypes;

/**
 * Created by YiMei on 5/10/16.
 */
public class FSGPRuleEvolutionState extends GPRuleEvolutionState implements TerminalsChangable, FeatureIgnorable {

    public static final String P_IGNORER = "ignorer";
    public static final String P_PRE_GENERATIONS = "pre-generations";
    public static final String P_POP_ADAPT_FRAC_ELITES = "pop-adapt-frac-elites";
    public static final String P_POP_ADAPT_FRAC_ADAPTED = "pop-adapt-frac-adapted";
    public static final String P_DO_ADAPT = "feature-selection-adapt-population";
    public static final String P_NUMCLUSTERS1 = "numClusters1";
    public static final String P_NUMCLUSTERS2 = "numClusters2";

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

    ArrayList<Integer> numSelIndividual = new ArrayList<>();

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
        if (generation == preGenerations) {

            evaluator.evaluatePopulation(this); //evaluate each individual, and then clear poor individuals
            PopulationUtils.sort(population); //old population

           for (int subIndex = 0; subIndex < population.subpops.length; subIndex++) { //before here is ++i
                Individual[] individuals = population.subpops[subIndex].individuals;

             /*  System.out.println("fitness of individuals \n" + subIndex);
               for(int ind = 0; ind < individuals.length; ind++){
                   System.out.println(individuals[ind].fitness.fitness());
               }*/

                ArrayList<IndexPoint> points = new ArrayList<IndexPoint>();

                for(int indIndex = 0; indIndex < population.subpops[subIndex].individuals.length; indIndex++){
                    //System.out.println(individuals[indIndex].fitness.fitness());
                    if(individuals[indIndex].fitness.fitness() != Double.POSITIVE_INFINITY & individuals[indIndex].fitness.fitness() != Double.MAX_VALUE)
                       points.add(new IndexPoint(new double[]{indIndex,individuals[indIndex].fitness.fitness()}));
                }

              /* System.out.println("after delete inifinity fitness of individuals \n");
               for(int ind = 0; ind < points.size(); ind++){
                   System.out.println(points.get(ind).position[1]);
               }*/

               IndexPoint p1 = points.get(0);  //the min Point
               IndexPoint p2 = points.get(points.size() - 1); //the max point

               //calculate the line factors
               double a = p2.position[1] - p1.position[1];
               double b = p1.position[0] - p2.position[0];
               double c = p2.position[0]*p1.position[1] - p1.position[0]*p2.position[1];
              /* System.out.println(a*p1.position[0]+b*p1.position[1]+c);
               System.out.println(a*p2.position[0]+b*p2.position[1]+c);*/

              double maxDistance = 0.0;
              int kneePoint = -1;

              for(int idxPoints = 1; idxPoints < points.size(); idxPoints++){
                  IndexPoint p = points.get(idxPoints);
                  double distance = Math.abs(a*p.position[0]+b*p.position[1]+c);
                  if(distance > maxDistance){
                      maxDistance = distance;
                      kneePoint = idxPoints;
                  }
                }

              //System.out.println("index of knee point: " + kneePoint);

               Individual[] selIndis = new Individual[kneePoint+1];
               System.arraycopy(individuals, 0, selIndis, 0, kneePoint+1);

               numSelIndividual.add(selIndis.length);

                //====================feture selection, after this we can get features=======================
               Individual[] indsForFS = new Individual[10];
               System.arraycopy(individuals, 0, indsForFS, 0, 10);

               //fzhang 2019.7.7 still use top 10 for feature selection
               GPNode[] selFeatures =
                       FeatureUtil.featureSelection(this, indsForFS,
                               ruleTypes[subIndex], fitUB, fitLB);

            /*    GPNode[] selFeatures =
                        FeatureUtil.featureSelection(this, selIndis,
                                ruleTypes[subIndex], fitUB, fitLB);
  */
                //if we do not want to use doAdapt and only use this for mutation, we only do it in this way
                setTerminals(selFeatures,subIndex);
                if (doAdapt) {
	                //adaptPopulation(subIndex); //three part  original one --- already redefined in the method adaptPopulation(i) as shown below  --- mimic 20% inds and 80% randomly generated
                     //FeatureUtil.adaptPopulationThreeParts(this, fracElites, fracAdapted, subIndex);
                    //that means mimic selected individuals and randomly initialise others
                    adaptPopulationWithKneePoint(selIndis, subIndex); //the input is the selcted individuals, when we mimic the selected individuals, the process is the same as clustering
                    //FeatureUtil.adaptPopulationReplacedByOne(this, selIndis, subIndex);
                   }
                }

           saveNumSelectedIndstoFile();
//            //for niching
            //((multiPopCoevolutionaryClearingEvaluator)evaluator).setClear(false); //for calculate the phenotype distance
            ((MultiPopCoevolutionaryClearingEvaluator)evaluator).setClear(false);

             //((ClearingEvaluator)evaluator).setClear(false); //this is for simpleGP
             ((Surrogate)((RuleOptimizationProblem)evaluator.p_problem)
                     .getEvaluationModel()).useOriginal(); //use original simulation, 5000 jobs...   surrogate only use 500
        }
        
        // EVALUATION
        statistics.preEvaluationStatistics(this); //after adaptive population, here still the old population, maybe because we change the evalutor and model
        evaluator.evaluatePopulation(this); //Feature selection, firstly, evaluate population as usual; then clear population

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
        population = exchanger.preBreedingExchangePopulation(this);
        statistics.postPreBreedingExchangeStatistics(this);

        String exchangerWantsToShutdown = exchanger.runComplete(this);
        if (exchangerWantsToShutdown!=null)
        {
            output.message(exchangerWantsToShutdown);
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
        FeatureUtil.adaptPopulationBasedOnPhenotypeWhole(this,fracElites,subPopNum);//after get the whole generated population, evaluate them together
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

   public List<GPIndividual> saveBestCenInds(Individual[] individuals, int k){
       //clustering
       KMeansPlusPlusClustererTemp<Point> kMeansPlusPlusClusterer = new KMeansPlusPlusClustererTemp<Point>(k,100);
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

   //fzhang 2019.6.26 adapt individuals based on selected individuals by clustering
    public void adaptPopulationWithClustering(List<GPIndividual> individuals, int subPopNum){

        Individual[] newPop = population.subpops[subPopNum].individuals;

        PhenoCharacterisation pc = null;
        if (evaluator instanceof ClearingEvaluator) {
            ClearingEvaluator clearingEvaluator = (ClearingEvaluator) evaluator;
            pc = clearingEvaluator.getPhenoCharacterisation()[subPopNum];
        } else if (evaluator instanceof MultiPopCoevolutionaryClearingEvaluator) {
            MultiPopCoevolutionaryClearingEvaluator clearingEvaluator =
                    (MultiPopCoevolutionaryClearingEvaluator) evaluator;
            pc = clearingEvaluator.getPhenoCharacterisation()[subPopNum];
        }

        RuleType ruleType = ruleTypes[subPopNum];
        for (int ind = 0; ind < individuals.size(); ind++) {
            GPIndividual gpIndi = (GPIndividual) individuals.get(ind); //check the individual one by one
            int[] charList = pc.characterise(new GPRule(ruleType,gpIndi.trees[0]));//the measured rule
            GPIndividual newInd;
            double distance = 0.0;
            int i = 0;
            double tempDistance = Double.MAX_VALUE;
            GPIndividual tempNewInd = null;
            do {
                i++;
                newInd = (GPIndividual) population.subpops[subPopNum].species.newIndividual(this,0);
                int[] charListNewInd = pc.characterise(new GPRule(ruleType, newInd.trees[0]));//the measured rule
                distance = PhenoCharacterisation.distance(charList, charListNewInd); //calculate the distance
//                System.out.println("distance "+ distance);

                if(distance < tempDistance){
                    tempDistance = distance;
                    tempNewInd = newInd;
                }

            } while (distance != 0 && i != 10000);

            newPop[ind] = tempNewInd;
            newPop[ind].evaluated = false;
        }

        //randomly initialize other individuals
        for (int i = individuals.size(); i < newPop.length; i++) {
            newPop[i] = population.subpops[subPopNum].species.newIndividual(this, 0);
            newPop[i].evaluated = false;
        }
    }


    //fzhang 2019.6.26 adapt individuals based on selected individuals by using knee point
    public void adaptPopulationWithKneePoint(Individual[] individuals, int subPopNum){

        Individual[] newPop = population.subpops[subPopNum].individuals;

        PhenoCharacterisation pc = null;
        if (evaluator instanceof ClearingEvaluator) {
            ClearingEvaluator clearingEvaluator = (ClearingEvaluator) evaluator;
            pc = clearingEvaluator.getPhenoCharacterisation()[subPopNum];
        } else if (evaluator instanceof MultiPopCoevolutionaryClearingEvaluator) {
            MultiPopCoevolutionaryClearingEvaluator clearingEvaluator =
                    (MultiPopCoevolutionaryClearingEvaluator) evaluator;
            pc = clearingEvaluator.getPhenoCharacterisation()[subPopNum];
        }

        RuleType ruleType = ruleTypes[subPopNum];
        double[] minDistance = new double[individuals.length];
        Arrays.fill(minDistance, Double.MAX_VALUE);

        double[] minTempDistance = new double[individuals.length];
        Individual[] bestInds = new Individual[individuals.length];

        for(int tryTimes = 0; tryTimes < 2000; tryTimes++){

            //double minTempDisatnce = Double.MAX_VALUE;
            GPIndividual newInd = (GPIndividual) population.subpops[subPopNum].species.newIndividual(this,0);
            int[] charListNewInd = pc.characterise(new GPRule(ruleType, newInd.trees[0]));//the measured rule
            //int i = 0;
            //get which individual is the most similar one with new generated individual
            for(int ind = 0; ind < individuals.length; ind++){
                GPIndividual gpIndi = (GPIndividual) individuals[ind]; //check the individual one by one
                int[] charList = pc.characterise(new GPRule(ruleType,gpIndi.trees[0]));//the measured rule
                double distance = PhenoCharacterisation.distance(charList, charListNewInd); //calculate the distance

                minTempDistance[ind] = distance;
            }

            for(int repInds  = 0; repInds < individuals.length; repInds++){
                if(minTempDistance[repInds] < minDistance[repInds]){
                    //i++;
                    minDistance[repInds] = minTempDistance[repInds];
                    bestInds[repInds] = newInd;
                }
            }
         /* System.out.println("tryTimes: " + tryTimes);
          System.out.println("how many individuals are replaced: " + i);*/
        }

        System.arraycopy(bestInds, 0, newPop, 0, individuals.length);

        //randomly initialize other individuals
        for (int i = individuals.length; i < newPop.length; i++) {
            newPop[i] = population.subpops[subPopNum].species.newIndividual(this, 0);
            newPop[i].evaluated = false;
        }
    }

    public void saveNumSelectedIndstoFile() {
        //fzhang 2019.5.21 save the weight values
        File numSelectedIndsFile = new File("job." + jobSeed + ".numSelInds.csv"); // jobSeed = 0
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(numSelectedIndsFile));
            writer.write("Run, numIndsSeq, numIndsRou");
            writer.newLine();

            for (int i = 0; i < numSelIndividual.size(); i += 2) { //every two into one generation
                //writer.newLine();
                writer.write(jobSeed + ", " + numSelIndividual.get(i) + ", " + numSelIndividual.get(i+1));
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
