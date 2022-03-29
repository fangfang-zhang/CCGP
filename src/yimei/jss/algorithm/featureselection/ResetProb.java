package yimei.jss.algorithm.featureselection;

import ec.BreedingPipeline;
import ec.BreedingSource;
import ec.EvolutionState;
import ec.breed.MultiBreedingPipeline;
import ec.util.Parameter;

public class ResetProb extends MultiBreedingPipeline {
	private int preGenerations;

	public void setup(final EvolutionState state, final Parameter base) {
		super.setup(state, base); // load all the parameters about breeding pipeline and read the probability

		preGenerations = state.parameters.getIntWithDefault(new Parameter(P_PRE_GENERATIONS), null, -1); // 50
		// fzhang 21.7.2018
		if (state.generation >= preGenerations) {
			sources[0].probability = 0.15
					+ (0.8 - 0.15) * (state.generation - preGenerations) / (state.numGenerations - 1 - preGenerations);
			sources[1].probability = 0.8
					- (0.8 - 0.15) * (state.generation - preGenerations) / (state.numGenerations - 1 - preGenerations);
			sources[2].probability = 0.05;
		}

		System.out.println(sources[0].probability);
		System.out.println(sources[1].probability);
		System.out.println(sources[2].probability);

		Parameter def = defaultBase(); // return BreedDefaults.base().push(P_MULTIBREED);
		// System.out.println(def); //breed.multibreed breed.multibreed

		double total = 0.0;

		// System.out.println(sources.length); //3 3
		if (sources.length == 0) // uh oh
			state.output.fatal("num-sources must be provided and > 0 for MultiBreedingPipeline",
					base.push(P_NUMSOURCES), def.push(P_NUMSOURCES));

		for (int x = 0; x < sources.length; x++) {
			// make sure the sources are actually breeding pipelines
			if (!(sources[x] instanceof BreedingPipeline))
				state.output.error("Source #" + x + "is not a BreedingPipeline", base);
			else if (sources[x].probability < 0.0) // null checked from state.output.error above
				state.output.error("Pipe #" + x + " must have a probability >= 0.0", base); // convenient that
																							// NO_PROBABILITY is -1...
			else
				total += sources[x].probability;
		}

		state.output.exitIfErrors();

		// Now check for nonzero probability (we know it's positive)
		if (total == 0.0)
			state.output.warning(
					"MultiBreedingPipeline's children have all zero probabilities -- this will be treated as a uniform distribution.  This could be an error.",
					base);

		// allow all zero probabilities
		BreedingSource.setupProbabilities(sources);

		generateMax = state.parameters.getBoolean(base.push(P_GEN_MAX), def.push(P_GEN_MAX), true); // false: the
																									// maximum number of
																									// the typical
																									// numbers of
		// any child source varies depending on the child source picked.
		maxGeneratable = 0; // indicates that I don't know what it is yet.

		// declare that likelihood isn't used
		if (likelihood < 1.0)
			state.output.warning("MultiBreedingPipeline does not respond to the 'likelihood' parameter.",
					base.push(P_LIKELIHOOD), def.push(P_LIKELIHOOD));
	}
}
