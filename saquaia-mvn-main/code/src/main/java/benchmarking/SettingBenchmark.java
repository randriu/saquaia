package benchmarking;

import core.model.Setting;
import core.util.Examples;
import core.util.Progressable;

/**
 *
 * @author Martin
 */
public abstract class SettingBenchmark extends Benchmark{
    
    public String example;
    public Setting setting;
    public Double c;
    public double end_t;
    public int[] initial_state;
    
    public SettingBenchmark(Type type, long timout, String name, String example, double end_t) {
        super(type, timout, name);
        this.example = example;
        this.end_t = end_t;
    }
    
    public SettingBenchmark setC(double c) {
        this.c = c;
        return this;
    }
    
    public SettingBenchmark setInitialState(int[] initial_state) {
        this.initial_state = initial_state;
        return this;
    }
    
    public boolean prepare(Progressable p) {
        if (setting == null && example == null) {
            p.updateMessage("Not setting was specified. Please provide either a setting or the name of an example.");
            return false;
        }
        if (setting == null) setting = Examples.getSettingByName(example);
        if (c != null) {
            setting.changePopulationLevelGrowthFactor(c);
            p.updateMessage("Population level growth factor changed to " + c);
        }
        if (initial_state != null && initial_state.length == setting.dim() && setting.isWithinBounds(initial_state)) {
            setting.initial_state = initial_state;
        }
        return true;
    }
    
}
