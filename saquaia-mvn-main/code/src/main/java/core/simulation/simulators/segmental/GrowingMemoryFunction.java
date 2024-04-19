package core.simulation.simulators.segmental;

import core.util.IO;
import core.util.Stochastics;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

/**
 * For an interactive visualization see https://www.geogebra.org/classic/bpgzbgbu
 * @author Martin
 */
public class GrowingMemoryFunction extends MemoryFunction{
    public double s = 0.0045;
    
    public GrowingMemoryFunction(double s) {
        super(Type.GROWING);
        this.s = s;
    }

    @Override
    public double f(long uses) {
        return Math.log(s * uses + 1) / s;
    }
    
    public static GrowingMemoryFunction precise(){
        return new GrowingMemoryFunction(0.0045);
    }
    
    public static GrowingMemoryFunction normal(){
        return new GrowingMemoryFunction(0.0359);
    }
    
    public static GrowingMemoryFunction imprecise(){
        return new GrowingMemoryFunction(0.3565);
    }
    
    public static void main(String[] args) throws IOException {
        Random rand = new Random(42);
        double[] weights = new double[]{0.35, 0.3, 0.2, 0.1, 0.05, 0};
        long[] counts = new long[weights.length];
        double[] vs = new double[weights.length];
        double[] vs2 = new double[weights.length];
        int[] last = new int[weights.length];
        long total_counts = 0;
//        double alpha = 0.7;
//        double alpha = 0.9931;
//        double alpha = 0.99;
        double alpha = 1;
        for (int i = 0; i < 1000; i++) {
            int chosen = weights.length-1;
            if (i > 100) chosen = Stochastics.choose(rand, weights);
            System.out.print(chosen);
            counts[chosen]++;
            
            for (int j = 0; j < vs.length; j++) vs[j]*= alpha;
            vs[chosen]++;
            
            vs2[chosen] *= Math.pow(alpha, i - last[chosen]);
            vs2[chosen]++;
            last[chosen] = i;
            
            total_counts++;
            if (i % 10 == 0) {
                double sum = 0;
                for (int j = 0; j < vs.length; j++) sum += vs[j];
                double sum2 = 0;
                for (int j = 0; j < vs.length; j++) sum2 += vs2[j] * Math.pow(alpha, i - last[j]);
                String[] printing = new String[vs.length];
                String[] printing1 = new String[vs.length];
                String[] printing2 = new String[vs.length];
                String[] printing3 = new String[vs.length];
                String[] printing4 = new String[vs.length];
                for (int j = 0; j < vs.length; j++) printing[j] = IO.significantFigures(1.0 * counts[j] / total_counts, 2);
                for (int j = 0; j < vs.length; j++) printing1[j] = IO.significantFigures(vs[j], 2);
                for (int j = 0; j < vs.length; j++) printing2[j] = IO.significantFigures(vs[j] / sum, 2);
                for (int j = 0; j < vs.length; j++) printing3[j] = IO.significantFigures(vs2[j] * Math.pow(alpha, i - last[j]), 2);
                for (int j = 0; j < vs.length; j++) printing4[j] = IO.significantFigures(vs2[j] * Math.pow(alpha, i - last[j]) / sum2, 2);
                System.out.println("");
                System.out.println("i: " + i);
                System.out.println(Arrays.toString(weights));
                System.out.println(Arrays.toString(printing));
//                System.out.println(Arrays.toString(printing1));
//                System.out.println(Arrays.toString(printing2));
                System.out.println(Arrays.toString(printing4));
//                System.out.println(Arrays.toString(printing3));
                System.out.println(sum2);
                System.out.println(alpha >= 1.0 ? i + 1 : (Math.pow(alpha, i+1) - 1) / (alpha - 1));
                System.out.println("");
                
            }
        }
        
        System.in.read();
        
        double x = 0.99999999;
        long k = 10000000000l;
        for (long k_i = 1; k_i <= k; k_i*= 2) {
            double d = 1.0;
            System.out.println(k_i);
            long start = System.nanoTime();
            for (long i = 0; i < k_i; i++) d *= x;
            long diff = System.nanoTime() - start;
            System.out.println(d);
            System.out.println(IO.humanReadableDuration(diff));
            long start2 = System.nanoTime();
            d = Math.pow(x, k_i);
            long diff2 = System.nanoTime() - start2;
            System.out.println(d);
            System.out.println(IO.humanReadableDuration(diff2));
            if (diff2 > diff) System.out.println("ERROR!!!!");
            System.out.println("");
        }
        
        
//        GrowingMemoryFunction f = imprecise();
//        for (int i = 0; i < 10000; i++) {
//            double v = Math.ceil(f.target_nr_of_saved_segmentlikes(i));
//            double v_prev = Math.ceil(f.target_nr_of_saved_segmentlikes(i-1));
//            System.out.println(i + ": " + v + (v == v_prev ? "!" : ""));
//        }
//        System.in.read();
    }

    @Override
    public String abreviation() {
        return "g"+s;
    }
    
}
