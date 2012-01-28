package org.bdgp.util;

public class RangeHolder implements Range {

    private int low;
    private int high;
    private int hashCache;

    public RangeHolder(int low, int high) {
        this.low = low;
        this.high = high;
        this.hashCache = new Integer(low).hashCode();
    }

    public int getLow() {
        return low;
    }

    public int getHigh() {
        return high;
    }

    public boolean equals(Object o) {
        boolean result = false;
        
        if (o instanceof RangeHolder) {
            RangeHolder range = (RangeHolder) o;
            result = this.getLow() == range.getLow()
                  && this.getHigh() == range.getHigh();
        }
        return result;
    }

    public int hashCode() {
        return hashCache;
    }

    public String toString() {
        return "("+low+","+high+")";
    }
}