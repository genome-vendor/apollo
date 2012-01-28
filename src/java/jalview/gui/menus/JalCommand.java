package jalview.gui.menus;

import java.util.*;
import java.io.*;

public class JalCommand {
  Hashtable args;
  String    command;

  public JalCommand(String command, String args) {
    this.command = new String(command);
    StreamTokenizer tokenizer = new StreamTokenizer(new BufferedReader(new StringReader(args)));
 
    this.args = new Hashtable();
    try {
      boolean EOF     =  false;
      int     tokType =  0;
      Vector  words   =  new Vector();
      while (!EOF) {
        if ((tokType = tokenizer.nextToken()) == StreamTokenizer.TT_EOF){
          EOF = true;
        } else {
          if (tokenizer.sval != null) {
            words.addElement(tokenizer.sval);
          }
        }
      }
      if (words.size()%2 != 0) {
        System.err.println("Error: Odd number of tokens while parsing args");
        return;
      }
      for (int i=0; i<words.size(); i+=2) {
        this.args.put(words.elementAt(i),words.elementAt(i+1));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public JalCommand(String command, Hashtable args) {
    this.command = new String(command);
    this.args    = args;
  }

  public Hashtable getArgs() {
    return args;
  }
  public String getCommandString() {
    return command;
  }
}
