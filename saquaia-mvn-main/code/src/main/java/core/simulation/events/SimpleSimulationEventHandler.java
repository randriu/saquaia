package core.simulation.events;

import java.util.Arrays;

/**
 *
 * @author Martin
 */
public abstract class SimpleSimulationEventHandler implements SimulationEventHandler {
    private int sign = 1;
    
    private boolean debug = false;
    
    public boolean init(double start_time, double[] state, int[] interval_state_hint) {
//        System.out.println("running SimulationEventHandler init");
        sign = 1;
        return simpleInit(start_time, state, interval_state_hint);
    }

    abstract boolean simpleInit(double start_time, double[] start_state, int[] interval_state_hint);

    @Override
    public Preview preview(double time, double[] state) {
        return simplePreview(time, state);
    }
    
    abstract Preview simplePreview(double time, double[] state);

    @Override
    public boolean confirm(double time, double[] state, Preview preview) {
        if (preview == Preview.EVENT) {
            if (debug) System.out.println(this + " g: " + g(time, state));
            sign = -sign;
            if (debug) System.out.println(this + " swaped sign: " + sign);
            if (debug) System.out.println(this + " g: at " + Arrays.toString(state) + " is " + g(time, state));
        }
        return simpleConfirm(time, state, preview);
    }

    abstract boolean simpleConfirm(double time, double[] state, Preview preview);

    @Override
    public double g(double time, double[] state) {
        double val = sign * eventCountdown(time, state);
        if (debug) System.out.println("g: " + val + "   " + Arrays.toString(state) + " t=" + time);
        return val;
    }
    
    protected abstract double eventCountdown(double time, double[] state);
}
