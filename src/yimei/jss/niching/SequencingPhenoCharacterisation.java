package yimei.jss.niching;

import ec.gp.GPTree;
import yimei.jss.jobshop.FlexibleStaticInstance;
import yimei.jss.jobshop.OperationOption;
import yimei.jss.rule.AbstractRule;
import yimei.jss.rule.RuleType;
import yimei.jss.rule.operation.evolved.GPRule;
import yimei.jss.rule.operation.weighted.WSPT;
import yimei.jss.rule.workcenter.basic.WIQ;
import yimei.jss.ruleevaluation.MultipleRuleEvaluationModel;
import yimei.jss.simulation.DynamicSimulation;
import yimei.jss.simulation.SequencingDecisionSituation;
import yimei.jss.simulation.StaticSimulation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class SequencingPhenoCharacterisation extends PhenoCharacterisation {
    public List<SequencingDecisionSituation> decisionSituations;
    private int[][] referenceIndexes;
    private static HashMap<GPTree, int[]> CACHE = new HashMap<>();

    public SequencingPhenoCharacterisation(AbstractRule sequencingReferenceRule,
                                           List<SequencingDecisionSituation> decisionSituations) {
        super(sequencingReferenceRule);
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
            SequencingDecisionSituation situation = decisionSituations.get(i);
            List<OperationOption> queue = situation.getQueue();


            // Calculate the priority for all the operations.
            for (OperationOption op : queue) {
                op.setPriority(rule.priority(
                        op, situation.getWorkCenter(), situation.getSystemState()));
            }

            //get the index of best operation
            int idxBestOp = 0;
            for (int j = 0; j < queue.size(); j++) {
                if (queue.get(j).priorTo(queue.get(idxBestOp))) {
                    idxBestOp = j;
                }
            }
            charList[i] = referenceIndexes[i][idxBestOp];
        }

        CACHE.put(((GPRule)rule).getGPTree(), charList);
        return charList;
    }

    protected void calcReferenceIndexes() {
        for (int i = 0; i < decisionSituations.size(); i++) {
            SequencingDecisionSituation situation = decisionSituations.get(i);
            int[] ranks = referenceRule.priorValueOperation(situation);

            //int index = situation.getQueue().indexOf(op);
            referenceIndexes[i] = ranks;
        }
    }

    public static PhenoCharacterisation defaultPhenoCharacterisation() {
        AbstractRule defaultSequencingRule = new WSPT(RuleType.SEQUENCING); //op.getProcTime() / op.getJob().getWeight();
        //the larger the weight, the smaller the WSPT value
        AbstractRule defaultRoutingRule = new WIQ(RuleType.ROUTING);

        //fzhang 2019.6.22 original
     /*   int minQueueLength = 8;
        int numDecisionSituations = 20;*/

        //fzhang 2019.6.22 change to 7, otherwise, can not get this kinds of simulations---because the simulation can not enough queue size as 7
        int minQueueLength = 7;
        int numDecisionSituations = 20;//used for measuring the behavior of different rules

        long shuffleSeed = 8295342;

        DynamicSimulation simulation = DynamicSimulation.standardFull(0, defaultSequencingRule,
                defaultRoutingRule, 10, 500, 0,
                0.95, 4.0); //use this simulation, no warmup jobs? --- here, just to measure the behavior of rule, so need to get a steady state

//        DynamicSimulation simulation = (DynamicSimulation) MultipleRuleEvaluationModel.schedulingSet.getSimulations().get(0);
//        simulation.setRoutingRule(defaultRoutingRule);
//        simulation.setSequencingRule(defaultSequencingRule);

        List<SequencingDecisionSituation> situations = simulation.sequencingDecisionSituations(minQueueLength); //situations have 20 elements
        Collections.shuffle(situations, new Random(shuffleSeed)); //Randomly permute the specified list using the specified source of randomness.
        //randomly change the sorting of list of situations

        situations = situations.subList(0, numDecisionSituations); //Returns a view of the portion of this list between the specified fromIndex,
        //inclusive, and toIndex, exclusive. (If fromIndex and toIndex are equal, the returned list is empty.)
        return new SequencingPhenoCharacterisation(defaultSequencingRule, situations);
    }

    public static PhenoCharacterisation defaultPhenoCharacterisation(String filePath) {
        AbstractRule defaultSequencingRule = new WSPT(RuleType.SEQUENCING);
        AbstractRule defaultRoutingRule = new WIQ(RuleType.ROUTING);
        FlexibleStaticInstance flexibleStaticInstance = FlexibleStaticInstance.readFromAbsPath(filePath);
        StaticSimulation simulation = new StaticSimulation(defaultSequencingRule, defaultRoutingRule,
                flexibleStaticInstance);

    /*    int minQueueLength = 8; // some flexible static instances will have short queues
        int numDecisionSituations = 20;*/

        int minQueueLength = 7; // some flexible static instances will have short queues
        int numDecisionSituations = 100;

        long shuffleSeed = 8295342;

        //the number of sequencing decisions available of a given queue length will vary
        //greatly for different statics instances, so we'll start with 8 and decrease
        //the min queue length until we can get at least 20
        List<SequencingDecisionSituation> situations = simulation.sequencingDecisionSituations(minQueueLength);
        while (situations.size() < numDecisionSituations && minQueueLength > 2) {
            minQueueLength--;
            situations = simulation.sequencingDecisionSituations(minQueueLength);
        }

        if (minQueueLength == 2 && situations.size() < 100) {
            //no point going to queue length of 1, as this will only have 1 outcome
            System.out.println("Only "+situations.size() +" instances available for sequencing pheno characterisation.");
            numDecisionSituations = situations.size();
        }

        Collections.shuffle(situations, new Random(shuffleSeed));

        situations = situations.subList(0, numDecisionSituations);
        return new SequencingPhenoCharacterisation(defaultSequencingRule, situations);
    }

    public List<SequencingDecisionSituation> getDecisionSituations() {
        return decisionSituations;
    }

    public int[][] getReferenceIndexes() {
        return referenceIndexes;
    }
}