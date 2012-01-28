package apollo.gui.detailviewers.sequencealigner.filters;

import apollo.datamodel.SeqFeatureI;
import apollo.gui.detailviewers.sequencealigner.Strand;

public class FilterStrand implements Filter<SeqFeatureI>{

  private Strand strand;
  
  public FilterStrand(Strand s) {
    strand = s;
  }

  public boolean keep(SeqFeatureI f) {
    return f.getStrand() == strand.toInt();
  }
  
  public String toString() {
    return "Keep " + valueToString();
  }

  public String valueToString() {
    return "Strand " + strand;
  }
  
}
