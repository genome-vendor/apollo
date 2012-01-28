package org.bdgp.util;

import java.util.*;

public class RangeHash {

    private Hashtable data;
    private Vector ranges;

    private Comparator rangeComparator = new RangeComparator();

    protected static boolean equals(Range a, Range b) {
	return a.getLow() == b.getLow() && a.getHigh() == b.getHigh();
    }

    protected static int compare(Range a, Range b) {
	if (a.getLow() < b.getLow())
	    return -1;
	else if (a.getLow() > b.getLow())
	    return 1;
	else
	    return 0;
    }

    protected static boolean contains(Range r, int value) {
	return value >= r.getLow() && value <= r.getHigh();
    }

    protected static boolean overlaps(Range a, Range b) {
	return (contains(a, b.getLow()) ||
		contains(a, b.getHigh()) ||
		contains(b, a.getLow()) ||
		contains(b,a.getHigh()));
    }

    private class RangeComparator implements Comparator {
	public int compare(Object a, Object b) {
	    Range ah = (Range) a;
	    Range bh = (Range) b;
	    return RangeHash.compare(ah, bh);
	}
    }

    public static void main(String [] args) {
	RangeHash rh = new RangeHash();
	System.err.println("apple = "+rh.put(12,20,"Apple"));
	System.err.println("carrot = "+rh.put(41,50,"Carrot"));
	System.err.println("doughnut = "+rh.put(59,70,"Doughnut"));
	System.err.println("egg = "+rh.put(78,79,"Egg"));
	System.err.println("banana = "+rh.put(23,27,"Banana"));
	//	System.err.println("apricot = "+rh.put(14,50,"Apricot"));

	System.err.println("fish = "+rh.put(121,160,"Fish"));

	System.err.println("REMOVE OF (14,51) = "+rh.getInterval(41)[0]+","+
			   rh.getInterval(41)[1]);
	System.err.println();

	Enumeration e = rh.values();
	while(e.hasMoreElements())
	    System.err.println(e.nextElement());
/*
	System.err.println("duds:");
	System.err.println("9 = "+rh.get(9));
	System.err.println("21 = "+rh.get(21));
	System.err.println();
	System.err.println("good ones:");
	System.err.println("20 = "+rh.get(20));
	System.err.println("12 = "+rh.get(12));
	System.err.println("41 = "+rh.get(41));
	System.err.println("59 = "+rh.get(59));
	System.err.println("78 = "+rh.get(78));
	System.err.println("23 = "+rh.get(23));
	System.err.println("121 = "+rh.get(121));
	System.err.println();
	System.err.println("63 = "+rh.get(63));
	System.err.println("50 = "+rh.get(50));
	System.err.println("129 = "+rh.get(129));
	System.err.println("26 = "+rh.get(26));
*/

    }

    private class ValueEnumeration implements Enumeration {
	int pos = 0;

	public Object nextElement() {
	    return data.get(ranges.elementAt(pos++));
	}

	public boolean hasMoreElements() {
	    return pos < ranges.size();
	}
    }

    public RangeHash() {
	data = new Hashtable();
	ranges = new Vector();
    }

    public Vector get(int low, int high) {
	Vector output = new Vector();
	RangeHolder holder = new RangeHolder(low,high);

	int lowPos = VectorUtil.binarySearch(ranges,
				  new RangeHolder(low,low),
				  rangeComparator,
				  true);
	if (lowPos == -1)
	    lowPos = 0;

	int highPos = VectorUtil.binarySearch(ranges,
					      new RangeHolder(high, high),
					      rangeComparator,
					      true);

	for(int i=(lowPos - 1 >= 0 ? lowPos-1 : 0);
	    i < highPos && i < ranges.size();
	    i++) {
	    Range getMe = (Range) ranges.elementAt(i);
	    if (overlaps(getMe, holder)) {
		output.addElement(data.get(getMe));
	    }
	}
	
	return output;
    }

    public int getIntervalIndex(int low, int high) {
	return getIntervalIndex(low, high, false);
    }

    // TODO: wtf?
    public int getIntervalIndex(int low, int high, boolean exact) {
	int lowPos = VectorUtil.binarySearch(ranges,
					     new RangeHolder(low,high),
					     rangeComparator,
					     !exact);
	return lowPos;
    }

    public Object getItemAtIndex(int index) {
	return ranges.elementAt(index);
    }
    
    public int [] getDefinedInterval() {
	if (ranges.size() < 1)
	   return null;
	int [] output = new int[2];
	int low = 0;
	int high = ranges.size() - 1;
	output[0] = ((Range) ranges.elementAt(low)).getLow();
	output[1] = ((Range) ranges.elementAt(low)).getHigh();
	return output;
    }

    public int [] getDefinedInterval(int key) {
	RangeHolder holder = new RangeHolder(key, key);
	int pos = VectorUtil.binarySearch(ranges,
					  holder,
					  rangeComparator,
					  true)-1;
	if (pos < 0 || pos >= ranges.size())
	    return null;
	Range r = (Range) ranges.elementAt(pos);
	if (r.getLow() <= key && r.getHigh() >= key) {
	    int [] out = new int[2];
	    out[0] = r.getLow();
	    out[1] = r.getHigh();
	    return out;
	} else
	    return null;
    }

