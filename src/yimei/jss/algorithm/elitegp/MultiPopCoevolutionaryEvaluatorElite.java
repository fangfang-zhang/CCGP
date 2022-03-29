package yimei.jss.algorithm.elitegp;

import ec.EvolutionState;
import ec.coevolve.MultiPopCoevolutionaryEvaluator;

public class MultiPopCoevolutionaryEvaluatorElite extends MultiPopCoevolutionaryEvaluator {

	/**
	 * Returns true if the subpopulation should be evaluated. This will happen if
	 * the Breeder believes that the subpopulation should be breed afterwards.
	 */
	public boolean shouldEvaluateSubpop(EvolutionState state, int subpop, int threadnum) {
		//original version
		/*
		 * return (state.breeder instanceof SimpleBreeder &&
		 * ((SimpleBreeder)(state.breeder)).shouldBreedSubpop(state, subpop,
		 * threadnum));
		 */

		//modified by fzhang 16.5.2018  after change SimpleBreeder to keep five elites from last n generations, here need to be modified
        //because this is to check whether we need to breed or not
		//if we do not change here, it will check SimpleBreeder, and return false, because we do not use SimpleBreeder

		return (state.breeder instanceof SimpleBreederElite
				&& ((SimpleBreederElite) (state.breeder)).shouldBreedSubpop(state, subpop, threadnum));
	}
}
