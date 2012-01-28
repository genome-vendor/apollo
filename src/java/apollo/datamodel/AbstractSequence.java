package apollo.datamodel;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.io.IOException;
import java.lang.String;
import java.util.Date;

import org.bdgp.util.DNAUtils;

public abstract class AbstractSequence implements SequenceI {
  String name;
  String accession;
  String description;
  Vector dbxrefs = new Vector();
  String residue_type = null; //SequenceI.DNA;
  String checksum;
  // changed to protected so subclasses could reach them --suz
  protected int length = 0;
  protected RangeI    genomicRange = null;
  /** allow for some adjustments if the genomic sequence has 
      sequencing errors */
  protected HashMap genomic_errors;
  private Date update_date;
  private String organism;
//  Vector synonyms = null;

  public AbstractSequence(String id) {
    setName(id);
    if (id == null || id.equals (""))
      setName(RangeI.NO_NAME);
  }

  public int       getLength() {
    return length;
  }

  public void setLength (int length) {
    this.length = length;
  }

  public void setDescription (String desc) {
    if (desc != null && ! desc.equals (""))
      this.description = desc;
  }

  public String getDescription () {
    return description;
  }

  public void setChecksum (String checksum) {
    // Why can't we set it to ""?
    if (checksum != null) //  && ! checksum.equals (""))
      this.checksum = checksum;
  }

  public String getChecksum () {
    return checksum;
  }

  public void addDbXref (String db, String acc) {
    addDbXref(db, acc, 1);
  }

  public void addDbXref (String db, String acc, int isCurrent) {
    if (db != null && ! db.equals ("") &&
        acc != null && ! acc.equals ("") ) {
      DbXref dbxref = new DbXref ("id", acc, db);
      dbxref.setDbName (db);
      dbxref.setIdValue (acc);
      dbxref.setCurrent(isCurrent);
      dbxrefs.addElement(dbxref);
    }
  }

  public void addDbXref(DbXref xref) {
    dbxrefs.addElement(xref);
  }

  public Vector getDbXrefs () {
    return dbxrefs;
  }

  // CHECK
  public char getBaseAt(int loc) {
    return getResidues(loc,loc).charAt(0);
  }


  public abstract SequenceI getSubSequence(int start, int end);
  // Will be something like
  //    return new Sequence(getDisplayId(), getResidues(start,end));

  public String    getName  () {
    return name;
  }

  public void      setName  (String name) {
    if (name != null && ! name.equals (""))
      this.name = name;
  }

  /** Return true if name is NO_NAME. this is true if sequence constructed with
      null or empty string. */
  public boolean hasName() {
    return name != RangeI.NO_NAME;
  }

  /**
     I (SUZ) am removing the call to setLength, because the length
     of the viewed range should not logically change the length
     of the sequence. This may have a negative effect if there
     is any code around that was assuming the length of the
     sequence would be set this way. When this happens the code
     should be changed to independently call seq.setLength()
     This was having very bad effects when the sequence length
     was already being set. Then when setRefSequence was called
     the length was being silently changed to 1 (because the
     range was not yet set) Then the range could no longer be
     set because the sequence length was 0. Ugh.
  */
  public void setRange(RangeI loc) {
    this.genomicRange = loc;
    // Also this was stupidly re-inventing the length() method
    // it should have been setLength(loc.length());
    // setLength(loc.getEnd()-loc.getStart()+1);
  }

  public RangeI getRange() {
    return genomicRange;
  }

  /** Convenience for getRange null check */
  protected boolean hasRange() {
    return genomicRange != null;
  }

  public String    getAccessionNo() {
    return accession;
  }

  public void      setAccessionNo(String acc) {
    // why cant you set to null? took this out as was problematic undoing pep
    // names that were originally null
    //if (acc != null && ! acc.equals (""))
    this.accession = acc;
  }

  public void      setResidues  (String seqString) {
    // why not always implemented? why not residues = seqString?
    throw(new apollo.dataadapter.NotImplementedException("setResidues will not always be implemented"));
  }

  /** sets residues to empty string */
  public void clearResidues() {
    setResidues("");
    setLength(0);
  }

  /** This will return false if getResidues() is null or "", clearResidues()
      will set residues to "" causing this to be false. */
  public boolean hasResidues() {
    return getResidues() != null && !getResidues().equals("");
  }


  // CHECK
  public String getResidues () {
    if (getRange() ==  null) {
      return getResidues(1,getLength());
    } else {
      return getResidues((int)getRange().getStart(),
                         (int)getRange().getEnd());
    }
  }

