/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package benchmarking;

import benchmarking.simulatorconfiguration.SimulatorConfig;
import com.google.gson.reflect.TypeToken;
import core.model.Distribution;
import core.model.TransientDistribution;
import core.util.DoubleArrayWrapper;
import core.util.IO;
import core.util.JSON;
import core.util.Progressable;
import core.util.Vector;
import java.util.ArrayList;
import java.util.Random;
import core.simulation.Simulation;
import core.simulation.simulators.SegmentalSimulator;
import core.simulation.simulators.Simulator;

/**
 *
 * @author Martin
 */
public class SequenceBenchmark extends SettingBenchmark {

    public SimulatorConfig simconf;
    public Task[] tasks;
    public int repeat;

    public SequenceBenchmark(long timout, String name, String example, SimulatorConfig simconf, int repeat, Task... tasks) {
        super(Type.SEQUENCE, timout, name, example, 0);
        this.simconf = simconf;
        this.tasks = tasks;
        this.repeat = repeat;
    }

    @Override
    public Object doBenchmark(Progressable p) {
        if (!prepare(p)) {
            return "Error: no setting found";
        }

        return doTasksRepeatatly(p);
    }

    public TaskResult doTask(Progressable p, Task task, Simulator simulator, Random rand) {
        TaskResult taskResult = new TaskResult(task);

        long last_info = 0;

        for (int sim_i = 0; sim_i < task.sims; sim_i++) {
            if (p.isStopped()) {
                break;
            }

            Simulation sim = task.initial_state == null ? setting.createSimulation() : new Simulation(Vector.asDoubleArray(task.initial_state));
            sim.setSeed(rand.nextLong());

            long start_time_sim = System.nanoTime();
            if (start_time_sim > 10_000_000_000l + last_info) {
                last_info = start_time_sim;
                p.updateMessage("sim " + (sim_i + 1) + "/" + task.sims + " ...");
                start_time_sim = System.nanoTime();
            }
            for (int snapshot_i = 0; snapshot_i < task.snapshots; snapshot_i++) {
                if (p.isStopped()) {
                    break;
                }
                SimulationResults snapshot = taskResult.snapshots[snapshot_i];
                double t = taskResult.times[snapshot_i];
                simulator.simulate(sim, t, p.start_subroutine(1.0 / task.snapshots));
                p.end_subroutine();
                snapshot.addSimulation(sim, System.nanoTime() - start_time_sim);
            }
        }

        return taskResult;
    }

    @Override
    public TaskSequenceResult[] loadResult(String json) {
        if (json == null || json.equals("")) return null;
        return JSON.getGson().fromJson(json, new TypeToken<TaskSequenceResult[]>(){}.getType());
    }

    public TaskSequenceResult doTasks(Progressable p, Random rand) {

        Simulator simulator = simconf.createSimulator(setting);
        TaskSequenceResult res = new TaskSequenceResult(tasks.length);

        // without resets
        p.updateMessage("TaskSequence without resets");
        for (int task_i = 0; task_i < tasks.length; task_i++) {
            if (p.isStopped()) {
                break;
            }
            p.start_subroutine(1.0);
            Task task = tasks[task_i];

            p.updateMessage("task " + (task_i + 1) + "/" + tasks.length + " ...");
            long task_start = System.nanoTime();
            TaskResult taskResult = doTask(p.start_subroutine(1.0), task, simulator, new Random(getRandom().nextLong()));
            p.end_subroutine();
            p.updateMessage("... took " + IO.humanReadableDuration(System.nanoTime() - task_start));
            p.updateMessage("... avg_speed_per_sim with " + taskResult.getFinalSnapshot().numberOfResults() + " sims: " + IO.humanReadableDuration(taskResult.getFinalSnapshot().avg_comp_time()));
            res.without_reset[task_i] = taskResult;
            p.end_subroutine();
        }

        // export memory stats if segmental!
        try {
            SegmentalSimulator seg_simulator = (SegmentalSimulator) simulator;
            p.updateMessage("" + seg_simulator.getStatistics());
        } catch (ClassCastException e) {
        }

        // with resets
        if (simconf.createSimulator(setting).getMemory() != null && tasks.length > 1) {
            p.updateMessage("TaskSequence with resets");
            for (int task_i = 0; task_i < tasks.length; task_i++) {
                if (p.isStopped()) {
                    break;
                }
                p.start_subroutine(1.0);
                Task task = tasks[task_i];

                simulator = simconf.createSimulator(setting);

                p.updateMessage("task " + (task_i + 1) + "/" + tasks.length + " ...");
                long task_start = System.nanoTime();

                TaskResult taskResult = doTask(p.start_subroutine(1.0), task, simulator, new Random(getRandom().nextLong()));
                p.end_subroutine();
                p.updateMessage("... took " + IO.humanReadableDuration(System.nanoTime() - task_start));
                p.updateMessage("... avg_speed_per_sim with " + taskResult.getFinalSnapshot().numberOfResults() + " sims: " + IO.humanReadableDuration(taskResult.getFinalSnapshot().avg_comp_time()));
                res.with_reset[task_i] = taskResult;

                // export memory stats if segmental!
                try {
                    SegmentalSimulator seg_simulator = (SegmentalSimulator) simulator;
                    p.updateMessage("" + seg_simulator.getStatistics());
                } catch (ClassCastException e) {
                }
                p.end_subroutine();
            }
        }

        return res;
    }

