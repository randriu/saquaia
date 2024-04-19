package core.model;

import com.google.gson.Gson;
import core.util.Examples;
import core.util.JSON;
import core.util.PopulationLevelHelper;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.function.Function;
import core.util.Vector;
import java.util.Set;
import core.simulation.Simulatable;
import core.simulation.Simulation;


public class Setting implements Simulatable {
    public String name;
    public int[] initial_state;
    public int[] bounds;
    public double population_level_growth_factor = 1.5;
    public Set<Integer>[] extraLevels;
    public CRN crn;
    public Interval[][] intervals;
    public double end_time;

    public Setting(String name, CRN crn, int[] initial_state, int[] bounds, Set<Integer>[] extraLevels, double population_level_growth_factor, double end_time) {
        this.name = name;
        this.crn = crn;
        this.initial_state = initial_state;
        this.bounds = bounds;
        
        this.extraLevels = extraLevels;
        this.population_level_growth_factor = population_level_growth_factor;
        this.end_time = end_time;
        recomputeIntervals(false);
    }

    public Setting(){
        this(1);
    }

    public Setting(int dimension){
        name = "unnamed setting";
        this.crn = new CRN(dimension);
        initial_state = new int[dimension];
        bounds = new int[dimension];
        for (int i = 0; i < dimension; i++){
            bounds[i] = 100000;
        }
        end_time = 100.0;
        recomputeIntervals(false);
    }
    
    public static Setting product(Setting s1, Setting s2) {
        int d1 = s1.dim();
        int d2 = s2.dim();
        int d = d1 + d2;
        
        CRN crn = CRN.product(s1.crn, s2.crn);
        
        int[] initial = new int[d];
        System.arraycopy(s1.initial_state, 0, initial, 0, d1);
        System.arraycopy(s2.initial_state, 0, initial, d1, d2);
        
        int[] bounds = new int[d];
        System.arraycopy(s1.bounds, 0, bounds, 0, d1);
        System.arraycopy(s2.bounds, 0, bounds, d1, d2);
        
        TreeSet<Integer>[] extraLevels = new TreeSet[d];
        if (s1.extraLevels != null) System.arraycopy(s1.extraLevels, 0, extraLevels, 0, d1);
        if (s1.extraLevels != null) System.arraycopy(s2.extraLevels, 0, extraLevels, d1, d2);
        if (s1.extraLevels == null && s2.extraLevels == null) extraLevels = null;
        
        return new Setting(
                s1.name + " x " + s2.name, 
                crn, 
                initial, 
                bounds, 
                extraLevels, 
                s1.population_level_growth_factor, 
                Math.max(s1.end_time, s2.end_time)
        );
    }
    
    public void changeDim(int new_dim) {
        int old_dim = dim();
        crn.changeDim(new_dim);
        initial_state = Arrays.copyOf(initial_state, new_dim);
        bounds = Arrays.copyOf(bounds, new_dim);
        for (int dim_i = old_dim; dim_i < new_dim; dim_i++) bounds[dim_i] = 10000;
        if (this.extraLevels != null) extraLevels = Arrays.copyOf(extraLevels, new_dim);
        recomputeIntervals(false);
    }
    
    public void deleteDim(int dim_to_del) {
        this.initial_state = Vector.removeDim(initial_state, dim_to_del);
        this.bounds = Vector.removeDim(bounds, dim_to_del);
        if (this.extraLevels != null) this.extraLevels = Vector.removeDim(extraLevels, dim_to_del);
        this.crn.deleteDim(dim_to_del);
        recomputeIntervals(false);
    }
    
    public void switchDims(int dim1, int dim2) {
        Vector.switchDims(initial_state, dim1, dim2);
        Vector.switchDims(bounds, dim1, dim2);
        if (this.extraLevels != null) Vector.switchDims(extraLevels, dim1, dim2);
        this.crn.switchDims(dim1, dim2);
        recomputeIntervals(false);
    }
    
    public void addDim(String species_name) {
        changeDim(dim()+1);
        this.crn.speciesNames[this.crn.speciesNames.length-1] = species_name;
    }
    
