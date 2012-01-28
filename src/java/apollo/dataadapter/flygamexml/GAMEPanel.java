package apollo.dataadapter.flygamexml;

import java.awt.Color;
import java.util.Vector;

import apollo.config.Config;
import apollo.config.Style;
import apollo.dataadapter.DataInputType;
import apollo.dataadapter.GuiTabPanel;
import apollo.dataadapter.gamexml.GAMEAdapter;
import apollo.util.GuiUtil;

/**
 * This is only used by FlyGAMEAdapterGUI. It was an inner class of it, but
 * it seems to have outgrown innerclassness, so I broke it out into its own
 * class
 * GAMEPanel class represents a panel within the JTabbedPane
 * For now this is hardwired with one JComboBox to get input from.
 * Change later if have different kinds of panels.
 */
public class GAMEPanel extends GuiTabPanel {

  public GAMEPanel(String nm,String label, Color bkgnd) {
    super(nm,label,bkgnd);
  }
  
  public GAMEPanel(String nm,String label,DataInputType type, Color bkgnd) {
    super(nm,label,type,bkgnd);
  }

  public GAMEPanel(String nm,String label,DataInputType type,String example,
                   Color bkgnd) {
    super(nm, label, type, example, bkgnd);
  }

  protected Style getAdapterStyle() {
    // Cant think of another way to do this - dont have a hold of game adapter
    // yet at init time
    return Config.getStyle(GAMEAdapter.class.getName());
  }

  /** Overrides default getDatabaseList in guiTabPanel.
   *  Tell Style which type of query we want the database list for. */
  protected Vector getDatabaseList() {
    Vector dblist = null;
    Style style = getAdapterStyle();
    if (style != null)
      dblist =style.getDatabaseList(getName());
    if (dblist == null)
      dblist = new Vector();
    return dblist;
  }

}

