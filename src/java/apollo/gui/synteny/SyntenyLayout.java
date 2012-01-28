package apollo.gui.synteny;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

public class SyntenyLayout implements LayoutManager {

  public SyntenyLayout() {}

  public void layoutContainer(Container target) {
    if (target instanceof SyntenyPanel) {
      SyntenyPanel sp = (SyntenyPanel)target;

      Vector panels     = sp.getPanels();
      Vector linkPanels = sp.getLinkPanels();

      // Should check that we have the right number of
      // linkPanels to panels.
      int linkHeight       = 50;

      int totalHeight      = target.getSize().height;
      int totalWidth       = target.getSize().width;

      int totalPanelHeight = totalHeight - linkPanels.size() * linkHeight;
      int panelHeight      = totalPanelHeight/panels.size();

      int currenty = 0;

      for (int i=0; i < panels.size(); i++) {
        JPanel jp = (JPanel)panels.elementAt(i);

        jp.setBounds(0,currenty,totalWidth,panelHeight);

        currenty += panelHeight;

        if (linkPanels.size() > i) {
          SyntenyLinkPanel lp = (SyntenyLinkPanel)linkPanels.elementAt(i);

          lp.setBounds(0,currenty,totalWidth,linkHeight);
        }
      }
    }
  }

  public Dimension preferredLayoutSize(Container c) {
    return c.getPreferredSize();
  }

  public Dimension minimumLayoutSize(Container c) {
    return c.getMinimumSize();
  }

  public void addLayoutComponent(String str, Component c) {}

  public void removeLayoutComponent(Component c) {}
}







