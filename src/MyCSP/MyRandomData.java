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

	public int arraysum(int[] array) {// �������������ܺ�
		int sum = 0;
		for (int t : array) {
			sum += t;
		}

		return sum;
	}

	public void printMyData(int sum, int numofclass) {
		dataBase = new ArrayList<int[]>();
		ArrayList<int[]> myData = new ArrayList<int[]>();// �����������յĽ������
		
		dataBase = getDataBase();
		int least = sum - numofclass + 1;
		int num = 0;
		for (int n = 0; n < numofclass - 1; n++) {
			// �����ȡ�������е�һ��
			int index = (int) (Math.random() * dataBase.size());
			int[] dbtmp = dataBase.get(index);
			dataBase.remove(index);
			// �������ƣ���ֹ��������������󣬵��������ѽ�
			int lr = least;
			int dbs = arraysum(dbtmp);
			int ftmp = (5 - dbs) * sum / numofclass;
			if (lr > ftmp / dbs) {
				lr = ftmp / dbs;
			}
			num = (int) (Math.random() * lr + 1);

			int[] tmp = new int[6];
			tmp[0] = num;
			System.arraycopy(dbtmp, 0, tmp, 1, 5); // �������ȡ���������ݱ��浽�������

			least = least - num + 1;
			// System.out.println(least+" "+num);
			myData.add(tmp);

		}
		// ���ϣ�ʹ�����е������֮�͵���sum
		int index = (int) (Math.random() * dataBase.size());
		int[] dbtmp = dataBase.get(index);
		dataBase.remove(index);
		// �����ʣ����������������ƣ����������һ��
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
			// ����ʽ��ӡ����
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

	private int[] extractor(ArrayList<int[]> mac, int num) {// ��ȡ�����е�һ������

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
				System.out.println("����para����");
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
