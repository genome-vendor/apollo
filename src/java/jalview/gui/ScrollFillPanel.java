package jalview.gui;

import java.awt.*;
import java.util.*;
import javax.swing.*;

import jalview.gui.event.*;

public class ScrollFillPanel extends JPanel {
  JScrollBar sb;

  public ScrollFillPanel(JScrollBar sb) {
    setBackground(Color.white);
    this.sb = sb;
  }

  public ScrollFillPanel(JScrollBar sb,Color colour) {
    setBackground(colour);
    this.sb = sb;
  }

  public Dimension minimumSize() {
    if (sb.getOrientation() == JScrollBar.VERTICAL) {
      return new Dimension(sb.getSize().width,0);
    } else {
      return new Dimension(0,sb.getSize().height);
    }
  }
 
  public Dimension preferredSize() {
    return minimumSize();
  }
}
