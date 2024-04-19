package core.model;

import com.google.gson.Gson;
import core.util.JSON;
import core.util.Vector;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;

/**
 *
 * @author Martin Helfrich
 */
public class CRN implements FirstOrderDifferentialEquations{
    public String name;
    public String[] speciesNames;
    public Reaction[] reactions;

    public CRN(String name, String[] speciesNames, Reaction[] reactions) {
        this.reactions = reactions;
        this.name = name;
        this.speciesNames = speciesNames;
    }
    
    public CRN(int dim) {
        this.reactions = new Reaction[0];
        this.name = "unnamed CRN";
        this.speciesNames = new String[dim];
        for (int i = 0; i < dim; i++) {
            this.speciesNames[i] = defaultSpeciesName(i);
        }
    }
    
    public static CRN product(CRN c1, CRN c2) {
        int d1 = c1.dim();
        int d2 = c1.dim();
        int d = d1 + d2;
        
        ArrayList<String> species_list = new ArrayList<>();
        for (String s : c1.speciesNames) species_list.add(s);
        for (String s : c2.speciesNames) {
            if (species_list.contains(s)) species_list.add(s + "2");
            else species_list.add(s);
        }
        String[] species = species_list.toArray(new String[d]);
        
        Reaction[] rs = new Reaction[c1.reactions.length + c2.reactions.length];
        for (int i = 0; i < c1.reactions.length; i++) {
            Reaction r = c1.reactions[i];
            Reaction r_new = new Reaction(new int[d], new int[d], r.rate_constant, r.label);
            rs[i] = r_new;
            System.arraycopy(r.reactants, 0, r_new.reactants, 0, d1);
            System.arraycopy(r.products, 0, r_new.products, 0, d1);
        }
        for (int i = 0; i < c2.reactions.length; i++) {
            Reaction r = c2.reactions[i];
            Reaction r_new = new Reaction(new int[d], new int[d], r.rate_constant, r.label);
            rs[c1.reactions.length + i] = r_new;
            System.arraycopy(r.reactants, 0, r_new.reactants, d1, d2);
            System.arraycopy(r.products, 0, r_new.products, d1, d2);
        }
        
        return new CRN(c1.name + " x " + c2.name, species, rs);
    }
    
    public int dim(){
        return speciesNames.length;
    }
    
    private String defaultSpeciesName(int dim_i) {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        if (dim_i < letters.length()) return "" + letters.charAt(dim_i);
        else return "Species " + (dim_i+1);
    }
    
    public int getDimensionForSpeciesName(String name) {
        for (int dim_i = 0; dim_i < speciesNames.length; dim_i++) if (speciesNames[dim_i].equals(name)) return dim_i;
        return -1;
    }
    
    public CRN() {
        this(2);
    }
    
    public void changeDim(int new_dim) {
        for (Reaction r : reactions) r.changeDim(new_dim);
        String[] new_speciesNames = Arrays.copyOf(speciesNames, new_dim);
        for (int i = speciesNames.length; i < new_dim; i++) new_speciesNames[i] = defaultSpeciesName(i);
        this.speciesNames = new_speciesNames;
    }
    
    public void deleteDim(int dim_to_del) {
        this.speciesNames = Vector.removeDim(speciesNames, dim_to_del);
        for (Reaction r : reactions) r.deleteDim(dim_to_del);
    }
    
    public void switchDims(int dim1, int dim2) {
        Vector.switchDims(speciesNames, dim1, dim2);
        for (Reaction r : reactions) r.switchDims(dim1, dim2);
    }
    
    public void addDim(String species_name) {
        changeDim(dim()+1);
        this.speciesNames[speciesNames.length-1] = species_name;
    }
    
    public void moveDim(int from, int to) {
        Vector.moveDim(speciesNames, from, to);
        for (Reaction r : reactions) r.moveDim(from, to);
    }
    
    @Override
    public String toString() {
        String r = "";
        r += "Name:\n";
        r += this.name + "\n";
        r += "Dimension:\n";
        r += this.dim() + "\n";
        r += "Species name:\n";
        r += Arrays.toString(this.speciesNames) + "\n";
        r += "Reactions:\n";
        String[] reaction_strings = new String[reactions.length];
        for(int r_i = 0; r_i < reactions.length; r_i++) reaction_strings[r_i] = reactions[r_i].toString(this);
        r += Arrays.toString(reaction_strings);
        return r;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CRN other = null;
        try {
            other = (CRN) o;
        } catch (ClassCastException e) {return false;}
        
        return name.equals(other.name) && Arrays.equals(reactions, other.reactions) && Arrays.equals(speciesNames, other.speciesNames);
    }

    public String toJson() {
        return JSON.getGson().toJson(this);
    }
    
    public static CRN fromJson(String s) {
        return JSON.getGson().fromJson(s, CRN.class);
    }

    @Override
    public int getDimension() {
       return dim();
    }

    @Override
    public void computeDerivatives(double t, double[] y, double[] yDot) {
        int dim = dim();
        for (int dim_i = 0; dim_i < dim; dim_i++) {
            yDot[dim_i] = 0;
            for (Reaction r : this.reactions) {
                yDot[dim_i]+= (r.products[dim_i] - r.reactants[dim_i]) * r.propensity(y);
            }
        }
    }
    
    public double[] propensities(int[] state) {
        double[] res = new double[reactions.length];
        for (int i = 0; i < reactions.length; i++) res[i] = reactions[i].propensity(state);
        return res;
    }
    
    public double[] propensities(double[] state) {
        double[] res = new double[reactions.length];
        for (int i = 0; i < reactions.length; i++) res[i] = reactions[i].propensity(state);
        return res;
    }
    
    public CRN copy() {
        Gson gson = JSON.getGson();
        return gson.fromJson(gson.toJson(this), CRN.class);
    }
}
