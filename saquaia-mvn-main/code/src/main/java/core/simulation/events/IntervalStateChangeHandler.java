package core.simulation.events;

import core.model.Setting;
import core.util.Vector;
import java.util.ArrayList;

/**
 *
 * @author Martin
 */
public class IntervalStateChangeHandler extends SimpleSimulationEventHandler implements IntervalStateChangeNotifier{
    
    public final Setting setting;
    public final int dim;
    
    public double OVERSHOT = 1E-3;
    
    private int[] interval_state = null;
    private int[] previous_interval_state = null;
    private int[] interv_state_offset = null;
    
    ArrayList<IntervalStateChangeListener> intervalStateChangeListeners = new ArrayList<>();
    
    public IntervalStateChangeHandler(Setting setting) {
        this.setting = setting;
        this.dim = setting.dim();
    }

    @Override
    boolean simpleInit(double start_time, double[] start_state, int[] interval_state_hint) {
//        System.out.println("running IntervalStateEventHandler init");
        
        if (interval_state_hint != null) {
            interval_state = Vector.copy(interval_state_hint);
            interval_state = setting.updateIntervalStateForState(start_state, interval_state);
            if (this.interval_state == null) return false;
        }
        if (this.interval_state == null || !setting.isStateInIntervalState(start_state, interval_state)) {
            this.interval_state = setting.intervalStateForState(start_state);
            if (this.interval_state == null) return false;
        }
//        System.out.println("--> cur_interval_state: " + Arrays.toString(cur_interval_state));
        if (this.previous_interval_state ==  null || this.previous_interval_state.length != dim) {
            this.previous_interval_state = new int[dim];
        }
        if (this.interv_state_offset ==  null || this.interv_state_offset.length != dim) {
            this.interv_state_offset = new int[dim];
        }
        for (IntervalStateChangeListener l : intervalStateChangeListeners) l.init(start_time, start_state, interval_state);

        return true;
    }

    @Override
    public Preview simplePreview(double time, double[] state) {
        return setting.isStateInIntervalState(state, interval_state) ? Preview.CONTINUE : Preview.EVENT;
    }

    @Override
    boolean simpleConfirm(double time, double[] state, Preview preview) {
//        System.out.println("simpleConfirm " + Arrays.toString(state) + " " + time);
        if (preview == Preview.CONTINUE) return true;
        
        boolean interval_state_changed = false;
        for (int dim_i = 0; dim_i < dim; dim_i++) {
            previous_interval_state[dim_i] = interval_state[dim_i];
            interv_state_offset[dim_i] = setting.intervals[dim_i][interval_state[dim_i]].compareTo(state[dim_i]);
            if (interv_state_offset[dim_i] == 0) continue;
            interval_state[dim_i] += interv_state_offset[dim_i];
            
            if (interval_state[dim_i] >= 0
                    && interval_state[dim_i] < setting.intervals[dim_i].length
                    && !setting.intervals[dim_i][interval_state[dim_i]].contains(state[dim_i])) {
//                System.out.println("CARE: binary search in dimension " + dim_i + ": expected " + state[dim_i] + " in interval "  + setting.intervals[dim_i][interval_state[dim_i]]);
                interval_state[dim_i] = setting.intervalIndexFor(dim_i, state[dim_i]);
                if (Math.abs(previous_interval_state[dim_i] - interval_state[dim_i]) > 1) {
//                    System.out.println("CARE: interval skipped in dimension " + dim_i + ": " + previous_interval_state[dim_i] + " -> " + interval_state[dim_i]);
                }
            }
            
            if (interval_state[dim_i] != previous_interval_state[dim_i]) interval_state_changed = true;
            
            if (interval_state[dim_i] < 0 || interval_state[dim_i] >= setting.intervals[dim_i].length) {
                interval_state = null;
                interval_state_changed = true;
                break;
            }
        }

        if (!interval_state_changed) {
            System.out.println("CARE: IntervalChangeEvent did not change interval state!");
        }
        
        boolean cont = interval_state != null;
        for (IntervalStateChangeListener l : intervalStateChangeListeners) {
            cont = cont && l.notify(time, state, previous_interval_state, interval_state, interv_state_offset);
        }
        return cont;
    }

    @Override
    protected double eventCountdown(double time, double[] state) {
        double distances_min = Double.MAX_VALUE;
        for (int dim_i = 0; dim_i < dim; dim_i++) {
            double distance_i = setting.intervals[dim_i][interval_state[dim_i]].distanceOf(state[dim_i]);
            distances_min = Math.min(distances_min, distance_i);
        }
        return distances_min + OVERSHOT;
    }

    @Override
    public void addIntervalStateChangeListener(IntervalStateChangeListener l) {
        if (!intervalStateChangeListeners.contains(l)) intervalStateChangeListeners.add(l);
    }

    @Override
    public boolean removeIntervalStateChangeListener(IntervalStateChangeListener l) {
        return intervalStateChangeListeners.remove(l);
    }

    @Override
    public void clearIntervalStateChangeListeners() {
        intervalStateChangeListeners.clear();
    }
    
    public boolean active() {
        return !intervalStateChangeListeners.isEmpty();
    }
    
}
