package core.simulation.events;

/**
 *
 * @author Martin
 */
public interface OutOfBoundsNotifier {
    public void addOutOfBoundsListener(OutOfBoundsListener l);
    public boolean removeOutOfBoundsListener(OutOfBoundsListener l);
    public void clearOutOfBoundsListeners();
    
}
