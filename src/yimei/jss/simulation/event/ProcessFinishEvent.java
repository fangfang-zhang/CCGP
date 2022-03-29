package yimei.jss.simulation.event;

import yimei.jss.jobshop.Process;
import yimei.jss.jobshop.*;
import yimei.jss.simulation.RoutingDecisionSituation;
import yimei.jss.simulation.SequencingDecisionSituation;
import yimei.jss.simulation.Simulation;

import java.util.List;

/**
 * Created by yimei on 22/09/16.
 */
public class ProcessFinishEvent extends AbstractEvent {

    private Process process;
    //fzhang 29.8.2018 in order to record the completion time of jobs
    protected long jobSeed;

    public ProcessFinishEvent(double time, Process process) {
        super(time);
        this.process = process;
    }

    public ProcessFinishEvent(Process process) {
        this(process.getFinishTime(), process);
    }

    @Override
    public void trigger(Simulation simulation) {
        WorkCenter workCenter = process.getWorkCenter();
        process.getOperationOption().getJob().addProcessFinishEvent(this);

        if (!workCenter.getQueue().isEmpty()) {
            SequencingDecisionSituation sequencingDecisionSituation =
                    new SequencingDecisionSituation(workCenter.getQueue(), workCenter,
                            simulation.getSystemState());

            //System.out.println("=======================================sequencing==========================================");
            OperationOption dispatchedOp =
                    simulation.getSequencingRule().priorOperation(sequencingDecisionSituation);

            workCenter.removeFromQueue(dispatchedOp);

            //must wait for machine to be ready
            double processStartTime = Math.max(workCenter.getReadyTime(), time);

            Process nextP = new Process(workCenter, process.getMachineId(),
                    dispatchedOp, processStartTime);
            simulation.addEvent(new ProcessStartEvent(nextP));
        }

        OperationOption nextOp = process.getOperationOption().getNext(simulation.getSystemState(),
                simulation.getRoutingRule());

        if (nextOp == null) {
            Job job = process.getOperationOption().getJob();
            job.setCompletionTime(process.getFinishTime());
            simulation.completeJob(job);
            
            //fzhang 29.8.2018 when a job is finished, record the completion time of this job. So, we have 5000 jobs, 5000 information
            //too much information. This is suitable in test process and set the job number a relative smaller number.
            /*System.out.println("Job ID: "+job.getId());
            System.out.println("Number of Operations: "+job.getOperations().size());
            System.out.println("Arrival Time: "+job.getArrivalTime());
            System.out.println("Completion Time: "+job.getCompletionTime()); //getCompletionTime is a time point.  flowtime = completionTime - arrivalTime
            System.out.println("Total Processing Time: "+job.getTotalProcTime());
            System.out.println("Average Processing Time: "+job.getAvgProcTime()); //getTotalProcTime/numOfOperations
            System.out.println("Flow Time: "+job.flowTime());
            System.out.println("Waiting Time: "+job.getWaitingTime());*/
        }
        else {
            simulation.addEvent(new OperationVisitEvent(time, nextOp));
        }
    }

	// modified by fzhang 26.4.2018 write bad run times to *.csv
/* 	public void WriteCompletionTime(EvolutionState state, final Parameter base) {
 		Parameter p;
 		// Get the job seed.
 		p = new Parameter("seed").push("" + 0);
 		jobSeed = state.parameters.getLongWithDefault(p, null, 0);
 		File completiontime = new File("job." + jobSeed + ".BadRun.csv");

 		try {
 			BufferedWriter writer = new BufferedWriter(new FileWriter(completiontime));
 			writer.write("jobID,arrivaltime,finishtime,completiontime");
 			writer.newLine();
 			  
 			writer.close();
 		} catch (IOException e) {
 			e.printStackTrace();
 		}
 	}*/
 	
