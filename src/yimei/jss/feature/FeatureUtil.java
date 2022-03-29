package yimei.jss.feature;

import ec.EvolutionState;
import ec.Fitness;
import ec.Individual;
import ec.Population;
import ec.coevolve.GroupedProblemForm;
import ec.coevolve.MultiPopCoevolutionaryEvaluator;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPTree;
import ec.multiobjective.MultiObjectiveFitness;
import ec.simple.SimpleBreeder;
import ec.simple.SimpleInitializer;
import ec.util.RandomChoice;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import yimei.jss.feature.ignore.Ignorer;
import yimei.jss.gp.GPNodeComparator;
import yimei.jss.gp.GPRuleEvolutionState;
import yimei.jss.gp.TerminalsChangable;
import yimei.jss.gp.terminal.BuildingBlock;
import yimei.jss.gp.terminal.ConstantTerminal;
import yimei.jss.niching.ClearingEvaluator;
import yimei.jss.niching.PhenoCharacterisation;
import yimei.jss.rule.RuleType;
import yimei.jss.rule.operation.evolved.GPRule;
import yimei.jss.ruleoptimisation.RuleOptimizationProblem;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Utility functions for feature selection and construction.
 *
 * Created by YiMei on 5/10/16.
 */

public class FeatureUtil {

    public static RuleType[] ruleTypes = {RuleType.SEQUENCING, RuleType.ROUTING};

    /**
     * Select a diverse set of individuals from the current population.
     * @param state the current evolution state.
     * @param archive the archive from which the set will be chosen.
     * @param n the number of individuals in the diverse set.
     * @return the selected diverse set of individuals.
     */
    public static List<GPIndividual> selectDiverseIndis(EvolutionState state, Individual[] archive,
                                                        int subPopNum, int n) {
        //this is
        Arrays.sort(archive); //ascending order---all the individuals in the subpop

        PhenoCharacterisation pc = null;
        double radius = 0; //niching
        if (state.evaluator instanceof ClearingEvaluator) {
            ClearingEvaluator clearingEvaluator = (ClearingEvaluator) state.evaluator;
            pc = clearingEvaluator.getPhenoCharacterisation()[subPopNum];
            radius = clearingEvaluator.getRadius();
        } else if (state.evaluator instanceof yimei.jss.niching.MultiPopCoevolutionaryClearingEvaluator) {
            yimei.jss.niching.MultiPopCoevolutionaryClearingEvaluator clearingEvaluator =
                    (yimei.jss.niching.MultiPopCoevolutionaryClearingEvaluator) state.evaluator;
            pc = clearingEvaluator.getPhenoCharacterisation()[subPopNum]; //get the sequencing decisions
            radius = clearingEvaluator.getRadius();
        }

        RuleType ruleType = ruleTypes[subPopNum];
        pc.setReferenceRule(new GPRule(ruleType,((GPIndividual)archive[0]).trees[0]));//set the best one to reference rule  //also the smallest one

        List<GPIndividual> selIndis = new ArrayList<>();
        List<int[]> selIndiCharLists = new ArrayList<>();

        for (Individual indi : archive) { //every individual in archive
            boolean tooClose = false;

            GPIndividual gpIndi = (GPIndividual) indi; //check the individual one by one

            int[] charList = pc.characterise(new GPRule(ruleType,gpIndi.trees[0]));//the measured rule

            for (int i = 0; i < selIndis.size(); i++) {
                double distance = PhenoCharacterisation.distance(charList, selIndiCharLists.get(i)); //calculate the distance
                if (distance <= radius) {// radius: the range of each niche
                    tooClose = true; //the code cannot come here
                    break;//terminates the loop (jumps to the code below it)
                }
            }
//			System.out.println(tooClose);
            if (tooClose)
                continue;    //terminates the rest of the processing of the code within the loop for the current iteration, but continues the loop.

            selIndis.add(gpIndi);
            selIndiCharLists.add(charList);

            if (selIndis.size() == n)
                break;
        }

        return selIndis;  //from the best to the worst---small to large
    }

    public static void terminalsInTree(List<GPNode> terminals, GPNode tree) {
        if (tree.depth() == 0) {
            boolean duplicated = false;

            for (GPNode terminal : terminals) {
                if (terminal.toString().equals(tree.toString())) {
                    duplicated = true;
                    break;
                }
            }

            if (!duplicated)
                terminals.add(tree);
        }
        else {
            for (GPNode child : tree.children) {
                terminalsInTree(terminals, child);
            }
        }
    }

    public static List<GPNode> terminalsInTree(GPNode tree) {
        List<GPNode> terminals = new ArrayList<>();
        terminalsInTree(terminals, tree);

        return terminals;
    }

