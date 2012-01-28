package apollo.datamodel;

import java.util.*;

public interface RangeI extends java.io.Serializable, Cloneable {
  /**
   * The type String to use for features that have no other name.
   */
  public static final String NO_NAME="no_name";

  /** In the case where the range is chromosomal the name is the chromosome name */
  public String      getName();
  public void        setName(String name);
  /** return true if name !equal NO_NAME */
  public boolean hasName();

  /**
   * The type String to use for features that have no other type.
   */
  public static final String NO_TYPE="no_type";

  /** getType is not the "visual" type, ie the type one sees in the EvidencePanel.
      getType returns the "logical" type(the type from the data). These are the 
      types in the squiggly brackets in the tiers file that map to the visual type
      listed before the squigglies. gui.scheme.FeatureProperty maps logical types
      to visual types (convenience function in DetailInfo.getPropertyType) */
  public String      getFeatureType();
  public void        setFeatureType(String type);
  /** convenience method returns !getType()==NO_TYPE */
  public boolean     hasFeatureType();

  public int        getStart();
  public void        setStart(int start);

  public int        getEnd();
  public void        setEnd(int end);

  public int        getLow();
  public void        setLow(int low);

  public int        getHigh();
  public void       setHigh(int high);

  public boolean     isSequenceAvailable (int position);

  /**
   * Retrieve the SequenceI that this feature annotates.
   *
   * @return the current parent SequenceI
   */
  public SequenceI   getRefSequence();
  
  /** return false if no ref seq, ranges should generally have ref seq, only during
   * initialization might it not
   */
  public boolean hasRefSequence();

  /**
   * Set the SequenceI that this feature annotates.
   *
   * @param refSeq the new parent SequenceI
   */
  public void        setRefSequence(SequenceI refSeq);

  /** Return true if ref seq contains this range */
  public boolean isContainedByRefSeq();

  public String      getResidues();

  /** @return 1 for forward strand, -1 for reverse strand, 0 for strandless */
  public int         getStrand();
  public void        setStrand(int strand);

  // a convenience to avoid testing for == 1 all of the time
  public boolean    isForwardStrand();

  public int        length();

  public int        getLeftOverlap (RangeI sf);
  public int        getRightOverlap(RangeI sf);

  public boolean     isExactOverlap (RangeI sf);
  /** Returns true if range of sf is entierly within this RangeI */
  public boolean     contains       (RangeI sf);
  public boolean     contains       (int position);
  public boolean     overlaps       (RangeI sf);
  /** Return true if start and end are equal */
  public boolean sameRange(RangeI r);

  /** If RangeI is an instanceof FeatureSetI and
      FeatureSetI.hasChildFeatures is true then return true.
      Basically convenience method that does the awkward instanceof
      for you. */
  public boolean canHaveChildren();

  /** Returns clone of self */
  public RangeI getRangeClone();

  /** Returns true if same start,end,type and name. This could potentially
      be changed to equals, theres implications there for hashing. A range
      and its clone will be identical barring modifications. */
  public boolean isIdentical(RangeI range);

  /** Return true if range has been assigned high & low */
  public boolean rangeIsUnassigned();

  /** Converts base oriented range to interbase range */
  public void convertFromBaseOrientedToInterbase();
  /** Converts interbase range to base oriented range */
  public void convertFromInterbaseToBaseOriented();
}
