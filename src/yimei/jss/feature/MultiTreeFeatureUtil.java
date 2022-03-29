package yimei.jss.feature;

import ec.EvolutionState;
import ec.Fitness;
import ec.Individual;
import ec.app.tutorial4.Mul;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPTree;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Parameter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import yimei.jss.feature.ignore.Ignorer;
import yimei.jss.gp.GPNodeComparator;
import yimei.jss.gp.GPRuleEvolutionState;
import yimei.jss.gp.TerminalsChangable;
import yimei.jss.gp.terminal.BuildingBlock;
import yimei.jss.gp.terminal.ConstantTerminal;
import yimei.jss.niching.ClearingEvaluator;
import yimei.jss.niching.MultiPopCoevolutionaryClearingEvaluator;
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

public class MultiTreeFeatureUtil {

    public static RuleType[] ruleTypes = {RuleType.SEQUENCING, RuleType.ROUTING};

    /**
     * Select a diverse set of individuals from the current population.
     * @param state the current evolution state.
     * @param archive the archive from which the set will be chosen.
     * @param n the number of individuals in the diverse set.
     * @return the selected diverse set of individuals.
     */
    public static List<GPIndividual> selectDiverseIndis(EvolutionState state, Individual[] archive,
                                                        int numTrees, int n) {
        //archive: all the individuals in the population  from the smallest to largest
    	Arrays.sort(archive);

        PhenoCharacterisation pc = null;
        double radius = 0;
        //get the different type of PhenoCharacterisation: sequencing or routing
        if (state.evaluator instanceof ClearingEvaluator) {
            ClearingEvaluator clearingEvaluator = (ClearingEvaluator) state.evaluator;
            pc = clearingEvaluator.getPhenoCharacterisation()[numTrees];
            radius = clearingEvaluator.getRadius();
        } else if (state.evaluator instanceof MultiPopCoevolutionaryClearingEvaluator) {
            MultiPopCoevolutionaryClearingEvaluator clearingEvaluator =
                    (MultiPopCoevolutionaryClearingEvaluator) state.evaluator;
            //this part is useless here
            pc = clearingEvaluator.getPhenoCharacterisation()[numTrees];
            radius = clearingEvaluator.getRadius();
        }
        
        RuleType ruleType = ruleTypes[numTrees];
        pc.setReferenceRule(new GPRule(ruleType,((GPIndividual)archive[0]).trees[numTrees]));//set the best one to reference rule
        
        //pc.setReferenceRule(new GPRule(ruleType,((GPIndividual)archive[0]).trees[0]));//set the best one to reference rule

        List<GPIndividual> selIndis = new ArrayList<>();
        List<int[]> selIndiCharLists = new ArrayList<>();

        for (Individual indi : archive) { //every individual in archive 
            boolean tooClose = false;

            GPIndividual gpIndi = (GPIndividual) indi;

            //return the distance   charList contains 1 (count 20)
            int[] charList = pc.characterise(new GPRule(ruleType,gpIndi.trees[numTrees]));
            //int[] charList = pc.characterise(new GPRule(ruleType,gpIndi.trees[0]));

            for (int i = 0; i < selIndis.size(); i++) {
                double distance = PhenoCharacterisation.distance(charList, selIndiCharLists.get(i)); //calculate the distance
                if (distance <= radius) {// radius: the range of each niche  
                	//only when distance is zero, they will be too close.  so normally, the selected individuals are always different.
                    //but if you set radius as a larger number, it means we have more chance to choose some bad individuals
                	tooClose = true;
                    break;
                }
            }

            if (tooClose)
                continue;

            selIndis.add(gpIndi);
            selIndiCharLists.add(charList);

            if (selIndis.size() == n)
                break;
        }

        return selIndis;
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
        //fzhang 15.7.2018 use multi-tree structure
        RuleOptimizationProblem problem =
                (RuleOptimizationProblem)state.evaluator.p_problem;
        Ignorer ignorer = ((FeatureIgnorable)state).getIgnorer();

        //record to fitness to calculate the difference 
        MultiObjectiveFitness fit1 = (MultiObjectiveFitness) indi.fitness;
        MultiObjectiveFitness fit2 = (MultiObjectiveFitness) fit1.clone(); // the same as fit1
        
        GPRule rule = new GPRule(ruleType,(GPTree)indi.trees[0].clone()); //here, the first time, rule is sequencing rule
        GPRule contextRule = new GPRule(ruleType,(GPTree)indi.trees[1].clone()); //here, the first time, rule is sequencing rule
        int index = 0;
        int contextIndex = 1;
        
		if (ruleType == RuleType.ROUTING) {
			rule = new GPRule(ruleType, (GPTree) indi.trees[1].clone()); // here, the first time, rule is sequencing rule
			contextRule = new GPRule(ruleType,(GPTree)indi.trees[0].clone()); //here, the first time, rule is sequencing rule
			index = 1;
			contextIndex = 0;
		}

        rule.ignore(feature, ignorer);
        
		//better to read this value from parameter
        int numTrees = 2;
       
        //Fitness[] fitnesses = new Fitness[numTrees];
        Fitness[] fitnesses = new Fitness[numTrees];
        GPRule[] rules = new GPRule[numTrees];

        //It is important that sequencing rule is at [0] and routing rule is at [1]
        //as the CCGP evaluation model is expecting this
        fitnesses[index] = fit2; //fitnesses[0] is 0.34...866
        rules[index] = rule;  //rules[0] = sequencing rule

        if (numTrees == 2) {
        	rules[index] = rule;
            rules[contextIndex] = contextRule; //routing rule
        }
        
        problem.getEvaluationModel().evaluate(Arrays.asList(fitnesses), Arrays.asList(rules), state);

        return fit2.fitness() - fit1.fitness(); //return the max objective
    
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
            double normFit = (selIndi.fitness.fitness() - fitLB) / (fitUB - fitLB);
            //System.out.println("sequencing ");
            //System.out.println(normFit);
            if (normFit  < 0)
                normFit = 0;

            double votingWeight = normFit; //set the voting weight to normFit
            votingWeightStat.addValue(votingWeight); //votingWeightStat: save all the norm fitnesses of selected individuals
        }

