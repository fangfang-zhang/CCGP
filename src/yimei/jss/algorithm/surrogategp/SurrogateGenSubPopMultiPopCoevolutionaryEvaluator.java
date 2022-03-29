/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/


package yimei.jss.algorithm.surrogategp;

import ec.*;
import ec.coevolve.GroupedProblemForm;
import ec.coevolve.MultiPopCoevolutionaryEvaluator;
import yimei.jss.ruleoptimisation.RuleOptimizationProblem;
import yimei.jss.surrogate.Surrogate;

//fzhang 4.7.2018   subpop0 uses original shop, subpop1 use surrogate shop  this is evaluator file
public class SurrogateGenSubPopMultiPopCoevolutionaryEvaluator extends MultiPopCoevolutionaryEvaluator
    {
    // individuals to evaluate together
    Individual[] inds = null;
    // which individual should have its fitness updated as a result
    boolean[] updates = null;
    // the selection method used to select the other partners from the previous generation
    public static final String P_SELECTION_METHOD_PREV = "select-prev"; //randomly
    SelectionMethod[] selectionMethodPrev;

    // the selection method used to select the other partners from the current generation
    public static final String P_SELECTION_METHOD_CURRENT = "select-current";
    SelectionMethod[] selectionMethodCurrent;
    
    // the number of random partners selected from the current and previous generations
    public final static String P_NUM_IND = "num-prev"; //0
    protected int numPrev;
    Population previousPopulation;

    
    public void performCoevolutionaryEvaluation( final EvolutionState state,
        final Population population,
        final GroupedProblemForm prob ) {
        int evaluations = 0;

        inds = new Individual[population.subpops.length];
        updates = new boolean[population.subpops.length];//check if we need to evaluate the individuals, because it is so expensive, should be careful.

        // we start by warming up the selection methods
        //System.out.println("numCurrent: "+numCurrent); //0
        //numCurrent: the number of random individuals from any given subpopulation from the current population to be selected as collaborators

        //==========================here skip step 1: load the selectionMethod for current generation==================================
        //selectionMethodCurrent: the selection method used to select the other partners from the current generation
        //System.out.println(selectionMethodCurrent.length); //2, means we have two selectionMethod (each one for each populations) for Current
        //System.out.println(selectionMethodCurrent); //[Lec.SelectionMethod;@5ae63ade   ec.select.RandomSelection
        //numCurrent = 0
        if (numCurrent > 0) {
            for (int i = 0; i < selectionMethodCurrent.length; i++) {
                selectionMethodCurrent[i].prepareToProduce(state, i, 0);  //A default version of prepareToProduce which does nothing.
            }
        }

        //==========================here skip: step 2: load the selectionMethod for previous generation==================================
        //the selection method used to select the other partners from the previous generation
        //System.out.println("numPrev = "+numPrev); //0
        //System.out.println("selectionMethodPrev.length = "+selectionMethodPrev.length); //2
        if (numPrev > 0) {
            for (int i = 0; i < selectionMethodPrev.length; i++) {
                // do a hack here
                Population currentPopulation = state.population; //state.population is current population.
                state.population = previousPopulation;
                selectionMethodPrev[i].prepareToProduce(state, i, 0);  //A default version of prepareToProduce which does nothing.  here state is important, it is the state
                //for previous population
                state.population = currentPopulation; //currentPopulaiton is a temp parameter to save current population
            }
        }

        //step 3: build subPopulaiton, subpops[0] and subpops[1]
        // build subpopulation array to pass in each time
        int[] subpops = new int[state.population.subpops.length];
        //System.out.println(subpops.length);
        for(int j = 0; j < subpops.length; j++) {
            subpops[j] = j;
        }

        //System.out.println(prob);  //yimei.jss.ruleoptimisation.RuleCoevolutionProblem@5ae63ade
        //here skip: step 3: setup the shuffle: here num-shuffled = 0  here, we do not use it.
        if (numShuffled > 0) {
            int[/*numShuffled*/][/*subpop*/][/*shuffledIndividualIndexes*/] ordering = null;
            // build shuffled orderings
            ordering = new int[numShuffled][state.population.subpops.length][state.population.subpops[0].individuals.length]; // if num-shuffled =1  [1][2][512]
            for(int c = 0; c < numShuffled; c++)
                for(int m = 0; m < state.population.subpops.length; m++)
                    {
                    for(int i = 0; i < state.population.subpops[0].individuals.length; i++)
                        ordering[c][m][i] = i; //ordering[0][0][0] = 0, ordering[0][0][1] = 1, ordering[0][0][2] = 2, ordering[0][0][3] = 3
                    if (m != 0)
                        shuffle(state, ordering[c][m]); //ordering = new int[numShuffled][state.population.subpops.length]
                    }

            // for each individual
            for (int i = 0; i < state.population.subpops[0].individuals.length; i++) {
                for (int k = 0; k < numShuffled; k++) {
                    for (int ind = 0; ind < inds.length; ind++) {
                        inds[ind] = state.population.subpops[ind].individuals[ordering[k][ind][i]];
                        updates[ind] = true;
                    }
                    prob.evaluate(state, inds, updates, false, subpops, 0);  ////yimei.jss.ruleoptimisation.RuleCoevolutionProblem@5ae63ade
                    evaluations++;
                }
            }
        }
          //System.out.println(evaluations); //0

//        if (state.generation > 0) {
//            //want to find out whether elite individuals and or their collaborators are being included
//            //in the next generation
//            boolean[][] found = new boolean[2][2]; //should all be false
//
//            for (int subpop = 0; subpop < state.population.subpops.length; subpop++)
//            {
//                GPIndividual eliteInd = (GPIndividual) eliteIndividuals[subpop][0]; //one for each subpop
//                int otherSubpop = (subpop+1)%2;
//                GPIndividual otherEliteCollab = (GPIndividual) eliteIndividuals[otherSubpop][0].fitness.context[0];
//                //checking each individual
//                for (int i = state.population.subpops[subpop].individuals.length-2;
//                     i < state.population.subpops[subpop].individuals.length; i++)
//                {
//                    GPIndividual ind = (GPIndividual) state.population.subpops[subpop].individuals[i];
//                    if (ind.equals(eliteInd) || ind == eliteInd) {
//                        found[subpop][0] = true;
//                    }
//                    if (ind.equals(otherEliteCollab) || ind == otherEliteCollab) {
//                        found[otherSubpop][1] = true;
//                    }
//                }
//            }
//            for (int i = 0; i < 2; ++i) {
//                for (int j = 0; j < 2; ++j) {
//                    if (!found[i][j]) {
//                        if (j == 0) {
//                            System.out.println("Elite missing: "+i+" "+j);
//                        } else {
//                            System.out.println("Collab missing: "+i+" "+j);
//                        }
//                    }
//                }
//            }
//        }

        //==========================useful and important part=======================================
        //step 4: for each subpopulation, j means subPopulation   2*512*4*2= 8192  cost
        for (int j = 0; j < state.population.subpops.length; j++) //0,1
            {
            // now do elites and randoms
        	//===========fzhang 4.7.2018 
        	//in even generations, the first population use original shop and the sencond shop use surrogate shop=====
			//in odd generations, the first population use surrogate shop and the sencond shop use original shop
        	if (state.generation % 2 == 0) {
				if (j == 0)
				{
					((Surrogate) ((RuleOptimizationProblem) this.p_problem).getEvaluationModel()).useOriginal();
//				    System.out.println("even generation, subpop 0, original");
				}		
				else {
					((Surrogate) ((RuleOptimizationProblem) this.p_problem).getEvaluationModel()).useSurrogate();
//					System.out.println("even generation, subpop 1, surrogate");
				}
					
			} else if (j == 0) {
				((Surrogate) ((RuleOptimizationProblem) this.p_problem).getEvaluationModel()).useSurrogate();
//				System.out.println("odd generation, subpop 0, surrogate");
			} else {
				((Surrogate) ((RuleOptimizationProblem) this.p_problem).getEvaluationModel()).useOriginal();
//				System.out.println("odd generation, subpop 1, original");
			}
				
	
        	//System.out.println(!shouldEvaluateSubpop(state, j, 0)); //false
        	//System.out.println(eliteIndividuals[j].length); //4
        	//System.out.println(inds.length); //2
        	//System.out.println(state.population.subpops[j].individuals.length);  //512

            if (!shouldEvaluateSubpop(state, j, 0)) continue;  // don't evaluate this subpopulation

            // for each individual
            for (int i = 0; i < state.population.subpops[j].individuals.length; i++) //512
                {
                Individual individual = state.population.subpops[j].individuals[i];

                // Test against all the elites
                for (int k = 0; k < eliteIndividuals[j].length; k++) { //2
                    for (int ind = 0; ind < inds.length; ind++) { //2
                        if (ind == j) {   //j = 0, 1  (ind j) ---> (0 0) or (1 1) that is to say, this is the subpopulation1
                            inds[ind] = individual; //inds[0] = individual = state.population.subpops[0].individuals[0];
                                                    //inds[1] = individual = state.population.subpops[1].individuals[1];
                                                    //the individuals to evaluate together
                            updates[ind] = true;   // updates[0] = true    updates[1] = true   evaluate
                        }
                        else {  // this is subpopulation2
                            inds[ind] = eliteIndividuals[ind][k];   // (ind j) ---> (0 1) or (1 0)
                                                                    //inds[1] = eliteIndividuals[1][*]   inds[0] = eliteIndividuals[0][*]
                            updates[ind] = false;  // do not evaluate
                        }

                    /*    System.out.println("ind "+ ind);
                        System.out.println("i "+ i);
                        System.out.println("j "+ j);
                        System.out.println("k "+ k);*/
                    }

                    prob.evaluate(state,inds,updates, false, subpops, 0);
                   /* System.out.println("Evaluated finished: population "+ j);
                    System.out.println("individual "+ i);*/
                    evaluations++;
                }
                //System.out.println(evaluations);  //4  8  12 16 20 24 28 32 ... 4096   2*512*4 = 4096  inds[] is used to save the individuals we want to evaluated.

                //here, skip this part: test against random selected individuals of the current population
                for(int k = 0; k < numCurrent; k++) //0  skip this part
                    {
                    for(int ind = 0; ind < inds.length; ind++) //2
                        {
                        if (ind == j) { inds[ind] = individual; updates[ind] = true; }
                        else { inds[ind] = produceCurrent(ind, state, 0); updates[ind] = true; }
                        }
                    prob.evaluate(state,inds,updates, false, subpops, 0);
                    evaluations++;
                    }

                // here, skip this part. Test against random individuals of previous population
                for(int k = 0; k < numPrev; k++)  // 0  skip this part
                    {
                    for(int ind = 0; ind < inds.length; ind++)
                        {
                        if (ind == j) { inds[ind] = individual; updates[ind] = true; }
                        else { inds[ind] = producePrevious(ind, state, 0); updates[ind] = false; }
                        }
                    prob.evaluate(state,inds,updates, false, subpops, 0);
                    evaluations++;
                    }
                }
            }
        //============================================================================================================================

        //here, skip this part
        // now shut down the selection methods
        if (numCurrent > 0)
            for( int i = 0 ; i < selectionMethodCurrent.length; i++)
                selectionMethodCurrent[i].finishProducing( state, i, 0 );  //A default version of finishProducing, which does nothing.

        if (numPrev > 0)
            for( int i = 0 ; i < selectionMethodPrev.length ; i++ )
                {
                // do a hack here
                Population currentPopulation = state.population;
                state.population = previousPopulation;
                selectionMethodPrev[i].finishProducing( state, i, 0 );
                state.population = currentPopulation;
                }

        state.output.message("Evaluations: " + evaluations);
        }
    }





   