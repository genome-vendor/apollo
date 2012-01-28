package jalview.gui;

import java.awt.*;
import java.util.*;

public class ColourChooserPanelDriver extends Driver {

  public static void main(String[] args) {
    Frame f = new Frame();

//MG    ColourChooserPanel cp = new ColourChooserPanel(null,ResidueProperties.color);
    f.setLayout(new BorderLayout());
//MG    f.add("Center",cp);
//MG    f.resize(cp.getPreferredSize().width,cp.getPreferredSize().height);
    f.show();
  }
}
