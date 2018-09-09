package MyCSP;

import org.chocosolver.samples.AbstractProblem;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.variables.IntVar;

public class test  extends AbstractProblem{

	@Override
	public void buildModel() {
		// TODO Auto-generated method stub
		int[] p = {1,2,1,2,1};
		int[] q = {2,3,3,5,5};

//		Tuples t = 
		IntVar[][] sum = model.intVarMatrix("matrix", 5, 10, 0, 1);
		for(int i = 0; i < 5 ; i++) {
			Constraint cntr = model.sum(sum[i], "=", p[i]);
			for(int j = 0; j < q[i];j++) {
				IntVar[] subsum = new IntVar[q[i]];
//				subsum = 
//				Constraint cntrs = model.t
			}
		}
		
	}

	@Override
	public void solve() {
		// TODO Auto-generated method stub
		
	}

}
