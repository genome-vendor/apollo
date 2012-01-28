package apollo.datamodel;

import java.util.*;
import apollo.util.FeatureList;

public interface SeqFeatureI extends RangeI {
  public String      getId();
  public void        setId(String id);
  /** returns false if getId() == null */
  public boolean hasId();

  /**
     This is used if the Reference Sequence is being
     reverse complemented to move this feature onto
     the coordinates counting from the 3' end rather
     than the 5' end
     <br>
  */
  // public void        reverseComplement();

  /** This is used simply to move the feature directly
  to the opposite strand, without complementation
  of the Reference Sequence
  */
  public void        flipFlop ();

  /**
   * Retrieve the SequenceI defined by this feature itself.
   *
   * @return the current self SequenceI
   */
  public SequenceI   getFeatureSequence();

  public String      getRefId();

  /**
   * Retrieve the parent SeqFeatureI for this SeqFeatureI, or null if it is
   * a tree root.
   *
   * @return the parent SeqFeatureI
   Couldnt this return a FeatureSetI, doesnt a ref feature have to be a FeatureSetI?
   */
  public SeqFeatureI getRefFeature();

  /** Returns refFeature as FeatureSetI which I believe it is always the case
      that the refFeature is a FeatureSetI */
  public FeatureSetI getParent(); 

  /**
   * Set the parent SeqFeatureI.
   * <P>
   * When building a part-whole hieracy of features, you must both add a
   * feature to its parent, and then call child.setRefFeature(parent). It is
   * possible for a feature to be added to multiple feature sets, but there is
   * only one legitimate parent.
   *
   * @param refFeature the new parent SeqFeatureI
   */
  public void        setRefFeature(SeqFeatureI refFeature);

  public String      getTopLevelType();
  public void        setTopLevelType(String type);

  public String      getProgramName();
  public void        setProgramName(String type);

  public String      getDatabase();
  public void        setDatabase(String db);

  public double      getScore();
  public double      getScore(String score);
  public Hashtable   getScores();
  public void        setScore(double score);
  public void        addScore (Score s);
  public void        addScore (double score);
  public void        addScore (String name, double score);
  public void        addScore (String name, String score);

  public void        addProperty(String name, String property);
  public void        removeProperty(String name);
  public String      getProperty(String name);
  public Hashtable   getProperties();
  public void replaceProperty(String key, String value);
  public void clearProperties();
  public Hashtable   getPropertiesMulti();
  public Vector getPropertyMulti(String name);


  /** @return 1,2, or 3 for frame, or -1 if no frame */
  public int         getFrame();
  public void        setPhase(int phase);
  /** Phase is the internal offset from feature start for translation. It is
      NOT the frame relative to the ref sequence. 
      @return 0, 1, or 2
  */
  public int         getPhase();

  public int         getEndPhase();

  public int         compareTo(Object sfObj);

  public SeqFeatureI merge          (SeqFeatureI sf);

  /*
    It is possible to attempt to translate any piece of sequence
    Therefore the most general seq feature model must support 
    these operations
  */
  public String translate();
  public String get_cDNA();
  /** Returns a String of sequence from start to end of translation, in the
      case where a feature does not have a start and stop the cDNA is returned */
  public String getCodingDNA();
  public int getGenomicPosition(int feature_pos);
  /** For a position in peptide coordinates get the corresponding genomic position*/ 
  public int getGenomicPosForPeptidePos(int peptidePos);
  public int getFeaturePosition(int genomic_pos);

  /** Returns the number of features (not all descnedants)
      this feature has. 0 is returned if not a FeatureSetI */
  public int getNumberOfChildren();
  /** merge this with getNumberOfChildren */
  public int size();

  /** clear out allkids - if have any */
  public void clearKids();

  /** returns a vector of all the child features belonging to this
      feature. an empty vector is returned if the feature is unable
      to have children - should this be renamed getChildren for clarity?
      This should be changed to return a FeatureList */
  public Vector      getFeatures    ();

  /** Returns false if getFeatures().size() == 0 */
  public boolean hasKids();

  /** returns a seqfeature at the specified position - 
      rename getChild(i) for clarity?*/
  public SeqFeatureI getFeatureAt   (int i);

  /** Returns index of child if feat has child. If not returns -1 */
  public int getFeatureIndex(SeqFeatureI child);

  /** Add child feature */
  public void addFeature(SeqFeatureI child);
  /** Add feature at sorted position */
  public void addFeature(SeqFeatureI feature, boolean sort);
  
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
  public int getNumberOfDescendents();
  
