package apollo.datamodel.seq;

import apollo.datamodel.SequenceI;

public interface LazySequenceI extends SequenceI {

  public String getResiduesFromSource(int low, int high);

}











