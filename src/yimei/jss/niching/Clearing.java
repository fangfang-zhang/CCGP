package yimei.jss.niching;

import ec.EvolutionState;
import ec.Individual;
import ec.Subpopulation;
import ec.gp.GPIndividual;
import yimei.jss.rule.RuleType;
import yimei.jss.rule.operation.evolved.GPRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The clearing method for niching.
 *
 * Created by YiMei on 4/10/16.
 */
public class Clearing {

	// delete the poor individuals      radius: control the range of each niche   capacity: determines the number of individuals in each niche.
    public static void clearPopulation(final EvolutionState state,
                                       double radius, int capacity,
                                       PhenoCharacterisation[] pc) {
        RuleType[] ruleTypes = {RuleType.SEQUENCING, RuleType.ROUTING}; //ruleType is an array
        for (int subpopNum = 0; subpopNum < state.population.subpops.length; subpopNum++) {
            Subpopulation subpop = state.population.subpops[subpopNum];
            RuleType ruleType = ruleTypes[subpopNum];  //ruleType is a rule type---ruleType[0] = SEQUENCING  ruleType[1] = ROUTING
            PhenoCharacterisation phenoCharacterisation = pc[subpopNum];//fzhang 2018.10.02  define two phenotype characteristic---phenoCharacterisation
            //this is a defined type, 1---decisionSituations is an arraylist (length = 20)
                                   // 2---referenceIndexes:[6, 6, 5, 6, 3, 5, 5, 6, 6, 5, 6, 6, 6, 6, 5, 7, 5, 3, 4, 6] 19 elements
                                   // 3---referenceRule: WSPT, this will be replaced later. this is default rule

            // sort the individuals from best to worst
            Individual[] sortedPop = subpop.individuals; //should according to the fitness value
            Arrays.sort(sortedPop);

            //We are setting the reference rule of the phenotype to the rule of the individual
            //with the best fitness of this subpop
            phenoCharacterisation.setReferenceRule(new GPRule(ruleType,((GPIndividual)sortedPop[0]).trees[0]));

            //fzhang 2018.10.2 calculate the distance of each individual and the reference rule
            List<int[]> sortedPopCharLists = new ArrayList<>();
            for (Individual indi : sortedPop) {
                //here we are comparing the different ways each rule ranked objects in the decision making
                //compared to the (best) reference rule of the phenotype characterisation

                //where the examined rule set the chosen operation by reference rule  for example: 3. means examined rule set the chosen operation by reference rule as the third one
                int[] charList = phenoCharacterisation.characterise(  //.characterise: calculate the distance
                        new GPRule(ruleType,((GPIndividual)indi).trees[0]));
                sortedPopCharLists.add(charList); //charList:  [2, 1, 2, 3, 1, 2, 3, 1, 1, 1, 1, 1, 1, 2, 2, 1, 1, 1, 1, 1]
            }

            // clear this subpopulation
            for (int i = 0; i < sortedPop.length; i++) {
                // skip the cleared individuals
                if (((Clearable)sortedPop[i].fitness).isCleared()) {
                    continue;
                }

                int numWinners = 1;
                for (int j = i+1; j < sortedPop.length; j++) {
                    // skip the cleared individuals
                    if (((Clearable)sortedPop[j].fitness).isCleared()) {
                        continue;
                    }

                    // calculate the distance between individuals i and j
                    double distance = PhenoCharacterisation.distance(
                            sortedPopCharLists.get(i), sortedPopCharLists.get(j));
                    if (distance > radius) {
                        // Individual j is not in the niche
                        continue; //if distance, means two individuals are the same, clear (below) the individual, get out of current loop
                    }

                    if (numWinners < capacity) {
                        numWinners ++;
                    }
                    else {
                        // Clear the fitness of individual j
                        ((Clearable)sortedPop[j].fitness).clear();
                    }
                }
            }
        }
    }
}
