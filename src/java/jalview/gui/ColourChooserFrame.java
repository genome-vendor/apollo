package jalview.gui;

import java.awt.*;
import java.awt.event.*;

public class ColourChooserFrame extends Frame {


  ColourChooserPanel ccp;

  public ColourChooserFrame(AlignFrame af, AlignViewport av, Controller controller,Color[] color) {
    super("User colours");
    ccp = new ColourChooserPanel(af,av,controller,color);
    setLayout(new BorderLayout());
    add("Center",ccp);
    resize(ccp.getPreferredSize().width,ccp.getPreferredSize().height);
    addWindowListener(new CCFWindowListener());
  }

  class CCFWindowListener extends WindowAdapter {
    public void windowClosing(WindowEvent evt) {
      ColourChooserFrame.this.hide();
      ColourChooserFrame.this.dispose();
    }
  }
}
