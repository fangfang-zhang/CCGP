package yimei.jss.algorithm.adaptivepop;

import ec.EvolutionState;
import ec.Individual;
import ec.select.TournamentSelection;
import ec.util.Parameter;
import yimei.jss.feature.FeatureUtil;

public class TournamentSelectionForPop extends TournamentSelection {

    public static final String P_PRE_GENERATIONS = "pre-generations";
    public static final String P_POP_ADAPT_FRAC_ELITES = "pop-adapt-frac-elites";
    private int preGenerations;
    private double fracElites;

    public int produce(final int subpopulation,
                       final EvolutionState state,
                       final int thread)
    {
        // pick size random individuals, then pick the best.
        preGenerations = state.parameters.getIntWithDefault(new Parameter(P_PRE_GENERATIONS), null, -1);  //50
        Individual[] oldinds;

        if(state.generation == preGenerations)
            oldinds = FeatureUtil.getNewpop().subpops[subpopulation].individuals;
        else
            oldinds = state.population.subpops[subpopulation].individuals;

        int best = getRandomIndividual(0, subpopulation, state, thread);

        int s = getTournamentSizeToUse(state.random[thread]);

        if (pickWorst)
            for (int x=1;x<s;x++)
            {
                int j = getRandomIndividual(x, subpopulation, state, thread);
                if (!betterThan(oldinds[j], oldinds[best], subpopulation, state, thread))  // j is at least as bad as best
                    best = j;
            }
        else
            for (int x=1;x<s;x++)
            {
                int j = getRandomIndividual(x, subpopulation, state, thread);
                if (betterThan(oldinds[j], oldinds[best], subpopulation, state, thread))  // j is better than best
                    best = j;
            }

        return best;
    }


    /** Produces the index of a (typically uniformly distributed) randomly chosen individual
     to fill the tournament.  <i>number</> is the position of the individual in the tournament.  */
    public int getRandomIndividual(int number, int subpopulation, EvolutionState state, int thread)
    {
        preGenerations = state.parameters.getIntWithDefault(new Parameter(P_PRE_GENERATIONS), null, -1);  //50
        fracElites = state.parameters.getDoubleWithDefault(
                new Parameter(P_POP_ADAPT_FRAC_ELITES), null, 0.0); //0.0

        if(state.generation == preGenerations){
            return state.random[thread].nextInt((int)(state.population.subpops[0].individuals.length * fracElites));
        }else
            {
            Individual[] oldinds = state.population.subpops[subpopulation].individuals;
            return state.random[thread].nextInt(oldinds.length);
        }
    }
}
