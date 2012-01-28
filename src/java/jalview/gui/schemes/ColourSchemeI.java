package jalview.gui.schemes;

import jalview.datamodel.*;
import jalview.gui.*;
import java.awt.*;
import java.util.*;

public interface ColourSchemeI {

  public Vector  getColours(AlignSequenceI s, Vector aa);
  public Color   getColour (AlignSequenceI s, int pos, Vector aa);
  public Vector  getColours(SequenceGroup sg, Vector aa);
  public Color   findColour(AlignSequenceI seq, String s,int j, Vector aa);

  public boolean canThreshold();
  public boolean isUserDefinable();
}
