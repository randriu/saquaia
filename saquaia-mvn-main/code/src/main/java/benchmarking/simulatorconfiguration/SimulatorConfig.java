package benchmarking.simulatorconfiguration;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import core.model.Setting;
import core.util.JSON;
import core.simulation.simulators.AbstractSimulator;

public abstract class SimulatorConfig {
    
    public static final GSONAdapter ADAPTER = new GSONAdapter();
    
    public enum Type 
    {
        SSA, TAU, ODE, HYB, SEG;
        
        public static Class classFor(Type type) {
            switch (type) {
                case SSA:
                    return SSAConfig.class;
                case TAU:
                    return TAUConfig.class;
                case ODE:
                    return ODEConfig.class;
                case HYB:
                    return HybridConfig.class;
                case SEG:
                    return SegmentalConfig.class;
                default:
                    break;
            }
            throw new IllegalArgumentException();
        }
        
        public static SimulatorConfig defaultFor(Type type) {
            switch (type) {
                case SSA:
                    return new SSAConfig();
                case TAU:
                    return new TAUConfig();
                case ODE:
                    return new ODEConfig();
                case HYB:
                    return new HybridConfig();
                case SEG:
                    return new SegmentalConfig();
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
    
    public SimulatorConfig(Type type) {
        this.type = type;
    }
    
    public abstract AbstractSimulator createSimulator(Setting setting);
 
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
    
    public SimulatorConfig copy() {
        return fromJSON(toJson());
    }

    public Type getType() {
        return type;
    }
}
