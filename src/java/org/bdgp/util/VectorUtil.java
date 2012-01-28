package org.bdgp.util;

import java.util.*;
import java.lang.reflect.*;

/**
 * A collection of static methods for manipulating Vectors.
 * @see java.util.Vector
 */
public class VectorUtil {

    private final static ComparableComparator comparableComparator =
    new ComparableComparator();
    private final static EqualsEqualityComparator equalityComparator =
    new EqualsEqualityComparator();

    private VectorUtil() {
    }

    /**
     * Adds all the elements from vector b into vector a. Equivalent to
     * a.addAll(b); Note that a's contents are modified in place
     * @param a the vector to merge into
     * @param b the vector to merge from
     * @return the resulting vector (always the same as a)
     */
    public static Vector mergeVectors(Vector a, Vector b) {
	for(int i=0; i < b.size(); i++) {
	    if (!a.contains(b.elementAt(i)))
		a.addElement(b.elementAt(i));
	}
	return a;
    }

    public static Vector condense(Vector vector,
				  VectorCondenser condenser) {
	int oldCount = 0;
	do {
	    oldCount = vector.size();
	    vector = VectorUtil.doCondense(vector, condenser);
	} while(oldCount != vector.size());

	return vector;
    }

    private static Vector doCondense(Vector vector,
				     VectorCondenser condenser) {
	vector = (Vector) vector.clone();
	Vector out = new Vector();

	int lastIndex = 0;
	while(lastIndex < vector.size()) {
	    Object item = vector.elementAt(lastIndex);
	    if (lastIndex + 1 >= vector.size()) {
		out.addElement(item);
		break;
	    }
	    boolean condensed = false;
	    for(int i=lastIndex+1; i < vector.size(); i++) {
	        Object item2 = vector.elementAt(i);
		Pair pair = condenser.condense(item, item2);
		if (pair != null && (pair.getA() != null ||
				     pair.getB() != null)) {
		    condensed = true;
		    if (pair.getB() == VectorCondenser.REMOVE_OP) {
			vector.removeElementAt(i);
		    } else if (pair.getB() != null) {
			vector.setElementAt(pair.getB(), i);
		    }
		    
		    if (pair.getA() == VectorCondenser.REMOVE_OP) {
			vector.removeElementAt(lastIndex);
		    } else if (pair.getA() != null) {
			vector.setElementAt(pair.getA(), lastIndex);
		    }
		    break;
		}
	    }
	    if (!condensed) {
		out.addElement(item);
		lastIndex++;
	    }
	}
	return out;
    }

    /**
     * Creates a new Vector containing clones of all the elements of the
     * original Vector. If an element cannot be cloned, it will be set to
     * null.
     * @param values the Vector to clone
     * @return a vector containing clones of all the elements of values
     */
    public static Vector trueClone(Vector values) {
	Vector out = new Vector(values.size());
	for(int i=0; i < values.size(); i++) {
	    out.addElement(ObjectUtil.cloneObject(values.elementAt(i)));
	}
	return out;
    }

    /**
     * Creates a new List containing clones of all the elements of the
     * original List. If an element cannot be cloned, it will be set to
     * null.
     * @param values the List to clone
     * @return a List containing clones of all the elements of values
     */
    public static List trueClone(List values) {
	Vector out = new Vector(values.size());
	for(int i=0; i < values.size(); i++) {
	    out.add(ObjectUtil.cloneObject(values.get(i)));
	}
	return out;
    }

    /**
     * Tests to see if two vectors contain the same data, perhaps in a
     * different order
     * @param a a vector
     * @param b a vector
     * @return whether or not the vectors have the same elements
     */
    public static boolean hasSameContents(Vector a, Vector b) {
	return hasSameContents(a, b, equalityComparator);
    }

    /**
     * Tests to see if two vectors contain the same data, perhaps in a
     * different order
     * @param a a vector
     * @param b a vector
     * @param cmp the comparitor to use for elements of the vector
     * @return whether or not the vectors have the same elements
     */
    public static boolean hasSameContents(Vector a, Vector b,
					  EqualityComparator cmp) {
	if (a.size() != b.size())
	    return false;
	Iterator it = a.iterator();
	while(it.hasNext()) {
	    if (!contains(b, it.next(), cmp))
		return false;
	}
	return true;
    }