  /**
   * returns substring of sequence residues,
   * all of this apollo stuff is counting from 1 -not- 0
   */
  public final String getResidues (int start, int end) {
    if (start <= end) {
      /* order here is important. pegLimits checking expects one or 
         range based  without the extended end for inclusiveness
         pegging does CHECK > or >= start = pegLimits (start, true); */
      start = pegLimits (start);
      end = pegLimits (end);

      if (needInclusiveEnd())
        end = makeEndInclusive(end);

      if (needShiftFromOneToZeroBasedCoords()) {
	start = shiftFromOneToZeroBased(start);
	end = shiftFromOneToZeroBased(end);
      }

      return getResiduesImpl(start, end);
    } else {
      return(DNAUtils.reverseComplement(getResidues(end, start)));
    }
  }

  /** Whether the end needs adding to, to make it inclusive. EnsCGISequence does not
      need an inclusive end and overrides this to return false. 
      I think inclusive end combined with need shift from zero to one based is 
      actually capturing interbase vs base-oriented - i put these in before i knew
      of interbase - refactor - set/getInterbase(boolean)? default true? */
  protected boolean needInclusiveEnd() {
    return true;
  }

  /** This just adds one to the end. This is because java String.substring 
      does not include the end, where in apollo we want the character at the
      end coordinate included in our string. */
  private int makeEndInclusive(int nonInclusiveEnd) {
    return nonInclusiveEnd + 1;
  }

  private boolean needShiftFromOneToZeroBasedCoords = true;
  /** Perhaps this is funny, but if we have a Range then shift wont happen 
      regardless of what you set here. This only applies if we have no range */
  protected void setNeedShiftFromOneToZeroBasedCoords(boolean needShift) {
    needShiftFromOneToZeroBasedCoords = needShift;
  }
  
  /** Returns true if getRange()==null && needShiftFromOneToZeroBasedCoords 
      is true (default is true). If AbsSeq has a range then the shift will be handled
      by the subclass dealing with the range(for now) (look into migrating range
      math to AbsSeq - it has been migrated i believe) */
  protected boolean needShiftFromOneToZeroBasedCoords() {  
    return getRange()==null && needShiftFromOneToZeroBasedCoords;
  }

  /** This just subtracts one from coord to shift from one based(apollo) 
      to zero based(java string) coord system. Made this an explicit method for
      clarity sake. */
  protected int shiftFromOneToZeroBased(int coord) {
    return coord - 1;
  }


  /** If getRange!=null, checks that value is in range, and if not then puts
      it in range. If no range then does check with 1 to length. If 0 based
      should override this (as jalview.datamodel.AlignSequence does)
      Get rid of begin/one->zero shift here - clever but opaque.
      This now expects coordinates to be one based or range based
      (not zero based)  (it used to do the actual one->zero shifting)
      Zero based seqs need to override this mthod
      (see jalview.datamodel.AlignSeq)
  */
  protected int pegLimits (int value/*, boolean begin*/) {
    /* There was a problem (that came out when I was trying to get 
       BlixemRunner to work) where some sequences inexplicably had ranges
       of -1 to -1/ when they should have had null ranges.
       I hope this check won't break anything! */
    if (getRange() == null || getRange().getStart() < 0) {
      if (value < 1)
	value = 1;
      if (value > getLength())
	value = getLength();
    } else {
      RangeI range = getRange();
      if (value < range.getStart())
        value = (int)range.getStart();
      if (value > range.getEnd())
        value = (int)range.getEnd();
    }
    return value;
  }

  protected abstract String getResiduesImpl(int start,int end);
  protected abstract String getResiduesImpl(int start);

  public String getReverseComplement() {
    return(DNAUtils.reverseComplement(getResidues()));
  }

  public boolean isSequenceAvailable(long position) {
    // This was returning true for regions with no sequence.
    // Don't we need to test that we actually have sequence??
    if (this instanceof apollo.datamodel.seq.GAMESequence) // --> if !seq.isLazy()
      if (!((apollo.datamodel.seq.GAMESequence)this).hasSequence()) {
        return false;
      }

    if (getRange() != null) {
      if (position >= getRange().getStart() &&
          position <= getRange().getEnd()) {
        return true;
      } else {
        return false;
      }
    } else {
      if (position >= 1 && position <= getLength()) {
        return true;
      } else {
        return false;
      }
    }
  }

