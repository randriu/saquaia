package core.model;

import com.google.gson.Gson;
import core.util.JSON;

/**
 *
 * @author Martin Helfrich
 */
public class Interval {

    public final int min; // inclusive
    public final int rep; 
    public final int max; // inclusive

    public Interval(int min, int max) {
        this(min, max, false);
    }

    public Interval(int min, int max, boolean roundUp) {
        this(min, (int) ((long) min + max + (roundUp ? 1 : 0)) / 2, max);
    }

    public Interval(int min, int rep, int max) {
        if (min > max) {
            throw new IllegalArgumentException("The minimum must be at most the maximum.");
        }
        this.min = min;
        this.max = max;
        if (!contains(rep)) {
            throw new IllegalArgumentException("The representative must be within the interval.");
        }
        this.rep = rep;
    }

    @Override
    public String toString() {
        if (min == max) {
            return "[" + min + "]";
        }
        return "[" + min + "," + rep + "," + max + "]";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Interval i = (Interval) o;
        return i.min == min && i.max == max && i.rep == rep;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + this.min;
        hash = 89 * hash + this.rep;
        hash = 89 * hash + this.max;
        return hash;
    }

    public final boolean contains(int i) {
        return min <= i && i <= max;
    }

    public final boolean contains(double d) {
        return min <= d && d < max+1;
    }

    public int compareTo(int i) {
        return (i < min) ? -1 : ((i > max) ? 1 : 0);
    }

    public int compareTo(double d) {
        return (d < min) ? -1 : ((d >= max + 1) ? 1 : 0);
    }

    public int direction(int delta) {
        if (delta == 0) return 0;
        double target = rep + delta;
        int compared = compareTo(target);
        if (compared != 0) return 2*compared;
        return Integer.compare(delta, rep);
    }

    public int direction(double delta) {
        if (delta == 0) return 0;
        double target = rep + delta;
        int compared = compareTo(target);
        if (compared != 0) return 2*compared;
        return Double.compare(delta, rep);
    }

    public double distanceOf(double d) {
        return distanceOf(d, 0);
    }

    public double distanceOf(double d, double negative_leeway) {
        return distanceOf(min, max, d, negative_leeway);
    }
    
    public static double distanceOf(int min, int max, double d, double negative_leeway) {
        return Math.min(max + 1 - d, d - min + negative_leeway); 
    }

    public int size() {
        return max - min + 1;
    }
    
    public Interval copy() {
        Gson gson = JSON.getGson();
        return gson.fromJson(gson.toJson(this), Interval.class);
    }
}
