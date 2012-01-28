package apollo.datamodel;


public class SyntenyRegion {

  private Chromosome chr1;
  private int        start1;
  private int        end1;
  private Chromosome chr2;
  private int        start2;
  private int        end2;
  private int        orientation;

  public SyntenyRegion (Chromosome chr1, int start1, int end1,Chromosome chr2, int start2, int end2, int orientation) {
    this.chr1   = chr1;
    this.start1 = start1;
    this.end1   = end1;

    this.chr2   = chr2;
    this.start2 = start2;
    this.end2   = end2;

    this.orientation = orientation;
  }

  public Chromosome getChromosome1() {
    return chr1;
  }
  public int getStart1() {
    return start1;
  }
  public int getEnd1() {
    return end1;
  }
  public Chromosome getChromosome2() {
    return chr2;
  }
  public int getStart2() {
    return start2;
  }
  public int getEnd2() {
    return end2;
  }
  public int getOrientation() {
    return orientation;
  }
}

