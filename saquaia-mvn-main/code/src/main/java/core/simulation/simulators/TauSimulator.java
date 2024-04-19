package core.simulation.simulators;

import core.model.Setting;
import core.util.Progressable;
import core.util.Stochastics;
import java.util.Arrays;
import java.util.Random;
import org.apache.commons.math3.random.AbstractRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import core.simulation.Simulation;
import core.simulation.events.SimulationEventHandler.Preview;

/**
 *
 * @author Martin
 */
public class TauSimulator extends AbstractSimulator {
    
    public final double epsilon;
    
    public boolean DEBUG = false;
    private boolean tauComputationDataInitialized = false;
    
    private double mi[];
    private double sigmaSquared[];
    private int [][] v;
    private GCalculator [] g;
    private boolean[] I_ri;
    
    public TauSimulator(Setting setting, double epsilon) {
        super(setting);
        this.epsilon = epsilon;
    }
    
    @Override
    public boolean dependsOnC() {
        return false;
    };

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
        
        double t = sim.getTime();
        double[] state = sim.getState();
        double[] state_copy = sim.getState();
        
        double[] propensities = new double[setting.crn.reactions.length]; // aka: a(x)
        int[] times = new int[setting.crn.reactions.length];
        int reactions = 0;
        
        if (!initSimulationEventHandlers(t, state, interval_state_hint)) {
            return;
        }
        
        if (!tauComputationDataInitialized) initTauComputationData();
        
        while (t < end_t) {
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
                
                // get tau
                double tau = end_t - t;
                double TAU_tau = computeTau(state, propensities);
                if (TAU_tau <= 0) throw new IllegalStateException("TAU_tau must be larger than 0");
                if (tau > TAU_tau) tau = TAU_tau;
                
                // greedy: do full leap
                int reactions_in_leap = 0;
                for (int r_i = 0; r_i < crn.reactions.length; r_i++) {
                    times[r_i] = 0;
                    if (propensities[r_i] > 0 && active[r_i]) times[r_i] = Stochastics.samplePoissonDistribution(rand2, propensities[r_i]*tau);
                    reactions_in_leap += times[r_i];
                    crn.reactions[r_i].applyTo(state, times[r_i]);
                }
                if (DEBUG && p.needsMessage()) p.updateMessage("leaping " + reactions_in_leap + " (" + Arrays.toString(times) + ") to " + Arrays.toString(state));
                if (DEBUG && p.needsMessage() && tau == TAU_tau && reactions_in_leap == 0) p.updateMessage("TAULEAP with no effect!");
                
                preview = preview(t + tau, state);
                if (preview == Preview.CONTINUE || reactions_in_leap == 0) {
                    // whole step worked!
                    reactions += reactions_in_leap;
                    t += tau;
                }
                else if (reactions_in_leap > 0) {  // step by step!
                    // undo greedy jump
                    System.arraycopy(state_copy, 0, state, 0, dim);
                    preview = Preview.CONTINUE;
                    if (DEBUG && p.needsMessage()) p.updateMessage("    undo to " + Arrays.toString(state));
                    
                    // compute the time between steps / reactions
                    double tau_part = tau / (reactions_in_leap + 1);
                    
                    // wait for first reaction
                    t += tau_part;
                    
                    int reaction_discrete_left = reactions_in_leap;
                    while (reaction_discrete_left > 0) {
                        // choose a random reaction that was part of the jump
                        int chosenReactionIndex = Stochastics.choose(rand, times, reaction_discrete_left);
                        // apply it
                        reaction_discrete_left--;
                        times[chosenReactionIndex]--;
                        reactions++;
                        setting.crn.reactions[chosenReactionIndex].applyTo(state);
                        if (DEBUG && p.needsMessage()) p.updateMessage("    discrete: 1");
                        // stop if there is an event
                        preview = preview(t, state);
                        if (preview == Preview.PROHIBITED) {
                            // undo last step because it is prohibited
                            reaction_discrete_left++;
                            times[chosenReactionIndex]++;
                            reactions--;
                            crn.reactions[chosenReactionIndex].applyTo(state, -1);
                            preview = preview(t, state);
                            break;
                        }
                        if (preview != Preview.CONTINUE) break;
                        // wait until next reaction
                        t += tau_part;
                    }
                    if (DEBUG && p.needsMessage()) p.updateMessage("    " + reaction_discrete_left + "    " + Arrays.toString(times));
                } 
            }
            
            if (sim.needIntermidiateSteps()) {
                sim.evolve(state, t, reactions);
                reactions = 0;
            }
            
            p.progressSubroutineTo(t / end_t);
            
