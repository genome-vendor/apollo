package apollo.datamodel;

public class GenomicRange extends Range {
  protected String chromosome;

  public GenomicRange () {}

  public GenomicRange (String chr, int start, int end) {
    super (chr, start, end);
    this.chromosome = chr;
  }

  public String getChromosome() {
    return chromosome;
  }

  /** Im thinking this should be changed to setTopLevelName to generalize
      beyond chromosome (chrom_arm, contigs, super contigs...) */
  public void setChromosome (String chr) {
    if (chr == null || chr.equals("")) {
      try {
        new Throwable().printStackTrace();
      } catch (Exception e) {}
    }
    else
      this.chromosome = chr;
  }

  public String getOrganism() {
    if (getRefSequence() != null)
      return getRefSequence().getOrganism();
    else
      return null;
  }

  public void setOrganism (String org) {
    if (this.getRefSequence() != null)
      this.getRefSequence().setOrganism(org);
  }

}
