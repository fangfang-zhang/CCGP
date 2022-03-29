package yimei.jss.algorithm.elitegp;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.simple.SimpleBreeder;
import ec.util.Parameter;
import ec.util.QuickSort;
import ec.util.SortComparatorL;

public class SimpleBreederElite extends SimpleBreeder {
	/**
	 * A private helper function for breedPopulation which loads elites into a
	 * subpopulation.
	 */

	// modified by fzhang 14.5.2018 save the elites in last several generations
	int numPreElites = 5;
	// method 1: arraylist
	/*
	 * ArrayList<Individual> tempSub1Elites= new ArrayList<Individual>();
	 * ArrayList<Individual> tempSub2Elites= new ArrayList<Individual>();
	 */
	// modified by fzhang 16.5.2018 method 2: use array
	// method 2: array
	Individual tempSub1Elites[] = new Individual[numPreElites];
	Individual tempSub2Elites[] = new Individual[numPreElites];
	int replaceInd1 = 0;
	int replaceInd2 = 0;
	int bestInd1 = 0; // bestInd will be set to 0,1,2,3,4
	int bestInd2 = 0;

	protected void loadElites(EvolutionState state, Population newpop) {
		// are our elites small enough?
		for (int x = 0; x < state.population.subpops.length; x++) {
			if (numElites(state, x) > state.population.subpops[x].individuals.length)
				state.output.error(
						"The number of elites for subpopulation " + x + " exceeds the actual size of the subpopulation",
						new Parameter(EvolutionState.P_BREEDER).push(P_ELITE).push("" + x));
			if (numElites(state, x) == state.population.subpops[x].individuals.length)
				state.output.warning(
						"The number of elites for subpopulation " + x + " is the actual size of the subpopulation",
						new Parameter(EvolutionState.P_BREEDER).push(P_ELITE).push("" + x));
		}
		state.output.exitIfErrors();

		// =================find out the index of the best
		// indivudual=======================
		// we assume that we're only grabbing a small number (say <10%), so
		// it's not being done multithreaded
		int[] bestIndex = new int[state.population.subpops.length];
		for (int sub = 0; sub < state.population.subpops.length; sub++) {
			int best = 0;
			Individual[] oldinds = state.population.subpops[sub].individuals;
			for (int x = 1; x < oldinds.length; x++) {
				if (oldinds[x].fitness.betterThan(oldinds[best].fitness)) {
					best = x;
				}
			}
			bestIndex[sub] = best;
			// the index of best individuals in each subpopulation are save in
			// bestIndex[sub], like bestIndex[0] = 0, bestIndex[1]=145
		}

		for (int sub = 0; sub < state.population.subpops.length; sub++) // 0 1
		{
			// System.out.println(!shouldBreedSubpop(state, sub, 0)); //nothing is printed
			// out.
			// skip here
			if (!shouldBreedSubpop(state, sub, 0)) // don't load the elites for this one, we're not doing breeding of it
													// true
			{
				continue;
			}

			// System.out.println("numElites(state, sub) "+numElites(state, sub) ); //always
			// eauql two, seems like no related to eval.num-elites and breed.elite.0
			// if the number of elites is 1, then we handle this by just finding the best
			// one.
			if (numElites(state, sub) == 1) {
				Individual[] oldinds = state.population.subpops[sub].individuals;
				Individual[] inds = newpop.subpops[sub].individuals;
				if (state.population.subpops.length > 1) {
					int otherSubPop = (sub + 1) % 2;
					Individual[] oldindsOtherSubpop = state.population.subpops[otherSubPop].individuals;
					// want to also insert context of best individual
					Individual otherCollab = (Individual) oldindsOtherSubpop[bestIndex[otherSubPop]].fitness
							.getContext()[sub].clone();
					inds[inds.length - 2] = otherCollab;
				}
				Individual elite = (Individual) (oldinds[bestIndex[sub]].clone());
				// inds[inds.length-1] = elite;

				// method 1: use arraylist, compare with array, this is more computation complex
				// because when remove the first element, all the elements behind will be moved.

				// modified by fzhang 14.5.2018 to put the elites from last five generaiton into
				// current generation
				// save the elites in last five generation
				/*
				 * if(sub == 0) { if(state.generation < numPreElites) { inds[inds.length-1] =
				 * elite; tempSub1Elites.add(elite); } else { if(tempSub1Elites.size() <
				 * numPreElites) System.out.println("Have not got enough elites!!!"); else
				 * if(tempSub1Elites.size() > numPreElites)
				 * System.out.println("Too many elites!!!"); else
				 * System.out.println("Got the right number of elites!!!");
				 *
				 * int i = 0; //in current generation, put the five elites in new(current)
				 * generation for(int x=inds.length-numPreElites;x<inds.length;x++) {//start
				 * from 510, because numElites(state,sub) inds[x] = tempSub1Elites.get(i); i++;
				 * } tempSub1Elites.remove(0); tempSub1Elites.add(elite); } } else {
				 * if(state.generation < numPreElites) { inds[inds.length-1] = elite;
				 * tempSub2Elites.add(elite); } else { if(tempSub2Elites.size() < numPreElites)
				 * System.out.println("Have not got enough elites!!!"); else
				 * if(tempSub2Elites.size() > numPreElites)
				 * System.out.println("Too many elites!!!"); else
				 * System.out.println("Got the right number of elites!!!");
				 *
				 * int i = 0; //in current generation, put the five elites in new(current)
				 * generation for(int x=inds.length-numPreElites;x<inds.length;x++) {//start
				 * from 510, because numElites(state,sub) inds[x] = tempSub2Elites.get(i); i++;
				 * } tempSub2Elites.remove(0); tempSub2Elites.add(elite); } }
				 */

				// method 2: use array
				if (sub == 0) {
					if (state.generation < numPreElites) {
						inds[inds.length - 1] = elite;
						tempSub1Elites[bestInd1 % numPreElites] = elite;
						bestInd1++;
					} else {
						/*
						 * if(tempSub1Elites.length < numPreElites)
						 * System.out.println("Have not got enough elites!!!"); else
						 * if(tempSub1Elites.length > numPreElites)
						 * System.out.println("Too many elites!!!"); else
						 * System.out.println("Got the right number of elites!!!");
						 */

						int i = 0;// loop tempSub1Elites[]
						// in current generation, put the five elites in new(current) generation
						for (int x = inds.length - numPreElites; x < inds.length; x++) {// start from 510, because
																						// numElites(state,sub)
							inds[x] = tempSub1Elites[i];
							i++;
						}

						tempSub1Elites[replaceInd1 % numPreElites] = elite;
						replaceInd1++;
					}

				} else {

					if (state.generation < numPreElites) {
						inds[inds.length - 1] = elite;
						tempSub2Elites[bestInd2] = elite;
						bestInd2++;
					} else {
						/*
						 * if(tempSub2Elites.length < numPreElites)
						 * System.out.println("Have not got enough elites!!!"); else
						 * if(tempSub2Elites.length > numPreElites)
						 * System.out.println("Too many elites!!!"); else
						 * System.out.println("Got the right number of elites!!!");
						 */

						int i = 0;// loop tempSub1Elites[]
						// in current generation, put the five elites in new(current) generation
						for (int x = inds.length - numPreElites; x < inds.length; x++) {// start from 510, because
																						// numElites(state,sub)
							inds[x] = tempSub2Elites[i];
							i++;
						}

						tempSub2Elites[replaceInd2 % numPreElites] = elite;
						replaceInd2++;
					}

				}
			} else if (numElites(state, sub) > 0) // we'll need to sort
			{
				// define int[] orderPop, length = 512 and its elements are from 0 to 511
				int[] orderedPop = new int[state.population.subpops[sub].individuals.length];
				for (int x = 0; x < state.population.subpops[sub].individuals.length; x++)
					orderedPop[x] = x;
				// orderPop[0]= 0, orderPop[1]= 1, orderPop[2]= 2....orderPop[511]= 511

				// sort the best so far where "<" means "not as fit as"
				QuickSort.qsort(orderedPop, new EliteComparator(state.population.subpops[sub].individuals));
				// load the top N individuals

				Individual[] inds = newpop.subpops[sub].individuals; // has not value
				Individual[] oldinds = state.population.subpops[sub].individuals; // has values
				for (int x = inds.length - numElites(state, sub); x < inds.length; x++)// start from 510, because
																						// numElites(state,sub)
					inds[x] = (Individual) (oldinds[orderedPop[x]].clone());
			}
		}

		// optionally force reevaluation
		unmarkElitesEvaluated(state, newpop);
	}

    //override
	static class EliteComparator implements SortComparatorL {
		Individual[] inds;

		public EliteComparator(Individual[] inds) {
			super();
			this.inds = inds;
		}

		public boolean lt(long a, long b) {
			return inds[(int) b].fitness.betterThan(inds[(int) a].fitness);
		}

		public boolean gt(long a, long b) {
			return inds[(int) a].fitness.betterThan(inds[(int) b].fitness);
		}
	}
}
