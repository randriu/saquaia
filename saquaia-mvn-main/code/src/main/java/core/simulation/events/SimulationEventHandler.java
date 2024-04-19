package core.simulation.events;

import org.apache.commons.math3.ode.events.EventHandler;

/**
 *
 * @author Martin
 */
public interface SimulationEventHandler extends EventHandler{
    
    public enum Preview {
        CONTINUE,
        EVENT,
        PROHIBITED
    }
    
    public boolean init(double start_time, double[] state, int[] interval_state_hint);
    public abstract Preview preview(double time, double[] state);
    public boolean confirm(double time, double[] state, Preview preview);
    
    public default boolean justGoto(double time, double[] state) {
        Preview preview = preview(time, state);
        return confirm(time, state, preview);
    }
    
    @Override
    public default void init(double start_time, double[] state, double end_time) {
        init(start_time, state, null);
    }

    @Override
    public default Action eventOccurred(double time, double[] state, boolean rising) {
        // System.out.println("eventOccured" + Arrays.toString(state) + " at t=" + time);
        return justGoto(time, state) ? Action.CONTINUE : Action.STOP;
    }

    @Override
    public default void resetState(double time, double[] state) {}
}