    /**
     * Tests to see if a vector contains a certain object, given a user
     * specified equality comparator
     * @param vector a vector
     * @param o the object to find
     * @param cmp the comparitor to use for elements of the vector
     * @return whether vector contains an element that is equal to o
     */
    public static boolean contains(Vector vector, Object o,
				   EqualityComparator cmp) {
	Iterator it = vector.iterator();
	while(it.hasNext())
	    if (cmp.equals(it.next(), o))
		return true;
	return false;
    }

    /**
     * Creates a transformed Vector by calling transformers
     * transform method on each element in values. The original
     * Vector is not modified
     * @param transformer the VectorTransformer to apply
     * @param values the Vector to transform
     * @return the transformed Vector
     * @see org.bdgp.util.VectorTransformer
     */
    public static Vector transform(VectorTransformer transformer,
				   Vector values) {
	return filter(null, transformer, values);
    }

    /**
     * Creates a transformed Vector by calling transformers
     * transform method on each element in values. The enumeration
     * is emptied by calling this method
     * @param transformer the VectorTransformer to apply
     * @param values the enumeration to transform
     * @return the transformed Vector
     * @see org.bdgp.util.VectorTransformer
     */
    public static Vector transform(VectorTransformer transformer,
				   Enumeration values) {
	return filter(null, transformer, values);
    }


    /**
     * Creates a filtered Vector by calling the satisfies() method
     * of filter on each element of values. If the satisfies() method
     * returns true, that element is added to the output vector.
     * The original Vector is not modified.
     * @param filter the VectorFilter to apply
     * @param values the Vector to filter
     * @return the filtered Vector
     * @see org.bdgp.util.VectorFilter
     */
    public static Vector filter(VectorFilter filter, Vector values) {
	return filter(filter, null, values);
    }

    /**
     * Creates a filtered Vector by calling the satisfies() method
     * of filter on each element of values. If the satisfies() method
     * returns true, that element is added to the output vector.
     * The enumeration is emptied by calling this method.
     * @param filter the VectorFilter to apply
     * @param values the Enumeration to filter
     * @return the filtered Vector
     * @see org.bdgp.util.VectorFilter
     */
    public static Vector filter(VectorFilter filter, Enumeration values) {
	return filter(filter, null, values);
    }

    /**
     * Filters and transforms a Vector. The equivalent of calling
     * VectorUtil.transform(transformer, VectorUtil.filter(filter, values))
     * @param filter the VectorFilter to apply
     * @param values the Vector to filter and transform
     * @return the filtered Vector
     */
    public static Vector filter(VectorFilter filter,
				VectorTransformer transformer,
				Vector values) {
	Vector out = new Vector(values.size());
	for(int i=0; i < values.size(); i++) {
	    Object o = values.elementAt(i);
	    if (filter == null ||
		filter.satisfies(o)) {
		if (transformer != null)
		    o = transformer.transform(o);
		out.addElement(o);
	    }
	}
	out.trimToSize();
	return out;
    }

     /**
     * Filters and transforms an Enumeration. The equivalent of calling
     * VectorUtil.transform(transformer, VectorUtil.filter(filter, values))
     * @param filter the VectorFilter to apply
     * @param values the Enumeration to filter and transform
     * @return the filtered Vector
     */
    public static Vector filter(VectorFilter filter,
				VectorTransformer transformer,
				Enumeration values) {
	Vector out = new Vector();
	while(values.hasMoreElements()) {
	    Object o = values.nextElement();
	    if (filter == null ||
		filter.satisfies(o)) {
		if (transformer != null)
		    o = transformer.transform(o);
		out.addElement(o);
	    }
	}
	out.trimToSize();
	return out;	
    }

    /**
     * Creates a Vector containing all the elements of an Enumeration
     * @param e an enumeration
     * @return a vector containing the elements of the enumeration
     */
    public static Vector getVector(Enumeration e) {
	Vector out = new Vector();
	while(e.hasMoreElements())
	    out.addElement(e.nextElement());
	return out;
    }

