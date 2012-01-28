package jalview.gui.event;

import java.util.EventObject;

public class StatusEvent extends EventObject {

  public static final int INFO    = 1;
  public static final int WARNING = 2;
  public static final int ERROR   = 3;

  String text;
  int    type;

  public StatusEvent(Object source, String text, int type) {
    super(source);
    this.text = text;
    this.type = type;
  }

  public Object getSource() {
    return source;
  }

  public String getText() {
    return text;
  }
 
  public int getType() {
    return type;
  }
}
