package yimei.jss.jobshop;

import yimei.jss.rule.AbstractRule;
import yimei.jss.rule.RuleType;
import yimei.jss.rule.workcenter.basic.WIQ;
import yimei.jss.simulation.DynamicSimulation;
import yimei.jss.simulation.RoutingDecisionSituation;
import yimei.jss.simulation.state.SystemState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by yimei on 22/09/16.
 */
public class Operation {
    private final Job job;
    private final int id;
    private List<OperationOption> operationOptions;
    private Operation next;

    public Operation(Job job, int id) {
        this.job = job;
        this.id = id;
        this.operationOptions = new ArrayList<>();
    }

    public Operation(Job job, int id, double procTime, WorkCenter workCenter) {
        this.job = job;
        this.id = id;
        this.operationOptions = new ArrayList<>();
        this.next = null;
        operationOptions.add(new OperationOption(this,
                operationOptions.size()+1,procTime,workCenter));
    }

    public String toString() {
        String msg = "";
        for (OperationOption option: operationOptions) {
            msg += String.format("[J%d O%d-%d, W%d, T%.1f], ",
                    job.getId(), id, option.getOptionId(), option.getWorkCenter().getId(), option.getProcTime());
        }
        msg = msg.substring(0, msg.length()-2);
        msg += "\n";
        return msg;
    }

    public Job getJob() {
        return job;
    }

    public int getId() {
        return id;
    }

    public void setNext(Operation next) {this.next = next; }

    public void setOperationOptions (List<OperationOption> operationOptions) {this.operationOptions = operationOptions; }

    public Operation getNext() { return next; }

    public List<OperationOption> getOperationOptions() { return operationOptions; }

    public void addOperationOption(OperationOption option) {
        operationOptions.add(option);
    }

    //modified by fzhang  28.5.2018  get the min workload among candiate machines for the next operation
    public double getLeastWorkLoad() {
    	double leastWorkLoad = getOperationOptions().get(0).getWorkCenter().getWorkInQueue();
    	for (int j = 1; j <  getOperationOptions().size(); j++) {
    		if(getOperationOptions().get(j).getWorkCenter().getWorkInQueue() < leastWorkLoad)
    			leastWorkLoad = getOperationOptions().get(j).getWorkCenter().getWorkInQueue();
        }
    	return leastWorkLoad;
    }

    //modified by fzhang  28.5.2018  get the max workload among candiate machines for the next operation
    public double getMaxWorkLoad() {
    	double maxWorkLoad = getOperationOptions().get(0).getWorkCenter().getWorkInQueue();
    	for (int j = 1; j < getOperationOptions().size(); j++) {
    		if(getOperationOptions().get(j).getWorkCenter().getWorkInQueue() > maxWorkLoad)
    			maxWorkLoad = getOperationOptions().get(j).getWorkCenter().getWorkInQueue();
        }
    	return maxWorkLoad;
    }

    //modified by fzhang  28.5.2018  get the average workload among candiate machines for the next operation
    public double getAveWorkLoad() {
    	double totalWorkLoad = 0;
    	for (int j = 0; j < getOperationOptions().size(); j++) {
    		totalWorkLoad += getOperationOptions().get(j).getWorkCenter().getWorkInQueue();
        }
    	return totalWorkLoad/getOperationOptions().size();
    }
//==========================================================================================================
    //modified by fzhang  28.5.2018  get the min number of operation among candiate machines for the next operation
    public double getLeastNumOfOperation() {
    	double leastNumOfOperation = getOperationOptions().get(0).getWorkCenter().getNumOpsInQueue();
    	for (int j = 1; j <  getOperationOptions().size(); j++) {
    		if(getOperationOptions().get(j).getWorkCenter().getNumOpsInQueue() < leastNumOfOperation)
    			leastNumOfOperation = getOperationOptions().get(j).getWorkCenter().getNumOpsInQueue();
        }
    	return leastNumOfOperation;
    }

    //modified by fzhang  28.5.2018  get the max number of operation among candiate machines for the next operation
    public double getMaxNumOfOperation() {
    	double maxNumOfOperation = getOperationOptions().get(0).getWorkCenter().getNumOpsInQueue();
    	for (int j = 1; j < getOperationOptions().size(); j++) {
    		if(getOperationOptions().get(j).getWorkCenter().getNumOpsInQueue() > maxNumOfOperation)
    			maxNumOfOperation = getOperationOptions().get(j).getWorkCenter().getNumOpsInQueue();
        }
    	return maxNumOfOperation;
    }

