package benchmarking.simulatorconfiguration;

import core.model.Setting;
import core.simulation.events.OutOfBoundsEndCondition;
import core.simulation.simulators.TauSimulator;

/**
 *
 * @author Martin
 */
public class TAUConfig extends SimulatorConfig{
    
    public double epsilon = 0.03;
    
    public TAUConfig(){
        super(Type.TAU);
    }
    
    public TAUConfig setEpsilon(double x) {
        this.epsilon = x;
        return this;
    }

    public double getEpsilon() {
        return epsilon;
    }

    @Override
    public TauSimulator createSimulator(Setting setting) {
        TauSimulator res = new TauSimulator(setting, epsilon);
        res.addOutOfBoundsListener(new OutOfBoundsEndCondition());
        return res;
    }
}
