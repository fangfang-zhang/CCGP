package yimei.jss.simulation;

import yimei.jss.jobshop.OperationOption;
import yimei.jss.simulation.state.SystemState;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dyska on 7/09/17.
 */
public class RoutingDecisionSituation extends DecisionSituation {

    private List<OperationOption> queue;
    private SystemState systemState;

    public RoutingDecisionSituation(List<OperationOption> operationOptions, SystemState systemState) {
        this.queue = operationOptions;
        this.systemState = systemState;
    }

    public List<OperationOption> getQueue() {
        return queue;
    }

    public SystemState getSystemState() {
        return systemState;
    }

    public RoutingDecisionSituation clone() {
        List<OperationOption> clonedQ = new ArrayList<>(queue);
        SystemState clonedState = systemState.clone();

        return new RoutingDecisionSituation(clonedQ, clonedState);
    }
}
