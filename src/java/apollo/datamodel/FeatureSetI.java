package apollo.datamodel;

import java.util.Vector;
import java.util.HashMap;

import apollo.util.FeatureList;

public interface FeatureSetI extends SeqFeatureI, TranslationI {

  /**
   * The number of directly containd features.
   *
   * @return the number of directly contained features
   */
  //public int         size(); --> SeqFeatureI

  //public void        addFeature   (SeqFeatureI feature); --> SeqFeatureI
  //public void        addFeature   (SeqFeatureI feature, boolean sort);
  public void        deleteFeature(SeqFeatureI feature);

  public SeqFeatureI deleteFeatureAt(int i);
  //public int         getFeatureIndex (SeqFeatureI sf); -> SeqFeatureI
  //public SeqFeatureI getFeatureContaining (int pos); -> SeqFeatureI
  public int getIndexContaining (int pos);

//   /** This is used in the base editor to find the sub features that
//       overlap a base with a sequence edit on it */
//   public FeatureList getLeafFeatsOver(int pos); --> SeqFeatureI

  /**
     returns true if the count of the number of leaf features (those that
     can't have child features themselves) is > 0. That is, this feature
     isn't merely a collection of feature sets which are empty themselves */
  public boolean hasDescendents();

  public void        adjustEdges();
  public void        adjustEdges(SeqFeatureI sf);
  public void        sort (int sortStrand);
  public void        sort (int sortStrand, boolean byLow);

  public boolean hasNameBeenSet();

  public FeatureList findFeaturesByHitName(String hname);
  public FeatureList findFeaturesByName(String name);
  public FeatureList findFeaturesByName(String name, boolean kidNamesOverParent);
  /** Search for names and hit names */
  public FeatureList findFeaturesByAllNames(String name);
  public FeatureList findFeaturesByAllNames(String name, boolean useRegExp);
  /** searchString is pattern to search, useRegExp - whether pattern is reg exp,
      kidNamesOverParent if descendants match then ignore self match */
  public FeatureList findFeaturesByAllNames(String searchString, boolean useRegExp,
                                            boolean kidNamesOverParent);
  // This is used to synchronize drawables with models, if feature
  // can't be found then drawable is removed
  // moved to SeqFeatureUtil.containsFeature
  //public boolean findFeature(SeqFeatureI sf);

  // -> TranslationI
//   public boolean setTranslationStart (int pos);
//   public boolean setTranslationStart (int pos, boolean set_end);
//   public void setTranslationEnd (int pos);
//   public int getTranslationStart();
//   public int getTranslationEnd();
//   public void setTranslationStartAtFirstCodon();
//   public void setTranslationEndFromStart();
//   public int getLastBaseOfStopCodon();

  public int getPositionFrom(int at_pos, int offset);

  /** 
      Returns true if the position is within the CDS of this feature
  */
  public boolean withinCDS(int pos);

    /** returns true if this can encode a protein. This was just
     * for annotated features originally, but since the translation
     * code is in FeatureSet it needs to reside here. The default
     * is to return true;
     */
  public boolean isProteinCodingGene();
  
  /**
   * A setter is needed for some featureSet, like gene prediction results, 
   * which can have translation start and stop whithout being instances of transcripts   
   * @author cpommier
   */
  public void setProteinCodingGene(boolean isProteinCodingGene);

  // moved to TranslationI
//   /* these are used if the annotated transcript is truncated on
//      either end */
//   public void setMissing5prime (boolean partial);
//   /** If true this means there is no real start codon - its missing, 
//    rename this isMissing5PrimeStart? or isMissingTranslationStart? */
//   public boolean isMissing5prime ();
//   public void setMissing3prime (boolean partial);
//   public boolean isMissing3prime ();

  /* this is used to describe proteins that have unconventional
     start sites */
  public boolean unConventionalStart();
  public String getStartCodon();
  public String getStartAA();

  public boolean hasReadThroughStop();
  public String readThroughStopResidue();
  public void setReadThroughStop(boolean readthrough);
  public void setReadThroughStop(String residue);
  public int readThroughStopPosition();

  public int plus1FrameShiftPosition();
  public int minus1FrameShiftPosition();

  /** returns false of the frame shift position is unworkable */
  public boolean setPlus1FrameShiftPosition(int shift_pos);
  public boolean setMinus1FrameShiftPosition(int shift_pos);

  public boolean isSequencingErrorPosition(int base_position);
  public SequenceEdit getSequencingErrorAtPosition(int base_position);

  public SequenceEdit [] buildEditList();

  public SequenceI getHitSequence();
  public void setHitSequence(SequenceI seq);

  public int getSplicedLength();
  

  /** some useful flags, hopefully using minimal space, to pass
      information about handling through from the parser to the filter
      when running bop.  */
  public boolean isFlagSet(int flag);
  public void setFlag(boolean state, byte mask);
}
