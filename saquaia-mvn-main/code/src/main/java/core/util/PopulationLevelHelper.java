package core.util;

import core.model.Interval;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;

/**
 *
 * @author Martin
 */
public class PopulationLevelHelper {

    public static int maxNeighborSizeFor(int size, double population_level_growth_factor) {
        //if (size == 1 && population_level_growth_factor > 1) return 2;
        int res = (int) Math.ceil(population_level_growth_factor * size);
        return res >= 1 ? res : 1;
    }

    public static boolean checkIntervals(Interval[] intervals) {
        return checkIntervals(intervals, null);
    }

    /**
     * Check if a population level abstraction can be used with segmental
     * simulation.
     *
     * @param intervals population level abstraction (i.e. partitioning of
     * population)
     * @param population_level_growth_factor neighnouring intervals differ in
     * size by at most this factor
     * @return true, iff population level abstraction can be used with segmental
     * simulation.
     */
    public static boolean checkIntervals(Interval[] intervals, Double population_level_growth_factor) {
        return problemsWithIntervals(intervals, population_level_growth_factor).isEmpty();
    }

    public static ArrayList<String> problemsWithIntervals(Interval[] intervals) {
        return problemsWithIntervals(intervals, null);
    }

    /**
     * Compute problems of a population level abstraction. Problems can be gaps
     * and intervals that can be skipped by segmental simulation.
     *
     * @param intervals population level abstraction (i.e. partitioning of
     * population)
     * @param population_level_growth_factor neighnouring intervals differ in
     * size by at most this factor
     * @return list of problem descriptions
     */
    public static ArrayList<String> problemsWithIntervals(Interval[] intervals, Double population_level_growth_factor) {
        ArrayList<String> problems = new ArrayList<>();
        for (int i = 0; i < intervals.length; i++) {
            Interval cur = intervals[i];
            if (i > 0) {
                Interval prev = intervals[i - 1];
                // check that there is nothing missing
                if (prev.max != cur.min - 1) {
                    problems.add("gap between intervals " + prev + " and " + cur);
                }
                // check that we cannot skip prev interval
                int negativeStep = prev.max - cur.rep;
                if (cur.min + negativeStep < prev.min) {
                    problems.add("from + " + cur + " you can step over " + prev);
                }
                // check that the growth factor was respected
                if (population_level_growth_factor != null && maxNeighborSizeFor(cur.size(), population_level_growth_factor) < prev.size()) {
                    problems.add(cur + " has with size " + cur.size() + ", thus neighbours may at most have size " + maxNeighborSizeFor(cur.size(), population_level_growth_factor) + " but " + prev + " has size " + prev.size() + ".");
                }
            }
            if (i + 1 < intervals.length) {
                Interval next = intervals[i + 1];
                // check that we cannot skip next interval
                int positiveStep = next.min - cur.rep;
                if (cur.max + positiveStep > next.max) {
                    problems.add("from + " + cur + " you can step over " + next);
                }
                // check that the growth factor was respected
                if (population_level_growth_factor != null && maxNeighborSizeFor(cur.size(), population_level_growth_factor) < next.size()) {
                    problems.add(cur + " has with size " + cur.size() + ", thus neighbours may at most have size " + maxNeighborSizeFor(cur.size(), population_level_growth_factor) + " but " + next + " has size " + next.size() + ".");
                }
            }
        }
        return problems;
    }

