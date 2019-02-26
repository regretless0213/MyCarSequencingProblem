
package MyCSP.heuristic.values;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.variables.IntVar;

public final class SelectionAggregation implements IntValueSelector {

	private TIntList bests;

	private java.util.Random random;

	private IntVar[] CarSeq; // ��������
	private int[][] matrix, optfreq; // ���ݹ�����0/1������������
	private int[] demands, result, slotava; // ���ݹ�����ÿ���������;�洢��̬��ÿ������Ѿ���װ�˼��Σ�ÿ�������Ӧ��ʣ������
//	private int slots; // �ܲ����

	public SelectionAggregation(IntVar[] vars, long seed, int[][] options, int[][] frequency, int[] nums) {
		bests = new TIntArrayList();
		random = new java.util.Random(seed);
		matrix = options;
		optfreq = frequency;
		demands = nums;
		CarSeq = vars;
//		slots = 0;
		slotava = new int[optfreq.length];

		for (int j = 0; j < slotava.length; j++) {
			slotava[j] = sumofArray(demands);
		}
		// System.out.println(slots);
	}

	private int sumofArray(int[] array) {
		int sum = 0;
		for (int i = 0; i < array.length; i++) {
			sum += array[i];
		}
		return sum;
	}

	@Override
	public int selectValue(IntVar var) {
		// TODO Auto-generated method stub
		// IntVar best = null;
		bests.clear();
		// System.out.println("selectvalue");
		DynamicInformation di = new DynamicInformation(CarSeq);
		int[] retmp = null;
		if (di.getICarSeq() != null) {
			retmp = di.getICarSeq();

			// System.out.print("retmp=");
			// for (int out : retmp) {
			// System.out.print(out + " ");
			// }
			// System.out.println();
		}

		int length = matrix[0].length;
		// System.out.println(length);
		result = new int[length];
		if (retmp != null) {
			// ͳ���м�ֵ�����и�����ѱ���ֵ�Ĵ���
			for (int m = 0; m < retmp.length; m++) {
				int row = retmp[m];
				// System.out.println(row);
				for (int n = 0; n < length; n++) {
					// System.out.println(result[n]);
					result[n] += matrix[row][n];
//					System.out.print("result" + n + " " + result[n]);
				}
//				System.out.println();
			}
			// ����ʣ������
			for (int n = 0; n < length; n++) {
				int m = 0;
				int count = 0;
				int index = 0;
				for (; m < retmp.length; m++) {
					int row = retmp[m];
					// �Ѹ�ֵ�Ĳ���һ������Լ��
					if (optfreq[n][0] == 1) {
						if (matrix[row][n] == 1) {
							m += optfreq[n][1] - 1;
						}
					} else if (optfreq[n][0] == 2) {
						if (matrix[row][n] == 1) {
							count++;
							if (count < 2) {
								index = m;
							} else {
								if (m - index > optfreq[n][1] - 1) {
									count = 1;
									index = m;
								} else {
									m = index + optfreq[n][1];
								}
								count = 0;
							}
						}
					}
				}
				slotava[n] = sumofArray(demands) - m;
				if (slotava[n] < 0) {
					slotava[n] = 0;
				}

			}
		}

		double _d = 0;
		int up = var.getUB();
		for (int idx = var.getLB(); idx <= up; idx = var.nextValue(idx)) {
			// System.out.print("value ");
			// System.out.print(idx + " ");
			double weight = load(matrix[idx]); // Ȩֵ
			if (weight > _d) {
				bests.clear();
				bests.add(idx);
				_d = weight;
			} else if (_d == weight) {
				bests.add(idx);
			}
		}
//		System.out.println("����ֵ�б��С"+bests.size());
		// System.out.println();
//		if (bests.size() > 0) {
//			int currentVar = bests.get(random.nextInt(bests.size()));
//			return currentVar;
//		}else {
//			System.out.println("weight error!");
//			System.exit(0);
//			return up;
//		}
		// System.out.println();
		int currentVar = up;
		try {
			currentVar = bests.get(random.nextInt(bests.size()));
		} catch (Exception e) {
			System.out.println("weight error!");
		}
		return currentVar;

	}

	// ����
	private double weight(int[] v) {
		double w = 0;
		for (int i = 0; i < v.length; i++) {
			w += v[i];
		}
//		System.out.print(w + " ");
		return w;
	}

	// ����
	private double capacity(int[] v) {
		double w = 0;
		for (int i = 0; i < v.length; i++) {
			w += v[i] * optfreq[i][1] / optfreq[i][0];
		}
		return w;

	}

	// ��̬
	// ʣ������
	private double redemand(int[] v) {
		double w = 0;
		// ��̬ʣ��������μ���
		for (int i = 0; i < v.length; i++) {
			w += v[i] * (demand(i) - result[i]);
		}
//		System.out.print(w + " ");
		return w;
	}

	private double demand(int optnum) {
		double d = 0;
		// System.out.println("��������");
		for (int i = 0; i < matrix.length; i++) {
			d += matrix[i][optnum] * demands[i];
			// System.out.print("matrix "+ matrix[i][optnum] + " demands "+ demands[i]);
		}
//		System.out.println(" d=" + d);
		return d;
	}

	// ����
	private double load(int[] v) {
		double w = 0;
		// ��̬ʣ��������μ���
		for (int i = 0; i < v.length; i++) {
			w += v[i] * loadcompute(i);
		}
//		System.out.print(w + " ");
		return w;
	}

	private double loadcompute(int optnum) {
		double l = (demand(optnum) - result[optnum]) * optfreq[optnum][1] / optfreq[optnum][0];
		return l;
	}

	// �ɳڶ�
	private double slack(int[] v) {
		double w = 0;
		// ��̬ʣ��������μ���
		for (int i = 0; i < v.length; i++) {
//			w += v[i] * (slots - demand(i) + loadcompute(i));
//			w += v[i] * (slots - (demand(i) - result[i]) + loadcompute(i));
			// δ���
			w += v[i] * (sumofArray(slotava) - slotava[i] + loadcompute(i));
		}
//		System.out.print(w + " ");
		return w;
	}

	// ������
	private double usagerate(int[] v) {
		double w = 0;
		// ��̬ʣ��������μ���
		for (int i = 0; i < v.length; i++) {
			int tmp = slotava[i];
			if (tmp == 0) {
				tmp = 1;
			}
			w += v[i] * (loadcompute(i) / tmp);
		}
//		System.out.print(w + " ");
		return w;
	}
	
	// ����
	private double neweight(int[] v) {
		double w = 0;
		// ��̬ʣ��������μ���
		for (int i = 0; i < v.length; i++) {
			int tmp = slotava[i];
			if (tmp == 0) {
				tmp = 1;
			}
			w += v[i] * (sumofArray(slotava) * loadcompute(i) / (sumofArray(slotava) - slotava[i] + loadcompute(i)));
		}
//		System.out.print(w + " ");
		return w;
	}

}
