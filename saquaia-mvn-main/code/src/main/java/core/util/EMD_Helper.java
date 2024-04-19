/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

/**
 *
 * @author Martin
 */
public class EMD_Helper {
    public static double summed(double[] EMDs) {
        double res = 0;
        for (double EMD : EMDs) res+=EMD;
        return res;
    }
    
    public static double summed_normalized(double[] EMDs) {
        return summed(EMDs) / EMDs.length;
    }
    
    public static double multiplied(double[] EMDs) {
        double res = 1;
        for (double EMD : EMDs) res*=1+EMD;
        return res;
    }
    
    public static double multiplied_normalized(double[] EMDs) {
        return Math.pow(multiplied(EMDs), 1.0 / EMDs.length) - 1;
    }
    
    public static <S extends Number, V extends Number> double EMD(Map<S,V> d1, Map<S,V> d2) {
        double d1_sum = 0;
        double d2_sum = 0;
        HashSet<S> keys = new HashSet<>();
        for (S value : d1.keySet()) {
            keys.add(value);
            d1_sum+= d1.get(value).doubleValue();
        }
        for (S value : d2.keySet()) {
            keys.add(value);
            d2_sum+= d2.get(value).doubleValue();
        }
        ArrayList<S> key_list = new ArrayList<>(keys);
        key_list.sort((S o1, S o2) -> Double.compare(o1.doubleValue(), o2.doubleValue()));

        double last = 0;
        double to_move = 0;
        double dist = 0;
        for (Number d : key_list) {
            double d_value = d.doubleValue();
            dist += Math.abs((d_value - last) * to_move);
            double d1_value = 0;
            if (d1.containsKey(d)) d1_value = d1.get(d).doubleValue();
            double d2_value = 0;
            if (d2.containsKey(d)) d2_value = d2.get(d).doubleValue();
            to_move += d1_value / d1_sum - d2_value / d2_sum;
            last = d_value;
        }
        return dist;
    }
}
