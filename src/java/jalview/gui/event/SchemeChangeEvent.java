package jalview.gui.event;

import jalview.gui.schemes.*;

import java.util.EventObject;


public class SchemeChangeEvent extends EventObject {

  ColourScheme scheme;

  public SchemeChangeEvent(Object source, ColourScheme scheme) {
    super(source);
    this.scheme = scheme;
  }

  public ColourScheme getScheme() {
    return scheme;
  }

  public Object getSource() {
    return source;
  }
}