    public void moveDim(int from, int to) {
        Vector.moveDim(initial_state, from, to);
        Vector.moveDim(bounds, from, to);
        if (this.extraLevels != null) Vector.moveDim(extraLevels, from, to);
        this.crn.moveDim(from, to);
        recomputeIntervals(false);
    }
    
    public void changePopulationLevelGrowthFactor(double c) {
        this.population_level_growth_factor = c;
        recomputeIntervals(false);
    }

    @Override
    public String toString() {
        String r = "";
        r += "Name:\n";
        r += name + "\n";
        r += "CRN:\n";
        r += crn + "\n";
        r += "Initial state:\n";
        r += Arrays.toString(initial_state) + "\n";
        r += "Bound:\n";
        r += Arrays.toString(bounds) + "\n";
        r += "Splitters:\n";
        r += Arrays.toString(extraLevels) + "\n";
        r += "Intervals:\n";
        for (int i = 0; i < crn.dim(); i++) {
            r += Arrays.toString(intervals[i]) + "\n";
        }
        return r;
    }

    public String toJson() {
        return JSON.getGson().toJson(this);
    }
    
    public static Setting fromJson(String s) {
        return JSON.getGson().fromJson(s, Setting.class);
    }

    public double getNrOfIntervalStates() {
        double res = 1;
        int dim = dim();
        for (int dim_i = 0; dim_i < dim; dim_i++) res *= intervals[dim_i].length;
        return res;
    }
    
    public int intervalIndexFor(int dim_i, double state) {
//        System.out.println("slow!! " + dim_i);
        if (state < 0) return -1;
        if (state >= bounds[dim_i]+1) return intervals[dim_i].length;
        // binary search
        int l = 0, r = intervals[dim_i].length - 1;
        while (l <= r) {
            int mid = l + (r - l) / 2;
            int compared = intervals[dim_i][mid].compareTo(state);
            // Check if value is present at mid
            if (compared == 0) {
                return mid;
            }
            // If value greater, ignore left half
            if (compared > 0)
                l = mid + 1;
            // If value is smaller, ignore right half
            else
                r = mid - 1;
        }
        System.out.println("finding in dim " + dim_i + " state " + state + " failed!");
        throw new IllegalStateException("Bianry search for interval state failed!");
    }
    
    public int intervalIndexFor(int dim_i, double state, int previous_index) {
        if (intervals[dim_i][previous_index].contains(state)) return previous_index;
        if (previous_index > 0 && intervals[dim_i][previous_index-1].contains(state)) return previous_index - 1;
        if (previous_index < intervals[dim_i].length - 1 && intervals[dim_i][previous_index+1].contains(state)) return previous_index + 1;
        return intervalIndexFor(dim_i, state);
    }
    
    public int intervalIndexFor(int dim_i, int state) {
//        System.out.println("slow!! " + dim_i);
        if (state < 0) return -1;
        if (state >= bounds[dim_i]+1) return intervals[dim_i].length;
        // binary search
        int l = 0, r = intervals[dim_i].length - 1;
        while (l <= r) {
            int mid = l + (r - l) / 2;
            int compared = intervals[dim_i][mid].compareTo(state);
            // Check if value is present at mid
            if (compared == 0) {
                return mid;
            }
            // If value greater, ignore left half
            if (compared > 0)
                l = mid + 1;
            // If value is smaller, ignore right half
            else
                r = mid - 1;
        }
        throw new IllegalStateException("Bianry search for interval state failed!");
    }
    
    public int intervalIndexFor(int dim_i, int state, int previous_index) {
        if (intervals[dim_i][previous_index].contains(state)) return previous_index;
        if (previous_index > 0 && intervals[dim_i][previous_index-1].contains(state)) return previous_index - 1;
        if (previous_index < intervals[dim_i].length - 1 && intervals[dim_i][previous_index+1].contains(state)) return previous_index + 1;
        return intervalIndexFor(dim_i, state);
    }
    
