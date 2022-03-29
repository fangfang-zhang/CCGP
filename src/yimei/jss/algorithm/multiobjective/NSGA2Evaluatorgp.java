/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package yimei.jss.algorithm.multiobjective;

import java.util.*;
import ec.*;
import ec.multiobjective.*;
import ec.multiobjective.nsga2.NSGA2Evaluator;
import ec.simple.*;
import ec.util.*;

/* 
 * NSGA2Evaluator.java
 * 
 * Created: Sat Oct 16 00:19:57 EDT 2010
 * By: Faisal Abidi and Sean Luke
 */


/**
 * The NSGA2Evaluator for gp. Use it to calculate the rank and distance only.
 */
 
public class NSGA2Evaluatorgp extends NSGA2Evaluator
    {
  
   /**
	 * 
	 */

public void evaluatePopulation(final EvolutionState state)
       {
       super.evaluatePopulationgp(state);
       for (int x = 0; x < state.population.subpops.length; x++)
           //state.population.subpops[x].individuals = 
               extrafitness(state, x);//父代和子代同竞争
       }


    /** Build the auxiliary fitness data */
    public void extrafitness(EvolutionState state, int subpop)
        {
        Individual[] dummy = new Individual[0];//伪种群
        ArrayList ranks = assignFrontRanks(state.population.subpops[subpop]);//安排ranks
                
        int size = ranks.size();
        for(int i = 0; i < size; i++)
            {
            Individual[] rank = (Individual[])((ArrayList)(ranks.get(i))).toArray(dummy);//取第i个rank
            assignSparsity(rank);//安排分散度  [0,2]
            }
        }
    }