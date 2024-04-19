package benchmarking;

import benchmarking.simulatorconfiguration.SegmentalConfig;
import benchmarking.simulatorconfiguration.SimulatorConfig;
import com.google.gson.reflect.TypeToken;
import core.analysis.TransientAnalysis;
import core.analysis.TransientAnalysisResult;
import core.util.JSON;
import core.util.Progressable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import core.simulation.simulators.Simulator;
import core.util.Pair;

/**
 *
 * @author Martin
 */
public class NewTransientAnalysisBenchmark extends SettingBenchmark {

    public SimulatorConfig simconf;
    public int sims;
    public int repeat;

    public NewTransientAnalysisBenchmark(long timout, String name, String example, double end_t, SimulatorConfig simconf, int sims, int[] initial_state, int repeat) {
        super(Type.TRANSIENTNEW, timout, name, example, end_t);
        this.simconf = simconf;
        this.sims = sims;
        if (initial_state != null) {
            this.initial_state = new int[initial_state.length];
            System.arraycopy(initial_state, 0, this.initial_state, 0, initial_state.length);
        }
        this.repeat = repeat;
    }

    @Override
    public Object doBenchmark(Progressable p) {
        if (!prepare(p)) {
            return "Error: no setting found";
        }

        return doRepeatedTransientAnalysis(p, getRandom());
    }
    
    public TransientAnalysisResult doTransientAnalysis(Progressable p, Random rand) {
        TransientAnalysisResult res = new TransientAnalysisResult();
        Simulator simulator = simconf.createSimulator(setting);
        TransientAnalysis.analyize(res, setting, simulator, sims, p, rand);
        return res;
    }
    
    public TransientAnalysisResult[] doRepeatedTransientAnalysis(Progressable p, Random rand) {
        ArrayList<TransientAnalysisResult> res = new ArrayList<>();
        for (int repeat_i = 0; repeat_i < repeat; repeat_i++) {
            if (p.isStopped()) {
                break;
            }
            p.updateMessage("repeat " + (repeat_i + 1) + "/" + repeat);
            res.add(doTransientAnalysis(p.start_subroutine(1.0 / repeat), new Random(getRandom().nextLong())));
            p.end_subroutine();
        }
        return res.toArray(TransientAnalysisResult[]::new);
    }

    @Override
    public TransientAnalysisResult[] loadResult(String json) {
        if (json == null || json.equals("")) return null;
        return JSON.getGson().fromJson(json, new TypeToken<TransientAnalysisResult[]>(){}.getType());
    }
    
    public static TransientAnalysisResult[] filterFinishedResults(TransientAnalysisResult[] results) {
        int max_sims = -1;
        ArrayList<TransientAnalysisResult> filtered = new ArrayList<>();
        for (TransientAnalysisResult res : results) {
            if (res.numberOfResults() > max_sims) {
                max_sims = res.numberOfResults();
                filtered = new ArrayList<>();
            }
            if (res.numberOfResults() == max_sims) {
                filtered.add(res);
            }
        }
        return filtered.toArray(TransientAnalysisResult[]::new);
    }
    
    public static ArrayList<Pair<String,Number>> getAverageStats(TransientAnalysisResult[] results, boolean onlyForEqualNumberOfSims) {
        if (onlyForEqualNumberOfSims) results = filterFinishedResults(results);
        HashMap<String,Integer> summed = new HashMap<>();
        HashMap<String,Double> sums = new HashMap<>();
        ArrayList<String> categories = new ArrayList<>();
        for (TransientAnalysisResult res : results) {
            for (Pair<String, Number> pair : res.stats) {
                if (!categories.contains(pair.left)) categories.add(pair.left);
                summed.put(pair.left, 1 + summed.getOrDefault(pair.left, 0));
                sums.put(pair.left, pair.right.doubleValue() + sums.getOrDefault(pair.left, 0.0));
            }
        }
        ArrayList<Pair<String,Number>> res = new ArrayList<>(categories.size());
        for (String stat : categories) {
            res.add(new Pair(stat, sums.get(stat) / summed.get(stat)));
        }
        return res;
    }
    
    
    public static void main(String[] args) {
        Benchmark b = new NewTransientAnalysisBenchmark(1000 * 60, "Test", "predator_prey_canonical.json", 200, new SegmentalConfig(), 10000, null, 10);
        
        System.out.println(JSON.getGson().toJson(b));
        
        Benchmarking.benchmark(new Benchmark[]{b});
    }
}
