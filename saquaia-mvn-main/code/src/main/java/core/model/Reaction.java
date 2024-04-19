package core.model;

import com.google.gson.Gson;
import core.util.IO;
import core.util.JSON;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import core.util.Vector;

public class Reaction {
    public int[] reactants;
    public int[]  products;
    public double rate_constant;
    public String label;

    public Reaction(int[] reactants, int[] products, double rate_constant, String label){
        this.reactants = Vector.copy(reactants);
        this.products = Vector.copy(products);
        if (reactants.length != products.length) throw new IllegalArgumentException("Reactants and products need to have the same dimension.");
        this.rate_constant = rate_constant;
        this.label = label;
    }
    
    public void changeDim(int new_dim) {
        this.reactants = Arrays.copyOf(this.reactants, new_dim);
        this.products = Arrays.copyOf(this.products, new_dim);
    }
    
    public void deleteDim(int dim_to_del) {
        this.reactants = Vector.removeDim(reactants, dim_to_del);
        this.products = Vector.removeDim(products, dim_to_del);
    }
    
    public void switchDims(int dim1, int dim2) {
        Vector.switchDims(reactants, dim1, dim2);
        Vector.switchDims(products, dim1, dim2);
    }
    
    public void moveDim(int from, int to) {
        Vector.moveDim(reactants, from, to);
        Vector.moveDim(products, from, to);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reaction reaction = (Reaction) o;
        boolean labelEqual = true;
        if (label != null && reaction.label != null){
            labelEqual = label.equals(reaction.label);
        }
        else if (!((label == null) && (reaction.label == null))){
            return false;
        }

        return Double.compare(reaction.rate_constant, rate_constant) == 0 &&
                Arrays.equals(reactants, reaction.reactants) &&
                Arrays.equals(products, reaction.products);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(rate_constant, label);
        result = 31 * result + Arrays.hashCode(reactants);
        result = 31 * result + Arrays.hashCode(products);
        return result;
    }
    
    public double propensity(int[] state) {
        double res = this.rate_constant;
        int dim = dim();
        for (int dim_i = 0; dim_i < dim; dim_i++) {
            if (state[dim_i] < this.reactants[dim_i]) return 0;
            for (int i = 0; i < this.reactants[dim_i]; i++) {
                res *= state[dim_i] - i;
                res /= (i+1);
            }
        }
        return res;
    }
    
    public double propensity(double[] state) {
        double res = this.rate_constant;
        int dim = dim();
        for (int dim_i = 0; dim_i < dim; dim_i++) {
            if (state[dim_i] < this.reactants[dim_i]) return 0;
            for (int i = 0; i < this.reactants[dim_i]; i++) {
                res *= state[dim_i] - i;
                res /= (i+1);
            }
        }
        return res;
    }
    
    public void applyTo(int[] state) {
        applyTo(state, 1);
    }
    
    public void applyTo(int[] state, int times) {
        if (times == 0) return;
        for (int dim_i = 0; dim_i < state.length; dim_i++) state[dim_i] += times * (products[dim_i] - reactants[dim_i]);
    }
    
    public void applyTo(double[] state) {
        applyTo(state, 1);
    }
    
    public void applyTo(double[] state, double times) {
        if (times == 0) return;
        for (int dim_i = 0; dim_i < state.length; dim_i++) state[dim_i] += times * (products[dim_i] - reactants[dim_i]);
    }
    
    public int getReactionOrder() {
        return Arrays.stream(reactants).sum();
    }
    
