package org.bdgp.util;

/**
 * Interface for object comparators. Very similar to java.util.Comparator
 * @see VectorUtil
 */
public interface Comparator extends ComparisonConstants {
    public int compare(Object a, Object b);
}
