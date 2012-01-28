package jalview.io;

import jalview.datamodel.*;
import java.util.*;

public interface SequenceFeatureSourceI {
  public AlignSequenceI getSequence();
  public String   getSequenceString();
  public String   getId();
  public Vector   getFeatures();
  public Vector   getPDBCode();
  
}
