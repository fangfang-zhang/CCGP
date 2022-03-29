package yimei.jss.simulation;

import org.apache.commons.math3.random.RandomDataGenerator;
import yimei.jss.jobshop.*;
import yimei.util.random.*;
import yimei.jss.rule.AbstractRule;
import yimei.jss.simulation.event.AbstractEvent;
import yimei.jss.simulation.event.JobArrivalEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The dynamic simulation -- discrete event simulation
 *
 * Created by yimei on 22/09/16.
 */
public class DynamicSimulation extends Simulation {

    public final static int SEED_ROTATION = 10000;

    private long seed;
    private RandomDataGenerator randomDataGenerator;

    private final int minNumOperations;
    private final int maxNumOperations;
    private final double utilLevel;
    private final double dueDateFactor;
    private final boolean revisit;

    private AbstractIntegerSampler numOperationsSampler;
    //modified by fzhang, 17.04.2018  in order to set options from 2 to 10
    //private AbstractIntegerSampler numOptionsSampler;

    private AbstractRealSampler procTimeSampler;
    private AbstractRealSampler interArrivalTimeSampler;
    private AbstractRealSampler jobWeightSampler;

    private DynamicSimulation(long seed,
                              AbstractRule sequencingRule,
                              AbstractRule routingRule,
                              int numWorkCenters,
                              int numJobsRecorded,
                              int warmupJobs,
                              int minNumOperations,
                              int maxNumOperations,
                              double utilLevel,
                              double dueDateFactor,
                              boolean revisit,
                              AbstractIntegerSampler numOperationsSampler,
                              //modified by fzhang, 17.04.2018
                              //AbstractIntegerSampler numOptionsSampler,

                              AbstractRealSampler procTimeSampler,
                              AbstractRealSampler interArrivalTimeSampler,
                              AbstractRealSampler jobWeightSampler) {
        super(sequencingRule, routingRule, numWorkCenters, numJobsRecorded, warmupJobs);

        this.seed = seed;
        this.randomDataGenerator = new RandomDataGenerator();
        this.randomDataGenerator.reSeed(seed);

        this.minNumOperations = minNumOperations;
        this.maxNumOperations = maxNumOperations;
        this.utilLevel = utilLevel;
        this.dueDateFactor = dueDateFactor;
        this.revisit = revisit;

        this.numOperationsSampler = numOperationsSampler;
        //modified by fzhang 17.04.2018
        //this.numOptionsSampler = numOptionsSampler;

        this.procTimeSampler = procTimeSampler;
        this.interArrivalTimeSampler = interArrivalTimeSampler;
        this.jobWeightSampler = jobWeightSampler;

        setInterArrivalTimeSamplerMean();

        // Create the work centers, with empty queue and ready to go initially.
        for (int i = 0; i < numWorkCenters; i++) {
            systemState.addWorkCenter(new WorkCenter(i));
        }

        setup();
    }

    public DynamicSimulation(long seed,
                             AbstractRule sequencingRule,
                             AbstractRule routingRule,
                             int numWorkCenters,
                             int numJobsRecorded,
                             int warmupJobs,
                             int minNumOperations,
                             int maxNumOperations,
                             double utilLevel,
                             double dueDateFactor,
                             boolean revisit) {
        this(seed, sequencingRule, routingRule, numWorkCenters, numJobsRecorded, warmupJobs,
                minNumOperations, maxNumOperations, utilLevel, dueDateFactor, revisit,
                //here, specifiy the range of UniformIntegerSample to (1,10)
                new UniformIntegerSampler(minNumOperations, maxNumOperations), //these two values will be changed during the evolutionary process, because different models are called.
                //the surrogate model will set them to 1 and 5, but the original model will set them to 1 and 10
                //when calculate the phenotype, in this code, full simulation is used, they will be set to 10 and 10.
                //modified by fzhang 17.04.2018
                //new UniformIntegerSampler(1, numWorkCenters), //in this way, whether add this parameter or not is the same
                //new UniformIntegerSampler(1, 5), //one operation only can be processed at 5 machines

                new UniformSampler(1, 99),
                new ExponentialSampler(),
                new TwoSixTwoSampler());
    }

