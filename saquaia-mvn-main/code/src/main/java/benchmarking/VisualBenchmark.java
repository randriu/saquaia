package benchmarking;

import benchmarking.simulatorconfiguration.SimulatorConfig;
import core.model.Setting;
import core.util.IO;
import core.util.Progressable;
import java.io.File;
import java.util.Random;
import org.jfree.chart.JFreeChart;
import plotting.Plotter;
import core.simulation.Simulation;
import core.simulation.simulators.Simulator;

/**
 *
 * @author Martin
 */
public class VisualBenchmark extends SettingBenchmark{

    public SimulatorConfig simconf;
    public int sims;
    public Integer repeat;
    public Integer plot_first = 100;
    
    public VisualBenchmark(long timout, String name, String example, double end_t, SimulatorConfig simconf, int sims) {
        this(timout, name, example, end_t, simconf, sims, 1);
    }
    
    public VisualBenchmark(long timout, String name, String example, double end_t, SimulatorConfig simconf, int sims, int repeat) {
        super(Type.VISUAL, timout, name, example, end_t);
        this.simconf = simconf;
        this.sims = sims;
        this.repeat = repeat;
    }
    
    @Override
    public Object doBenchmark(Progressable p) {
        if (!prepare(p)) return "Error: no setting found";
        
        if (repeat == null) repeat = 1;

        int target_number_length = ("" + (repeat-1)).length();
        int generated = 0;
        for (int repeat_i = 0; repeat_i < repeat; repeat_i++) {
            Simulator simulator = simconf.createSimulator(setting);
            int number_length = ("" + (repeat_i-1)).length();
            String subfolder_name = "0".repeat(number_length < target_number_length ? target_number_length-number_length: 0) + repeat_i;
            File subfolder = new File(getFolder(), subfolder_name);
            
            int generated_i = generateData(subfolder, simulator, setting, end_t, sims, plot_first == null ? 100 : plot_first, new Random(getRandom().nextLong()), p.start_subroutine(1.0 / repeat));
            p.end_subroutine();
            p.updateMessage("repeat " + (repeat_i+1) + "/" + repeat + ": generated " + generated_i + " simulations");
            generated += generated_i;
        }
        
        return generated;
    }
    
    public static int generateData(File folder, Simulator simulator, Setting setting, double end_t, int sims, int plot_first, Random random, Progressable p){
        folder.mkdirs();
        File data_folder = new File(folder, "data");
        data_folder.mkdirs();
        int target_number_length = ("" + (sims-1)).length();
        int sim_i = 0;
        for (; sim_i < sims; sim_i++) {
            if (p.isStopped()) break;
            Simulation s = setting.createSimulation();
            s.setSeed(random.nextLong());
            s.setKeepHistory(true);
            simulator.simulate(s, end_t, p.start_subroutine(1.0 / sims));
            p.end_subroutine();
            JFreeChart chart = Plotter.plotSimulation(s, setting);
            int number_length = ("" + sim_i).length();
            String file_name = "sim" + "0".repeat(number_length < target_number_length ? target_number_length-number_length: 0) + sim_i;
            if (sim_i < plot_first) Plotter.saveAsPNG(chart, new File(folder, file_name + ".png"));
            IO.writeObjectToJsonFile(new File(data_folder, file_name + ".json"), s);
        }
        return sim_i;
    }

    @Override
    public Object loadResult(String json) {
        return null;
    }
}
