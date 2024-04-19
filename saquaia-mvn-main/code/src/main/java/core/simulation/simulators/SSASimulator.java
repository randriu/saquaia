package core.simulation.simulators;

import core.model.Reaction;
import core.util.Pair;
import core.model.Setting;
import core.util.Progressable;
import core.util.Stochastics;
import java.util.Random;
import core.simulation.Simulation;
import java.util.HashSet;

/**
 *
 * @author Martin
 */
public class SSASimulator extends AbstractSimulator{
    
    // OPTIMIZED VERSION
    private double[] tmp_propensities;
    private double[] precomputed_propensities;
    private DependencyGraph g;
    
    private boolean OPTIMIZED = false;      // use dependency graph?
    
    public SSASimulator(Setting setting) {
        super(setting);
        
        tmp_propensities = new double[setting.crn.reactions.length];
        precomputed_propensities = new double[setting.crn.reactions.length];
        g = new DependencyGraph();
    }
    
    @Override
    public boolean dependsOnC() {
        return false;
    };

    @Override
    public void simulate(Simulation sim, double end_t, Progressable p, int[] interval_state_hint) {
        double t = sim.getTime();
        double[] state = sim.getState();
        if (!initSimulationEventHandlers(t, state, interval_state_hint)) {
            return;
        }
        
        int reactions_applied_but_not_reported = 0;
        
        // OPTIMIZED VERSION needs initial values
        if (OPTIMIZED) precomputed_propensities = activePropensities(state);
        
        while (t < end_t) {
            if (p.isStopped()) break;
            
            if (OPTIMIZED) t = SSASimulator.this.optimizedStep(sim.getRandom(), t, end_t, state, tmp_propensities, precomputed_propensities, g);
            else t = SSASimulator.this.step(sim.getRandom(), t, end_t, state);
            
            reactions_applied_but_not_reported ++;
            boolean cont = t < end_t && justGoto(t, state);
            if (!cont || sim.needIntermidiateSteps()) {
                sim.evolve(state, t, reactions_applied_but_not_reported);
                reactions_applied_but_not_reported = 0;
            }
            
            p.progressSubroutineTo(t / end_t);
            if (!cont) break;
        }
    }
    
    public double step(Random rand, double t, double end_t, double[] state) {
        double[] propensities = activePropensities(state);
        Pair<Integer, Double> chooseAndSum = Stochastics.chooseAndSum(rand, propensities);
        if (chooseAndSum == null) return end_t; // all proensities are 0!
        double next_t = t + Stochastics.sampleExponential(rand, chooseAndSum.right);
        setting.crn.reactions[chooseAndSum.left].applyTo(state);
        return next_t;
    }
    
    public double optimizedStep(Random rand, double t, double end_t, double[] state, double[] tmp_propensities, double[] precomputed_propensities, DependencyGraph g) {
        double propensities_sum = activePropensitiesWithPrecomputedValues(tmp_propensities, precomputed_propensities);
        
        if (propensities_sum == 0) return end_t; // all proensities are 0!
        
        int chosen = Stochastics.choose(rand, tmp_propensities, propensities_sum);
        
        double next_t = t + Stochastics.sampleExponential(rand, propensities_sum);
        setting.crn.reactions[chosen].applyTo(state);
        
        g.updatePropensitiesAfterReaction(chosen, precomputed_propensities, state);
        
        return next_t;
    }
    
//    public double step(Random rand, double[] state, int[] times, double[] tmp_propensities, double[] precomputed_propensities) {
//        double propensities_sum = 0;
//        if (tmp_propensities == null) tmp_propensities = new double[crn.reactions.length];
//        if (precomputed_propensities != null) 
//            propensities_sum = activePropensitiesWithPrecomputedValues(tmp_propensities, precomputed_propensities);
//        else
//            propensities_sum = activePropensities(state, tmp_propensities);
//        if (propensities_sum > 0) {
//            times[Stochastics.choose(rand, tmp_propensities, propensities_sum)] ++;
//            return Stochastics.sampleExponential(rand, propensities_sum);
//        } 
//        return Double.MAX_VALUE;
//        
//    }
    
    public double step(Random rand, double[] state, int[] chosenReaction, double[] tmp_propensities, double[] precomputed_propensities) {
        double propensities_sum = 0;
        if (tmp_propensities == null) tmp_propensities = new double[setting.crn.reactions.length];
        if (chosenReaction == null) chosenReaction = new int[1];
        if (precomputed_propensities != null) 
            propensities_sum = activePropensitiesWithPrecomputedValues(tmp_propensities, precomputed_propensities);
        else
            propensities_sum = activePropensities(state, tmp_propensities);
        chosenReaction[0] = -1;
        if (propensities_sum > 0) {
            chosenReaction[0] = Stochastics.choose(rand, tmp_propensities, propensities_sum);
            return Stochastics.sampleExponential(rand, propensities_sum);
        } 
        return Double.MAX_VALUE;
    }

    private class DependencyGraph {
        int[][] dependencies;
        public DependencyGraph() {
            dependencies = new int[setting.crn.reactions.length][];
            
            for (int rection_i = 0; rection_i < setting.crn.reactions.length; rection_i++) {
                HashSet<Integer> to_update = new HashSet<>();
                for (int dim_i = 0; dim_i < setting.dim(); dim_i++) {
                    if (setting.crn.reactions[rection_i].reactants[dim_i] - setting.crn.reactions[rection_i].products[dim_i] != 0) {
                        for (int rection_j = 0; rection_j < setting.crn.reactions.length; rection_j++) {
                            if (setting.crn.reactions[rection_j].reactants[dim_i] > 0) {
                                to_update.add(rection_j);
                            }
                        }
                    }
                }
                dependencies[rection_i] = new int[to_update.size()];
                int i = 0;
                for (int x : to_update) {
                    dependencies[rection_i][i] = x;
                    i++;
                }
            }
        }
        
        public void updatePropensitiesAfterReaction(int reaction_i, double[] propensities, double[] state) {
            for (int rection_j : dependencies[reaction_i]) {
                if (! active[rection_j]) continue;
                propensities[rection_j] = setting.crn.reactions[rection_j].propensity(state);
            }
        }
    }
}
