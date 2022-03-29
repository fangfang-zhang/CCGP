package yimei.jss.niching;

import yimei.jss.rule.AbstractRule;

/**
 * The phenotypic characterisation of rules.
 *
 * Created by YiMei on 3/10/16.
 */
//in this file, we have two class, the first one is PhenoCharacterisation
public abstract class PhenoCharacterisation {
    protected AbstractRule referenceRule;

    public PhenoCharacterisation(AbstractRule referenceRule) {
        this.referenceRule = referenceRule;
    }

    public AbstractRule getReferenceRule() {
        return referenceRule;
    }

    protected abstract void calcReferenceIndexes();

    public void setReferenceRule(AbstractRule rule) {
        this.referenceRule = rule;
        calcReferenceIndexes();
    }



    public abstract int[] characterise(AbstractRule rule);

    //the difference of the two arrays: sqrt
    public static double distance(int[] charList1, int[] charList2) {
        double distance = 0.0;
        for (int i = 0; i < charList1.length; i++) {
            double diff = charList1[i] - charList2[i];
            distance += diff * diff;
        }

        return Math.sqrt(distance);
    }

    //fzhang 2019.9.15
    public static double distance(double[] charList1, double[] charList2) {
        double distance = 0.0;
        for (int i = 0; i < charList1.length; i++) {
            double diff = charList1[i] - charList2[i];
            distance += diff * diff;
        }

        return Math.sqrt(distance);
    }
}

//the second class:  SequencingPhenoCharacterisation  extends the first class
/*class SequencingPhenoCharacterisation extends PhenoCharacterisation {
    public List<SequencingDecisionSituation> decisionSituations;
    private int[] referenceIndexes;

    public SequencingPhenoCharacterisation(AbstractRule sequencingReferenceRule,
                                       List<SequencingDecisionSituation> decisionSituations) {
        super(sequencingReferenceRule);
        this.decisionSituations = decisionSituations;
        this.referenceIndexes = new int[decisionSituations.size()];

        calcReferenceIndexes();
    }

    public int[] characterise(AbstractRule rule) {
        int[] charList = new int[decisionSituations.size()];

        for (int i = 0; i < decisionSituations.size(); i++) {
            SequencingDecisionSituation situation = decisionSituations.get(i);
            List<OperationOption> queue = situation.getQueue();

            int refIdx = referenceIndexes[i]; //[0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]  20 values---20 situations

            // Calculate the priority for all the operations.
            for (OperationOption op : queue) {
                op.setPriority(rule.priority(
                        op, situation.getWorkCenter(), situation.getSystemState()));
            }

            // get the rank of the processing chosen by the reference rule.
            //the operation chosen by reference rule, rank to where by examined rules
            int rank = 1;
            for (int j = 0; j < queue.size(); j++) {
                if (queue.get(j).priorTo(queue.get(refIdx))) {
                    rank ++;
                }
            }

            charList[i] = rank;
        }

        return charList;
    }

    protected void calcReferenceIndexes() {
        for (int i = 0; i < decisionSituations.size(); i++) {
            SequencingDecisionSituation situation = decisionSituations.get(i);
            OperationOption op = referenceRule.priorOperation(situation);
            int index = situation.getQueue().indexOf(op);
            referenceIndexes[i] = index;
        }
    }

    public static PhenoCharacterisation defaultPhenoCharacterisation() {
        AbstractRule defaultSequencingRule = new WSPT(RuleType.SEQUENCING); //op.getProcTime() / op.getJob().getWeight();
        //the larger the weight, the smaller the WSPT value
        AbstractRule defaultRoutingRule = new WIQ(RuleType.ROUTING);

        //fzhang 2019.6.22 original
        int minQueueLength = 8;

        //fzhang 2019.6.22 change to two, otherwise, can not get this kinds of simulations---because ...
        //int minQueueLength = 4;
        int numDecisionSituations = 20;//used for measuring the behavior of different rules
        long shuffleSeed = 8295342;

        *//*DynamicSimulation simulation = DynamicSimulation.standardFull(0, defaultSequencingRule,
                defaultRoutingRule, 10, 500, 0,
                0.95, 4.0); //use this simulation, no warmup jobs? --- here, just to measure the behavior of rule, so need to get a steady state
*//*
        DynamicSimulation simulation = DynamicSimulation.standardFull(0, defaultSequencingRule,
                defaultRoutingRule, 10, 500, 0,
                0.95, 4.0); //use this simulation, no warmup jobs? --- here, just to measure the behavior of rule, so need to get a steady state

        List<SequencingDecisionSituation> situations = simulation.sequencingDecisionSituations(minQueueLength); //situations have 20 elements
        Collections.shuffle(situations, new Random(shuffleSeed)); //Randomly permute the specified list using the specified source ofrandomness.
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

        int minQueueLength = 8; // some flexible static instances will have short queues
        int numDecisionSituations = 20;
        long shuffleSeed = 8295342;

        //the number of sequencing decisions available of a given queue length will vary
        //greatly for different statics instances, so we'll start with 8 and decrease
        //the min queue length until we can get at least 20
        List<SequencingDecisionSituation> situations = simulation.sequencingDecisionSituations(minQueueLength);
        while (situations.size() < numDecisionSituations && minQueueLength > 2) {
            minQueueLength--;
            situations = simulation.sequencingDecisionSituations(minQueueLength);
        }

        if (minQueueLength == 2 && situations.size() < 20) {
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

    public int[] getReferenceIndexes() {
        return referenceIndexes;
    }
}*/

