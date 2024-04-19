package core.simulation.simulators;

import core.model.Reaction;
import core.model.Setting;
import core.util.Progressable;
import core.util.Stochastics;
import core.util.Vector;
import java.util.Arrays;
import java.util.Random;
import org.apache.commons.math3.random.AbstractRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import core.simulation.events.IntervalStateChangeEndCondition;
import core.simulation.events.IntervalStateChangeListener;
import core.simulation.Simulation;
import core.simulation.events.SimulationEventHandler.Preview;

/**
 *
 * @author Martin
 */
public class HybridSimulator extends AbstractSimulator implements IntervalStateChangeListener{
    
    public final int threshold_tau;
    public final int threshold_ode;
    
    public final double factor_SSA_to_TAU;
    public final double factor_TAU_to_ODE;
    
    private SSASimulator SSA_simulator;
    private boolean SSA_active;
    private TauSimulator TAU_simulator;
    private boolean TAU_active;
    private ODESimulator ODE_simulator;
    private boolean ODE_active;
    
    public static boolean DEBUG = false;
    
    private int[] classification;
    
    public long reactions_ssa = 0;
    public long reactions_tau = 0;
    public double reactions_ode = 0;
    
    double[] tmp_propensities = new double[crn.reactions.length];
    
    public HybridSimulator(Setting setting, SSASimulator SSA_simulator, TauSimulator TAU_simulator, ODESimulator ODE_simulator, int threshold_tau, int threshold_ode, double factor_SSA_to_TAU, double factor_TAU_to_ODE) {
        super(setting);
        this.SSA_simulator = SSA_simulator;
        this.TAU_simulator = TAU_simulator;
        this.ODE_simulator = ODE_simulator;
        this.threshold_tau = threshold_tau;
        this.threshold_ode = threshold_ode;
        this.factor_SSA_to_TAU = factor_SSA_to_TAU;
        this.factor_TAU_to_ODE = factor_TAU_to_ODE;
        
        ODE_simulator.addIntervalStateChangeListener(new IntervalStateChangeEndCondition());
        
        addIntervalStateChangeListener(this);
        
        classification = new int[setting.dim()];
//        this.reclassifier = new IntervalStateEventHandler(setting) {
//            @Override
//            public boolean init(double start_time, double[] start_state) {
////                System.out.println("running reclassifier init");
//                if (!super.init(start_time, start_state)) return false;
//                classifyReactions(cur_interval_state);
//                return true;
//            }
//            
//            @Override
//            protected boolean continueAfterIntervalStateCange(int[] to, int[] offsets) {
//                if (always_stop_when_leaving_interval_state || to == null) return false;
//                
//                classifyReactions(to);
//                return true;
//            }
//        };
//        addSimulationEventHandler(reclassifier);
    }

