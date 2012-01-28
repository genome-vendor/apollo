package apollo.datamodel;

import org.apache.log4j.*;
import org.bdgp.util.DNAUtils;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Enumeration;

import apollo.util.FeatureList;
import apollo.util.SeqFeatureUtil;
import apollo.util.QuickSort;

/**
 * A class to represent a basic sequence feature, such as a result span. This
 * is also the base class for the {@link FeatureSet} and {@link FeaturePair}
 * classes:
 *
 * FeatureSet should NOT subclass SeqFeature.
 * SeqFeature should have methods hasChildren()
 *
 */
public class SeqFeature extends Range
  implements SeqFeatureI, Comparable {
  
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(SeqFeature.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  protected String      id;
  protected String      refId;
  private   Identifier  identifier = null;

  protected SeqFeatureI refFeature;
  protected Hashtable   ref_features;

  private SeqFeatureI analogousOppositeStrandFeature=null;

  protected String      biotype = null;

  // SMJS Note these will be made less efficient by the 1.0
  //      but importantly less memory hungry.
  protected Hashtable scores = null;

  // Actually keep the default score outside the hash so we don't have to do
  // a hash table lookup every time we want it!!!!
  protected double score;

  protected Hashtable properties = null;

  protected byte phase = 0;

  /** alignment either comes from explicitAlignment or a cigar. null if there is no
      alignment. alignments are only for leaf features. alignment has peptide padding
      if a peptide alignment. */
  private String alignment;
  /** alignment with no padding */
  private String unpaddedAlignment;
  private String explicitAlignment;

    /** When translating the offset for the stop codon in genome
      coordinates needs to be adjusted to account for edits to
      the mRNA that alter the relative position of the Stop codon
      on the mRNA vs. the genome (e.g. from translational frame
      shift, or genomic sequencing errors */
    protected int edit_offset_adjust;

    //ADDED by TAIR User object to be used by any data adapter that needs it
  private Object userObject = null;

  private String syntenyLinkInfo = null;

  private SeqFeatureI cloneSource = null;
  
  public SeqFeature() {
  }

  public SeqFeature(SeqFeatureI sf) {
    initWithSeqFeat(sf);
  }
  
  protected void initWithSeqFeat(SeqFeatureI sf) {
    init(sf.getLow(),sf.getHigh(),sf.getFeatureType(),sf.getStrand());
    setName(sf.getName());
    setId(sf.getId());
    if (sf.getRefSequence() != null)
      setRefSequence (sf.getRefSequence());
  }

  public SeqFeature(int low, int high, String type) {
    init(low,high,type);
  }

  public SeqFeature(int low, int high, String type, int strand) {
    init(low,high,type,strand);
  }

  private void init(int low, int high, String type) {
    setLow  (low);
    setHigh (high);
    setFeatureType (type);
  }

  private void init(int low, int high, String type, int strand) {
    init(low,high,type);
    setStrand(strand);
  }

  /** Creates a Sequence object with Range.getResidues. getResidues returns
      the substring of the refSequence from getStart to getEnd */
  public SequenceI getFeatureSequence() {
    Sequence seq = new Sequence (getName(), getResidues());
    return seq;
  }

  /** Flips strand - FeatureSet overrides with a recursive version */
  public void flipFlop () {
    setStrand (getStrand() * -1);
  }

  /**
     This needs to be fixed to account for edits to the genomic
     sequence, but I don't think it is urgent because this case
     is still so extremely rare. If there are edits then the
     simple getResidues call is insufficient and needs post-processing
  */
  public String get_cDNA() {
    if (getGenomicErrors() != null) // ?
      logger.warn("SeqFeature.get_cDNA: not accounting for genomic errors in " + getName() + "!");
    return getResidues();
  }

  protected StringBuffer amend_RNA(StringBuffer dna) {
    SequenceEdit [] edit_list = buildmRNAEditList();
    int edits = (edit_list != null ? edit_list.length : 0);
    if (edits > 0) {
      String mRNA = dna.toString();
      dna.setLength(0);
      int prior_offset = 0;
      for (int i = 0; i < edits; i++) {
        SequenceEdit edit = edit_list[i];
        String edit_type = edit.getEditType();
        int edit_offset = getFeaturePosition(edit.getPosition()) - 1;
        if (edit_type.equals(SequenceI.DELETION)) {
          dna.append(getSubSequence(mRNA, prior_offset, edit_offset));
          edit_offset_adjust++;
          prior_offset = edit_offset + 1;
        }
        else if (edit_type.equals(SequenceI.INSERTION)) {
          dna.append(getSubSequence(mRNA, prior_offset, edit_offset) +
                     edit.getResidue());
          edit_offset_adjust--;
          prior_offset = edit_offset;
        }
        else if (edit_type.equals(SequenceI.SUBSTITUTION)) {
          dna.append(getSubSequence(mRNA, prior_offset, edit_offset) +
                     edit.getResidue());
          prior_offset = edit_offset + 1;
        }
      }
      dna.append(getSubSequence(mRNA, prior_offset, -1));
    }
    return dna;
  }

  /** This has nothing to do with translation starts, it merely
   * obtains the string between the 2 positions. If the end is
   * < 0 then it takes everything to the end
   */
  protected String getSubSequence (String sequence,
				   int start_offset,
				   int end_offset) {
    String sub_sequence = "";

    if (start_offset >= 0 && start_offset < sequence.length() &&
        start_offset != end_offset) {
      if (end_offset > start_offset &&
          end_offset < sequence.length()) {
        sub_sequence = sequence.substring(start_offset, end_offset);
      } else {
        sub_sequence = sequence.substring(start_offset);
      }
    }
    return sub_sequence;
  }

  protected SequenceEdit [] buildmRNAEditList() {
    HashMap adjustments = getGenomicErrors();
    int edits = ((adjustments == null ? 0 : 0 + adjustments.size()));
    if (edits > 0) {
      int [] int_list = new int [edits];
      SequenceEdit [] edit_list = new SequenceEdit [edits];
      int count = 0;
      if (adjustments != null) {
        Iterator positions = adjustments.keySet().iterator();
        while (positions.hasNext()) {
          String position = (String) positions.next();
          int_list[count] = Integer.parseInt(position);
          edit_list[count] = (SequenceEdit) adjustments.get(position);
          count++;
        }
      }
      QuickSort.sort(int_list, edit_list);
      if(getStrand() == -1)
      	QuickSort.reverse(edit_list);
      return edit_list;
    } else {
      return null;
    }
  }

  /** first gets all of the genomic sequencing errors from the
      reference sequence and then eliminates any that are from
      outside the region of this feature. only returns those
      that affect this feature */
  public HashMap getGenomicErrors() {
    HashMap genomic_errors = null;
    if (getRefSequence() != null) {
      HashMap all_errors = getRefSequence().getGenomicErrors();
      if (all_errors != null) {
        Iterator positions = all_errors.keySet().iterator();
        while (positions.hasNext()) {
          String position = (String) positions.next();
          int pos = Integer.parseInt(position);
          if (getFeatureContaining(pos) != null) {
            if (genomic_errors == null)
              genomic_errors = new HashMap(1);
            genomic_errors.put(position, all_errors.get(position));
          }
        }
      }
    }
    return genomic_errors;
  }

 
  /** Calls get_ORF with the cdna.
   */
  public String getCodingDNA() {
    String cdna = get_cDNA();
    // i think this gives the coding cdna.
    String codingSeq = get_ORF(cdna);
    // if no translation start is set (eg restults) get_ORF returns ""
    //if (codingSeq.equals("")) return cdna;
    //else
    return codingSeq;
  }

  public SeqFeatureI getRefFeature() {
    return this.refFeature;
  }

  /** Returns refFeature as FeatureSetI which I believe it is always the case
      that the refFeature is a FeatureSetI */
  public FeatureSetI getParent() {
    if (getRefFeature() instanceof FeatureSetI) 
      return (FeatureSetI)getRefFeature();
    else
      return null;
  }

  public SeqFeatureI getRefFeature(String type) {
    if (ref_features == null ||
        ref_features.get (type) == null) {
      return null;
    } else {
      return ((SeqFeatureI) ref_features.get (type));
    }
  }

  public void setRefFeature(SeqFeatureI refFeature) {
    addRefFeature ("primary", refFeature);
    this.refFeature = refFeature;
  }

  public void addRefFeature(String type, SeqFeatureI refFeature) {
    if (ref_features == null)
      ref_features = new Hashtable(1, 1.0F);
    if (refFeature != null)
      ref_features.put(type, refFeature);
    else
      ref_features.remove(type);
  }

  public String getRefId() {
    return (this.getRefFeature() != null ?
            this.getRefFeature().getId() : null);
  }
  
  /** If biotype is null, returns type */
  public String getTopLevelType() {
    String retType;
    if (biotype == null ||
        biotype.equals ("") || 
        biotype.equals(RangeI.NO_TYPE)) {
      retType = getFeatureType();
    } else {
      retType = biotype;
    }
    return retType;
  }

  public void setTopLevelType (String type) {
    this.biotype = type;
  }

  /** to get a field-by-field replica of this feature */
  public Object clone() {
    try {
      SeqFeature clone = (SeqFeature)super.clone();
      clone.clearProperties();
      Enumeration e = getProperties().keys();
      while (e.hasMoreElements()) {
        String type = (String) e.nextElement();
        Vector values = getPropertyMulti(type);
        for (int i = 0; values != null && i < values.size(); i++) {
          String value = (String) values.elementAt(i);
          clone.addProperty (type, value);
        }
      }
      clone.cloneSource = this;
      return clone;
    } catch (CloneNotSupportedException e) {
      logger.error("SeqFeature.clone: cloning failed for " + getName(), e);
      return null;
    }
  }
  public SeqFeatureI cloneFeature() {
    return (SeqFeatureI)clone();
  }
  
  public boolean isClone()
  {
    return cloneSource != null;
  }
  
  public SeqFeatureI getCloneSource()
  {
    return cloneSource;
  }
  
  // SMJS I've made these properties so I can save the memory when they're
  //      not used (like in the EnsCGIAdapter, the CORBAAdapter, the
  //      GFFAdapter).
  public String getProgramName() {
    return getProperty("program");
  }

  public void setProgramName(String name) {
    if (name != null && !name.equals(""))
      addProperty("program",name);
  }

  public String getDatabase() {
    return getProperty("database");
  }

  public void setDatabase(String name) {
    if (name != null && !name.equals(""))
      addProperty("database",name);
  }

  public void setId(String id) {
    this.id = id;
  }
  public String getId() {
    return this.id;
  }

  public boolean hasId() { return getId() != null; }

  public double getScore() {
    // This is a very slow way to get the score.
    //    return getScore ("score");
    return score;
  }

  public double getScore (String name) {
    if (scores == null)
      scores = new Hashtable(1, 1.0F);
    Score score = (Score) scores.get (name);
    if (score == null) {
      return -1;
    } else {
      return score.getValue();
    }
  }

  public Hashtable getScores () {
    if (scores == null)
      scores = new Hashtable(1, 1.0F);
    return scores;
  }

  public void setScore(double score) {
    if (scores == null)
      scores = new Hashtable(1, 1.0F);
    Score s = (Score) scores.get("score");
    if (s == null) {
      s = new Score("score", score);
      scores.put ("score", s);
      this.score = score;
    } else {
      s.setValue (score);
      this.score = score;
    }
  }

  public void addScore (Score s) {
    if (scores == null)
      scores = new Hashtable(1, 1.0F);
    if ( ! scores.contains (s) ) {
      scores.put (s.getName(), s);
    }
  }

  public void addScore (double score) {
    if (scores == null)
      scores = new Hashtable(1, 1.0F);
    String name = scores.size() == 0 ? "score" : "score"+scores.size()+1;

    addScore (new Score (name, score));
    if (name.equals("score")) {
      this.score = score;
    }
  }

  public void addScore (String name, double score) {
    addScore (new Score (name, score));
  }

  public void addScore (String name, String score) {
    try {
      double s = Double.valueOf (score).doubleValue();
      addScore (name, s);
    } catch (Exception ex) {
      logger.error("Could parse " +  score + " as a double", ex);
    }
  }

  public SeqFeatureI merge   (SeqFeatureI sf) {
    if (getStrand() != sf.getStrand()) {
      logger.error("Can't merge features - wrong strands");
      return null;
    }

    int newlow;
    int newhigh;

    if (sf.getLow() < getLow()) {
      newlow = sf.getLow();
    } else {
      newlow = getLow();
    }

    if (sf.getHigh() > getHigh()) {
      newhigh = sf.getHigh();
    } else {
      newhigh = getHigh();
    }

    setLow (newlow);
    setHigh(newhigh);

    return this;
  }


  public static void main(String[] args) {
    SeqFeature sf1 = new SeqFeature(100,200,"pog",1);
    SeqFeature sf2 = new SeqFeature(100,200,"pog",-1);

    System.out.println("SeqFeature 1 " + sf1 + " " + sf1.getLow() + " " + sf1.getHigh());
    System.out.println("SeqFeature 2 " + sf2 + " " + sf2.getLow() + " " + sf2.getHigh());

    System.out.println("Overlap is " + sf1.overlaps(sf2) + " " + sf2.overlaps(sf1));

    sf1.setStrand(-1);

    System.out.println("Overlap is " + sf1.overlaps(sf2) + " " + sf2.overlaps(sf1));


    sf1.setStart(150);
    sf1.setEnd(50);

    System.out.println("Overlap is " + sf1.getLeftOverlap(sf2) + " " + sf1.getRightOverlap(sf2));
    System.out.println("Overlap is " + sf2.getLeftOverlap(sf1) + " " + sf2.getRightOverlap(sf1));

    SeqFeature sf3 = (SeqFeature)sf1.merge(sf2);

    System.out.println("Merged feature = " + sf3 + " " + sf3.getLow() + " " + sf3.getHigh());

    SeqFeature sf4 = (SeqFeature)sf2.merge(sf1);

    System.out.println("Merged feature = " + sf4 + " " + sf4.getLow() + " " + sf4.getHigh());
  }

  public void setPhase(int phase) {
    if (phase < -1 || phase > 2) {
      logger.error("Phase must be -1,0,1,2 : " + phase);
    } else
      this.phase = (byte)phase;
  }

  public int getPhase() {
    return (int)this.phase;
  }

  public int getEndPhase() {
    return (int)((this.length() - getPhase()) % 3);
  }

  
  /** @return 1,2,3 for frame, or -1 if no frame */
  public int getFrame () {
    int frame = -1; // shouldnt this be -2?
    if (getRefSequence() != null) {
      frame = getRefSequence().getFrame(getStart(), getStrand() == 1);
    }
    // 0,1,2 -> 1,2,3
    return frame + 1;
  }

  // compares ranges - not used - delete?
  public int compareTo(Object sfObj) {
    SeqFeatureI sf = (SeqFeatureI)sfObj;
    int complow = sf.getLow();

    int low     = getLow();
    if (low > complow) {
      return 1;
    } else if (low < complow) {
      return -1;
    }

    int comphigh = sf.getHigh();
    int high     = getHigh();
    if (high > comphigh) {
      return 1;
    } else if (high < comphigh) {
      return -1;
    }
    return 0;
  }

  public void addProperty(String name, String value) {
    if (value != null && !value.equals("")) {
      if (properties == null) {
        properties = new Hashtable(1, 1.0F);
      }
      Vector values;
      if (properties.get(name) == null) {
        values = new Vector();
        properties.put(name, values);
      } else {
        values = (Vector) properties.get(name);
      }
      values.addElement(value);
    }
  }

  public void removeProperty(String key) {
    if (key != null && !key.equals("")) {
      if (properties == null)
        return;
      if (properties.containsKey(key)) {
        properties.remove(key);
      }
    }
  }

  public void replaceProperty(String key, String value) {
    removeProperty(key);
    addProperty(key, value);
  }

  // Modified 12/2002 CTW - now uses vector of values.
  // 2/2003: If there's more than one value, return the LAST one, not the first.  --NH
  public String getProperty(String name) {
    if ((properties == null) || (properties.get(name) == null)) {
      return "";
    } else {
      Vector values = (Vector) properties.get(name);
      //      return (String) values.elementAt(0);
      return (String) values.lastElement();
    }
  }

  // New 12/2002 CTW - supports multiple properties with same name.
  public Vector getPropertyMulti(String name) {
    if (properties == null)
      return null;
    else 
      return (Vector) properties.get(name);
  }

  // Modified 12/2002 CTW - returns hash with only one string for each key.
  public Hashtable getProperties() {
    if (properties == null)
      properties = new Hashtable(1, 1.0F);
    Hashtable hash = new Hashtable(1, 1.0F);
    Enumeration e = properties.keys();
    while (e.hasMoreElements()) {
      Object key = e.nextElement();
      String value = (String) ((Vector) properties.get(key)).elementAt(0); 
      hash.put(key, value);
    }
    return hash;
  }

  // New 12/2002 CTW - returns hash of property vectors.
  public Hashtable getPropertiesMulti() {
    if (properties == null)
      properties = new Hashtable(1, 1.0F);
    return properties;
  }

  public void clearProperties() {
    // Clearing out the hash isn't good enough.  If we've cloned this object,
    // we need to set its properties to null so the clone will be forced to
    // make a new properties rather than dangerously sharing its parent's.
    properties = null;
  }

  /** conceptually any piece of sequence may potentially be translated
    // this is useful to ascertain the potential of any given sequence
    // it may also be needed if a prediction program provides this
    // information

    // This method translates by extracting the coding pieces of
    // sequence from the exons to create a single string which
    // is then translated with phase 0. The stop codon
    // is NOT included in the translated region when the
    // Translation start and stop have come from the fly XML data.
    translates get_ORF of cdna. get_ORF returns "" if no translation_start
    set, in this case the cdna is translated. Does phase need to be taken into
    account somehow?
  */
  public String translate() {

    String aa = "";

    String sub_sequence = get_cDNA();

    if (sub_sequence.length() > 2) {
      aa = na2aa (get_ORF(sub_sequence));
    }
    return aa;
  }

  public String na2aa (String na) {
    String aa = ((na != null && na.length() > 2) ?
                 (DNAUtils.translate (na,
                                      DNAUtils.FRAME_ONE,
                                      DNAUtils.ONE_LETTER_CODE)) :
                 "");
    return aa.trim();
  }

  /** this will always return something even if the parent has
      not had a start of translation set. If translation start has not been
      set it equals 0 and wont pass the contains test and the empty string
      will be returned.
      Changed this so exons of annotations can get translated. If doesnt contain
      start uses phase as start (if doesnt contain end uses end of seq)
      Otherwise an exon that doesnt contain the start will not get translated.
      So a side effect of this is this will translate utr exons that are before 
      the start of translation. Should this be checked for and not translated?
  */
  public String get_ORF (String sub_sequence) {
    String orf = "";

    if (getParent() != null) {
      FeatureSetI fs = getParent();
      int begin = phase;
      if (this.contains (fs.getTranslationStart()) &&
          this.isSequenceAvailable (fs.getTranslationStart()))
        begin = Math.abs((int)(fs.getTranslationStart() - this.getStart()));

      int end = sub_sequence.length(); // + 1?
      if (this.contains (fs.getTranslationEnd()) &&
          this.isSequenceAvailable (fs.getTranslationEnd()))
        // JC: note that this = length - 1 because we don't want to include the first base of the stop codon
        end = Math.abs((int)(fs.getTranslationEnd() - this.getStart()));
      orf = sub_sequence.substring(begin,end);
    } else { // would get_ORF ever be called on something without a ref feat?
      orf = sub_sequence.substring (phase);
      logger.info (getName() + " phase " + phase);
    }
    return orf;
  }

  public int getGenomicPosition(int transcript_pos) {
    int genome_pos = 0;
    int transcript_offset = transcript_pos - 1;

    int genome_offset = (getStrand() == 1 ?
                         getStart() + transcript_offset :
                         getStart() - transcript_offset);

    if (this.contains (genome_offset + 1))
      genome_pos = genome_offset + 1;

    logger.debug("transcript position " + transcript_pos + " is genomic position " + genome_pos);
    return genome_pos;
  }

  /** For a position in peptide coordinates get the corresponding genomic position 
      peptide object? */
  public int getGenomicPosForPeptidePos(int peptidePosition) {
    int featPos = peptidePosition * 3;
    return getGenomicPosition(featPos);
  }

  /**
     This needs to be fixed to account for edits to the genomic
     sequence, but I don't think it is urgent because this case
     is still so extremely rare */
  public int getFeaturePosition (int genomic_pos) {
    int offset = 0;
    if (contains (genomic_pos)) {
      if (getStrand() == 1) {
        offset = genomic_pos - getStart();
      } else {
        offset = getStart() - genomic_pos;
      }
    }
    return ((int) offset + 1);
  }

  /** Returns the number of direct children (not all descendants) this feature
      has. 0 is returned if not a FeatureSetI */
  public int getNumberOfChildren() {
    if (!canHaveChildren()) 
      return 0;
    else
      return size();
  }

  /** FeatureSet overrides - merge with getNumberOfChildren */
  public int size() { return 0; }

  /** by default no kids - no-op */
  public void clearKids() {}

  /** returns a vector of all the child features belonging to this
      feature. an empty vector is returned if the feature is unable
      to have children */
  public Vector      getFeatures    () {
    if (!canHaveChildren()) 
      return new Vector();
    else {
      logger.error(getName() + " a " + this.getClass().getName() +
                   " says it can have children, but it is not a FeatureSetI");
      return new Vector();
    }
  }
  
  /** Return true if getFeatures().size() > 0, we actually have a kid */
  public boolean hasKids() {
    return getFeatures().size() > 0;
  }
  
  /** returns a seqfeature at the specified position */
  public SeqFeatureI getFeatureAt   (int i) {
    if (!canHaveChildren())
      return null;
    else {
      logger.error (getName() + " a " + this.getClass().getName() +
                    " says it can have features, but it is not a FeatureSetI");
      return null;
    }
  }
  
  /** By default SeqFeature has no kids so returns -1 be default. */
  public int getFeatureIndex(SeqFeatureI sf) {
    return -1;
  }
  
  /** no-op. SeqFeatures with children should override(eg FeatureSet). 
      a non child bearing SeqFeature neednt do anything */
  public void addFeature(SeqFeatureI child) {}
  /** no-op - overridden by FeatureSet */
  public void addFeature(SeqFeatureI feature, boolean sort){}

  /**
   * The number of descendants (direct and indirect) in this FeatureSetI.
   * This method should find each child, and invoke numChildFeatures for each
   * child that is a FeatureSetI, and add 1 to the count for all others.
   * FeatureSetI implementors should not count themselves, but only the
   * leaf SeqFeatureI implementations.
   * This should be renamed numDescendants. numChild can lead one to think its
   * its just the kids and not further descendants.
   * In fact there should be 2 methods: numDescendants, numChildren
   *
   * @return the number of features contained anywhere under this FeatureSetI
   */
  public int getNumberOfDescendents() {
    return 0;
  }

  /* this does count itself as well */
  public int numberOfGenerations() {
    int gen_count = 1;
    if (this.canHaveChildren()) {
      if (((FeatureSetI) this).size() > 0) { 
  SeqFeatureI sf = ((FeatureSetI) this).getFeatureAt(0);
  gen_count += sf.numberOfGenerations();
      }
    }
    return gen_count;
  }

  /**
   * General implementation of Visitor pattern. (see apollo.util.Visitor).
   ** At this level, the SeqFeature just throws a not-implemented exception, but 
   * this is subject to change if and when people write more Visitors against
   * this datamodel.
  **/
  public void accept(apollo.util.Visitor visitor){
    if(true){
      throw new RuntimeException("Visitor/accept not yet implemented for SeqFeature");
    }
    visitor.visit(this);
  }//end accept

  /**
   * Helper method designed to compare two feats which has just been initialized.
   * If the name, start and end are not the default values, then we call Range.isIdentical. 
   * Otherwise, we compare the IDs.
   * 
   * @param seqFeat
   * @return boolean
   * @see #isIdentical(RangeI range)
   */
  public boolean isSameFeat(SeqFeatureI seqFeat){
    if (!isIdentical(seqFeat))
      return false;
    //check Range and FeatureSet default value (this is bad, better to change the datamodel with a flag attribute to know if it's default) 
    if (!seqFeat.getName().equals(SeqFeatureI.NO_NAME) || 
        !((seqFeat.getStart() == 1 || seqFeat.getStart() ==  1000000000) &&
            seqFeat.getEnd() == -1 || seqFeat.getEnd() == -1000000000))
      return isIdentical(seqFeat);
    return this.getId().equals(seqFeat.getId());
    
  }
  
  
  /** Returns true if this == sf. This is overridden by FeatureSet which 
      checks for descendants with recursion. 
      An alternate way of doing this class heirarchy would be to have SeqFeature
      inherit from FeatureSet, or to just allow SeqFeature to have kids (and if
      you wanted have a special subclass that didnt have kids that would have 
      this function). Then these traversal functions would be more intuitive
      as its funny to have a contains function in SeqFeature superclass but
      awfully handy programaticly.
  */
  public boolean isAncestorOf(SeqFeatureI sf) {
    //return this == sf; 
    // ancestor is just an inverted descends
    return sf.descendsFrom(this);
  }
  /** This is the opposite of contains. Could be called isContainedBy I guess but
      I like descendsFrom better. Returns true if descends from (or is equal to)
      the SeqFeatureI passed in */
  public boolean descendsFrom(SeqFeatureI sf) {
    if (this == sf) return true;
    // Does the top level feature have a null ref?
    // If we are at the top then we failed to find ancestor equality
    if (getRefFeature() == null) return false;
    return getRefFeature().descendsFrom(sf);
  }

  public FeatureList getLeafFeatsOver(int pos) {
    FeatureList fl = new FeatureList(1);
    if (contains(pos)) {
      fl.addFeature(this);
    }
    return fl;
  }

  /** Whether the SeqFeature is an annotation - return false by default - 
      AnnotatedFeature overrides */
  public boolean hasAnnotatedFeature() {
    return false;
  }
  /** If we have an AnnotatedFeature then we are an annot and vice versa */
  public boolean isAnnot() { return hasAnnotatedFeature(); }

  public AnnotatedFeatureI getAnnotatedFeature() { return null; }

  /** Whether SeqFeatureI has a hit feature. FeaturePair returns true. */
  public boolean hasHitFeature() {
    // if we go to new paradigm where any seq feat can have a hit feat then this
    // would do getHitFeature() != null - but will that wreak havoc?
    return false;
  }
  
  private SeqFeatureI queryFeature;
  /** Query feats hold cigars. This gives hit feats access to query feat & its cigar */
  public void setQueryFeature(SeqFeatureI queryFeat) {
    this.queryFeature = queryFeat;
  }

  /** A sequence feature is always alignable if it has sequence */
  public boolean hasAlignable() { 
    return getResidues()!=null; 
  }

  // Should we have some way of querying if the alignment 
  // is just a trivial one?
  // public boolean alignmentIsTrivial() { return true; }
  
  /** Alignment will be padded if peptide */
  public String getAlignment() { 
    if (alignment == null) {
      // have to have both alignments
      if (haveRealAlignment()) {
        // Check if hit alignment given explicitly
        if (!haveExplicitAlignment() && hasCigar()) {
          parseCigar();
        }
        alignment = getExplicitAlignmentWithSpacing();
      }
      // if there is no explicit alignment for both nor cigar
      else if (queryFeature != null) {
        alignment = queryFeature.getAlignment();
      }
      // if alignment still equals null use genomic residues
      // this is somewhat controversial (len trigg wanted it this way) and 
      // this could be a configurable. minimally i think it needs to return
      // the empty string instead of null - jalview will display NULL
      if (alignment == null)
        alignment = getResidues();
    }
    return alignment;
  }
  
  /** set the alignment string (with padding if has it) */
  public void setAlignment(String alignment) {
    this.alignment = alignment;
  }

  public boolean haveRealAlignment() {
    if (hasCigar() || haveExplicitAlignment())
      return true;
    return hasHitFeature() && getHitFeature().haveRealAlignment();
  }

  protected boolean hasCigar() { return false; }

  public void parseCigar() {
    // query feats got the cigar
    if (queryFeature != null)
      queryFeature.parseCigar();
  }

  /** Alignment without padding. peptide alignment with no padding. */
  public String getUnpaddedAlignment() {
    if (unpaddedAlignment == null) {
      if (haveExplicitAlignment())
        unpaddedAlignment = getExplicitAlignment(); 
      // if (hasCigar()) parseCigar(); // sets unpaddedAlignment
    }
    return unpaddedAlignment;
  }
  /** Explicitly set alignment. no padding */
  public String getExplicitAlignment() { return explicitAlignment; }

  public void setExplicitAlignment(String align) {
    this.explicitAlignment = align;
  }
  public boolean haveExplicitAlignment() { return getExplicitAlignment() != null; }

//  public String     getHitAlignment() { return getQueryAlignment(); }
  /** do nothing - meaningless if not a feature pair */
//  public void setExplicitHitAlignment(String explicitHitAlignment) {}
  /** Returns null - feature pair overrides */
//  public String getExplicitHitAlignment() { return null; }
//  public boolean haveExplicitHitAlignment() { return getExplicitHitAlignment() != null; }
//  public String     getQueryAlignment() { return getResidues(); }
  /** do nothing - meaningless outside of FeaturePair context */
//  public void setExplicitQueryAlignment(String expRefAlignment) {}
  /** Returns null - feature pair overrides */
//  public String getExplicitQueryAlignment() { return null; }

  private String getExplicitAlignmentWithSpacing() {
    if (getExplicitAlignment() == null)
      return null;
    if (alignmentIsPeptide())
      return addPeptideSpacing(getExplicitAlignment()); // with peptide spacing
    else
      return getExplicitAlignment(); // no spacing
  }

  private String addPeptideSpacing(String unspacedPeptide) {
    StringBuffer spacedPeptide = new StringBuffer();
    // 2 spaces for peptide spacing
    for (int i = 0; i < unspacedPeptide.length(); i++) 
      spacedPeptide.append(unspacedPeptide.charAt(i) + "  ");
    return spacedPeptide.toString();
  }


  /** an explicit variable for cigar strings that are compact representations of alignments */
  public String getCigar() { return null; }
  public void setCigar(String cigar) {}

  /** Set explicit hit alignment - string with gaps. The compact alternative 
      to this is cigars(which should be migrated to this interface from 
      FeaturePairI). For an alignment to work you must set both setHitAlignmnet
      and setRefAlignment. Previously this was set with 
      hit.setProperty("alignment") 
  */
  //public void setAlignment(String explicitAlignment) {
  //  this.explicitAlignment = explicitAlignment;
  //}
  //public void setHitAlignment(String refAlignment) {}
  public String     getHname() { return getRefSequence().getName(); }
  public int        getHstart() { return getStart(); }
  public int        getHend  () { return getEnd(); }
  public int        getHlow() { return getLow(); }
  public int        getHhigh() { return getHigh(); }
  public int        getHstrand() { return getStrand(); }

  //public boolean alignmentIsPeptide() { return false; } 

  // in lieu of a real Sequence object for alignment... for now...
  private String alignmentResidueType = null;
  // private SequenceI alignmentSequence; // ...eventually...

  /** Returns true if alignment is peptide sequence, false if nucleotide 
   This is for the hit alignment(not query). Rename hitAlignIsPeptide?
   actually this seems to be for both hit & query now 
   This code aint right - its mixing up alignment seq and feat seq. feat seq can
   be dna while align seq can be aa - the feat seq translated (tblastx) - so ya
   cant muddle the 2. really we need a Sequence object for the alignment itself!
  */
  public boolean alignmentIsPeptide() {

    // Nasty hack to tell peptide from dna. Blergh.
    // BLAST Hstarts and Hends are actually in peptide coordinates, which seems
    // erroneous to me. Anyways its just as well to use alignment length.
    // so feat seq residue type and align res type can be diff - align can translate
    // the feat seq. however if feat seq type is aa, align has to be aa (doesnt it?)
    if (alignmentResidueType == null) {
      //String residueType = null;
      SequenceI seq = getRefSequence();//hit.getRefSequence();
      if (seq != null && seq.isAA()) {
        alignmentResidueType = SequenceI.AA;
      }

//     if (residueType == null ) {
      if (alignmentResidueType == null) {
        // These add up the length of all the parents feats 
        //int hit_length = ((FeatureSetI) getRefFeature()).getSplicedLength();
        // for now just doing this single feature - will reimplement whole featset 
        // if need be - after discussion - its a better way to do the isPeptide hack
        // but even better would be to have a isPeptide flag in tiers file
        //int hit_length = length();
        int featBasePairLength = length(); // for query feats
        // for hits cant get length of hit feat itself as hit may be in peptide coords!
        // have to get length of query coords (presumably in bps)
        if (queryFeature != null) // hit feats
          featBasePairLength = queryFeature.length();
        //int seq_length = getParentAlignmentSeqLength();
        int seq_length = getUnpaddedAlignment().length();
        
        alignmentResidueType = 
          SeqFeatureUtil.guessResidueTypeFromFeatureLength(seq_length,featBasePairLength);

        // alignment seq is different than feat seq!
        //if (seq != null)  seq.setResidueType (residueType);
      }
    }
    return alignmentResidueType == SequenceI.AA;
  }

  /** this is so we can specifically get the length of a alignment
      or the portion for transcripts that share a 5' first exon 
      This actually works on the FeaturePairs FeatureSet parent - should it be
      a FeatureSet method? its not the length of this seq feats align seq, but the
      length of all of its parents childrens hits align seqs - its very specialized
      and awkward because there isnt a separate data structre (FeatSet) for hits (as
      there is in chado) - so to gather all hits ya hafta go 
      hit->query(FP)->FS->querie kids(FPs)->hits - awkward! this doesnt yet do this
      and this is all for the peptide hack
      12/2005: NO LONGER USED
  */
//   private int getParentAlignmentSeqLength() {
//     int spliced_length = 0;

//     FeatureSetI parent = (FeatureSetI) getRefFeature();
//     int size = parent.size();
//     for (int i = 0; i < size; i++) {
//       //FeaturePairI span = (FeaturePairI) parent.getFeatureAt(i);
//       SeqFeatureI querySpan = parent.getFeatureAt(i);
      
//       SeqFeatureI hitSpan = querySpan.getHitFeature();
//       // why just explicits?? - what about cigars??
//       //if (querySpan.haveExplicitHitAlignment())
//       if (hitSpan.getUnpaddedAlignment() != null) // haveUnspacedAlignment
//         //spliced_length += querySpan.getExplicitHitAlignment().length();
//         spliced_length += hitSpan.getUnpaddedAlignment().length();
//       else
//         spliced_length += hitSpan.length();
//     }
//     return spliced_length;
//   }

  // private boolean isHit() { return queryFeature != null; }
  // util?
  // private int getWholeHitAlignmentLength()
  // private int getWholeQueryAlignmentLength()

  // my new thought 
  // SeqFeatureI hitFeature;
  // setHitFeature(SeqFeatureI)
  

  /** shoulnt this return null? yea it should - why is it returning this?? */
  public SeqFeatureI getHitFeature() { return this; }

  /** returns null - FeaturePair and FeatureSet override */
  public SequenceI getHitSequence() { return null; }

  /** Whether feature has a peptide sequence - default is false - override if otherwise 
      (Transcript returns true) */
  public boolean hasPeptideSequence() {
    return false;
  }

  /** If SeqFeatureI has a peptide sequence returns it, otherwise returns null.
      By default SeqFeature returns null. SeqFeature subclasses that have peptide
      sequence must override (eg Transcript) */
  public SequenceI getPeptideSequence() {
    return null;
  }

  public Protein getProteinFeat() { return null; }

  /** returns self if self has position. Otherwise returns null.
      FeatureSet overrides this to return child who has pos
      position is genomic */
  public SeqFeatureI getFeatureContaining(int position) {
    if (position >= getLow() && position <= getHigh()) {
      return this;
    }
    else {
      return null;
    }
  }

  /** As a convenience one can record what the analogous feature is on 
      the opposite strand. For instance a forward strand EST "analysis" would
      hold the reverse strand EST analysis.
      This comes in handy for moving results to opposite strand.  */
  public void setAnalogousOppositeStrandFeature(SeqFeatureI oppositeFeature) {
    analogousOppositeStrandFeature = oppositeFeature;
  }
  /** Returns false if analog opp strand fs not set */
  public boolean hasAnalogousOppositeStrandFeature() {
    return analogousOppositeStrandFeature != null;
  }
 
  /** Returns null if hasAnalogousOppositeStrandFeatureSet is false. Otherwise
      returns the analog opposite strand feature set */
  public SeqFeatureI getAnalogousOppositeStrandFeature() {
    return analogousOppositeStrandFeature;
  }

  /** Return true is feature has translation start and stop. transcript returns 
      true - should this be renamed isTranscript? 
      should FeatureSet potentially return true */
  //public boolean hasTranslationStartAndStop();
  public boolean isTranscript() { return false; }
  public boolean isProtein() { return false; }
  public boolean isExon() { return false; }
  public boolean isAnnotTop() { return false; }
  public boolean isSequencingError() { return false; }
  
  /** Return false by default - FeatureSet overrides - 
      if this is the same as isProteinCodingGene then we should delete it */
  public boolean hasTranslation() { return false; }
  
  public boolean isProteinCodingGene() { return false; }

  /** By default seqfeature has no translation - returns null */
  public TranslationI getTranslation() { return null; }

  /** FeatureSet subclasses implement translation functionality */
//   public boolean setTranslationStart (int pos) { return false; }
//   public boolean setTranslationStart (int pos, boolean set_end) { return false; }
//   public void setTranslationEnd (int pos) {}
//   public int getTranslationStart() { return -1; }
//   public int getTranslationEnd() { return -1; }
//   public void setTranslationStartAtFirstCodon() {}
//   public void setTranslationEndFromStart() {}
//   public int getLastBaseOfStopCodon() { return -1; }
//   public RangeI getTranslationRange() {
//     return new Range(getTranslationStart(),getTranslationEnd());
//   }


  /** These methods dealing with xrefs have been moved from AnnotatedFeature. */
  public Vector getDbXrefs() {
    return getIdentifier().getDbXrefs();
  }

  public DbXref getDbXref(int i) {
    return (DbXref)getDbXrefs().get(i);
  }

  public void addDbXref(DbXref xref) {
    getIdentifier().addDbXref(xref);
  }

  /** Return the DbXref, if any, that has isPrimary==true */
  public DbXref getPrimaryDbXref() {
    Vector xrefs = getDbXrefs();
    for (int i = 0; i < xrefs.size(); i++) {
      DbXref xref = (DbXref) xrefs.elementAt (i);
      if (xref.isPrimary())
        return xref;
    }
    return null;
  }

  public Identifier getIdentifier() {
    if (identifier == null)
      identifier = new Identifier("");
    return this.identifier;
  }

  public void setIdentifier(Identifier identifier) {
    this.identifier = identifier;
  }

/**ADDED BY TAIR Set the user object */
  public void setUserObject(Object userObject) {
      this.userObject = userObject;
  }

  /**ADDED BY TAIR Return the user object or null if none exists */
  public Object getUserObject() {
      return userObject;
  }

  /** Return an integer that describes coding props. Integers defined above.
      Changed logic here for undefined translation end. Previously transcripts
      with no end came back with UNKNOWN. Now if it has a start but no end
      it can be CODING, UTR_5PRIME, or MIXED_5PRIME. The reason the transcript 
      can exist without a stop if the data just doesnt support a stop, 
      or if its amidst editing. I felt ok modifying this method since it is
      only used in OtterXMLRenderingVisitor (and these changes should be ok
      for what its doing. There are redundant methods in 
      DrawableGeneSeqFeature and DrawableAnnotatedGeneSeqFeature called 
      getExonType(). All of these methods should be merged into one. 
      If there is no translation start it still returns UNKNOWN which is 
      probably appropriate.

      Suz- merged the methods (found a bug and rather than change it
      both places-ugh-did the merge). the bug was: not testing for
      the lack of a translation stop (transEnd == 0)

  */
  public int getCodingProperties() {
    if (getRefFeature() instanceof FeatureSetI) {
      FeatureSetI trans = (FeatureSetI)getRefFeature();
      int transStart = trans.getTranslationStart();
      int transEnd   = trans.getTranslationEnd();
      
      int exontype;

      /* only test for start of translation, the end may not
         be known if the 3' end is truncated */
      if (transStart == 0) {
        return CodingPropertiesI.UNKNOWN;
      }
    
      boolean has_start = this.contains(transStart);
      boolean has_stop = this.contains(transEnd);

      if (has_start && has_stop)
        exontype = CodingPropertiesI.MIXED_BOTH;
      else if (has_start)
        exontype = CodingPropertiesI.MIXED_5PRIME;
      else if (has_stop)
        exontype = CodingPropertiesI.MIXED_3PRIME;
      else {
        if (trans.getStrand() == 1) {
          if (getHigh() < transStart)
            exontype = CodingPropertiesI.UTR_5PRIME;
          else if (getLow() > transEnd && transEnd > 0)
            exontype = CodingPropertiesI.UTR_3PRIME;
          else
            exontype = CodingPropertiesI.CODING;
        }
        else {
          if (getLow() > transStart)
            exontype = CodingPropertiesI.UTR_5PRIME;
          else if (getHigh() < transEnd && transEnd > 0)
            exontype = CodingPropertiesI.UTR_3PRIME;
          else
            exontype = CodingPropertiesI.CODING;
        }
      }
      return exontype;
    }
    return CodingPropertiesI.UNKNOWN;
  }

  public StrandedFeatureSetI getStrandedFeatSetAncestor() {
    return getRefFeature().getStrandedFeatSetAncestor();
  }

  /** Returns true if feature is start or stop codon 
   * currently does with feature type - perhaps should have a setIsCodon(bool)
   * method? */
  public boolean isCodon() {
    String type = getFeatureType();
    return type.startsWith("startcodon") || type.startsWith("stopcodon");
  }

  public void setSyntenyLinkInfo(String linkInfo) {
    syntenyLinkInfo = linkInfo;
  }
  public String getSyntenyLinkInfo() {
    return syntenyLinkInfo;
  }
  public boolean hasSyntenyLinkInfo() {
    return syntenyLinkInfo != null;
  }


  /** For debugging */
  public String toString() {
    return "[SeqFeature " + name + " (id " + id + "): type = " + type + ", biotype = " + biotype + ", range = " + getStart() + "-" + getEnd() + ((getStrand() == -1) ? " (minus)" : "" + ", parent = " + ((getRefFeature() == null) ? "none" : getRefFeature().getId())) + "]"; // DEL
  }

}