    /**
     * Calculate the contribution of a feature to an individual
     * using the current training set.
     * @param state the current evolution state (training set).
     * @param indi the individual.
     * @param feature the feature.
     * @return the contribution of the feature to the individual.
     */
    public static double contribution(EvolutionState state,
                                      GPIndividual indi,
                                      GPNode feature,
                                      RuleType ruleType) {
        RuleOptimizationProblem problem =
                (RuleOptimizationProblem)state.evaluator.p_problem;
        Ignorer ignorer = ((FeatureIgnorable)state).getIgnorer();

        MultiObjectiveFitness fit1 = (MultiObjectiveFitness) indi.fitness; //[1475.0335218025866]
        MultiObjectiveFitness fit2 = (MultiObjectiveFitness) fit1.clone(); // the same as fit1  1475.0335218025866
        GPRule rule = new GPRule(ruleType,(GPTree)indi.trees[0].clone()); //here, the first time, rule is sequencing rule
        rule.ignore(feature, ignorer);
        int numSubPops = state.population.subpops.length;

        Fitness[] fitnesses = new Fitness[numSubPops];
        GPRule[] rules = new GPRule[numSubPops];
        int index = 0;
        if (ruleType == RuleType.ROUTING) {
            index = 1;
        }

        //It is important that sequencing rule is at [0] and routing rule is at [1]
        //as the CCGP evaluation model is expecting this
        fitnesses[index] = fit2; //fitnesses[0] is 0.34...866
        rules[index] = rule;  //rules[0] = sequencing rule  this rule is already replaced by "1"

        if (numSubPops == 2) {
            //also need to get context of other rule to compare
            RuleType otherRuleType = RuleType.SEQUENCING; //sequencing rule
            int contextIndex = 0;
            if (ruleType == RuleType.SEQUENCING) {
                otherRuleType = RuleType.ROUTING;
                contextIndex = 1;
            }

            GPIndividual contextIndi = (GPIndividual) fit2.context[contextIndex]; //get the corresponding Collaborator
            MultiObjectiveFitness contextFitness = (MultiObjectiveFitness) contextIndi.fitness.clone();
            GPRule contextRule = new GPRule(otherRuleType,
                    (GPTree) (contextIndi).trees[0].clone()); //get the corresponding routing rule
            fitnesses[contextIndex] = contextFitness;  //Collaborator's fitness
            rules[contextIndex] = contextRule; //routing rule
        }
        problem.getEvaluationModel().evaluate(Arrays.asList(fitnesses), Arrays.asList(rules), state);
//        System.out.println(fit2.fitness());
//        System.out.println(fit1.fitness());
//        System.out.println("=====================");
        return fit2.fitness() - fit1.fitness(); //return the max objective fit2: changed one  fit1: original one
        //if the result is larger---> without this feature makes the performance worse ---> then this feature is good;
        //if the result is smaller ---> without this feature makes the performance better---> then this feature is bad.
        //if equal = 0 ---> 1. no this feature   2. have this feature, but not have contribution
    	}

    /**
     * Feature selection by majority voting based on feature contributions.
     * @param state the current evolution state (training set).
     * @param selIndis the selected diverse set of individuals.
     * @param fitUB the upper bound of individual fitness.
     * @param fitLB the lower bound of individual fitness.
     * @return the set of selected features.
     */
    //==================select features and save to .cvs===========================================
    public static GPNode[] featureSelection(EvolutionState state,
                                                List<GPIndividual> selIndis, //selected individuals
                                                RuleType ruleType,
                                                double fitUB, double fitLB) {
        DescriptiveStatistics votingWeightStat = new DescriptiveStatistics();

        for (GPIndividual selIndi : selIndis) { //normalize all the fitnesses of selected individuals

            //fzhang 2019.6.25
            //before here, the normFit is [0,1]
            //=======================start======================
            //double fitness = 1 / (1 + selIndi.fitness.fitness());

            double normFit;
            if(fitUB == fitLB)
                normFit = selIndi.fitness.fitness();
            else
                normFit = (fitUB - selIndi.fitness.fitness()) / (fitUB - fitLB); //selIndi.fitness.fitness() is larger than fitLB
            //========================end=======================

            //fzhang 2019.6.25 original
            //======================start======================
          /*  double normFit = (selIndi.fitness.fitness() - fitLB) / (fitUB - fitLB); //selIndi.fitness.fitness() is larger than fitLB

            if (normFit  < 0)
                normFit = 0;*/
          //=========================end=======================

            double votingWeight = normFit; //set the voting weight to normFit
            votingWeightStat.addValue(votingWeight); //votingWeightStat: save all the norm fitnesses of selected individuals
        }

        double totalVotingWeight = votingWeightStat.getSum(); //the sum of all the normFitness of selected individuals

        List<DescriptiveStatistics> featureContributionStats = new ArrayList<>(); //Maintains a dataset of values of a single variable and computes descriptive statistics based on stored data.
        List<DescriptiveStatistics> featureVotingWeightStats = new ArrayList<>();

        int subPopNum = 0;
        if (ruleType == RuleType.ROUTING) {
            subPopNum = 1;
        }

        GPNode[] terminals = ((TerminalsChangable)state).getTerminals(subPopNum); //terminals here contain all the terminals

        for (int i = 0; i < terminals.length; i++) { //terminals: the terminal we defined (all, in systemstate, we have 25 terminals)
            featureContributionStats.add(new DescriptiveStatistics()); //these to are different
            featureVotingWeightStats.add(new DescriptiveStatistics()); //stats are used to save the information of each feature
        }

        for (int s = 0; s < selIndis.size(); s++) {
            GPIndividual selIndi = selIndis.get(s); // the first selected individual

            for (int i = 0; i < terminals.length; i++) {
                double c = contribution(state, selIndi, terminals[i], ruleType); //terminals[i]: all the terminals
                featureContributionStats.get(i).addValue(c);

                //in this way, if have little difference, we think it is useful.
                //if (c > 0.001) { //original
                if (c > 0) {  //if contribution is larger than 0, actually 0.001, this mean this feature has contribution,
                	//set the weight as fitness value
                    featureVotingWeightStats.get(i).addValue(votingWeightStat.getElement(s));
                }
                else {
                    featureVotingWeightStats.get(i).addValue(0);
                }
            }
        }

        // a another way to get seed value
        // after select features, save the information to .fsinfo.csv
        long jobSeed = ((GPRuleEvolutionState)state).getJobSeed();
        String outputPath = initPath(state);
        File featureInfoFile = new File(outputPath + "job." + jobSeed +
                "-"+ ruleType.name() + ".fsinfo.csv");

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(featureInfoFile));
            writer.write("Feature,Fitness,Contribution,VotingWeights,NormFit,Size");
            writer.newLine();

