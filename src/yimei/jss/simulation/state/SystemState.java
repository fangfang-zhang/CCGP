package yimei.jss.simulation.state;

import yimei.jss.jobshop.*;
import yimei.jss.jobshop.Process;
import yimei.jss.simulation.DynamicSimulation;
import yimei.jss.simulation.event.ProcessFinishEvent;

import java.util.*;

/**
 * The state of the discrete event simulation system.
 *
 * Created by yimei on 22/09/16.
 */
public class SystemState {

    private double clockTime;
    private List<WorkCenter> workCenters;
    private List<Job> jobsInSystem;
    private List<Job> jobsCompleted;

    protected DynamicSimulation dynamicSimulation;

    public SystemState(double clockTime, List<WorkCenter> workCenters,
                       List<Job> jobsInSystem, List<Job> jobsCompleted) {
        this.clockTime = clockTime;
        this.workCenters = workCenters;
        this.jobsInSystem = jobsInSystem;
        this.jobsCompleted = jobsCompleted;
    }

    public SystemState(double clockTime) {
        this(clockTime, new ArrayList<>(), new LinkedList<>(), new ArrayList<>());
    }

    public SystemState() {
        this(0.0);
    }

    public double getClockTime() {
        return clockTime;
    }

    public List<WorkCenter> getWorkCenters() {
        return workCenters;
    }

    public WorkCenter getWorkCenter(int idx) {
        return workCenters.get(idx);
    }

    public List<Job> getJobsInSystem() {
        return jobsInSystem;
    }

    public List<Job> getJobsCompleted() {
        return jobsCompleted;
    }

    public void setClockTime(double clockTime) {
        this.clockTime = clockTime;
    }

    public void setWorkCenters(List<WorkCenter> workCenters) {
        this.workCenters = workCenters;
    }

    public void setJobsInSystem(List<Job> jobsInSystem) {
        this.jobsInSystem = jobsInSystem;
    }

    public void setJobsCompleted(List<Job> jobsCompleted) {
        this.jobsCompleted = jobsCompleted;
    }

    public void addWorkCenter(WorkCenter workCenter) {
        workCenters.add(workCenter);
    }

    public void addJobToSystem(Job job) {
        jobsInSystem.add(job);
    }

    public void removeJobFromSystem(Job job) {
        jobsInSystem.remove(job);
//        if (jobsInSystem.size() == 0) {
//            if (!verifyRestrictionsMet(jobsCompleted)) {
//                //System.out.println("Still problems with machine allocation");
//            }
//        }
    }

    public void addCompletedJob(Job job) {
        jobsCompleted.add(job);
        
//        System.out.println("the number completed jobs: "+jobsCompleted.size());
//        if (jobsCompleted.size() == 5000) {
//            checkDuplicates();
//            calcUtilLevel();
//            System.out.println("Successful");
//        }
    }


    public DynamicSimulation getdynamicSimulation() {
        return dynamicSimulation;
    }
    
    //fzhang 19.7.2018  calculate total busy time  history
    public double getTotalBusyTime() {
    	double totalBusyTime = 0;
    	for(int i = 0; i< workCenters.size(); i++) {
    		totalBusyTime += workCenters.get(i).getBusyTime();
    	}
		return totalBusyTime;
    }

    //modified by fzhang  27.5.2018  get the total work in system
    public double getWorkInSystem() {
    	double totalProcessTimeInSystem =0;
    	for(int i = 0; i< workCenters.size(); i++) {
    		totalProcessTimeInSystem += workCenters.get(i).getWorkInQueue();
    	}
        return totalProcessTimeInSystem;
    }

    //modified by fzhang  27.5.2018  get the total number of operation in system
    public double getNumOfOperationInSystem() {
    	double totalNumOfOperationInSystem =0;
    	for(int i = 0; i< workCenters.size(); i++) {
    		totalNumOfOperationInSystem += workCenters.get(i).getNumOpsInQueue();
    	}
        return totalNumOfOperationInSystem;
    }

    //modified by fzhang  29.5.2018  get the min,max,ave work in next queue
    //========================================================================
    public double getMinWorkInNextQueue(Operation op) {
    	Operation next  = op.getNext();

    	if (next == null)
    		return 0;

    	return next.getLeastWorkLoad();
    }

    public double getMaxWorkInNextQueue(Operation op) {
    	Operation next  = op.getNext();

    	if (next == null)
    		return 0;

    	return next.getMaxWorkLoad();
    }

    public double getAvgWorkInNextQueue(Operation op) {
    	Operation next  = op.getNext();

    	if (next == null)
    		return 0;

    	return next.getAveWorkLoad();
    }
  //==============================================================

