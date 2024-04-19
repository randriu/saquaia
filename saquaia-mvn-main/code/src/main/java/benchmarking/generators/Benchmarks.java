package benchmarking.generators;

import benchmarking.Benchmark;
import benchmarking.Benchmarking;
import benchmarking.SequenceBenchmark;
import benchmarking.VisualBenchmark;
import benchmarking.simulatorconfiguration.HybridConfig;
import benchmarking.simulatorconfiguration.ODEConfig;
import benchmarking.simulatorconfiguration.SSAConfig;
import benchmarking.simulatorconfiguration.SegmentalConfig;
import benchmarking.simulatorconfiguration.SimulatorConfig;
import benchmarking.simulatorconfiguration.TAUConfig;
import core.model.Setting;
import core.util.IO;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import core.simulation.simulators.segmental.GrowingMemoryFunction;
import core.simulation.simulators.segmental.MemoryFunction;

/**
 *
 * @author Martin
 */
public class Benchmarks {
        
    public static final boolean doVisual = true;
    public static final boolean doSequence = false;
    public static final boolean doSequenceSingle = false;
    
    public static final boolean save_simulator = false;
    
    public static final int times_visual = 100;
    public static final int times_transient = 10000;
    public static final int times_performance = 10;
    
    public static final int repeat_visual = 1;
        
    public static final SSAConfig confSSA = new SSAConfig();
    public static final TAUConfig confTAU = new TAUConfig();
    public static final ODEConfig confODE = new ODEConfig();
    public static final HybridConfig confHYB = new HybridConfig().setThresholdTau(5).setThresholdODE(400);
    
    public static final Random rand = new Random();
    
