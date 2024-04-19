package benchmarking.simulatorconfiguration;

import core.model.Setting;
import core.simulation.events.OutOfBoundsEndCondition;
import core.simulation.simulators.SSASimulator;

/**
 *
 * @author Martin
 */
public class SSAConfig extends SimulatorConfig{
    
    public SSAConfig(){
        super(Type.SSA);
    }

    @Override
    public SSASimulator createSimulator(Setting setting) {
        SSASimulator res = new SSASimulator(setting);
        res.addOutOfBoundsListener(new OutOfBoundsEndCondition());
        return res;
    }
}
