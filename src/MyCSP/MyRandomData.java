package MyCSP;

import java.util.ArrayList;
import java.util.Collections;

public class MyRandomData {
	ArrayList<int[]> dataBase;

	public ArrayList<int[]> getDataBase() {
		ArrayList<int[]> classData = new ArrayList<int[]>();
		for (int a = 0; a < 2; a++) {
			for (int b = 0; b < 2; b++) {
				for (int c = 0; c < 2; c++) {
					for (int d = 0; d < 2; d++) {
						for (int e = 0; e < 2; e++) {
							int[] tmp = { a, b, c, d, e };
							classData.add(tmp);
						}
					}
				}
			}

		}
		// System.out.println(sum(classData.get(0)));
		classData.remove(0);
		return classData;
	}

	public int arraysum(int[] array) {// 计算整形数组总和
		int sum = 0;
		for (int t : array) {
			sum += t;
		}

		return sum;
	}

	public void printMyData(int sum, int numofclass) {
		dataBase = new ArrayList<int[]>();
		ArrayList<int[]> myData = new ArrayList<int[]>();// 用来保存最终的结果数据
		
		dataBase = getDataBase();
		int least = sum - numofclass + 1;
		int num = 0;
		for (int n = 0; n < numofclass - 1; n++) {
			// 随机抽取基础表中的一列
			int index = (int) (Math.random() * dataBase.size());
			int[] dbtmp = dataBase.get(index);
			dataBase.remove(index);
			// 给出限制，防止产生的随机数过大，导致问题难解
			int lr = least;
			int dbs = arraysum(dbtmp);
			int ftmp = (5 - dbs) * sum / numofclass;
			if (lr > ftmp / dbs) {
				lr = ftmp / dbs;
			}
			num = (int) (Math.random() * lr + 1);

			int[] tmp = new int[6];
			tmp[0] = num;
			System.arraycopy(dbtmp, 0, tmp, 1, 5); // 将随机抽取出来的数据保存到结果表中

			least = least - num + 1;
			// System.out.println(least+" "+num);
			myData.add(tmp);

		}
		// 整合，使得所有的随机数之和等于sum
		int index = (int) (Math.random() * dataBase.size());
		int[] dbtmp = dataBase.get(index);
		dataBase.remove(index);
		// 若最后剩余的数量不符合限制，则重新随机一遍
		int dbs = arraysum(dbtmp);
		int ftmp = (6 - dbs) * sum / numofclass;
		if (least > ftmp / dbs) {
			printMyData(sum, numofclass);
		} else {
			int[] tmp = new int[6];
			tmp[0] = least;
			System.arraycopy(dbtmp, 0, tmp, 1, 5);
			myData.add(tmp);

			// System.out.println(tmp[0]);
			// 按格式打印数据
			if (!satisfic(myData, sum)) {
				printMyData(sum, numofclass);
			} else {
				System.out.println("\"" + sum + " 5 " + numofclass + "\\n\" + ");
				System.out.println("\"" + "1 2 1 2 1" + "\\n\" + ");
				System.out.println("\"" + "2 3 3 5 5" + "\\n\" + ");
				Collections.shuffle(myData);
				for (int i = 0; i < myData.size(); i++) {
					System.out.print("\"" + i + " ");
					for (int j = 0; j < myData.get(i).length; j++) {
						if (j == myData.get(i).length - 1) {
							System.out.print(myData.get(i)[j] + "\\n\"");
						} else {
							System.out.print(myData.get(i)[j] + " ");
						}
					}
					if (i != myData.size() - 1) {
						System.out.println(" + ");
					}
				
				}
			}
		}
	}

	private int[] extractor(ArrayList<int[]> mac, int num) {// 提取矩阵中第一列数据

		int[] tmp = new int[mac.size()];
		for (int m = 0; m < mac.size(); m++) {
			tmp[m] = mac.get(m)[num] * mac.get(m)[0];
		}
		return tmp;
	}

	private boolean satisfic(ArrayList<int[]> mac, int sum) {
		for (int i = 1; i < 6; i++) {
			int para = 0;
			switch (i) {
			case 1:
				para = sum / 2;
				break;
			case 2:
				para = sum * 2 / 3;
				break;
			case 3:
				para = sum / 3;
				break;
			case 4:
				para = sum * 2 / 5;
				break;
			case 5:
				para = sum / 5;
				break;
			default:
				System.out.println("参数para出错！");
			}
			if (arraysum(extractor(mac, i)) > para) {
				return false;
			}
		}
		return true;

	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new MyRandomData().printMyData(5000, 25);
		// int[] test = {1,2,3,4};
		// int r = new MyRandomData().sum(test);
		// System.out.println(r);
	}

}
