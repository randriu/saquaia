package core.simulation.events;

/**
 *
 * @author Martin
 */
public class OutOfBoundsEndCondition implements OutOfBoundsListener{
    @Override
    public boolean notify(double time, double[] state) {
        return false;
    }
}