    @Override
    public void simulate(Simulation sim, double end_t, Progressable p, int[] interval_state_hint) {
        Random rand = sim.getRandom();
        RandomGenerator rand2 = new AbstractRandomGenerator() {
            @Override
            public void setSeed(long l) {
                rand.setSeed(l);
            }

            @Override
            public double nextDouble() {
                return rand.nextDouble();
            }
        };
        
        ODE_simulator.resetEventHandlers();
        
        double t = sim.getTime();
        double[] state = sim.getState();
        double[] state_copy = sim.getState();
        
        double[] propensities = new double[crn.reactions.length]; // aka: a(x)
        int[] times = new int[crn.reactions.length];
        int[] SSA_reaction = new int[1];
        double[] ODE_nrOfReactionsReturn = new double[1];
        
        int reactions = 0;
        
        if (!initSimulationEventHandlers(t, state, interval_state_hint)) {
            return;
        }
        
        while (t < end_t) {
//            System.out.println(reactions_ssa + " " + reactions_tau + " " + reactions_ode);
            if (p.isStopped()) break;
            
            double propensities_sum = activePropensities(state, propensities); 
            Preview preview = Preview.CONTINUE;
            
            // check if stuck:
            if (propensities_sum == 0) {
                // jump to t_end
                t = end_t;
            }
            else {
                // save start (in case we need to undo the greedy jump)    
                System.arraycopy(state, 0, state_copy, 0, dim);
                
                double tau = end_t - t;
                                
                // sample SSA reaction
                double SSA_tau = Double.MAX_VALUE;
                if (SSA_active) {
                    SSA_tau = SSA_simulator.step(rand, state, SSA_reaction, tmp_propensities, propensities);
                    if (SSA_tau <= 0) throw new IllegalStateException("SSA_tau must be larger than 0");
                }
                if (SSA_tau <= tau) tau = SSA_tau;
                else SSA_reaction[0] = -1;  // SSA reaction is too late (i.e. after t_end)
                
                // compute tau for TAU_LEAPING
                double TAU_tau = Double.MAX_VALUE;
                if (TAU_active) {
                    TAU_tau = TAU_simulator.computeTau(state, propensities);
                    if (TAU_tau <= 0) throw new IllegalStateException("TAU_tau must be larger than 0");
                }
                if (TAU_tau < tau) {
                    tau = TAU_tau;
                    SSA_reaction[0] = -1;  // SSA reaction is too late (i.e. after t + TAU_tau)
                }
                
                
                if (DEBUG && p.needsMessage()) {
                    p.updateMessage("");
                    p.updateMessage("time: " + t);
                    p.updateMessage("state: " + Arrays.toString(state));
                    p.updateMessage("propensities: " + Arrays.toString(propensities));
                }
                
                // perform ODE change
                double ODE_tau = Double.MAX_VALUE;
                double ODE_reactions = 0;
                if (ODE_active) {
                    ODE_tau = ODE_simulator.simulate(0, state, tau, ODE_nrOfReactionsReturn);
                    if (ODE_tau <= 0) throw new IllegalStateException("ODE_tau must be larger than 0");
                    ODE_reactions = ODE_nrOfReactionsReturn[0];
                    if (DEBUG && ODE_reactions > 0 && p.needsMessage()) p.updateMessage("    " + ODE_reactions + " ODE reactions to " + Arrays.toString(state));
                }
                if (ODE_tau < tau) {
                    tau = ODE_tau;
                    SSA_reaction[0] = -1;   // SSA reaction is too late (i.e. after t + ODE_tau)
                }
                
                if (DEBUG && p.needsMessage()) p.updateMessage("taus: " + tau + " " + Arrays.toString(new double[]{SSA_tau, TAU_tau, ODE_tau}) + "     at t=" + t);
                
                // greedy: do full TAU leap
                int reactions_TAU = 0;
                for (int r_i = 0; r_i < crn.reactions.length; r_i++) {
                    times[r_i] = 0;
                    if (propensities[r_i] > 0 && TAU_simulator.active[r_i]) {
                        times[r_i] = Stochastics.samplePoissonDistribution(rand2, propensities[r_i]*tau);
                    }
                    reactions_TAU += times[r_i];
                    crn.reactions[r_i].applyTo(state, times[r_i]);
                }
                if (DEBUG && reactions_TAU > 0 && p.needsMessage()) p.updateMessage("    TAU-leaping " + reactions_TAU + " (" + Arrays.toString(times) + ") reactions to " + Arrays.toString(state));
                if (DEBUG && tau == TAU_tau && reactions_TAU == 0 && p.needsMessage()) p.updateMessage("    TAULEAP with no effect!");
                
                // check if ODE + TAU worked without event
                preview = preview(t + tau, state);
                if (preview == Preview.CONTINUE || reactions_TAU == 0) {
                    // whole step worked!
                    reactions += reactions_TAU;
                    reactions_tau += reactions_TAU;
                    reactions += ODE_reactions;
                    reactions_ode += ODE_reactions;
                    t += tau;
                }
                else if (reactions_TAU > 0) {   // step by step!
                    // undo greedy jump
                    System.arraycopy(state_copy, 0, state, 0, dim);
                    if (DEBUG && p.needsMessage()) p.updateMessage("    undo to " + Arrays.toString(state));
                    
                    // compute the time between discrete reactions
                    double tau_part = tau / (reactions_TAU + 1);
                    
                    // wait for next discrete reaction
                    if (ODE_active) {
                        t += ODE_simulator.simulate(0, state, tau_part, ODE_nrOfReactionsReturn);
                        reactions += ODE_nrOfReactionsReturn[0];
                        reactions_ode += ODE_nrOfReactionsReturn[0];
                        if (DEBUG && p.needsMessage()) p.updateMessage("        ODE: " + ODE_nrOfReactionsReturn[0]);
                    } else t += tau_part;
                    
                    preview = preview(t, state);
                    if (preview == Preview.CONTINUE) {
                        int reaction_discrete_left = reactions_TAU;
                        while (reaction_discrete_left > 0) {
                            // choose a random reaction that was part of the jump
                            int chosenReactionIndex = Stochastics.choose(rand, times, reaction_discrete_left);
                            // apply it
                            reaction_discrete_left--;
                            times[chosenReactionIndex]--;
                            reactions++;
                            reactions_tau++;
                            crn.reactions[chosenReactionIndex].applyTo(state);
                            if (DEBUG && p.needsMessage()) p.updateMessage("        discrete: 1");
                            // stop if there is an event
                            preview = preview(t, state);
                            if (preview == Preview.PROHIBITED) {
                                // undo last step because it is prohibited
                                reaction_discrete_left++;
                                times[chosenReactionIndex]++;
                                reactions--;
                                reactions_tau--;
                                crn.reactions[chosenReactionIndex].applyTo(state, -1);
                                preview = preview(t, state);
                                if (DEBUG && p.needsMessage()) p.updateMessage("            undo to " + Arrays.toString(state));
                                break;
                            }
                            if (preview == Preview.EVENT) break;
                            
                            // wait for next discrete reaction
                            if (ODE_active) {
                                t += ODE_simulator.simulate(0, state, tau_part, ODE_nrOfReactionsReturn);
                                reactions += ODE_nrOfReactionsReturn[0];
                                reactions_ode += ODE_nrOfReactionsReturn[0];
                                if (DEBUG && p.needsMessage()) p.updateMessage("        ODE: " + ODE_nrOfReactionsReturn[0]);
                                preview = preview(t, state);
                                if (preview != Preview.CONTINUE) break;
                            } else t += tau_part;
                        }
                    }
                }
                
                // perform SSA
                if (SSA_reaction[0] >= 0 && preview == Preview.CONTINUE) {
                    Preview preview_before_SSA = preview;
                    crn.reactions[SSA_reaction[0]].applyTo(state);
                    if (DEBUG && p.needsMessage()) p.updateMessage("    1 SSA reaction (" + SSA_reaction[0] + ") to " + Arrays.toString(state));
                    reactions++;
                    reactions_ssa++;
                    preview = preview(t, state);
                    if (preview == Preview.PROHIBITED) {
                        // undo last step because it is prohibited
                        reactions--;
                        reactions_ssa--;
                        crn.reactions[SSA_reaction[0]].applyTo(state, -1);
                        preview = preview_before_SSA;
                        if (DEBUG && p.needsMessage()) p.updateMessage("        undo to " + Arrays.toString(state));
                    }
                }
            }
            
            if (sim.needIntermidiateSteps()) {
                if (t == Double.POSITIVE_INFINITY) t = end_t;
                sim.evolve(state, t, reactions);
                reactions = 0;
            }
            
            p.progressSubroutineTo(t / end_t);
            
            boolean cont = t < end_t && preview != Preview.PROHIBITED && confirm(t, state, preview);
            if (!cont) break;
        }
        
        // check that we updated the simulation data
        if (t == Double.POSITIVE_INFINITY) t = end_t;
        if (sim.getTime() != t || reactions != 0 || !Arrays.equals(state, sim.getState())) {
            sim.evolve(state, t, reactions);
        }
        p.progressSubroutineTo(1.0);
    }
    
