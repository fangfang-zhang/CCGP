package yimei.jss.algorithm.multiobjective;

import ec.EvolutionState;
import ec.Individual;
import ec.Subpopulation;
import ec.simple.SimpleEvaluator;
import ec.simple.SimpleProblemForm;
//2019.2.21 this aims to normalise the objective values for weighted sum method
public class NormWeightedSimpleEvaluator extends SimpleEvaluator{
    protected void evalPopChunk(EvolutionState state, int[] numinds, int[] from,
            int threadnum, SimpleProblemForm p)
            {
            ((ec.Problem)p).prepareToEvaluate(state,threadnum);

            Subpopulation[] subpops = state.population.subpops;
            int len = subpops.length;

            //=============start=================
            //============find the max and min values of each objective==========
            for(int pop=0;pop<len;pop++)
            {
            // start evaluatin'!
            int fp = from[pop];
            int upperbound = fp+numinds[pop];
            Individual[] inds = subpops[pop].individuals;
            for (int x=fp;x<upperbound;x++)
                p.normObjective(state,inds[x], pop, threadnum);
            }


            //===========end===============

            for(int pop=0;pop<len;pop++)
                {
                // start evaluatin'!
                int fp = from[pop];
                int upperbound = fp+numinds[pop];
                Individual[] inds = subpops[pop].individuals;
                for (int x=fp;x<upperbound;x++)
                    p.evaluate(state,inds[x], pop, threadnum);
                }

            ((ec.Problem)p).finishEvaluating(state,threadnum);
            }
}
