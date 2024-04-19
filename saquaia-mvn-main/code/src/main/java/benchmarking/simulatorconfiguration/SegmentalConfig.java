package benchmarking.simulatorconfiguration;

import core.model.Setting;
import core.simulation.simulators.SegmentalSimulator;
import core.simulation.simulators.segmental.GrowingMemoryFunction;
import core.simulation.simulators.segmental.MemoryFunction;

/**
 *
 * @author Martin
 */
public class SegmentalConfig extends SimulatorConfig{ 
    
    SimulatorConfig baseConfig = new SSAConfig();
    boolean useSummaries = true;
    Boolean adaptive = true;
    long max_memory = 1000L * 1000 * 1000 * 5;     // 5GB
    MemoryFunction memory_function = GrowingMemoryFunction.normal();
    double memory_free_target_fraction = 0.85;
    int max_segment_count_per_direction = 10;
    int min_segments_for_stats = 10;
    Double memory_fraction_for_abs_cache = 0.1;
    
    public SegmentalConfig(){
        super(Type.SEG);
    }

    public SimulatorConfig getBaseConfig() {
        return baseConfig;
    }

    public boolean isUseSummaries() {
        return useSummaries;
    }

    public Boolean getAdaptive() {
        return adaptive;
    }

    public long getMax_memory() {
        return max_memory;
    }

    public MemoryFunction getMemoryFunction() {
        return memory_function;
    }

    public double getMemoryFreeTargetFraction() {
        return memory_free_target_fraction;
    }

    public int getMaxSegmentCountPerDirection() {
        return max_segment_count_per_direction;
    }

    public int getMinSegmentsForStats() {
        return min_segments_for_stats;
    }

    public Double getMemoryFractionForAbsCache() {
        return memory_fraction_for_abs_cache;
    }
    
    public SegmentalConfig setBaseConfig(SimulatorConfig x) {
        this.baseConfig = x;
        return this;
    }
    
    public SegmentalConfig setUseSummaries(boolean x) {
        this.useSummaries = x;
        return this;
    }
    
    public SegmentalConfig setAdaptive(boolean x)
    {
        this.adaptive = x;
        return this;
    }
    
    public SegmentalConfig setMaxMemory(long x) {
        this.max_memory = x;
        return this;
    }
    
    public SegmentalConfig setMemoryFractionForAbsCache(double x) {
        this.memory_fraction_for_abs_cache = x;
        return this;
    }
    
    public SegmentalConfig setMemoryFunction(MemoryFunction f)
    {
        this.memory_function = f;
        return this;
    }
    
    public SegmentalConfig setMemoryFreeTargetFraction(double x)
    {
        this.memory_free_target_fraction = x;
        return this;
    }
    
    public SegmentalConfig setMaxSegmentCountPerDirection(int x)
    {
        this.max_segment_count_per_direction = x;
        return this;
    }
    
    public SegmentalConfig setMinSegmentsForStats(int x)
    {
        this.min_segments_for_stats = x;
        return this;
    }
    
    @Override
    public SegmentalSimulator createSimulator(Setting setting) {
        return new SegmentalSimulator(setting, 
                baseConfig.createSimulator(setting), 
                useSummaries, 
                adaptive == null ? true : adaptive, 
                max_memory, 
                memory_fraction_for_abs_cache == null ? 0.1 : memory_fraction_for_abs_cache,
                memory_function,  
                memory_free_target_fraction,
                max_segment_count_per_direction,
                min_segments_for_stats
        );
    }
}
