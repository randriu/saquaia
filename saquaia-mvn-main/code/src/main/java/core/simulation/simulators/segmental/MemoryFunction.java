/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.simulation.simulators.segmental;

import benchmarking.simulatorconfiguration.SimulatorConfig;
import benchmarking.simulatorconfiguration.TAUConfig;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import core.util.JSON;

/**
 *
 * @author Martin
 */
public abstract class MemoryFunction {
    public static final GSONAdapter ADAPTER = new GSONAdapter();
    
    public Integer start_at = null;
    
    public enum Type 
    {
        CONSTANT, LIMIT, GROWING, LINEAR, POINTLINEAR;
        
        public static Class classFor(Type type) {
            switch (type) {
                case CONSTANT:
                    return ConstantMemoryFunction.class;
                case LIMIT:
                    return TAUConfig.class;
                case GROWING:
                    return GrowingMemoryFunction.class;
                case LINEAR:
                    return LinearMemoryFunction.class;
                case POINTLINEAR:
                    return PointLinearMemoryFunction.class;
                default:
                    break;
            }
            throw new IllegalArgumentException();
        }
        
        public static Type typeFromString(String s) {
            for (Type t : Type.values()) if (t.toString().equals(s)) return t;
            return null;
        }
    }
    
    public final Type type;
    
    public MemoryFunction(Type type) {
        this.type = type;
    }
    
    public MemoryFunction setStartAt(int start_at) {
        this.start_at = start_at;
        return this;
    }
    
    public double target_nr_of_saved_segmentlikes(long uses) {
        if (start_at != null) {
            return uses <= start_at ? 0 : f(uses - start_at);
        }
        return f(uses);
    }
    
    public abstract double f(long uses);
 
    public static SimulatorConfig fromJSON(String s) {
        Gson gson = JSON.getGson();
        SimulatorConfig conf = gson.fromJson(s, SimulatorConfig.class);
        return conf;
    }
    
    public String toJson() {
        Gson gson = JSON.getGson();
        return gson.toJson(this, SimulatorConfig.class);
    }
    
    private static class GSONAdapter implements 
            JsonSerializer<Object>,
            JsonDeserializer<Object> {
        
        @Override
        public JsonElement serialize(Object object, java.lang.reflect.Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject unordered = jsonSerializationContext.serialize(object, object.getClass()).getAsJsonObject();
            JsonObject ordered = new JsonObject();
            if (unordered.has("type")) ordered.add("type", unordered.remove("type"));
            for(String s : unordered.keySet()) ordered.add(s, unordered.get(s));
            return ordered;
        }
        
        @Override
        public Object deserialize(JsonElement jsonElement, java.lang.reflect.Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jsonObj = jsonElement.getAsJsonObject();
            String t = jsonObj.get("type").getAsString();
            return jsonDeserializationContext.deserialize(jsonElement, Type.classFor(Type.typeFromString(t)));
        }
    }
    
    public abstract String abreviation();
}
