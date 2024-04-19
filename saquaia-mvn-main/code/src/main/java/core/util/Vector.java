package core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Martin
 */
public class Vector {
    public static int[] copy(int[] v) {
        if (v == null) return null;
        return Arrays.copyOf(v, v.length);
    }
    
    public static double[] copy(double[] v) {
        if (v == null) return null;
        return Arrays.copyOf(v, v.length);
    }
    
    public static <T> T[] copy(T[] v) {
        if (v == null) return null;
        return Arrays.copyOf(v, v.length);
    }
    
    public static double[] asDoubleArray(int[] v) {
        if (v == null) return null;
        double[] res = new double[v.length];
        for(int i = 0; i < res.length; i++) res[i] = v[i];
        return res;
    }
    
    public static double[] asDoubleArray(Double[] v) {
        if (v == null) return null;
        double[] res = new double[v.length];
        for(int i = 0; i < res.length; i++) res[i] = v[i];
        return res;
    }
    
    public static int[] asIntArray(double[] v) {
        if (v == null) return null;
        int[] res = new int[v.length];
        for(int i = 0; i < res.length; i++) res[i] = (int) Math.round(v[i]);
        return res;
    }
    
    public static int[] asIntArray(Integer[] v) {
        if (v == null) return null;
        int[] res = new int[v.length];
        for(int i = 0; i < res.length; i++) res[i] = (int) Math.round(v[i]);
        return res;
    }
    
    public static boolean isNull(int[] v) {
        for(int i : v) if (i != 0) return false;
        return true;
    }
    
    public static boolean isNull(double[] v) {
        return isNull(v, 0);
    }
    
    public static boolean isNull(double[] v, double epsilon) {
        if (epsilon < 0) epsilon*=-1;
        for(double d : v) if (d < -epsilon || d > epsilon) return false;
        return true;
    }
    
//    public static void add(int[] v, int[] v2) {
//        for (int i = 0; i < v.length; i++) v[i]+= v2[i];
//    }
//    
//    public static void add(double[] v, double[] v2) {
//        for (int i = 0; i < v.length; i++) v[i] += v2[i];
//    }
    
    public static boolean geq(double[] v1, double[] v2) {
        if (v1.length != v2.length) throw new IllegalArgumentException();
        for (int i = 0; i < v1.length; i++) if (v1[i] < v2[i]) return false;
        return true;
    }
    
    public static boolean geq(int[] v1, double[] v2) {
        if (v1.length != v2.length) throw new IllegalArgumentException();
        for (int i = 0; i < v1.length; i++) if (v1[i] < v2[i]) return false;
        return true;
    }
    
    public static boolean geq(double[] v1, int[] v2) {
        if (v1.length != v2.length) throw new IllegalArgumentException();
        for (int i = 0; i < v1.length; i++) if (v1[i] < v2[i]) return false;
        return true;
    }
    
    public static boolean geq(int[] v1, int[] v2) {
        if (v1.length != v2.length) throw new IllegalArgumentException();
        for (int i = 0; i < v1.length; i++) if (v1[i] < v2[i]) return false;
        return true;
    }
    
    public static boolean eq(double[] v1, double[] v2) {
        if (v1.length != v2.length) throw new IllegalArgumentException();
        for (int i = 0; i < v1.length; i++) if (v1[i] != v2[i]) return false;
        return true;
    }
    
    public static boolean eq(int[] v1, double[] v2) {
        if (v1.length != v2.length) throw new IllegalArgumentException();
        for (int i = 0; i < v1.length; i++) if (v1[i] != v2[i]) return false;
        return true;
    }
    
    public static boolean eq(double[] v1, int[] v2) {
        if (v1.length != v2.length) throw new IllegalArgumentException();
        for (int i = 0; i < v1.length; i++) if (v1[i] != v2[i]) return false;
        return true;
    }
    
    public static boolean eq(int[] v1, int[] v2) {
        if (v1.length != v2.length) throw new IllegalArgumentException();
        for (int i = 0; i < v1.length; i++) if (v1[i] != v2[i]) return false;
        return true;
    }
    
    public static double distance(int[] i1, int[] i2) {
        int dist = 0;
        for (int i = 0; i < i1.length; i++) dist += (i1[i] - i2[i]) * (i1[i] - i2[i]);
        return Math.sqrt(dist);
    }
    
