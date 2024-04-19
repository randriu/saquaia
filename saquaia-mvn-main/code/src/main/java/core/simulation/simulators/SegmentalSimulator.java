package core.simulation.simulators;

import core.model.Setting;
import core.util.IO;
import core.util.IntArrayWrapper;
import core.util.LFU_DA_Cache;
import core.util.LFU_DA_Cache_for_sorting;
import core.util.Pair;
import core.util.Progressable;
import core.util.Stochastics;
import core.util.Vector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.stream.Collectors;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;
import org.openjdk.jol.vm.VM;
import core.simulation.events.IntervalStateChangeEndCondition;
import core.simulation.Simulation;
import core.simulation.simulators.segmental.MemoryFunction;
import core.simulation.simulators.segmental.Segment;
import core.simulation.simulators.segmental.Segmentlike;
import core.simulation.simulators.segmental.Summary;

/**
 *
 * @author Martin
 */
public class SegmentalSimulator extends AbstractSimulator{
    public Simulator baseSimulator;
    public boolean useSummaries = true;
    public boolean adaptive = true;
    public MemoryFunction memoryFunction;
    
    public static final long ABS_MEMORY_SHALLOW_SIZE = ClassLayout.parseClass(Memory.AbsMemory.class).instanceSize();
    public static final long MEMORY_SHALLOW_SIZE = ClassLayout.parseClass(Memory.class).instanceSize();
    public static final long OBJECT_ALIGNMENT = VM.current().objectAlignment();
    
    double memory_fraction_for_abs_cache;
    double memory_free_target_fraction;
    int max_segment_count_per_direction;
    int min_segments_for_stats;
    
    public final Memory memory;
    
    public SegmentalSimulator(
            Setting setting, 
            AbstractSimulator baseSimulator, 
            boolean useSummaries, 
            boolean adaptive, 
            long max_memory, 
            double memory_fraction_for_abs_cache,
            MemoryFunction memoryFunction,
            double memory_free_target_fraction,
            int max_segment_count_per_direction,
            int min_segments_for_stats) {
        super(setting);
        
        this.baseSimulator = baseSimulator;
        baseSimulator.addIntervalStateChangeListener(new IntervalStateChangeEndCondition());
        
        this.useSummaries = useSummaries;
        this.adaptive = adaptive;
        this.memoryFunction = memoryFunction;
        this.memory_fraction_for_abs_cache = memory_fraction_for_abs_cache;
        this.memory_free_target_fraction = memory_free_target_fraction;
        this.max_segment_count_per_direction = max_segment_count_per_direction;
        this.min_segments_for_stats = min_segments_for_stats;
        
        memory = new Memory(max_memory);
    }
    
    @Override
    public boolean speedsUp() {
        return true;
    };

    @Override
    public Object getMemory() {
        return memory.mem;
    }
    
    public boolean isCompatibleWith(Setting other_setting) {
        if (!setting.crn.equals(other_setting.crn)) return false;
        for (int dim_i = 0; dim_i < dim; dim_i++) {
            if (!Arrays.equals(setting.intervals[dim_i], other_setting.intervals[dim_i])) return false;
        }
        return true;
    }
    