    public int[] intervalStateForState(int[] state) {
        int[] res = new int[dim()];
        int dim = dim();
        for (int dim_i = 0; dim_i < dim; dim_i++) {
            res[dim_i] = intervalIndexFor(dim_i, state[dim_i]);
            if (res[dim_i] < 0) return null;
            if (res[dim_i] >= intervals[dim_i].length) return null;
        }
        return res;
    }
    
    public int[] intervalStateForState(double[] state) {
        int[] res = new int[dim()];
        int dim = dim();
        for (int dim_i = 0; dim_i < dim; dim_i++) {
            res[dim_i] = intervalIndexFor(dim_i, state[dim_i]);
            if (res[dim_i] < 0) return null;
            if (res[dim_i] >= intervals[dim_i].length) return null;
        }
        return res;
    }
    
    public int[] updateIntervalStateForState(int[] state, int[] interval_state) {
        if (interval_state == null) return intervalStateForState(state);
        int dim = dim();
        for (int dim_i = 0; dim_i < dim; dim_i++) {
            if (!intervals[dim_i][interval_state[dim_i]].contains(state[dim_i])) interval_state[dim_i] = intervalIndexFor(dim_i, state[dim_i], interval_state[dim_i]);
            if (interval_state[dim_i] < 0) return null;
            if (interval_state[dim_i] >= intervals[dim_i].length) return null;
        }
        return interval_state;
    }
    
    public int[] updateIntervalStateForState(double[] state, int[] interval_state) {
        if (interval_state == null) return intervalStateForState(state);
        int dim = dim();
        for (int dim_i = 0; dim_i < dim; dim_i++) {
            if (!intervals[dim_i][interval_state[dim_i]].contains(state[dim_i])) interval_state[dim_i] = intervalIndexFor(dim_i, state[dim_i], interval_state[dim_i]);
            if (interval_state[dim_i] < 0) return null;
            if (interval_state[dim_i] >= intervals[dim_i].length) return null;
        }
        return interval_state;
    }
    
    public int[] representativeForIntervalState(int[] interval_state) {
        int dim = dim();
        int[] res = new int[dim];
        Interval[] intervals = intervalsForIntervalState(interval_state);
        for (int dim_i = 0; dim_i < dim; dim_i++) {
            res[dim_i] = intervals[dim_i].rep;
        }
        return res; 
    }
    
    public Interval[] intervalsForIntervalState(int[] interval_state) {
        int dim = dim();
        Interval[] res = new Interval[dim()];
        for (int dim_i = 0; dim_i < dim; dim_i++) {
            if (interval_state[dim_i] < 0) throw new IllegalArgumentException("The interval state contains negative values.");
            if (interval_state[dim_i] >= intervals[dim_i].length) throw new IllegalArgumentException("The interval state contains too large values.");
            res[dim_i] = intervals[dim_i][interval_state[dim_i]];
        }
        return res;
    }
    
    public boolean isStateInIntervalState(double[] state, int[] interval_state) {
        for (int dim_i = 0; dim_i < interval_state.length; dim_i++) {
            if (interval_state[dim_i] < 0) throw new IllegalArgumentException("The interval state contains negative values.");
            if (interval_state[dim_i] >= intervals[dim_i].length) throw new IllegalArgumentException("The interval state contains too large values.");
            int loc = intervals[dim_i][interval_state[dim_i]].compareTo(state[dim_i]);
            if (loc != 0) return false;
        }
        return true;
    }
    
    public final void recomputeIntervals(boolean divide_output_species) {
        int dim = dim();
        this.intervals = new Interval[dim()][];
        for (int dim_i = 0; dim_i < dim; dim_i++) {
            TreeSet<Integer> forced = forcedByReactions(dim_i);
            if (extraLevels != null && extraLevels[dim_i] != null) forced.addAll(extraLevels[dim_i]);
            if (divide_output_species) forced.add(1);
            this.intervals[dim_i] = PopulationLevelHelper.intervalsFor(0, bounds[dim_i], this.population_level_growth_factor, forced);
        }
    }
    