    //modified by fzhang 31.5.2018   get the min,max,ave number of operation in next queue
    public double getMinNumOperationInNextQueue(Operation op) {
    	Operation next  = op.getNext();

    	if (next == null)
    		return 0;

    	return next.getLeastNumOfOperation();
    }

    public double getMaxNumOperationInNextQueue(Operation op) {
    	Operation next  = op.getNext();

    	if (next == null)
    		return 0;

    	return next.getMaxNumOfOperation();
    }

    public double getAveNumOperationInNextQueue(Operation op) {
    	Operation next  = op.getNext();

    	if (next == null)
    		return 0;

    	return next.getAveNumOfOperation();
    }
//=====================================================================================
  //modified by fzhang 31.5.2018   get the min,max,ave number of operation in next queue
    public double getMinNextProcessTime(Operation op) {
    	Operation next  = op.getNext();

    	if (next == null)
    		return 0;

    	return next.getLeastProcessTime();
    }

    public double getMaxNextProcessTime(Operation op) {
    	Operation next  = op.getNext();

    	if (next == null)
    		return 0;

    	return next.getMaxProcessTime();
    }

    public double getMedianNextProcessTime(Operation op) {
    	Operation next  = op.getNext();

    	if (next == null)
    		return 0;

    	return next.getMedianProcessTime();
    }
    //modified by fzhang 31.5.2018  get the interArrivalTimeMean value
  /*  public DynamicSimulation getSimulaiton() {
        return ds;
    }

    public double getInterArrivalTimeMean(DynamicSimulation ds) {
        int numWorkCenters = ds.getNumWorkCenters();
        int minNumOps =  ds.getMinNumOperations();
        int maxNumOps =  ds.getMaxNumOperations();
        double utilLevel = ds.getUtilLevel();

    	return ds.interArrivalTimeMean(numWorkCenters, minNumOps, maxNumOps, utilLevel);
    }*/

    private boolean verifyRestrictionsMet(List<Job> jobsCompleted) {
        //as a basic start, let's go through the work centers and create an array with clocktime empty slots for each
        //then we can fill in each array with the operation that was being worked on, and check none used the
        //same work center at the same time
        int numWorkCenters = workCenters.size();
        int clockTime = (int) getClockTime();
        //System.out.println("Clock time: "+clockTime);

        //By looking at all operation values, lets check workDone makes sense
        //It's the average proc time per operation we're sceptical of



        //So with traditional JSS, say we have a job with 10 operations
        //then the average procedure time will be the sampler's average
        //but in FJSS, a job with 10 operations and 5 options per operation
        //if it chooses the lowest procedure time for each option, then the util
        //level will be very low comparatively

//
//        double medianWork = 0.0;
//        double minWork = 0.0;
//        double maxWork = 0.0;
//        int numOperationsAgain = 0;
//        for (Job job: jobsCompleted) {
//            for (Operation operation: job.getOperations()) {
//                double medianProcTime;
//                double[] procTimes = new double[operation.getOperationOptions().size()];
//                for (int j = 0; j < operation.getOperationOptions().size(); ++j) {
//                    procTimes[j] = operation.getOperationOptions().get(j).getProcTime();
//                }
//                Arrays.sort(procTimes);
//                minWork += procTimes[0]; //minimum proc time
//                maxWork += procTimes[operation.getOperationOptions().size()-1]; //maximum proc time
//
//                if (procTimes.length % 2 == 0){
//                    //halfway between two points, as even number of elements
//                    medianProcTime = ((double) procTimes[procTimes.length/2] + (double)procTimes[procTimes.length/2 - 1])/2;
//                }
//                else {
//                    medianProcTime = (double) procTimes[procTimes.length / 2];
//                }
//                medianWork += medianProcTime;
//                numOperationsAgain++;
//            }
//        }
//
//        System.out.println("Minimum average time per operation: "+minWork/numOperationsAgain);
//        System.out.println("Median time per operation: "+medianWork/numOperationsAgain);
//        System.out.println("Maximum time per operation: "+maxWork/numOperationsAgain);


        double[] jobArrivalTimes = new double[jobsCompleted.size()];
        for (int i = 0; i < jobsCompleted.size(); ++i) {
            jobArrivalTimes[i] = jobsCompleted.get(i).getArrivalTime();
        }
        //Arrays.sort(jobArrivalTimes);
        //double[] interArrivalTimes = new double[jobsCompleted.size()-1];
        double interArrivalTimesSum = 0.0;
        for (int i = 0; i < jobsCompleted.size()-1; ++i) {
            interArrivalTimesSum += (jobArrivalTimes[i+1] - jobArrivalTimes[i]);
        }
//        Arrays.sort(interArrivalTimes);
//        double medianInterArrivalTime;
//        if (interArrivalTimes.length % 2 == 0){
//            //halfway between two points, as even number of elements
//            medianInterArrivalTime = ((double) interArrivalTimes[interArrivalTimes.length/2] + (double)interArrivalTimes[interArrivalTimes.length/2 - 1])/2;
//        }
//        else {
//            medianInterArrivalTime = (double) interArrivalTimes[interArrivalTimes.length / 2];
//        }
//
//        System.out.println("Median inter-arrival time for this simulation: "+medianInterArrivalTime);
        System.out.println("Mean inter-arrival time for this simulation: "+(interArrivalTimesSum/
                jobsCompleted.size()-1));
        return true;
    }