    @Override
    public ArrayList<Pair<String,Number>> getStatistics(){
        ArrayList<Pair<String,Number>> stats = new ArrayList<>();
        stats.add(new Pair("memory size (in byte)", memory.total_memory));
        stats.add(new Pair("uses", memory.total_uses));
        stats.add(new Pair("abstract states", memory.known.size()));
        stats.add(new Pair("ignored abstract states", memory.currently_ignored));
        stats.add(new Pair("ignored abstract states (percentage)",  memory.known.isEmpty() ? 0 : (100.0 * memory.currently_ignored / memory.known.size())));
        stats.add(new Pair("ejected abstract states", memory.total_ejected));
        stats.add(new Pair("effective segmentlikes", memory.total_effective_segmentlikes));
        stats.add(new Pair("segmentlikes", memory.total_segmentlikes));
        stats.add(new Pair("segmentlikes reduction because of smart memory representation (percentage)", memory.total_effective_segmentlikes == 0 ? 0 : (100.0 - (100.0 * memory.total_segmentlikes / memory.total_effective_segmentlikes))));
        stats.add(new Pair("segmentlikes generated from current state because ignored", memory.total_segmentlikes_generated_from_current_state_because_ignored));
        stats.add(new Pair("segmentlikes generated from current state because not adaptive", memory.total_segmentlikes_generated_from_current_state_because_not_adaptive));
        stats.add(new Pair("segmentlikes generated from representatives", memory.total_segmentlikes_generated_from_representatives));
        stats.add(new Pair("segmentlikes reused", memory.total_segmentlikes_reused));
        stats.add(new Pair("dynamic_aging_events", memory.known.dynamicAgingEvents()));
        stats.add(new Pair("memory_reevaluations", memory.memory_reevaluations));
        stats.add(new Pair("time_spent_on_memory_reevaluation (in ns)", memory.time_spent_on_memory_reevaluation));
        
        return stats;
    }
    
    
        
//    public String getMemoryStatsAsString(){
//        String res = "";
//        res += "mem=" + memory.total_memory + "\n";
//        res += "mem_pretty=" + IO.humanReadableByteCountSI(memory.total_memory) + "\n";
//        long exact_size = memory.total_memory <= 2_000_000_000 ? GraphLayout.parseInstance(memory).totalSize() : 0;
//        res += "mem_measured=" + exact_size + "\n";
//        res += "mem_measured_pretty=" + IO.humanReadableByteCountSI(exact_size) + "\n";
//        res += "effective_segments=" + memory.total_effective_segmentlikes + "\n";
//        res += "segmentlikes=" + memory.total_segmentlikes + "\n";
//        res += "segmentlikes_removed=" + memory.total_segmentlikes_removed + "\n";
//        res += "segmentlikes_generated_from_current_state_because_ignored=" + memory.total_segmentlikes_generated_from_current_state_because_ignored + "\n";
//        res += "total_segmentlikes_generated_from_current_state_because_not_adaptive=" + memory.total_segmentlikes_generated_from_current_state_because_not_adaptive + "\n";
//        res += "total_segmentlikes_generated_from_representatives=" + memory.total_segmentlikes_generated_from_representatives + "\n";
//        res += "total_segmentlikes_reused=" + memory.total_segmentlikes_reused + "\n";
//        res += "segmentlikes_effective=" + memory.total_effective_segmentlikes + "\n";
//        if (memory.total_effective_segmentlikes > 0) res += "segments_saved_percent=" + IO.significantFigures(100.0 * memory.total_segmentlikes / memory.total_effective_segmentlikes) + "\n";
//        else res += "segments_saved_percent=100.0";
//        res += "known_abs=" + memory.known.size() + "\n";
//        res += "known_abs_ignored=" + memory.currently_ignored + "\n";
//        res += "known_abs_not_ignored=" + (memory.known.size() - memory.currently_ignored) + "\n";
//        res += "abs_ignored_total=" + memory.total_ignored + "\n";
//        res += "ejected=" + memory.total_ejected + "\n";
//        res += "uses=" + memory.total_uses + "\n";
//        res += "dynamic_aging_events=" + memory.known.dynamicAgingEvents() + "\n";
//        res += "memory_reevaluations=" + memory.memory_reevaluations + "\n";
//        res += "time_spent_on_memory_reevaluation=" + memory.time_spent_on_memory_reevaluation + "\n";
//        return res;
//    }
    
    private Segmentlike useBaseSimulator(Random random, double[] start_state, Progressable p, int[] abs) {
        Simulation sim = new Simulation(start_state);
        sim.setSeed(random.nextLong());
        sim.setKeepHistory(!useSummaries);

        p.start_subroutine_with_unknown_percentage();
        baseSimulator.simulate(sim, Double.POSITIVE_INFINITY, p, abs);
        p.end_subroutine();

        if (sim.getNrOfSteps() == 0) return null;    // stuck!
        Segmentlike res;
        if (useSummaries) res = new Summary(sim);
        else res = new Segment(sim);
        return res;
    }

