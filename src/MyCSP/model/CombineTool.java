package MyCSP.model;

import java.util.ArrayList;
import java.util.List;

public class CombineTool {

	
	public int[][] genResult(int n,int m){
		List<Integer> data = new ArrayList<Integer>();
		for (int i = 0; i < m; i++) {
			data.add(i);
		}
		for (int i = 1; i <=n; i++)
			combinerSelect(data, new ArrayList<Integer>(), data.size(), i);
		
		int[][] re=new int[0][0];
		re=result.toArray(re);
		return re;
	}

	/**
	 * ���裺��ÿ�εݹ�ʱ����ԭʼ���ݺ����������Ĺ����ռ临��һ�ݣ����еĲ������ڸ����ļ��н��У�Ŀ�ľ��Ǳ�֤���ƻ�ԭʼ���ݣ�
	 * �Ӷ�������һ�ֵݹ�������������������һ�֡�
	 * ��Σ������ݵĵ�һ��Ԫ����ӵ������ռ��У��жϹ����ռ�Ĵ�С�����С��k,����Ҫ�����ݹ飬����ʱ������ݹ麯����
	 * ������Ҫע�⣺���赱ǰ����Ľڵ���±���i,��Ϊ��˳������,����i֮ǰ���������ݶ�Ӧ����ȥ��ֻ����i֮���δʹ�ù������ݡ�
	 * ����ڴ���֮ǰ��Ӧ�ö�copydata���Դ���������k��ʱ��������Ѿ��ҵ����������ĵ�һ�������Ȼ��ֻ���޸ĸ���������һ��������ɡ�
	 * �磺�ҵ�abcʱ����ֻ���滻cΪd������ɸ��ֵݹ顣
	 * 
	 * @param data
	 *            ԭʼ����
	 * @param workSpace
	 *            �Զ���һ����ʱ�ռ䣬�����洢ÿ�η���������ֵ
	 * @param k
	 *            C(n,k)�е�k
	 */
	public <E> void combinerSelect(List<E> data, List<E> workSpace, int n, int k) {
		List<E> copyData;
		List<E> copyWorkSpace;

		if (workSpace.size() == k) {
			int[] tp=new int[k];
			for(int i=0;i<k;i++){
				tp[i]=(int)workSpace.get(i);
			}
			result.add(tp);
		}

		for (int i = 0; i < data.size(); i++) {
			copyData = new ArrayList<E>(data);
			copyWorkSpace = new ArrayList<E>(workSpace);

			copyWorkSpace.add(copyData.get(i));
			for (int j = i; j >= 0; j--)
				copyData.remove(j);
			combinerSelect(copyData, copyWorkSpace, n, k);
		}

	}
	
	ArrayList<int[]> result=new ArrayList<int[]>();

}