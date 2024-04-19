/*
 * This file is part of SeQuaiA.
 *
 *     SeQuaiA is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     SeQuaiA is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package core.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import core.util.DoubleArrayWrapper;
import core.util.EMD_Helper;
import core.util.IntArrayWrapper;
import core.util.JSON;
import core.util.Pair;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;

public class Distribution<StateType> extends HashMap<StateType, Double> {

    public Distribution() {
        super();
    }

    public Distribution(Distribution<StateType> dist) {
        super(dist != null ? dist : new Distribution<>());
    }

    public String toJson() {
        return JSON.getGson().toJson(this);
    }

    // call this as follows:
    // Distribution.<DoubleArrayWrapper>fromJson(test_as_json, new TypeToken<Distribution<DoubleArrayWrapper>>(){});
    // replace DoubleArrayWrapper by the StateType of your Distribution
    public static <StateType> Distribution<StateType> fromJson(String s, TypeToken<Distribution<StateType>> typeToken) {
        Gson gson = JSON.getGson();
        Type type = typeToken.getType();

        Distribution<StateType> res = gson.fromJson(s, type);
        res.normalize();
        return res;
    }

    public void add(StateType state, double additionalMass) {
        if (additionalMass < 0) {
            throw new IllegalArgumentException("Additional mass may not be negative!");
        }
        put(state, getOrDefault(state, 0.0) + additionalMass);
    }

    public double totalMass() {
        double res = 0.0;
        for (StateType state : keySet()) {
            res += this.get(state);
        }
        return res;
    }

    public void normalize() {
        double totalMass = totalMass();
        if (totalMass == 1.0) {
            return;
        }
        for (StateType state : keySet()) {
            double normalized_mass = get(state) / totalMass;
            put(state, normalized_mass);
        }
    }

    public int denormalize() {
        double min_mass = Double.MAX_VALUE;
        for (StateType state : keySet()) {
            min_mass = Math.min(min_mass, get(state));
        }
        return denormalize(min_mass);
    }

    public int denormalize(double base_value) {
        int res = 0;
        for (StateType state : keySet()) {
            double normalized_mass = Math.round(get(state) / base_value);
            res += (int) normalized_mass;
            put(state, normalized_mass);
        }
        return res;
    }
    
    public ArrayList<StateType> asValueList(int target_size){
        int amount = denormalize(1.0 / target_size);
        ArrayList<StateType> res = new ArrayList<>(amount);int next = 0;
        for (Iterator<StateType> it = this.keySet().iterator(); it.hasNext();) {
            StateType s = it.next();
            double repeat = this.get(s);
            for (int i = 0; i < repeat; i++) res.add(s);
        }
        normalize();
        return res;
    }
    
    public ArrayList<StateType> asValueList(){
        int amount = denormalize();
        ArrayList<StateType> res = new ArrayList<>(amount);int next = 0;
        for (Iterator<StateType> it = this.keySet().iterator(); it.hasNext();) {
            StateType s = it.next();
            double repeat = this.get(s);
            for (int i = 0; i < repeat; i++) res.add(s);
        }
        normalize();
        return res;
    }

    public static Distribution<IntArrayWrapper> convertFromConcreteStatesToIntervalStates(Distribution<DoubleArrayWrapper> dist, Setting s) {
        Distribution<IntArrayWrapper> res = new Distribution();
        dist.forEach((concrete_state, mass) -> {
            res.add(new IntArrayWrapper(s.intervalStateForState(s.closestStateWithinBounds(concrete_state.array))), mass);
        });
        return res;
    }

    public static Distribution<IntArrayWrapper> convertFromIntervalStatesToRepresentatives(Distribution<IntArrayWrapper> dist, Setting s) {
        Distribution<IntArrayWrapper> res = new Distribution();
        dist.forEach((interv_state, mass) -> {
            res.add(new IntArrayWrapper(s.representativeForIntervalState(interv_state.array)), mass);
        });
        return res;
    }

    public static Distribution<Double> projectToDimensionDouble(Distribution<DoubleArrayWrapper> dist, int dim_i) {
        if (dim_i < 0) {
            throw new IllegalArgumentException("The chosen dimension is negative.");
        }
        Distribution<Double> res = new Distribution();
        dist.forEach((state, mass) -> {
            if (dim_i >= state.array.length) {
                throw new IllegalArgumentException("The dimension of state " + state + " is smaller than the chosen dimension.");
            }
            res.add(state.array[dim_i], mass);
        });
        return res;
    }

    public static Distribution<Integer> projectToDimensionInt(Distribution<IntArrayWrapper> dist, int dim_i) {
        if (dim_i < 0) {
            throw new IllegalArgumentException("The chosen dimension is negative.");
        }
        Distribution<Integer> res = new Distribution();
        dist.forEach((state, mass) -> {
            if (dim_i >= state.array.length) {
                throw new IllegalArgumentException("The dimension of state " + state + " is smaller than the chosen dimension.");
            }
            res.add(state.array[dim_i], mass);
        });
        return res;
    }
    
    public static Distribution<Integer> floorValues(Distribution<Double> dist) {
        Distribution<Integer> res = new Distribution();
        dist.forEach((state, mass) -> {
            res.add((int)Math.floor(state), mass);
        });
        return res;
    }

    public static <StateType> double absolute_error(Distribution<StateType> d1, Distribution<StateType> d2) {
        HashSet<StateType> all_states = new HashSet<>(d1.keySet());
        all_states.addAll(d2.keySet());
        double error = 0;
        for (StateType state : all_states) {
            double v1 = d1.getOrDefault(state, 0.0);
            double v2 = d2.getOrDefault(state, 0.0);
            double new_error = Math.abs(v1 - v2);
            // if (new_error > 0.003) System.out.println(state + ": d1=" + v1 + " d2=" + v2 + " --> error=" + new_error);
            error += new_error;
        }
        return error / 2;     // every error is counted twice
    }
    
    public static double earth_mover_distance_1D(Distribution<Integer> d1, Distribution<Integer> d2) {
        if (d1 == null && d2 == null) {
            return 0;
        }
        if (d1 == null || d2 == null) {
            throw new IllegalArgumentException("Distributions must not be null.");
        }

        HashSet<Integer> keys = new HashSet<>(d1.keySet());
        keys.addAll(d2.keySet());
        ArrayList<Integer> key_list = new ArrayList<>(keys);
        key_list.sort((Integer o1, Integer o2) -> Integer.compare(o1, o2));

        double last = 0;
        double to_move = 0;
        double dist = 0;
        for (Integer d : key_list) {
            dist += Math.abs((d - last) * to_move);
            to_move += d1.getOrDefault(d, 0.0) - d2.getOrDefault(d, 0.0);
//            System.out.println("    " + d + " to_move=" + to_move + " dist="+ dist);
            last = d;
        }
        return dist;
    }

    public static double earth_mover_distance_1D(Distribution<Double> d1, Distribution<Double> d2, boolean floor) {
        if (d1 == null && d2 == null) {
            return 0;
        }
        if (d1 == null || d2 == null) {
            throw new IllegalArgumentException("Distributions must not be null.");
        }
        
        if (floor) {
            Distribution<Double> d1_clean = new Distribution();
            for (double x : d1.keySet()) {
                d1_clean.add(Math.floor(x), d1.get(x));
            }
            d1 = d1_clean;
            Distribution<Double> d2_clean = new Distribution();
            for (double x : d2.keySet()) {
                d2_clean.add(Math.floor(x), d2.get(x));
            }
            d2 = d2_clean;
        }

        return EMD_Helper.EMD(d1, d2);
    }

    public static double[] earth_mover_distances(Distribution<DoubleArrayWrapper> d1, Distribution<DoubleArrayWrapper> d2, boolean floor) {
        if (d1 == null || d2 == null) {
            throw new IllegalArgumentException("Distributions must not be null.");
        }
        
        int dim = -1;
        for (DoubleArrayWrapper x : d1.keySet()) {
            dim = x.array.length;
            break;
        }
        if (dim < 0) throw new IllegalArgumentException("Could not determine dimension of states!");

        double[] res = new double[dim];
        for (int dim_i = 0; dim_i < dim; dim_i++) {
            Distribution<Double> d1p = Distribution.projectToDimensionDouble(d1, dim_i);
            Distribution<Double> d2p = Distribution.projectToDimensionDouble(d2, dim_i);
            res[dim_i] = Distribution.earth_mover_distance_1D(d1p, d2p, floor);
        }
        return res;
    }

    public static double[] earth_mover_distances(Distribution<IntArrayWrapper> d1, Distribution<IntArrayWrapper> d2) {
        if (d1 == null || d2 == null) {
            throw new IllegalArgumentException("Distributions must not be null.");
        }
        
        int dim = -1;
        for (IntArrayWrapper x : d1.keySet()) {
            dim = x.array.length;
            break;
        }
        if (dim < 0) throw new IllegalArgumentException("Could not determine dimension of states!");

        double[] res = new double[dim];
        for (int dim_i = 0; dim_i < dim; dim_i++) {
            Distribution<Integer> d1p = Distribution.projectToDimensionInt(d1, dim_i);
            Distribution<Integer> d2p = Distribution.projectToDimensionInt(d2, dim_i);
            res[dim_i] = Distribution.earth_mover_distance_1D(d1p, d2p);
        }
        return res;
    }

    public void print_heavy_states(double total_weight) {
        ArrayList<Entry<StateType, Double>> arrayList = new ArrayList<>(this.entrySet());
        Collections.sort(arrayList, (o1, o2) -> {
            return -o1.getValue().compareTo(o2.getValue());
        });

        double weight = 0.0;
        for (Entry<StateType, Double> e : arrayList) {
            if (weight >= total_weight) {
                return;
            }
            weight += e.getValue();
            System.out.println(e.getKey() + ": " + e.getValue());
        }
    }
    
    public void print_states_havier_than(double weight) {
        ArrayList<Entry<StateType, Double>> arrayList = new ArrayList<>(this.entrySet());
        Collections.sort(arrayList, (o1, o2) -> {
            return -o1.getValue().compareTo(o2.getValue());
        });

        for (Entry<StateType, Double> e : arrayList) {
            if (e.getValue() <= weight) break;
            System.out.println(e.getKey() + ": " + e.getValue());
        }
    }

    public static Pair<Double, Double> getMeanAndVarianceInteger(Distribution<Integer> dist) {
        double expectation = 0.0;
        for (int key : dist.keySet()) {
            expectation += key * dist.get(key);
        }
        double variance = 0.0;
        for (int key : dist.keySet()) {
            variance += (key - expectation) * (key - expectation) * dist.get(key);
        }
        return new Pair<>(expectation, variance);
    }

    public static Pair<Double, Double> getMeanAndVarianceDouble(Distribution<Double> dist) {
        double expectation = 0.0;
        for (double key : dist.keySet()) {
            expectation += key * dist.get(key);
        }
        double variance = 0.0;
        for (double key : dist.keySet()) {
            variance += (key - expectation) * (key - expectation) * dist.get(key);
        }
        return new Pair<>(expectation, variance);
    }

    public static TreeMap<Double, Double> toCDF(Distribution<Double> dist) {
        dist.normalize();
        TreeMap<Double, Double> res = new TreeMap<>();
        double sum = 0;
        for (double d : dist.keySet().stream().sorted().toArray(Double[]::new)) {
            sum += dist.get(d);
            res.put(d, sum);
        }
        return res;
    }
}
