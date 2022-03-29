package yimei.jss.niching;

import ec.EvolutionState;
import ec.multiobjective.MultiObjectiveFitness;

/**
 * The multi-objective fitness with clearing method for niching.
 *
 * Created by yimei on 21/11/16.
 */
public class ClearingMultiObjectiveFitness
        extends MultiObjectiveFitness implements Clearable {

    private boolean cleared;
  
    @Override //when want to clear population, call method .clear()
    public void clear() {
        for (int i = 0; i < objectives.length; i++) {
            if (maximize[i]) {
                objectives[i] = Double.NEGATIVE_INFINITY; // when this is a maximize objective, set the bad objective to negative value
            }
            else {
                objectives[i] = Double.POSITIVE_INFINITY; // when this is a minimize objective, set the bad objective to positive value
            }
        }

        cleared = true;
    }

    @Override
    public boolean isCleared() {
        return cleared;
    }

    public void setObjectives(final EvolutionState state, double[] newObjectives) {
//    	System.out.println("setObjectives");
        super.setObjectives(state, newObjectives);

        cleared = false; //fzhang    label whether it is cleared or not
    }
}
