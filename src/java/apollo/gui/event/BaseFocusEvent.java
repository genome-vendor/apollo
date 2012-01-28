package apollo.gui.event;

import apollo.datamodel.*;
import apollo.dataadapter.Region;
import java.util.EventObject;

public class BaseFocusEvent extends EventObject {
  private int        focusPosition;
  private SeqFeatureI feature;
  /** base focus can either be for a position (single base to center on - scroll) or a
   range (set view to range - zoom & scroll). If this is too much to load onto
   base focus event, then a new range event could be made */
  private boolean isPosition = true;
  private int rangeStart;
  private int rangeEnd;

  /** Constructor for position focus event, request to center on focus */
  public BaseFocusEvent(Object source,int focus,SeqFeatureI feature) {
    super(source);
    this.focusPosition = focus;
    this.feature = feature;
    isPosition = true;
  }

//   /** Constructor range event, request to display range */
//   public BaseFocusEvent(Object source, int start, int end) {
//     super(source);
//     rangeStart = start;
//     rangeEnd = end;
//     isPosition = false;
//     // focusPosition = (start + end) / 2; ???
//   }
  public BaseFocusEvent(Object source, Region region) {
    super(source);
    rangeStart = region.getStart();
    rangeEnd = region.getEnd();
    isPosition = false;
    // focusPosition = (start + end) / 2; ???
  }

  public int getFocus() {
    return focusPosition;
  }
  public SeqFeatureI getFeature() {
    return feature;
  }
  
  /** base focus can either be for a position (single base to center on - scroll) or a
   range (set view to range - zoom & scroll). If this is too much to load onto
   base focus event, then a new range event could be made */
  public boolean isPosition() { return isPosition; }

  public int getRangeStart() { return rangeStart; }
  public int getRangeEnd() { return rangeEnd; }
  public int[] getRangeLimits() {
    return new int[] { rangeStart, rangeEnd };
  }

//public Object getSource() {return source; } already in EventObject
}


