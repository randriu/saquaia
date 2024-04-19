/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.simulation.events;

import core.model.Interval;
import core.model.Setting;
import java.util.ArrayList;

/**
 *
 * @author Martin
 */
public class OutOfBoundsHandler extends SimpleSimulationEventHandler implements OutOfBoundsNotifier{
    public double NEGATIVE_LEEWAY = 1E-3;
    
    public final Setting setting;
    public final int dim;
    
    ArrayList<OutOfBoundsListener> outOfBoundListeners = new ArrayList<>();
    
    public OutOfBoundsHandler(Setting setting) {
        this.setting = setting;
        this.dim = setting.dim();
    }
    
    @Override
    boolean simpleInit(double start_time, double[] start_state, int[] interval_state_hint) {
        for (OutOfBoundsListener l : outOfBoundListeners) l.init(start_time, start_state);
        
        return preview(start_time, start_state) == Preview.CONTINUE;
    }

    @Override
    public Preview simplePreview(double time, double[] state) {
        boolean larger_than_some_bound = false;
        for (int dim_i = 0; dim_i < dim; dim_i++) {
            if (state[dim_i] < 0) return Preview.PROHIBITED;
            if (!larger_than_some_bound && setting.bounds[dim_i] + 1 <= state[dim_i]) larger_than_some_bound = true;
        }
        return larger_than_some_bound ? Preview.EVENT : Preview.CONTINUE;
    }

    @Override
    boolean simpleConfirm(double time, double[] state, Preview preview) {
        if (preview == Preview.CONTINUE) return true;
        boolean cont = preview != Preview.PROHIBITED;
        for (OutOfBoundsListener l : outOfBoundListeners) cont = cont && l.notify(time, state);
        return cont;
    }

    @Override
    protected double eventCountdown(double time, double[] state) {
        double distance = Double.MAX_VALUE;
        for (int dim_i = 0; dim_i < dim; dim_i++) {
            distance = Math.min(distance, Interval.distanceOf(0, setting.bounds[dim_i], state[dim_i], NEGATIVE_LEEWAY));
        }
        return distance;
    }

    @Override
    public void addOutOfBoundsListener(OutOfBoundsListener l) {
        if (!outOfBoundListeners.contains(l)) outOfBoundListeners.add(l);
    }

    @Override
    public boolean removeOutOfBoundsListener(OutOfBoundsListener l) {
        return outOfBoundListeners.remove(l);
    }

    @Override
    public void clearOutOfBoundsListeners() {
        outOfBoundListeners.clear();
    }
    
    public boolean active() {
        return !outOfBoundListeners.isEmpty();
    }
    
}
