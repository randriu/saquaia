package core.util;

import benchmarking.Benchmark;
import benchmarking.simulatorconfiguration.SimulatorConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import core.simulation.simulators.segmental.MemoryFunction;

public class JSON {
    
    private static Gson gson;
    
    public static Gson getGson() {
        if (gson == null) {
            GsonBuilder builder = new GsonBuilder();
            gson = builder
                    .enableComplexMapKeySerialization()
                    .serializeSpecialFloatingPointValues()
                    .setPrettyPrinting()
                    .registerTypeAdapter(SimulatorConfig.class, SimulatorConfig.ADAPTER)
                    .registerTypeAdapter(Benchmark.class, Benchmark.ADAPTER)
                    .registerTypeAdapter(MemoryFunction.class, MemoryFunction.ADAPTER)
                    .create();
        }
        return gson;
    }
}
