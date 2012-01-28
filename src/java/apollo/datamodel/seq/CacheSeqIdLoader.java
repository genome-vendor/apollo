package apollo.datamodel.seq;

import java.util.Vector;
import java.io.IOException;
import java.lang.String;

import apollo.datamodel.RangeI;
import apollo.datamodel.Range;

import org.bdgp.util.*;

/** This presently will only work with parent as GAMESequence. 
    Perhaps rename CacheGameLoader? */

public class CacheSeqIdLoader extends CacheSequenceLoader {

  public CacheSeqIdLoader(GAMESequence parent) {
    super(parent);
  }

  // Zero based numbering for sequence. - thats wrong
  // low and high are in range coords (not zero based) and the end is
  // added on to make it inclusive
  public String getResidues(int low, int high) {
    RangeI range = ((GAMESequence) parent).getRange();

    if (low < range.getLow())
      return "";

    if (high > range.getHigh() + 1) // +1 -> inclusive end
      return "";

    String seqStr = (parent.getResiduesFromSource(low, high));

    return seqStr;
  }

}