  /** This returns frame as 0, 1, or 2 (not 1,2,3) */
  public int getFrame (long base, boolean forward) {
    int frame = -1;
    // the base that is passed in is a position
    // in reference to the genomic, not the
    // available residues (which is what the length is)
    long genomic_length = (getRange() == null ?
                           getLength() :
                           getRange().getEnd());
    long genomic_start = getRange() == null ? 1 : getRange().getStart();
    if (genomic_length > 0 && base <= genomic_length && base > 0) {
      // using base - 1 is a relic from when cur set sequences started at 1
      long offset = (forward ?
                     base - genomic_start : //base - 1 :
                     genomic_length - base);
      frame = (int) (offset % 3);
    }
    return frame;
  }

  /** Returns null if never set. Use calculateResFromFeatLength to come up with
      a res type based on feature length. */
  public String getResidueType () {
    return residue_type;
  }

  public boolean hasResidueType() {
    return residue_type!=null;
  }
  
  /** convenience */
  public boolean isAA() { return residue_type == SequenceI.AA; }

  public void setResidueType (String res_type) {
    if (res_type == SequenceI.DNA ||
        res_type == SequenceI.AA ||
        res_type == SequenceI.RNA)
      residue_type = res_type;
  }

  public boolean usesGenomicCoords() {
    return getRange() != null;
  }

  public boolean isLazy() { return false; }

  // Given the current control structure, it's hard to tell whether a sequence
  // is "recent" when it's first read in (because we don't know here which tier
  // that sequence is going to appear in, and the recent dates are associated
  // with tiers (actually types)).  So we'll check, if necessary, and then cache
  // the answer so we don't have to work so hard next time.
  public boolean isNewerThan(Date recentDate) {
    // See if we already did the comparison
    if (update_date == null || recentDate == null)
      return false;
    else
      return (recentDate.compareTo(update_date) <= 0);
  }

  public void setDate (Date update_date) {
    this.update_date = update_date;
  }

  public boolean isSequencingErrorPosition(int base_position) {
    if (genomic_errors != null)
      return genomic_errors.get("" + base_position) != null;
    else
      return false;
  }

  public SequenceEdit getSequencingErrorAtPosition(int base_position) {
    if (genomic_errors != null)
      return (SequenceEdit) genomic_errors.get("" + base_position);
    else
      return null;
  }

  public HashMap getGenomicErrors() {
    return genomic_errors;
  }

  /** 
      The position of the genomic sequencing error is in genomic 
      coordinates. This is NOT for describing mutations or altering
      the genomic sequence. It simply accounts for errors in
      low quality sequence so that genes can be translated */
  public boolean addSequencingErrorPosition(String operation, 
                                            int pos, 
                                            String residue) {
    return addSequenceEdit(operation, pos, residue, null);
  }

  public boolean addSequenceEdit(SequenceEdit seq_edit) {
    if (seq_edit != null)
      return addSequenceEdit(seq_edit.getEditType(), 
                             seq_edit.getPosition(), 
                             seq_edit.getResidue(),
                             seq_edit);
    else
      return false;
  }

  /** Returns true if edit actually went through - there are cases where the edit
      is irrelevant apparently
  if operation is SequenceI.CLEAR_EDIT, then removes seq edit 
  actually thas been moved to removeSequenceEdit */
  private boolean addSequenceEdit(String operation, 
                                  int pos, 
                                  String residue,
                                  SequenceEdit seq_edit) {
    /* since so much work is involved, avoid it for no change */
    /* make sure the position is even on this transcript */
    boolean edited = false;
    if (operation.equals(SequenceI.INSERTION) ||
        operation.equals(SequenceI.DELETION) ||
        operation.equals(SequenceI.SUBSTITUTION)) {
      if (genomic_errors == null)
        genomic_errors = new HashMap(1);
      String key = "" + pos;
      if (!genomic_errors.containsKey(key)) {
        if (seq_edit == null) {
          seq_edit = new SequenceEdit(operation, pos, residue);
          seq_edit.setRefSequence(this);
        }
        genomic_errors.put(key, seq_edit);
        edited = true;
      }
    }
    // --> moved to removeSequenceError
//     else if (operation.equals(SequenceI.CLEAR_EDIT)) {
//       String key = "" + pos;
//       if (genomic_errors != null && genomic_errors.containsKey(key)) {
//         genomic_errors.remove(key);
//         edited = true;
//       }
//     }
    return edited;
  }

  public boolean removeSequenceEdit(SequenceEdit seqEdit) {
    if (genomic_errors == null)
      return false; // shouldnt happen
    String key = seqEdit.getHashKey();
    if (!genomic_errors.containsKey(key))
      return false; // shouldnt happen

    genomic_errors.remove(key);
    return true;
  }

  public String getOrganism() {
    return this.organism;
  }

  public void setOrganism(String organism) {
    if (organism != null && !organism.equals(""))
      this.organism = organism;
  }
}
