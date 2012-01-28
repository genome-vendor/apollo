package apollo.editor;

import java.util.EventObject;
import apollo.datamodel.*;

/**
 * A controller managed event class which signals when a change is made to
 * a set of features and what type of change occurred. Objects interested
 * in listening for these event should implement the FeatureChangeListener
 * interface and register with the controller.
 */

public class ResultChangeEvent extends FeatureChangeEvent {

  public static final int            RESULTSPAN   = 16;
  public static final int            RESULTSET    = 17;
  public static final int            ANALYSIS     = 18;

  private     int            objectClass=0;

  /**
   *
   */
  public ResultChangeEvent(Object      source,
                           SeqFeatureI changeTop,
                           int         operation,
                           int         objectClass,
                           SeqFeatureI feature1,
                           SeqFeatureI feature2) {
    super(source,changeTop,operation,feature1,feature2);
    this.objectClass = objectClass;
  }

  /** can object class be figured automatically from feature type? this works
      for annots */
  public int getObjectClass() {
    return objectClass;
  }

  /** this is only used by FeatureChangeEvent.toString */
  String getObjectClassAsString() {
    // NOTE: No breaks
    switch (getObjectClass()) {
    case RESULTSPAN:
      return new String("RESULTSPAN");
    case RESULTSET:
      return new String("RESULTSET");
    case ANALYSIS:
      return new String("ANALYSIS");
    default:
      return new String("!!!UNKNOWN!!!");
    }
  }
}