    public String toLaTeX(CRN crn, Function<String, String> LaTeXNameFor) {
        if (crn.dim() != this.dim()) throw new IllegalArgumentException("Dimension of reaction and CRN do not match.");
        String res = "$" + this.label + ": ";
        boolean first = true;
        for (int i = 0; i < this.reactants.length; i++) {
            if (this.reactants[i] == 0) continue;
            
            if (first) first = false;
            else res+= " + ";
            
            if (this.reactants[i] != 1) res+= this.reactants[i];
            res+= LaTeXNameFor.apply(crn.speciesNames[i]);
        }
        if (first) res += "\\emptyset";
        res += " \\xrightarrow{" + this.rate_constant;
        for (int dim_i = 0; dim_i < dim(); dim_i++) {
            if (this.reactants[dim_i] > 1) {
                res+= "\\binom(" + LaTeXNameFor.apply(crn.speciesNames[dim_i]) +"," + this.reactants[dim_i] +")";
            }
            else if (this.reactants[dim_i] == 1) {
                res+= "\\cdot " + LaTeXNameFor.apply(crn.speciesNames[dim_i]);
            }
        }
        res += "} ";
        first = true;
        for (int i = 0; i < this.products.length; i++) {
            if (this.products[i] == 0) continue;
            
            if (first) first = false;
            else res+= " + ";
            
            if (this.products[i] != 1) res+= this.products[i];
            res+= LaTeXNameFor.apply(crn.speciesNames[i]);
        }
        if (first) res += "\\emptyset";
        return res + "$";
    }
    
    public String toString(CRN crn) {
        if (crn.dim() != this.dim()) throw new IllegalArgumentException("Dimension of reaction and CRN do not match.");
        String res = "";
        boolean first = true;
        for (int i = 0; i < this.reactants.length; i++) {
            if (this.reactants[i] == 0) continue;
            
            if (first) first = false;
            else res+= "+ ";
            
            if (this.reactants[i] != 1) res+= this.reactants[i];
            res+= crn.speciesNames[i].strip() + " ";
        }
        if (this.rate_constant != 1.0) {
            res += "->{" + IO.toString(rate_constant) + "} ";
        }
        else res += "-> ";
        first = true;
        for (int i = 0; i < this.products.length; i++) {
            if (this.products[i] == 0) continue;
            
            if (first) first = false;
            else res+= "+ ";
            
            if (this.products[i] != 1) res+= this.products[i];
            res+= crn.speciesNames[i].strip() + " ";
        }
        if (label != null && label.length() > 0) {
            if (res.length() < 30) res += " ".repeat(30 - res.length());
            res += "    // " + label;
        }
        return res;
    }
    
    public static Reaction fromString(CRN crn, String s) {
        String[] splitted = s.split("//", 2);
        String s_label = "";
        if (splitted.length == 2) {
            s_label = splitted[1].strip();
        }
        s = splitted[0] + " ";
        splitted = s.split("->");
        if (splitted.length != 2) return null;
        String s_reactants = splitted[0].strip();
        s = splitted[1] + " ";
        double rate = 1;
        if (s.startsWith("{") && s.contains("}")) {
            try {
                rate = Double.valueOf(s.substring(1, s.indexOf("}")));
                s = s.substring(s.indexOf("}")+1);
            } catch (NumberFormatException e) {return null;}
        }
        if (rate < 0) return null;
        String s_product = s.strip();
        
        int[] reactants = complexFromString(crn, s_reactants);
        int[] products = complexFromString(crn, s_product);
        if (reactants == null || products == null) return null;
        return new Reaction(reactants, products, rate, s_label);
    }
    
    private static int[] complexFromString(CRN crn, String s) {
        int[] res = new int[crn.dim()];
        String[] splitted = s.split("\\+");
        for (String x : splitted) {
            x = x.strip();
            if (x.length() == 0) continue;
            int number_part = 0;
            while(x.length() > number_part && Character.isDigit(x.charAt(number_part))) number_part++;
            int amount = 1;
            try {
                if (number_part > 0) amount = Integer.parseInt(x.substring(0, number_part));
            } catch (NumberFormatException e) {return null;}
            if (amount < 0) return null;
            
            int index = -1;
            String species = x.substring(number_part).strip();
            for (int dim_i = 0; dim_i < crn.dim(); dim_i++) {
                if (crn.speciesNames[dim_i].equals(species)) {
                    index = dim_i;
                    break;
                }
            }
            if (index == -1) return null;
            res[index] += amount;
        }
        return res;
    }

    public int dim() {
        return this.reactants.length;
    }
    
    public Reaction copy() {
        Gson gson = JSON.getGson();
        return gson.fromJson(gson.toJson(this), Reaction.class);
    }
}