    @Override
    public void simulate(Simulation sim, double end_t, Progressable p, int[] interval_state_hint) {
        double t = sim.getTime();
        double[] state = sim.getState();
        Random random = sim.getRandom();
        
        int reactions = 0;
        
        if (!initSimulationEventHandlers(t, state, interval_state_hint)) {
            return;
        }
        
        int[] abs = setting.intervalStateForState(state);
        
        while (t < end_t) {
            if (p.isStopped()) break;
            
            if (abs == null) break; // out of setting bounds
            abs = setting.updateIntervalStateForState(state, abs);
            if (abs == null) break; // out of setting bounds
            IntArrayWrapper abs_wrapped = new IntArrayWrapper(Vector.copy(abs));
            
            Segmentlike seg = memory.segmentlikeFor(abs_wrapped, state, random, p);
            
            if (seg == null) { // out of setting bounds
                break;
            }
            
            boolean cont = true;
            if (seg.getSteps() != null) {   // follow steps
                Pair<double[], Double> last_pair = seg.getSteps().get(0);
                for (int step_i = 1; step_i < seg.getSteps().size(); step_i++) {
                    Pair<double[], Double> pair = seg.getSteps().get(step_i);
                    t += pair.right - last_pair.right;
                    Vector.plus(state, pair.left);
                    Vector.minus(state, last_pair.left);
                    last_pair = pair;

                    if (step_i < seg.getSteps().size() - 1) {
                        cont = t < end_t && justGoto(t, state);
                        if (cont && sim.needIntermidiateSteps()) {
                            if (t == Double.POSITIVE_INFINITY) t = end_t;
                            sim.evolve(state, t, 0);
                        }
                        if (!cont) break;
                    }
                }
            }
            else {      // only apply delta
                t += seg.getDeltaTime();
                seg.applyDeltaStateTo(state);
            }
            
            reactions += seg.getReactions();
            
//            System.out.println("new SEG state: " + Arrays.toString(state));
            
            if (sim.needIntermidiateSteps()) {
                if (t == Double.POSITIVE_INFINITY) t = end_t;
                sim.evolve(state, t, reactions);       // care: not accurate for segment steps (we always add full nr of reactions even if the segment was only applied partially)
                reactions = 0;
            }
            
            p.progressSubroutineTo(t / end_t);
            
            cont = cont && t < end_t && justGoto(t, state);
            if (!cont) break;
        }
        
        // check that we updated the simulation data
        if (t == Double.POSITIVE_INFINITY) t = end_t;
        if (sim.getTime() != t || reactions != 0 || !Arrays.equals(state, sim.getState())) {
            sim.evolve(state, t, reactions);
        }
        p.progressSubroutineTo(1.0);
    }

    
    public class Memory {
        // memory
        public long max_memory;
        
        // cache for remembering abstract states
        public LFU_DA_Cache<IntArrayWrapper> known;
        
        // memory per abstract state
        public HashMap<IntArrayWrapper, AbsMemory> mem = new HashMap<>();
        
        // stats
        public int total_segmentlikes = 0;
        public long total_segmentlikes_removed = 0;
        public long total_effective_segmentlikes = 0;
        public long total_memory = 0;
        public long total_memory_freed = 0;
        public long total_uses = 0;
        public long total_segmentlikes_reused = 0;
        public long total_segmentlikes_generated_from_representatives = 0;
        public long total_segmentlikes_generated_from_current_state_because_ignored = 0;
        public long total_segmentlikes_generated_from_current_state_because_not_adaptive = 0;
        public long total_ignored = 0;
        public int currently_ignored = 0;
        public long total_ejected = 0;
        
        public int memory_reevaluations = 0;
        public long time_spent_on_memory_reevaluation = 0;
        
        public Memory(long max_memory){
            this.max_memory = max_memory;
            
            // calculate the capacity of the cache (this guarantees that most of the memory is used for segment
            int cache_capacity = (int) ((memory_fraction_for_abs_cache * max_memory) / ABS_MEMORY_SHALLOW_SIZE);   // --> memory_fraction_for_abs_cache of memory for cache & rest for segmentlikes
            known = new LFU_DA_Cache_for_sorting<>(cache_capacity, 10000, 0.001, IntArrayWrapper.class);
        }
        
