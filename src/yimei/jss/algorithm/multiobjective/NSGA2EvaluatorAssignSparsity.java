package yimei.jss.algorithm.multiobjective;

import ec.Individual;
import ec.multiobjective.nsga2.NSGA2Evaluator;
import ec.multiobjective.nsga2.NSGA2MultiObjectiveFitness;
import ec.util.SortComparator;

public class NSGA2EvaluatorAssignSparsity extends NSGA2Evaluator{
	public void assignSparsity(Individual[] front) {
		int numObjectives = ((NSGA2MultiObjectiveFitness) front[0].fitness).getObjectives().length;

		for (int i = 0; i < front.length; i++)
			((NSGA2MultiObjectiveFitness) front[i].fitness).sparsity = 0;

		for (int i = 0; i < numObjectives; i++) {
			final int o = i;
			// 1. Sort front by each objective.
			// 2. Sum the manhattan distance of an individual's neighbours over
			// each objective.
			// NOTE: No matter which objectives objective you sort by, the
			// first and last individuals will always be the same (they maybe
			// interchanged though). This is because a Pareto front's
			// objective values are strictly increasing/decreasing.
			ec.util.QuickSort.qsort(front, new SortComparator() {
				public boolean lt(Object a, Object b) {
					Individual i1 = (Individual) a;
					Individual i2 = (Individual) b;
					return (((NSGA2MultiObjectiveFitness) i1.fitness)
							.getObjective(o) < ((NSGA2MultiObjectiveFitness) i2.fitness).getObjective(o));
				}

				public boolean gt(Object a, Object b) {
					Individual i1 = (Individual) a;
					Individual i2 = (Individual) b;
					return (((NSGA2MultiObjectiveFitness) i1.fitness)
							.getObjective(o) > ((NSGA2MultiObjectiveFitness) i2.fitness).getObjective(o));
				}
			});

			//fzhang 2018.11.22 normize the objective to calculate the sparsity
			final double min = ((NSGA2MultiObjectiveFitness) front[0].fitness).getObjective(o);
            final double max = ((NSGA2MultiObjectiveFitness) front[front.length - 1].fitness).getObjective(o);
            
//            System.out.println("normalised min: " + min );
//            System.out.println("normalised max: " + max );
       
			// Compute and assign sparsity.
			// the first and last individuals are the sparsest.
			((NSGA2MultiObjectiveFitness) front[0].fitness).sparsity = Double.POSITIVE_INFINITY;
			((NSGA2MultiObjectiveFitness) front[front.length - 1].fitness).sparsity = Double.POSITIVE_INFINITY;
			for (int j = 1; j < front.length - 1; j++) {
				NSGA2MultiObjectiveFitness f_j = (NSGA2MultiObjectiveFitness) (front[j].fitness);
				NSGA2MultiObjectiveFitness f_jplus1 = (NSGA2MultiObjectiveFitness) (front[j + 1].fitness);
				NSGA2MultiObjectiveFitness f_jminus1 = (NSGA2MultiObjectiveFitness) (front[j - 1].fitness);

//				System.out.println("original min: " + f_j.minObjective[o] );
//			    System.out.println("original max: " + f_j.maxObjective[o] );
          
				if (max ==  min)
            	{
					f_j.sparsity += 0;
            	}
            else {
            // store the NSGA2Sparsity in sparsity
            	f_j.sparsity += (f_jplus1.getObjective(o) - f_jminus1.getObjective(o)) / (max- min);
            }
				
				//original version
//				System.out.println(f_j.maxObjective[o] - f_j.minObjective[o]);  //1
				// store the NSGA2Sparsity in sparsity
				/*f_j.sparsity += (f_jplus1.getObjective(o) - f_jminus1.getObjective(o))
						/ (f_j.maxObjective[o] - f_j.minObjective[o]);*/
			}
		}
	}
}