/*
class RoutingPhenoCharacterisation extends PhenoCharacterisation {
    //private List<RoutingDecisionSituation> decisionSituations;
    public List<RoutingDecisionSituation> decisionSituations;
    private int[] referenceIndexes;

    public RoutingPhenoCharacterisation(AbstractRule routingReferenceRule,
                                           List<RoutingDecisionSituation> decisionSituations) {
        super(routingReferenceRule);
        this.decisionSituations = decisionSituations;
        this.referenceIndexes = new int[decisionSituations.size()];

        calcReferenceIndexes();
    }

    public int[] characterise(AbstractRule rule) {
        int[] charList = new int[decisionSituations.size()];

        for (int i = 0; i < decisionSituations.size(); i++) {
        	//this is for routing rule
            RoutingDecisionSituation situation = decisionSituations.get(i);
            List<OperationOption> queue = situation.getQueue();

            int refIdx = referenceIndexes[i];

            // Calculate the priority for all the operations.
            for (OperationOption op : queue) {
                op.setPriority(rule.priority(
                        op, op.getWorkCenter(), situation.getSystemState()));
            }
            // get the rank of the processing chosen by the reference rule.
            int rank = 1;
            for (int j = 0; j < queue.size(); j++) {
                if (queue.get(j).priorTo(queue.get(refIdx))) {
                    rank ++;
                }
            }
            charList[i] = rank;
        }
        return charList;
    }

    protected void calcReferenceIndexes() {
        for (int i = 0; i < decisionSituations.size(); i++) {
            RoutingDecisionSituation situation = decisionSituations.get(i);
            OperationOption op = referenceRule.nextOperationOption(situation);
            int index = situation.getQueue().indexOf(op);
            referenceIndexes[i] = index;
        }
    }

    public static PhenoCharacterisation defaultPhenoCharacterisation() {
        AbstractRule defaultSequencingRule = new WSPT(RuleType.SEQUENCING);
        AbstractRule defaultRoutingRule = new WIQ(RuleType.ROUTING);

        //fzhang 2019.6.22 original code
        //int minQueueLength = 8; //original setting

        //fzhang 2019.6.22
        int minQueueLength = 8; //because we only have five machines, so here at most 5 machines, otherwise there will be no routing scenarios
        int numDecisionSituations = 20;
        long shuffleSeed = 8295342;

        DynamicSimulation simulation = DynamicSimulation.standardFull(0, defaultSequencingRule,
                defaultRoutingRule, 10, 500, 0,
                0.95, 4.0);

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

        int minQueueLength = 8;
        int numDecisionSituations = 20;
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

    public int[] getReferenceIndexes() {
        return referenceIndexes;
    }
}
*/
