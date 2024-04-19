package core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.random.RandomGenerator;

/**
 *
 * @author Martin Helfrich
 */
public class Stochastics {
    public static boolean coin(Random random) {
        return coin(random, 0.5);
    }
    public static boolean coin(Random random, double p) {
        return random.nextDouble() < p;
    }
    
    public static <T> T choose(Random random, T[] options) {
        if (options == null || options.length == 0) return null;
        return options[random.nextInt(options.length)];
    }   
    
    public static <T> T choose(Random random, ArrayList<T> options) {
        if (options == null || options.isEmpty()) return null;
        return options.get(random.nextInt(options.size()));
    } 
    
    public static <T> T choose(Random random, Collection<T> options, DoubleWeightSupplier<T> w) {
        double sum = 0;
        for (T t : options) {
            double weight = w.weightFor(t);
            if (weight < 0) throw new IllegalArgumentException("Weights must not be negative!");
            sum += weight;
        }
        double sum_up_to_chosen = 0;
        double random_value = random.nextDouble();
        for (T t : options) {
            double prob_for_chosen = w.weightFor(t) / sum;
            if (sum_up_to_chosen <= random_value && random_value < sum_up_to_chosen + prob_for_chosen) return t;
            sum_up_to_chosen += prob_for_chosen;
        }
        return null;
    }
    
    public static <T> T choose(Random random, Collection<T> options, DoubleWeightSupplier<T> w, double sum) {
        if (sum <= 0) throw new IllegalArgumentException("Sum must be positiv!");
        double sum_up_to_chosen = 0;
        double random_value = random.nextDouble();
        for (T t : options) {
            double weight = w.weightFor(t);
            if (weight < 0) throw new IllegalArgumentException("Weights must not be negative!");
            double prob_for_chosen = w.weightFor(t) / sum;
            if (sum_up_to_chosen <= random_value && random_value < sum_up_to_chosen + prob_for_chosen) return t;
            sum_up_to_chosen += prob_for_chosen;
        }
        return null;
    }
    
    public interface DoubleWeightSupplier<T>{
        public double weightFor(T t);
    }
    
    public static <T> T choose(Random random, Collection<T> options, IntWeightSupplier<T> w) {
        int sum = 0;
        for (T t : options) {
            double weight = w.weightFor(t);
            if (weight < 0) throw new IllegalArgumentException("Weights must not be negative!");
            sum += weight;
        }
        int sum_up_to_chosen = 0;
        int random_value = random.nextInt(sum);
        for (T t : options) {
            double weight = w.weightFor(t);
            if (sum_up_to_chosen <= random_value && random_value < sum_up_to_chosen + weight) return t;
            sum_up_to_chosen += weight;
        }
        return null;
    }
    
    public static <T> T choose(Random random, Collection<T> options, IntWeightSupplier<T> w, int sum) {
        if (sum <= 0) throw new IllegalArgumentException("Sum must be positiv!");
        int sum_up_to_chosen = 0;
        int random_value = random.nextInt(sum);
        for (T t : options) {
            double weight = w.weightFor(t);
            if (weight < 0) throw new IllegalArgumentException("Weights must not be negative!");
            if (sum_up_to_chosen <= random_value && random_value < sum_up_to_chosen + weight) return t;
            sum_up_to_chosen += weight;
        }
        return null;
    }
    
    public interface IntWeightSupplier<T>{
        public int weightFor(T t);
    }
    
    public static int choose(Random random, int[] weights, int sum) {
        if (sum <= 0) throw new IllegalArgumentException();
        int chosen = 0;
        int sum_up_to_chosen = 0;
        int random_value = random.nextInt(sum);
        for (; chosen < weights.length; chosen++) {
            if (sum_up_to_chosen <= random_value && random_value < sum_up_to_chosen + weights[chosen]) {
                break;
            } else {
                sum_up_to_chosen += weights[chosen];
            }
        }
        return chosen;
    }
    
    public static int choose(Random random, double[] weights, double sum) {
        if (sum <= 0) throw new IllegalArgumentException();
        int chosen = 0;
        double sum_up_to_chosen = 0;
        double random_value = random.nextDouble();
        for (; chosen < weights.length; chosen++) {
            double prob_for_chosen = weights[chosen] / sum;
            if (sum_up_to_chosen <= random_value && random_value < sum_up_to_chosen + prob_for_chosen) {
                break;
            } else {
                sum_up_to_chosen += prob_for_chosen;
            }
        }
        return chosen;
    }
    
    
    public static Pair<Integer,Double> chooseAndSum(Random random, double[] weights) {
        double sum = 0;
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] < 0) throw new IllegalArgumentException("weights must be non-negative");
            sum += weights[i];
        }
        if (sum == 0) return null;
        return new Pair(choose(random, weights, sum), sum);
    }
    
    public static Integer choose(Random random, double[] weights) {
        double sum = 0;
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] < 0) throw new IllegalArgumentException("weights must be non-negative");
            sum += weights[i];
        }
        if (sum == 0) return null;
        return choose(random, weights, sum);
    }
    
    public static double sampleExponential(Random random, double rate) {
        if (rate <= 0) throw new IllegalArgumentException("rate must be positive");
        return Math.log(1 - random.nextDouble()) / (-rate);
    }
    
    public static int samplePoissonDistribution(RandomGenerator rg, double rate) {
        return new PoissonDistribution(rg, rate, PoissonDistribution.DEFAULT_EPSILON, PoissonDistribution.DEFAULT_MAX_ITERATIONS).sample();
    }
}
