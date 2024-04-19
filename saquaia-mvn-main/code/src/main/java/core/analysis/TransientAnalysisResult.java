package core.analysis;

import core.model.Distribution;
import core.simulation.Simulation;
import core.util.DoubleArrayWrapper;
import core.util.Pair;
import java.util.ArrayList;

/**
 *
 * @author helfrich
 */
public class TransientAnalysisResult {

    public int results = 0;
    public ArrayList<double[]> states = new ArrayList<>();
    public ArrayList<Double> times = new ArrayList<>();
    public ArrayList<Long> comp_time_sum = new ArrayList<>();
    public ArrayList<Pair<String, Number>> stats = new ArrayList<>();
    public String name;

    // distribution for reuse
    private transient Distribution<DoubleArrayWrapper> dist;
    private transient Integer results_in_dist;

    public long getTotalCompTime() {
        if (comp_time_sum.isEmpty()) {
            return 0;
        }
        return comp_time_sum.get(comp_time_sum.size() - 1);
    }

    public void addSimulation(Simulation sim, long comp_time) {
        states.add(sim.getState());
        times.add(sim.getTime());
        comp_time_sum.add(getTotalCompTime() + comp_time);
        results++;
    }

    public int numberOfResults() {
        return results;
    }

    public long comp_time(int sim_i) {
        long comp_time_sum_before = sim_i == 0 ? 0 : comp_time_sum.get(sim_i - 1);
        return comp_time_sum.get(sim_i) - comp_time_sum_before;
    }

    public double avg_comp_time(int sim_i) {
        if (sim_i >= numberOfResults()) {
            throw new IllegalArgumentException();
        }
        return 1.0 * comp_time_sum.get(sim_i) / (sim_i + 1);
    }

    public double avg_comp_time() {
        if (numberOfResults() == 0) {
            return 0;
        }
        return 1.0 * getTotalCompTime() / numberOfResults();
    }

    public Distribution<DoubleArrayWrapper> getDistribution() {
        if (dist == null || numberOfResults() != results_in_dist) {
            dist = new Distribution<>();
            for (double[] state : states) {
                dist.add(new DoubleArrayWrapper(state), 1);
            }
            dist.normalize();
            results_in_dist = numberOfResults();
        }
        return dist;
    }
}
