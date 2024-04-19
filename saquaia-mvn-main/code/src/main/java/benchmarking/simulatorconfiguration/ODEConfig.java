package benchmarking.simulatorconfiguration;

import core.model.Setting;
import org.apache.commons.math3.ode.nonstiff.DormandPrince54Integrator;
import core.simulation.events.OutOfBoundsEndCondition;
import core.simulation.simulators.ODESimulator;

/**
 *
 * @author Martin
 */
public class ODEConfig extends SimulatorConfig{
    
    double min_step = ODESimulator.DEFAULT_MIN_STEP;
    double max_step = ODESimulator.DEFAULT_MAX_STEP;
    double scal_absolute_tolerance = ODESimulator.DEFAULT_SCAL_ABSOLUTE_TOLERANCE;
    double scal_relative_tolerance = ODESimulator.DEFAULT_SCAL_RELATIVE_TOLERANCE;
    
    public ODEConfig(){
        super(Type.ODE);
    }

    public double getMinStep() {
        return min_step;
    }

    public double getMaxStep() {
        return max_step;
    }

    public double getScalAbsoluteTolerance() {
        return scal_absolute_tolerance;
    }

    public double getScalRelativeTolerance() {
        return scal_relative_tolerance;
    }

    public ODEConfig setMinStep(double min_step) {
        this.min_step = min_step;
        return this;
    }

    public ODEConfig setMaxStep(double max_step) {
        this.max_step = max_step;
        return this;
    }

    public ODEConfig setScalAbsoluteTolerance(double scal_absolute_tolerance) {
        this.scal_absolute_tolerance = scal_absolute_tolerance;
        return this;
    }

    public ODEConfig setScalRelativeTolerance(double scal_relative_tolerance) {
        this.scal_relative_tolerance = scal_relative_tolerance;
        return this;
    }
    
    

    @Override
    public ODESimulator createSimulator(Setting setting) {
        ODESimulator res = new ODESimulator(
                setting, 
                new DormandPrince54Integrator(min_step, max_step, scal_absolute_tolerance, scal_relative_tolerance)
        );
        res.addOutOfBoundsListener(new OutOfBoundsEndCondition());
        return res;
    }
}
