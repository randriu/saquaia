package core.simulation.simulators.segmental;

import core.util.Pair;
import java.util.ArrayList;

/**
 *
 * @author Martin
 */
public interface Segmentlike {
    public double[] getDeltaState();
    public double getDeltaTime();
    public double getReactions();
    public ArrayList<Pair<double[],Double>> getSteps();
    public long getMem();
    
    public void applyDeltaStateTo(double[] state);
}
