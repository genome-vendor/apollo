package apollo.gui.detailviewers.sequencealigner;

import apollo.datamodel.CurationSet;
import apollo.datamodel.Range;
import apollo.datamodel.RangeI;
import apollo.datamodel.Sequence;
import apollo.datamodel.SequenceI;

public interface ReferenceFactoryI {

  public abstract SequenceI getReference(Strand s);
  
  public abstract SequenceI getReference(Strand s, ReadingFrame rf);
  
}
