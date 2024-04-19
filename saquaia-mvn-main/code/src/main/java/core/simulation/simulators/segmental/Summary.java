package core.simulation.simulators.segmental;

import core.util.IO;
import core.util.Pair;
import core.util.Vector;
import java.util.ArrayList;
import java.util.Arrays;
import org.openjdk.jol.info.GraphLayout;
import core.simulation.Simulation;

/**
 *
 * @author Martin
 */
public class Summary implements Segmentlike{
    
    private static long MEM_BASE = -1;
    private static long MEM_PER_DIM = -1;
    
    public final double[] delta_state;
    public final double delta_time;
    public final double reactions;
    
    public Summary(double[] delta_state, double delta_time, double reactions){
        this.delta_state = Vector.copy(delta_state);
        this.delta_time = delta_time;
        this.reactions = reactions;
    }
    
    public Summary(Simulation sim){
        delta_state = sim.getState();
        for (int dim_i = 0; dim_i < delta_state.length; dim_i++) delta_state[dim_i] -= sim.start_state[dim_i];
        delta_time = sim.getTime();
        reactions = sim.getNrOfReactions();
    }

    @Override
    public double[] getDeltaState() {
        return delta_state;
    }

    @Override
    public double getDeltaTime() {
        return delta_time;
    }

    @Override
    public ArrayList<Pair<double[], Double>> getSteps() {
        return null;
    }
    
    @Override
    public String toString() {
        return IO.significantFigures(delta_time,2) + ": " + Arrays.toString(delta_state);
    }

    @Override
    public long getMem() {
        if (MEM_BASE == -1) {
            Summary s0 = new Summary(new double[]{}, 0.0, 0);
            Summary s1 = new Summary(new double[]{1.0}, 0.0, 0);
            MEM_BASE = GraphLayout.parseInstance(s0).totalSize();
            MEM_PER_DIM = GraphLayout.parseInstance(s1).totalSize() - MEM_BASE;
            
//            Summary test = new Summary(new double[10], delta_time, reactions);
//            System.out.println("should be " + test.getMem());
//            System.out.println("is " + GraphLayout.parseInstance(test).totalSize());
//            System.out.println();
//            System.out.println(GraphLayout.parseInstance(test).toPrintable());
//            System.out.println();
//            System.out.println(ClassLayout.parseClass(Summary.class).toPrintable());
//            System.out.println();
//            
//            System.out.println(VM.current().details());
        }
        
        return MEM_BASE + delta_state.length * MEM_PER_DIM;
    }
    
    public static void main(String[] args) {
        Summary s = new Summary(new double[]{1.0, 2.0, 3.0, 4.0, 5.0}, 0, 0);
        System.out.println(s.getMem());
        System.out.println(GraphLayout.parseInstance(s).totalSize());
    }

    @Override
    public void applyDeltaStateTo(double[] state) {
        Vector.plus(state, delta_state);
    }

    @Override
    public double getReactions() {
        return reactions;
    }
}