        double totalVotingWeight = votingWeightStat.getSum(); //the sum of all the normFitness of selected individuals

        List<DescriptiveStatistics> featureContributionStats = new ArrayList<>();
        List<DescriptiveStatistics> featureVotingWeightStats = new ArrayList<>();

        int numTrees = 0;
        if (ruleType == RuleType.ROUTING) {
        	numTrees = 1;
        }

        GPNode[] terminals = ((TerminalsChangable)state).getTerminals(0); //terminals here contain all the terminals

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
                if (c > 0.001) {  //if contribution is larger than 0, actually 0.001, this mean this feature has contribution,
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
            if (votingWeight > 0.5 * totalVotingWeight) {
                selFeatures.add(terminals[i]);
            }
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
                                                 int numTrees) {
        GPNode[] tree0Terminals = ((TerminalsChangable)state).getTerminals(0); //TerminalsChangable whether to change the old individual
        GPNode[] tree1Terminals = ((TerminalsChangable)state).getTerminals(1);
        //to 1. If yes, the population will not have old terminals
		/*for (GPNode sel : terminals) {
			System.out.println(subPopNum);
			System.out.println(sel);
		}*/
        
        //fzhang 27.6.2018  replace/generate the individuals according to cooresponding subpopulation
        Individual[] newPop = state.population.subpops[0].individuals;
        //Individual[] newPop = state.population.subpops[0].individuals;
        
        int numElites = (int)(fracElites * newPop.length); //elites: how many individuals to copy directly
        int numAdapted = (int)(fracAdapted * newPop.length); //how many individuals to replace old terminals to 1

     /*   System.out.println(numElites);  //5
        System.out.println(numAdapted); //51
*/        
        // Sort the individuals from best to worst
        Arrays.sort(newPop);

        // Part 1: keep the elites from 0 to numElite-1
        // for this part, we do not need to do anything, the new population have generated and sorted. The first five elites 
        // already there
      /*  for (int i = 0; i < numElites; i++) {
			System.out.println("Indi " + i + ", fitness = " + newPop[i].fitness.fitness());
           }*/
        // Part 2: replace the unselected terminals by 1
        for (int i = numElites; i < numElites + numAdapted; i++) {
//			System.out.println("Indi " + i + ", fitness = " + newPop[i].fitness.fitness());
            adaptTree(((GPIndividual)newPop[i]).trees[0].child, tree0Terminals);
            adaptTree(((GPIndividual)newPop[i]).trees[1].child, tree1Terminals);
            newPop[i].evaluated = false;
        }

        // Part 3: reinitialize the remaining individuals
        for (int i = numElites + numAdapted; i < newPop.length; i++) {
//			System.out.println("Indi " + i + ", fitness = " + newPop[i].fitness.fitness());
        	
        	//fzhang 27.6.2018 replace/generate the individuals according to cooresponding subpopulation
            //newPop[i] = state.population.subpops[0].species.newIndividual(state, 0);
        	newPop[i] = state.population.subpops[0].species.newIndividual(state, 0);
            
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
}