    public TaskSequenceResult[] doTasksRepeatatly(Progressable p) {
        ArrayList<TaskSequenceResult> res = new ArrayList<>();
        for (int repeat_i = 0; repeat_i < repeat; repeat_i++) {
            p.updateMessage("repeat " + (repeat_i + 1) + "/" + repeat);
            if (p.isStopped()) {
                break;
            }
            res.add(doTasks(p.start_subroutine(1.0 / repeat), new Random(getRandom().nextLong())));
            p.end_subroutine();
        }
        return res.toArray(TaskSequenceResult[]::new);
    }

    public static class Task {

        public int[] initial_state;
        public int sims;
        public double end_t;
        public int snapshots;

        public Task(int[] initial_state, int sims, double end_t, int snapshots) {
            this.initial_state = initial_state;
            this.sims = sims;
            this.end_t = end_t;
            this.snapshots = snapshots;
        }
        
        public Task setSims(int sims) {
            this.sims = sims;
            return this;
        }
    }

    public static class SimulationResults {

        public ArrayList<double[]> states = new ArrayList<>();
        public ArrayList<Double> times = new ArrayList<>();
        public ArrayList<Long> comp_times = new ArrayList<>();
        public ArrayList<Long> comp_time_sum = new ArrayList<>();
        public long total_comp_time = 0;

        // distribution for reuse
        private transient Distribution<DoubleArrayWrapper> dist;
        private transient Integer results_in_dist;

        public void addSimulation(Simulation sim, long comp_time) {
            states.add(sim.getState());
            times.add(sim.getTime());
            comp_times.add(comp_time);
            total_comp_time += comp_time;
            comp_time_sum.add(total_comp_time);
        }

        public int numberOfResults() {
            return states.size();
        }

        public double comp_time_in_s(int sim_i) {
            return 1.0 * comp_times.get(sim_i) / 1000_000_000l;
        }

        public double comp_time_sum_in_s(int sim_i) {
            return 1.0 * comp_time_sum.get(sim_i) / 1000_000_000l;
        }

        public long avg_comp_time(int sim_i) {
            return comp_time_sum.get(sim_i) / (sim_i + 1);
        }

        public long avg_comp_time() {
            return total_comp_time / numberOfResults();
        }

        public double avg_comp_time_in_s(int sim_i) {
            return 1.0 * avg_comp_time(sim_i) / 1000_000_000l;
        }

        public double avg_comp_time_in_s() {
            return 1.0 * avg_comp_time() / 1000_000_000l;
        }

        public Distribution<DoubleArrayWrapper> getDistribution() {
            if (dist == null || numberOfResults() != results_in_dist) {
                dist = new Distribution<>();
                for (double[] state : states) {
                    dist.add(new DoubleArrayWrapper(state), 1);
                }
                dist.normalize();
                results_in_dist = numberOfResults();
            }
            return dist;
        }
    }

    public static class SimulationResultsWithSnapshots {

        public double end_t;
        public double[] times;
        public SimulationResults[] snapshots;