    /**
     * Check for three consecutive intervals (prev,cur,next) if the middle can
     * be skipped in a segmental simulation. If prev can skip cur, return a
     * split of prev, where the new neighbor of cur cannot skip cur. If next can
     * skip cur, return a split of next, where the new neighbor of cur cannot
     * skip cur.
     *
     * @param prev Previous interval with smallest values. May be null.
     * @param cur Current interval.
     * @param next Next interval with largest values. May be null.
     * @param population_level_growth_factor When splitting large intervals
     * (i.e. with size > 2*|cur|), then the size of new neighbor will be this
     * much larger than the size of the current interval.
     * @return Two replacements that may be null if there was not problem. The
     * first is a split of prev into two intervals. The second is a split of
     * next into two intervals.
     */
    public static Pair<Pair<Interval, Interval>, Pair<Interval, Interval>> wantsToReplace(Interval prev, Interval cur, Interval next, double population_level_growth_factor) {
        if (prev != null && prev.max != cur.min - 1 || next != null && cur.max != next.min - 1) {
            throw new IllegalArgumentException("Intervals not consecutive.");
        }

        int targetNeighborSize = maxNeighborSizeFor(cur.size(), population_level_growth_factor);
        int maxNeighborSize = maxNeighborSizeFor(cur.size(), Math.max(population_level_growth_factor, 2));

        Pair<Interval, Interval> replacePrev = null;
        // can prev step over cur? or prev is too large?
        if (prev != null) {
            int step = cur.min - prev.rep;

            if (prev.max + step > cur.max || prev.size() > maxNeighborSize) {
                // yes --> wants to replace prev!
                Interval prevLeft, prevRight;
                if (prev.size() <= 2 * targetNeighborSize) {
                    // split in middle
                    prevLeft = new Interval(
                            prev.min,
                            (prev.min + prev.max - 1) / 2,
                            false);
                    prevRight = new Interval(
                            (prev.min + prev.max + 1) / 2,
                            prev.max,
                            true);
                } else {
                    // split with target size
                    prevLeft = new Interval(
                            prev.min,
                            prev.max - targetNeighborSize,
                            false);
                    prevRight = new Interval(
                            prev.max - targetNeighborSize + 1,
                            prev.max,
                            true);
                }
                // put representatives as close to middle as possible
                if (prevRight.rep < cur.min - cur.size()) {
                    prevRight = new Interval(prevRight.min, cur.min - cur.size(), prevRight.max);
                }
                if (prev.rep < prevLeft.rep) {
                    prevLeft = new Interval(prevLeft.min, prev.rep, prevLeft.max);
                }
                if (prevLeft.rep < prevRight.min - prevRight.size()) {
                    prevLeft = new Interval(prevLeft.min, prevRight.min - prevRight.size(), prevLeft.max);
                }
                replacePrev = new Pair<>(prevLeft, prevRight);
            }
        }

        Pair<Interval, Interval> replaceNext = null;
        // can next step over cur? or next is too large?
        if (next != null) {
            int step = cur.max - next.rep;

            if (next.min + step < cur.min || next.size() > maxNeighborSize) {
                // yes --> wants to replace next!

                Interval nextLeft, nextRight;
                if (next.size() <= 2 * targetNeighborSize) {
                    // split in middle
                    nextLeft = new Interval(
                            next.min,
                            (next.min + next.max - 1) / 2,
                            false);
                    nextRight = new Interval(
                            (next.min + next.max + 1) / 2,
                            next.max,
                            true);
                } else {
                    // split with target size
                    nextLeft = new Interval(next.min,
                            next.min + targetNeighborSize - 1,
                            false);
                    nextRight = new Interval(next.min + targetNeighborSize,
                            next.max,
                            true);
                }
                // put representatives as close to middle as possible
                if (nextLeft.rep > cur.max + cur.size()) {
                    nextLeft = new Interval(nextLeft.min, cur.max + cur.size(), nextLeft.max);
                }
                if (next.rep > nextRight.rep) {
                    nextRight = new Interval(nextRight.min, next.rep, nextRight.max);
                }
                if (nextRight.rep > nextLeft.max + nextLeft.size()) {
                    nextRight = new Interval(nextRight.min, nextLeft.max + nextLeft.size(), nextRight.max);
                }
                replaceNext = new Pair<>(nextLeft, nextRight);
            }
        }
        return new Pair<>(replacePrev, replaceNext);
    }

    /**
     * Computes intervals for one dimension where segemental simulation cannot
     * skip any levels. The result can we tested with checkInterval().
     *
     * @param min minimum value (inclusive)
     * @param max maximum value (inclusive)
     * @param population_level_growth_factor describes target groth between
     * neighboring intervals
     * @param forced for each forced value v there will be an interval with
     * minimum v
     * @return intervals usable by segmental simulation
     */
    public static Interval[] intervalsFor(int min, int max, double population_level_growth_factor, Collection<Integer> forced) {
//        if (forced.size() == 0) throw new IllegalArgumentException("You need to force at least one value. Usually 1...");

        TreeSet<Integer> t = new TreeSet<>();
        for (int i : forced) {
            if (i > min && i <= max) {
                t.add(i);
            }
        }
        ArrayList<Integer> t2 = new ArrayList<>(t);

        ArrayList<Interval> res = new ArrayList<>();
        if (t2.isEmpty()) {
            res.add(new Interval(min, max));
        } else {
            res.add(new Interval(min, t2.get(0) - 1));
            for (int i = 0; i < t2.size() - 1; i++) {
                int left = t2.get(i);
                int right = t2.get(i + 1);
                res.add(new Interval(left, right - 1));
            }
            res.add(new Interval(t2.get(t2.size() - 1), max));
        }

        while (true) {
            // get smallest problem interval
            int problemIntervalPos = -1;
            int problemIntervalSize = Integer.MAX_VALUE;
            Interval problemInterval = null;
            Pair<Pair<Interval, Interval>, Pair<Interval, Interval>> solution = null;
            for (int i = res.size() - 1; i >= 0; i--) {
                Interval cur = res.get(i);
                if (cur.size() >= problemIntervalSize) {
                    continue;
                }

                Pair<Pair<Interval, Interval>, Pair<Interval, Interval>> cur_sol = wantsToReplace(i > 0 ? res.get(i - 1) : null, cur, i < res.size() - 1 ? res.get(i + 1) : null, population_level_growth_factor);
                if (cur_sol.left != null || cur_sol.right != null) {
                    problemIntervalPos = i;
                    problemIntervalSize = cur.size();
                    problemInterval = cur;
                    solution = cur_sol;
                }
            }
            if (problemInterval == null) {
                break;
            }

            // handle problem
            if (solution.right != null) {
                res.remove(problemIntervalPos + 1);
                res.add(problemIntervalPos + 1, solution.right.right);
                res.add(problemIntervalPos + 1, solution.right.left);
            }
            if (solution.left != null) {
                res.remove(problemIntervalPos - 1);
                res.add(problemIntervalPos - 1, solution.left.right);
                res.add(problemIntervalPos - 1, solution.left.left);
            }
        }
        return res.toArray(Interval[]::new);
    }