    public int getNumWorkCenters() {
        return numWorkCenters;
    }

    public int getNumJobsRecorded() {
        return numJobsRecorded;
    }

    public int getWarmupJobs() {
        return warmupJobs;
    }

    public int getMinNumOperations() {
        return minNumOperations;
    }

    public int getMaxNumOperations() {
        return maxNumOperations;
    }

    public double getUtilLevel() {
        return utilLevel;
    }

    public double getDueDateFactor() {
        return dueDateFactor;
    }

    public boolean isRevisit() {
        return revisit;
    }

    public RandomDataGenerator getRandomDataGenerator() {
        return randomDataGenerator;
    }

    public AbstractIntegerSampler getNumOperationsSampler() {
        return numOperationsSampler;
    }

    public AbstractRealSampler getProcTimeSampler() {
        return procTimeSampler;
    }

    public AbstractRealSampler getInterArrivalTimeSampler() {
        return interArrivalTimeSampler;
    }

    public AbstractRealSampler getJobWeightSampler() {
        return jobWeightSampler;
    }

    @Override
    public void setup() {
        numJobsArrived = 0;
        throughput = 0;
        generateJob();
    }

    @Override
    public void resetState() {
        systemState.reset();
        eventQueue.clear();
        setup();
    }

    @Override
    public void reset() {
        reset(seed);
    }

    public void reset(long seed) {
        reseed(seed);
        resetState();
    }

    public void reseed(long seed) {
        this.seed = seed;
        randomDataGenerator.reSeed(seed);
    }

    @Override
    public void rotateSeed() {//this is use for changing seed value in next generation
    	//this only relates to generation
        seed += SEED_ROTATION;
        reset();
        //System.out.println(seed);//when seed=0, after Gen0, the value is 10000, after Gen1, the value is 20000....
    }

    @Override
    public void generateJob() {
        //runExperiments();
    	//modified by fzhang 15.5.2018  to avoid negative time  finallly decide to keep double type: to avoid same arrival time
        double arrivalTime = getClockTime()
                + interArrivalTimeSampler.next(randomDataGenerator);
        double weight = jobWeightSampler.next(randomDataGenerator);
        Job job = new Job(numJobsArrived, new ArrayList<>(),
                arrivalTime, arrivalTime, 0, weight);
        int numOperations = numOperationsSampler.next(randomDataGenerator);

        for (int i = 0; i < numOperations; i++) {
            Operation o = new Operation(job, i);
            //modified by fzhang 17.04.2018
            //int numOptions = numOptionsSampler.next(randomDataGenerator);
            int numOptions = numOperationsSampler.next(randomDataGenerator);
            //System.out.println("numOptions: "+numOptions);

            int[] route = randomDataGenerator.nextPermutation(numWorkCenters, numOptions);
            //nextPermutation(n,k)
            //Generates an integer array of length k whose entries are selected randomly, without repetition, from the integers 0, ..., n - 1 (inclusive).

            //modified by fzhang  14.5.2018  in order to avoid negative or positive time(equal = 0)  finallly decide to keep double type
            //double procTime = procTimeSampler.next(randomDataGenerator); //use same proc time for all options for now
            //================start==========
            double procTime = procTimeSampler.next(randomDataGenerator);
            for (int j = 0; j < numOptions; j++) {//9
                o.addOperationOption(new OperationOption(o,j,procTime,systemState.getWorkCenter(route[j])));
            }
            //==========end===========
            
            //modified by fzhang  29.5.2018  set different processing time for different machines
           /* for (int j = 0; j < numOptions; j++) {
            	double procTime = procTimeSampler.next(randomDataGenerator);
                o.addOperationOption(new OperationOption(o,j,procTime,systemState.getWorkCenter(route[j])));
            }
*/

           //fzhang 2019.6.22 set different processtime to each machine
            //============================start========================================================
            /*double ptmean =  procTimeSampler.next(randomDataGenerator);// set processtime of each option
            AbstractRealSampler ptnsampler=new NormalSampler(ptmean, ptmean/10);

            for (int j = 0; j < numOptions; j++) {
                double procTime= ptnsampler.next(randomDataGenerator);
                o.addOperationOption(new OperationOption(o,j,procTime,systemState.getWorkCenter(route[j])));
            }*/
            //==============================end=================================================

            job.addOperation(o);
        }

        job.linkOperations();
        //just set totalProcTime to average value, as we don't know which option will be chosen
        //this is just used to define dueDate value
        double totalProcTime = numOperations * procTimeSampler.getMean();
        double dueDate = job.getReleaseTime() + dueDateFactor * totalProcTime;

        job.setDueDate(dueDate);
//        if (job.getId() > 501) {
//            int a  = 1;
//        }

        systemState.addJobToSystem(job);
        numJobsArrived ++;

        eventQueue.add(new JobArrivalEvent(job));
    }

//    private void runExperiments() {
//        double interArrivalSum = 0.0;
//        double numOperationsSum = 0.0;
//        double numOptionsSum = 0.0;
//        double procTimeSum = 0.0;
//        int numRuns = 5000000;
//
//        for (int i = 0; i < numRuns; ++i) {
//            interArrivalSum += interArrivalTimeSampler.next(randomDataGenerator);
//            numOperationsSum += numOperationsSampler.next(randomDataGenerator);
//            numOptionsSum += numOperationsSampler.next(randomDataGenerator);
//            procTimeSum += procTimeSampler.next(randomDataGenerator);
//        }
//        System.out.println("Average interarrival time: "+interArrivalSum/numRuns);
//        System.out.println("Average num operations: "+numOperationsSum/numRuns);
//        System.out.println("Average num options: "+numOptionsSum/numRuns);
//        System.out.println("Average procedure time: "+procTimeSum/numRuns);
//        System.out.println();
//    }

