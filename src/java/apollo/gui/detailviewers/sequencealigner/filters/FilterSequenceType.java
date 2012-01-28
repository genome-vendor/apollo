package apollo.gui.detailviewers.sequencealigner.filters;

import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;
import apollo.gui.detailviewers.sequencealigner.SequenceType;

public class FilterSequenceType implements Filter<SeqFeatureI> {
  
  private SequenceType sequenceType;
  
  public FilterSequenceType(SequenceType st) {
    sequenceType = st;
  }

  public boolean keep(SeqFeatureI f) {
    SequenceI sequence = f.getFeatureSequence();
    String seqType = sequence.getResidueType();
    String alignment = f.getExplicitAlignment();

    return alignment != null && 
           (sequenceType.toString().equals(seqType) || 
            sequenceType.equals(SequenceType.AA) == isAminoAcid(alignment));
  }

  public String toString() {
    return "Keep " + valueToString();
  }
  
  //TODO:find a way to figure out if the alignment or feature is an aminoAcid...
  private boolean isAminoAcid(String a) {
    return a.contains("E") || a.contains("V") || a.contains("P") || 
    a.contains("M") || a.contains("R") || a.contains("Q") || a.contains("L")
    || a.contains("Y") || a.contains("S") || a.contains("H");
  }

  public String valueToString() {
    return "Sequence Type " + sequenceType;
  }
}
