package core.simulation.simulators;

import core.model.CRN;
import core.model.Setting;
import java.util.ArrayList;
import java.util.Collection;
import core.simulation.events.IntervalStateChangeHandler;
import core.simulation.events.IntervalStateChangeListener;
import core.simulation.events.IntervalStateChangeNotifier;
import core.simulation.events.OutOfBoundsHandler;
import core.simulation.events.OutOfBoundsListener;
import core.simulation.events.OutOfBoundsNotifier;
import core.simulation.events.SimulationEventHandler;
import core.simulation.events.SimulationEventHandler.Preview;

/**
 *
 * @author Martin
 */
public abstract class AbstractSimulator implements Simulator, OutOfBoundsNotifier, IntervalStateChangeNotifier{
    public final Setting setting;
    public final CRN crn;
    public final int dim;
    public boolean[] active;
    protected final ArrayList<SimulationEventHandler> eventHandlers = new ArrayList<>();
    private final OutOfBoundsHandler outOfBoundsHandler;
    private final IntervalStateChangeHandler intervalStateChangeHandler;
    
    private Preview[] previews;
    
    public AbstractSimulator(Setting setting) {
        this.setting = setting;
        this.crn = setting.crn;
        this.dim = setting.dim();
        this.active = new boolean[setting.crn.reactions.length];
        for (int r_i = 0; r_i < setting.crn.reactions.length; r_i++) this.active[r_i] = true;
        outOfBoundsHandler = new OutOfBoundsHandler(setting);
        intervalStateChangeHandler = new IntervalStateChangeHandler(setting);
    }
    
    public void setAllActive() {
        for (int r_i = 0; r_i < setting.crn.reactions.length; r_i++) this.active[r_i] = true;
    }
    
    public double[] activePropensities(double[] state) {
        double[] propensities = new double[setting.crn.reactions.length];
        for (int r_i = 0; r_i < setting.crn.reactions.length; r_i++) {
            if (active[r_i]) propensities[r_i] = setting.crn.reactions[r_i].propensity(state);
        }
        return propensities;
    }
    
    public double activePropensities(double[] state, double[] propensities) {
        double propensities_sum = 0;
        for (int r_i = 0; r_i < setting.crn.reactions.length; r_i++) {
            if (active[r_i]) {
                propensities[r_i] = setting.crn.reactions[r_i].propensity(state);
                propensities_sum += propensities[r_i];
            } else propensities[r_i] = 0;
        }
        return propensities_sum;
    }
    
    public double activePropensitiesWithPrecomputedValues(double[] propensities, double[] precomputed) {
        double propensities_sum = 0;
        for (int r_i = 0; r_i < setting.crn.reactions.length; r_i++) {
            if (active[r_i]) {
                propensities[r_i] = precomputed[r_i];
                propensities_sum += propensities[r_i];
            } else propensities[r_i] = 0;
        }
        return propensities_sum;
    }

    @Override
    public ArrayList<SimulationEventHandler> getSimulationEventHandlers() {
        return eventHandlers;
    }
    
    public boolean initSimulationEventHandlers(double state_time, double[] state, int[] interval_state_hint) {
        boolean can_start = true;
        for (SimulationEventHandler h : eventHandlers) can_start = can_start && h.init(state_time, state, interval_state_hint);
        return can_start;
    }
    
    public Preview preview(double time, double[] state) {
        if (previews == null || previews.length != eventHandlers.size()) {
            previews = new Preview[eventHandlers.size()];
        }
        Preview preview = Preview.CONTINUE;
        for (int h_i = 0; h_i < eventHandlers.size(); h_i++) {
            previews[h_i] = eventHandlers.get(h_i).preview(time, state);
            if (preview == Preview.CONTINUE) preview = previews[h_i];
            else if (previews[h_i] == Preview.PROHIBITED) {
                preview = Preview.PROHIBITED;
                break;
            }
        }
        return preview;
    }
    
    public boolean confirm(double time, double[] state, Preview preview) {
        boolean cont = true;
        for (int h_i = 0; h_i < eventHandlers.size(); h_i++) {
            boolean cont_i = eventHandlers.get(h_i).confirm(time, state, previews[h_i]);
            cont = cont && cont_i;
        }
        return cont;
    }
    
    public boolean justGoto(double time, double[] state) {
        Preview preview = preview(time, state);
        return confirm(time, state, preview);
    }
    

    @Override
    public void addSimulationEventHandler(SimulationEventHandler h) {
        if (!eventHandlers.contains(h)) eventHandlers.add(h);
    }

    @Override
    public void addSimulationEventHandlers(Collection<SimulationEventHandler> hs) {
        for (SimulationEventHandler h : hs) addSimulationEventHandler(h);
    }

    @Override
    public boolean removeSimulationEventHandler(SimulationEventHandler h) {
        return eventHandlers.remove(h);
    }

    @Override
    public void clearSimulationEventHandlers() {
        outOfBoundsHandler.clearOutOfBoundsListeners();
        intervalStateChangeHandler.clearIntervalStateChangeListeners();
        eventHandlers.clear();
    }

    @Override
    public void addIntervalStateChangeListener(IntervalStateChangeListener l) {
        intervalStateChangeHandler.addIntervalStateChangeListener(l);
        addSimulationEventHandler(intervalStateChangeHandler);
    }

    @Override
    public boolean removeIntervalStateChangeListener(IntervalStateChangeListener l) {
        return intervalStateChangeHandler.removeIntervalStateChangeListener(l);
    }

    @Override
    public void clearIntervalStateChangeListeners() {
        intervalStateChangeHandler.clearIntervalStateChangeListeners();
        eventHandlers.remove(intervalStateChangeHandler);
    }

    @Override
    public void addOutOfBoundsListener(OutOfBoundsListener l) {
        outOfBoundsHandler.addOutOfBoundsListener(l);
        addSimulationEventHandler(outOfBoundsHandler);
    }

    @Override
    public boolean removeOutOfBoundsListener(OutOfBoundsListener l) {
        return outOfBoundsHandler.removeOutOfBoundsListener(l);
    }

    @Override
    public void clearOutOfBoundsListeners() {
        outOfBoundsHandler.clearOutOfBoundsListeners();
        eventHandlers.remove(outOfBoundsHandler);
    }
}