    private static void quickSort(Vector a, Comparator c, int lo0, int hi0) {
        int lo = lo0;
        int hi = hi0;

        if (lo >= hi) {
            return;
        }

	Object loElt = (Object) a.elementAt(lo);
	Object hiElt = (Object) a.elementAt(hi);

        if( lo == hi - 1 ) {
            /*
             *  sort a two element list by swapping if necessary 
             */
            if (c.compare(loElt, hiElt) == ComparisonConstants.GREATER_THAN) {
		a.setElementAt(hiElt, lo);
		a.setElementAt(loElt, hi);
            }
            return;
        }


        /*
         *  Pick a pivot and move it out of the way
         */
	Object pivot = a.elementAt((lo + hi) / 2);

        a.setElementAt(a.elementAt(hi), (lo + hi) / 2);
	a.setElementAt(pivot, hi);

        while( lo < hi ) {
            /*
             *  Search forward from a[lo] until an element is found that
             *  is greater than the pivot or lo >= hi 
             */
	    while((c.compare(a.elementAt(lo), pivot) !=
		   ComparisonConstants.GREATER_THAN) &&
		  lo < hi) {
                lo++;
            }

            /*
             *  Search backward from a[hi] until element is found that
             *  is less than the pivot, or lo >= hi
             */
	    while((c.compare(pivot, a.elementAt(hi)) !=
		   ComparisonConstants.GREATER_THAN) &&
		  lo < hi) {
                hi--;
            }

            /*
             *  Swap elements a[lo] and a[hi]
             */
            if( lo < hi ) {
                Object t = a.elementAt(lo);
		a.setElementAt(a.elementAt(hi), lo);
		a.setElementAt(t, hi);
            }
        }

        /*
         *  Put the median in the "center" of the list
         */
        a.setElementAt(a.elementAt(hi), hi0);
        a.setElementAt(pivot, hi);

        /*
         *  Recursive calls, elements a[lo0] to a[lo-1] are less than or
         *  equal to pivot, elements a[hi+1] to a[hi0] are greater than
         *  pivot.
         */
        quickSort(a, c, lo0, lo-1);
        quickSort(a, c, hi+1, hi0);
    }

    /**
     * Binary searches a vector for a particular object. This assumes that
     * all the elements in the Vector are Comparable. Since this is
     * binary search, the Vector must be sorted to get a correct result
     * @param v a sorted Vector to search
     * @param o the object to find
     * @return int the index of the object or -1 if not found
     */
    public static int binarySearch(Vector v, Object o) {
	return binarySearch(v,o,false);
    }

    /**
     * Binary searches for object o, assuming that all the objects in the
     * array are Comparable.
     * @param v a sorted Vector to search
     * @param o the object to find
     * @param findSpace if true, return the index where the object would have
     *  been if it were in the Vector
     * @return the index of object or -1 if not found
     */
    public static int binarySearch(Vector v, Object o, boolean findSpace) {
	return binarySearch(v,o,comparableComparator, findSpace);
    }

    /**
     * Binary searches for object o, using a user specified comparator
     * @param v a sorted Vector to search
     * @param o the object to find
     * @param c the comparator to use for comparisons
     * @param findSpace if true, return the index where the object would have
     *  been if it were in the Vector
     * @return the index of object or -1 if not found
     */
    public static int binarySearch(Vector v, Object o, Comparator c, boolean findSpace) {
	if (v.size() == 0) {
	    if (findSpace)
		return 0;
	    else
		return -1;
	}
	int first = 0;
	int last = v.size()-1;
	int middle = -1;
	while(first <= last) {
	    middle = (first+last) / 2;

	    Object middleElt = v.elementAt(middle);
	    int compValue = c.compare(middleElt,o);

	    if (findSpace) {
		boolean leftSmaller = true;
		boolean rightLarger = (compValue == 1);

		if (middle > 0)
		    leftSmaller = (c.compare(o,v.elementAt(middle-1))
				   != -1);
		if (leftSmaller && rightLarger)
		    return middle;
	    }

	    if (compValue == 0)
		if (findSpace)
		    return middle+1;
		else
		    return middle;
	    else if (compValue == -1)
		first = middle + 1;
	    else
		last = middle - 1;
	}
	if (findSpace)
	    return v.size();
	else
	    return -1;
    }

    /**
     * Creates a sorted Vector out of an Enumeration using insertionSort
     * @param e the enumeration to sort
     * @param c the comparator to use for sorting
     * @return a sorted Vector
     */
    public static Vector insertionSort(Enumeration e, Comparator c) {
	Vector out = new Vector();
	while(e.hasMoreElements()) {
	    Object o = e.nextElement();
	    int pos = binarySearch(out, o, c, true);
	    out.insertElementAt(o, pos);
	}
	return out;
    }

