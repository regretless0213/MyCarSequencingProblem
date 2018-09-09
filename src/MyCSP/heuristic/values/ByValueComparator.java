package MyCSP.heuristic.values;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

public class ByValueComparator implements Comparator<Entry<Integer,Double>> {
    Map<Integer, Double> hashmap;
    public ByValueComparator(Map<Integer, Double> hm) {
        this.hashmap = hm;
    }
    @Override
    public int compare(Entry<Integer, Double> o1, Entry<Integer, Double> o2) {
        // TODO Auto-generated method stub
        if (o1.getValue().compareTo(o2.getValue()) == -1) {
            return 1;
        }else {
            return -1;
        }
    }
}
