package core.simulation.simulators;

import core.util.Progressable;
import java.util.Collection;
import core.simulation.Simulation;
import core.simulation.events.SimulationEventHandler;
import core.util.Pair;
import java.util.ArrayList;

/**
 *
 * @author Martin
 */
public interface Simulator {
    public default boolean isDeterministic() {
        return false;
    };
    public default boolean dependsOnC() {
        return true;
    };
    public default boolean speedsUp() {
        return false;
    };
    public default Object getMemory() {
        return null;
    };
    public default ArrayList<Pair<String,Number>> getStatistics() {
        return new ArrayList<>();
    };
    
    public default void simulate(Simulation sim, double end_t, Progressable p) {
        simulate(sim, end_t, p, null);
    }
    public void simulate(Simulation sim, double end_t, Progressable p, int[] interval_state_hint);
    
    public void addSimulationEventHandler(SimulationEventHandler h);
    public void addSimulationEventHandlers(Collection<SimulationEventHandler> hs);
    public boolean removeSimulationEventHandler(SimulationEventHandler h);
    public void clearSimulationEventHandlers();
    public Collection<SimulationEventHandler> getSimulationEventHandlers();
}
