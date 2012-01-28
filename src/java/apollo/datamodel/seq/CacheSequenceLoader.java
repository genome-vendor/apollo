package apollo.datamodel.seq;

import java.util.Vector;
import java.io.IOException;
import java.lang.String;

import org.apache.log4j.*;

import org.bdgp.util.*;

public class CacheSequenceLoader {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(CacheSequenceLoader.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  LazySequenceI parent;
  int       minChunkSize = 50000;
  RangeHash sequenceRanges = new RangeHash();
  int       currentSize = 0;
  int       maxSize = 500000;

  public CacheSequenceLoader(LazySequenceI parent) {
    this.parent = parent;
  }

  public void setMinChunkSize(int size) {
    minChunkSize = size;
  }


  public void setMaxSize(int size) {
    maxSize = size;
  }
  // Zero based numbering for sequence.
  public String getResidues(int low, int high) {

    boolean gotCompleteMatch = false;

    //    try {
    //       throw new NullPointerException();
    //    } catch (Exception e) {
    //       logger.error("NOT A REAL ERROR", e);
    //    }

    if (currentSize > maxSize) {
      clearCache();
    }

    if (parent.getLength() == 0) {
      return null;
    }

    // Modify the range to get to be multiples of the chunk size

    logger.debug("Original range " + low + " - " + high);
    int chunkedLow  = (low / minChunkSize * minChunkSize);
    int chunkedHigh = ((high + minChunkSize) / minChunkSize * minChunkSize) - 1;

    //chunkedHigh = (chunkedHigh >= parent.getLength() ? parent.getLength() - 1 : chunkedHigh);
    //chunkedLow  = (chunkedLow < 0 ? 0 : chunkedLow);
    // This is problematic if ever used with an inclusive end
    chunkedHigh = (chunkedHigh > parent.getRange().getHigh() ? parent.getRange().getHigh() : chunkedHigh);
    chunkedLow = (chunkedLow < parent.getRange().getLow() ? parent.getRange().getLow() : chunkedLow);

    logger.debug("Getting chunked range " + chunkedLow + " - " + chunkedHigh);

    Vector existingRanges = sequenceRanges.get(chunkedLow,chunkedHigh);
    String seqStr = null;
    StringBuffer seqBuff = new StringBuffer(minChunkSize);

    if (existingRanges.size() > 0) {
      SequenceRange range = (SequenceRange)existingRanges.elementAt(0);

      // First is special case with single range which covers entire range.
      // This should not do any allocs which is important when the GC takes
      // so long.

      logger.debug("range: " + range.getLow() + "-" + range.getHigh() + " low " + chunkedLow + " high " + chunkedHigh);
      if (existingRanges.size() == 1 &&
          range.getLow() <= chunkedLow &&
          range.getHigh() > (chunkedHigh-1)) {

	logger.debug("Getting single sequence piece");
        chunkedLow = range.getLow();
        chunkedHigh = range.getHigh();
        seqStr = (range.getSeqString());
        gotCompleteMatch = true;

      } else {
	logger.debug("Constructing sequence");
        if (range.getLow() > chunkedLow) {
          seqBuff.append(parent.getResiduesFromSource(chunkedLow,range.getLow()-1));
          currentSize += range.getLow()-chunkedLow-1;
        } else if (range.getLow() <= chunkedLow) {
          chunkedLow = range.getLow();
        }
        seqBuff.append(range.getSeqString());

	logger.debug("First range gave: " + seqStr);

        for (int i=0; i<existingRanges.size()-1; i++) {
          range = (SequenceRange)existingRanges.elementAt(i);
          int start = range.getHigh()+1;
          SequenceRange range2 = (SequenceRange)existingRanges.elementAt(i+1);
          int end   = range2.getLow()-1;
	  logger.debug("Start = " + start + " end = " + end);
          if (start < end) {
            seqBuff.append(parent.getResiduesFromSource(start,end));
            currentSize += end-start+1;
          }
          seqBuff.append(range2.getSeqString());
	  logger.debug("After adding range: " + seqStr);
        }

	logger.debug("Last range = " + range.getLow() + "-" + range.getHigh());

        range = (SequenceRange)existingRanges.lastElement();
        if (range.getHigh() < chunkedHigh) {
          seqBuff.append(parent.getResiduesFromSource(range.getHigh()+1,chunkedHigh));
          currentSize += chunkedHigh-range.getHigh();
	  logger.debug("After adding final seq: " + seqStr);
        }
        else {
          chunkedHigh = range.getHigh();
        }
        seqStr = seqBuff.toString();
      }
    } else {
      logger.debug("getResFromSrc parent: "+parent+"\nseq "+parent.getResiduesFromSource(chunkedLow,chunkedHigh));
      // Whats the difference between calling getResidues and getResiduesFromSource?
      // getRes has called us - if getRes called here endless loop
      seqStr = (parent.getResiduesFromSource(chunkedLow,chunkedHigh));
    }

    if (seqStr == null)
      return null;

    if (!(existingRanges.size() == 1 && gotCompleteMatch)) {
      SequenceRange newRange = new SequenceRange(seqStr,chunkedLow,chunkedHigh);
      sequenceRanges.put(newRange,newRange);
    }

    logger.debug("Return range " + (low-chunkedLow) + " - " + (high-chunkedLow));
    return seqStr.substring(low-chunkedLow,high-chunkedLow+1);
  }

  public void clearCache() {
    logger.debug("Clearing cache");
    sequenceRanges = new RangeHash();
    currentSize = 0;
  }

  protected class SequenceRange implements org.bdgp.util.Range {
    String seqString;
    int    low;
    int    high;


    public SequenceRange(String seqString, int low, int high) {
      this.seqString = seqString;
      this.low = low;
      this.high = high;
      if (seqString.length() != (high-low+1)) {
        logger.error("ERROR: seqString length inconsistent with range for: " +
                     " seqString = " + seqString +
                     " low = " + low + " high = " + high +
                     " seqString length = " + seqString.length() + " high-low+1 = " + (high-low+1));
      }
    }

    public int getLow() {
      return low;
    }

    public int getHigh() {
      return high;
    }

    public int getLength() {
      return seqString.length();
    }

    public String getSeqString() {
      return seqString;
    }

    public String toString() {
      return "("+getLow()+","+getHigh()+") = " + seqString;
    }
  }
}
