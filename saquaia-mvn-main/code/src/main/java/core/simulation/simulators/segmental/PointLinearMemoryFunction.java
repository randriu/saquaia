package core.simulation.simulators.segmental;

import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Martin
 */
public class PointLinearMemoryFunction extends MemoryFunction{
    public TreeMap<Long,Integer> points;
    public double limit_a;
    
    public PointLinearMemoryFunction(TreeMap<Long,Integer> points, double limit_a) {
        super(Type.POINTLINEAR);
        this.points = new TreeMap<>(points);
        this.limit_a = limit_a;
    }

    @Override
    public double f(long uses) {
        Map.Entry<Long, Integer> floorEntry = points.floorEntry(uses);
        Map.Entry<Long, Integer> ceilingEntry = points.ceilingEntry(uses);
        if (floorEntry == null && ceilingEntry == null) return limit_a * uses;
        if (floorEntry == null) return ceilingEntry.getValue();
        if (ceilingEntry == null) return floorEntry.getValue() + limit_a * (uses - floorEntry.getKey());
        if (floorEntry.getKey() == uses) return floorEntry.getValue();
        if (ceilingEntry.getKey() == uses) return ceilingEntry.getValue();
        return floorEntry.getValue() + 1.0 * (ceilingEntry.getValue() - floorEntry.getValue()) / (ceilingEntry.getKey() - floorEntry.getKey()) * (uses - floorEntry.getKey());
    }
    
    public static void main(String[] args) {
        TreeMap<Long, Integer> treeMap = new TreeMap<Long, Integer>();
        treeMap.put(0l, 0);
        treeMap.put(100l, 100);
        treeMap.put(9100l, 1000);
        PointLinearMemoryFunction f = new PointLinearMemoryFunction(treeMap, 0.001);
        for (int i = 0; i < 10; i++) {
            System.out.println(Math.pow(10, i) + ": " + f.target_nr_of_saved_segmentlikes((long) Math.pow(10, i)));
        }
    }

    @Override
    public String abreviation() {
        String res = "pl";
        for (Map.Entry<Long, Integer> entry : points.entrySet()) {
            res+="_"+entry.getKey()+"_"+entry.getValue();
        }
        return res;
    }
    
}