  /** Returns the number of generations (including itself)
      that are at and below this feature. In other words,
      how deep is the tree from this feature down to the leaves */
  public int numberOfGenerations();
  
  /**
   * A general utility method to accept Visitors.
  **/
  public void accept(apollo.util.Visitor visitor);

  
  /**
   * Helper method designed to compare two feats which has just been initialized.
   * If the name, start and end are not the default values, then we call Range.isIdentical. 
   * Otherwise, we compare the IDs.
   * 
   * @param seqFeat
   * @return boolean
   * @see #isIdentical(RangeI range)
   */
  public boolean isSameFeat(SeqFeatureI seqFeat);
  
  
  
  /** Whether this is the ancestor of sf. For a feature set this is true
      if sf is a descendant of this or equal. For SeqFeature(non FeatureSet) 
      its true for equality. This replaces FeatureSetI.findFeature(sf)
      This needs to be renamed, there already is a contains from RangeI 
      that means something totally different, having 2 different contains 
      is confusing.
      Change to isAncestorOf(sf)
  */
  public boolean isAncestorOf(SeqFeatureI sf);

  /** This is the opposite of isAncestor. Returns true if descends from 
      (or is equal to) the SeqFeatureI passed in */
  public boolean descendsFrom(SeqFeatureI sf);

  /** This is used in the base editor to find the sub features that
      overlap a base with a sequence edit on it */
  public FeatureList getLeafFeatsOver(int pos);

  /** Whether the SeqFeature is an annotation - annotation here meaning
      gene, transcript or exon - not just top level annot - i fear the
      term "annotation" has 2 meanings - strictly the top level annotation,
      and loosely any level of the annotation. This needs to be resolved and
      made consistent. Should this be renamed hasAnnot - the fact that 
      AnnotatedFeatureI extends SeqFeatureI is an implementation detail */
  public boolean hasAnnotatedFeature();
  public boolean isAnnot();

  /** if hasAnnotatedFeature is true, this returns the AnnotatedFeature */
  public AnnotatedFeatureI getAnnotatedFeature();

  /** if isAnnot is true returns the associated AnnotatedFeature (may be ISA 
      but thats inconsequential here), if isAnnot is false returns null */
  //public AnnotatedFeatureI getAnnotatedFeature();

  /** Returns true if feat has an alignable (may be an ISA but thats 
      inconsequetial here) */
  public boolean hasAlignable();

  /** Whether SeqFeatureI has a hit feature. FeaturePairI returns true. */
  public boolean hasHitFeature();
  /** if hasHitFeature true return hit, else return itself */
  public SeqFeatureI getHitFeature();

  /** If feat has hit feat, or children with hit feat, then this is the seq
   * associated with the hit or kid hits */
  public SequenceI getHitSequence();

  public void setQueryFeature(SeqFeatureI queryFeat);

  /** Alignment will be padded if peptide */
  public String getAlignment();
  public void setAlignment(String alignment);

  /** Alignment without padding. peptide alignment with no padding. */
  public String getUnpaddedAlignment();
  /** Explicitly set alignment. no padding */
  public String getExplicitAlignment();
  
  public void setExplicitAlignment(String explicitAlignment);
  public boolean haveExplicitAlignment();

  public boolean haveRealAlignment();

  /** an explicit variable for cigar strings that are compact representations of alignments */
  public String getCigar();
  public void setCigar(String cigar);
  public void parseCigar();

  /* This is to indicate whether or not the feature can be shown
     in an alignment, i.e. it is either a FeaturePairI or an Exon
     This is a convenience for getHitFeature().getAlignment()
  */
//  public String      getHitAlignment();
  /** Convenience for getQueryFeature().getAlignment() */
//  public String      getQueryAlignment(); // rename getQueryAlignment?
  /** Set explicit hit alignment - string with gaps. The compact alternative 
      to this is cigars(which should be migrated to this interface from 
      FeaturePairI). For an alignment to work you must do both 
      setExplicitHitAlignment and setExplicitQueryAlignment. Previously this was 
      set with hit.setProperty("alignment") */
//  public void setExplicitHitAlignment(String explicitHitAlignment);
  /** Return explicit hit alignment string - null if have none */
//  public String getExplicitHitAlignment();
//  public boolean haveExplicitHitAlignment();
  /** should it be just setAlignment and then ya do a hit.setAlignmnet and
      query.setAlignment? */
//  public void setExplicitQueryAlignment(String explicitQueryAlignment);
  /** return null if not explicit query alignment */
//  public String getExplicitQueryAlignment();

  /** convenience function for getHitFeature().getName() */
  public String      getHname();

