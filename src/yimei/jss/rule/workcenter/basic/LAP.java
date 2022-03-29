package yimei.jss.rule.workcenter.basic;

import yimei.jss.jobshop.OperationOption;
import yimei.jss.jobshop.WorkCenter;
import yimei.jss.rule.AbstractRule;
import yimei.jss.rule.RuleType;
import yimei.jss.simulation.state.SystemState;

/**
 * Created by fzhang on 17/04/18.
 * Average processing time in queue.
 * The priority of this method should be the average processing time in the queue.
 */
public class LAP extends AbstractRule {

    public LAP(RuleType t) {
        name = "\"LAP\"";
        this.type = t;
    }

    @Override
    public double priority(OperationOption op, WorkCenter workCenter, SystemState systemState) {
        return workCenter.getWorkInQueue()/workCenter.getNumMachines();
    }
}