    private void calcUtilLevel() {
        int numWorkCenters = workCenters.size();
        double[] maxTimesEarliest = new double[numWorkCenters];
        for (int i = 0; i < numWorkCenters; ++i) {
            maxTimesEarliest[i] = Double.MAX_VALUE; //so we know default
        }
        double[] maxTimesLatest = new double[numWorkCenters];

        //for max times we just need to know the earliest and latest operationOption to be performed
        //on a given workcenter right?
        double[] busyTimes = new double[numWorkCenters];
        for (Job j: jobsCompleted) {
            if (j.getOperations().size() != j.getProcessFinishEvents().size()) {
                System.out.println("Job isn't finished...");
            }
            for (ProcessFinishEvent processFinishEvent: j.getProcessFinishEvents()) {
                Process p = processFinishEvent.getProcess();
                int workCenterId = p.getWorkCenter().getId();
                if (maxTimesEarliest[workCenterId] > p.getStartTime()) {
                    maxTimesEarliest[workCenterId] = p.getStartTime();
                }
                if (maxTimesLatest[workCenterId] < p.getFinishTime()) {
                    maxTimesLatest[workCenterId] = p.getFinishTime();
                }
                double operationDuration = p.getFinishTime() - p.getStartTime();
                busyTimes[workCenterId] += operationDuration;
            }
        }

        double sumMaxTime = 0;
        double sumBusyTime = 0;
        for (int i = 0; i < numWorkCenters; ++i) {
            sumMaxTime += (maxTimesLatest[i] - maxTimesEarliest[i]);
            sumBusyTime += busyTimes[i];
        }

        String print = " "+(sumBusyTime*100)/sumMaxTime;

//        //This method should calculate the proportion of the time that the workCenters
//        //are busy
//        //If util level = 100% then -> numWorkCenters * timeTaken = amount of work done
//        double maxBusyTime = 0;
//        int totalBusyTime = 0;
//        for (int i = 0; i < numWorkCenters; ++i) {
//            int workCenterBusyTime = 0;
//            int workCenterMaxBusyTime = 0;
//            boolean warmupComplete = false;
//            boolean finished = false;
//            int warmupStartedAt = 0;
//            int finishedAt = 0;
//            int finishedCount = 0; //maintain this count in case we are not finished
//
//            for (int j = 0; j < workCenterAllocations[i].length; ++j) {
//                if (workCenterAllocations[i][j] != -1) {
//                    workCenterBusyTime++;
//                    if (finished) {
//                        if (warmupComplete) {
//                            workCenterMaxBusyTime += finishedCount; //we were wrong
//                        }
//                        finished = false;
//                        finishedCount = 0;
//                    }
//                    if (!warmupComplete) {
//                        warmupComplete = true;
//                        warmupStartedAt = j;
//                    }
//                }
//                if (warmupComplete) {
//                    if (workCenterAllocations[i][j] == -1) {
//                        if (!finished) {
//                            finishedAt = j;
//                            finished = true;
//                        }
//                        finishedCount++;
//                    } else {
//                        workCenterMaxBusyTime++;
//                    }
//                }
//            }
//            if (workCenterAllocations[i][workCenterAllocations[i].length-1] != -1) {
//                finishedAt = workCenterAllocations[i].length-1;
//            }
//            //System.out.println("Work center "+i+" was busy "+workCenterBusyTime +" out of "+workCenterMaxBusyTime);
//            totalBusyTime += workCenterBusyTime;
//            maxBusyTime += workCenterMaxBusyTime;
//            int workCenterBusy = finishedAt - warmupStartedAt;
//            //System.out.println("Max busy time: "+workCenterMaxBusyTime+" vs "+workCenterBusy);
//        }

        //double utilLevel = (totalBusyTime*100)/maxBusyTime;
        //System.out.println("Util level: "+print);
        //System.out.println();
    }

