package jalview.gui.menus;

import java.io.*;
import java.util.*;

public class TextCommandAdapter extends CommandAdapter {
  String filename;

  public TextCommandAdapter(String filename) {
    this.filename = filename;
  }
  public void writeLog(CommandLog log) {
  }
  public JalCommandVector getCommands() {
    return null;
  }
  public String getType() {
    return "Text command adapter";
  }
  public String getName() {
    return "Text command adapter";
  }
}
