package org.bdgp.util;

/**
 * A ReverseComparator wraps another Comparator and returns the reverse
 * ordering of the wrapped comparator.
 */
public class ReverseComparator implements Comparator {

    private Comparator original;
    
    public ReverseComparator(Comparator in) {
	original = in;
    }
    
    public int compare(Object a, Object b) {
	int comp = original.compare(a, b);
	if (comp == ComparisonConstants.GREATER_THAN)
	    return ComparisonConstants.LESS_THAN;
	else if (comp == ComparisonConstants.LESS_THAN)
	    return ComparisonConstants.GREATER_THAN;
	else
	    return ComparisonConstants.EQUAL_TO;
    }
}