    public static double distance(double[] i1, double[] i2) {
        double dist = 0;
        for (int i = 0; i < i1.length; i++) dist += (i1[i] - i2[i]) * (i1[i] - i2[i]);
        return Math.sqrt(dist);
    }
    
    public static <T> List<T> key(T[] items) {
        List<T> mutableList = new ArrayList<>();
        for (T item : items) {
            mutableList.add(item);
        }
        return Collections.unmodifiableList(mutableList);
    }
    
    public static List<Integer> key(int[] items) {
        List<Integer> mutableList = new ArrayList<>();
        for (Integer item : items) {
            mutableList.add(item);
        }
        return Collections.unmodifiableList(mutableList);
    }
    
    public static List<Double> key(double[] items) {
        List<Double> mutableList = new ArrayList<>();
        for (Double item : items) {
            mutableList.add(item);
        }
        return Collections.unmodifiableList(mutableList);
    }
    
    public static void plus(double[] to, double[] x) {
        for (int i = 0; i < to.length; i++) to[i] += x[i];
    }
    
    public static void minus(double[] from, double[] x) {
        for (int i = 0; i < from.length; i++) from[i] -= x[i];
    }
    
    public static void times(double[] base, double scale) {
        for (int i = 0; i < base.length; i++) base[i] *= scale;
    }
    
    public static double[] interpolate(double[] a, double[] b, double frac) {
        frac = Math.max(0, Math.min(1, frac));
        double[] res = copy(b);
        minus(res, a);
        times(res, frac);
        plus(res, a);
        return res;
    }
    
    public static int[] removeDim(int[] vector, int dim_to_del) {
        int[] res = new int[vector.length-1];
        for(int dim_i = 0; dim_i < dim_to_del && dim_i < vector.length; dim_i++) {
            res[dim_i] = vector[dim_i];
        }
        for(int dim_i = dim_to_del+1; dim_i < vector.length; dim_i++) {
            res[dim_i-1] = vector[dim_i];
        }
        return res;
    }
    
    public static double[] removeDim(double[] vector, int dim_to_del) {
        double[] res = new double[vector.length-1];
        for(int dim_i = 0; dim_i < dim_to_del && dim_i < vector.length; dim_i++) {
            res[dim_i] = vector[dim_i];
        }
        for(int dim_i = dim_to_del+1; dim_i < vector.length; dim_i++) {
            res[dim_i-1] = vector[dim_i];
        }
        return res;
    }
    
    public static <T> T[] removeDim(T[] vector, int dim_to_del) {
        T[] res = Arrays.copyOf(vector, vector.length-1);
        for(int dim_i = 0; dim_i < dim_to_del && dim_i < vector.length; dim_i++) {
            res[dim_i] = vector[dim_i];
        }
        for(int dim_i = dim_to_del+1; dim_i < vector.length; dim_i++) {
            res[dim_i-1] = vector[dim_i];
        }
        return res;
    }
    
    public static void switchDims(int[] vector, int dim1, int dim2) {
        int x = vector[dim1];
        vector[dim1] = vector[dim2];
        vector[dim2] = x;
    }
    
    public static void switchDims(double[] vector, int dim1, int dim2) {
        double x = vector[dim1];
        vector[dim1] = vector[dim2];
        vector[dim2] = x;
    }
    
    public static <T> void switchDims(T[] vector, int dim1, int dim2) {
        T x = vector[dim1];
        vector[dim1] = vector[dim2];
        vector[dim2] = x;
    }
    
    public static void moveDim(int[] vector, int from, int to) {
        if (from > to) {
            int x = vector[from];
            for (int i = from; i > to; i--) vector[i] = vector[i-1];
            vector[to] = x;
        } else {
            int x = vector[from];
            for (int i = from; i < to; i++) vector[i] = vector[i+1];
            vector[to] = x;
        }
    }
    
    public static void moveDim(double[] vector, int from, int to) {
        if (from > to) {
            double x = vector[from];
            for (int i = from; i > to; i--) vector[i] = vector[i-1];
            vector[to] = x;
        } else {
            double x = vector[from];
            for (int i = from; i < to; i++) vector[i] = vector[i+1];
            vector[to] = x;
        }
    }
    
