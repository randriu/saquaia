package core.simulation;

import core.util.Pair;
import core.util.Vector;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

/**
 *
 * @author Martin
 */
public class Simulation {
    public String name = null;
    public final double[] start_state;
    
    // current state
    private double time = 0.0;
    private double prev_time = 0.0;
    private double[] state;
    private double[] prev_state;
    private double nr_of_reactions = 0;
    private int nr_of_steps = 0;
    
    // random (for seed)
    private transient Random rand = null;
    private Long seed = null;
    
    // history
    private boolean keepHistory = false;
    private int maxHistoryLength = 10000;
//    private int maxHistoryLength = 1000000;
    private ArrayList<Pair<double[],Double>> history = null;
    
    // listeners
    private transient HashSet<StepListener> stepListeners = null;
    private transient HashSet<HistoryListener> historyListeners = null;
    
    public Simulation(double[] start_state) {
        this.state = Vector.copy(start_state);
        this.prev_state = Vector.copy(start_state);
        this.start_state = start_state;
    }
    
    public double getTime(){
        return this.time;
    }
    
    public double getPrevTime(){
        return this.time;
    }
    
    public double[] getStartState(){
        return this.start_state;
    }
    
    public double[] getState(){
        return Vector.copy(this.state);
    }
    
    public double[] getPrevState(){
        return Vector.copy(this.prev_state);
    }
    
    public int dim(){
        return this.state.length;
    }
    
    public void evolve(double[] new_state, double new_time){
        evolve(new_state, new_time, 1);
    }
    
    public void evolve(double[] new_state, double new_time, double nr_of_reactions){
        if (new_time < time) throw new IllegalArgumentException("Simulation: Time can only increase.");
        
        if (nr_of_steps == 0) notifyStepListeners();
        
        for (int dim_i = 0; dim_i < state.length; dim_i++) {
            this.prev_state[dim_i] = state[dim_i];
            this.state[dim_i] = new_state[dim_i];
        }
        this.prev_time = time;
        this.time = new_time;
        this.nr_of_steps++;
        this.nr_of_reactions += nr_of_reactions;
        
        recordHistory();
        notifyStepListeners();
    }
    
    public boolean needIntermidiateSteps() {
        return keepHistory || stepListeners != null && !stepListeners.isEmpty();
    }
    
    public long getNrOfSteps() {
        return this.nr_of_steps;
    }
    
    public double getNrOfReactions() {
        return this.nr_of_reactions;
    }
    
    // -----------------------------
    // RANDOM
    // -----------------------------
    
    public Random getRandom() {
        if (this.rand == null) this.rand = new Random();
        return this.rand;
    }
    
    public void setSeed(long seed) {
        this.seed = seed;
        this.rand = new Random(seed);
    }
    
    // -----------------------------
    // HISTORY
    // -----------------------------
    
    public boolean getKeepHistory() {
        return keepHistory;
    }
    
    public void setKeepHistory(boolean b) {
        this.keepHistory =  b;
    }
    
    public int getMaxHistoryLength() {
        return this.maxHistoryLength;
    }
    
    public void setMaxHistoryLength(int l) {
        if (l < 0) l = 0;
        this.maxHistoryLength = l;
    }

    public ArrayList<Pair<double[],Double>> getHistory(){
        if (this.history == null) return new ArrayList();
        return new ArrayList(this.history);
    }
    
    public void recordHistory() {
        if (! keepHistory) return;
        if (history == null) {
            this.history = new ArrayList<>();
            this.history.add(new Pair<>(Vector.copy(this.start_state), 0.0));
        }
        Pair<double[],Double> last = history.get(history.size()-1);
        if (last.right == this.time && last.left.equals(this.state)) return;
        
        // history too long -> shorten
        if (history.size() >= maxHistoryLength) {
            // notify history listeners
            if (this.historyListeners != null) {
                for (HistoryListener l : this.historyListeners) l.onHistoryWillBeTrimmed();
            }
            
            // reduce history length
            double delta_t = this.time / (maxHistoryLength / 2);
            ArrayList<Pair<double[],Double>> new_history = new ArrayList<>();
            Double last_taken = null;
            for (Pair<double[], Double> pair : this.history) {
                if (last_taken == null || pair.right >= last_taken + delta_t) {
                    new_history.add(pair);
                    last_taken = pair.right;
                }
                else {
                    // notify history listeners
                    if (this.historyListeners != null) {
                        for (HistoryListener l : this.historyListeners) l.onStepTrimmed(pair);
                    }
                }
            }
            this.history = new_history;
            
            // notify history listeners
            
            if (this.historyListeners != null) {
                for (HistoryListener l : this.historyListeners) l.onHistoryWasTrimmed();
            }
        }
        Pair<double[],Double> new_pair = new Pair(Vector.copy(this.state), this.time);
        this.history.add(new_pair);
        
        // notify history listeners
        if (this.historyListeners != null) {
            for (HistoryListener l : this.historyListeners) l.onStepRecorded(new_pair);
        }
    }
    
    // -----------------------------
    // LISTENERS
    // -----------------------------
    
    public interface StepListener {
        void onStep(double[] state, double time);
    }
    
    public void addStepListener(StepListener l) {
        if (this.stepListeners == null) this.stepListeners = new HashSet<>();
        this.stepListeners.add(l);
    }
    
    public boolean removeStepListener(StepListener l) {
        if (this.stepListeners == null) return false;
        return this.stepListeners.remove(l);
    }
    
    public void clearStepListeners() {
        this.stepListeners = null;
    }
    
    private void notifyStepListeners() {
        if (this.stepListeners != null) {
            for (StepListener l : this.stepListeners) l.onStep(Vector.copy(this.state), this.time);
        }
    }
    
    public interface HistoryListener {
        void onStepRecorded(Pair<double[],Double> step);
        void onStepTrimmed(Pair<double[],Double> step);
        void onHistoryWillBeTrimmed();
        void onHistoryWasTrimmed();
    }
    
    public void addHistoryTrimListener(HistoryListener l) {
        if (this.historyListeners == null) this.historyListeners = new HashSet<>();
        this.historyListeners.add(l);
    }
    
    public boolean removeHistoryTrimListener(HistoryListener l) {
        if (this.historyListeners == null) return false;
        return this.historyListeners.remove(l);
    }
    
    public void clearHistoryListeners() {
        this.historyListeners = null;
    }
}
