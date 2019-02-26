
package MyCSP.heuristic.values;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.variables.IntVar;

public final class SelectionAggregation implements IntValueSelector {

	private TIntList bests;

	private java.util.Random random;

	private IntVar[] CarSeq; // 变量数组
	private int[][] matrix, optfreq; // 传递过来的0/1矩阵、容量矩阵
	private int[] demands, result, slotava; // 传递过来的每个类的需求;存储动态下每个零件已经安装了几次；每个零件对应的剩余插槽数
//	private int slots; // 总插槽数

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
			// 统计中间值矩阵中各零件已被赋值的次数
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
			// 计算剩余插槽数
			for (int n = 0; n < length; n++) {
				int m = 0;
				int count = 0;
				int index = 0;
				for (; m < retmp.length; m++) {
					int row = retmp[m];
					// 已赋值的部分一定满足约束
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
			double weight = load(matrix[idx]); // 权值
			if (weight > _d) {
				bests.clear();
				bests.add(idx);
				_d = weight;
			} else if (_d == weight) {
				bests.add(idx);
			}
		}
//		System.out.println("最优值列表大小"+bests.size());
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

	// 常数
	private double weight(int[] v) {
		double w = 0;
		for (int i = 0; i < v.length; i++) {
			w += v[i];
		}
//		System.out.print(w + " ");
		return w;
	}

	// 容量
	private double capacity(int[] v) {
		double w = 0;
		for (int i = 0; i < v.length; i++) {
			w += v[i] * optfreq[i][1] / optfreq[i][0];
		}
		return w;

	}

	// 动态
	// 剩余需求
	private double redemand(int[] v) {
		double w = 0;
		// 动态剩余需求如何计算
		for (int i = 0; i < v.length; i++) {
			w += v[i] * (demand(i) - result[i]);
		}
//		System.out.print(w + " ");
		return w;
	}

	private double demand(int optnum) {
		double d = 0;
		// System.out.println("计算需求");
		for (int i = 0; i < matrix.length; i++) {
			d += matrix[i][optnum] * demands[i];
			// System.out.print("matrix "+ matrix[i][optnum] + " demands "+ demands[i]);
		}
//		System.out.println(" d=" + d);
		return d;
	}

	// 负载
	private double load(int[] v) {
		double w = 0;
		// 动态剩余需求如何计算
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

	// 松弛度
	private double slack(int[] v) {
		double w = 0;
		// 动态剩余需求如何计算
		for (int i = 0; i < v.length; i++) {
//			w += v[i] * (slots - demand(i) + loadcompute(i));
//			w += v[i] * (slots - (demand(i) - result[i]) + loadcompute(i));
			// 未完成
			w += v[i] * (sumofArray(slotava) - slotava[i] + loadcompute(i));
		}
//		System.out.print(w + " ");
		return w;
	}

	// 利用率
	private double usagerate(int[] v) {
		double w = 0;
		// 动态剩余需求如何计算
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
	
	// 测试
	private double neweight(int[] v) {
		double w = 0;
		// 动态剩余需求如何计算
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
