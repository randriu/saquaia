package core.simulation.events;

/**
 *
 * @author Martin
 */
public interface IntervalStateChangeNotifier {
    public void addIntervalStateChangeListener(IntervalStateChangeListener l);
    public boolean removeIntervalStateChangeListener(IntervalStateChangeListener l);
    public void clearIntervalStateChangeListeners();
}
