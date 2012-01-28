package jalview.gui.schemes;

import java.lang.reflect.*;
import java.util.*;

public class ColourSchemeFactory {

  public static ColourSchemeI get(int index) {
    try {
      String name = ColourProperties.getClassName(index);
      Class  c    = Class.forName(name);
    
      return (ColourSchemeI)c.newInstance();
    } catch (Exception e) {
      System.err.println(e);
      return null;
    }
  }

  public static ColourSchemeI get(String scheme) {
    return get(ColourProperties.indexOf(scheme));
  }

  public static int get(ColourSchemeI cs) {
    return ColourProperties.indexOfClass(cs);
  }
}
