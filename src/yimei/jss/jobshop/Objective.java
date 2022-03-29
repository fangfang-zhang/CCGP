package yimei.jss.jobshop;

import yimei.jss.rule.AbstractRule;
import yimei.jss.rule.RuleType;
import yimei.jss.rule.operation.basic.EDD;
import yimei.jss.rule.operation.basic.FCFS;
import yimei.jss.rule.operation.basic.SPT;
import yimei.jss.rule.operation.composite.ATC;
import yimei.jss.rule.operation.weighted.WATC;
import yimei.jss.rule.operation.weighted.WSPT;
import yimei.jss.rule.workcenter.basic.WIQ;


import java.util.HashMap;
import java.util.Map;

/**
 * The enumeration of all the objectives that may be optimised in job shop scheduling.
 * All the objectives are assumed to be minimised.
 *
 * Created by yimei on 28/09/16.
 *
 */
public enum Objective {

    MAKESPAN("makespan"),
    MEAN_FLOWTIME("mean-flowtime"),
    MAX_FLOWTIME("max-flowtime"),
    MEAN_TARDINESS("mean-tardiness"),
    MAX_TARDINESS("max-tardiness"),
    MEAN_WEIGHTED_FLOWTIME("mean-weighted-flowtime"),
    MAX_WEIGHTED_FLOWTIME("max-weighted-flowtime"),
    MEAN_WEIGHTED_TARDINESS("mean-weighted-tardiness"),
    MAX_WEIGHTED_TARDINESS("max-weighted-tardiness"),
    PROP_TARDY_JOBS("prop-tardy-jobs"),
	
	//2018.12.20 define rule size as an objective
	RULESIZE("rulesize"),
	
	//2019.2.26 define routing rule size as an objective
	RULESIZER("rulesizeR"),
	
	//2019.2.26 define sequencing rule size as an objective
    RULESIZES("rulesizeS");

    private final String name;

    Objective(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // Reverse-lookup map
    private static final Map<String, Objective> lookup = new HashMap<>();

    static {
        for (Objective a : Objective.values()) {
            lookup.put(a.getName(), a);
        }
    }

    public static Objective get(String name) {
        return lookup.get(name);
    }

    public AbstractRule benchmarkSequencingRule() {
        switch (this) {
            case MAKESPAN:
                return new FCFS(RuleType.SEQUENCING);
            case MEAN_FLOWTIME:
                return new SPT(RuleType.SEQUENCING);
            case MAX_FLOWTIME:
                return new FCFS(RuleType.SEQUENCING);
            case MEAN_WEIGHTED_FLOWTIME:
                return new WSPT(RuleType.SEQUENCING);
//                return new TwoPTplusWINQplusNPT(RuleType.SEQUENCING);
            case MEAN_TARDINESS:
                return new ATC(RuleType.SEQUENCING);
            case MEAN_WEIGHTED_TARDINESS:
                return new WATC(RuleType.SEQUENCING);
            case MAX_TARDINESS:
                return new EDD(RuleType.SEQUENCING);
        }

        return null;
    }

    public AbstractRule benchmarkRoutingRule() {
        return new WIQ(RuleType.ROUTING);
    }
}