            for (int i = 0; i < terminals.length; i++) {
                for (int j = 0; j < selIndis.size(); j++) {
                    writer.write(terminals[i].toString() + "," +
                            selIndis.get(j).fitness.fitness() + "," +
                            featureContributionStats.get(i).getElement(j) + "," +
                            featureVotingWeightStats.get(i).getElement(j) + "," +
                            votingWeightStat.getElement(j) + "," +
                            selIndis.get(j).size());
                    writer.newLine();
                }
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<GPNode> selFeatures = new LinkedList<>();

        for (int i = 0; i < terminals.length; i++) {
            double votingWeight = featureVotingWeightStats.get(i).getSum();

            // majority voting
            //if (votingWeight > 0.8 * totalVotingWeight) {
            if (votingWeight > 0.5 * totalVotingWeight) {
                selFeatures.add(terminals[i]);
            }


           //========================start======================
         /*   double threshold = 0.5;
            if(subPopNum == 1)
               threshold = 0.3;

            if (votingWeight > threshold * totalVotingWeight) {
                selFeatures.add(terminals[i]);
            }*/
            //======================end==========================
        }

        File fsFile = new File(outputPath + "job." + jobSeed + "-"
                + ruleType.name() + ".terminals.csv");

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fsFile));

            for (GPNode terminal : selFeatures) {
                writer.write(terminal.toString());
                writer.newLine();
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return selFeatures.toArray(new GPNode[0]);
    }


    /**
     * Feature selection by majority voting based on feature contributions.
     * @param state the current evolution state (training set).
     * @param selIndis the selected diverse set of individuals.
     * @param fitUB the upper bound of individual fitness.
     * @param fitLB the lower bound of individual fitness.
     * @return the set of selected features.
     * fzhang 2019.7.4 change the type of individual parameter from GPIndividual to Individual
     */
    //==================select features and save to .cvs===========================================
    public static GPNode[] featureSelection(EvolutionState state,
                                            Individual[] selIndis, //selected individuals
                                            RuleType ruleType,
                                            double fitUB, double fitLB) {
        DescriptiveStatistics votingWeightStat = new DescriptiveStatistics();

        for (Individual selIndi : selIndis) { //normalize all the fitnesses of selected individuals
            votingWeightStat.addValue(1); //votingWeightStat: save all the norm fitnesses of selected individuals
        }

        double totalVotingWeight = votingWeightStat.getSum(); //the sum of all the normFitness of selected individuals

        List<DescriptiveStatistics> featureContributionStats = new ArrayList<>(); //Maintains a dataset of values of a single variable and computes descriptive statistics based on stored data.
        List<DescriptiveStatistics> featureVotingWeightStats = new ArrayList<>();

        int subPopNum = 0;
        if (ruleType == RuleType.ROUTING) {
            subPopNum = 1;
        }

        GPNode[] terminals = ((TerminalsChangable)state).getTerminals(subPopNum); //terminals here contain all the terminals

        for (int i = 0; i < terminals.length; i++) { //terminals: the terminal we defined (all, in systemstate, we have 25 terminals)
            featureContributionStats.add(new DescriptiveStatistics()); //these to are different
            featureVotingWeightStats.add(new DescriptiveStatistics()); //stats are used to save the information of each feature
        }

        for (int s = 0; s < selIndis.length; s++) {
            Individual selIndi = selIndis[s]; // the first selected individual

            for (int i = 0; i < terminals.length; i++) {
                double c = contribution(state, (GPIndividual) selIndi, terminals[i], ruleType); //terminals[i]: all the terminals
                featureContributionStats.get(i).addValue(c);

                //in this way, if have little difference, we think it is useful.
                //if (c > 0.001) { //original
                if (c > 0) {  //if contribution is larger than 0, actually 0.001, this mean this feature has contribution,
                    //set the weight as fitness value
                    featureVotingWeightStats.get(i).addValue(votingWeightStat.getElement(s));
                }
                else {
                    featureVotingWeightStats.get(i).addValue(0);
                }
            }
        }

        // a another way to get seed value
        // after select features, save the information to .fsinfo.csv
        long jobSeed = ((GPRuleEvolutionState)state).getJobSeed();
        String outputPath = initPath(state);
        File featureInfoFile = new File(outputPath + "job." + jobSeed +
                "-"+ ruleType.name() + ".fsinfo.csv");

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(featureInfoFile));
            writer.write("Feature,Fitness,Contribution,VotingWeights,NormFit,Size");
            writer.newLine();

            for (int i = 0; i < terminals.length; i++) {
                for (int j = 0; j < selIndis.length; j++) {
                    writer.write(terminals[i].toString() + "," +
                            selIndis[j].fitness.fitness() + "," +
                            featureContributionStats.get(i).getElement(j) + "," +
                            featureVotingWeightStats.get(i).getElement(j) + "," +
                            votingWeightStat.getElement(j) + "," +
                            selIndis[j].size());
                    writer.newLine();
                }
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<GPNode> selFeatures = new LinkedList<>();

        for (int i = 0; i < terminals.length; i++) {
            double votingWeight = featureVotingWeightStats.get(i).getSum();

            // majority voting
            //if (votingWeight > 0.8 * totalVotingWeight) {
            if (votingWeight > 0.5 * totalVotingWeight) {
                selFeatures.add(terminals[i]);
            }


            //========================start======================
         /*   double threshold = 0.5;
            if(subPopNum == 1)
               threshold = 0.3;

            if (votingWeight > threshold * totalVotingWeight) {
                selFeatures.add(terminals[i]);
            }*/
            //======================end==========================
        }

        File fsFile = new File(outputPath + "job." + jobSeed + "-"
                + ruleType.name() + ".terminals.csv");

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fsFile));

            for (GPNode terminal : selFeatures) {
                writer.write(terminal.toString());
                writer.newLine();
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return selFeatures.toArray(new GPNode[0]);
    }

    /**
     * Feature construction by majority voting based on contribution.
     * A constructed feature/building block is a depth-2 sub-tree.
     * @param state the current evolution state (training set).
     * @param selIndis the selected diverse set of individuals.
     * @param fitUB the upper bound of individual fitness.
     * @param fitLB the lower bound of individual fitness.
     * @return the constructed features (building blocks).
     */
    public static List<GPNode> featureConstruction(EvolutionState state,
                                                   List<GPIndividual> selIndis,
                                                   RuleType ruleType,
                                                   double fitUB, double fitLB) {
        List<GPNode> BBs = buildingBlocks(selIndis, 2);

        DescriptiveStatistics votingWeightStat = new DescriptiveStatistics();

        for (GPIndividual selIndi : selIndis) {
            double normFit = (selIndi.fitness.fitness() - fitLB) / (fitUB - fitLB);

            if (normFit  < 0)
                normFit = 0;

            double votingWeight = normFit;
            votingWeightStat.addValue(votingWeight);
        }

        double totalVotingWeight = votingWeightStat.getSum();

        List<DescriptiveStatistics> BBContributionStats = new ArrayList<>();
        List<DescriptiveStatistics> BBVotingWeightStats = new ArrayList<>();

        for (int i = 0; i < BBs.size(); i++) {
            BBContributionStats.add(new DescriptiveStatistics());
            BBVotingWeightStats.add(new DescriptiveStatistics());
        }

        for (int s = 0; s < selIndis.size(); s++) {
            GPIndividual selIndi = selIndis.get(s);

            for (int i = 0; i < BBs.size(); i++) {
                double c = contribution(state, selIndi, BBs.get(i), ruleType);
                BBContributionStats.get(i).addValue(c);

                if (c > 0.001) {
                    BBVotingWeightStats.get(i).addValue(votingWeightStat.getElement(s));
                }
                else {
                    BBVotingWeightStats.get(i).addValue(0);
                }
            }
        }

        long jobSeed = ((GPRuleEvolutionState)state).getJobSeed();
        String outputPath = initPath(state);
        File BBInfoFile = new File(outputPath + "job." + jobSeed +
                "-"+ ruleType.name() + ".fcinfo.csv");

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(BBInfoFile));
            writer.write("BB,Fitness,Contribution,VotingWeights,NormFit,Size");
            writer.newLine();

            for (int i = 0; i < BBs.size(); i++) {
                BuildingBlock bb = new BuildingBlock(BBs.get(i));

                for (int j = 0; j < selIndis.size(); j++) {
                    writer.write(bb.toString() + "," +
                            selIndis.get(j).fitness.fitness() + "," +
                            BBContributionStats.get(i).getElement(j) + "," +
                            BBVotingWeightStats.get(i).getElement(j) + "," +
                            votingWeightStat.getElement(j) + "," +
                            selIndis.get(j).size());
                    writer.newLine();
                }
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<GPNode> selBBs = new LinkedList<>();
        for (int i = 0; i < BBs.size(); i++) {
            double votingWeight = BBVotingWeightStats.get(i).getSum();

            // majority voting
            if (votingWeight > 0.5 * totalVotingWeight) {
                selBBs.add(BBs.get(i));
            }
        }

        File fcFile = new File(outputPath + "job." + jobSeed + "-"+ ruleType.name() + ".bbs.csv");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fcFile));

            for (GPNode BB : selBBs) {
                BuildingBlock bb = new BuildingBlock(BB);
                writer.write(bb.toString());
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return selBBs;
    }

    /**
     * Find all the depth-k sub-tree as building blocks from a set of individuals.
     * @param indis the set of individuals.
     * @param depth the depth of the sub-trees/building blocks.
     * @return the building blocks.
     */
    public static List<GPNode> buildingBlocks(List<GPIndividual> indis, int depth) {
        List<GPNode> bbs = new ArrayList<>();

        for (GPIndividual indi : indis) {
            collectBuildingBlocks(bbs, indi.trees[0].child, depth);
        }

        return bbs;
    }

    /**
     * Collect all the depth-k building blocks from a tree.
     * @param buildingBlocks the set of building blocks.
     * @param tree the tree.
     * @param depth the depth of the building blocks.
     */
    public static void collectBuildingBlocks(List<GPNode> buildingBlocks,
                                             GPNode tree,
                                             int depth) {
        if (tree.depth() == depth) {
            boolean duplicate = false;

            for (GPNode bb : buildingBlocks) {
                if (GPNodeComparator.equals(tree, bb)) {
                    duplicate = true;
                    break;
                }
            }

            if (!duplicate)
                buildingBlocks.add(tree);
        }
        else {
            for (GPNode child : tree.children) {
                collectBuildingBlocks(buildingBlocks, child, depth);
            }
        }
    }

    /**
     * Adapt the current population into three parts based on a changed
     * terminal set.
     * @param state the current evolution state (new terminal set).
     * @param fracElites the fraction of elite (directly copy).
     * @param fracAdapted the fraction of adapted (fix the ignored features to 1.0).
     */
    public static void adaptPopulationThreeParts(EvolutionState state,
                                                 double fracElites,
                                                 double fracAdapted,
                                                 int subPopNum) {
        GPNode[] terminals = ((TerminalsChangable)state).getTerminals(subPopNum); //TerminalsChangable whether to change the old individual
        //to 1. If yes, the population will not have old terminals

        //fzhang 27.6.2018  replace/generate the individuals according to cooresponding subpopulation
        Individual[] newPop = state.population.subpops[subPopNum].individuals;
        //Individual[] newPop = state.population.subpops[0].individuals;

        int numElites = (int)(fracElites * newPop.length); //elites: how many individuals to copy directly
        int numAdapted = (int)(fracAdapted * newPop.length); //how many individuals to replace old terminals to 1

        // Sort the individuals from best to worst
        //Arrays.sort(newPop);

        // Part 2: replace the unselected terminals by 1
        for (int i = numElites; i < numElites + numAdapted; i++) {
//			System.out.println("Indi " + i + ", fitness = " + newPop[i].fitness.fitness());
            adaptTree(((GPIndividual)newPop[i]).trees[0].child, terminals);
            newPop[i].evaluated = false;
        }

        // Part 3: reinitialize the remaining individuals
        for (int i = numElites + numAdapted; i < newPop.length; i++) {
//			System.out.println("Indi " + i + ", fitness = " + newPop[i].fitness.fitness());

        	//fzhang 27.6.2018 replace/generate the individuals according to cooresponding subpopulation
            //newPop[i] = state.population.subpops[0].species.newIndividual(state, 0);
        	newPop[i] = state.population.subpops[subPopNum].species.newIndividual(state, 0);
            newPop[i].evaluated = false;
        }
    }


    //fzhang 2019.6.26 adapt individuals based on selected individuals by clustering
    public static void adaptPopulationReplacedByOne(EvolutionState state, Individual[] individuals, int subPopNum){

        Individual[] newPop = state.population.subpops[subPopNum].individuals;
        GPNode[] terminals = ((TerminalsChangable)state).getTerminals(subPopNum); //TerminalsChangable whether to change the old individual

        for(int ind = 0; ind < individuals.length; ind++){
            adaptTree(((GPIndividual)newPop[ind]).trees[0].child, terminals);
            newPop[ind].evaluated = false;
        }

        //randomly initialize other individuals
        for (int i = individuals.length; i < newPop.length; i++) {
            newPop[i] = state.population.subpops[subPopNum].species.newIndividual(state, 0);
            newPop[i].evaluated = false;
        }
    }


    /**
     * Adapt a tree using the new terminal set.
     * @param tree the tree.
     * @param terminals the new terminal set.
     */
    private static void adaptTree(GPNode tree, GPNode[] terminals) {
    	//fzhang  21.6.2018  change the selected terminals to 1
        if (tree.children.length == 0) {
            // It's a terminal
            boolean selected = false;
            for (GPNode terminal : terminals) {
                if (tree.toString().equals(terminal.toString())) {
                    selected = true;
                    break;
                }
            }

            if (!selected) {
                GPNode newTree = new ConstantTerminal(1.0);
                newTree.parent = tree.parent;
                newTree.argposition = tree.argposition;
                if (newTree.parent instanceof GPNode) {
                    ((GPNode)(newTree.parent)).children[newTree.argposition] = newTree;
                }
                else {
                    ((GPTree)(newTree.parent)).child = newTree;
                }
            }
        }
        else {
            for (GPNode child : tree.children) {
                adaptTree(child, terminals);
            }
        }
    }

    private static String initPath(EvolutionState state) {
//        String outputPath = "/Users/dyska/Desktop/Uni/COMP489/GPJSS/out/terminals/";
//        if (state.population.subpops.length == 2) {
//            outputPath += "coevolution/";
//        } else {
//            outputPath += "simple/";
//        }
//        String filePath = state.parameters.getString(new Parameter("filePath"), null);
//        if (filePath == null) {
//            outputPath += "dynamic/";
//        } else {
//            outputPath += "static/";
//        }
//        return outputPath;
        return "";
    }

	public static double[] featureWeighting(EvolutionState state, List<GPIndividual> selIndis, // selected individuals
			RuleType ruleType, double fitUB, double fitLB) {
		DescriptiveStatistics votingWeightStat = new DescriptiveStatistics();

		for (GPIndividual selIndi : selIndis) { // normalize all the fitnesses of selected individuals
			double normFit = (selIndi.fitness.fitness() - fitLB) / (fitUB - fitLB); // selIndi.fitness.fitness() is

			if (normFit < 0)
				normFit = 0;

			double votingWeight = normFit; // set the voting weight to normFit
			votingWeightStat.addValue(votingWeight); // votingWeightStat: save all the norm fitnesses of selected
														// individuals
		}

		List<DescriptiveStatistics> featureContributionStats = new ArrayList<>(); // Maintains a dataset of values of a
																					// single variable and computes
																					// descriptive statistics based on
																					// stored data.
		List<DescriptiveStatistics> featureVotingWeightStats = new ArrayList<>();

		int subPopNum = 0;
		if (ruleType == RuleType.ROUTING) {
			subPopNum = 1;
		}

		GPNode[] terminals = ((TerminalsChangable) state).getTerminals(subPopNum); // terminals here contain all the
																					// terminals

		for (int i = 0; i < terminals.length; i++) { // terminals: the terminal we defined (all, in systemstate, we have
														// 25 terminals)
			featureContributionStats.add(new DescriptiveStatistics()); // these to are different
			featureVotingWeightStats.add(new DescriptiveStatistics()); // stats are used to save the information of each
																		// feature
		}

		//fzhang 2019.5.19 set the contribution as the weighting power for each terminal
		double[] weights = new double[terminals.length];
		for (int s = 0; s < selIndis.size(); s++) {
			GPIndividual selIndi = selIndis.get(s); // the first selected individual

			for (int i = 0; i < terminals.length; i++) {
				double c = contribution(state, selIndi, terminals[i], ruleType); // terminals[i]: all the terminals
				featureContributionStats.get(i).addValue(c);

				// in this way, if have little difference, we think it is useful.
                //if (c > 0.001) { // original code
				if (c > 0) { // if contribution is larger than 0, actually 0.001, this mean this feature has
									// contribution,
					// set the weight as fitness value
					featureVotingWeightStats.get(i).addValue(votingWeightStat.getElement(s));
				} else {
					featureVotingWeightStats.get(i).addValue(0);
				}
			}
		}

		for (int i = 0; i < weights.length; i++) {
			weights[i] = featureVotingWeightStats.get(i).getMean();
		}

		/*for (int j = 0; j < weights.length; j++) {
			System.out.println("The weights are " + weights[j]);
		}*/

		RandomChoice.organizeDistribution(weights); //based on the weights to random choose
		return weights;
	}

    /**
     * based on the information of stage1, and use phenotype to generate new individuals with selected features
     *replace the individuals and then evaluate them directly
     * ---generated new individuals based on phenotype information and copy the rest directly(still have unselected features---not use this one)
     * @param state
     * @param subPopNum
     * fzhang 2019.5.29
     */
    public static void adaptPopulationBasedOnPhenotype(EvolutionState state, double fracElites, int subPopNum) {
        Individual[] newPop = state.population.subpops[subPopNum].individuals;

        Individual[] inds = new Individual[state.population.subpops.length]; // individuals to evaluate together
        boolean[] updates = new boolean[state.population.subpops.length];; // which individual should have its fitness updated as a result

        PhenoCharacterisation pc = null;
        if (state.evaluator instanceof ClearingEvaluator) {
            ClearingEvaluator clearingEvaluator = (ClearingEvaluator) state.evaluator;
            pc = clearingEvaluator.getPhenoCharacterisation()[subPopNum];
        } else if (state.evaluator instanceof yimei.jss.niching.MultiPopCoevolutionaryClearingEvaluator) {
            yimei.jss.niching.MultiPopCoevolutionaryClearingEvaluator clearingEvaluator =
                    (yimei.jss.niching.MultiPopCoevolutionaryClearingEvaluator) state.evaluator;
            pc = clearingEvaluator.getPhenoCharacterisation()[subPopNum];
        }

        RuleType ruleType = ruleTypes[subPopNum];

        for (int ind = 0; ind < newPop.length * fracElites; ind++) {
            GPIndividual gpIndi = (GPIndividual) newPop[ind]; //check the individual one by one
            int[] charList = pc.characterise(new GPRule(ruleType,gpIndi.trees[0]));//the measured rule
            GPIndividual newInd;
            double distance = 0.0;
            int i = 0;
            double tempDistance = Double.MAX_VALUE;
            GPIndividual tempNewInd = null;
            do {
                i++;
                newInd = (GPIndividual) state.population.subpops[subPopNum].species.newIndividual(state,0);
                int[] charListNewInd = pc.characterise(new GPRule(ruleType, newInd.trees[0]));//the measured rule
                distance = PhenoCharacterisation.distance(charList, charListNewInd); //calculate the distance

                if(distance < tempDistance){
                    tempDistance = distance;
                    tempNewInd = newInd;
                }

            } while (distance != 0 && i != 100000);
            //} while (newPop[ind].genotypeToString() != newInd.genotypeToString());
            //read eliteIndividuals for coevolution
            Individual[][] eliteIndividuals = ((MultiPopCoevolutionaryEvaluator)(state.evaluator)).getEliteindividual();
            for (int k = 0; k < eliteIndividuals[subPopNum].length; k++) { //2
                for (int ind1 = 0; ind1 < inds.length; ind1++) { //2
                    if (ind1 == subPopNum) {   //j = 0, 1  (ind j) ---> (0 0) or (1 1) that is to say, this is the subpopulation1
                        inds[ind1] = tempNewInd; //inds[0] = individual = state.population.subpops[0].individuals[0];
                        //the individuals to evaluate together
                        updates[ind1] = true;   // updates[0] = true    updates[1] = true   evaluate
                    }
                    else {  // this is subpopulation2
                        inds[ind1] = eliteIndividuals[ind1][k];   // (ind j) ---> (0 1) or (1 0)
                        updates[ind1] = false;  // do not evaluate
                    }
                }
            }

            //evaluate new individuals
            ((MultiObjectiveFitness)(tempNewInd.fitness)).trials = new ArrayList();//this is always make trials.size == 1, actually useless
            ((GroupedProblemForm)(state.evaluator.p_problem)).evaluate(state, inds
                    , updates // Should the fitness of individuals be updated? Here it says yes and yes.
                    , false
                    , new int[]{0, 1} // Which subpopulation to use? Here we have two subpops and we want to use them both so it should be 0 and 1
                    , 0);// real evaluation

            newPop[ind] = tempNewInd;
            newPop[ind].evaluated = true;
        }
    }

    /**
     * mimic top k% individuals and randomly generate other individuals with selected features
     */
    static ArrayList<Double> savePheDistanceSubPop0 = new ArrayList<>();
    static ArrayList<Double> savePheDistanceSubPop1 = new ArrayList<>();
    public static void adaptPopulationBasedOnPhenotypeWhole(EvolutionState state, double fracElites, int subPopNum) {
        Individual[] newPop = state.population.subpops[subPopNum].individuals;
        int numElites = (int)(fracElites * newPop.length);

        PhenoCharacterisation pc = null;
        if (state.evaluator instanceof ClearingEvaluator) {
            ClearingEvaluator clearingEvaluator = (ClearingEvaluator) state.evaluator;
            pc = clearingEvaluator.getPhenoCharacterisation()[subPopNum];
        } else if (state.evaluator instanceof yimei.jss.niching.MultiPopCoevolutionaryClearingEvaluator) {
            yimei.jss.niching.MultiPopCoevolutionaryClearingEvaluator clearingEvaluator =
                    (yimei.jss.niching.MultiPopCoevolutionaryClearingEvaluator) state.evaluator;
            pc = clearingEvaluator.getPhenoCharacterisation()[subPopNum];
        }

        RuleType ruleType = ruleTypes[subPopNum];

        for (int ind = 0; ind < numElites; ind++) {
            GPIndividual gpIndi = (GPIndividual) newPop[ind]; //check the individual one by one
            int[] charList = pc.characterise(new GPRule(ruleType,gpIndi.trees[0]));//the measured rule
            GPIndividual newInd;
            double distance = 0.0;
            int i = 0;
            double tempDistance = Double.MAX_VALUE;
            GPIndividual tempNewInd = null;
            do {
                i++;
                newInd = (GPIndividual) state.population.subpops[subPopNum].species.newIndividual(state,0);
                int[] charListNewInd = pc.characterise(new GPRule(ruleType, newInd.trees[0]));//the measured rule
                distance = PhenoCharacterisation.distance(charList, charListNewInd); //calculate the distance
//                System.out.println("distance "+ distance);

                if(distance < tempDistance){
                    tempDistance = distance;
                    tempNewInd = newInd;
                }

            } while (distance != 0 && i != 10000);
            //System.out.println(distance);
           /* if(subPopNum == 0)
                savePheDistanceSubPop0.add(distance);
                //savePheDistanceSubPop0[ind] = distance;
            else
                savePheDistanceSubPop1.add(distance);*/

            newPop[ind] = tempNewInd;
            newPop[ind].evaluated = false;
        }

        //randomly initialize other individuals
        for (int i = numElites; i < newPop.length; i++) {
            newPop[i] = state.population.subpops[subPopNum].species.newIndividual(state, 0);
            newPop[i].evaluated = false;
        }
    }

    //save the phenotype distance for measuring the effectiveness of consistence between surrogate and real fitness
    public static void savePheDistance(final EvolutionState state) {
        //fzhang 2019.5.21 save the weight values
        long jobSeed = ((GPRuleEvolutionState)state).getJobSeed();
        File pheDistanceFile = new File("job." + jobSeed + ".pheDistance.csv"); // jobSeed = 0
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(pheDistanceFile));
            writer.write("PheDistanceSubPop0, PheDistanceSubPop1");
            writer.newLine();

            for (int i = 0; i < savePheDistanceSubPop0.size(); i++) { //every two into one generation
                //writer.newLine();
                writer.write(savePheDistanceSubPop0.get(i) + ", ");
                writer.write(+ savePheDistanceSubPop1.get(i) + "\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Double> getPheDistance(int subPopNum) {
        if (subPopNum == 0)
           return savePheDistanceSubPop0;
        else
           return savePheDistanceSubPop1;
    }


    static Population newpop= null;
    public static void adaptPopulationBasedOnPhenotypeGeneticOperator(EvolutionState state, double fracElites, int subPopNum) {

        //Individual[] newPop = state.population.subpops[subPopNum].individuals;
        int numElites = (int)(fracElites * state.population.subpops[subPopNum].individuals.length);
        Individual[] newPop = new Individual[numElites];

        Individual[] inds = new Individual[state.population.subpops.length]; // individuals to evaluate together
        boolean[] updates = new boolean[state.population.subpops.length];; // which individual should have its fitness updated as a result

        PhenoCharacterisation pc = null;
        if (state.evaluator instanceof ClearingEvaluator) {
            ClearingEvaluator clearingEvaluator = (ClearingEvaluator) state.evaluator;
            pc = clearingEvaluator.getPhenoCharacterisation()[subPopNum];
        } else if (state.evaluator instanceof yimei.jss.niching.MultiPopCoevolutionaryClearingEvaluator) {
            yimei.jss.niching.MultiPopCoevolutionaryClearingEvaluator clearingEvaluator =
                    (yimei.jss.niching.MultiPopCoevolutionaryClearingEvaluator) state.evaluator;
            pc = clearingEvaluator.getPhenoCharacterisation()[subPopNum];
        }

        RuleType ruleType = ruleTypes[subPopNum];


        if(subPopNum == 0){
            SimpleInitializer simpleInitializer = new SimpleInitializer();
            newpop = simpleInitializer.setupPopulation(state,0);
        }


        for (int ind = 0; ind < numElites; ind++) {
            GPIndividual gpIndi = (GPIndividual) state.population.subpops[subPopNum].individuals[ind]; //check the individual one by one
            int[] charList = pc.characterise(new GPRule(ruleType,gpIndi.trees[0]));//the measured rule
            GPIndividual newInd;
            double distance = 0.0;
            int i = 0;
            double tempDistance = Double.MAX_VALUE;
            GPIndividual tempNewInd = null;
            do {
                i++;
                newInd = (GPIndividual) state.population.subpops[subPopNum].species.newIndividual(state,0);
                int[] charListNewInd = pc.characterise(new GPRule(ruleType, newInd.trees[0]));//the measured rule
                distance = PhenoCharacterisation.distance(charList, charListNewInd); //calculate the distance

                if(distance < tempDistance){
                    tempDistance = distance;
                    tempNewInd = newInd;
                }

            } while (distance != 0 && i != 10000);
            //System.out.println(distance);
            if(subPopNum == 0)
                savePheDistanceSubPop0.add(distance);
                //savePheDistanceSubPop0[ind] = distance;
            else
                savePheDistanceSubPop1.add(distance);

            Individual[][] eliteIndividuals = ((MultiPopCoevolutionaryEvaluator)(state.evaluator)).getEliteindividual();
            for (int k = 0; k < eliteIndividuals[subPopNum].length; k++) { //2
                for (int ind1 = 0; ind1 < inds.length; ind1++) { //2
                    if (ind1 == subPopNum) {   //j = 0, 1  (ind j) ---> (0 0) or (1 1) that is to say, this is the subpopulation1
                        inds[ind1] = tempNewInd; //inds[0] = individual = state.population.subpops[0].individuals[0];
                        //the individuals to evaluate together
                        updates[ind1] = true;   // updates[0] = true    updates[1] = true   evaluate
                    }
                    else {  // this is subpopulation2
                        inds[ind1] = eliteIndividuals[ind1][k];   // (ind j) ---> (0 1) or (1 0)
                        updates[ind1] = false;  // do not evaluate
                    }
                }
            }

            //evaluate new individuals
            ((MultiObjectiveFitness)(tempNewInd.fitness)).trials = new ArrayList();//this is always make trials.size == 1, actually useless
            ((GroupedProblemForm)(state.evaluator.p_problem)).evaluate(state, inds
                    , updates // Should the fitness of individuals be updated? Here it says yes and yes.
                    , false
                    , new int[]{0, 1} // Which subpopulation to use? Here we have two subpops and we want to use them both so it should be 0 and 1
                    , 0);// real evaluation

            newPop[ind] = tempNewInd;
            newPop[ind].evaluated = true;

            newpop.subpops[subPopNum].individuals[ind] = newPop[ind];
        }

        //now, we got the 20% individuals in the two subpops---newpop
        //fzhang 2019.6.8 generate new individuals based on the top k% individuals by crossover and mutation
        if(subPopNum == 1){
            Population breedpop = breedPopulation(state, newpop); //breedpop get subpop[0].individuals.length individuals
            state.population = breedpop;

            for(int i = 0; i < state.population.subpops.length; i++){
                PopulationThreeParts(state, 1-2*fracElites,i);
            }
        }

/*        if(subPopNum == 1){
            for(int num = 0; num < 1/fracElites-1; num ++){
                Population breedpop = breedPopulation(state, newpop); //breedpop get subpop[0].individuals.length individuals
//                for(int pop = 0; pop < state.population.subpops.length; pop++){
//                    for (int i = numElites*(num+1); i < numElites*(num+2); i++) {
//                        newPop[i] = breedpop.subpops[pop].individuals[i];
//                        newPop[i].evaluated = false;
//                    }
//                }
            }
        }*/
    }

    public static Population breedPopulation(EvolutionState state, Population pop){
        return state.breeder.breedPopulation(state, pop);
    }

    public static void PopulationThreeParts(EvolutionState state,
                                                 double fracAdapted,
                                                 int subPopNum) {

        Individual[] newPop = state.population.subpops[subPopNum].individuals;

        int numAdapted = (int)(fracAdapted * newPop.length); //how many individuals to replace old terminals to 1

        // Part 3: reinitialize the remaining individuals
        for (int i = 0; i < numAdapted; i++) {
            newPop[i] = state.population.subpops[subPopNum].species.newIndividual(state, 0);
            newPop[i].evaluated = false;
        }
    }

    public static Population getNewpop(){
        return newpop;
    }
}