  public int        getHstart();

  public int        getHend  ();

  public int        getHlow();

  public int        getHhigh();

  public int         getHstrand();

  /** Sequence of the alignment of hit to query, previously stored in hit features
      subject_alignment property. If no actual alignment sequence returns hit 
      sequence. When cigars get put in, this will calculate the alignment from the
      cigar and hit seq
  */
  public boolean alignmentIsPeptide();

  /** Whether feature has a peptide sequence */
  public boolean hasPeptideSequence();

  /** If SeqFeatureI.hasPeptideSequence()==true returns pep seq, otherwise returns null */
  public SequenceI getPeptideSequence();

  /** getProteinFeature().getReferenceSequence() should replace getPeptideSeq() */
  public Protein getProteinFeat();

  /** Returns the FIRST child feature containing the position. If no children returns self
      if self has position. position is genomic */
  public SeqFeatureI getFeatureContaining (int position);

  /** to get a field-by-field replica of this feature */
  public Object clone();
  /** clones feature */
  public SeqFeatureI cloneFeature();
  
  /** Check to see if this feature is a cloned feature
   * 
   * @return whether this feature is a cloned feature
   */
  public boolean isClone();

  /** If this feature was cloned from another feature, return the original feature.
   *  Note that the original feature may have changed since the time of cloning.
   *  Returns null if this is not a cloned feature.
   *  
   * @return the source of the cloned feature
   */
  public SeqFeatureI getCloneSource();
  
  /** As a convenience one can record what the analogous feature is on 
      the opposite strand. For instance a forward strand EST "analysis" would
      hold the reverse strand EST analysis.
      This comes in handy for moving results to opposite strand. 
  */
  public void setAnalogousOppositeStrandFeature(SeqFeatureI oppositeFeature);
  /** Returns false if analog opp strand fs not set */
  public boolean hasAnalogousOppositeStrandFeature();
  /** Returns null if hasAnalogousOppositeStrandFeatureSet is false. Otherwise
      returns the analog opposite strand feature set */
  public SeqFeatureI getAnalogousOppositeStrandFeature();


  /** Return true is feature has translation start and stop. transcript returns 
      true - should this be renamed isTranscript? 
      should FeatureSet potentially return true */
  //public boolean hasTranslationStartAndStop();
  public boolean isTranscript();

  public boolean isExon();

  public boolean isProtein();
  
  public boolean isSequencingError();

  /** true for top level annots (e.g. genes & 1 level annots not transcripts nor exons)
   * i think we also need an isResultTop or just isTop() especially if results get more
   * varied in structure - like 1 level results */
  public boolean isAnnotTop();

  /** im thinking maybe there should be a separate interface for translation methods 
      (yes i know there is - its called a feature set) 
      and seqfeatureI would have this - not be 
      implemented in a subclass (or if it is not have that be noticaeable at the
      api/interface level) in other words a HASA not a ISA! i think that would be
      cleaner */
//   public boolean setTranslationStart (int pos);
//   public boolean setTranslationStart (int pos, boolean set_end);
//   public void setTranslationEnd (int pos);
//   public int getTranslationStart();
//   public int getTranslationEnd();
//   public void setTranslationStartAtFirstCodon();
//   public void setTranslationEndFromStart();
//   public int getLastBaseOfStopCodon();
//   public RangeI getTranslationRange();
  public boolean hasTranslation(); // delete? synonomous with isProteinCodingGene?
  public boolean isProteinCodingGene();
  public TranslationI getTranslation();

  // Moved from AnnotatedFeatureI.  In GAME XML data, only annotations and sequences
  // have dbxrefs, but in Chado data, other features can have them as well.
  // Seems harmless enough to allow it.
  public void addDbXref(DbXref xref);
  public Vector getDbXrefs();
  public DbXref getDbXref(int i);

    public HashMap getGenomicErrors();

     /**ADDED by TAIR Set the user object */
  public void setUserObject(Object userObject);
  /**ADDED BY TAIR Return the user object or null if none exists */
  public Object getUserObject();

  public int getCodingProperties();

  public StrandedFeatureSetI getStrandedFeatSetAncestor();

  /** Returns true if feature is start or stop codon */
  public boolean isCodon();

  /** Trying this out?? can set information needed to make synteny links between
      features for the synteny viewer. for now just a string (may need to be more
      involved in the future) 
      actually this needs to be a list! so either set(List) or add(String)
      refactor!
  */
  public void setSyntenyLinkInfo(String linkInfo);
  public String getSyntenyLinkInfo();
  public boolean hasSyntenyLinkInfo();
  
}
