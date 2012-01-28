package jalview.gui.schemes;

import java.util.*;

public class ColourSchemePropertyVector {
  Vector schemeProps = new Vector();
  Vector schemeDescs = new Vector();

  public void add(ColourSchemeProperty prop) {
    schemeProps.addElement(prop);
    schemeDescs.addElement(prop.getDescription());
  }
  public Vector getColourSchemeNames() {
    return schemeDescs;
  }
  public String getClassName(int ind) {
    return get(ind).getClassName();
  }
  public String getMenuString(int ind) {
    return get(ind).getMenuString();
  }
  public String getSchemeName(int ind) {
    return get(ind).getDescription();
  }
  public boolean contains(String description) {
    return schemeDescs.contains(description);
  }
  public int indexOf(String description) {
    return schemeDescs.indexOf(description);
  }
  public ColourSchemeProperty get(int index) {
    return (ColourSchemeProperty)schemeProps.elementAt(index);
  }

  public int indexOfClass(ColourSchemeI cs) {
    if (cs != null) {
      String className = cs.getClass().getName();
      for (int i=0; i<schemeProps.size(); i++) {
        if (get(i).getClassName().equals(className)) {
          return i;
        }
      }
    }
    return -1;
  }
}
