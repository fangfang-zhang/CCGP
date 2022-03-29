package yimei.jss.algorithm.multipletreegp;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPInitializer;
import ec.gp.GPNode;
import ec.gp.GPNodeBuilder;
import ec.gp.GPTree;
import ec.gp.koza.CrossoverPipeline;
import ec.gp.koza.MutationPipeline;

/**
 * Created by fzhang on 26.05.2018.
 */
public class AllMutationPipeline extends MutationPipeline {
    /**
     * Overrides the normal crossover pipeline to use all pairs of the SAME TREE INDEX in each individual when performing crossover. This is necessary where the tree locations ~mean something~.
     * <p>
     * Unfortunately, this is an ugly extension due to the parent class design, but the changed parts from the parent can be found between the >>>>>>>START<<<<<<< and >>>>>>>>END<<<<<<<< tags.
     */
    @Override
    public int produce(final int min,
            final int max,
            final int start,
            final int subpopulation,
            final Individual[] inds,
            final EvolutionState state,
            final int thread)
            {
            // grab individuals from our source and stick 'em right into inds.
            // we'll modify them from there
            int n = sources[0].produce(min,max,start,subpopulation,inds,state,thread); //sources[0].produce is a tournament selection

            // should we bother?
            if (!state.random[thread].nextBoolean(likelihood))
                return reproduce(n, start, subpopulation, inds, state, thread, false);  // DON'T produce children from source -- we already did


            GPInitializer initializer = ((GPInitializer)state.initializer);

            // now let's mutate 'em
            for(int q=start; q < n+start; q++) //q=start = 0  q<1
                {
                GPIndividual i = (GPIndividual)inds[q];

                if (tree!=TREE_UNFIXED && (tree<0 || tree >= i.trees.length))
                    // uh oh
                    state.output.fatal("GP Mutation Pipeline attempted to fix tree.0 to a value which was out of bounds of the array of the individual's trees.  Check the pipeline's fixed tree values -- they may be negative or greater than the number of trees in an individual");

                // pick random tree
            /*    if (tree==TREE_UNFIXED)
                    if (i.trees.length>1) t = state.random[thread].nextInt(i.trees.length);
                    else t = 0;
                else t = tree;*/

                int length = i.trees.length;
                GPNode[] p1 = new GPNode[length]; //save the mutation point
                GPNode[] p2 = new GPNode[length]; //save new generated subtree
                boolean res = false;
                // validity result...
                for(int t = 0; t < length; t++) {
                	
                   for(int x=0;x<numTries;x++)
                         {
                         // prepare the nodeselector
                         nodeselect.reset();

                         // pick a node

                         GPNode p11=null;  // the node we pick
                         GPNode p21=null;  //new generated subtree
                         // pick a node in individual 1
                         p11 = nodeselect.pickNode(state,subpopulation,thread,i,i.trees[t]);

                         // generate a tree swap-compatible with p1's position

                         int size = GPNodeBuilder.NOSIZEGIVEN;
                         if (equalSize) size = p11.numNodes(GPNode.NODESEARCH_ALL);

                         p21 = builder.newRootedTree(state,
                             p11.parentType(initializer),
                             thread,
                             p11.parent,
                             i.trees[t].constraints(initializer).functionset,
                             p11.argposition,
                             size);

                         // check for depth and swap-compatibility limits
                         res = verifyPoints(p21,p11);  // p2 can fit in p1's spot  -- the order is important! //p2: the new generated subtree

                         // did we get something that had both nodes verified?
                         if (res) {
                        	 p1[t] = p11;
                        	 p2[t] = p21;              	 
                         	 break;
                         }

                       }
                }

                GPIndividual j;
                		 j = (GPIndividual)(i.lightClone());
                         // Fill in various tree information that didn't get filled in there
                         j.trees = new GPTree[i.trees.length];

                         // at this point, p1 or p2, or both, may be null.
                         // If not, swap one in.  Else just copy the parent.
                         for(int x = 0;x < j.trees.length;x++)
                             {
                        	 if(p1[x] != null)
                              // we've got a tree with a kicking cross position!
                                 {
                                 j.trees[x] = (GPTree)(i.trees[x].lightClone());
                                 j.trees[x].owner = j;
                                 j.trees[x].child = i.trees[x].child.cloneReplacingNoSubclone(p2[x],p1[x]);
                                 j.trees[x].child.parent = j.trees[x];
                                 j.trees[x].child.argposition = 0;
                                 j.evaluated = false;
                                 } // it's changed
                             else
                                 {
                                 j.trees[x] = (GPTree)(i.trees[x].lightClone());
                                 j.trees[x].owner = j;
                                 j.trees[x].child = (GPNode)(i.trees[x].child.clone());
                                 j.trees[x].child.parent = j.trees[x];
                                 j.trees[x].child.argposition = 0;
                                 }
                             }
                         
            // add the new individual, replacing its previous source
                inds[q] = j;
                }
            return n;
            }
}