    //control the inter time of job arrival
    public double interArrivalTimeMean(int numWorkCenters,
                                             int minNumOps,
                                             int maxNumOps,
                                             double utilLevel) {
        double meanNumOps = 0.5 * (minNumOps + maxNumOps); //(1+9)/2=5.5 average operations for a job is 5.5
        double meanProcTime = procTimeSampler.getMean(); //(1+99)/2=50   average processing time for a operation is 50

        //for machines with same capacity, this return value is the same.
        //for machines with different capacities, this return value is different because utilLevel is dynamic
        return (meanNumOps * meanProcTime) / (utilLevel * numWorkCenters); // the time to processing a job on each workcenter
    }

    public void setInterArrivalTimeSamplerMean() {
        double mean = interArrivalTimeMean(numWorkCenters, minNumOperations, maxNumOperations, utilLevel);
        interArrivalTimeSampler.setMean(mean);
    }

    public List<SequencingDecisionSituation> sequencingDecisionSituations(int minQueueLength) {
        List<SequencingDecisionSituation> sequencingDecisionSituations = new ArrayList<>();

        
        while (!eventQueue.isEmpty() && throughput < numJobsRecorded) {
            AbstractEvent nextEvent = eventQueue.poll();
//            System.out.println("throughput "+throughput);
            systemState.setClockTime(nextEvent.getTime());
            nextEvent.addSequencingDecisionSituation(this, sequencingDecisionSituations, minQueueLength);
        }

        resetState();

        return sequencingDecisionSituations;
    }

    public List<RoutingDecisionSituation> routingDecisionSituations(int minQueueLength) {
        List<RoutingDecisionSituation> routingDecisionSituations = new ArrayList<>();

        while (!eventQueue.isEmpty() && throughput < numJobsRecorded) {
            AbstractEvent nextEvent = eventQueue.poll();

            systemState.setClockTime(nextEvent.getTime());
            nextEvent.addRoutingDecisionSituation(this, routingDecisionSituations, minQueueLength);
        }

        resetState();

        return routingDecisionSituations;
    }

