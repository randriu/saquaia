/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author Martin
 */
public class LFU_DA_Cache_for_sorting<E> extends LFU_DA_Cache<E>{
    public E[] sorting_array;
    private HashMap<E, Integer> position_for = new HashMap<>();
    private ArrayList<Integer> nulls = new ArrayList<>();
    private int nulls_starting_at = 0;
    private final Class<E> clazz;
    
    public LFU_DA_Cache_for_sorting(int capacity, Class<E> clazz) {
        super(capacity);
        this.clazz = clazz;
        sorting_array = (E[]) Array.newInstance(clazz, capacity);
    }
    
    public LFU_DA_Cache_for_sorting(int capacity, double DA_fraction, Class<E> clazz) {
        super(capacity, DA_fraction);
        this.clazz = clazz;
        sorting_array = (E[]) Array.newInstance(clazz, capacity);
    }

    public LFU_DA_Cache_for_sorting(int capacity, int maxFreq, double DA_fraction, Class<E> clazz) {
        super(capacity, maxFreq, DA_fraction);
        this.clazz = clazz;
        sorting_array = (E[]) Array.newInstance(clazz, capacity);
    }
    
    @Override
    public boolean add(E o) {
        if (o == null) throw new IllegalArgumentException();
        boolean changed = super.add(o);
        Integer pos = position_for.get(o);
        if (pos == null) {
            if (!nulls.isEmpty()) {
                pos = nulls.remove(nulls.size()-1);
            } else {
                pos = nulls_starting_at;
                nulls_starting_at++;
            }
            sorting_array[pos] = o;
            position_for.put(o, pos);
        }
        return changed;
    }
    
    @Override
    public boolean remove(Object element) {
        boolean removed = super.remove(element);
        if (removed) {
            int pos = position_for.remove(element);
            sorting_array[pos] = null;
            nulls.add(pos);
        }
        return removed;
    }
    
    @Override
    public void clear() {
        super.clear();
        sorting_array = (E[]) Array.newInstance(clazz, capacity);
    }
    
    public Iterator<E> sortedIterator(Comparator<E> comp) {
        int[] comparisons = new int[1];
        Arrays.sort(sorting_array, new Comparator<E>() {
            @Override
            public int compare(E o1, E o2) {
                comparisons[0]++;
                E e1 = (E) o1;
                E e2 = (E) o2;
                if (e1 == null) return 1;
                if (e2 == null) return -1;
                return comp.compare(e1, e2);
            }
        });
        System.out.println("sorting " + size() + " elements with " + comparisons[0] + " comparisions");
        for (int i = 0; i < sorting_array.length; i++) {
            if (sorting_array[i] == null) {
                nulls_starting_at = i;
                break;
            }
            position_for.put(sorting_array[i], i);
        }
        nulls.clear();
        int asd2 = nulls_starting_at;
        E[] asd = sorting_array;
        
        return new Iterator<E>() {
            int i = 0;
            
            @Override
            public boolean hasNext() {
                return i < sorting_array.length && sorting_array[i] != null;
            }

            @Override
            public E next() {
                return sorting_array[i++];
            }
        };
    }
    
    public void log_status(){
        System.out.println(Arrays.toString(sorting_array));
        position_for.forEach((t, u) -> {
            System.err.println(t + " -> " + u);
        });
        System.out.println(nulls);
        System.out.println("nulls_starting_at=" + nulls_starting_at);
        System.out.println("");
    }
    
    
    public static void main(String[] args) {
        LFU_DA_Cache_for_sorting<Integer> c = new LFU_DA_Cache_for_sorting<Integer>(5, Integer.class);
        c.add(1);
        c.log_status();
        c.add(2);
        c.log_status();
        c.add(3);
        c.log_status();
        c.add(3);
        c.log_status();
        c.add(3);
        c.log_status();
        c.add(4);
        c.log_status();
        c.add(5);
        c.log_status();
        c.add(6);
        c.log_status();
        c.add(8);
        c.log_status();
        c.add(7);
        c.log_status();
        c.add(9);
        c.log_status();
        c.add(10);
        c.log_status();
        c.remove(8);
        c.log_status();
        c.sortedIterator(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return Integer.compare(o1, o2);
            }
        });
        c.log_status();
        c.add(15);
        c.log_status();
        c.add(4);
        c.log_status();
        c.add(4);
        c.add(4);
        c.log_status();
        c.add(6);
        c.log_status();
        c.sortedIterator(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return -Integer.compare(o1, o2);
            }
        });
        c.log_status();
    }
}
