/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package benchmarking.evaluation;

import benchmarking.SequenceBenchmark;
import benchmarking.SequenceBenchmark.Task;
import benchmarking.SequenceBenchmark.TaskResult;
import core.util.IO;
import core.util.JSON;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import plotting.Plotter;

/**
 *
 * @author helfrich
 */
public class SequenceBenchmarkSpeedEvaluation {
    public static void main(String[] args) {
//        File benchmark_output = new File(IO.BENCHMARK_FOLDER, "tmp");
        File benchmark_output = new File(IO.BENCHMARK_FOLDER, "20230122_sequence");
        
        HashMap<String, ArrayList<File>> compare = new HashMap<>();
        
        File[] listFiles = benchmark_output.listFiles();
        
        for (final File f: listFiles) {
            if (!f.isDirectory()) continue;
            if (!f.getName().contains("Sequence")) continue;
            
            String setting_abr = f.getName().substring(0, 2);
            
            File result_file = new File(f, "result.json");
            if (!result_file.exists()) continue;
            if (!compare.containsKey(setting_abr)) compare.put(setting_abr, new ArrayList<>());
            compare.get(setting_abr).add(f);
        }
        
        for (String setting_abr : compare.keySet()) {
            
            System.out.println("");
            System.out.println(setting_abr +": " + compare.get(setting_abr).size() + " folders");
            
            File folder = new File(benchmark_output, setting_abr + "_SequenceSpeed");
            folder.mkdirs();
            
            
                
            // get base simulator speed
            File baseFile = null;
            for (File f: compare.get(setting_abr)) {
                if (f.getName().contains("Sequence_SSA")) baseFile = f;
            }
            if (baseFile == null) {
                System.out.println("Didn't find base file for " + setting_abr);
                continue;
            }
            // load json results
            SequenceBenchmark.TaskSequenceResult[] taskSequenceResult_base_repeated = JSON.getGson().fromJson(
                    IO.getContentOfFile(new File(baseFile, "result.json")), 
                    SequenceBenchmark.TaskSequenceResult[].class
            );
            
            // get average computation time for each task with base simulator
            int nr_of_tasks = taskSequenceResult_base_repeated[0].number_of_tasks();
            double[] avg_comp_times_in_s_of_base_simulator = new double[nr_of_tasks];
            for (int task_i = 0; task_i < nr_of_tasks; task_i++) {
                double comp_time_sum_in_s = 0;
                int repeats_in_sum = 0;
                for (int repeat_i = 0; repeat_i < taskSequenceResult_base_repeated.length; repeat_i++) {
                    TaskResult taskResult = taskSequenceResult_base_repeated[repeat_i].without_reset[task_i];
                    if (taskResult != null) {
                        comp_time_sum_in_s += taskResult.getFinalSnapshot().avg_comp_time_in_s();
                        repeats_in_sum++;
                    }
                }
                if (repeats_in_sum > 0) avg_comp_times_in_s_of_base_simulator[task_i] = comp_time_sum_in_s / repeats_in_sum;
            }
            
            System.out.println(Arrays.toString(avg_comp_times_in_s_of_base_simulator));
            
            // draw comparision
            XYSeriesCollection dataset = new XYSeriesCollection();
            
            boolean taskLineValuesDone = false;
            ArrayList<Integer> taskLineValues = new ArrayList<>();
            taskLineValues.add(0);
            
            for (File f : compare.get(setting_abr)) {
                
                if (f.getName().equals(baseFile.getName())) continue;
                
                System.out.println("");
                System.out.println(f.getName());
                
                XYSeries series_without_resets = new XYSeries(f.getName());
                XYSeries series_with_resets = new XYSeries(f.getName() + "_reset");
                
                boolean without_resets_is_not_empty = false;
                boolean with_resets_is_not_empty = false;
                
                // load json results
                SequenceBenchmark.TaskSequenceResult[] taskSequenceResult_repeated = JSON.getGson().fromJson(
                        IO.getContentOfFile(new File(f, "result.json")), 
                        SequenceBenchmark.TaskSequenceResult[].class
                );
                
                // without resets
                int sims_in_previous_tasks = 0;
                for (int task_i = 0; task_i < taskSequenceResult_repeated[0].number_of_tasks(); task_i++) {
                    if (taskSequenceResult_repeated[0].without_reset[task_i] == null) break;
                    Task task = taskSequenceResult_repeated[0].without_reset[task_i].task;
                    for (int simulation_i = 0; simulation_i < task.sims; simulation_i++) {
                        double sum = 0;
                        int summed = 0;
                        for (int repeat_i = 0; repeat_i < taskSequenceResult_repeated.length; repeat_i++) {
                            TaskResult taskResult = taskSequenceResult_repeated[repeat_i].without_reset[task_i];
                            if (taskResult == null || simulation_i >= taskResult.getFinalSnapshot().numberOfResults()) continue;
                            sum += taskResult.getFinalSnapshot().avg_comp_time_in_s(simulation_i);
//                            sum += taskResult.getFinalSnapshot().comp_time_in_s(simulation_i);
                            summed++;
                            if (simulation_i < 10 || simulation_i > task.sims - 10) System.out.println("without task_i=" + task_i + " simulation_i=" + simulation_i + " repeat_i=" + repeat_i + " avg_comp_time=" + taskResult.getFinalSnapshot().avg_comp_time_in_s(simulation_i));
                        }
                        
                        if (summed > 0) {
                            double average =  sum / summed;
                            series_without_resets.add(sims_in_previous_tasks + simulation_i + 1, avg_comp_times_in_s_of_base_simulator[task_i] / average);
                            without_resets_is_not_empty = true;
                            if (simulation_i < 10 || simulation_i > task.sims - 10) System.out.println("without task_i=" + task_i + " simulation_i=" + simulation_i + " avg_avg_comp_time=" + average + " base_avg=" + avg_comp_times_in_s_of_base_simulator[task_i] + " speedup=" + avg_comp_times_in_s_of_base_simulator[task_i] / average);
                        }
                    }
                    
                    sims_in_previous_tasks += task.sims;
                    if (!taskLineValuesDone) taskLineValues.add(sims_in_previous_tasks + 1);
                }
                if (without_resets_is_not_empty) dataset.addSeries(series_without_resets);
                taskLineValuesDone = true;
                
                
                // with resets
                sims_in_previous_tasks = 0;
                for (int task_i = 0; task_i < taskSequenceResult_repeated[0].number_of_tasks(); task_i++) {
                    if (taskSequenceResult_repeated[0].with_reset[task_i] == null) break;
                    Task task = taskSequenceResult_repeated[0].with_reset[task_i].task;
                    for (int simulation_i = 0; simulation_i < task.sims; simulation_i++) {
                        double sum = 0;
                        int summed = 0;
                        for (int repeat_i = 0; repeat_i < taskSequenceResult_repeated.length; repeat_i++) {
                            TaskResult taskResult = taskSequenceResult_repeated[repeat_i].with_reset[task_i];
                            if (taskResult == null || simulation_i >= taskResult.getFinalSnapshot().numberOfResults()) continue;
                            sum += taskResult.getFinalSnapshot().avg_comp_time_in_s(simulation_i);
//                            sum += taskResult.getFinalSnapshot().comp_time_in_s(simulation_i);
                            summed++;
                            if (simulation_i < 10 || simulation_i > task.sims - 10) System.out.println("with task_i=" + task_i + " simulation_i=" + simulation_i + " repeat_i=" + repeat_i + " avg_comp_time=" + taskResult.getFinalSnapshot().avg_comp_time_in_s(simulation_i));
                        }
                        
                        if (summed > 0) {
                            double average =  sum / summed;
                            series_with_resets.add(sims_in_previous_tasks + simulation_i + 1, avg_comp_times_in_s_of_base_simulator[task_i] / average);
                            with_resets_is_not_empty = true;
                            if (simulation_i < 10 || simulation_i > task.sims - 10) System.out.println("with task_i=" + task_i + " simulation_i=" + simulation_i + " avg_avg_comp_time=" + average + " base_avg=" + avg_comp_times_in_s_of_base_simulator[task_i] + " speedup=" + avg_comp_times_in_s_of_base_simulator[task_i] / average);
                        }
                    }
                    
                    sims_in_previous_tasks += task.sims;
                }
                if (with_resets_is_not_empty) dataset.addSeries(series_with_resets);
//                
//                
//                
//                    
//                    
//                    for (int repeat_i = 0; repeat_i < taskSequenceResult_repeated.length; repeat_i++) {
//                        TaskResult taskResult = taskSequenceResult_repeated[repeat_i].without_reset[task_i];
//                        
//                        if (taskResult != null) {
//                            SimulationResults finalSnapshot = taskResult.getFinalSnapshot();
//                            for (int simulation_i = 0; simulation_i < finalSnapshot.numberOfResults(); simulation_i++) {
//                                long comp_time = finalSnapshot.comp_times.get(simulation_i);
//                                sum += comp_time;
//                                double comp_time_in_s = 1.0 * comp_time /  1000_000_000l;
//                                double avg_comp_time_in_s = 1.0 * sum / (simulation_i + 1) / 1000_000_000l;
//                                System.out.println(f.getName() + " " +  + (simulation_i + 1) + " " + avg_comp_time_in_s);
//                                series_without_resets.add(sims_in_previous_tasks + simulation_i + 1, avg_comp_times_in_s_of_base_simulator[task_i] / avg_comp_time_in_s);
//                            }
//                            comp_time_sum_in_s += taskResult.getFinalSnapshot().avg_comp_time_in_s();
//                            repeats_in_sum++;
//                        }
//                    }
//                    if (repeats_in_sum > 0) avg_comp_times_in_s_of_base_simulator[task_i] = comp_time_sum_in_s / repeats_in_sum;
//                    
//                    
//                    TaskResult taskResult = taskSequenceResult.without_reset[task_i];
//                    if (taskResult == null) break;
//                    without_resets_is_not_empty = true;
//                    
//                    SequenceBenchmark.SimulationResults simulationResults = taskResult.snapshots[taskResult.snapshots.length-1];
//                    
//                    long sum = 0;
//                    for (int simulation_i = 0; simulation_i < simulationResults.numberOfResults(); simulation_i++) {
//                        long comp_time = simulationResults.comp_times.get(simulation_i);
//                        sum += comp_time;
//                        double comp_time_in_s = 1.0 * comp_time /  1000_000_000l;
//                        double avg_comp_time_in_s = 1.0 * sum / (simulation_i + 1) / 1000_000_000l;
//                        System.out.println(f.getName() + " " +  + (simulation_i + 1) + " " + avg_comp_time_in_s);
//                        series_without_resets.add(sims_in_previous_tasks + simulation_i + 1, avg_comp_times_in_s_of_base_simulator[task_i] / avg_comp_time_in_s);
//                    }
//                    
//                    sims_in_previous_tasks += taskResult.task.sims;
//                }
//                if (without_resets_is_not_empty) dataset.addSeries(series_without_resets);
//                
//                // with resets
//                sims_in_previous_tasks = 0;
//                for (int task_i = 0; task_i < taskSequenceResult.number_of_tasks(); task_i++) {
//                    TaskResult taskResult = taskSequenceResult.with_reset[task_i];
//                    if (taskResult == null) break;
//                    with_resets_is_not_empty = true;
//                    
//                    SequenceBenchmark.SimulationResults simulationResults = taskResult.snapshots[taskResult.snapshots.length-1];
//                    
//                    long sum = 0;
//                    for (int simulation_i = 0; simulation_i < simulationResults.numberOfResults(); simulation_i++) {
//                        long comp_time = simulationResults.comp_times.get(simulation_i);
//                        sum += comp_time;
//                        double comp_time_in_s = 1.0 * comp_time /  1000_000_000l;
//                        double avg_comp_time_in_s = 1.0 * sum / (simulation_i + 1) / 1000_000_000l;
//                        series_with_resets.add(sims_in_previous_tasks + simulation_i + 1, avg_comp_times_in_s_of_base_simulator[task_i] / avg_comp_time_in_s);
//                    }
//                    
//                    sims_in_previous_tasks += taskResult.task.sims;
//                }
//                if (with_resets_is_not_empty) dataset.addSeries(series_with_resets);
//                
            }
            
            JFreeChart chart = ChartFactory.createXYLineChart("comp. time",
                    "#sims", "speedup", dataset);
            
            for (int i = 0; i < taskLineValues.size()-1; i++) {
                chart.getXYPlot().addDomainMarker(new ValueMarker(taskLineValues.get(i)));
            }
            
            Plotter.betterColors(chart);
            Plotter.saveAsPNG(chart, new File(folder, "comparison.png"));
        }
    }
}
