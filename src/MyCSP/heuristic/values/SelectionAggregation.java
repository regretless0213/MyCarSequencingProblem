/**
 * Copyright (c) 2016, Ecole des Mines de Nantes
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *    This product includes software developed by the <organization>.
 * 4. Neither the name of the <organization> nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package MyCSP.heuristic.values;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.variables.IntVar;

/**
 * Implementation of DowOverWDeg[1].
 *
 * [1]: F. Boussemart, F. Hemery, C. Lecoutre, and L. Sais, Boosting Systematic
 * Search by Weighting Constraints, ECAI-04. <br/>
 *
 * @author Charles Prud'homme
 * @since 12/07/12
 */
public final class SelectionAggregation implements IntValueSelector {

	/**
	 * Temporary. Stores index of variables with the same (best) score
	 */
	private TIntList bests;

	/**
	 * Randomness to break ties
	 */
	private java.util.Random random;

	private IntVar[] CarSeq; // 变量数组
	private int[][] matrix, optfreq; // 传递过来的0/1矩阵、容量矩阵
	private int[] demands, result; // 传递过来的每个类的需求;存储动态下每个零件已经安装了几次
	private int slots; // 总插槽数

	public SelectionAggregation(IntVar[] vars, long seed, int[][] options, int[][] frequency, int[] nums) {
		bests = new TIntArrayList();
		random = new java.util.Random(seed);
		matrix = options;
		optfreq = frequency;
		demands = nums;
		CarSeq = vars;
		slots = 0;

		for (int j = 0; j < matrix.length; j++) {
			for (int i = 0; i < matrix[0].length; i++) {
				slots += matrix[j][i] * demands[j];
			}
		}
		// System.out.println(slots);
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
//					System.out.print("result" + n + " " + result[n]);
				}
//				System.out.println();
			}
		}

		double _d = 0;
		int up = var.getUB();
		for (int idx = var.getLB(); idx <= up; idx = var.nextValue(idx)) {
			// System.out.print("value ");
			// System.out.print(idx + " ");
			double weight = load(matrix[idx]); //权值
			if (weight > _d) {
				bests.clear();
				bests.add(idx);
				_d = weight;
			} else if (_d == weight) {
				bests.add(idx);
			}
		}
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
			w += v[i] * (slots - (demand(i) - result[i]) + loadcompute(i));
		}
//		System.out.print(w + " ");
		return w;
	}

	// 利用率
	private double usagerate(int[] v) {
		double w = 0;
		// 动态剩余需求如何计算
		for (int i = 0; i < v.length; i++) {
			w += v[i] * (loadcompute(i) / demand(i));
		}
//		System.out.print(w + " ");
		return w;
	}
}
