package core.util;

import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * <p> Implements a set with a LFU (least frequently used) DA (dynamic aging) caching strategy.Specifically, it uses "periodic aging by division" (see https://dl.acm.org/doi/pdf/10.1145/98457.98523 ).
 * Implemented according to http://dhruvbird.com/lfu.pdf (https://stackoverflow.com/a/61963145) to guarantee O(1) amortized cost for all operations. The cost is only amortized O(1) because the dynamic aging strategy may need to iterate over all nodes to update the freq.
 * 
 * <p> If an element gets ejected because the set was full before some {@link #add(E element)} call, then the last ejected element can be retrieved via {@link #getEjectedElement()}.
 * Alternatively, you can call {@link #addAndReturnEjected(E element)} instead.
 * 
 * @author helfrich
 * @param <E> the type of elements held in this cache
 */
public class LFU_DA_Cache<E> extends AbstractSet<E>{

    private Map<E, Node<E>> nodes;
    private FrequencyMap<E> frequencies;
    final int capacity;
    private final int maxFreq;
    private final double DA_fraction;
//    private final long freqSum_threshold_for_DA;
    private long freqSum;
    private int add_operations_since_last_aging = 0;
//    private final int age_at_least_ever_add_operations;
    
    private int DAs = 0;
    
    private E ejected = null;
    
    public LFU_DA_Cache(int capacity) {
        this(capacity, 0.1);
    }
    
    public LFU_DA_Cache(int capacity, double DA_fraction) {
//        this(capacity, Math.min(capacity, 1000), DA_fraction, 1000_000);
        this(capacity, 1000, DA_fraction);
    }

    public LFU_DA_Cache(int capacity, int maxFreq, double DA_fraction) {
//    public LFU_DA_Cache(int capacity, int maxFreq, double DA_fraction, int age_at_least_every_x_add_operations) {
        if (DA_fraction <= 0 || DA_fraction > 1) throw new IllegalArgumentException("The DA_fraction must be larger than 0 and at most 1.");
        this.DA_fraction = DA_fraction;
        capacity = Math.max(capacity, 0);
        this.capacity = capacity;
        maxFreq = Math.max(maxFreq, 1);
        this.maxFreq = maxFreq;
//        double freqSum_threshold_for_DA = DA_fraction * capacity * maxFreq;
//        this.freqSum_threshold_for_DA = freqSum_threshold_for_DA >= Long.MAX_VALUE ? Long.MAX_VALUE - 1 : (long) freqSum_threshold_for_DA;
//        this.age_at_least_ever_add_operations = age_at_least_every_x_add_operations;
        this.nodes = new HashMap<>(capacity);
        this.frequencies = new FrequencyMap<>();
    }

    @Override
    public int size() {
        return nodes.size();
    }
    
    public boolean isFull() {
        return size() >= capacity;
    }

    @Override
    public boolean contains(Object o) {
        return nodes.containsKey(o);
    }

    @Override
    public void clear() {
        nodes.clear();
        frequencies = new FrequencyMap<>();
        DAs = 0;
    }
    
    @Override
    public boolean add(E o) {
        add_operations_since_last_aging++;
        ejected = null;
        if (nodes.containsKey(o)) {
            Node<E> old_node = nodes.get(o);
            access(old_node);
            return false;
        } else {
            Node<E> new_node = new Node<>(o);
            if (!isEmpty() && size() >= capacity) {
                ejected = frequencies.head.head.value;
                remove(ejected);
            }
            frequencies.add(new_node);
            nodes.put(o, new_node);
            increaseFreqSum();
            return true;
        }
    }
    
    public int dynamicAgingEvents() {
        return DAs;
    }
    
    public boolean addIfExists(E o) {
        if (nodes.containsKey(o)) {
            Node<E> old_node = nodes.get(o);
            access(old_node);
            return true;
        }
        return false;
    }
    
    public E addAndReturnEjected(E element) {
        add(element);
        return ejected;
    }
    
    public E getEjectedElement() {
        return ejected;
    }
    
    @Override
    public boolean remove(Object element) {
        Node<E> removed_node = nodes.remove(element);
        if (removed_node != null) {
            freqSum -= removed_node.nodeList.freq;
            removed_node.remove();
        }
        return removed_node != null;
    }
    
    public int getFrequency(E element) {
        Node<E> n = nodes.get(element);
        return element == null ? 0 : n.nodeList.freq;
    }
    
    // see: https://dl.acm.org/doi/pdf/10.1145/98457.98523 Section 2.3 (citing https://dl.acm.org/doi/pdf/10.1145/1994.2022)
    private void access(Node<E> n) {
        if (n.nodeList.freq >= maxFreq) return;
        
        boolean matching_nodeList_exists = n.nodeList.next != null && n.nodeList.next.freq == n.nodeList.freq + 1;
        if (!matching_nodeList_exists && n.nodeList.size == 1) {
            // we can just change the freq of whole nodeList
            n.nodeList.freq++;
        } else {
            // we need to move the node to the matching nodeList
            NodeList<E> nodeListToAdd = n.nodeList.next;
            if (!matching_nodeList_exists) {
                // the matching nodeList does not exist yet
                // -> create it first
                nodeListToAdd = new NodeList<>(n.nodeList.freq + 1, frequencies);
                nodeListToAdd.prev = n.nodeList;
                nodeListToAdd.next = n.nodeList.next;
                if (nodeListToAdd.next != null) nodeListToAdd.next.prev = nodeListToAdd;
                else frequencies.tail = nodeListToAdd;
                n.nodeList.next = nodeListToAdd;
            }
            // mode the node
            n.remove();
            nodeListToAdd.addAtTail(n);
        }
        
        increaseFreqSum();
    }
    
    private void increaseFreqSum() {
        freqSum++;
        
        // dynamic aging: if average frequency is too high, halve all frequencies
//        System.out.println("freqSum="+freqSum + " size()=" + size() + " DA_fraction=" + DA_fraction + " maxFreq="+ maxFreq);
        if (freqSum / size() > DA_fraction * maxFreq) {
            
//        if (freqSum > freqSum_threshold_for_DA || add_operations_since_last_aging >= age_at_least_ever_add_operations) {

//        int brand_new = frequencies.head == null || frequencies.head.freq != 1 ? 0 : frequencies.head.size;
//        int not_brand_new = size() - brand_new;
//        if (not_brand_new != 0 && freqSum - brand_new / not_brand_new >= DA_fraction * maxFreq) {

//            System.out.println("dynamic aging!!!");
            DAs++;
            for (NodeList<E> curNodeList : frequencies) {
                int halved = (curNodeList.freq + 1) / 2;
                freqSum -= curNodeList.size * (curNodeList.freq - halved);
                curNodeList.freq = halved;
                if (curNodeList.prev != null && curNodeList.prev.freq == curNodeList.freq) {    // halving mapped two node lists to same frequency (e.g. 1->1 and 2->1)
                    curNodeList.prev.tail.next = curNodeList.head;
                    curNodeList.head.prev = curNodeList.prev.tail;
                    curNodeList.prev.tail = curNodeList.tail;
                    for (Node<E> curNode : curNodeList) curNode.nodeList = curNodeList.prev;
                    curNodeList.prev.size += curNodeList.size;
                    curNodeList.size = 0;
                    curNodeList.head = null;
                    curNodeList.tail = null;
                    curNodeList.remove();
                }
            }
            add_operations_since_last_aging = 0;
        }
    }
    
    public long getFrequencySum() {
        return freqSum;
    }
    
    public long getMinFrequency() {
        return frequencies.head == null ? 0 : frequencies.head.freq;
    }
    
    public long getMaxFrequency() {
        return frequencies.tail == null ? 0 : frequencies.tail.freq;
    }
    
    public double getAvgFrequency() {
        return 1.0 * freqSum / size();
    }

    @Override
    public Iterator<E> iterator() {
        Node<E> start = frequencies.head == null? null : frequencies.head.head;
        return new Iterator<E>() {
            Node<E> cur = start;
            @Override
            public boolean hasNext() {
                return cur != null;
            }

            @Override
            public E next() {
                Node<E> res = cur;
                if (cur == null) throw new NoSuchElementException();
                if (cur.next != null) {
                    cur = cur.next;
                } else if (cur.nodeList.next != null) {
                    cur = cur.nodeList.next.head;
                } else cur = null;
                return res.value;
            }
        };
    }
    
    public String toBetterString() {
        return "" + frequencies;
    }
    
    private class Node<V>{
        NodeList<V> nodeList;
        V value;
        Node<V> prev, next;
        
        public Node(V value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value.toString();
        }
        
        public void remove() {
            if (prev != null) prev.next = next;
            if (next != null) next.prev = prev;
            if (equals(nodeList.head)) nodeList.head = next;
            if (equals(nodeList.tail)) nodeList.tail = prev;
            nodeList.size--;
            if (nodeList.head == null) nodeList.remove();
        }
    }

    private class NodeList<V> implements Iterable<Node<V>>{
        FrequencyMap<V> freqMap;
        int freq;
        NodeList<V> prev, next;
        Node<V> head, tail;
        int size = 0;
        
        public NodeList(int freq, FrequencyMap<V> freqMap) {
            this.freqMap = freqMap;
            this.freq = freq;
        }
        
        public void addAtHead(Node<V> n){
            n.nodeList = this;
            if (head == null) {
                head = n;
                tail = n;
                n.prev = null;
                n.next = null;
            } else {
                n.next = head;
                n.prev = null;
                head.prev = n;
                head = n;
            }
            size++;
        }
        
        public void addAtTail(Node<V> n){
            n.nodeList = this;
            if (tail == null) {
                head = n;
                tail = n;
                n.prev = null;
                n.next = null;
            } else {
                n.prev = tail;
                n.next = null;
                tail.next = n;
                tail = n;
            }
            size++;
        }
        
        public void remove() {
            if (size != 0) return;
            if (prev != null) prev.next = next;
            if (next != null) next.prev = prev;
            if (equals(freqMap.head)) freqMap.head = next;
            if (equals(freqMap.tail)) freqMap.tail = prev;
        }
        
        @Override
        public Iterator<Node<V>> iterator() {
            Node<V> start = head;
            return new Iterator<Node<V>>() {
                Node<V> cur = start;
                @Override
                public boolean hasNext() {
                    return cur != null;
                }

                @Override
                public Node<V> next() {
                    Node<V> res = cur;
                    if (cur == null) throw new NoSuchElementException();
                    cur = cur.next;
                    return res;
                }
            };
        }

        @Override
        public String toString() {
            String res = "[";
            boolean first = true;
            for (Node<V> cur : this) {
                res += (first ? "" : ",") + cur;
                first = false;
            }
            return res + "]";
        }
    }
    
    private class FrequencyMap<V> implements Iterable<NodeList<V>>{
        NodeList<V> head, tail;
        
        public void add(Node<V> n){
            if (head == null) {
                head = new NodeList<>(1, this);
                tail = head;
            }
            if (head.freq != 1) {
                NodeList<V> newList = new NodeList<>(1, this);
                newList.next = head;
                head.prev = newList;
                head = newList;
            }
            head.addAtTail(n);
        }
        
        @Override
        public Iterator<NodeList<V>> iterator() {
            NodeList<V> start = head;
            return new Iterator<NodeList<V>>() {
                NodeList<V> cur = start;
                @Override
                public boolean hasNext() {
                    return cur != null;
                }

                @Override
                public NodeList<V> next() {
                    NodeList<V> res = cur;
                    if (cur == null) throw new NoSuchElementException();
                    cur = cur.next;
                    return res;
                }
            };
        }

        @Override
        public String toString() {
            String res = freqSum + " {";
            boolean first = true;
            for (NodeList<V> cur : this) {
                res += (first ? "" : ",") + cur.freq + ":" + cur;
                first = false;
            }
            return res + "}";
        }
    }
    
    /**
     * Check that the internal structure of cache is correct and make sure that all assumed invariants are holding.
     */
    public void validateInternalStructure() {
        if (!isEmpty() && frequencies.head == null) {
            throw new IllegalStateException("frequencies head was null but should not be??");
        }
        if ((frequencies.head != null) != (frequencies.tail != null)) {
            throw new IllegalStateException("head and tail dont match!!");
        }
        if (this.toArray().length != size()) {
            throw new IllegalStateException("Sizes do not match!" + this.toArray().length + " vs " + size());
        }
        int sum = 0;
        for (E v : this) {
            Node<E> n = nodes.get(v);
            sum += n.nodeList.freq;
            if (!(n.prev != null && n.prev.next == n || n.prev == null && n.nodeList.head == n)) {
                throw new IllegalStateException("!!! node prev pointer invariant failed!");
            }
            if (!(n.next != null && n.next.prev == n || n.next == null && n.nodeList.tail == n)) {
                throw new IllegalStateException("!!! node next pointer invariant failed!");
            }
        }
        if (sum != freqSum) {
            throw new IllegalStateException("!!! FreqSum should be " + sum + " but was " + freqSum);
        }
        int prevFreq = -1;
        for (NodeList<E> x : frequencies) {
            if (prevFreq >= x.freq) {
                throw new IllegalStateException("!!! frequencies are not ordered: " + prevFreq + ":" + x.prev + " " + x.freq + ":" + x);
            }
            prevFreq = x.freq;
            int i = 0;
            for (Node<E> n : x) {
                i++;
                if (n.nodeList != x) throw new IllegalStateException("Node " + x + " was in NodeList " + x + " but pointed to wrong NodeList " + n.nodeList);
            }
            if (i != x.size) {
                throw new IllegalStateException("!!! Size of " + x + " should be " + x.size + " but was " + i);
            }
        }
    }
    
    public static void main(String[] args) {
        LFU_DA_Cache<Integer> test = new LFU_DA_Cache<>(3);
        test.add(5);
        System.out.println(test.frequencies); test.validateInternalStructure();
        test.add(6);
        System.out.println(test.frequencies); test.validateInternalStructure();
        test.add(6);
        System.out.println(test.frequencies); test.validateInternalStructure();
        test.add(7);
        System.out.println(test.frequencies); test.validateInternalStructure();
        test.add(1);
        System.out.println(test.frequencies); test.validateInternalStructure();
        test.add(1);
        System.out.println(test.frequencies); test.validateInternalStructure();
        test.add(1);
        System.out.println(test.frequencies); test.validateInternalStructure();
        test.add(2);
        System.out.println(test.frequencies); test.validateInternalStructure();
        test.remove(6);
        System.out.println(test.frequencies); test.validateInternalStructure();
        test.add(2);
        System.out.println(test.frequencies); test.validateInternalStructure();
        test.add(2);
        System.out.println(test.frequencies); test.validateInternalStructure();
        test.add(2);
        System.out.println(test.frequencies); test.validateInternalStructure();
        test.add(5);
        System.out.println(test.frequencies); test.validateInternalStructure();
        test.add(5);
        System.out.println(test.frequencies); test.validateInternalStructure();
        test.add(2);
        System.out.println(test.frequencies); test.validateInternalStructure();
        test.add(2);
        System.out.println(test.frequencies); test.validateInternalStructure();
        test.add(1);
        System.out.println(test.frequencies); test.validateInternalStructure();
        test.add(1);
        System.out.println(test.frequencies); test.validateInternalStructure();
        System.out.println(test);
        
        test = new LFU_DA_Cache<>(10);
        Random rand = new Random();
        System.out.println(test);
        for (int i = 0; i < 100000; i++) {
//            int value = (int) Math.round(Stochastics.sampleExponential(rand , 0.2));
            int value = (int) Math.round(rand.nextGaussian()*3+10);
            if (rand.nextInt(100) == 0) {
                System.out.println("removing the value " + value);
                test.remove(value);
            }
            else test.add(value);
            test.validateInternalStructure();
            System.out.println(String.format("%-40s %-40s", test, test.frequencies));
//            System.out.println("        " + test);
        }
    }
    
}