            boolean cont = t < end_t && preview != Preview.PROHIBITED && confirm(t, state, preview);
            if (!cont) break;
        }
        
        if (t == Double.POSITIVE_INFINITY) t = end_t;
        if (sim.getTime() != t || reactions != 0 || !Arrays.equals(state, sim.getState())) {
            sim.evolve(state, t, reactions);
        }
        p.progressSubroutineTo(1.0);
    }

    public double computeTau(double[] state, double[] propensities) {
        if (!tauComputationDataInitialized) initTauComputationData();
        
    	for (int i = 0; i < crn.dim(); i++) {
            if (!I_ri[i]) continue;
            mi[i] = 0.;
            sigmaSquared[i] = 0;
            for (int j = 0; j < crn.reactions.length; j++) {
                if (!active[j]) continue;
                mi[i] += v[i][j]*propensities[j];
                sigmaSquared[i] += v[i][j]*v[i][j]*propensities[j];
            }
    	}
        
    	double tau = Double.POSITIVE_INFINITY;
    	for (int i = 0; i < crn.dim(); i++) {
            if (!I_ri[i]) continue;
            double gVal = g[i].calculateG(state);
            if (gVal == 0.) {
                continue;
            }
    		
            assert(mi[i] != 0. && sigmaSquared[i] != 0.);
    		
            double tauPrime = Math.max(epsilon*state[i]/gVal, 1.) / Math.abs(mi[i]);
            double tauPrimePrime = Math.pow(Math.max(epsilon*state[i]/gVal, 1.), 2.) / sigmaSquared[i];
            double newTau = Math.min(tauPrime, tauPrimePrime);
    		
            if (newTau < tau) {
                tau = newTau;
            }
    	}
    	
        return tau;
    }
    
    private interface GCalculator {
        public double calculateG(double[] state);
    }
    
    private class ConstantGCalculator implements GCalculator {
    	private double constantFactor;
    	
        @Override
    	public double calculateG(double[] state) {
            return constantFactor;
    	}
    	
    	public ConstantGCalculator(double constantFactor) {
            this.constantFactor = constantFactor;
    	}
    	
    	public ConstantGCalculator() {
            this(0.);
    	}
    }
    
    private class GCalculatorHOR2 implements GCalculator {
    	private int speciesNum;
    	
    	@Override
    	public double calculateG(double[] state) {
            return 2. + 1./(state[speciesNum] - 1.);
    	}
    	
    	public GCalculatorHOR2(int speciesNum) {
            this.speciesNum = speciesNum;
    	}
    }
    
    private class GCalculatorHOR3_1 implements GCalculator {
    	private int speciesNum;
    	
    	@Override
    	public double calculateG(double[] state) {
            return 1.5 * (2. + 1. / (state[speciesNum] - 1.));  //3. + 1.5/(state[speciesNum] - 1.);
    	}
    	
    	public GCalculatorHOR3_1(int speciesNum) {
            this.speciesNum = speciesNum;
    	}
    }
    
    private class GCalculatorHOR3_2 implements GCalculator {
    	private int speciesNum;
    	
    	@Override
    	public double calculateG(double[] state) {
            return 3. + 1./(state[speciesNum] - 1.) + 2./(state[speciesNum] - 2.);
    	}
    	
    	public GCalculatorHOR3_2(int speciesNum) {
            this.speciesNum = speciesNum;
    	}
    }
    
    public void initTauComputationData() {
        tauComputationDataInitialized = true;
        
    	mi = new double [crn.dim()];
    	sigmaSquared = new double [crn.dim()];
    	g = new GCalculator[crn.dim()];
    	v = new int [crn.dim()][crn.reactions.length];
        I_ri = new boolean[dim];
        
        // compute HOR(i), I_ri and v matrix
        int[] HOR = new int[dim];   // HOR - Highest order reaction
        int[] reactionOrders = new int[crn.reactions.length];
        for (int j = 0; j < crn.reactions.length; j++) {
            reactionOrders[j] = crn.reactions[j].getReactionOrder();
            for (int i = 0; i < crn.dim(); i++) {
                v[i][j] = crn.reactions[j].products[i] - crn.reactions[j].reactants[i];
                if (crn.reactions[j].reactants[i] > 0) {
                    I_ri[i] = true;
                    if (HOR[i] < reactionOrders[j]) HOR[i] = reactionOrders[j];
                }
            }
        }
    	
    	for (int i = 0; i < crn.dim(); i++) {
            if (!I_ri[i]) continue;
            if (HOR[i] == 1) g[i] = new ConstantGCalculator(1);
            else if (HOR[i] == 2) {
                int max_i_in_order2 = 0;
                for (int j = 0; j < crn.reactions.length; j++) {
                    if (reactionOrders[j] == 2 && crn.reactions[j].reactants[i] > max_i_in_order2) max_i_in_order2 = crn.reactions[j].reactants[i];
                }
                if (max_i_in_order2 == 2) g[i] = new GCalculatorHOR2(i);
                else g[i] = new ConstantGCalculator(2);
            }
            else if (HOR[i] == 3) {
                int max_i_in_order3 = 0;
                for (int j = 0; j < crn.reactions.length; j++) {
                    if (reactionOrders[j] == 3 && crn.reactions[j].reactants[i] > max_i_in_order3) max_i_in_order3 = crn.reactions[j].reactants[i];
                }
                if (max_i_in_order3 == 3) g[i] = new GCalculatorHOR3_2(i);
                else if (max_i_in_order3 == 2) g[i] = new GCalculatorHOR3_1(i);
                else g[i] = new ConstantGCalculator(3);
            }
            else throw new IllegalArgumentException("TAU Leaping only inplemented for reactions with at most 3 reactants.");
    	}
    }
}
