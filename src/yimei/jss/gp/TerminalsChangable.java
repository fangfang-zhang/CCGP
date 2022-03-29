package yimei.jss.gp;

import ec.EvolutionState;
import ec.gp.GPNode;

import java.util.List;

/**
 * An EvolutionState class with this interface
 * means that the terminal set is changable
 * during the evolutionary process.
 *
 * Created by YiMei on 6/10/16.
 */
public interface TerminalsChangable {

    GPNode[][] getTerminals();
   /* //fzhang 16.7.2018 in order to initialize population with multiple trees  F
    GPNode[] getMultiTerminals(int numTrees);*/
    
    GPNode[] getTerminals(int subPopNum);
    void setTerminals(GPNode[][] terminals);
    void adaptPopulation(int subPopNum);
}
