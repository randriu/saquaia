package core.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import core.util.JSON;
import java.lang.reflect.Type;

/**
 *
 * @author Martin Helfrich
 * @param <StateType>
 */
public class TransientDistribution<StateType> {
    public final double time;
    public final Distribution<StateType> distribution;
    
    public TransientDistribution(double time) {
        this.time = time;
        this.distribution = new Distribution<>();
    }
    
    public TransientDistribution(double time, Distribution<StateType> dist) {
        this.time = time;
        this.distribution = new Distribution<>(dist);
    }

    public String toJson() {
        return JSON.getGson().toJson(this, TransientDistribution.class);
    }
    
    // call this as follows:
    // TransientDistribution.<DoubleArrayWrapper>fromJson(test_as_json, new TypeToken<TransientDistribution<DoubleArrayWrapper>>(){});
    // replace DoubleArrayWrapper by the StateType of your Distribution
    public static <StateType> TransientDistribution<StateType> fromJson(String s, TypeToken<TransientDistribution<StateType>> typeToken) {
        Gson gson = JSON.getGson();
        Type type = typeToken.getType();
        
        TransientDistribution<StateType> res = gson.fromJson(s, type);
        res.distribution.normalize();
        return res;
    }
}