    public static void main(String[] args) {
        Long seed = 42l;
        rand.setSeed(seed);
        
        long timeout_visual = 1000l * 60 * 60 * 4;
        long timeout_performance = 1000l * 60 * 5;
        long timeout_transient = 1000l * 60 * 60 * 12;
        long timeout_sequence = 1000l * 60 * 60 * 24 * 2;
        
        ArrayList<Benchmark> bs = new ArrayList<>();
        
//        double[] cs = new double[]{2.0, 1.5, 1.3};
//        double[] cs = new double[]{3.0};
//        double[] cs = new double[]{2.0};
        double[] cs = new double[]{1.5};
//        double[] cs = new double[]{1.3};
//        double[] cs = new double[]{1.3, 1.2, 1.1, 1.05};
        
        bs.addAll(generateBenchmarks(
                "PP", 
                "predator_prey_canonical.json", 
                200, cs, timeout_visual, timeout_performance, timeout_transient)
        );
        bs.addAll(generateBenchmarks(
                "TS", 
                "toggle_switch_advanced_canonical.json", 
                50000, cs, timeout_visual, timeout_performance, timeout_transient)
        );
        bs.addAll(generateBenchmarks(
                "RP", 
                "repressilator_canonical.json", 
                50000, cs, timeout_visual, timeout_performance, timeout_transient)
        );
        bs.addAll(generateBenchmarks(
                "VI", 
                "viral_withV_canonical.json", 
                200, cs, timeout_visual, timeout_performance, timeout_transient)
        );
//        bs.addAll(generateBenchmarks(
//                "EC", 
//                "ecoli_canonical.json", 
//                2000, cs, timeout_visual, timeout_performance, timeout_transient)
//        );
//        bs.addAll(generateBenchmarks(
//                "VI_limit", 
//                "viral_withVlimit_canonical.json", 
//                2000, cs, timeout_visual, timeout_performance, timeout_transient)
//        );
//        bs.addAll(generateBenchmarks(
//                "TR", 
//                "toggle_switch_advanced_x_repressilator_canonical.json", 
//                50000, cs, timeout_visual, timeout_performance, timeout_transient)
//        );
        
        
        double c = 1.5;
        MemoryFunction f = GrowingMemoryFunction.normal();
//        MemoryFunction f = new LinearMemoryFunction(1.0);

        SegmentalConfig confSEG_SSA = new SegmentalConfig().setBaseConfig(confSSA).setMemoryFunction(f);
        SegmentalConfig confSEG_HYB = new SegmentalConfig().setBaseConfig(confHYB).setMemoryFunction(f);
//        SegmentalConfig confSEG_SSA_nonadapting = new SegmentalConfig().setBaseConfig(confSSA).setMemoryFunction(f).setAdaptive(false);
//        SegmentalConfig confSEG_HYB_nonadapting = new SegmentalConfig().setBaseConfig(confHYB).setMemoryFunction(f).setAdaptive(false);

        int repeat = 1;
        
//        long[] memory_limits = new long[]{1_000_000_000};
//        long[] memory_limits = new long[]{200_000, 400_000, 600_000, 800_000, 1_000_000};
//        long[] memory_limits = new long[]{50_000, 100_000, 150_000, 200_000, 250_000, 300_000, 350_000, 400_000, 500_000};
//        long[] memory_limits = new long[]{100_000_000, 200_000_000, 300_000_000, 400_000_000, 600_000_000, 800_000_000, 1_000_000_000, 1_200_000_000, 1_400_000_000, 1_600_000_000, 1_800_000_000, 2_000_000_000l, 2_500_000_000l, 3_000_000_000l, 4_000_000_000l, 5_000_000_000l};
//        long[] memory_limits = new long[]{300_000_000, 600_000_000, 1_000_000_000, 2_000_000_000l, 3_500_000_000l, 5_000_000_000l};
//        int sims_for_memory_speedup_tests = 2000;
        
        if (doSequenceSingle) {
            boolean doHybrid = true;
            boolean doTau = false;
            boolean doSeg = true;
//            bs.add(new SequenceBenchmark(timeout_sequence, "VI_Sequence_SSA", "viral_withV_canonical.json", confSSA, repeat, SequenceBenchmark.getVITask()).setC(c).setSeed(rand.nextLong()));
//            for (long ml : memory_limits) {
//                SegmentalConfig conf_i = new SegmentalConfig().setBaseConfig(confHYB).setMaxMemory(ml).setMemoryFunction(f);
//                bs.add(new SequenceBenchmark(timeout_sequence, "VI_Sequence_SEG+" + ml + "_" + f.abreviation() + "_HYB_" + c, "viral_withV_canonical.json", conf_i, repeat, SequenceBenchmark.getVITask()).setC(c).setSeed(rand.nextLong()));
//            }
//            bs.add(new SequenceBenchmark(timeout_sequence, "PP_Sequence_SSA", "predator_prey_canonical.json", confSSA, repeat, SequenceBenchmark.getPPTask()).setC(c).setSeed(rand.nextLong()));
//            for (long ml : memory_limits) {
//                SegmentalConfig conf_i = new SegmentalConfig().setBaseConfig(confHYB).setMaxMemory(ml).setMemoryFunction(f);
//                bs.add(new SequenceBenchmark(timeout_sequence, "PP_Sequence_SEG+" + ml + "_" + f.abreviation() + "_HYB_" + c, "predator_prey_canonical.json", conf_i, repeat, SequenceBenchmark.getPPTask()).setC(c).setSeed(rand.nextLong()));
//            }
//            bs.add(new SequenceBenchmark(timeout_sequence, "TR_Sequence_SSA", "toggle_switch_advanced_x_repressilator_canonical.json", confSSA, repeat, SequenceBenchmark.getTRTask().setSims(sims_for_memory_speedup_tests)).setC(c).setSeed(rand.nextLong()));
//            for (long ml : memory_limits) {
//                SegmentalConfig conf_i = new SegmentalConfig().setBaseConfig(confSSA).setMaxMemory(ml).setMemoryFunction(f);
//                bs.add(new SequenceBenchmark(timeout_sequence, "TR_Sequence_SEG+" + ml + "_" + f.abreviation() + "_SSA_" + c, "toggle_switch_advanced_x_repressilator_canonical.json", conf_i, repeat, SequenceBenchmark.getTRTask().setSims(sims_for_memory_speedup_tests)).setC(c).setSeed(rand.nextLong()));
//            }
//            bs.add(new SequenceBenchmark(timeout_sequence, "TR_Sequence_HYB", "toggle_switch_advanced_x_repressilator_canonical.json", confHYB, repeat, SequenceBenchmark.getTRTask().setSims(sims_for_memory_speedup_tests)).setC(c).setSeed(rand.nextLong()));
//            for (long ml : memory_limits) {
//                SegmentalConfig conf_i = new SegmentalConfig().setBaseConfig(confHYB).setMaxMemory(ml).setMemoryFunction(f);
//                bs.add(new SequenceBenchmark(timeout_sequence, "TR_Sequence_SEG+" + ml + "_" + f.abreviation() + "_HYB_" + c, "toggle_switch_advanced_x_repressilator_canonical.json", conf_i, repeat, SequenceBenchmark.getTRTask().setSims(sims_for_memory_speedup_tests)).setC(c).setSeed(rand.nextLong()));
//            }
//            bs.add(new SequenceBenchmark(timeout_sequence, "EC_Sequence_SSA", "ecoli_canonical.json", confSSA, repeat, SequenceBenchmark.getECTask().setSims(sims_for_memory_speedup_tests)).setC(c).setSeed(rand.nextLong()));
//            for (long ml : memory_limits) {
//                SegmentalConfig conf_i = new SegmentalConfig().setBaseConfig(confSSA).setMaxMemory(ml).setMemoryFunction(f);
//                bs.add(new SequenceBenchmark(timeout_sequence, "EC_Sequence_SEG+" + ml + "_" + f.abreviation() + "_SSA_" + c, "ecoli_canonical.json", conf_i, repeat, SequenceBenchmark.getECTask().setSims(sims_for_memory_speedup_tests)).setC(c).setSeed(rand.nextLong()));
//            }
//            bs.add(new SequenceBenchmark(timeout_sequence, "EC_Sequence_HYB", "ecoli_canonical.json", confHYB, repeat, SequenceBenchmark.getECTask().setSims(sims_for_memory_speedup_tests)).setC(c).setSeed(rand.nextLong()));
//            for (long ml : memory_limits) {
//                SegmentalConfig conf_i = new SegmentalConfig().setBaseConfig(confHYB).setMaxMemory(ml).setMemoryFunction(f);
//                bs.add(new SequenceBenchmark(timeout_sequence, "EC_Sequence_SEG+" + ml + "_" + f.abreviation() + "_HYB_" + c, "ecoli_canonical.json", conf_i, repeat, SequenceBenchmark.getECTask().setSims(sims_for_memory_speedup_tests)).setC(c).setSeed(rand.nextLong()));
//            }

//            bs.add(new SequenceBenchmark(timeout_sequence, "VI_Sequence_SSA", "viral_withV_canonical.json", confSSA, repeat, SequenceBenchmark.getVITask()).setC(c).setSeed(rand.nextLong()));
//            if (doSeg) bs.add(new SequenceBenchmark(timeout_sequence, "VI_Sequence_SEG+1GB_" + f.abreviation() + "_SSA_" + c, "viral_withV_canonical.json", confSEG_SSA, repeat, SequenceBenchmark.getVITask()).setC(c).setSeed(rand.nextLong()));
//            if (doSeg) bs.add(new SequenceBenchmark(timeout_sequence, "VI_Sequence_SEG+1GB_" + f.abreviation() + "_HYB_" + c, "viral_withV_canonical.json", confSEG_HYB, repeat, SequenceBenchmark.getVITask()).setC(c).setSeed(rand.nextLong()));
//            if (doHybrid) bs.add(new SequenceBenchmark(timeout_sequence, "VI_Sequence_HYB_" + c, "viral_withV_canonical.json", confHYB, repeat, SequenceBenchmark.getVITask()).setC(c).setSeed(rand.nextLong()));
//            if (doTau) bs.add(new SequenceBenchmark(timeout_sequence, "VI_Sequence_TAU_" + c, "viral_withV_canonical.json", confTAU, repeat, SequenceBenchmark.getVITask()).setC(c).setSeed(rand.nextLong()));
//
//            bs.add(new SequenceBenchmark(timeout_sequence, "PP_Sequence_SSA", "predator_prey_canonical.json", confSSA, repeat, SequenceBenchmark.getPPTask()).setC(c).setSeed(rand.nextLong()));
//            if (doSeg) bs.add(new SequenceBenchmark(timeout_sequence, "PP_Sequence_SEG+1GB_" + f.abreviation() + "_SSA_" + c, "predator_prey_canonical.json", confSEG_SSA, repeat, SequenceBenchmark.getPPTask()).setC(c).setSeed(rand.nextLong()));
//            if (doSeg) bs.add(new SequenceBenchmark(timeout_sequence, "PP_Sequence_SEG+1GB_" + f.abreviation() + "_HYB_" + c, "predator_prey_canonical.json", confSEG_HYB, repeat, SequenceBenchmark.getPPTask()).setC(c).setSeed(rand.nextLong()));
//            if (doHybrid) bs.add(new SequenceBenchmark(timeout_sequence, "PP_Sequence_HYB_" + c, "predator_prey_canonical.json", confHYB, repeat, SequenceBenchmark.getPPTask()).setC(c).setSeed(rand.nextLong()));
//            if (doTau) bs.add(new SequenceBenchmark(timeout_sequence, "PP_Sequence_TAU_" + c, "predator_prey_canonical.json", confTAU, repeat, SequenceBenchmark.getPPTask()).setC(c).setSeed(rand.nextLong()));
//
//            bs.add(new SequenceBenchmark(timeout_sequence, "TS_Sequence_SSA", "toggle_switch_advanced_canonical.json", confSSA, repeat, SequenceBenchmark.getTSTask()).setC(c).setSeed(rand.nextLong()));
//            if (doSeg) bs.add(new SequenceBenchmark(timeout_sequence, "TS_Sequence_SEG+1GB_" + f.abreviation() + "_SSA_" + c, "toggle_switch_advanced_canonical.json", confSEG_SSA, repeat, SequenceBenchmark.getTSTask()).setC(c).setSeed(rand.nextLong()));
//            if (doSeg) bs.add(new SequenceBenchmark(timeout_sequence, "TS_Sequence_SEG+1GB_" + f.abreviation() + "_HYB_" + c, "toggle_switch_advanced_canonical.json", confSEG_HYB, repeat, SequenceBenchmark.getTSTask()).setC(c).setSeed(rand.nextLong()));
//            if (doHybrid) bs.add(new SequenceBenchmark(timeout_sequence, "TS_Sequence_HYB_" + c, "toggle_switch_advanced_canonical.json", confHYB, repeat, SequenceBenchmark.getTSTask()).setC(c).setSeed(rand.nextLong()));
//            if (doTau) bs.add(new SequenceBenchmark(timeout_sequence, "TS_Sequence_TAU_" + c, "toggle_switch_advanced_canonical.json", confTAU, repeat, SequenceBenchmark.getTSTask()).setC(c).setSeed(rand.nextLong()));
//
//            bs.add(new SequenceBenchmark(timeout_sequence, "RP_Sequence_SSA", "repressilator_canonical.json", confSSA, repeat, SequenceBenchmark.getRPTask()).setC(c).setSeed(rand.nextLong()));
//            if (doSeg) bs.add(new SequenceBenchmark(timeout_sequence, "RP_Sequence_SEG+1GB_" + f.abreviation() + "_SSA_" + c, "repressilator_canonical.json", confSEG_SSA, repeat, SequenceBenchmark.getRPTask()).setC(c).setSeed(rand.nextLong()));
//            if (doSeg) bs.add(new SequenceBenchmark(timeout_sequence, "RP_Sequence_SEG+1GB_" + f.abreviation() + "_HYB_" + c, "repressilator_canonical.json", confSEG_HYB, repeat, SequenceBenchmark.getRPTask()).setC(c).setSeed(rand.nextLong()));
//            if (doHybrid) bs.add(new SequenceBenchmark(timeout_sequence, "RP_Sequence_HYB_" + c, "repressilator_canonical.json", confHYB, repeat, SequenceBenchmark.getRPTask()).setC(c).setSeed(rand.nextLong()));
//            if (doTau) bs.add(new SequenceBenchmark(timeout_sequence, "RP_Sequence_TAU_" + c, "repressilator_canonical.json", confTAU, repeat, SequenceBenchmark.getRPTask()).setC(c).setSeed(rand.nextLong()));

            bs.add(new SequenceBenchmark(timeout_sequence, "EC_Sequence_SSA", "ecoli_canonical.json", confSSA, repeat, SequenceBenchmark.getECTask()).setC(c).setSeed(rand.nextLong()));
            if (doSeg) bs.add(new SequenceBenchmark(timeout_sequence, "EC_Sequence_SEG+1GB_" + f.abreviation() + "_SSA_" + c, "ecoli_canonical.json", confSEG_SSA, repeat, SequenceBenchmark.getECTask()).setC(c).setSeed(rand.nextLong()));
            if (doSeg) bs.add(new SequenceBenchmark(timeout_sequence, "EC_Sequence_SEG+1GB_" + f.abreviation() + "_HYB_" + c, "ecoli_canonical.json", confSEG_HYB, repeat, SequenceBenchmark.getECTask()).setC(c).setSeed(rand.nextLong()));
//            if (doSeg) bs.add(new SequenceBenchmark(timeout_sequence, "EC_Sequence_SEG+1GB_" + f.abreviation() + "_SSA_" + c + "_nA", "ecoli_canonical.json", confSEG_SSA_nonadapting, repeat, SequenceBenchmark.getECTask()).setC(c).setSeed(rand.nextLong()));
//            if (doSeg) bs.add(new SequenceBenchmark(timeout_sequence, "EC_Sequence_SEG+1GB_" + f.abreviation() + "_HYB_" + c + "_nA", "ecoli_canonical.json", confSEG_HYB_nonadapting, repeat, SequenceBenchmark.getECTask()).setC(c).setSeed(rand.nextLong()));
            if (doHybrid) bs.add(new SequenceBenchmark(timeout_sequence, "EC_Sequence_HYB" + c, "ecoli_canonical.json", confHYB, repeat, SequenceBenchmark.getECTask()).setC(c).setSeed(rand.nextLong()));
            if (doTau) bs.add(new SequenceBenchmark(timeout_sequence, "EC_Sequence_TAU" + c, "ecoli_canonical.json", confTAU, repeat, SequenceBenchmark.getECTask()).setC(c).setSeed(rand.nextLong()));

            bs.add(new SequenceBenchmark(timeout_sequence, "TR_Sequence_SSA", "toggle_switch_advanced_x_repressilator_canonical.json", confSSA, repeat, SequenceBenchmark.getTRTask()).setC(c).setSeed(rand.nextLong()));
            if (doSeg) bs.add(new SequenceBenchmark(timeout_sequence, "TR_Sequence_SEG+1GB_" + f.abreviation() + "_SSA_" + c, "toggle_switch_advanced_x_repressilator_canonical.json", confSEG_SSA, repeat, SequenceBenchmark.getTRTask()).setC(c).setSeed(rand.nextLong()));
            if (doSeg) bs.add(new SequenceBenchmark(timeout_sequence, "TR_Sequence_SEG+1GB_" + f.abreviation() + "_HYB_" + c, "toggle_switch_advanced_x_repressilator_canonical.json", confSEG_HYB, repeat, SequenceBenchmark.getTRTask()).setC(c).setSeed(rand.nextLong()));
            if (doHybrid) bs.add(new SequenceBenchmark(timeout_sequence, "TR_Sequence_HYB" + c, "toggle_switch_advanced_x_repressilator_canonical.json", confHYB, repeat, SequenceBenchmark.getTRTask()).setC(c).setSeed(rand.nextLong()));
            if (doTau) bs.add(new SequenceBenchmark(timeout_sequence, "TR_Sequence_TAU" + c, "toggle_switch_advanced_x_repressilator_canonical.json", confTAU, repeat, SequenceBenchmark.getTRTask()).setC(c).setSeed(rand.nextLong()));
        } 
        if (doSequence) {
            bs.add(new SequenceBenchmark(timeout_sequence, "VI_Sequence_SSA", "viral_withV_canonical.json", confSSA, 1, SequenceBenchmark.toBaseTask(SequenceBenchmark.getVISequenceTasks())).setC(c).setSeed(rand.nextLong()));
            bs.add(new SequenceBenchmark(timeout_sequence, "VI_Sequence_SEG+1GB_" + f.abreviation() + "_SSA_" + c, "viral_withV_canonical.json", confSEG_SSA, repeat, SequenceBenchmark.getVISequenceTasks()).setC(c).setSeed(rand.nextLong()));
            bs.add(new SequenceBenchmark(timeout_sequence, "VI_Sequence_SEG+1GB_" + f.abreviation() + "_HYB_" + c, "viral_withV_canonical.json", confSEG_HYB, repeat, SequenceBenchmark.getVISequenceTasks()).setC(c).setSeed(rand.nextLong()));

            bs.add(new SequenceBenchmark(timeout_sequence, "PP_Sequence_SSA", "predator_prey_canonical.json", confSSA, 1, SequenceBenchmark.toBaseTask(SequenceBenchmark.getPPSequenceTasks())).setC(c).setSeed(rand.nextLong()));
            bs.add(new SequenceBenchmark(timeout_sequence, "PP_Sequence_SEG+1GB_" + f.abreviation() + "_SSA_" + c, "predator_prey_canonical.json", confSEG_SSA, repeat, SequenceBenchmark.getPPSequenceTasks()).setC(c).setSeed(rand.nextLong()));
            bs.add(new SequenceBenchmark(timeout_sequence, "PP_Sequence_SEG+1GB_" + f.abreviation() + "_HYB_" + c, "predator_prey_canonical.json", confSEG_HYB, repeat, SequenceBenchmark.getPPSequenceTasks()).setC(c).setSeed(rand.nextLong()));

            bs.add(new SequenceBenchmark(timeout_sequence, "TS_Sequence_SSA", "toggle_switch_advanced_canonical.json", confSSA, 1, SequenceBenchmark.toBaseTask(SequenceBenchmark.getTSSequenceTasks())).setC(c).setSeed(rand.nextLong()));
            bs.add(new SequenceBenchmark(timeout_sequence, "TS_Sequence_SEG+1GB_" + f.abreviation() + "_SSA_" + c, "toggle_switch_advanced_canonical.json", confSEG_SSA, repeat, SequenceBenchmark.getTSSequenceTasks()).setC(c).setSeed(rand.nextLong()));
            bs.add(new SequenceBenchmark(timeout_sequence, "TS_Sequence_SEG+1GB_" + f.abreviation() + "_HYB_" + c, "toggle_switch_advanced_canonical.json", confSEG_HYB, repeat, SequenceBenchmark.getTSSequenceTasks()).setC(c).setSeed(rand.nextLong()));

            bs.add(new SequenceBenchmark(timeout_sequence, "RP_Sequence_SSA", "repressilator_canonical.json", confSSA, 1, SequenceBenchmark.toBaseTask(SequenceBenchmark.getRPSequenceTasks())).setC(c).setSeed(rand.nextLong()));
            bs.add(new SequenceBenchmark(timeout_sequence, "RP_Sequence_SEG+1GB_" + f.abreviation() + "_SSA_" + c, "repressilator_canonical.json", confSEG_SSA, repeat, SequenceBenchmark.getRPSequenceTasks()).setC(c).setSeed(rand.nextLong()));
            bs.add(new SequenceBenchmark(timeout_sequence, "RP_Sequence_SEG+1GB_" + f.abreviation() + "_HYB_" + c, "repressilator_canonical.json", confSEG_HYB, repeat, SequenceBenchmark.getRPSequenceTasks()).setC(c).setSeed(rand.nextLong()));

            bs.add(new SequenceBenchmark(timeout_sequence, "EC_Sequence_SSA", "ecoli_canonical.json", confSSA, 1, SequenceBenchmark.toBaseTask(SequenceBenchmark.getECSequenceTasks())).setC(c).setSeed(rand.nextLong()));
            bs.add(new SequenceBenchmark(timeout_sequence, "EC_Sequence_SEG+1GB_" + f.abreviation() + "_SSA_" + c, "ecoli_canonical.json", confSEG_SSA, repeat, SequenceBenchmark.getECSequenceTasks()).setC(c).setSeed(rand.nextLong()));
            bs.add(new SequenceBenchmark(timeout_sequence, "EC_Sequence_SEG+1GB_" + f.abreviation() + "_HYB_" + c, "ecoli_canonical.json", confSEG_HYB, repeat, SequenceBenchmark.getECSequenceTasks()).setC(c).setSeed(rand.nextLong()));
        }

        
        
        bs.sort((Benchmark o1, Benchmark o2) -> {
            return o1.name.compareTo(o2.name);
        });
        
        System.out.println("Generated " + bs.size() + " benchmarks...");
        System.out.println("");
        for (Benchmark b : bs) System.out.println(b.name);
        System.out.println("");
        
        IO.writeStringToFile(IO.DEFAULT_BENCHMARK_FILE, Benchmark.toJsonList(bs.toArray(Benchmark[]::new)));
        
        IO.CURRENT_BENCHMARK_FOLDER = new File(IO.BENCHMARK_FOLDER, "20230520_visual");
//        IO.CURRENT_BENCHMARK_FOLDER = new File(IO.BENCHMARK_FOLDER, "tmp");
        Benchmarking.main(args);
    }
    