    //modified by fzhang  28.5.2018  get the average number of operation among candiate machines for the next operation
    public double getAveNumOfOperation() {
    	double totalNumOfOperation = 0;
    	for (int j = 0; j < getOperationOptions().size(); j++) {
    		totalNumOfOperation += getOperationOptions().get(j).getWorkCenter().getNumOpsInQueue();
        }
    	return totalNumOfOperation/getOperationOptions().size();
    }

    //=========================================================================================
  //modified by fzhang  28.5.2018  get the min number of operation among candiate machines for the next operation
    public double getLeastProcessTime() {
    	double leastProcessTime = getOperationOptions().get(0).getProcTime();
    	for (int j = 1; j <  getOperationOptions().size(); j++) {
    		if(getOperationOptions().get(j).getProcTime() < leastProcessTime)
    			leastProcessTime = getOperationOptions().get(j).getProcTime();
        }
    	return leastProcessTime;
    }

    //modified by fzhang  28.5.2018  get the max number of operation among candiate machines for the next operation
    public double getMaxProcessTime() {
    	double maxProcessTime = getOperationOptions().get(0).getProcTime();

    	for (int j = 1; j < getOperationOptions().size(); j++) {
    		if(getOperationOptions().get(j).getProcTime() > maxProcessTime)
    			maxProcessTime = getOperationOptions().get(j).getProcTime();
        }
    	return maxProcessTime;
    }

    //modified by fzhang  28.5.2018  get the average number of operation among candiate machines for the next operation
    public double getMedianProcessTime() {
    	List<Double> totalProcessTime = new ArrayList<Double>();
    	for (int j = 0; j < getOperationOptions().size(); j++) {
    		totalProcessTime.add(getOperationOptions().get(j).getProcTime());
        }
    	return getMedian(totalProcessTime);
    }

    public double getMedian(List<Double> arraylist){

    	Collections.sort(arraylist);
        double median;
            if (arraylist.size()%2 == 0) {
            	median = (arraylist.get(arraylist.size()/2)+ arraylist.get(arraylist.size()/2 - 1))/2;
            } else {
            	median = arraylist.get((arraylist.size()-1)/2);
            }
          return median;
    }

    /*
    This method is to be called before a simulation has begun and additional information
    has been made availble. It will simply return the OperationOption with the highest
    procedure time, aka the most pessimistic procedure time guess.
     */
    public OperationOption getOperationOption() {
        double highestProcTime = Double.NEGATIVE_INFINITY;
        OperationOption best = null;
        for (OperationOption option: operationOptions) {
            if (option.getProcTime() > highestProcTime || highestProcTime == Double.NEGATIVE_INFINITY) {
                highestProcTime = option.getProcTime();
                best = option;
            }
        }
        return best;
    }

    // use routing rule to decide which option we will choose
    public OperationOption chooseOperationOption(SystemState systemState, AbstractRule routingRule) {

    	RoutingDecisionSituation decisionSituation = routingDecisionSituation(systemState);

        if (routingRule == null) {
            routingRule = new WIQ(RuleType.ROUTING);
        }
        return routingRule.nextOperationOption(decisionSituation);
    }

    public RoutingDecisionSituation routingDecisionSituation(SystemState systemState) {
        return new RoutingDecisionSituation(operationOptions,systemState);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Operation operation = (Operation) o;

        if (id != operation.id) return false;
        if (job != null ? !job.equals(operation.job) : operation.job != null) return false;
        if (operationOptions != null ? !operationOptions.equals(operation.operationOptions) : operation.operationOptions != null)
            return false;
        return next != null ? next.equals(operation.next) : operation.next == null;
    }

    @Override
    public int hashCode() {
        int result = job != null ? job.hashCode() : 0;
        result = 31 * result + id;
        result = 31 * result + (operationOptions != null ? operationOptions.hashCode() : 0);
        result = 31 * result + (next != null ? next.hashCode() : 0);
        return result;
    }
}