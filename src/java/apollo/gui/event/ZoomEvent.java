package apollo.gui.event;

import java.util.EventObject;

public class ZoomEvent extends EventObject {

  //
  //Should this zoom event be propagated by any capable listener to other
  //controllers?
  private boolean propagated = false;
  String  zoomFactor = null;
  double  factorMultiplier = 0.0;
  
  public ZoomEvent(Object source) {
    super(source);
  }//end ZoomEvent

  public ZoomEvent(Object source, boolean propagate, String factor){
    super(source);
    propagated = propagate;
    zoomFactor = factor;
  }//end ZoomEvent

  public ZoomEvent(Object source, boolean propagate, String factor, double multiplier){
    super(source);
    propagated = propagate;
    zoomFactor = factor;
    factorMultiplier = multiplier;
  }//end ZoomEvent

  public Object getSource() {
    return source;
  }//end getSource
  
  public boolean isPropagated(){
    return propagated;
  }//end isPropagate
  
  public String getZoomFactor(){
    return zoomFactor;
  }//end getZoomFactor

  public double getFactorMultiplier(){
    return factorMultiplier;
  }//end getFactorMultiplier
}