    /**
     * Inserts an object into a sorted vector so that the vector remains
     * sorted. The insert is done in log(n)+(vector insert cost) time.
     * @param v the vector to insert into
     * @param c the comparator that defines the sort order on this vector
     * @param o the object to insert
     */
    public static void insertSorted(Vector v, Comparator c, Object o) {
	int pos = binarySearch(v, o, c, true);
	v.insertElementAt(o, pos);
    }

    /**
     * Inserts an comparable object into a sorted vector
     * so that the vector remains sorted. The insert is done in
     * log(n)+(vector insert cost) time.
     * @param v the vector to insert into
     * @param o the object to insert
     */
    public static void insertSorted(Vector v, Object o) {
	int pos = binarySearch(v, o, comparableComparator, true);
	v.insertElementAt(o, pos);
    }

    /**
     * Creates a sorted vector out of an enumeration using insertionSort.
     * The enumeration will be emptied
     * @param e the enumeration to sort
     * @param c the comparator to use for sorting
     * @param reverse if true, put the sorted vector in reverse order
     * @return a sorted Vector
     */
    public static Vector sort(Enumeration e, Comparator c, boolean reverse) {
	if (reverse)
	    c = new ReverseComparator(c);
	return insertionSort(e, c);
    }

    /**
     * Creates a sorted vector out of an enumeration using insertionSort.
     * Equivalent to calling sort(e, c, false)
     * The enumeration will be emptied
     * @param e the enumeration to sort
     * @param c the comparator to use for sorting
     * @return a sorted Vector
     */
    public static Vector sort(Enumeration e, Comparator c) {
	return sort(e, c, false);
    }

    /**
     * Creates a sorted vector out of an enumeration using insertionSort,
     * assuming
     * that every element in the enumeration implements Comparable.
     * Equivalent to calling sort(e, new ComparableComparator(), false) .
     * The enumeration will be emptied.
     * @param e the enumeration to sort
     * @return a sorted Vector
     */
    public static Vector sort(Enumeration e) {
	return sort(e, comparableComparator);
    }


    /**
     * Sorts a vector in place using the default sorting algorithm
     * assuming all the elements in the Vector implement Comparable.
     * Equivalent to sort(a, new ComparableComparator())
     * @param a the Vector to sort
     * @return a sorted Vector
     */
    public static Vector sort(Vector a) {
	return sort(a, comparableComparator);
    }


    /**
     * Sorts a vector in place using the default sorting algorithm
     * assuming all the elements in the Vector implement Comparable.
     * Equivalent to sort(a, new ComparableComparator(), reverse)
     * @param a the Vector to sort
     * @param reverse if true, put the sorted vector in reverse order
     * @return a sorted Vector
     */
    public static Vector sort(Vector a, boolean reverse) {
	return sort(a, comparableComparator, reverse);
    }

    /**
     * Sorts a vector in place using the default sorting algorithm
     * and a user specified comparator.
     * Equivalent to sort(a, c, false)
     * @param a the Vector to sort
     * @param c the Comparator to use for sorting
     * @return a sorted Vector
     */
    public static Vector sort(Vector a, Comparator c) {
	return sort(a, c, false);
    }

    /**
     * Sorts a Vector in place using the default sorting algorithm
     * and a user specified comparator. This is currently equivalent to
     * quickSort(a, (reverse ? new ReverseComparator(c) : c)), but
     * the default sorting algorithm could be changed to something better.
     * The Vector given will be modified.
     * @param a the Vector to sort
     * @param c the Comparator to use for sorting
     * @param reverse if true, put the sorted vector in reverse order
     * @return a sorted Vector
     */
    public static Vector sort(Vector a, Comparator c, boolean reverse) {
	if (reverse)
	    c = new ReverseComparator(c);
	return quickSort(a, c);
    }

    /**
     * Sorts a Vector in place using quickSort.
     * @param a the Vector to sort
     * @param c the Comparator to use for sorting
     * @return a sorted Vector
     */
    public static Vector quickSort(Vector a, Comparator c) {
	quickSort(a, c, 0, a.size() - 1);
	return a;
    }

    /**
     * Sorts a Vector in place using quickSort, assuming all the
     * elements of the Vector implement Comparable
     * @param a the Vector to sort
     * @return a sorted Vector
     */
    public static Vector quickSort(Vector a) {
	return quickSort(a, comparableComparator);
    }
}