    public double [] getInterval(int key) {
	double [] output = new double[2];
	RangeHolder holder = new RangeHolder(key, key+1);
	int pos = VectorUtil.binarySearch(ranges,
					  holder,
					  rangeComparator,
					  true) - 1;
	//System.err.println("ranges = "+ranges);
	if (ranges.size() == 0) {
	    output[0] = Double.NEGATIVE_INFINITY;
	    output[1] = Double.POSITIVE_INFINITY;
	    return output;
	}
	
	if (pos < 0) {
	    output[0] = Double.NEGATIVE_INFINITY;
	    output[1] = (double) ((Range) ranges.elementAt(0)).
	                 getLow()-1;
	    return output;
	}

	if (pos >= ranges.size()) {
	    output[0] = (double) ((Range) ranges.elementAt(ranges.size() - 1)).
	                 getHigh()+1;
	    output[1] = Double.POSITIVE_INFINITY;
	    return output;
	}

	Range rh = (Range) ranges.elementAt(pos);
	if (contains(rh, key)) {
	    output[0] = (double) rh.getLow();
	    output[1] = (double) rh.getHigh();
	} else {
	    output[0] = (double) rh.getHigh()+1;
	    if (pos < ranges.size()-1)
	        output[1] = ((Range) ranges.elementAt(pos+1)).getLow() - 1;
	    else
	        output[1] = Double.POSITIVE_INFINITY;
	}
	
	return output;
    }

    public Vector put(Range holder, Object value) {
      int pos = VectorUtil.binarySearch(ranges,
                                        holder,
                                        rangeComparator,
                                        true);
      if (pos == -1) {
        pos = 0;
      }
      Vector output = new Vector();
      
      int posHigh = VectorUtil.binarySearch(
          ranges,
          new RangeHolder(holder.getHigh(),
          holder.getHigh()),
          rangeComparator,
          true);
      
      Vector purged = new Vector();
      boolean positionRemoved = false;
      for(int i=(pos - 1 >= 0 ? pos-1 : 0);
          i < posHigh && i < ranges.size();
          i++) {
        Range clobberMe = (Range) ranges.elementAt(i);
        if (overlaps(clobberMe, holder)) {
          if (i == pos - 1)
            positionRemoved = true;
          output.addElement(data.remove(clobberMe));
          purged.addElement(clobberMe);
        }
      }
      for(int i=0; i < purged.size(); i++)
        ranges.removeElement(purged.elementAt(i));
      
      if (positionRemoved)
        pos = pos - 1;
      
      ranges.insertElementAt(holder, pos);
      data.put(holder, value);
      return output;
    }
    
    public Vector put(int low, int high, Object value) {
	return put(new RangeHolder(low, high), value);
    }

   /**
     * Removes and returns the value whose key range contains the given key.
     * If there is no such value, null is returned.
     */
    public Object remove(int key) {
	RangeHolder holder = new RangeHolder(key, key+1);
	int pos = VectorUtil.binarySearch(ranges,
					  holder,
					  rangeComparator,
					  true) - 1;
	if (pos < 0)
	    return null;
	Range rh = (Range) ranges.elementAt(pos);
	return data.remove(rh);
    }

    /**
     * Removes the EXACT range defined by (low,high). If no range is defined
     * on exactly those coordinates, no elements are removed
     */
    public Object remove(int low, int high) {
	RangeHolder holder = new RangeHolder(low, high);
	int pos = VectorUtil.binarySearch(ranges,
					  holder,
					  rangeComparator,
					  false);
	Range found = (Range) ranges.elementAt(pos);
	if (found.equals(holder))
	    return data.remove(holder);
	else
	    return null;
    }

    /**
     * Removes every element within the range (low,high)
     */
    public Vector removeRange(int low, int high) {
	Vector dead = get(low,high);
	for(int i=0; i < dead.size(); i++)
	    ranges.removeElement(dead.elementAt(i));
	return dead;
    }

    /**
     * Removes every element
     */
    public void removeAll() {
	ranges.removeAllElements();
	data.clear();
    }

   /**
     * Returns the value whose key range contains the given key, or null
     * if no range contains that key.
     */
    public Object get(int key) {
	RangeHolder holder = new RangeHolder(key, key+1);
	int pos = VectorUtil.binarySearch(ranges,
					  holder,
					  rangeComparator,
					  true) - 1;
	if (pos < 0)
	    return null;
	Range rh = (Range) ranges.elementAt(pos);
	if (contains(rh, key))
	    return data.get(rh);
	else
	    return null;
    }

    public Enumeration values() {
	return new ValueEnumeration();
    }

    public Enumeration ranges() {
	return ranges.elements();
    }

    public int size() {
	return ranges.size();
    }

    public String toString() {
	StringBuffer out = new StringBuffer("(");
	for(int i=0; i < ranges.size(); i++) {
	    Range r = (Range) ranges.elementAt(i);
	    Object o = data.get(r);
	    if (i > 0)
		out.append(", ");
	    out.append("{["+r.getLow()+", "+r.getHigh()+"] = "+
		       o.toString()+"}");
	}
	out.append(")");
	return out.toString();
    }
}
