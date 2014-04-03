/**
 * This file is part of SecureNIO. Copyright (C) 2014 K. Dermitzakis
 * <dermitza@gmail.com>
 *
 * SecureNIO is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * SecureNIO is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with SecureNIO. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.dermitza.securenio.util;

import java.util.Collections;
import java.util.HashSet;

/**
 * An extension of the {@link HashSet} implementation, with a reference to a
 * minimum element in the set.
 *
 * @param <T> The type of object to be contained, must extend {@link Comparable}
 * 
 * @author K. Dermitzakis
 * @version 0.18
 */
public class MinContainer<T extends Object & Comparable<? super T>> {

    private final HashSet<T> elements = new HashSet<>();
    private T min = null;

    /**
     * Add an element to the underlying {@link HashSet} and recalculate the
     * minimum element in this set.
     *
     * @param t The element to add to the container
     * @return if this set did not already contain the specified element
     */
    public boolean add(T t) {
        boolean ret = elements.add(t);
        if (min == null) {
            min = t;
            return ret;
        }

        if ((t.compareTo(min) < 0) ? true : false) {
            min = t;
        }
        return ret;
    }

    /**
     *
     * @return true if this container contains no elements.
     */
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /**
     * Get the minimum element in the the underlying {@link HashSet}
     *
     * @return the minimum element in the the underlying {@link HashSet}
     */
    public T getMin() {
        return min;
    }

    /**
     * Remove the given element from the underlying {@link HashSet} and
     * recalculate the minimum element contained within. If the underlying
     * {@link HashSet} is empty, the minimum element is set to null.
     *
     * @param t The element to be removed from the container
     * @return true if this set contained the specified element
     */
    public boolean remove(T t) {
        boolean ret = elements.remove(t);
        if (t.equals(min) && !elements.isEmpty()) {
            min = Collections.min(elements);
        }
        if (elements.isEmpty()) {
            min = null;
        }
        return ret;
    }

    /**
     * Clears the underlying {@link HashSet} and nullifies the minimum element.
     */
    public void clear() {
        min = null;
        elements.clear();
    }
}
