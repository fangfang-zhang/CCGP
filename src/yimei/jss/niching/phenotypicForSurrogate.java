package yimei.jss.niching;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.util.Parameter;
import org.apache.commons.lang3.ArrayUtils;
import yimei.jss.rule.RuleType;
import yimei.jss.rule.operation.evolved.GPRule;

import java.util.ArrayList;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
2019.9.24 calculate the (phenotypy characteristic) PC of each individuals in each population
 not used in weighted feature idea
 */
public class phenotypicForSurrogate {
    static double[][][] indsCharLists = null; //save the PC information
    static double[][] indsCharListsMultiTree = null;
    public static final String P_TREESIZE = "num-trees";
    public static int numTrees;
    static GPRule[] benckmarkRule = new GPRule[2];

    static int[][] oneIndCharLists = null; //save the PC information
    static int[] oneIndCharListsMultiTree = null;

    //previous PC calculation
   /* public static double[][] phenotypicPopulation(final EvolutionState state,
                                       PhenoCharacterisation[] pc) {

        RuleType[] ruleTypes = {RuleType.SEQUENCING, RuleType.ROUTING}; //ruleType is an array
        numTrees = state.parameters.getIntWithDefault(new Parameter(P_TREESIZE), null, 1);

        indsCharLists = new double[numTrees][(int)(state.population.subpops[0].individuals.length)][];

        for (int treeID = 0; treeID < numTrees; treeID++) {//each population
            RuleType ruleType = ruleTypes[treeID];  //ruleType is a rule type---ruleType[0] = SEQUENCING  ruleType[1] = ROUTING
            PhenoCharacterisation phenoCharacterisation = pc[treeID];//fzhang 2018.10.02  define two phenotype characteristic---phenoCharacterisation
            Individual[] inds = state.population.subpops[0].individuals; //only one subpop now
            phenoCharacterisation.setReferenceRule(new GPRule(ruleType,((GPIndividual)inds[0]).trees[treeID]));

            //each individuals
            for(int ind = 0; ind < (int)(inds.length); ind ++){
                int[] charList = phenoCharacterisation.characterise(  //.characterise: calculate the distance
                        new GPRule(ruleType,((GPIndividual)inds[ind]).trees[treeID]));
                indsCharLists[treeID][ind] = new double[charList.length];

                //each PC information---convert int[] to int[][]
                for(int numFeature = 0; numFeature < charList.length; numFeature ++){
                    indsCharLists[treeID][ind][numFeature] = charList[numFeature];
                }
            }
        }

        //combine the phenotype of sequencing rule with the phenotype of routing rule
        indsCharListsMultiTree = new double[indsCharLists[0].length][];
        //indsCharLists[0].length == indsCharLists[1].length
        for(int i = 0; i < indsCharLists[0].length; i++){
                double[] combinePheChar = ArrayUtils.addAll(indsCharLists[0][i], indsCharLists[1][i]);
                indsCharListsMultiTree[i] = combinePheChar;
        }

        return indsCharListsMultiTree;
    }*/

    //2019.9.25 fzhang
    public static double[][] phenotypicPopulation(final EvolutionState state,
                                                  PhenoCharacterisation[] pc,
                                                  Boolean nonIntermediatePop) {

        RuleType[] ruleTypes = {RuleType.SEQUENCING, RuleType.ROUTING}; //ruleType is an array
        numTrees = state.parameters.getIntWithDefault(new Parameter(P_TREESIZE), null, 1);

        indsCharLists = new double[numTrees][(int)(state.population.subpops[0].individuals.length)][];

        for (int treeID = 0; treeID < numTrees; treeID++) {//each population
            RuleType ruleType = ruleTypes[treeID];  //ruleType is a rule type---ruleType[0] = SEQUENCING  ruleType[1] = ROUTING
            PhenoCharacterisation phenoCharacterisation = pc[treeID];//fzhang 2018.10.02  define two phenotype characteristic---phenoCharacterisation
            Individual[] inds = state.population.subpops[0].individuals; //only one subpop now

          /*  if(treeID ==1){
                phenoCharacterisation.setReferenceRule(GPRule.readFromLispExpression(ruleType, "(+ WIQ MWT)"));  build a new GP Rule
            }*/

            //2021.6.29 set the best rule in the population as the reference rule, and set the same rule as the reference rule in the intermediate population.
            //when adding more samples in the KNN, we hope the PC of the individuals in different populations are comparable. So, set all calculations about PC
            // with the same benckmark rules.
/*          if(nonIntermediatePop){
              phenoCharacterisation.setReferenceRule(new GPRule(ruleType,((GPIndividual)inds[0]).trees[treeID]));
              benckmarkRule[treeID] = new GPRule(ruleType,((GPIndividual)inds[0]).trees[treeID]);
          }
          else{
              phenoCharacterisation.setReferenceRule(benckmarkRule[treeID]);
          }*/

            //each individuals
            for(int ind = 0; ind < (int)(inds.length); ind ++){
                int[] charList = phenoCharacterisation.characterise(  //characterise: calculate the distance
                        new GPRule(ruleType,((GPIndividual)inds[ind]).trees[treeID]));
                indsCharLists[treeID][ind] = new double[charList.length];

                //each PC information---convert int[] to int[][]
                for(int numFeature = 0; numFeature < charList.length; numFeature ++){
                    indsCharLists[treeID][ind][numFeature] = charList[numFeature];
                }
            }
        }

        //combine the phenotype of sequencing rule with the phenotype of routing rule
        indsCharListsMultiTree = new double[indsCharLists[0].length][];
        //indsCharLists[0].length == indsCharLists[1].length
        for(int i = 0; i < indsCharLists[0].length; i++){
            double[] combinePheChar = ArrayUtils.addAll(indsCharLists[0][i], indsCharLists[1][i]);
            indsCharListsMultiTree[i] = combinePheChar;
        }

        return indsCharListsMultiTree;
    }

