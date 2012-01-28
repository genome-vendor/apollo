package apollo.datamodel;

public interface ExonI extends AnnotatedFeatureI {

  /** Returns true if exon contains any coding region. */
  public boolean containsCoding();

  public Transcript getTranscript();

  public int getCodingProperties();

  /** Returns true if exon is nonConsensusAcceptor */
  public boolean isNonConsensusAcceptor();
  public boolean isNonConsensusDonor();

}