        public Segmentlike segmentlikeFor(IntArrayWrapper abs_wrapped, double[] state, Random random, Progressable p) {
            IntArrayWrapper ejected = null;
            if (!adaptive && known.isFull()) {
                boolean is_known = known.addIfExists(abs_wrapped);
                if (!is_known) {
                    // cannot add to memory!
                    total_segmentlikes_generated_from_current_state_because_ignored ++;
                    total_segmentlikes_generated_from_current_state_because_not_adaptive ++;
                    return useBaseSimulator(random, state, p, abs_wrapped.array);
                } 
            }
            else ejected = known.addAndReturnEjected(abs_wrapped);
            
            AbsMemory abs_mem = mem.get(abs_wrapped);
            if (abs_mem == null) {
                abs_mem = new AbsMemory(abs_wrapped.array);
                mem.put(abs_wrapped, abs_mem);
            }
            if (ejected != null) {
                AbsMemory abs_mem_ejected = mem.remove(ejected);
                abs_mem_ejected.eject();
            }
            Segmentlike res = abs_mem.use(random, state, p);
            checkMem();
            return res;
        }

        public void checkMem() {
            if (total_memory <= max_memory || !adaptive) return;
            
            System.out.println("");
            System.out.println("Start to free mem...");
            long start = System.nanoTime();
            memory_reevaluations++;
            // calculate efficiencies and order known abstract states
            AbsMemory[] abs_memories = mem.values().toArray(AbsMemory[]::new);
            Arrays.sort(abs_memories, (AbsMemory o1, AbsMemory o2) -> {
                return Double.compare(o1.efficiency(),o2.efficiency());
            });
            // free memory
            long total_memory_before = total_memory;
            int total_segmentlikes_before = total_segmentlikes;
            int new_ignores = 0;
            int new_unignores = 0;
            
            double cut_off_efficiency = 0;
            Double worst_efficiency = null;
            double best_efficiency = 0;
            double efficiency_sum_of_not_ignored = 0;
            int efficiency_sum_of_not_ignored_summed = 0;
            
//            Iterator<IntArrayWrapper> it = known.sortedIterator((IntArrayWrapper o1, IntArrayWrapper o2) -> {
//                return Double.compare(mem.get(o1).efficiency(),mem.get(o2).efficiency());
//            });
            for (AbsMemory m : abs_memories) {
//            while(it.hasNext()) {
//                AbsMemory m = mem.get(it.next());
                double efficiency = m.efficiency();
                if (worst_efficiency == null) worst_efficiency = efficiency;
                
                if (total_memory > memory_free_target_fraction * max_memory) {
                    if (m.ignore()) new_ignores++;
                    cut_off_efficiency = efficiency;
                }
                else {
                    if (m.unignore()) new_unignores++;
                    efficiency_sum_of_not_ignored += efficiency;
                    efficiency_sum_of_not_ignored_summed++;
                }
                best_efficiency = efficiency;
            }
            int segmentlikes_removed = total_segmentlikes_before - total_segmentlikes;
            
            System.out.println("known interval states: " + known.size() + "; ignored: " + total_ignored);
            System.out.println("worst efficiency: " + worst_efficiency);
            System.out.println("cut_off_efficiency: " + IO.significantFigures(cut_off_efficiency));
            System.out.println("best efficiency: " + best_efficiency);
            System.out.println("not ignored: " + efficiency_sum_of_not_ignored_summed);
            if (efficiency_sum_of_not_ignored_summed > 0) System.out.println("avg efficiency: " + efficiency_sum_of_not_ignored / efficiency_sum_of_not_ignored_summed);
            System.out.println("ignoring " + new_ignores + " interval states and removed " + segmentlikes_removed + " segmentlikes");
            System.out.println("unignoring " + new_unignores + " interval states");
            System.out.println("known interval states: " + known.size() + "; ignored: " + total_ignored);
            System.out.println("avg frequency in cache: " + IO.significantFigures(known.getAvgFrequency()) + "   cache_size: " + known.size() + "  freqSum: " + known.getFrequencySum() + "  freqMax " + known.getMaxFrequency());
            
            System.out.println("Ended mem freeing... (" + IO.humanReadableByteCountSI(total_memory_before) + " -> " + IO.humanReadableByteCountSI(total_memory) + ")");
            long took = System.nanoTime() - start;
            time_spent_on_memory_reevaluation += took;
            System.out.println("took " + IO.humanReadableDuration(took) + " (total: " + IO.humanReadableDuration(time_spent_on_memory_reevaluation) + ")");
            System.out.println("");
        }
        
