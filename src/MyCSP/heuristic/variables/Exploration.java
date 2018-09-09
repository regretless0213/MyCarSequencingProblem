
package MyCSP.heuristic.variables;

import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.search.strategy.selectors.variables.VariableSelector;
import org.chocosolver.solver.variables.Variable;


public class Exploration<V extends Variable> implements VariableSelector<V> {

    private IStateInt lastIdx; // index of the last non-instantiated variable


    public Exploration(Model model){
        lastIdx = model.getEnvironment().makeInt(0);
    }

    @Override
    public V getVariable(V[] variables) {
        for (int idx = lastIdx.get(); idx < variables.length; idx++) {
            if (!variables[idx].isInstantiated()) {
                lastIdx.set(idx);
                return variables[idx];
            }
        }
        lastIdx.set(variables.length);
        return null;
    }
}

