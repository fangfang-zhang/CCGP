package yimei.jss.helper;

import ec.EvolutionState;
import ec.gp.GPNode;
import ec.util.RandomChoice;
import yimei.jss.algorithm.featureweighted.FreBadGPRuleEvolutionState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;

public class Weights {
    /**
     *
     * @param state
     * @param topInds: top individuals are used to calculate the weights of features
     * @param weights: the weights in each generation
     * @param saveWeights: save the weights in all generations
     */
    public static void calculateWeights(final EvolutionState state, int topInds, double[][] weights, ArrayList<double[]>  saveWeights){
        ArrayList<HashMap<String, Integer>> stats = PopulationUtils.Frequency(state.population, topInds); //stats contains two values, one is terminal name
        //and the other is its frequency
        //stats.toString();
        if (weights == null || weights.length != state.population.subpops.length)
            throw new RuntimeException("Length of the  weight vecotr must be: state.population.subpops.length");

        GPNode[][] terminals = ((FreBadGPRuleEvolutionState)state).getTerminals();

//        weights = new double[state.population.subpops.length][];
        for (int subpop = 0; subpop < state.population.subpops.length; subpop++) {
            weights[subpop] = new double[terminals[subpop].length];
            for (int i = 0; i < terminals[0].length; i++) {
                String name = (terminals[0][i]).name();//the terminals in each population is same.  need to modifiy later for different terminal set setting
                for (int w = subpop; w < topInds * state.population.subpops.length; w += 2) {
                    if (stats.get(w).containsKey(name)) {
                        weights[subpop][i] += stats.get(w).get(name);
                    } else {
                        weights[subpop][i] += 0;
                    }
                }
            }
            //save the weights values in each generation
            //saveWeights.add(weights[subpop]) this is a java style, the weights will be changed later
            saveWeights.add(weights[subpop].clone()); //need to use clone to copy the array
            RandomChoice.organizeDistribution(weights[subpop]);
        } // for(int subpop = 0; ...
    }
}
