package org.bdgp.util;

public class ComparableComparator implements Comparator {
    public int compare(Object a, Object b) {
	Comparable ca = (Comparable) a;
	Comparable cb = (Comparable) b;
	return ca.compareTo(cb);
    }
}
