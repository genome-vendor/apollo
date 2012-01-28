package apollo.datamodel;

import java.util.Vector;

public interface AnnotatedFeatureI extends FeatureSetI {
  public Vector     getEvidence();
  public void       addEvidence(String evidenceId);
  public void       addEvidence(String evidenceId, int type);
  public void       addEvidence(Evidence evidence);
  public int        deleteEvidence(String evidenceId);

  public void       setEvidenceFinder(EvidenceFinder ef);
  public EvidenceFinder getEvidenceFinder();

  public Vector     getComments();
  public void       addComment(Comment comm);
  public void       addComment(int index,Comment comm);
  public void       deleteComment(Comment comm);
  public void       deleteComment(int index);
  public void       clearComments();
  public int        getCommentIndex(Comment comm);

  public Identifier getIdentifier();
  public void       setIdentifier(Identifier id);

  public Vector     getSynonyms();
  public Vector     getSynonyms(boolean excludeInternalSynonyms);
  public void       addSynonym(String syn);
  public void       addSynonym(int index,String syn);
  public void       addSynonym(Synonym syn);
  public void       deleteSynonym(String syn);
  public void       deleteSynonym(Synonym syn);
  public void       clearSynonyms();
  // convenience functions
  public int        getSynonymSize();
  public Synonym    getSynonym(int i);
  // just checks if already has synonym name
  public boolean    hasSynonym(String name);

  public boolean isProblematic();
  public boolean isFinished();
  public void setIsProblematic(boolean isProblematic);

  public String getDescription();
  public void setDescription(String desc);

  //public boolean isAnnotationRoot(); -> SeqFeatureI

  /** Set owner of annotation. Currently only Transcript actually uses this. */
  public void setOwner(String owner);
  /** Get owner of annotation. Only used by Transcript. */
  public String getOwner();
  // public boolean hasOwner(); ??? boolean canHaveOwner()?

  /** Returns transcript exon number with non consensus acceptor. If top level
      annot return -1. */
  public int getNonConsensusAcceptorNum();
  /** Returns transcript exon number with non consensus donor. -1 for top. */
  public int getNonConsensusDonorNum();
  /** Return true if non consensus splicing has been deemed ok. This is set
      by curator. */
  public boolean nonConsensusSplicingOkay();

  /** Set whether its ok to have non-consensus splice site */
  public void nonConsensusSplicingOkay(boolean okay);

  public AnnotatedFeatureI cloneAnnot();

//   /** Returns true if annotated feature has/is a transcript. Rename isTranscript? */
//   public boolean hasTranscript(); // ??
//   /** If hasTranscript is true returns its transcript */
//   public Transcript getTranscript(); // ??

  /** If annotation is a transcript return the transcripts cdna sequence */
  public SequenceI get_cDNASequence();
//   /** if annot is transcript, return peptide for cdna name 
//       - does this belong in name adapter? YES! */
//   public String derivePeptideName(String cdnaName);

  /** Convenience method. The children of annots are always annots. */
  public AnnotatedFeatureI getAnnotChild(int i);

  /** Methods for getting/setting dbxrefs have been moved to SeqFeatureI */
}