    public static <T> void moveDim(T[] vector, int from, int to) {
        if (from > to) {
            T x = vector[from];
            for (int i = from; i > to; i--) vector[i] = vector[i-1];
            vector[to] = x;
        } else {
            T x = vector[from];
            for (int i = from; i < to; i++) vector[i] = vector[i+1];
            vector[to] = x;
        }
    }
    
    public static int[] addDim(int[] vector, int value, int index) {
        vector = Arrays.copyOf(vector, vector.length+1);
        vector[vector.length-1] = value;
        moveDim(vector, vector.length-1, index);
        return vector;
    }
    
    public static double[] addDim(double[] vector, double value, int index) {
        vector = Arrays.copyOf(vector, vector.length+1);
        vector[vector.length-1] = value;
        moveDim(vector, vector.length-1, index);
        return vector;
    }
    
    public static <T> T[] addDim(T[] vector, T value, int index) {
        vector = Arrays.copyOf(vector, vector.length+1);
        vector[vector.length-1] = value;
        moveDim(vector, vector.length-1, index);
        return vector;
    }
    
    public static int[] clear(int[] vector) {
        return Arrays.copyOf(vector, 0);
    }
    
    public static double[] clear(double[] vector) {
        return Arrays.copyOf(vector, 0);
    }
    
    public static <T> T[] clear(T[] vector) {
        return Arrays.copyOf(vector, 0);
    }

    public static String toString(int[] vector, String[] names) {
        ArrayList<String> s = new ArrayList();
        for (int dim_i = 0; dim_i < vector.length; dim_i++) {
            if (vector[dim_i] != 0) s.add(vector[dim_i] + " " + names[dim_i]);
        }
        return String.join(", ", s);
    }

    public static String toString(double[] vector, String[] names) {
        ArrayList<String> s = new ArrayList();
        for (int dim_i = 0; dim_i < vector.length; dim_i++) {
            if (vector[dim_i] != 0) s.add(vector[dim_i] + " " + names[dim_i]);
        }
        return String.join(", ", s);
    }

    public static int[] intVectorFromString(String s, String[] names) {
        int[] res = new int[names.length];
        if (s == null || s.length() == 0) return res;
        String[] splitted = s.split(",");
        for (String s2 : splitted) {
            s2 = s2.strip();
            int best_dim = -1;
            for (int dim_i = 0; dim_i < names.length; dim_i++) {
                if (s2.endsWith(names[dim_i]) && 
                        (best_dim == -1 || names[best_dim].length() < names[dim_i].length())) {
                    best_dim = dim_i;
                }
            }
            if (best_dim == -1) return null;
            String name = names[best_dim];
            String s_amount = s2.substring(0, s2.length() - name.length()).strip();
            int amount = 1;
            if (s_amount.length() > 0) {
                try {
                    amount = Integer.parseInt(s_amount);
                } catch (NumberFormatException e) {return null;}
            }
            res[best_dim] += amount;
        }
        return res;
    }

    public static double[] doubleVectorFromString(String s, String[] names) {
        String[] splitted = s.split(",");
        double[] res = new double[names.length];
        for (String s2 : splitted) {
            s2 = s2.strip();
            int best_dim = -1;
            for (int dim_i = 0; dim_i < names.length; dim_i++) {
                if (s2.endsWith(names[dim_i]) && 
                        (best_dim == -1 || names[best_dim].length() < names[dim_i].length())) {
                    best_dim = dim_i;
                }
            }
            if (best_dim == -1) return null;
            String name = names[best_dim];
            String s_amount = s2.substring(0, s2.length() - name.length()).strip();
            double amount = 1;
            if (s_amount.length() > 0) {
                try {
                    amount = Double.parseDouble(s_amount);
                } catch (NumberFormatException e) {return null;}
            }
            res[best_dim] += amount;
        }
        return res;
    }
    
    public static void main(String[] args) {
        String[] names = new String[]{"A", "B", "C", "D", "E", "F", "G", "H"};
        double[] v1 = new double[]{1,2,3,-5,123123};
        String v1s = toString(v1, names);
        System.out.println(v1s);
        System.out.println(toString(doubleVectorFromString(v1s, names), names));
        System.out.println(Arrays.toString(intVectorFromString(v1s, names)));

        System.out.println(Arrays.toString(doubleVectorFromString("1A, A, 55E-5A, B, -123E55C", names)));
    }
}
