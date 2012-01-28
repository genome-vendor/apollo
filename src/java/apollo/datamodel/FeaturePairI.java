package apollo.datamodel;

public interface FeaturePairI extends SeqFeatureI {

  public void        setHstrand(int strand);
  public void        setHstart(int start);
  public void        setHend(int end);
  public void        setHlow(int low);
  public void        setHhigh(int high);

  public SeqFeatureI getQueryFeature();

  public void        setQueryFeature(SeqFeatureI sf);
  public void        setHitFeature  (SeqFeatureI sf);

  public SequenceI getHitSequence();
  public void setHitSequence(SequenceI seq);

  // public void        invert(); // never used

  /* an explicit variable for cigar strings used in alignment viewer */
  //public String getCigar(); --> SeqFeatureI
  //public void setCigar(String cigar);

}


