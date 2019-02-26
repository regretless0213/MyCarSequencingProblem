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

	private IntVar[] CarSeq; // ��������
	private int[][] matrix, optfreq; // ���ݹ�����0/1������������
	private int[] demands, result,slotava; // ���ݹ�����ÿ���������;�洢��̬��ÿ������Ѿ���װ�˼���
//	private int slots; // �ܲ����
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

			}
		}

		int up = var.getUB();
		domain = var.getDomainSize();

		ArrayList<Integer> idlist = new ArrayList<Integer>();// ֵidx�б�
		ArrayList<Double> wglist = new ArrayList<Double>(); // ��Ӧ��Ȩֵ�б�
		// System.out.println(domain);
		for (int idx = var.getLB(); idx <= up; idx = var.nextValue(idx)) {
			// System.out.print("value ");
			// System.out.print(idx + " ");

			// �洢Ȩֵ

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

	// ��׼��
	private int standardize(ArrayList<Integer> il, ArrayList<Double> dl, int cv) {
		Map<Integer, Double> hmresult = new HashMap<Integer, Double>();// ���������׼�������ʱ���

		int currentVar = cv;// ������������ֵ
		for (int j = 0; j < 2; j++) {// 2��Ȩֵ��2�����飬�ֱ���б�׼��
			double[] w = new double[domain];
			int num = 0;
			for (int i = j; i < dl.size(); i = i + 2) {
				w[num] = dl.get(i);
				num++;
			}
			num = 0;

			// System.out.print("Ȩֵ��");
			// for(double t:w) {
			// System.out.print(t+" ");
			// }
			// System.out.println();

			w = normalize(w);// �޸�����Ȩֵ

			// System.out.print("�޸ĺ�Ȩֵ��");
			// for(double t:w) {
			// System.out.print(t+" ");
			// }
			// System.out.println();

			for (int i = j; i < dl.size(); i = i + 2) {

				dl.set(i, w[num]);// �޸�Ȩֵ�б��е�Ȩֵ
				num++;
			}
		}
		for (int n = 0; n < dl.size(); n++) {// ����ֵidx������׼�����Ȩֵ�б��ж�Ӧ��2��Ȩֵ���м򵥵ļӺʹ��������浽��ϣ����
			int id = il.get(n);
			double wg = dl.get(n);
			if (hmresult.get(id) != null) {
				wg += hmresult.get(id);
				hmresult.put(id, wg);
			} else {
				hmresult.put(id, wg);
			}
		}
		// �Թ�ϣ���������ʹ��Ȩֵ��ߵ�ֵidx�ڱ�񶥲�
		ByValueComparator bvc = new ByValueComparator(hmresult);
		List<Map.Entry<Integer, Double>> ll = new ArrayList<Map.Entry<Integer, Double>>(hmresult.entrySet());
		Collections.sort(ll, bvc);

		// for (Map.Entry<Integer, Double> r : ll) {
		// System.out.println(r.getKey() + " " + r.getValue());
		// }

		ArrayList<Integer> best = new ArrayList<Integer>();// ��Ȩֵ��ߵ�ֵidx��Ψһʱ�����������

		
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

	// ��һ��
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

	// ��Сֵ
	private static double min(double[] array) {
		double minValue = array[0];
		for (int i = 0; i < array.length; i++) {
			if (array[i] < minValue)
				minValue = array[i];
		}
		return minValue;

	}

	// ���ֵ
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
		// System.out.println("��������");
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

	// �ɳڶ�
	private double slack(int[] v) {
		double w = 0;
		// ��̬ʣ��������μ���
		for (int i = 0; i < v.length; i++) {
//			w += v[i] * (slots - demand(i) + loadcompute(i));
//			w += v[i] * (slots - (demand(i) - result[i]) + loadcompute(i));
			//δ���
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
			w += v[i] * (loadcompute(i) / slotava[i]);
		}
//		System.out.print(w + " ");
		return w;
	}
}