        public void efficiencyDistribution() {
            ArrayList<AbsMemory> abs_mems = new ArrayList<>(memory.mem.values());
            
            System.out.println("");
            System.out.println("Total ejected: " + memory.total_ejected);
            System.out.println("");
            System.out.println("Ordered by freq:");
            abs_mems.sort((o1, o2) -> {
                return -Long.compare(o1.uses, o2.uses);
            });
            for (int i = 0; i<abs_mems.size(); i = (i+1)*2-1) {
                AbsMemory x = abs_mems.get(i);
                System.out.println(i + ": " + IO.significantFigures(1.0 * x.uses / total_uses) + "              " + Arrays.toString(x.abs));
            }
            AbsMemory x = abs_mems.get(abs_mems.size()-1);
            System.out.println((abs_mems.size()-1) + ": " + IO.significantFigures(1.0 * x.uses / total_uses) + "              " + Arrays.toString(x.abs));
            System.out.println("");
            
            System.out.println("Ordered by avg reactions:");
            abs_mems.sort((o1, o2) -> {
                return -Double.compare(o1.avgReactions(), o2.avgReactions());
            });
            for (int i = 0; i<abs_mems.size(); i = (i+1)*2-1) {
                x = abs_mems.get(i);
                System.out.println(i + ": " + IO.significantFigures(x.avgReactions()) + "              " + Arrays.toString(x.abs));
            }
            x = abs_mems.get(abs_mems.size()-1);
            System.out.println((abs_mems.size()-1) + ": " + IO.significantFigures(x.avgReactions()) + "              " + Arrays.toString(x.abs));
            System.out.println("");
            
            System.out.println("Ordered by efficiency:");
            abs_mems.sort((o1, o2) -> {
                return -Double.compare(o1.avgReactions() * o1.uses, o2.avgReactions() * o2.uses);
            });
            for (int i = 0; i<abs_mems.size(); i = (i+1)*2-1) {
                x = abs_mems.get(i);
                System.out.println(i + ": " + IO.significantFigures(x.avgReactions() * x.uses / total_uses) + "              " + Arrays.toString(x.abs));
            }
            x = abs_mems.get(abs_mems.size()-1);
            System.out.println((abs_mems.size()-1) + ": " + IO.significantFigures(x.avgReactions() * x.uses / total_uses) + "              " + Arrays.toString(x.abs));
            double efficiency_sum = abs_mems.stream().mapToDouble((a) -> {return a.avgReactions() * a.uses / total_uses;}).sum();
            System.out.println("sum: " + IO.significantFigures(efficiency_sum));
            System.out.println("");
            
            System.out.println("segmentlikes: " + total_segmentlikes);
            System.out.println("abs_in_mem: " + mem.size());
            System.out.println("ignored: " + currently_ignored);
            System.out.println("");
        }
        
        public class AbsMemory {
            public int[] abs;
            public HashMap<IntArrayWrapper,AbsMemoryDir> mem = new HashMap<>(1);
            int segmentlikes = 0;
            int effective_segmentlikes = 0;
            private long uses = 0;
            private boolean ignored = false;
            public long memory = ABS_MEMORY_SHALLOW_SIZE;
            public long time_spent_generating_segmentlikes_starting_at_representative = 0;
            public int segmentlikes_generated_starting_at_representative = 0;

            public AbsMemory(int[] abs) {
                this.abs = abs;
                total_memory+= memory;
            }
            
            private void addSegmentlikesToMemory(Random rand, Segmentlike s, IntArrayWrapper dir) {
                if (!adaptive && total_memory > max_memory) return;     // cannot add because not adaptive
                
                if (!mem.containsKey(dir)) mem.put(dir, new AbsMemoryDir());
                AbsMemoryDir abs_mem_dir = mem.get(dir);
                
                // the effective number of segments is always increased
                abs_mem_dir.effective++;
                effective_segmentlikes++;
                total_effective_segmentlikes++;
                
                if (s != null) {
                    // direction has not enough examples
                    if (abs_mem_dir.segmentlikes.size() < max_segment_count_per_direction) {
                        // add segment
                        abs_mem_dir.segmentlikes.add(s);
                        long additional_memory = s.getMem();
                        memory += additional_memory;
                        total_memory += additional_memory;
                        segmentlikes++;
                        total_segmentlikes++;
                    } 
                    // direction has enough examples
                    else {
                        // replace random segment in order to make approximation more stable 
                        int segment_id_to_replace = rand.nextInt(abs_mem_dir.segmentlikes.size());
                        Segmentlike replaced = abs_mem_dir.segmentlikes.get(segment_id_to_replace);
                        abs_mem_dir.segmentlikes.set(segment_id_to_replace, s);
                        long replaced_memory = replaced.getMem();
                        long s_memory = s.getMem();
                        memory += s_memory - replaced_memory;
                        total_memory += s_memory - replaced_memory;
                    }
                } else {
                    // special case for null-segments
                    if (abs_mem_dir.segmentlikes.isEmpty()) {
                        abs_mem_dir.segmentlikes.add(s);
                        segmentlikes++;
                        total_segmentlikes++;
                    }
                }
            }
            
