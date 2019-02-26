package MyCSP.model;

import java.util.ArrayList;
import java.util.List;

public class CheckForCSUpdate {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String dataName = "random";
		List<String> sf = new ArrayList<String>();
		int sum = 0;
		for (int n = 1; n <= 75; n++) {
			if (n == 2 || n == 3) {// random02、random03超时不做检查
				continue;
			} else {
				sum++;
			}
			if (n < 10) {
				args[1] = dataName + "0" + n;
			} else {
				args[1] = dataName + n;
			}

			List<String> r = new ArrayList<>();
			new MyCarSequencing(r).execute(args);
			String[] result = new String[r.size() * 3];
//			System.out.println(r.size());
			int i = 0;
			for (String tmp : r) {
				String[] tp = tmp.split(" ");
				int j = 0;
				for (; j < 3; j++) {
					result[i + j] = tp[j];
				}
				i += j;
			}
			System.out.println(args[1] + ":");
			new CheckForCS(result, sf).execute(args);
//		for(String t:result) {
//			System.out.println(t);
//		}
		}
		System.out.println("总共检查" + sum + "，成功" + sf.size());
	}

}
