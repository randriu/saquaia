package core.simulation.simulators;

import core.model.Setting;
import core.util.Progressable;
import org.apache.commons.math3.ode.AbstractIntegrator;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math3.ode.sampling.StepHandler;
import org.apache.commons.math3.ode.sampling.StepInterpolator;
import core.simulation.Simulation;
import core.simulation.events.SimulationEventHandler;

/**
 *
 * @author Martin
 */
public class ODESimulator extends AbstractSimulator {

    public final AbstractIntegrator integrator;
    public static double DEFAULT_MIN_STEP = 1.0E-12;//1.0e-8;
    public static double DEFAULT_MAX_STEP = 100;    //1.0e+6;
    public static double DEFAULT_SCAL_ABSOLUTE_TOLERANCE = 1.0E-3;//1.0e-10;
    public static double DEFAULT_SCAL_RELATIVE_TOLERANCE = 1.0E-8;//1.0e-10;
    
    public static double EVENT_MAX_CHECK_INTERVAL = 1.0e+6;
    public static double EVENT_CONVERGENCE = 1.0E-6;//1.0e-4;
    public static int EVENT_MAX_ITERATION_COUNT = 1000000;
    
    private final FirstOrderDifferentialEquations FODE;

    public ODESimulator(Setting setting, AbstractIntegrator integrator) {
        super(setting);
        this.integrator = integrator;
        this.FODE = new FirstOrderDifferentialEquations() {
            
            @Override
            public int getDimension() {
                return dim + 1;   // tracking number of reactions in extra dimension
            }

            @Override
            public void computeDerivatives(double t, double[] y, double[] yDot) {
                for (int i = 0; i < yDot.length; i++) yDot[i] = 0;
                
                for (int r_i = 0; r_i < crn.reactions.length; r_i++) {
                    if (active[r_i]) {
                        double propensity = crn.reactions[r_i].propensity(y);
                        yDot[dim] += propensity;
                        
                        for (int dim_i = 0; dim_i < dim; dim_i++) {
                            yDot[dim_i]+= (crn.reactions[r_i].products[dim_i] - crn.reactions[r_i].reactants[dim_i]) * propensity;
                        }
                    }
                }
            }
        };
    }

    @Override
    public boolean isDeterministic() {
        return true;
    }
    
    @Override
    public boolean dependsOnC() {
        return false;
    };

    @Override
    public void simulate(Simulation sim, double end_t, Progressable p, int[] interval_state_hint) {
        resetEventHandlers();   // FIX for: ODESimulator sometimes skips events. Not sure why, nore why this helps.
        
        integrator.clearStepHandlers();
        if (sim.needIntermidiateSteps()) {
            integrator.addStepHandler(new StepHandler() {
                double[] tmp = new double[crn.dim()];
                double last_reactions = 0;
                
                @Override
                public void init(double d, double[] doubles, double d1) {
                }

                @Override
                public void handleStep(StepInterpolator si, boolean bln) {
//                    System.out.println(Arrays.toString(si.getInterpolatedState()));
                    System.arraycopy(si.getInterpolatedState(), 0, tmp, 0, tmp.length);
                    double reactions = si.getInterpolatedState()[tmp.length];
                    sim.evolve(tmp, si.getCurrentTime(), reactions - last_reactions);
                    last_reactions = reactions;
                    p.progressSubroutineTo(si.getCurrentTime() / end_t);
                }
            });
        }
        
        double[] state = sim.getState();
        double[] nrOfReactions = new double[1];
        double t = simulate(sim.getTime(), state, end_t, nrOfReactions);
        sim.evolve(state, t, nrOfReactions[0]);
        p.progressSubroutineTo(t / end_t);
    }
    
    public double simulate(double start_time, double[] state, double end_time, double[] nrOfReactions) {
        if (start_time == end_time) {
            return end_time;
        }
        
        resetEventHandlers();   // FIX for: ODESimulator sometimes skips events. Not sure why, nore why this helps.
        
        double[] state_and_nr_of_reactions = new double[state.length+1];
        System.arraycopy(state, 0, state_and_nr_of_reactions, 0, state.length);
        double time = integrator.integrate(FODE, start_time, state_and_nr_of_reactions, end_time, state_and_nr_of_reactions);
        System.arraycopy(state_and_nr_of_reactions, 0, state, 0, state.length);
        if (nrOfReactions != null) nrOfReactions[0] = state_and_nr_of_reactions[state.length];
        return time;
    }
    
    public void resetEventHandlers() {
        integrator.clearEventHandlers();
        for (SimulationEventHandler h : eventHandlers) {
//            System.out.println("adding eventHandler to ODESimulator");
            integrator.addEventHandler(
                    h, 
                    EVENT_MAX_CHECK_INTERVAL, 
                    EVENT_CONVERGENCE, 
                    EVENT_MAX_ITERATION_COUNT
            );
        }
    }
}
