package apollo.gui;

import java.util.*;

/**
 * A class holding selection type information - whether the selection was made at
 * the feature level, the feature set level (eg by clicking on an intron) or some
 * other way.
 */
public class SelectionType {
  public static final int FEATURE = 1;
  public static final int SET     = 2;
  public static final int NONE    = -1;

  int type;


  public SelectionType() {
    type = NONE;
  }

  public SelectionType(int type) {
    this.type = type;
  }

  public void setType(int type) {
    this.type = type;
  }

  public int getType() {
    return type;
  }

  public String toString() {
    return new String("Selection type = " + type);
  }
}
