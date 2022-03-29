package yimei.jss.simulation.event;

import yimei.jss.jobshop.Job;
import yimei.jss.jobshop.Operation;
import yimei.jss.jobshop.OperationOption;
import yimei.jss.simulation.RoutingDecisionSituation;
import yimei.jss.simulation.SequencingDecisionSituation;
import yimei.jss.simulation.Simulation;

import java.util.List;

/**
 * Created by yimei on 22/09/16.
 */
public class JobArrivalEvent extends AbstractEvent {

    private Job job;

    public JobArrivalEvent(double time, Job job) {
        super(time);
        this.job = job;
    }

    public JobArrivalEvent(Job job) {
        this(job.getArrivalTime(), job);
    }

    @Override
    public void trigger(Simulation simulation) {
        //Job has just arrived, get first operation out
        Operation operation = job.getOperation(0);

        //yimei 2019.7.30 set the readytime of each operation to current time. It will be used to calculate operation waiting time in the queue of operation
        //before ther is a bug that all the readytime of operation is 0 and never update---the OWT = system.clocktime
        for (OperationOption op : operation.getOperationOptions())
            op.setReadyTime(job.getReleaseTime());

        //get options of operation, and SystemState
        RoutingDecisionSituation decisionSituation = new RoutingDecisionSituation(
                operation.getOperationOptions(), simulation.getSystemState());

        //System.out.println("===================routing=============");
        //use routing rule to decide which option we will use !!!!!
        OperationOption operationOption =
                simulation.getRoutingRule().nextOperationOption(decisionSituation);
        //operationOption.setReadyTime(job.getReleaseTime());  //yimei 2019.7.30 move it to above   before routing, the ready time should be set to clocktime

        simulation.addEvent(new OperationVisitEvent(job.getReleaseTime(), operationOption));
        simulation.generateJob();
    }

    @Override
    public void addSequencingDecisionSituation(Simulation simulation,
                                     List<SequencingDecisionSituation> situations,
                                     int minQueueLength) {
        trigger(simulation);
    }

    @Override
    public void addRoutingDecisionSituation(Simulation simulation,
                                               List<RoutingDecisionSituation> situations,
                                               int minQueueLength) {
        //System.out.println("============================here========================");

        //Job has just arrived, get first operation out
        Operation operation = job.getOperation(0);

        //yimei 2019.7.30 set the readytime of each operation to current time. It will be used to calculate operation waiting time in the queue of operation
        //before ther is a bug that all the readytime of operation is 0 and never update---the OWT = system.clocktime
        for (OperationOption op : operation.getOperationOptions())
            op.setReadyTime(job.getReleaseTime());

        RoutingDecisionSituation decisionSituation = new RoutingDecisionSituation(
                operation.getOperationOptions(), simulation.getSystemState());

//        if (operation.getOperationOptions().size() >= minQueueLength) {
//            situations.add(decisionSituation.clone());
//        }

        //fzhang 2019.9.4 in order to get same length matrix
        if (operation.getOperationOptions().size() == minQueueLength) {
            situations.add(decisionSituation.clone());
        }

        OperationOption operationOption =
                simulation.getRoutingRule().nextOperationOption(decisionSituation);
        //operationOption.setReadyTime(job.getReleaseTime());  //yimei 2019.7.30 move it to above

        simulation.addEvent(new OperationVisitEvent(job.getReleaseTime(), operationOption));
        simulation.generateJob();
    }

    @Override
    public String toString() {
        return String.format("%.1f: job %d arrives.\n", time, job.getId());
    }

    @Override
    public int compareTo(AbstractEvent other) {
        if (time < other.time)
            return -1;

        if (time > other.time)
            return 1;

        if (other instanceof JobArrivalEvent) {
            JobArrivalEvent otherJAE = (JobArrivalEvent)other;

            if (job.getId() < otherJAE.job.getId())
                return -1;

            if (job.getId() > otherJAE.job.getId())
                return 1;
        }

        return -1;
    }
}
