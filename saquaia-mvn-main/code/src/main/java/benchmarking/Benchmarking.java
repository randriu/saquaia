package benchmarking;

import benchmarking.evaluation.Full;
import core.util.IO;
import core.util.Msc;
import core.util.Progressable;
import core.util.Progressable.MessageListener;
import gui.GUI;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author Martin
 */
public class Benchmarking {

    public static void main(String[] args) {
        File benchmark_file = IO.DEFAULT_BENCHMARK_FILE;
        ArrayList<String> args_list = new ArrayList<>(Arrays.asList(args));
        
        if (args_list.contains("-gui")) {
            GUI.main(args);
            return;
        }
        
        if (args_list.contains("-f")) {
            int i = args_list.lastIndexOf("-f");
            if (args.length > i + 1) {
                benchmark_file = new File(args[i + 1]);
            }
            if (!benchmark_file.exists()) {
                benchmark_file = new File(IO.DATA_FOLDER, args[i + 1]);
            }
            if (!benchmark_file.exists()) {
                benchmark_file = IO.DEFAULT_BENCHMARK_FILE;
            }
        }
        if (args_list.contains("-o")) {
            int i = args_list.lastIndexOf("-o");
            if (args.length > i + 1) {
                IO.CURRENT_BENCHMARK_FOLDER = new File(args[i + 1]);
            }
        }

        Benchmark[] bs = Benchmark.fromJSONList(IO.getContentOfFile(benchmark_file));
        benchmark(bs);

        if (args_list.contains("-e")) {
            Full.evaluate(IO.CURRENT_BENCHMARK_FOLDER);
        }

        // make output smaller
        if (args_list.contains("-d")) {
            System.out.println("removing (most) data, only keeping evaluation...");
            for (File f : IO.CURRENT_BENCHMARK_FOLDER.listFiles()) {
                if (!f.isDirectory()) {
                    continue;
                }
                File f_b = Benchmark.getBenchmarkFile(f);
                if (!f_b.exists()) {
                    continue;
                }
                try {
                    Benchmark b = Benchmark.fromJSON(IO.getContentOfFile(f_b));
                    if (b.type == Benchmark.Type.VISUAL) {
                        // delete all but 10 plots
                        for (File sf : f.listFiles()) {
                            if (!sf.isDirectory()) {
                                continue;
                            }
                            File sf_data = new File(sf, "data");
                            if (sf_data.exists()) {
                                Msc.deleteDirectory(sf_data);
                            }
                            for (File f2 : sf.listFiles()) {
                                if (f2.isFile() && sf.listFiles().length > 11 && f2.getName().endsWith(".png")) {
                                    File f2_plotting_data = new File(new File(sf, "plotting_data"), f2.getName() + ".csv");
                                    if (f2_plotting_data.exists()) {
                                        f2_plotting_data.delete();
                                    }
                                    File f2_plotting_data_meta = new File(new File(sf, "plotting_data"), f2.getName() + ".csv.meta");
                                    if (f2_plotting_data_meta.exists()) {
                                        f2_plotting_data_meta.delete();
                                    }
                                    f2.delete();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void benchmark(Benchmark[] bs) {
        IO.CURRENT_BENCHMARK_FOLDER.mkdirs();
        IO.writeStringToFile(new File(IO.CURRENT_BENCHMARK_FOLDER, "benchmarks.json"), Benchmark.toJsonList(bs));
        File outFile = new File(IO.CURRENT_BENCHMARK_FOLDER, "out.txt");
        
        Progressable p = new Progressable();
        p.enableSystemOutput();
        
        PrintStream out = null;
        MessageListener listener = null;

        try {
            out = IO.tryToGetFileStream(outFile);
            final PrintStream out_ref = out; 
            listener = (message) -> {
                out_ref.println(p.formateMessage(message, true));
            };
            p.addMessageListener(listener);
            
            long start = System.nanoTime();
            p.updateMessage("Starting benchmarking at " + IO.getCurTimeString());
            p.updateMessage("");

            for (int i = 0; i < bs.length; i++) {
                String s = "#### Benchmark " + (i + 1) + "/" + bs.length + ": " + bs[i].name + " ####";
                p.updateMessage("#".repeat(s.length()));
                p.updateMessage(s);
                p.updateMessage("#".repeat(s.length()));
                bs[i].setProgressable(p.start_subroutine(1.0 / bs.length));
                bs[i].start();
                p.end_subroutine();
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ex) {
                }
                p.updateMessage("");
            }

            p.updateMessage("Benchmarking ended at " + IO.getCurTimeString());
            p.updateMessage("Total duration: " + IO.humanReadableDuration(System.nanoTime() - start));
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            p.removeMessageListener(listener);
            if (out != null) {
                out.flush();
                out.close();
            }
        }

        
    }
}
