package apollo.datamodel;

  /** im thinking maybe there should be a separate interface for translation methods 
      (yes i know there is - its called a feature set) 
      and seqfeatureI would have this - not be 
      implemented in a subclass (or if it is not have that be noticaeable at the
      api/interface level) in other words a HASA not a ISA! i think that would be
      cleaner */
public interface TranslationI {

  public boolean setTranslationStart (int pos);
  public boolean setTranslationStart (int pos, boolean set_end);
  public void setTranslationEnd (int pos);
  public boolean hasTranslationStart();
  public int getTranslationStart();
  public boolean hasTranslationEnd();
  /* these are used if the annotated transcript is truncated on
     either end */
  public void setMissing5prime (boolean partial);
  /** If true this means there is no real start codon - its missing, 
   rename this isMissing5PrimeStart? or isMissingTranslationStart? 
   hasTranslationStart() can be true while isMissing5prime is true - this means
   that theres a "contrived" start at the beginning of the transcript */
  public boolean isMissing5prime ();
  public void setMissing3prime (boolean partial);
  public boolean isMissing3prime ();

  public int getTranslationEnd();
  public void calcTranslationStartForLongestPeptide();
  public void setTranslationEndFromStart();
  public int getLastBaseOfStopCodon();
  public RangeI getTranslationRange();
  /** if validity is false the peptide is no longer valid and a new translation needs
      to be calculated  */
  public void setPeptideValidity(boolean validity);
}