    private void checkDuplicates() {

        double numJobs = 0;
        double numOperations = 0;
        double workDone = 0;

        //Process[][] processes = new Process[workCenters.size()][jobsCompleted.size()];
        List<List> processes = new ArrayList<>();
        for (int i = 0; i < workCenters.size(); ++i) {
            processes.add(new ArrayList<Process>());
        }

        for (Job job: jobsCompleted) {
            for (ProcessFinishEvent processFinishEvent: job.getProcessFinishEvents()) {
                Process p = processFinishEvent.getProcess();
                processes.get(p.getWorkCenter().getId()).add(p);
                //List<Process> workCenterSchedule = workCenterAllocations[p.getWorkCenter().getId()];
//                for (int i = (int) p.getStartTime(); i < (int) p.getFinishTime(); ++i) {
//                    if (workCenterSchedule[i] == -1) {
//                        workCenterSchedule[i] = job.getId();
//                    } else {
//                        System.out.println("Doubled up on the schedule");
//                        //return false;
//                    }
//                }
                //numOperations++;
                //workDone += p.getOperationOption().getProcTime();
            }
        }

        for (int i = 0; i < workCenters.size(); ++i) {
            List<Process> workCenterProcesses = processes.get(i);
            Collections.sort(workCenterProcesses);
            for (int j = 0; j < workCenterProcesses.size()-1; ++j) {
                //want to check that this process starts after the one in front of it
                //and finishes before the one after it
                Process p1 = workCenterProcesses.get(j);
                Process p2 = workCenterProcesses.get(j+1);
                if (p1.getFinishTime() > p2.getStartTime()) {
                    System.out.println("Double up of schedule");
                }
            }
        }

//        System.out.println("Num jobs: "+numJobs);
//        System.out.println("Num operations: "+numOperations+", Average Number per Job: "+numOperations/numJobs);
//        System.out.println("Work done: "+workDone+", Average Proc Time per Operation: "+workDone/numOperations);
    }

    public void reset() {
        clockTime = 0.0;
        for (WorkCenter workCenter : workCenters) {
            workCenter.reset();
        }
        jobsInSystem.clear();
        jobsCompleted.clear();
    }

    public double slack(OperationOption operation) {
        return operation.getOperation().getJob().getDueDate()
                - getClockTime() - operation.getWorkRemaining();
    }

//    public double workInNextQueue(OperationOption operation) {
//        OperationOption nextOp = operation.getNext(this);
//        if (nextOp == null) {
//            return 0;
//        }
//
//        return nextOp.getWorkCenter().getWorkInQueue();
//    }

//
//    public double numOpsInNextQueue(OperationOption operation) {
//        OperationOption nextOp = operation.getNext(this);
//        if (nextOp == null) {
//            return 0;
//        }
//
//        return nextOp.getWorkCenter().getQueue().size();
//    }
//
//    public double nextReadyTime(OperationOption operation) {
//        OperationOption nextOp = operation.getNext(this);
//        if (nextOp == null) {
//            return 0;
//        }
//
//        return nextOp.getWorkCenter().getReadyTime();
//    }

    @Override
    public SystemState clone() {
        List<WorkCenter> clonedWCs = new ArrayList<>();
        for (WorkCenter wc : workCenters) {
            clonedWCs.add(wc.clone());
        }

        //rules do not maintain state
        return new SystemState(clockTime, clonedWCs,
                new LinkedList<>(), new ArrayList<>());
    }

    @Override
    public String toString() {
        return "SystemState{" +
                "clockTime=" + clockTime +
                ", workCenters=" + workCenters +
                ", jobsInSystem=" + jobsInSystem +
                ", jobsCompleted=" + jobsCompleted +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SystemState that = (SystemState) o;

        if (Double.compare(that.clockTime, clockTime) != 0) return false;
        if (workCenters != null ? !workCenters.equals(that.workCenters) : that.workCenters != null) return false;
        if (jobsInSystem != null ? !jobsInSystem.equals(that.jobsInSystem) : that.jobsInSystem != null) return false;
        return jobsCompleted != null ? jobsCompleted.equals(that.jobsCompleted) : that.jobsCompleted == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(clockTime);
        result = (int) (temp ^ (temp >>> 32));
        result = 31 * result + (workCenters != null ? workCenters.hashCode() : 0);
        result = 31 * result + (jobsInSystem != null ? jobsInSystem.hashCode() : 0);
        result = 31 * result + (jobsCompleted != null ? jobsCompleted.hashCode() : 0);
        return result;
    }
}
