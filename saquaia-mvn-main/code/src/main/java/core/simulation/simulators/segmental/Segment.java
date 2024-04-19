package core.simulation.simulators.segmental;

import core.util.IO;
import core.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import org.openjdk.jol.info.GraphLayout;
import core.simulation.Simulation;

/**
 *
 * @author Martin
 */
public class Segment extends Summary{
    
    public ArrayList<Pair<double[],Double>> steps = new ArrayList<>(0);
    
    public Segment(double[] delta_state, double delta_time, double reactions, ArrayList<Pair<double[],Double>> steps) {
        super(delta_state, delta_time, reactions);
        this.steps = new ArrayList<>(steps);
        this.steps.trimToSize();
    }
    
    public Segment(Simulation sim) {
        super(sim);
        this.steps = new ArrayList<>(sim.getHistory());
        this.steps.trimToSize();
    }

    @Override
    public ArrayList<Pair<double[], Double>> getSteps() {
        return steps;
    }
    
    @Override
    public long getMem() {
        return GraphLayout.parseInstance(this).totalSize();
    }
    
    @Override
    public String toString() {
        return IO.significantFigures(delta_time,2) + ": " + Arrays.toString(delta_state);
    }
    
    public static void main(String[] args) {
        int count = 100000;
        Simulation sim;
        ArrayList<Pair<double[], Double>> s;
        
        for (int dim = 0; dim < 4; dim++) {
            long last = 0;
            System.out.println(dim + ":");
            sim = new Simulation(new double[dim]);
            sim.setKeepHistory(true);
           
            for (int i = 0; i < count; i++) {
                sim.evolve(new double[dim], 0.1);
//                s = sim.getHistory();
//                s.trimToSize();
//                long next = GraphLayout.parseInstance(s).totalSize();
//                Summary sum = new Summary(sim);
//                Segment seg = new Segment(sim);
////                System.out.println(next + " (" + (next - last) + ") " + (seg.getMem() - sum.getMem()));
//                last = next;
            }
            System.out.println("");
            
            s = sim.getHistory();
            s.trimToSize();
            int repeat = 1000000;
            long start = System.nanoTime();
            for (int i = 0; i < repeat; i++) {
                GraphLayout.parseInstance(s).totalSize();
            }
            System.out.println("time per MEM calc: " + ((System.nanoTime() - start) / repeat));
            System.out.println("");
            System.out.println("");
        
        }
    }
}
