package jalview.gui.menus;

import java.util.*;

public class JalCommandVector {
  Vector commands = new Vector();

  public void add(JalCommand com) {
    commands.addElement(com);
  }

  public void addElement(JalCommand com) {
    add(com);
  }

  public JalCommand get(int i) {
    return (JalCommand)commands.elementAt(i);
  }

  public JalCommand commandAt(int i) {
    return get(i);
  }

  public int size() {
    return commands.size();
  }

  public boolean contains(JalCommand com) {
    return commands.contains(com);
  }
}