    public static void main(String[] args) {
        wantsToReplace(new Interval(2, 5, 8), new Interval(9, 9, 9), new Interval(10, 10, 11), 2);
        wantsToReplace(new Interval(2, 5, 8), new Interval(9, 9, 9), new Interval(10, 11, 11), 2);

        Interval[] intervalsFor = intervalsFor(0, 1000, 2, Arrays.asList(1, 8, 9, 30, 35));
        System.out.println(Arrays.toString(intervalsFor));
        System.out.println(problemsWithIntervals(intervalsFor));
        System.out.println("");

        intervalsFor = intervalsFor(0, 1000, 1.5, Arrays.asList(1, 8, 9, 30, 35));
        System.out.println(Arrays.toString(intervalsFor));
        System.out.println(problemsWithIntervals(intervalsFor));
        System.out.println("");

        intervalsFor = intervalsFor(0, 1000, 1.25, Arrays.asList(1, 8, 9, 30, 35));
        System.out.println(Arrays.toString(intervalsFor));
        System.out.println(problemsWithIntervals(intervalsFor));
        System.out.println("");

        intervalsFor = intervalsFor(0, 1000, 1, Arrays.asList(1, 8, 9, 30, 35));
        System.out.println(Arrays.toString(intervalsFor));
        System.out.println(problemsWithIntervals(intervalsFor));
        System.out.println("");

        intervalsFor = intervalsFor(0, 1000, 2, Arrays.asList(1, 2, 500, 510, 512));
        System.out.println(Arrays.toString(intervalsFor));
        System.out.println(problemsWithIntervals(intervalsFor));
        System.out.println("");

        intervalsFor = intervalsFor(0, 50, 2, Arrays.asList(1, 2, 6, 7));
        System.out.println(Arrays.toString(intervalsFor));
        System.out.println(problemsWithIntervals(intervalsFor));
        System.out.println("");

        intervalsFor = intervalsFor(0, 1000, 3, Arrays.asList(100, 101, 111, 113));
        System.out.println(Arrays.toString(intervalsFor));
        System.out.println(problemsWithIntervals(intervalsFor));
        System.out.println("");

        intervalsFor = intervalsFor(100, 128, 3.0, Arrays.asList(100, 101, 127, 128));
        System.out.println(Arrays.toString(intervalsFor));
        System.out.println(problemsWithIntervals(intervalsFor, 3.0));
        System.out.println("");

        intervalsFor = intervalsFor(100, 128, 1.0, Arrays.asList(100, 103, 126, 128));
        System.out.println(Arrays.toString(intervalsFor));
        System.out.println(problemsWithIntervals(intervalsFor, 1.0));
        System.out.println("");

        intervalsFor = intervalsFor(100, 128, 1.0, Arrays.asList());
        System.out.println(Arrays.toString(intervalsFor));
        System.out.println(problemsWithIntervals(intervalsFor, 1.0));
        System.out.println("");

        intervalsFor = new Interval[]{new Interval(100, 100, 100), new Interval(101, 101, 104), new Interval(105, 108, 108), new Interval(109, 109, 109)};
        System.out.println(Arrays.toString(intervalsFor));
        System.out.println(problemsWithIntervals(intervalsFor, 2.4));
        System.out.println("");

        System.out.println("---------------------------------");
        System.out.println();

        double c = 1.01;
        for (int x = 5; x <= 24; x++) {
            intervalsFor = intervalsFor(0, x, c, Arrays.asList(1, x));
            for (Interval i : intervalsFor) {
                System.out.print(i.size() + ", ");
            }
            System.out.println("");
            System.out.println(Arrays.toString(intervalsFor));
            System.out.println(problemsWithIntervals(intervalsFor, c));
            System.out.println("");
        }

        System.out.println("---------------------------------");
        System.out.println();

        c = 3;
        for (int x = 12; x <= 42; x++) {
            intervalsFor = intervalsFor(0, x, c, Arrays.asList(1, x));
            for (Interval i : intervalsFor) {
                System.out.print(i.size() + ", ");
            }
            System.out.println("");
            System.out.println(Arrays.toString(intervalsFor));
            System.out.println(problemsWithIntervals(intervalsFor, c));
            System.out.println("");
        }

        System.out.println("---------------------------------");
        System.out.println();

        c = 4;
        intervalsFor = intervalsFor(0, 100000, c, Arrays.asList(1));
        System.out.println(Arrays.toString(intervalsFor));
        System.out.println(problemsWithIntervals(intervalsFor, c));
        System.out.println("");
    }
}
