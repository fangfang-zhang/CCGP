package yimei.jss.niching;

import ec.gp.GPTree;
import yimei.jss.ruleevaluation.MultipleRuleEvaluationModel;
import yimei.jss.jobshop.FlexibleStaticInstance;
import yimei.jss.jobshop.OperationOption;
import yimei.jss.rule.AbstractRule;
import yimei.jss.rule.RuleType;
import yimei.jss.rule.operation.evolved.GPRule;
import yimei.jss.rule.operation.weighted.WSPT;
import yimei.jss.rule.workcenter.basic.WIQ;
import yimei.jss.simulation.DynamicSimulation;
import yimei.jss.simulation.RoutingDecisionSituation;
import yimei.jss.simulation.StaticSimulation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class RoutingPhenoCharacterisation extends PhenoCharacterisation {
    //private List<RoutingDecisionSituation> decisionSituations;
    public List<RoutingDecisionSituation> decisionSituations;
    private int[][] referenceIndexes;

    private static HashMap<GPTree, int[]> CACHE = new HashMap<>();

    public RoutingPhenoCharacterisation(AbstractRule routingReferenceRule,
                                        List<RoutingDecisionSituation> decisionSituations) {
        super(routingReferenceRule);
        this.decisionSituations = decisionSituations;
        this.referenceIndexes = new int[decisionSituations.size()][];

        calcReferenceIndexes();
    }

    public int[] characterise(AbstractRule rule) {
        if(CACHE.containsKey(((GPRule)rule).getGPTree()))
            return CACHE.get(((GPRule)rule).getGPTree());
        if(CACHE.size() > 10000)
            CACHE.clear();

        int[] charList = new int[decisionSituations.size()];

        for (int i = 0; i < decisionSituations.size(); i++) {
            //this is for routing rule
            RoutingDecisionSituation situation = decisionSituations.get(i);
            List<OperationOption> queue = situation.getQueue();

            //int refIdx = referenceIndexes[i];

            // Calculate the priority for all the operations.
            for (OperationOption op : queue) {
                op.setPriority(rule.priority(
                        op, op.getWorkCenter(), situation.getSystemState()));
            }
            // get the rank of the processing chosen by the reference rule.
            int idxBestOption = 0;
            for (int j = 0; j < queue.size(); j++) {
                if (queue.get(j).priorTo(queue.get(idxBestOption))) {
                    idxBestOption = j;
                }
            }
            charList[i] = referenceIndexes[i][idxBestOption];
        }

        CACHE.put(((GPRule)rule).getGPTree(), charList);
        return charList;
    }

    protected void calcReferenceIndexes() {
        for (int i = 0; i < decisionSituations.size(); i++) {
            RoutingDecisionSituation situation = decisionSituations.get(i);
            int[] ranks = referenceRule.priorValueOperationMahcine(situation);
            referenceIndexes[i] = ranks;
        }
    }

    public static PhenoCharacterisation defaultPhenoCharacterisation() {
        //2021.7.27 this only is visited once. All the generations will use the same decision situations.
        AbstractRule defaultSequencingRule = new WSPT(RuleType.SEQUENCING);
        AbstractRule defaultRoutingRule = new WIQ(RuleType.ROUTING);

        //fzhang 2019.6.22 original code
   /*     int minQueueLength = 8; //original setting
        int numDecisionSituations = 20;*/

        //fzhang 2019.6.22
        int minQueueLength = 7; //because we only have five machines, so here at most 5 machines, otherwise there will be no routing scenarios
        int numDecisionSituations = 20;

        long shuffleSeed = 8295342;

        DynamicSimulation simulation = DynamicSimulation.standardMissing(0, defaultSequencingRule,
                defaultRoutingRule, 10, 500, 0,
                0.99, 4.0);

//        DynamicSimulation simulation = (DynamicSimulation) MultipleRuleEvaluationModel.schedulingSet.getSimulations().get(0);
//        simulation.setRoutingRule(defaultRoutingRule);
//        simulation.setSequencingRule(defaultSequencingRule);

        List<RoutingDecisionSituation> situations = simulation.routingDecisionSituations(minQueueLength);
        Collections.shuffle(situations, new Random(shuffleSeed));

        situations = situations.subList(0, numDecisionSituations);
        return new RoutingPhenoCharacterisation(defaultRoutingRule, situations);
    }

    public static PhenoCharacterisation defaultPhenoCharacterisation(String filePath) {
        AbstractRule defaultSequencingRule = new WSPT(RuleType.SEQUENCING);
        AbstractRule defaultRoutingRule = new WIQ(RuleType.ROUTING);
        FlexibleStaticInstance flexibleStaticInstance = FlexibleStaticInstance.readFromAbsPath(filePath);
        StaticSimulation simulation = new StaticSimulation(defaultSequencingRule, defaultRoutingRule,
                flexibleStaticInstance);

   /*     int minQueueLength = 8;
        int numDecisionSituations = 20;*/

        int minQueueLength = 7; //because we only have five machines, so here at most 5 machines, otherwise there will be no routing scenarios
        int numDecisionSituations = 100;

        long shuffleSeed = 8295342;

        List<RoutingDecisionSituation> situations = simulation.routingDecisionSituations(minQueueLength);
        while (situations.size() < numDecisionSituations && minQueueLength > 2) {
            minQueueLength--;
            situations = simulation.routingDecisionSituations(minQueueLength);
        }

        if (minQueueLength == 2 && situations.size() < numDecisionSituations) {
            //no point going to queue length of 1, as this will only have 1 outcome
            System.out.println("Only "+situations.size() +" instances available for routing pheno characterisation.");
            numDecisionSituations = situations.size();
        }

        Collections.shuffle(situations, new Random(shuffleSeed));


        situations = situations.subList(0, numDecisionSituations);
        return new RoutingPhenoCharacterisation(defaultRoutingRule, situations);
    }

    public List<RoutingDecisionSituation> getDecisionSituations() {
        return decisionSituations;
    }

    public int[][] getReferenceIndexes() {
        return referenceIndexes;
    }

}