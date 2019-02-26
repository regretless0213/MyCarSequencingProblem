package MyCSP.heuristic.values;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.variables.IntVar;

public final class SUStandardization implements IntValueSelector {

	private TIntList bests;
	private java.util.Random random;

	private IntVar[] CarSeq; // 变量数组
	private int[][] matrix, optfreq; // 传递过来的0/1矩阵、容量矩阵
	private int[] demands, result,slotava; // 传递过来的每个类的需求;存储动态下每个零件已经安装了几次
//	private int slots; // 总插槽数
	private int domain;

	public SUStandardization(IntVar[] vars, long seed, int[][] options, int[][] frequency, int[] nums) {
		bests = new TIntArrayList();

		random = new java.util.Random(seed);
		matrix = options;
		optfreq = frequency;
		demands = nums;
		CarSeq = vars;
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
			for (int m = 0; m < retmp.length; m++) {
				int row = retmp[m];
				// System.out.println(row);
				for (int n = 0; n < length; n++) {
					// System.out.println(result[n]);
					result[n] += matrix[row][n];
					// System.out.print("result" + n + " " + result[n]);
				}
				// System.out.println();
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

			}
		}

		int up = var.getUB();
		domain = var.getDomainSize();

		ArrayList<Integer> idlist = new ArrayList<Integer>();// 值idx列表
		ArrayList<Double> wglist = new ArrayList<Double>(); // 对应的权值列表
		// System.out.println(domain);
		for (int idx = var.getLB(); idx <= up; idx = var.nextValue(idx)) {
			// System.out.print("value ");
			// System.out.print(idx + " ");

			// 存储权值

			idlist.add(idx);
			wglist.add(slack(matrix[idx]));

			idlist.add(idx);
			wglist.add(usagerate(matrix[idx]));

		}
		// System.out.println();
		// if (bests.size() > 0) {
		// int currentVar = bests.get(random.nextInt(bests.size()));
		// return currentVar;
		// }else {
		// System.out.println("weight error!");
		// System.exit(0);
		// return up;
		// }
		// System.out.println();

		return standardize(idlist, wglist, up);

	}

	// 标准化
	private int standardize(ArrayList<Integer> il, ArrayList<Double> dl, int cv) {
		Map<Integer, Double> hmresult = new HashMap<Integer, Double>();// 用来保存标准化后的临时结果

		int currentVar = cv;// 用来保存最优值
		for (int j = 0; j < 2; j++) {// 2种权值。2个数组，分别进行标准化
			double[] w = new double[domain];
			int num = 0;
			for (int i = j; i < dl.size(); i = i + 2) {
				w[num] = dl.get(i);
				num++;
			}
			num = 0;

			// System.out.print("权值：");
			// for(double t:w) {
			// System.out.print(t+" ");
			// }
			// System.out.println();

			w = normalize(w);// 修改数组权值

			// System.out.print("修改后权值：");
			// for(double t:w) {
			// System.out.print(t+" ");
			// }
			// System.out.println();

			for (int i = j; i < dl.size(); i = i + 2) {

				dl.set(i, w[num]);// 修改权值列表中的权值
				num++;
			}
		}
		for (int n = 0; n < dl.size(); n++) {// 根据值idx，将标准化后的权值列表中对应的2种权值进行简单的加和处理，并保存到哈希表中
			int id = il.get(n);
			double wg = dl.get(n);
			if (hmresult.get(id) != null) {
				wg += hmresult.get(id);
				hmresult.put(id, wg);
			} else {
				hmresult.put(id, wg);
			}
		}
		// 对哈希表进行排序，使得权值最高的值idx在表格顶部
		ByValueComparator bvc = new ByValueComparator(hmresult);
		List<Map.Entry<Integer, Double>> ll = new ArrayList<Map.Entry<Integer, Double>>(hmresult.entrySet());
		Collections.sort(ll, bvc);

		// for (Map.Entry<Integer, Double> r : ll) {
		// System.out.println(r.getKey() + " " + r.getValue());
		// }

		ArrayList<Integer> best = new ArrayList<Integer>();// 当权值最高的值idx不唯一时进行随机处理

		
		int key = ll.get(0).getKey();
		best.add(key);
		double value = ll.get(0).getValue();
		for(int i = 1; i < ll.size();i++) {
			if(value == ll.get(i).getValue()) {
				best.add(ll.get(i).getKey());
				
			}
		}

		// for (int r : best) {
		// System.out.print(r + " ");
		// }
		try {
			int index = random.nextInt(best.size());
			// System.out.println(index);
			currentVar = best.get(index);
		} catch (Exception e) {
			System.out.println("weight error!");
		}
		return currentVar;
	}

	// 归一化
	private static double[] normalize(double[] da) {
		double min = min(da);
		double max = max(da);
		if (min == max) {
			min = 0;
		}
		if (max == 0) {
			max = 1;
		}
		for (int i = 0; i < da.length; i++) {
			da[i] = (da[i] - min) / (max - min);
		}
		return da;
	}

	// 最小值
	private static double min(double[] array) {
		double minValue = array[0];
		for (int i = 0; i < array.length; i++) {
			if (array[i] < minValue)
				minValue = array[i];
		}
		return minValue;

	}

	// 最大值
	private static double max(double[] array) {
		double maxValue = array[0];
		for (int i = 0; i < array.length; i++) {
			if (array[i] > maxValue)
				maxValue = array[i];
		}
		return maxValue;

	}



	private double demand(int optnum) {
		double d = 0;
		// System.out.println("计算需求");
		for (int i = 0; i < matrix.length; i++) {
			d += matrix[i][optnum] * demands[i];
			// System.out.print("matrix "+ matrix[i][optnum] + " demands "+ demands[i]);
		}
		// System.out.println(" d=" + d);
		return d;
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
			//未完成
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
			w += v[i] * (loadcompute(i) / slotava[i]);
		}
//		System.out.print(w + " ");
		return w;
	}
}
