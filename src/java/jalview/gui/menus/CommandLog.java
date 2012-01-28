package jalview.gui.menus;

import java.awt.event.*;
import java.util.*;

import java.net.*;
import java.util.*;
import java.io.*;

public class CommandLog {
  JalCommandVector commands = new JalCommandVector();

  CommandAdapter adapter;

  String filename;

  public CommandLog(String filename) {
    setFilename(filename);

    adapter = new TextCommandAdapter(filename);
  }

  public void add(String comStr, String args) {
    commands.addElement(new JalCommand(comStr,args));
  }

  public void add(String comStr, Hashtable args) {
    commands.addElement(new JalCommand(comStr,args));
  }
  public void add(JalCommand command) {
    commands.addElement(command);
  }

  public JalCommand get(int i) {
    return commands.commandAt(i);
  }

  public int size() {
    return commands.size();
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public void writeLog() {
    adapter.writeLog(this);
  }

  public void getCommands() { 
    JalCommandVector newcoms = adapter.getCommands();

    for (int i=0; i<newcoms.size(); i++) {
      commands.add(newcoms.commandAt(i));
    }
  }
}
