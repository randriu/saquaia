package core.simulation.events;

/**
 *
 * @author Martin
 */
public class IntervalStateChangeEndCondition implements IntervalStateChangeListener{
    @Override
    public boolean notify(double time, double[] state, int[] previous_interv_state, int[] new_interv_state, int[] interv_state_offset) {
        return false;
    }
}