    //2021.7.26 when calculating the phenotypic characterisation of the inds in the whole population, use this one.
    public static int[][] muchBetterPhenotypicPopulation(EvolutionState state, PhenoCharacterisation[] pc)
    {
        Individual[] inds = state.population.subpops[0].individuals;
        int[][] indsCharListsMultiTree = new int[inds.length][];
        for(int i = 0; i < inds.length; i++)
            indsCharListsMultiTree[i] = phenotypicPopulation(state, pc, inds[i]);

        return indsCharListsMultiTree;
    }

    //2021.8.16 implemented by mazhar---calculate the phenotypic characteristic of each individual  this one is more efficient
    public static int[] phenotypicPopulation(final EvolutionState state,
                                                PhenoCharacterisation[] pc,
                                                Individual ind) {

        RuleType[] ruleTypes = {RuleType.SEQUENCING, RuleType.ROUTING}; //ruleType is an array
        int numTrees = ((GPIndividual)ind).trees.length;
        oneIndCharLists = new int[numTrees][];

        for (int treeID = 0; treeID < numTrees; treeID++)
        {
            RuleType ruleType = ruleTypes[treeID];
            PhenoCharacterisation phenoCharacterisation = pc[treeID];

            oneIndCharLists[treeID] = phenoCharacterisation.characterise(new GPRule(ruleType, ((GPIndividual) ind).trees[treeID]));
        }

        //combine the phenotype of sequencing rule with the phenotype of routing rule
        oneIndCharListsMultiTree = Stream.of(oneIndCharLists).flatMapToInt(IntStream::of).toArray();

        return oneIndCharListsMultiTree;
    }

    //2021.9.21 calculate the pc of inds from an arraylist
    //===============================start=================================
    public static int[][] muchBetterPhenotypicPopulation(ArrayList<Individual> inds, PhenoCharacterisation[] pc)
    {
        int[][] indsCharListsMultiTree = new int[inds.size()][];
        for(int i = 0; i < inds.size(); i++)
            indsCharListsMultiTree[i] = phenotypicPopulation(pc, inds.get(i));

        return indsCharListsMultiTree;
    }

    //2021.8.16 implemented by mazhar---calculate the phenotypic characteristic of each individual  this one is more efficient
    public static int[] phenotypicPopulation(PhenoCharacterisation[] pc,
                                             Individual ind) {

        RuleType[] ruleTypes = {RuleType.SEQUENCING, RuleType.ROUTING}; //ruleType is an array
        int numTrees = ((GPIndividual)ind).trees.length;
        oneIndCharLists = new int[numTrees][];

        for (int treeID = 0; treeID < numTrees; treeID++)
        {
            RuleType ruleType = ruleTypes[treeID];
            PhenoCharacterisation phenoCharacterisation = pc[treeID];

            oneIndCharLists[treeID] = phenoCharacterisation.characterise(new GPRule(ruleType, ((GPIndividual) ind).trees[treeID]));
        }

        //combine the phenotype of sequencing rule with the phenotype of routing rule
        oneIndCharListsMultiTree = Stream.of(oneIndCharLists).flatMapToInt(IntStream::of).toArray();

        return oneIndCharListsMultiTree;
    }
    //=======================================end==========================================

    //2021.7.22 calculate the PC of one ind
//    public static int[] phenotypicPopulation(final EvolutionState state,
//                                                PhenoCharacterisation[] pc,
//                                                Individual ind) {
//
//        RuleType[] ruleTypes = {RuleType.SEQUENCING, RuleType.ROUTING}; //ruleType is an array
//        numTrees = state.parameters.getIntWithDefault(new Parameter(P_TREESIZE), null, 1);
//
//        oneIndCharLists = new int[numTrees][];
//
//        for (int treeID = 0; treeID < numTrees; treeID++) {//each population
//            RuleType ruleType = ruleTypes[treeID];  //ruleType is a rule type---ruleType[0] = SEQUENCING  ruleType[1] = ROUTING
//            PhenoCharacterisation phenoCharacterisation = pc[treeID];//fzhang 2018.10.02  define two phenotype characteristic---phenoCharacterisation
//
//            int[] charList = phenoCharacterisation.characterise(  //characterise: calculate the distance
//                    new GPRule(ruleType, ((GPIndividual) ind).trees[treeID]));
//            oneIndCharLists[treeID] = new int[charList.length];
//
//            //each PC information---convert int[] to int[][]
//            for (int numFeature = 0; numFeature < charList.length; numFeature++) {
//                oneIndCharLists[treeID][numFeature] = charList[numFeature];
//            }
//        }
//
//        //combine the phenotype of sequencing rule with the phenotype of routing rule
//        oneIndCharListsMultiTree = new int[oneIndCharLists[0].length];
//        //indsCharLists[0].length == indsCharLists[1].length
//        for (int i = 0; i < indsCharLists[0].length; i++) {
//            int[] combinePheChar = ArrayUtils.addAll(oneIndCharLists[0], oneIndCharLists[1]);
//            oneIndCharListsMultiTree = combinePheChar;
//        }
//
//        return oneIndCharListsMultiTree;
//    }
}
