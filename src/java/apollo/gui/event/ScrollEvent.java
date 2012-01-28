package apollo.gui.event;

import apollo.datamodel.*;
import java.util.EventObject;
import java.awt.event.*;

/**
 * Introduced to propagate join-scroll events to both panels: I use the
 * value transmitted as a relative-shift amount in terms of pixels.
**/
public class ScrollEvent extends EventObject {

  double value;
  Object source;
  
  public ScrollEvent(Object source, double value){
    super(source);
    this.source = source;
    this.value = value;
  }//end ZoomEvent

  public Object getSource() {
    return source;
  }//end getSource

  public double getValue(){
    return value;
  }
  
  public void setSource(Object source){
    this.source = source;
  }
}