    public static ArrayList<Benchmark> generateBenchmarks(String name, String example, double end_t, double[] cs, long timeout_visual, long timeout_performance, long timeout_transient) {
        ArrayList<Benchmark> res = new ArrayList<>();
        
        HashMap<String, SimulatorConfig> configs = new HashMap<>();
        configs.put("SSA", confSSA);
//        configs.put("TAU", confTAU);
        configs.put("HYB", confHYB);

//        SimulatorConfig[] seg_base_configs = new SimulatorConfig[]{confSSA};
        HashMap<String, SimulatorConfig> seg_base_configs = new HashMap<>();
        seg_base_configs.put("SSA", confSSA);
        seg_base_configs.put("HYB", confHYB);
        
        HashMap<String, Long> seg_mem = new HashMap<>();
//        seg_mem.put("40MB", 1000l * 1000 * 40);
        seg_mem.put("+5GB", 1000l * 1000 * 5000);
//        seg_mem.put("++4GB", 1000l * 1000 * 4000);
//        String pluses = "";
//        for (long mem = 1000l * 1000 * 80; mem <= 1000l * 1000 * 1280; mem*=4) {
//            String readable = IO.humanReadableByteCountSI(mem);
//            readable = readable.replace(" ", "");
//            readable = readable.replace(".0", "");
////            seg_mem.put(pluses + readable, mem);
//            pluses = pluses + "+";
//        }
        HashMap<String, MemoryFunction> seg_func = new HashMap<>();
        seg_func.put("g"+(GrowingMemoryFunction.normal().s), GrowingMemoryFunction.normal());
//        seg_func.put("g"+(GrowingMemoryFunction.precise().s), GrowingMemoryFunction.precise());
//        seg_func.put("g"+(GrowingMemoryFunction.imprecise().s), GrowingMemoryFunction.imprecise());
//        seg_func.put("k-1", new ConstantMemoryFunction(-1));
//        seg_func.put("k10", new ConstantMemoryFunction(10));
//        seg_func.put("k100", new ConstantMemoryFunction(100));
//        seg_func.put("k1000", new ConstantMemoryFunction(1000));
//        seg_func.put("kinf", new LinearMemoryFunction(1));
        
        for(String baseConfLabel : seg_base_configs.keySet()) {
            SimulatorConfig baseConf = seg_base_configs.get(baseConfLabel);
            for (String memLabel : seg_mem.keySet()) {
                long mem = seg_mem.get(memLabel);
                for (String funcLabel : seg_func.keySet()) {
                    MemoryFunction f = seg_func.get(funcLabel);
                    SegmentalConfig conf = new SegmentalConfig().setBaseConfig(baseConf).setMaxMemory(mem);
                    if (f != null) conf.setMemoryFunction(f);
                    configs.put("SEG" + memLabel + "_" + funcLabel + "_" + baseConfLabel, conf);
                }
            }
        }
        
        HashMap<String, SimulatorConfig> configDependingOnC = new HashMap<>();
        HashMap<String, SimulatorConfig> configNotDependingOnC = new HashMap<>();
        
        for (String configLabel : configs.keySet()) {
            SimulatorConfig conf = configs.get(configLabel);
            if (conf.createSimulator(new Setting()).dependsOnC()) {
                configDependingOnC.put(configLabel, conf);
            } else {
                configNotDependingOnC.put(configLabel, conf);
            }
        }
        
        for (String configLabel : configNotDependingOnC.keySet()) {
            SimulatorConfig conf = configs.get(configLabel);
            if (doVisual) res.add(new VisualBenchmark(
                    timeout_visual, 
                    name + "_Visual_" + configLabel, 
                    example, 
                    end_t, 
                    conf, 
                    times_visual,
                    repeat_visual
            ).setSeed(rand.nextLong()));
        }
        
        for (double c : cs) {
            for (String configLabel : configDependingOnC.keySet()) {
                SimulatorConfig conf = configs.get(configLabel);
                if (doVisual) res.add(new VisualBenchmark(
                        timeout_visual, 
                        name + "_Visual_" + configLabel + "_" + c, 
                        example, 
                        end_t, 
                        conf, 
                        conf.createSimulator(new Setting()).isDeterministic() ? 1: times_visual,
                        conf.createSimulator(new Setting()).isDeterministic() ? 1: repeat_visual
                ).setC(c).setSeed(rand.nextLong()));
            }
        }
        
        return res;
    }
}