            private void clearSegmentlikes() {
                long memory_freed = memory - ABS_MEMORY_SHALLOW_SIZE;
                total_memory -= memory_freed;
                total_memory_freed += memory_freed;
                memory = ABS_MEMORY_SHALLOW_SIZE;
                total_segmentlikes -= segmentlikes;
                total_segmentlikes_removed += segmentlikes;
                segmentlikes = 0;
                total_effective_segmentlikes -= effective_segmentlikes;
                effective_segmentlikes = 0;
                mem.clear();
            }
            
            private void eject() {
                clearSegmentlikes();
                total_memory -= memory;
                total_memory_freed += memory;
                memory = 0;
                if (ignored) currently_ignored--;
                if (total_ejected == 0) {
                    long totalSize = GraphLayout.parseInstance(known.toArray()).totalSize();
                    System.out.println("First eject!");
                    System.out.println("number of abstract states: " + known.size());
                    System.out.println("memory for array of abstract states: " + IO.humanReadableByteCountSI(totalSize));
                }
                total_ejected++;
            }
            
            private IntArrayWrapper directionOf(Segmentlike seg, double[] representative) {
                if (seg == null) return null;
                int[] dir = new int[representative.length];
                for (int dim_i = 0; dim_i < representative.length; dim_i++) {
                    dir[dim_i] = setting.intervals[dim_i][abs[dim_i]].direction(seg.getDeltaState()[dim_i]);
                }
                return new IntArrayWrapper(dir);
            }

            private Segmentlike generateForMemory(Random random, Progressable p, boolean save){
                long start = System.nanoTime();
                double[] rep = Vector.asDoubleArray(setting.representativeForIntervalState(abs));
                Segmentlike res = useBaseSimulator(random, rep, p, abs);
                if (save) addSegmentlikesToMemory(random, res, directionOf(res, rep));
                total_segmentlikes_generated_from_representatives++;
                segmentlikes_generated_starting_at_representative++;
                time_spent_generating_segmentlikes_starting_at_representative += (System.nanoTime() - start);
                
//                // enough segments to judge?
//                if (segmentlikes_generated_starting_at_representative == min_segments_for_stats) {
//                    double avgReactions = avgReactions();
//                    if (avgReactions < 5) { // TODO change this contant using config!
//                        System.out.println("ignored because of short segments: " + Arrays.toString(abs) + " only averages " + avgReactions + " reactions");
//                        ignore();
//                    }
//                }
                
                return res;
            }

            private Segmentlike generateWithoutMemory(Random random, double[] start_state, Progressable p){            
                total_segmentlikes_generated_from_current_state_because_ignored++;
                return useBaseSimulator(random, start_state, p, abs);
            }

            public Segmentlike getRandomEffectiveSegmentlike(Random random) {
                if (effective_segmentlikes == 0) return null;
                return getEffectiveSegmentlike(random, random.nextInt(effective_segmentlikes));
            }
            
