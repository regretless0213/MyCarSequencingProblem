package MyCSP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyRandomDataUpdate {
	private List<List<Integer>> dataBase;
	private int sum;
	private int numofop;
	private int numofclass;

	private List<Integer> CapacityN;
	private List<Integer> CapacityD;

	public MyRandomDataUpdate(int s, int no, int nc) {
		sum = s;
		numofop = no;
		numofclass = nc;

	}

	// 构建基础数据
	public List<List<Integer>> getDataBase() {
		List<List<Integer>> classData = new ArrayList<List<Integer>>();
		List<Integer> tmp = new ArrayList<Integer>();
		for (int i = 0; i < 2; i++) {
			tmp.add(i);
		}
		for (int i = 0; i < numofop; i++) {
			classData.add(tmp);
		}
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		descartes(classData, result, 0, new ArrayList<Integer>());
		// System.out.println(sum(classData.get(0)));
		// classData.remove(0);
		result.remove(0);
		return result;

	}

	// 笛卡尔积函数
	private static void descartes(List<List<Integer>> dimvalue, List<List<Integer>> result, int layer,
			List<Integer> curList) {
		if (layer < dimvalue.size() - 1) {
			if (dimvalue.get(layer).size() == 0) {
				descartes(dimvalue, result, layer + 1, curList);
			} else {
				for (int i = 0; i < dimvalue.get(layer).size(); i++) {
					List<Integer> list = new ArrayList<Integer>(curList);
					list.add(dimvalue.get(layer).get(i));
					descartes(dimvalue, result, layer + 1, list);
				}
			}
		} else if (layer == dimvalue.size() - 1) {
			if (dimvalue.get(layer).size() == 0) {
				result.add(curList);
			} else {
				for (int i = 0; i < dimvalue.get(layer).size(); i++) {
					List<Integer> list = new ArrayList<Integer>(curList);
					list.add(dimvalue.get(layer).get(i));
					result.add(list);
				}
			}
		}
	}

	private int arraysum(List<Integer> array) {// 计算整形数组总和
		int sum = 0;
		for (int t : array) {
			sum += t;
		}

		return sum;
	}

	// private void printDB() {
	// for(List<Integer> ltmp : dataBase) {
	// for(int t:ltmp) {
	// System.out.print(t+" ");
	// }
	// System.out.println();
	// }
	// System.out.println(dataBase.size());
	// }


	public List<List<Integer>> getMyData() {
		dataBase = new ArrayList<List<Integer>>();
		List<List<Integer>> myData = new ArrayList<List<Integer>>();// 用来保存最终的结果数据
		dataBase = this.getDataBase();
		// printDB();
		int least = sum - numofclass + 1;
		int num = 0;
		for (int n = 0; n < numofclass - 1; n++) {
			// 随机抽取基础表中的一列
			int index = (int) (Math.random() * dataBase.size());
			List<Integer> dbtmp = dataBase.get(index);
			dataBase.remove(index);
			// 给出限制，防止产生的随机数过大，导致问题难解
			int lr = least;
			int dbs = arraysum(dbtmp);
			int ftmp = (numofop - dbs) * sum / numofclass;
			if (lr > ftmp / dbs) {
				lr = ftmp / dbs;
			}
			num = (int) (Math.random() * lr + 1);

			// 将随机抽取出来的数据保存到结果表中
			List<Integer> tmp = new ArrayList<Integer>();
			tmp.add(num);
			ArrayCopy(tmp, dbtmp);

			least = least - num + 1;
			// System.out.println(least+" "+num);
			myData.add(tmp);

		}
		// 整合，使得所有的随机数之和等于sum
		int index = (int) (Math.random() * dataBase.size());
		List<Integer> dbtmp = dataBase.get(index);
		dataBase.remove(index);

		List<Integer> tmp = new ArrayList<Integer>();
		tmp.add(least);
		ArrayCopy(tmp, dbtmp);
		myData.add(tmp);

		// System.out.println(tmp[0]);
		// 按格式打印数据

		return myData;
		// if (!satisfic(myData)) {
		// getMyData();
		// } else {
		// printMyData(myData);
		// }

	}

	private void ArrayCopy(List<Integer> lr, List<Integer> ls) {// 列表复制
		for (int i : ls) {
			lr.add(i);
		}
	}

	public void printMyData(List<List<Integer>> data) {// 打印最终数据
		System.out.println("\"" + sum + " " + numofop + " " + numofclass + "\\n\" + ");
		System.out.print("\"");
		for (int n = 0; n < CapacityN.size(); n++) {
			System.out.print(CapacityN.get(n));
			if (n != CapacityN.size() - 1) {
				System.out.print(" ");
			}
		}
		System.out.println("\\n\" + ");
		System.out.print("\"");
		for (int d = 0; d < CapacityD.size(); d++) {
			System.out.print(CapacityD.get(d));
			if (d != CapacityD.size() - 1) {
				System.out.print(" ");
			}
		}
		System.out.println("\\n\" + ");
		Collections.shuffle(data);
		for (int i = 0; i < data.size(); i++) {
			System.out.print("\"" + i + " ");
			for (int j = 0; j < data.get(i).size(); j++) {
				if (j == data.get(i).size() - 1) {
					System.out.print(data.get(i).get(j) + "\\n\"");
				} else {
					System.out.print(data.get(i).get(j) + " ");
				}
			}
			if (i != data.size() - 1) {
				System.out.println(" + ");
			}

		}

	}

	private List<Integer> extractor(List<List<Integer>> mac, int num) {// 提取矩阵中第一列数据

		List<Integer> tmp = new ArrayList<Integer>();
		for (int m = 0; m < mac.size(); m++) {

			int sum = mac.get(m).get(num) * mac.get(m).get(0);
			tmp.add(sum);
		}
		return tmp;
	}

	public boolean satisfic(List<List<Integer>> mac) {// 判断容量约束
		CapacityN = new ArrayList<Integer>();
		CapacityD = new ArrayList<Integer>();
		int ci = 2;
		int ct = 1;
		for (int i = 1; i <= numofop; i++) {
			int para = 0;
			CapacityD.add(ci);

			if (i % 2 == 0) {
				para = sum * 2 / ci;

				CapacityN.add(2);
				ct = ci - ct;
			} else {
				para = sum / ci;

				CapacityN.add(1);
				ci += ct;
			}
			if (arraysum(extractor(mac, i)) > para) {
				return false;
			}
		}
		return true;

	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// int[] test = {1,2,3,4};
		// int r = new MyRandomData().sum(test);
		// System.out.println(r);
		MyRandomDataUpdate mrd = new MyRandomDataUpdate(10000, 5, 19);
		List<List<Integer>> mdb = mrd.getMyData();
		while (!mrd.satisfic(mdb)) {
			mdb = mrd.getMyData();
		}
		mrd.printMyData(mdb);

	}

}