    public void classifyReactions(int[] interval_state) {
        if (interval_state == null) return;     // out of bounds!
        
        // classify each species as SSA / TAU / ODE according to size of interval state
        for (int dim_i = 0; dim_i < dim; dim_i++) {
            int size = setting.intervals[dim_i][interval_state[dim_i]].size();
            if (size > threshold_ode) classification[dim_i] = 2;
            else if (size > threshold_tau) classification[dim_i] = 1;
            else classification[dim_i] = 0;
        }

        // classify each reaction with most precise type of all its reactants and products
        SSA_active = false;
        TAU_active = false;
        ODE_active = false;
        int[] lowest = new int[setting.crn.reactions.length];
        for (int r_i = 0; r_i < setting.crn.reactions.length; r_i++) {
            SSA_simulator.active[r_i] = false;
            TAU_simulator.active[r_i] = false;
            ODE_simulator.active[r_i] = false;
            if (!active[r_i]) continue;
            Reaction r = setting.crn.reactions[r_i];
            int lowest_classification = 2;
//            boolean has_effect = false;
            for (int dim_i = 0; dim_i < interval_state.length; dim_i++) {
//                if (r.reactants[dim_i] != 0 || r.products[dim_i] != 0) {
                if (r.products[dim_i] - r.reactants[dim_i] != 0) {
//                    has_effect = true;
                    lowest_classification = Math.min(lowest_classification, classification[dim_i]);
                    if (lowest_classification == 0) break;
                }
            }
            lowest[r_i] = lowest_classification;
//            if (!has_effect) continue;
            switch (lowest_classification) {
                case 2:
                    ODE_simulator.active[r_i] = true;
                    ODE_active = true;
                    break;
                case 1:
                    TAU_simulator.active[r_i] = true;
                    TAU_active = true;
                    break;
                default:
                    SSA_simulator.active[r_i] = true;
                    SSA_active = true;
                    break;
            }
        }
        
        // make sure that TAU / ODE are worth it
        double[] rep = Vector.asDoubleArray(setting.representativeForIntervalState(interval_state));
        double propensity_sum_SSA = SSA_simulator.activePropensities(rep, tmp_propensities);
        double propensity_sum_TAU = TAU_simulator.activePropensities(rep, tmp_propensities);
        double propensity_sum_ODE = ODE_simulator.activePropensities(rep, tmp_propensities);
        
//        System.out.println(
//                        Arrays.toString(interval_state) 
//                                + "   SSA: " + IO.significantFigures(propensity_sum_SSA)
//                                + "   TAU: " + IO.significantFigures(propensity_sum_TAU) 
//                                + "   ODE: " + IO.significantFigures(propensity_sum_ODE));
        if (TAU_active && ODE_active && factor_TAU_to_ODE * propensity_sum_TAU > propensity_sum_ODE) {
            ODE_active = false;
            for (int r_i = 0; r_i < setting.crn.reactions.length; r_i++) {
                if (ODE_simulator.active[r_i]) {
                    ODE_simulator.active[r_i] = false;
                    TAU_simulator.active[r_i] = true;
                }
            }
            propensity_sum_TAU += propensity_sum_ODE;
            propensity_sum_ODE = 0;
//            System.out.println(
//                        Arrays.toString(interval_state) 
//                                + "   SSA: " + IO.significantFigures(propensity_sum_SSA)
//                                + "   TAU: " + IO.significantFigures(propensity_sum_TAU) 
//                                + "   ODE: " + IO.significantFigures(propensity_sum_ODE));
        }
        if (SSA_active && ODE_active && factor_SSA_to_TAU * factor_TAU_to_ODE * propensity_sum_SSA > propensity_sum_ODE) {
            ODE_active = false;
            for (int r_i = 0; r_i < setting.crn.reactions.length; r_i++) {
                if (ODE_simulator.active[r_i]) {
                    ODE_simulator.active[r_i] = false;
                    if (TAU_active) TAU_simulator.active[r_i] = true;
                    else SSA_simulator.active[r_i] = true;
                }
            }
            if (TAU_active) propensity_sum_TAU += propensity_sum_ODE;
            else propensity_sum_SSA += propensity_sum_ODE;
            propensity_sum_ODE = 0;
//            System.out.println(
//                        Arrays.toString(interval_state) 
//                                + "   SSA: " + IO.significantFigures(propensity_sum_SSA)
//                                + "   TAU: " + IO.significantFigures(propensity_sum_TAU) 
//                                + "   ODE: " + IO.significantFigures(propensity_sum_ODE));
        }
        if (SSA_active && TAU_active && factor_SSA_to_TAU * propensity_sum_SSA > propensity_sum_TAU) {
            TAU_active = false;
            for (int r_i = 0; r_i < setting.crn.reactions.length; r_i++) {
                if (TAU_simulator.active[r_i]) {
                    TAU_simulator.active[r_i] = false;
                    SSA_simulator.active[r_i] = true;
                }
            }
            propensity_sum_SSA += propensity_sum_TAU;
            propensity_sum_TAU = 0;
//            System.out.println(
//                        Arrays.toString(interval_state) 
//                                + "   SSA: " + IO.significantFigures(propensity_sum_SSA)
//                                + "   TAU: " + IO.significantFigures(propensity_sum_TAU) 
//                                + "   ODE: " + IO.significantFigures(propensity_sum_ODE));
        }
        
        if (ODE_active) {
//            System.out.println("ODE_active with " + IO.significantFigures(propensity_sum_ODE));
        }
//        System.out.println("");
        
        if (DEBUG) System.out.println("    classification " + Arrays.toString(interval_state) + ": " + Arrays.toString(classification) + "   ->   " + Arrays.toString(lowest));
    }
    
    @Override
    public void init(double time, double[] state, int[] interv_state) {
        classifyReactions(interv_state);
    }

    @Override
    public boolean notify(double time, double[] state, int[] previous_interv_state, int[] new_interv_state, int[] interv_state_offset) {
        classifyReactions(new_interv_state);
        return true;
    }
}