            public Segmentlike getEffectiveSegmentlike(Random random, int effective_segmentlike_id){
                if (effective_segmentlike_id < 0 || effective_segmentlike_id >= effective_segmentlikes) throw new IllegalArgumentException("The effective segment with id " + effective_segmentlike_id + " does not exist.");
                
                total_segmentlikes_reused++;
                
                // look for the direction that contains the effective segment 
                int seen = 0;
                for (AbsMemoryDir x : mem.values()) {
                    if (effective_segmentlike_id < seen + x.effective) {
                        // direction contains effective segment
                        if (x.effective == x.segmentlikes.size()) {
                            // direction was not approximated
                            return x.segmentlikes.get(effective_segmentlike_id - seen);
                        } else {
                            // direction was approximated
                            // --> chose a random segment in this direction
                            return Stochastics.choose(random, x.segmentlikes);
                        }
                    }
                    // direction did not contain effective segment
                    // -> try next direction
                    seen += x.effective;
                }
                throw new IllegalStateException("Tried to get effective segmentlike with id " + effective_segmentlike_id + " but there where only " + seen + " segmentlikes. Expected there to be " + effective_segmentlikes + ".");
            }
            
            public void trim() {
                for (AbsMemoryDir x : mem.values()) {
                    x.segmentlikes.trimToSize();
                }
            }

            public Segmentlike use(Random random, double[] state, Progressable p) {
                total_uses++;
                uses++;

                // ignored but need to gather stats
                if (ignored && segmentlikes_generated_starting_at_representative < min_segments_for_stats) {
                    return generateForMemory(random, p, false);
                }
                
                // compute how many effective segmentlikes should be in memory
                // if we need more, we do not compute but simly pretend it is there
                // we only compute the segment the first time it is actually used (#lazy)
                double target = memoryFunction.target_nr_of_saved_segmentlikes(uses);
                
                // ignored by adaptive memory or ignored by memory function
                if (ignored || target <= 0) return generateWithoutMemory(random, state, p);
                
                // use a (possible not yet generated) segment from memory
                int effective_segmentlike_id = random.nextInt((int)Math.ceil(target));
                if (effective_segmentlike_id >= effective_segmentlikes) {
                    // effective segment does not exist
                    // --> lazy generation
                    return generateForMemory(random, p, true);
                }
                else {
                    // effective segment exists
                    // load from memory
                    return getEffectiveSegmentlike(random, effective_segmentlike_id);
                }
                
//                if (target > effective_segmentlikes) return generateForMemory(random, p, true);
//                return reuse(random);
            }

            public boolean ignore(){
                if (ignored) return false;
                clearSegmentlikes();
                ignored = true;
                currently_ignored++;
                total_ignored++;
                return true;
            }

            public boolean unignore(){
                if (!ignored) return false;
                ignored = false;
                currently_ignored--;
                total_ignored--;
                return true;
            }

            public String stats(){
                return Arrays.toString(abs)
                        + ": segments=" + segmentlikes
                        + " bytes=" + memory + " (" + IO.humanReadableByteCountSI(memory) + ")"; 
            }
                
            // computes the average number of reactions in the segment distribution approximation
            public double avgReactions(){
                if (effective_segmentlikes == 0) return 0;
                double res = 0;
                for (IntArrayWrapper dir : mem.keySet()) {
                    AbsMemoryDir x = mem.get(dir);
                    res += x.avgReactions() / x.segmentlikes.size() * x.effective;
                }
                return res / effective_segmentlikes;
            }
            
            public long avg_time_for_generating_segmentlikes_starting_at_represetnative(){
                return segmentlikes_generated_starting_at_representative == 0 ? 0 : time_spent_generating_segmentlikes_starting_at_representative / segmentlikes_generated_starting_at_representative;
            }
            
            // computes how many segments are represented per saved segment (compare "smart segment distribution approximation")
            public double memoryEfficiency(){
                return segmentlikes == 0 ? 1 : 1.0 * effective_segmentlikes / segmentlikes; 
            }
            
            // metric to decide what abstract states to ignore / unignore if memory is full
            public double efficiency() {
                return memoryEfficiency() 
                        * known.getFrequency(new IntArrayWrapper(abs))
                        * avg_time_for_generating_segmentlikes_starting_at_represetnative();
            }
            
            private class AbsMemoryDir {
                ArrayList<Segmentlike> segmentlikes = new ArrayList<>(1);
                int effective = 0;
                
                public double avgReactions(){
                    return segmentlikes.stream().collect(Collectors.summingDouble((value) -> {
                        return value == null ? 0 : value.getReactions();
                    }));
                }
                    
                @Override
                public String toString() {
                    if (segmentlikes.isEmpty()) return "-";
                    return segmentlikes.size() + "x: e.g." + segmentlikes.get(0);
                }
            }
        }
    }
}