    public TreeSet<Integer> forcedByReactions(int dim_i) {
        TreeSet<Integer> forced = new TreeSet<Integer>();
        for (Reaction r : this.crn.reactions) {
            for (int v = 1; v<= r.reactants[dim_i]; v++) forced.add(v);
            forced.add(r.reactants[dim_i]);
        }
        return forced;
    }
    
    public int[] closestStateWithinBounds(int[] state) {
        int[] res = new int[state.length];
        for (int dim_i = 0; dim_i < state.length; dim_i++) {
            if (state[dim_i] < 0) res[dim_i] = 0;
            else if (state[dim_i] > bounds[dim_i]) res[dim_i] = bounds[dim_i];
            else res[dim_i] = state[dim_i];
        }
        return res;
    }
    
    public double[] closestStateWithinBounds(double[] state) {
        double[] res = new double[state.length];
        for (int dim_i = 0; dim_i < state.length; dim_i++) {
            if (state[dim_i] < 0) res[dim_i] = 0;
            else if (state[dim_i] > bounds[dim_i]) res[dim_i] = bounds[dim_i];
            else res[dim_i] = state[dim_i];
        }
        return res;
    }
    
    public boolean roundToClosestStateWithinBounds(double[] state) {
        boolean rounded = false;
        for (int dim_i = 0; dim_i < state.length; dim_i++) {
            if (state[dim_i] < 0) {
                rounded = true;
                state[dim_i] = 0;
            }
            else if (state[dim_i] > bounds[dim_i]) {
                rounded = true;
                state[dim_i] = bounds[dim_i];
            }
        }
        return rounded;
    }
    
    public boolean isWithinBounds(int[] state) {
        for (int dim_i = 0; dim_i < state.length; dim_i++) {
            if (state[dim_i] < 0 || state[dim_i] >= bounds[dim_i] + 1) return false;
        }
        return true;
    }
    
    public boolean isWithinBounds(double[] state) {
        for (int dim_i = 0; dim_i < state.length; dim_i++) {
            if (state[dim_i] < 0 || state[dim_i] >= bounds[dim_i] + 1) return false;
        }
        return true;
    }
    
    public String toLaTeX(double time) {
        Function<String, String> LaTeXNameFor = (s) -> { return "\\textsc{" + s + "}";};
        String res = "\\begin{itemize} \n";
        res += "\\item species: ";
        String[] speciesAsLatex = new String[dim()];
        for (int dim_i = 0; dim_i < dim(); dim_i++) {
            speciesAsLatex[dim_i] = LaTeXNameFor.apply(crn.speciesNames[dim_i]);
        }
        res += "$" + String.join(", ", speciesAsLatex) + "$\n";
        res += "\\item reactions: \n \\begin{itemize}";
        for (int r_i = 0; r_i < crn.reactions.length; r_i++) {
            res += "\\item " + crn.reactions[r_i].toLaTeX(this.crn, LaTeXNameFor)+ "\n";
        }
        res += "\\end{itemize} \n";
        res += "\\item initial state: $";
        if (Vector.isNull(initial_state)) res += "\\emptyset ";
        else {
            res += "\\left( ";
            boolean first = true;
            for (int dim_i = 0; dim_i < dim(); dim_i++) {
                if (initial_state[dim_i] <= 0) continue;
                if (first) first = false;
                else res+= ", ";
                if (initial_state[dim_i] > 1) {
                     res+= initial_state[dim_i] + " \\times ";
                }
                res+= LaTeXNameFor.apply(crn.speciesNames[dim_i]);
            }
            res += "\\right) ";
        }
        res+="$ \n";
        res += "\\item end time: " + time + "s\n";
        res += "\\end{itemize}";
        return res;
    }

    public int dim() {
        return crn.dim();
    }

    @Override
    public Simulation createSimulation() {
        return new Simulation(Vector.asDoubleArray(initial_state));
    }
    
    public static void main(String[] args) {
        Setting setting = Examples.getSettingByName("ecoli_canonical.json");
        System.out.println(setting.toLaTeX(50000.0));
    }
    
    public Setting copy() {
        Gson gson = JSON.getGson();
        return gson.fromJson(gson.toJson(this), Setting.class);
    }
}
