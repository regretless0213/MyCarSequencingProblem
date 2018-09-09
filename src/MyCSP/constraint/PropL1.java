package MyCSP.constraint;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;

// 变量满足平均数--ok
public class PropL1 extends Propagator<IntVar> {

	/**
	 * The position of the last positive coefficient
	 */
	// final int pos;

	private static final long serialVersionUID = 6276623079109330844L;

	/**
	 * Number of variables
	 */
	final int l;

	/**
	 * Bound to respect
	 */
	// final int b;

	/**
	 * Variability of each variable (ie domain amplitude)
	 */
	final int[] I;

	/**
	 * SUm of lower bounds
	 */
	int sumLB;

	/**
	 * Sum of upper bounds
	 */
	int sumUB;

	int mean;
	boolean hasmean;
	int sum;
	
	public PropL1(IntVar[] variables, int mean, boolean hasmean) {
		this(variables, mean, hasmean, computePriority(variables.length), false);
	}

	PropL1(IntVar[] variables, int mean, boolean hasmean, PropagatorPriority priority, boolean reactOnFineEvent) {
		super(variables, priority, reactOnFineEvent);
		l = variables.length;
		I = new int[l];
		this.mean = mean;
		this.hasmean = hasmean;
		this.sum = mean*l;
		super.linkVariables();
	}

	protected static PropagatorPriority computePriority(int nbvars) {
		if (nbvars == 1) {
			return PropagatorPriority.UNARY;
		} else if (nbvars == 2) {
			return PropagatorPriority.BINARY;
		} else if (nbvars == 3) {
			return PropagatorPriority.TERNARY;
		} else {
			return PropagatorPriority.LINEAR;
		}
	}

	private int getOtherSumLB(IntVar v) {
		return sumLB-v.getLB();
	}
	
	private int getOtherSumUB(IntVar v) {
		//System.out.println("sumUB="+sumUB+"  v.UB"+v.getUB()+"  return="+(sumUB-v.getUB()));
		return sumUB-v.getUB();
	}

	private void updateLBUB() {
		sumLB = 0;
		sumUB = 0;
		for (int i = 0; i < l; i++) {
			sumUB += vars[i].getUB();
			sumLB += vars[i].getLB();
			//System.out.print(vars[i].toString()+"  ");
		}
		//System.out.println();
	}
	
	private void updateBounds() throws ContradictionException {
		if(hasmean) {
			for (int i = 0; i < l; i++) {
				IntVar v = vars[i];
				int lb = v.getLB();
				int ub = v.getUB();
				updateLBUB();
				for(;lb<=ub;lb = v.nextValue(lb)) {
					if(lb+getOtherSumUB(v)<sum) {
						//System.out.println("lb"+lb+"+sumu"+getOtherSumUB(v)+"<"+sum+"  remove:"+v.getName()+" "+lb);
						v.removeValue(lb, this);
					}
				}
				lb=v.getLB();
				ub = v.getUB();
				updateLBUB();
				for(;ub>=lb;ub=v.previousValue(ub)) {
					if(ub+getOtherSumLB(v)>sum) {
						//System.out.println("ub"+ub+"+suml"+getOtherSumLB(v)+">"+sum+"  remove:"+v.getName()+" "+ub);
						v.removeValue(ub, this);
					}
				}
			}
		}else {
			mean = sumLB/l;
		}
		//System.out.println("sumLB=" + sumLB + " sumUB=" + sumUB);
	}

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		//System.out.println("evtmask:"+evtmask);
		updateBounds();
		boolean pass = true;
		for (IntVar v : vars) {
			if (!v.isInstantiated()) {
				pass = false;
			}
		}
		if (pass) {
			setPassive();
		}
	}

	@Override
	public ESat isEntailed() {
		// TODO Auto-generated method stub
		return null;
	}

}
