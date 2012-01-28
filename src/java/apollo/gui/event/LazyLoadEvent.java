package apollo.gui.event;

import java.util.EventObject;
import apollo.datamodel.*;

/**
 */

public class LazyLoadEvent extends EventObject {

  public static final int SEQUENCE     = 1;

  public static final int BEFORE_LOAD  = 10;
  public static final int AFTER_LOAD   = 11;

  int    type;
  int    dataType;
  String id;

  /**
   *
   */
  public LazyLoadEvent(Object  source,
                       String  id,
                       int     type,
                       int     dataType) {

    super(source);

    this.type = type;
    this.dataType = dataType;
    this.id = id;
  }

  public int getType() {
    return type;
  }

  public int getDataType() {
    return dataType;
  }
  public String getID() {
    return id;
  }
}
