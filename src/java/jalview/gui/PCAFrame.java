package jalview.gui;

import java.awt.*;
import java.awt.event.*;

public class PCAFrame extends Frame {
  Object parent;

  public PCAFrame(String title,Object parent) {
    super(title);
    this.parent = parent;
    addWindowListener(new PCAWindowListener());
  }

  class PCAWindowListener extends WindowAdapter {
    public void windowClosing(WindowEvent evt) {
      if (parent != null || !AlignFrame.exitOnClose()) {
        PCAFrame.this.hide();
        PCAFrame.this.dispose();
      } else if (parent == null) {
        System.exit(0);
      }
    }
  }
}
