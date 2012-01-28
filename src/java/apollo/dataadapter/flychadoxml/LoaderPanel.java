package apollo.dataadapter.flychadoxml;

import java.awt.Color;
import apollo.dataadapter.DataInputType;
import apollo.dataadapter.flygamexml.GAMEPanel;
import apollo.config.Config;
import apollo.config.Style;

public class LoaderPanel extends GAMEPanel {

  public LoaderPanel(String nm,String label, Color bkgnd) {
    super(nm,label,bkgnd);
  }
  
  public LoaderPanel(String nm,String label,DataInputType type, Color bkgnd) {
    super(nm,label,type,bkgnd);
  }

  public LoaderPanel(String nm,String label,DataInputType type,String example,
                   Color bkgnd) {
    super(nm, label, type, example, bkgnd);
  }

  protected Style getAdapterStyle() {
    // Cant think of another way to do this - dont have a hold of adapter
    // yet at init time
    //    System.out.println("LoaderPanel: style = " + FlyChadoXmlAdapter.class.getName()); // DEL
    return Config.getStyle(FlyChadoXmlAdapter.class.getName());
  }
}
