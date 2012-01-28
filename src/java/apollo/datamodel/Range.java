package apollo.datamodel;

import apollo.util.SeqFeatureUtil;

import org.apache.log4j.*;

public class Range implements RangeI, java.io.Serializable {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(Range.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  protected int    low = -1;
  protected int    high = -1;
  protected byte   strand = 0;
  protected String name = RangeI.NO_NAME;
  protected String type = RangeI.NO_TYPE;

  protected SequenceI   refSeq;

  public Range () {}

  /** Range with NO_NAME name */
  public Range(int start, int end) {
    this(NO_NAME,start,end);
  }

  public Range (String name, int start, int end) {
    setName (name);
    setStrand (start <= end ? 1 : -1);
    setStart(start);
    setEnd(end);
  }

  /** returns clone of self */
  public RangeI getRangeClone() {
    try { return (RangeI)clone(); } 
    catch (CloneNotSupportedException e) { 
      logger.error("Range.getRangeClone(): clone failed for " + getName(), e); 
    }
    return null; // shouldnt happen
  }

  /** Returns true if same start,end,type and name. This could potentially
      be changed to equals, theres implications there for hashing. A range
      and its clone will be identical barring modifications. */
  public boolean isIdentical(RangeI range) {
    if (this == range)
      return true;
    // Features have to have same type,range, AND name
    return (range.getFeatureType().equals(getFeatureType()) && sameRange(range) &&
            range.getName().equals(getName()));
  }

  public void setName(String name) {
    if (name == null) {
      throw new NullPointerException("Range.setName: can't accept feature name of null. " +
                                     "Use RangeI.NO_NAME instead.");
    } else if (!name.equals(""))
      this.name = name;
  }

  public String getName() {
    return name;
  }

  public boolean hasName() {
    return !name.equals("") && !name.equals(NO_NAME);
  }

  /** getType is not the "visual" type, 
      ie the type one sees in the EvidencePanel.
      getType returns the "logical" type(the type from the data). 
      These are the types in the squiggly brackets in the tiers 
      file that map to the visual type listed before the squigglies. 
      gui.scheme.FeatureProperty maps logical types
      to visual types (convenience function in DetailInfo.getPropertyType) */
  public String getFeatureType() {
    return this.type;
  }

  public void setFeatureType(String type) {
    if(type == null) {
      throw new NullPointerException("Range.setFeatureType: can't accept feature type of null. " +
                                     "Use SeqFeatureI.NO_TYPE or 'SeqFeatureI.NO_TYPE' instead.");
    } else if (!type.equals(""))
      this.type = type;
  }

  public boolean hasFeatureType() {
    return ! (getFeatureType() == SeqFeatureI.NO_TYPE);
  }

  /** @return 1 for forward strand, -1 for reverse strand, 0 for strandless */
  public int getStrand() {
    return (int)this.strand;
  }

  /** Convenience method for getStrand() == 1 */
  public boolean isForwardStrand() {
    return getStrand() == 1;
  }

  public void setStrand(int strand) {
    this.strand = (byte)strand;
  }

  public void setStart(int start) {
    // check if strand is proper given start value?
    if (getStrand() == -1) {
      high = start;
    } else {
      low = start;
    }
  }

  public int getStart() {
    return (getStrand() == -1 ? high : low);
  }

  public void setEnd(int end) {
    if (getStrand() == -1) {
      low = end;
    } else {
      high = end;
    }
  }

  public int getEnd() {
    return (getStrand() == -1 ? low : high);
  }

  public int getLow() {
    return this.low;
  }

  public void setLow(int low) {
    // check if low < high - if not switch, and switch strand?
    this.low = low;
  }

  public int getHigh() {
    return this.high;
  }

  public void setHigh(int high) {
    this.high = high;
  }

  public String getStartAsString() {
    return String.valueOf(new Integer(getStart()));
  }

  public String getEndAsString() {
    return String.valueOf(new Integer(getEnd()));
  }

  public SequenceI getRefSequence() {
    return refSeq;
  }

  public boolean hasRefSequence() {
    return refSeq != null;
  }

  public boolean isContainedByRefSeq() {
    // Cant use contains as sometimes strand is not the same
    // Reverse strand feats can have ref seq for entry thats forward stranded
    //return getRefSequence().getRange().contains(this);
    if (getRefSequence() == null || getRefSequence().getRange() == null) {
      // safer than returning false, and better than throwing a 
      // NullPointerException - i guess the rationale is if we dont know the range
      // then just assume its in range? - of course the follow up question is 
      // why dont we have a ref seq or range?
      if (logger.isDebugEnabled()) {
        String m="Range.isContainedByRefSeq() feature has null ";
        m += getRefSequence()==null ? "reference seq" : "reference seq range";
        m += ".  Returning true even though we dont really know.";
        logger.error(m, new Throwable());
      }
      return true;
    }

    RangeI refSeqRange = getRefSequence().getRange();
    return (getLow() >= refSeqRange.getLow() && 
	    getHigh() <= refSeqRange.getHigh());
  }

  public void setRefSequence(SequenceI sequence) {
    this.refSeq = sequence;
    // SeqFeatureUtil.setSeqResidueType(sequence,this);
  }

  public String getResidues () {
    StringBuffer dna = new StringBuffer();

    if (refSeq != null) {
      String residues = refSeq.getResidues (getStart(), getEnd());
      if (residues != null)
        dna.append (residues);
    }
    return dna.toString();
  }
  
  // These are all overlap methods
  public int getLeftOverlap(RangeI sf) {
    return (getLow() - sf.getLow());
  }

  public int getRightOverlap(RangeI sf) {
    return (sf.getHigh() - getHigh());
  }

  public boolean     isExactOverlap (RangeI sf) {
    if (getLeftOverlap(sf)  == 0  &&
        getRightOverlap(sf) == 0 &&
        getStrand()         == sf.getStrand()) {
      return true;
    } else {
      return false;
    }
  }

  public boolean     contains(RangeI sf) {
    if (overlaps(sf)             &&
        getLeftOverlap(sf)  <= 0 &&
        getRightOverlap(sf) <= 0 &&
        getStrand()       == sf.getStrand()) {
      return true;
    } else {
      return false;
    }
  }

  public boolean     contains(int position) {
    return (position >= getLow() && position <= getHigh());
  }

  public boolean     overlaps(RangeI sf) {
    return (getLow()    <= sf.getHigh() &&
            getHigh()   >= sf.getLow()  &&
            getStrand() == sf.getStrand());
  }

  /** Return true if start and end are equal */
  public boolean sameRange(RangeI r) {
    return getStart() == r.getStart() && getEnd() == r.getEnd();
  }

  public int length() {
    return (getHigh() - getLow() + 1);
  }

  public boolean isSequenceAvailable(int position) {
    // if refSeq doesnt have a containing feature then it expects a position
    // that is 1 to length, if getLow > 1 then position is chromosomal
    // and needs correcting: subtract getLow - is this the right thing
    // to do or is it erroneous that the sequence does not have a
    // containing feature
    return (refSeq != null && refSeq.isSequenceAvailable(position));
  }

  /** If SeqFeature is an instanceof FeatureSetI and 
      FeatureSetI.hasChildFeatures is true then true.
      Basically convenience method that does the awkward instanceof for you. */
  public boolean canHaveChildren() {
    return false;
  }

  /** Return true if range has not been assigned high & low */
  public boolean rangeIsUnassigned() {
    return low == -1 && high == -1;
  }

  public void convertFromBaseOrientedToInterbase() {
    --low;
  }
  public void convertFromInterbaseToBaseOriented() {
    ++low;
  }

  public String toString() {
    return "Range[name=" + name + ",type=" + type + ",low=" + low + ",high=" + high + ",strand=" + strand + "]";
  }
}
