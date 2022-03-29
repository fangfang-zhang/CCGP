/*package yimei.jss.rule.workcenter.basic;

import yimei.jss.jobshop.OperationOption;
import yimei.jss.jobshop.WorkCenter;
import yimei.jss.rule.AbstractRule;
import yimei.jss.rule.RuleType;
import yimei.jss.simulation.state.SystemState;

*//**
 * Created by dyska on 6/06/17.
 * Least aggregate cost and process time in queue.
 * The priority of this method should be the amount of work in the queue.
 *//*
public class LACP extends AbstractRule {

    public LACP(RuleType t) {
        name = "\"LACP\"";
        this.type = t;
    }

    @Override
    public double priority(OperationOption op, WorkCenter workCenter, SystemState systemState) {
        return workCenter.getAverageCostInQueue()/workCenter.getTotalAverageCostInQueue() +
        		workCenter.getTotalAverageProcesTimeInQueue()/workCenter.getTotalAverageProcesTimeInQueue();
    }
}
*/