    @Override
    public Simulation surrogate(int numWorkCenters, int numJobsRecorded,
                                       int warmupJobs) {
        int surrogateMaxNumOperations = maxNumOperations;

        AbstractIntegerSampler surrogateNumOperationsSampler = numOperationsSampler.clone();
        AbstractIntegerSampler surrogateNumOptionsSampler = numOperationsSampler.clone();
        AbstractRealSampler surrogateInterArrivalTimeSampler = interArrivalTimeSampler.clone();

        if (surrogateMaxNumOperations > numWorkCenters) {
            surrogateMaxNumOperations = numWorkCenters;
            surrogateNumOperationsSampler.setUpper(surrogateMaxNumOperations);

            surrogateInterArrivalTimeSampler.setMean(interArrivalTimeMean(numWorkCenters,
                    minNumOperations, surrogateMaxNumOperations, utilLevel));
        }

        Simulation surrogate = new DynamicSimulation(seed, sequencingRule, routingRule, numWorkCenters,
                numJobsRecorded, warmupJobs, minNumOperations, surrogateMaxNumOperations,
                utilLevel, dueDateFactor, revisit, surrogateNumOperationsSampler,
                procTimeSampler, surrogateInterArrivalTimeSampler, jobWeightSampler);

        //modified by fzhang 17.04.2018
       /* Simulation surrogate = new DynamicSimulation(seed, sequencingRule, routingRule, numWorkCenters,
                numJobsRecorded, warmupJobs, minNumOperations, surrogateMaxNumOperations,
                utilLevel, dueDateFactor, revisit, surrogateNumOperationsSampler,
                numOptionsSampler, procTimeSampler, surrogateInterArrivalTimeSampler, jobWeightSampler);*/

        return surrogate;
    }

    @Override
    public Simulation surrogateBusy(int numWorkCenters, int numJobsRecorded,
                                int warmupJobs) {
        double utilLevel = 1;
        int surrogateMaxNumOperations = maxNumOperations;

        AbstractIntegerSampler surrogateNumOperationsSampler = numOperationsSampler.clone();
        AbstractRealSampler surrogateInterArrivalTimeSampler = interArrivalTimeSampler.clone();

        if (surrogateMaxNumOperations > numWorkCenters) {
            surrogateMaxNumOperations = numWorkCenters;
            surrogateNumOperationsSampler.setUpper(surrogateMaxNumOperations);

            surrogateInterArrivalTimeSampler.setMean(interArrivalTimeMean(numWorkCenters,
                    minNumOperations, surrogateMaxNumOperations, utilLevel));
        }

        Simulation surrogate = new DynamicSimulation(seed, sequencingRule, routingRule, numWorkCenters,
                numJobsRecorded, warmupJobs, minNumOperations, surrogateMaxNumOperations, utilLevel,
                dueDateFactor, revisit, surrogateNumOperationsSampler, procTimeSampler,
                surrogateInterArrivalTimeSampler, jobWeightSampler);

        //modified by fzhang 17.04.2018
     /*   Simulation surrogate = new DynamicSimulation(seed, sequencingRule, routingRule, numWorkCenters,
                numJobsRecorded, warmupJobs, minNumOperations, surrogateMaxNumOperations, utilLevel,
                dueDateFactor, revisit, surrogateNumOperationsSampler, numOptionsSampler, procTimeSampler,
                surrogateInterArrivalTimeSampler, jobWeightSampler);*/

        return surrogate;
    }

    public static DynamicSimulation standardFull(
            long seed,
            AbstractRule sequencingRule,
            AbstractRule routingRule,
            int numWorkCenters,
            int numJobsRecorded,
            int warmupJobs,
            double utilLevel,
            double dueDateFactor) {
        return new DynamicSimulation(seed, sequencingRule, routingRule, numWorkCenters, numJobsRecorded,
                warmupJobs, numWorkCenters, numWorkCenters, utilLevel,
                dueDateFactor, false);
    }

    public static DynamicSimulation standardMissing(
            long seed,
            AbstractRule sequencingRule,
            AbstractRule routingRule,
            int numWorkCenters,
            int numJobsRecorded,
            int warmupJobs,
            double utilLevel,
            double dueDateFactor) {
    	 return new DynamicSimulation(seed, sequencingRule, routingRule, numWorkCenters, numJobsRecorded,
                 warmupJobs,1, numWorkCenters, utilLevel, dueDateFactor, false);
    }
}