    @Override
    public void addSequencingDecisionSituation(Simulation simulation,
                                     List<SequencingDecisionSituation> situations,
                                     int minQueueLength) {
        WorkCenter workCenter = process.getWorkCenter();
        process.getOperationOption().getJob().addProcessFinishEvent(this);

        if (!workCenter.getQueue().isEmpty()) {
            SequencingDecisionSituation sequencingDecisionSituation =
                    new SequencingDecisionSituation(workCenter.getQueue(), workCenter,
                            simulation.getSystemState());

          /*  if (workCenter.getQueue().size() >= minQueueLength) { //when set operation with different processing time, the queue is hard to >= minQueueLength, an error happen here
                situations.add(sequencingDecisionSituation.clone());
            }
*/
          //fzhang 2019.9.4 change all the decison size as minQueueLength, in order to keep the matrix has the same length
            if (workCenter.getQueue().size() == minQueueLength) { //when set operation with different processing time, the queue is hard to >= minQueueLength, an error happen here
                situations.add(sequencingDecisionSituation.clone());
            }

            OperationOption dispatchedOp =
                    simulation.getSequencingRule().priorOperation(sequencingDecisionSituation);

            workCenter.removeFromQueue(dispatchedOp);

            //must wait for machine to be ready
            double processStartTime = Math.max(workCenter.getReadyTime(), time);

            Process nextP = new Process(workCenter, process.getMachineId(),
                    dispatchedOp, processStartTime);
            simulation.addEvent(new ProcessStartEvent(nextP));
        }

        OperationOption nextOp = process.getOperationOption().getNext(simulation.getSystemState(),
                simulation.getRoutingRule());

        if (nextOp == null) {
            Job job = process.getOperationOption().getJob();
            job.setCompletionTime(process.getFinishTime());
            simulation.completeJob(job);
        }
        else {
            simulation.addEvent(new OperationVisitEvent(time, nextOp));
        }
    }

    @Override
    public void addRoutingDecisionSituation(Simulation simulation,
                                               List<RoutingDecisionSituation> situations,
                                               int minOptions) {
        WorkCenter workCenter = process.getWorkCenter();
        process.getOperationOption().getJob().addProcessFinishEvent(this);

        if (!workCenter.getQueue().isEmpty()) {
            SequencingDecisionSituation sequencingDecisionSituation =
                    new SequencingDecisionSituation(workCenter.getQueue(), workCenter,
                            simulation.getSystemState());

            OperationOption dispatchedOp =
                    simulation.getSequencingRule().priorOperation(sequencingDecisionSituation);

            workCenter.removeFromQueue(dispatchedOp);

            //must wait for machine to be ready
            double processStartTime = Math.max(workCenter.getReadyTime(), time);

            Process nextP = new Process(workCenter, process.getMachineId(),
                    dispatchedOp, processStartTime);
            simulation.addEvent(new ProcessStartEvent(nextP));
        }

       /* if (process.getOperationOption().getOperation().getNext() != null) {
            if (process.getOperationOption().getOperation().getNext().getOperationOptions().size()
                    >= minOptions) {
                Operation o = process.getOperationOption().getOperation();
                RoutingDecisionSituation r = o.getNext().routingDecisionSituation(simulation.getSystemState());
                situations.add(r.clone());
            }
        }*/

        //fzhang 2019.9.25 change all the decison size as minQueueLength, in order to keep the matrix has the same length
        if (process.getOperationOption().getOperation().getNext() != null) {
            if (process.getOperationOption().getOperation().getNext().getOperationOptions().size()
                    == minOptions) {
                Operation o = process.getOperationOption().getOperation();
                RoutingDecisionSituation r = o.getNext().routingDecisionSituation(simulation.getSystemState());
                situations.add(r.clone());
            }
        }

        OperationOption nextOp = process.getOperationOption().getNext(simulation.getSystemState(),
                simulation.getRoutingRule());

        if (nextOp == null) {
            Job job = process.getOperationOption().getJob();
            job.setCompletionTime(process.getFinishTime());
            simulation.completeJob(job);
        }
        else {
            simulation.addEvent(new OperationVisitEvent(time, nextOp));
        }
    }


    @Override
    public String toString() {
        return String.format("%.1f: job %d op %d finished on work center %d.\n",
                time,
                process.getOperationOption().getJob().getId(),
                process.getOperationOption().getOperation().getId(),
                process.getWorkCenter().getId());
    }

    @Override
    public int compareTo(AbstractEvent other) {
        if (time < other.time)
            return -1;

        if (time > other.time)
            return 1;

        if (other instanceof ProcessFinishEvent) {
            ProcessFinishEvent otherPFE = (ProcessFinishEvent)other;

            if (process.getWorkCenter().getId() < otherPFE.process.getWorkCenter().getId())
                return -1;

            if (process.getWorkCenter().getId() > otherPFE.process.getWorkCenter().getId())
            return 1;
        }

        return 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProcessFinishEvent that = (ProcessFinishEvent) o;

        return process != null ? process.equals(that.process) : that.process == null;
    }

    @Override
    public int hashCode() {
        return process != null ? process.hashCode() : 0;
    }


    public Process getProcess() {
        return process;
    }
}
