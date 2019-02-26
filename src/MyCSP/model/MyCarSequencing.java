
package MyCSP.model;

import org.chocosolver.samples.AbstractProblem;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.variables.IntVar;
import org.kohsuke.args4j.Option;

import MyCSP.model.data.DataSet.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.chocosolver.solver.search.strategy.Search.MyHeuristicSearch;

public class MyCarSequencing extends AbstractProblem {
	private List<String> result;
	public MyCarSequencing(List<String> rtmp) {
		// TODO Auto-generated constructor stub
		result = rtmp;
	}

	@Option(name = "-d", aliases = "--data", usage = "Car sequencing data.", required = false)
	CSPLib data = CSPLib.valueOf("random18");
//	Data data = Data.P4_72;
//	MyData data = MyData.md;
//
	IntVar[] CarSeq;
	IntVar[][] cars;

	int nCars, nClasses, nOptions;

	int[] demands;
	int[][] optfreq, matrix;

	@Override
	public void buildModel() {
		model = new Model("CarSequencing");
		parse(data.source());
		int[] darray = new int[nOptions];
		for(int i = 0;i< nOptions;i++) {
			darray[i] = dscompute(matrix, i);
		}

		int max = nClasses - 1;
		CarSeq = model.intVarArray("CarSeq", nCars, 0, max, false);

		cars = model.intVarMatrix("cars", nCars, nOptions, 0, 1);

		Tuples tp = new Tuples(true);
		for (int i = 0; i < nClasses; i++) {
			int[] row = new int[nOptions + 1];
			row[0] = i;
			for (int j = 0; j < nOptions; j++) {
				row[j + 1] = matrix[i][j];
			}
			tp.add(row);
		}
		for (int i = 0; i < nCars; i++) {
			IntVar[] row = new IntVar[nOptions + 1];
			row[0] = CarSeq[i];
			for (int j = 0; j < nOptions; j++) {
				row[j + 1] = cars[i][j];
			}
			model.table(row, tp).post();
		}
		// for (int i = 0; i < nCars; i++) {
		//
		// model.table(cars[i], tp).post();
		// }

		for (int i = 0; i < nOptions; i++) {
			

			
			//table建模
			int n = optfreq[i][0];
			int m = optfreq[i][1];
			int[][] tps = geneAllowedTuple(n, m);
			Tuples facTuple = geneFactorTuples(tps);

			Tuples binTuple = geneConnection(tps, tps, n, m);

			int res = nCars % m;
			int facNum = nCars / m;
			if (res != 0) {
				facNum++;
			}
			IntVar[] facVars = new IntVar[facNum];
			for (int j = 0; j < nCars / m; j++) {
				IntVar[] column1 = new IntVar[m + 1];
				for (int k = 0; k < m; k++) {
					column1[k] = cars[j * m + k][i];
				}
				facVars[j] = model.intVar("B[" + i + "][" + j + "]", 0, tps.length - 1);
				column1[m] = facVars[j];
				model.table(column1, facTuple, "CT+").post();
			}
			for (int j = 0; j < nCars / m - 1; j++) {
				model.table(facVars[j], facVars[j + 1], binTuple, "AC3bit+rm").post();
			}

			if (res != 0) {
				int[][] rtps = geneAllowedTuple(n, res);
				Tuples rtp = geneFactorTuples(rtps);
				facVars[facNum - 1] = model.intVar("B[" + i + "][" + (facNum - 1) + "]", 0, rtps.length - 1);

				IntVar[] column = new IntVar[res + 1];
				for (int k = 0; k < res; k++) {
					column[k] = cars[(facNum - 1) * m + k][i];
				}
				column[res] = facVars[facNum - 1];
				model.table(column, rtp, "CT+").post();

				Tuples rbinT = geneConnection(tps, rtps, n, m);
				model.table(facVars[facNum - 2], facVars[facNum - 1], rbinT, "AC3bit+rm").post();
			}
			
			//对每个零件进行总数约束
			IntVar[] sumarray = extractor(cars,0,nCars,i);
			model.sum(sumarray, "=",darray[i]).post();
		}


		
		//全局约束
		int[] values = new int[nClasses];
		IntVar[] expArray = new IntVar[nClasses];
		for (int i = 0; i < nClasses; i++) {
			expArray[i] = model.intVar("var_" + i, 0, demands[i], false);
			values[i] = i;
		}
		model.globalCardinality(CarSeq, values, expArray, false).post();

	}
	
