package apollo.dataadapter.genbank;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import javax.swing.*;

import java.util.Properties;
import java.awt.Color;

import apollo.dataadapter.DataInputType;
import apollo.dataadapter.GuiTabPanel;
import apollo.util.GuiUtil;
import apollo.config.Config;
import apollo.config.Style;


/**
 * This is only used by GenbankAdapterGUI. It was an inner class of it, but
 * it seems to have outgrown innerclassness, so I broke it out into its own
 * class
 * PublicDbPanel class represents a panel within the JTabbedPane
 * For now this is hardwired with one JComboBox to get input from
 * change later if have different kinds of panels 
 */
class PublicDbPanel extends GuiTabPanel {

  PublicDbPanel(String nm,String label, Color bkgnd) {
    super(nm,label,bkgnd);
  }
  
  PublicDbPanel(String nm,String label,DataInputType type, Color bkgnd) {
    super(nm,label,type,bkgnd);
  }

  PublicDbPanel(String nm,String label,DataInputType type,String example,
                Color bkgnd) {
    super(nm, label, type, example, bkgnd);
  }

  public DataInputType getInputType() {
    return DataInputType.URL;
  }

  protected Style getAdapterStyle() {
    // Cant think of another way to do this - dont have a hold of game adapter
    // yet at init time
    return Config.getStyle(GenbankAdapter.class.getName());
  }

  protected Vector getDatabaseList() {
    Hashtable pubDbToURL = Config.getPublicDbList();
    Enumeration keys = pubDbToURL.keys();
    Vector dbs = new Vector(pubDbToURL.size());
    while(keys.hasMoreElements()) {
      dbs.addElement(keys.nextElement());
    }
    return dbs;
  }

}

