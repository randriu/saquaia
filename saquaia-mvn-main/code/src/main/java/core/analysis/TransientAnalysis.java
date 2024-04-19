package core.analysis;

import core.model.Setting;
import core.simulation.Simulation;
import core.simulation.simulators.Simulator;
import core.util.Pair;
import core.util.Progressable;
import java.util.ArrayList;
import java.util.Random;

public class TransientAnalysis{
    public static long REPORT_EVERY = 10_000_000_000l;  // in ms
    
    public static void analyize(TransientAnalysisResult result, Setting setting, Simulator simulator, int sims, Progressable p, Random rand) {
        if (rand == null) {
            long seed = new Random().nextLong();
            rand = new Random(seed);
            p.updateMessage("using random seed: " + seed);
        }

        long last_info = 0;

        // run simulations
        double fraction_of_goal = sims > 0 ? 1.0 / sims : 1.0;
        for (int sim_i = 0; sim_i < sims; sim_i++) {
            if (p.isStopped()) {
                p.updateMessage("stopping early (only " + sim_i + " simulations)");
                break;
            }

            Simulation sim = setting.createSimulation();
            sim.setKeepHistory(false);
            sim.setSeed(rand.nextLong());

            long start_time_sim = System.nanoTime();
            if (start_time_sim > REPORT_EVERY + last_info) {
                last_info = start_time_sim;
                String message = "simulation " + (sim_i + 1) + "/" + sims + " ...";
                p.updateMessage(message);
                start_time_sim = System.nanoTime();
            }
            
            simulator.simulate(sim, setting.end_time, p.start_subroutine(fraction_of_goal));
            p.end_subroutine();
            result.addSimulation(sim, System.nanoTime() - start_time_sim);
        }
        
        // save stats
        ArrayList<Pair<String, Number>> stats = simulator.getStatistics();
        result.stats = stats;
    }
}