	public Tuples geneFactorTuples(int[][] tuples) {
		int m = tuples.length;
		int n = tuples[0].length;
		int[][] factorTuples = new int[m][n + 1];
		for (int j = 0; j < m; j++) {
			for (int k = 0; k < n; k++) {
				factorTuples[j][k] = tuples[j][k];
			}
			factorTuples[j][n] = j;
		}
		Tuples facTuple = new Tuples(true);
		facTuple.add(factorTuples);
		return facTuple;
	}

	public Tuples geneConnection(int[][] tps1, int[][] tps2, int n, int m) {
		ArrayList<int[]> list = new ArrayList<int[]>(m * n);
		for (int i = 0; i < tps1.length; i++) {
			int[] t1 = tps1[i];
			for (int j = 0; j < tps2.length; j++) {
				int[] t2 = tps2[j];
				boolean conflict = false;
				for (int k = 1; k < m; k++) {
					int num = 0;
					for (int p = k; p < m; p++) {
						num += t1[p];
					}
					int tt1 = tps2[0].length;
					tt1 = tt1 > k ? k : tt1;
					for (int q = 0; q < tt1; q++) {
						num += t2[q];
					}
					if (num > n) {
						conflict = true;

					}
				}
				if (!conflict) {
					int[] tt = new int[2];
					tt[0] = i;
					tt[1] = j;
					list.add(tt);
				}
			}
		}
		int[][] binary = new int[0][0];
		binary = list.toArray(binary);
		Tuples facTuple = new Tuples(true);
		facTuple.add(binary);
		return facTuple;
	}

	public int[][] geneAllowedTuple(int n, int m) {
		ArrayList<int[]> list = new ArrayList<int[]>();
		list.add(new int[m]);

		CombineTool ct = new CombineTool();
		int[][] com = ct.genResult(n, m);
		int[][] tuples = new int[com.length + 1][m];
		for (int i = 0; i < com.length; i++) {
			for (int j = 0; j < com[i].length; j++) {
				tuples[i + 1][com[i][j]] = 1;
			}
		}

		return tuples;
	}
	//将矩阵中的一列数据提取出来
	private static IntVar[] extractor(IntVar[][] mac, int initialNumber, int amount, int num) {// 提取矩阵中第一列数据
		if ((initialNumber + amount) > mac.length) {
			amount = mac.length - initialNumber;
		}
		IntVar[] tmp = new IntVar[amount];
		for (int m = 0; m < amount; m++) {
			tmp[m] = mac[initialNumber + m][num];
		}
		return tmp;

	}
	//计算单一零件的总需求量
	private int dscompute(int[][] matrixtmp, int num) {
		int d = 0;
		for (int m = 0; m < nClasses; m++) {
			d += matrixtmp[m][num]* demands[m];
		}
//		System.out.println(d);
		return d;
		
	}

	@Override
	public void configureSearch() {
		model.getSolver().setSearch(MyHeuristicSearch(CarSeq, matrix, optfreq, demands));
		model.getSolver().limitTime("20m");
		// model.getSolver().setSearch(inputOrderLBSearch(CarSeq));
	}

	@Override
	public void solve() {
		model.getSolver().solve();
//		model.getSolver().printStatistics();
		// 打印结果
		for (int i = 0; i < nCars; i++) {
			//将结果存储到List中
			result.add(model.getVars()[i].toString());
//			System.out.println(model.getVars()[i]);
//			if (i < nCars) {
//				System.out.println();
//			}
//			if ((i >= nCars) && ((i + 1 - nCars) % nOptions == 0) && ((i - nCars) < (nOptions * nCars))) {
//				System.out.println();
//			}
//			if ((i - nCars) >= (nOptions * nCars)) {
//				System.out.println();
//			}
		}

	}

	public static void main(String[] args) {
		List<String> r = new ArrayList<>();
		new MyCarSequencing(r).execute(args);
		System.out.println("列表：");
		for(String tmp : r) {
			System.out.println(tmp);
		}
	}

	private int[][] parse(String source) {
		int[][] data = null;
		Scanner sc = new Scanner(source);
		nCars = sc.nextInt();
		nOptions = sc.nextInt();
		nClasses = sc.nextInt();

		optfreq = new int[nOptions][2];
		// get frequencies
		for (int i = 0; i < nOptions; i++) {
			optfreq[i][0] = sc.nextInt();
		}
		for (int i = 0; i < nOptions; i++) {
			optfreq[i][1] = sc.nextInt();
		}

		// get the demand and options
		demands = new int[nClasses];
		matrix = new int[nClasses][nOptions];
		for (int i = 0; i < nClasses; i++) {
			sc.nextInt();
			demands[i] = sc.nextInt();
			for (int j = 0; j < nOptions; j++) {
				matrix[i][j] = sc.nextInt();
			}
		}
		sc.close();
		return data;
	}

}


