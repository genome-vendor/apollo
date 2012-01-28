package apollo.datamodel;

public class Protein extends AnnotatedFeature {

  // for now derives info from transcript - eventually take over the info in transcript
  private Transcript transcript;

  public Protein(Transcript transcript) {
    init(transcript);
  }

  public Protein(SeqFeatureI protFeat,Transcript transcript) {
    // gets start, end, type, strand & refseq
    this(transcript);
    //super(protFeat);
    super.initWithSeqFeat(protFeat);
    //init(transcript);
    transcript.setProteinFeat(this);
  }

  private void init(Transcript transcript) {
    this.transcript = transcript;
    setStrand(transcript.getStrand());
  }

  public boolean isProtein() { return true; }
  
  /** this better be true considering we are a protein */
  public boolean isProteinCodingGene() { return true; }

  // actually im wondering if this should be main genomic seq and protein seq separate?
  // as start and end are relative to main genomic not prot ??
  public SequenceI getRefSequence() {
    return transcript.getRefSequence();
    // return transcript.getPeptideSequence();??
  }

  /** transcripts peptide sequence should be moved here eventually */
  public SequenceI getPeptideSequence() {
    return transcript.getPeptideSequence();
  }
  // or getFeatureSequence()?

  /** for now just a wrapper for prot seq - eventually id will migrate here */
  public String getId() {
    return getPeptideSequence().getAccessionNo();
  }

  public void setId(String id) {
    // for now...
    getPeptideSequence().setAccessionNo(id);
  }

  public void setName(String name) {
    //super.setName(name);
    getPeptideSequence().setName(name); // for now
  }

  // eventually name & id for peptide should reside here - for now since everything
  // still accesses pep seq get from pep sequence
  public String getName() {
    return getPeptideSequence().getName();
  }

  /** Returns "polypeptide" as that is the SO term for proteins */
  public String getFeatureType() {
    return "polypeptide";
  }

  public int getStart() {
    // if a protein start has been set and transcripts translation start hasnt 
    // then use prot start - this is one of those funny transition things 
    // eventually it would be nice if transcripts translation start was the
    // protein start, but for now...
    if (super.getStart() != -1 && transcript.getTranslationStart() == 0)
      return super.getStart();
    return transcript.getTranslationStart();
  }

  /** Theres 2 situations for setting start - one is when initially building the datamodel
      and the exons havent been read in yet so you are not yet ready to calculate
      the translation, the other is the transcript is in place so setting start should
      cause a translate recalc to get a new end - for now just doing former(for game
      adapters sake) - for the latter i think there should be a 2nd setStart with a
      recalc flag or vice versa - or is setting start a result of the translate
      process rather than a trigger? */
  public void setStart(int start) {
    super.setStart(start);
  }

  //public void setStart(int start, boolean recalcTranslation) ...

  /** so the end is rather controversial. transcript.getTranslationEnd() is actually
      the first base of the stop - the last base of the protein is in fact the base
      before this - so the question is do we use the translation end or the last base
      of the protein (as chado does for their proteins) which is in fact
      getTranslationEnd() +/- 1???  i think we should go with real prot end
      not trans stop */
  public int getEnd() {
    //return transcript.getTranslationEnd();
    // ok im going with proteins ending at their end not the translation end
    // (like chado) - i hope this doesnt make things confusing??
    // also this aint quite right - needs to take into account introns
    int stop = transcript.getTranslationEnd() - getStrand();
    // in the case where there is no translation stop just use the end of the
    // transcript as thats the best you can do
    if (!transcript.hasTranslationEnd())
      stop = transcript.getEnd();
    return stop;
  }

  /** should this just be hasStart? translation is implicit now isnt it? */
  public boolean hasTranslationStart() {
    return getStart() != 0;
  }

  /** well this is actually controversial - translation end is just beyond the 
      protien - this should either be refactored or just be in transcript - need
      to think about this...*/
  public boolean hasTranslationEnd() {
    //return getEnd() != 0;
    return transcript.hasTranslationEnd();
  }


}
