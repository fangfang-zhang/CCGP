package yimei.jss.algorithm.adaptivepop;

import ec.EvolutionState;
import ec.Individual;
import ec.SelectionMethod;
import ec.breed.ReproductionPipeline;

//fzhang 2019.6.16 when breeding some individuals, we plan to choose the parents from some specific individual, so we need modify something
public class fracReproductionPipeline extends ReproductionPipeline {
    public int produce(
            final int min,
            final int max,
            final int start,
            final int subpopulation,
            final Individual[] inds,
            final EvolutionState state,
            final int thread)
    {
        // grab individuals from our source and stick 'em right into inds.
        // we'll modify them from there
        int n = sources[0].produceFrac(min,max,start,subpopulation,inds,state,thread);

        if (mustClone || sources[0] instanceof SelectionMethod)
            for(int q=start; q < n+start; q++)
                inds[q] = (Individual)(inds[q].clone());
        return n;
    }
}
