package benchmarking;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import core.util.IO;
import core.util.JSON;
import core.util.Progressable;
import core.util.Progressable.MessageListener;
import java.io.File;
import java.io.PrintStream;
import java.util.Random;

/**
 *
 * @author Martin
 */
public abstract class Benchmark implements Runnable {

    public static final GSONAdapter ADAPTER = new GSONAdapter();

    public enum Type {
        VISUAL, PERFORMANCE, TRANSIENT, TRANSIENTNEW, SEQUENCE;

        public static Class classFor(Type type) {
            switch (type) {
                case VISUAL:
                    return VisualBenchmark.class;
                case TRANSIENTNEW:
                    return NewTransientAnalysisBenchmark.class;
                case SEQUENCE:
                    return SequenceBenchmark.class;
                default:
                    break;
            }
            throw new IllegalArgumentException();
        }

        public static Type typeFromString(String s) {
            for (Type t : Type.values()) {
                if (t.toString().equals(s)) {
                    return t;
                }
            }
            return null;
        }
    }

    public final Type type;
    public long timeout;
    public String name;
    public Long seed;
    private transient Progressable p;
    private transient Random rand;

    public Benchmark(Type type, long timout, String name) {
        this.type = type;
        this.timeout = timout;
        this.name = name;
    }

    public Benchmark setSeed(long seed) {
        this.seed = seed;
        this.rand = null;
        return this;
    }
    
    public Benchmark setProgressable(Progressable p) {
        this.p = p;
        return this;
    }

    public static Benchmark fromJSON(String s) {
        Gson gson = JSON.getGson();
        Benchmark benchmark = gson.fromJson(s, Benchmark.class);
        return benchmark;
    }

    public static Benchmark[] fromJSONList(String s) {
        Gson gson = JSON.getGson();
        Benchmark[] benchmarks = gson.fromJson(s, new TypeToken<Benchmark[]>() {
        }.getType());
        return benchmarks;
    }

    public String toJson() {
        Gson gson = JSON.getGson();
        return gson.toJson(this, Benchmark.class);
    }

    public static String toJsonList(Benchmark... benchmarks) {
        Gson gson = JSON.getGson();
        return gson.toJson(benchmarks, new TypeToken<Benchmark[]>() {
        }.getType());
    }

    public Random getRandom() {
        if (this.rand != null) {
            return rand;
        }
        if (this.seed == null) {
            this.seed = new Random().nextLong();
        }
        this.rand = new Random(seed);
        return rand;
    }

    public abstract Object doBenchmark(Progressable p);

    public static File getOutFile(File folder) {
        return new File(folder, "out.txt");
    }

    public static File getResultFile(File folder) {
        return new File(folder, "result.json");
    }

    public static File getBenchmarkFile(File folder) {
        return new File(folder, "benchmark.json");
    }

    @Override
    public void run() {
        if (p == null) {
            p = new Progressable();
        }
        
        int p_level_initially = p.getCurrentLevel();
        File folder = getFolder();
        
        PrintStream out = null;
        MessageListener listener = null;

        try {
            out = IO.tryToGetFileStream(getOutFile(folder));
            final PrintStream out_ref = out; 
            listener = (message) -> {
                out_ref.println(p.formateMessage(message, true));
            };
            p.addMessageListener(listener);
            Thread main = new Thread(() -> {
                long start = System.nanoTime();
                p.updateMessage("benchmark start: " + IO.getCurTimeString());
                IO.writeObjectToJsonFile(getBenchmarkFile(folder), this, Benchmark.class);
                Object res = doBenchmark(p);
                IO.writeObjectToJsonFile(getBenchmarkFile(folder), this, Benchmark.class);
                p.updateMessage("benchmark ended: " + IO.getCurTimeString());
                p.updateMessage("duration: " + IO.humanReadableDuration(System.nanoTime() - start));
                p.updateMessage("saving result...");
                long result_size = IO.writeObjectToJsonFile(getResultFile(folder), res);
                p.updateMessage("result size: " + IO.humanReadableByteCountSI(result_size));
            });

            Thread interrupter = new Thread(() -> {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException ex) {
//                    p.updateMessage("interrupter's wait period was interrupted: " + ex.getMessage());
                    return;
                }
                p.updateMessage("benchmark timed out");
                p.stop();
                try {
                    main.join(5000);
                } catch (InterruptedException ex) {
                    p.updateMessage("interrupter's grace period was interrupted: " + ex.getMessage());
                    return;
                }
                if (!main.isAlive()) {
                    return;
                }
                p.updateMessage("interruptor: benchmark takes too long after timeout!");
                try {
                    main.join(295000);
                } catch (InterruptedException ex) {
                    p.updateMessage("interrupter's grace period was interrupted: " + ex.getMessage());
                    return;
                }
                if (!main.isAlive()) {
                    return;
                }
                main.stop();
                p.updateMessage("interruptor: killing benchmark manually... Are you checking the progressable regularly?");
            });

            interrupter.start();
            main.start();
            try {
                main.join();
            } catch (InterruptedException ex) {
                p.updateMessage("waiting for benchmark was interrupted...");
                p.updateMessage("killing benchmark manually...");
                main.stop();
            }
            if (interrupter.isAlive()) {
                interrupter.interrupt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            p.removeMessageListener(listener);
            while(p.getCurrentLevel() > p_level_initially) p.end_subroutine();
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    public void start() {
        Thread t = new Thread(this);
        t.start();
        try {
            t.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public File getFolder() {
        File file = new File(IO.CURRENT_BENCHMARK_FOLDER, this.name);
        file.mkdirs();
        return file;
    }

    private static class GSONAdapter implements
            JsonSerializer<Object>,
            JsonDeserializer<Object> {

        @Override
        public JsonElement serialize(Object object, java.lang.reflect.Type type, JsonSerializationContext jsonSerializationContext) {

            JsonObject unordered = jsonSerializationContext.serialize(object, object.getClass()).getAsJsonObject();
            JsonObject ordered = new JsonObject();
            if (unordered.has("type")) {
                ordered.add("type", unordered.remove("type"));
            }
            if (unordered.has("name")) {
                ordered.add("name", unordered.remove("name"));
            }
            for (String s : unordered.keySet()) {
                ordered.add(s, unordered.get(s));
            }
            return ordered;
        }

        @Override
        public Object deserialize(JsonElement jsonElement, java.lang.reflect.Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jsonObj = jsonElement.getAsJsonObject();
            String t = jsonObj.get("type").getAsString();
            return jsonDeserializationContext.deserialize(jsonElement, Type.classFor(Type.typeFromString(t)));
        }
    }

    public abstract Object loadResult(String json);
}