        public SimulationResultsWithSnapshots(double end_t, int snapshots) {
            this.end_t = end_t;
            this.times = new double[snapshots];
            this.snapshots = new SimulationResults[snapshots];
            for (int i = 0; i < snapshots; i++) {
                this.times[i] = end_t / snapshots * (i + 1);
                this.snapshots[i] = new SimulationResults();
            }
        }

        public SimulationResults getFinalSnapshot() {
            return snapshots[snapshots.length - 1];
        }

        public TransientDistribution<DoubleArrayWrapper> getTransientDistribution(int snapshot_i) {
            return new TransientDistribution(times[snapshot_i], snapshots[snapshot_i].getDistribution());
        }

        public TransientDistribution<DoubleArrayWrapper> getTransientDistribution() {
            return getTransientDistribution(snapshots.length - 1);
        }
    }

    public static class TaskResult extends SimulationResultsWithSnapshots {

        public Task task;

        public TaskResult(Task task) {
            super(task.end_t, task.snapshots);
            this.task = task;
        }
    }

    public static class TaskSequenceResult {

        public TaskResult[] without_reset;
        public TaskResult[] with_reset;

        public TaskSequenceResult(int number_of_tasks) {
            this.without_reset = new TaskResult[number_of_tasks];
            this.with_reset = new TaskResult[number_of_tasks];
        }

        public int number_of_tasks() {
            return with_reset.length;
        }
    }

    public static Task getPPTask() {
        return new Task(null, 10000, 200.0, 1);
    }

    public static Task[] getPPSequenceTasks() {
        return new Task[]{
            new Task(new int[]{200, 200}, 1000, 50.0, 1),
            new Task(new int[]{200, 200}, 9000, 50.0, 1),
            new Task(new int[]{200, 200}, 10000, 1000.0, 1),
            new Task(new int[]{100, 100}, 10000, 1000.0, 1)
        };
    }

    public static Task getVITask() {
        return new Task(null, 10000, 200.0, 1);
    }

    public static Task[] getVISequenceTasks() {
        return new Task[]{
            new Task(new int[]{0, 1, 0, 0}, 1000, 200.0, 1),
            new Task(new int[]{0, 1, 0, 0}, 9000, 200.0, 1),
            new Task(new int[]{0, 1, 0, 0}, 10000, 2000.0, 1),
            new Task(new int[]{0, 5, 0, 0}, 10000, 2000.0, 1)
        };
    }

    public static Task getTSTask() {
        return new Task(null, 10000, 50000.0, 1);
    }

    public static Task[] getTSSequenceTasks() {
        return new Task[]{
            new Task(new int[]{0, 0, 0, 0, 0, 0}, 1000, 5000.0, 1),
            new Task(new int[]{0, 0, 0, 0, 0, 0}, 9000, 5000.0, 1),
            new Task(new int[]{0, 0, 0, 0, 0, 0}, 10000, 500000.0, 1),
            new Task(new int[]{2, 5, 5, 1, 10000, 10000}, 10000, 500000.0, 1)
        };
    }

    public static Task getRPTask() {
        return new Task(null, 10000, 50000.0, 1);
    }

    public static Task[] getRPSequenceTasks() {
        return new Task[]{
            new Task(new int[]{10, 0, 0, 500, 0, 0}, 1000, 5000.0, 1),
            new Task(new int[]{10, 0, 0, 500, 0, 0}, 9000, 5000.0, 1),
            new Task(new int[]{10, 0, 0, 500, 0, 0}, 10000, 500000.0, 1),
            new Task(new int[]{0, 0, 0, 0, 0, 0}, 10000, 500000.0, 1)
        };
    }

    public static Task getECTask() {
        return new Task(null, 10000, 2000.0, 1);
    }

    public static Task[] getECSequenceTasks() {
        return new Task[]{
            new Task(null, 1000, 200.0, 1),
            new Task(null, 9000, 200.0, 1),
            new Task(null, 10000, 2000.0, 1),
            new Task(new int[]{
                1,
                35,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                150,    // changed from 350
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0}, 10000, 2000.0, 1),};
    }

    public static Task getTRTask() {
        return new Task(null, 10000, 50000.0, 1);
    }

    public static Task toBaseTask(Task task) {
        task.sims = 100;
        return task;
    }

    public static Task[] toBaseTask(Task[] tasks) {
        for (int task_i = 0; task_i < tasks.length; task_i++) {
            tasks[task_i].sims = 100;
        }
        return tasks;
    }

}
