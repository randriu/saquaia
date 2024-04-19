package core.util;

import java.util.Arrays;

/**
 *
 * @author Martin
 */
public class DoubleArrayWrapper {
    public double[] array;
    
    public DoubleArrayWrapper(double[] array) {
        this.array = array;
    }
    
    @Override
    public String toString() {
        return Arrays.toString(array);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DoubleArrayWrapper other = (DoubleArrayWrapper) o;
        return Arrays.equals(array, other.array);
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(array);
    }
}
