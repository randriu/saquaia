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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core.util;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jankretinsky
 * tuple of length 2
 */
public class Pair<L, R> {

    public final L left;
    public final R right;

    public Pair(L l, R r) {
        left = l;
        right = r;
    }

    @Override
    public String toString() {
        return "<" + (left==null ? "null" : left.toString()) + ", " + (right==null ? "null" : right.toString()) + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Pair<?, ?>) {
            try {
                @SuppressWarnings("unchecked")
                Pair<L, R> pair = (Pair<L, R>) o;
                return left.equals(pair.left) && right.equals(pair.right);
            } catch (ClassCastException e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int l = 0, r = 0;
        if (left != null) {
            l = left.hashCode();
        }
        if (right != null) {
            r = right.hashCode();
        }
        return 17 * l + 5 * r;
    }
    
    public static <A,B> List<A> getFirst(List<Pair<A,B>> list_of_pairs) {
        ArrayList<A> res = new ArrayList<>(list_of_pairs.size());
        for (Pair<A,B> p : list_of_pairs) res.add(p.left);
        return res;
    }
    
    public static <A,B> List<B> getSecond(List<Pair<A,B>> list_of_pairs) {
        ArrayList<B> res = new ArrayList<>(list_of_pairs.size());
        for (Pair<A,B> p : list_of_pairs) res.add(p.right);
        return res;
    }
}
