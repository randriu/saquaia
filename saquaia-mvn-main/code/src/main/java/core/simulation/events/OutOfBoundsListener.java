package core.simulation.events;

/**
 *
 * @author Martin
 */
public interface OutOfBoundsListener {
    public default void init(double time, double[] state) {};
    public boolean notify(double time, double[] state);
}
