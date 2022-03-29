package yimei.jss.niching;

import ec.EvolutionState;
import ec.coevolve.MultiPopCoevolutionaryEvaluator;
import ec.util.Parameter;
import yimei.jss.helper.PopulationUtils;
import yimei.jss.simulation.state.SystemState;

/**
 * Created by dyska on 26/09/17.
 */
//a class can not extend from more than one class
public class MultiPopCoevolutionaryClearingEvaluator extends MultiPopCoevolutionaryEvaluator {
    public static final String P_RADIUS = "radius";
    public static final String P_CAPACITY = "capacity";
    
    //fzhang 2018.10.9 to get the pre-generation value
    public static final String P_PRE_GENERATIONS = "pre-generations";
    private int preGenerations;

    protected boolean clear = true;

    protected double radius;
    protected int capacity;

    protected PhenoCharacterisation[] phenoCharacterisation;

    public double getRadius() {
        return radius;
    }

    public int getCapacity() {
        return capacity;
    }

    public PhenoCharacterisation[] getPhenoCharacterisation() {
        return phenoCharacterisation;
    }

    public PhenoCharacterisation getPhenoCharacterisation(int index) {
        return phenoCharacterisation[index];
    }

    public void setup(final EvolutionState state, final Parameter base) {
        super.setup(state, base);

        radius = state.parameters.getDoubleWithDefault(
                base.push(P_RADIUS), null, 0.0);
        capacity = state.parameters.getIntWithDefault(
                base.push(P_CAPACITY), null, 1);
        String filePath = state.parameters.getString(new Parameter("filePath"), null);
        //It's a little tricky to know whether we have 1 or 2 populations here, so we will assume
        //2 for the purpose of the phenoCharacterisation, and ignore the second object if only
        //1 is used
        phenoCharacterisation = new PhenoCharacterisation[2];
        if (filePath == null) {
            //dynamic simulation
            phenoCharacterisation[0] =
                    SequencingPhenoCharacterisation.defaultPhenoCharacterisation();
            phenoCharacterisation[1] =
                    RoutingPhenoCharacterisation.defaultPhenoCharacterisation();
        } else {
            //static simulation
            phenoCharacterisation[0] =
                    SequencingPhenoCharacterisation.defaultPhenoCharacterisation(filePath);
            phenoCharacterisation[1] =
                    RoutingPhenoCharacterisation.defaultPhenoCharacterisation(filePath);
        }
    }

    @Override
    public void evaluatePopulation(final EvolutionState state) {
        super.evaluatePopulation(state); //fzhang  all the evolution process is the same. for one individual, get a fitness value
        
        //fzhang 2018.10.9  only use niching at predefined generation
        //2019.1.22 fzhang before predefined generation, should always use niching 
        //================start===============================
 /*       preGenerations = state.parameters.getIntWithDefault(
                new Parameter(P_PRE_GENERATIONS), null, -1);  //50
           
        if(state.generation == preGenerations)
        	clear = true;
        else
            clear = false;*/
        //======================end===================================
        
        //original one
        //=====================start===========================
     /*   PopulationUtils.sort(state.population);
        for(int i = 0; i<state.population.subpops.length; i++){
            System.out.println("i: " + i);
            for(int ind = 0; ind < state.population.subpops[i].individuals.length; ind++){
                System.out.println(state.population.subpops[i].individuals[ind].fitness.fitness());
            }
        }
*/
        if (clear) {
        	//  System.out.println("MultiPopCoevolution");
            Clearing.clearPopulation(state, radius, capacity,
                    phenoCharacterisation);
        }
        //======================end============================

  /*      for(int i = 0; i<state.population.subpops.length; i++){
            System.out.println(" after clearing:  \n i: " + i);
            for(int ind = 0; ind < state.population.subpops[i].individuals.length; ind++){
                System.out.println(state.population.subpops[i].individuals[ind].fitness.fitness());
            }
        }*/
    }

    public void setClear(boolean clear) {
        this.clear = clear;
    }
}
