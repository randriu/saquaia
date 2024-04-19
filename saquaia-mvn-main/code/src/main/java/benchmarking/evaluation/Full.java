package benchmarking.evaluation;

import benchmarking.Benchmark;
import benchmarking.NewTransientAnalysisBenchmark;
import benchmarking.simulatorconfiguration.SimulatorConfig;
import core.analysis.TransientAnalysisResult;
import core.model.Distribution;
import core.model.Setting;
import core.util.DoubleArrayWrapper;
import core.util.EMD_Helper;
import core.util.IO;
import core.util.IntArrayWrapper;
import core.util.Msc;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import plotting.Plotter;

/**
 *
 * @author Martin
 */
public class Full {

    public static void main(String[] args) {
        // folder to be analy
        File benchmark_output = IO.getMostRecentBenchmarkOutputFolder();
//        File benchmark_output = new File(IO.BENCHMARK_FOLDER, "20230216_test");
        evaluate(benchmark_output);
    }

    public static void evaluate(File benchmark_output) {
        evaluateSpeedAndEMDAndMemory(benchmark_output);

        System.out.println("");
        System.out.println("");
        System.out.println("performing model specific accuracy evaluation (THIS MAY TAKE A WHILE!!! PLEASE WAIT...)");

        // use python script for model specific metrics
        File python_script = new File(new File(new File(IO.CODE_FOLDER, "scripts"), "python"), "qualitative_props.py");

        try {
            Process process = new ProcessBuilder(
                    "python",
                    python_script.getAbsolutePath(),
                    benchmark_output.getAbsolutePath()
            ).inheritIO().start();
            process.waitFor();
        } catch (IOException ex) {
            Logger.getLogger(Full.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(Full.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("finished model specific accuracy evaluation...)");
    }

    public static void evaluateSpeedAndEMDAndMemory(File benchmark_output) {
        System.out.println();
        System.out.println("##################################");
        System.out.println("#### Speed and EMD evaluation ####");
        System.out.println("##################################");
        System.out.println("folder: " + benchmark_output.getAbsolutePath());
        System.out.println("");

        // mapping from abreviation to files
        HashMap<String, ArrayList<File>> files = new HashMap<>();

        // collect files for each abreviation
        for (final File f : benchmark_output.listFiles()) {
            if (!f.isDirectory() || !Benchmark.getBenchmarkFile(f).exists() || !Benchmark.getResultFile(f).exists()) {
                continue;
            }
            Benchmark b = Benchmark.fromJSON(IO.getContentOfFile(Benchmark.getBenchmarkFile(f)));
            if (b.type != Benchmark.Type.TRANSIENTNEW) {
                continue;
            }
            String[] f_name_splitted = f.getName().split("_");
            if (f_name_splitted.length <= 1) {
                continue;
            }
            String abr = f_name_splitted[0];

            if (!files.containsKey(abr)) {
                files.put(abr, new ArrayList<>());
            }
            files.get(abr).add(f);
        }

        System.out.println("Found the following " + files.size() + " settings: ");
        for (String abr : files.keySet()) {
            System.out.println("  - " + abr + files.get(abr).size());
            for (File f : files.get(abr)) {
                System.out.println("    * " + f.getName());
            }
        }

        for (String abr : files.keySet()) {
            System.out.println();
            System.out.println("Evaluating " + abr);
            evaluateSpeedAndEMDAndMemory(files.get(abr));
        }
    }

    public static void evaluateSpeedAndEMDAndMemory(ArrayList<File> files) {
        // find base file
        File file_base = null;
        for (File f : files) {
            Benchmark b = Benchmark.fromJSON(IO.getContentOfFile(Benchmark.getBenchmarkFile(f)));
            if (file_base == null) {
                file_base = f;
                continue;
            }
            NewTransientAnalysisBenchmark tab = (NewTransientAnalysisBenchmark) b;
            if (tab.simconf.type == SimulatorConfig.Type.SSA) {
                file_base = f;
            }
        }

        double base_speed = Double.NaN;
        Distribution<DoubleArrayWrapper> d_base = null;
        if (file_base == null) {
            System.out.println("Did not find a base file!");
        } else {
            System.out.println("base file: " + file_base.getName());
            File base_benchmark_file = Benchmark.getBenchmarkFile(file_base);
            NewTransientAnalysisBenchmark b = (NewTransientAnalysisBenchmark) Benchmark.fromJSON(IO.getContentOfFile(base_benchmark_file));
            TransientAnalysisResult[] res = b.loadResult(IO.getContentOfFile(b.getResultFile(file_base)));
            base_speed = getAverageSimulationSpeed(res);
            int base_results = getTotalNumberOfResults(res);
            System.out.println("    total simulations: " + base_results);
            System.out.println("    time per simulation: " + IO.humanReadableDuration((long) base_speed));
            d_base = getAverageDistribution(res);
        }

        // evaluate
        for (File f : files) {
            System.out.println("");
            System.out.println(f.getName());

            File benchmark_file = Benchmark.getBenchmarkFile(f);
            NewTransientAnalysisBenchmark b = (NewTransientAnalysisBenchmark) Benchmark.fromJSON(IO.getContentOfFile(benchmark_file));
            TransientAnalysisResult[] res = b.loadResult(IO.getContentOfFile(b.getResultFile(f)));
            System.out.print("    repeated: " + res.length);
            res = NewTransientAnalysisBenchmark.filterFinishedResults(res);
            System.out.println(" (" + res.length + " finished and used for average)");
            Setting setting = b.setting;
            setting.recomputeIntervals(true);   // we want to compute the EMD for all species, --> split output species into levels
            double speed = getAverageSimulationSpeed(res);
            Distribution<DoubleArrayWrapper> d = getAverageDistribution(res);
            int sims = res[0].results;
            System.out.println("    simulations: " + sims);
            System.out.println("    memory: " + NewTransientAnalysisBenchmark.getAverageStats(res, true));
            System.out.println("    time per simulation: " + IO.humanReadableDuration((long) speed));
            System.out.println("    speedup: " + IO.significantFigures(base_speed / speed));

            Distribution<IntArrayWrapper> d_abs = Distribution.convertFromConcreteStatesToIntervalStates(d, setting);
            Distribution<IntArrayWrapper> d_base_abs = Distribution.convertFromConcreteStatesToIntervalStates(d_base, setting);
            double[] levelEMDs = Distribution.earth_mover_distances(d_abs, d_base_abs);
            System.out.println("    level EMD per dimension: " + Arrays.toString(levelEMDs));
            System.out.println("    total level EMD: " + IO.significantFigures(EMD_Helper.summed(levelEMDs)));

            XYSeries series_speed = new XYSeries(f.getName());
            XYSeries series_speedup = new XYSeries(f.getName());
            // speed evolution
            for (int simulation_i = 0; simulation_i < sims; simulation_i++) {
                double sum = 0;
                int summed = 0;
                for (int repeat_i = 0; repeat_i < res.length; repeat_i++) {
                    TransientAnalysisResult r = res[repeat_i];
                    sum += r.avg_comp_time(simulation_i);
                    summed++;
                }

                if (summed > 0) {
                    if (simulation_i % 100 != 0 && !Msc.isPowerOf2(simulation_i) && simulation_i != sims - 1) {
                        continue;
                    }
                    double average = sum / summed;
                    double speedup = base_speed / average;
                    series_speed.add(simulation_i + 1, 1.0 * average / 1000_000_000l);
                    series_speedup.add(simulation_i + 1, speedup);
                }
            }
            XYSeriesCollection dataset_speed = new XYSeriesCollection();
            XYSeriesCollection dataset_speedup = new XYSeriesCollection();
            dataset_speed.addSeries(series_speed);
            dataset_speedup.addSeries(series_speedup);
            JFreeChart chart = ChartFactory.createXYLineChart("comp. time",
                    "#sims", "avg. time to compute 1 simulation (in s)", dataset_speed);
            Plotter.betterColors(chart);
            Plotter.saveAsPNG(chart, new File(f, "comp_time.png"));
            chart = ChartFactory.createXYLineChart("speedup factor",
                    "#sims", "speedup over base simulation", dataset_speedup);
            Plotter.betterColors(chart);
            File file_speedup_evolution = new File(f, "speedup.png");
            Plotter.saveAsPNG(chart, file_speedup_evolution);
            System.out.println("    speedup evolution: " + file_speedup_evolution.getAbsolutePath());
        }

    }

    public static int getTotalNumberOfResults(TransientAnalysisResult[] results) {
        results = NewTransientAnalysisBenchmark.filterFinishedResults(results);
        int s = 0;
        for (TransientAnalysisResult r : results) {
            s += r.results;
        }
        return s;
    }

    public static double getAverageSimulationSpeed(TransientAnalysisResult[] results) {
        results = NewTransientAnalysisBenchmark.filterFinishedResults(results);
        double sum = 0;
        int summed = 0;
        for (TransientAnalysisResult r : results) {
            double x = r.avg_comp_time();
            if (x > 0) {
                sum += x;
                summed++;
            }
        }
        if (summed == 0) {
            return Double.NaN;
        }
        return sum / summed;
    }

    public static Distribution<DoubleArrayWrapper> getAverageDistribution(TransientAnalysisResult[] results) {
        results = NewTransientAnalysisBenchmark.filterFinishedResults(results);
        Distribution<DoubleArrayWrapper> d = new Distribution<>();
        for (TransientAnalysisResult r : results) {
            for (double[] state : r.states) {
                d.add(new DoubleArrayWrapper(state), 1);
            }
        }
        d.normalize();
        return d;
    }
}
