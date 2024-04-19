package core.simulation.events;

/**
 *
 * @author Martin
 */
public interface IntervalStateChangeListener {
    public default void init(double time, double[] state, int[] interv_state) {};
    public boolean notify(double time, double[] state, int[] previous_interv_state, int[] new_interv_state, int[] interv_state_offset);
}
