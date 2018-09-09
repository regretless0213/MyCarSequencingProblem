package MyCSP.heuristic.values;

import org.chocosolver.solver.variables.IntVar;

public class DynamicInformation {
	private static IntVar[] array;
	private static int[] result;

	public DynamicInformation(IntVar[] CarSeq) {
		array = CarSeq;
	}

	public int getValue(IntVar var) {
		if (var.isInstantiated()) {
			return var.getValue();
		} else {
			return -1;
		}
	}

	public int[] getICarSeq() {
		int[] tmp = new int[array.length];
		int num = 0;
		for (int i = 0; i < array.length; i++) {
			tmp[i] = getValue(array[i]);
			if (tmp[i] > -1) {
				num += 1;
			}
		}
		if (num > 0) {
			result = new int[num];
			int cn = 0;
			for(int tp:tmp) {
				if(tp>-1) {
					result[cn] = tp;
					cn++;
				}
			}
			return result;
		}else {
			return null;
		}
		
	}

}
