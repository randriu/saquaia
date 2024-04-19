package benchmarking.generators;

import benchmarking.Benchmark;
import benchmarking.NewTransientAnalysisBenchmark;
import benchmarking.VisualBenchmark;
import static benchmarking.generators.Benchmarks.rand;
import benchmarking.simulatorconfiguration.*;
import core.model.Setting;
import core.util.IO;
import core.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Martin
 */
public class ArtifactBenchmarks {

    public static void main(String[] args) {
        Long seed = 42l;
        rand.setSeed(seed);

        double c = 1.5;

        SSAConfig confSSA = new SSAConfig();
        SSAConfig confSSAcontrol = new SSAConfig();
        TAUConfig confTAU = new TAUConfig();
        HybridConfig confHYB = new HybridConfig();
        SegmentalConfig confSEGSSA = new SegmentalConfig().setBaseConfig(confSSA);
        SegmentalConfig confSEGHYB = new SegmentalConfig().setBaseConfig(confHYB);

        HashMap<SimulatorConfig, String> confNames = new HashMap<>();
        confNames.put(confSSA, "SSA");
        confNames.put(confSSAcontrol, "SSA2");
        confNames.put(confTAU, "TAU");
        confNames.put(confHYB, "HYB");
        confNames.put(confSEGSSA, "SEGSSA");
        confNames.put(confSEGHYB, "SEGHYB");

        // models
        HashMap<String, Double> end_times = new HashMap<>();
        HashMap<String, String> example_files = new HashMap<>();

        end_times.put("PP", 200.0);
        example_files.put("PP", "predator_prey_canonical.json");

        end_times.put("VI", 200.0);
        example_files.put("VI", "viral_withV_canonical.json");

        end_times.put("TS", 50000.0);
        example_files.put("TS", "toggle_switch_advanced_canonical.json");

        end_times.put("RP", 50000.0);
        example_files.put("RP", "repressilator_canonical.json");

        end_times.put("EC", 2000.0);
        example_files.put("EC", "ecoli_canonical.json");

        end_times.put("TR", 50000.0);
        example_files.put("TR", "toggle_switch_advanced_x_repressilator_canonical.json");

        // build benchmarks
        ArrayList<Benchmark> bs = new ArrayList<>();

        // full simulations: for plots and model specific property analysis
        String[] models_for_plots = new String[]{"PP", "VI", "TS", "RP", "EC", "TR"};
//        String[] models_for_plots = new String[]{"PP"};
        SimulatorConfig[] configs_for_plots = new SimulatorConfig[]{confSSA, confTAU, confHYB, confSEGSSA, confSEGHYB};
        long timeout_plots = 1000 * 60 * 60 * 2;
        for (String abr : models_for_plots) {
            for (SimulatorConfig conf : configs_for_plots) {
                String name = abr + "_Visual_" + confNames.get(conf);
                bs.add(
                        new VisualBenchmark(
                                timeout_plots, 
                                name, 
                                example_files.get(abr), 
                                end_times.get(abr), 
                                conf, 
                                1000, 
                                10
                        ).setC(c).setSeed(rand.nextLong())
                );
            }
        }

        // accuracy and speed evaluation
        String[] models_for_acc_and_speed = new String[]{"PP", "VI", "TS", "RP", "EC", "TR"};
//        String[] models_for_acc_and_speed = new String[]{"PP"};
        SimulatorConfig[] configs_for_acc_and_speed = new SimulatorConfig[]{confSSA, confSSAcontrol, confTAU, confHYB, confSEGSSA, confSEGHYB};
        long timeout_acc_and_speed = 1000 * 60 * 60 * 24 * 2;
        for (String abr : models_for_acc_and_speed) {
            for (SimulatorConfig conf : configs_for_acc_and_speed) {
                String name = abr + "_" + confNames.get(conf);
                int repeat = conf.createSimulator(new Setting()).getMemory() != null ? 10 : 1;
                bs.add(
                        new NewTransientAnalysisBenchmark(
                                timeout_acc_and_speed,
                                name,
                                example_files.get(abr),
                                end_times.get(abr),
                                conf,
                                10000,
                                null,
                                repeat
                        ).setC(c).setSeed(rand.nextLong())
                );
            }
        }

        // memory and adaptiveness
        long[] memory_limits = new long[]{300_000_000, 600_000_000, 1_000_000_000, 2_000_000_000l, 3_500_000_000l, 5_000_000_000l};
        ArrayList<Pair<String,SimulatorConfig>> models_for_memory_with_base_config = new ArrayList<>();
        models_for_memory_with_base_config.add(new Pair<>("EC", confSSA));
        models_for_memory_with_base_config.add(new Pair<>("TR", confHYB));
//        models_for_memory_with_base_config.add(new Pair<>("PP", confHYB));
        long timeout_memory = 1000 * 60 * 60 * 8;
        for (Pair<String, SimulatorConfig> abr_and_base_conf : models_for_memory_with_base_config) {
            for (long memory_limit : memory_limits) {
                String abr = abr_and_base_conf.left;
                SimulatorConfig base_config = abr_and_base_conf.right;
                int sims = 2000;
                int repeat = 10;
                // adaptive
                SegmentalConfig conf = new SegmentalConfig().setBaseConfig(base_config).setMaxMemory(memory_limit);
                String name = abr + "_MEM_" + confNames.get(base_config) + "_" + IO.humanReadableByteCountSI(memory_limit);
                bs.add(
                        new NewTransientAnalysisBenchmark(
                                timeout_memory,
                                name,
                                example_files.get(abr),
                                end_times.get(abr),
                                conf,
                                sims,
                                null,
                                repeat
                        ).setC(c).setSeed(rand.nextLong())
                );
                // non-adaptive
                conf = new SegmentalConfig().setBaseConfig(base_config).setMaxMemory(memory_limit).setAdaptive(false);
                name = abr + "_MEM_" + confNames.get(base_config) + "_" + IO.humanReadableByteCountSI(memory_limit) + "_nonadaptive";
                bs.add(
                        new NewTransientAnalysisBenchmark(
                                timeout_memory,
                                name,
                                example_files.get(abr),
                                end_times.get(abr),
                                conf,
                                sims,
                                null,
                                repeat
                        ).setC(c).setSeed(rand.nextLong())
                );
            }
        }

        long total_timeout = 0;
        for (Benchmark b : bs) {
            total_timeout += b.timeout;
        }

        System.out.println("generated " + bs.size() + " benchmarks with total timeout " + IO.humanReadableDuration(total_timeout * 1000 * 1000));
        
        System.out.println("");
        for (Benchmark b : bs) System.out.println(b.name);
        System.out.println("");

        IO.writeStringToFile(IO.DEFAULT_BENCHMARK_FILE, Benchmark.toJsonList(bs.toArray(Benchmark[]::new)));
    }

}
