package apollo.gui.menus;

import apollo.gui.*;
import apollo.gui.genomemap.ApolloPanel;
import apollo.gui.genomemap.GraphView;
import apollo.util.*;
import apollo.datamodel.*;
import apollo.dataadapter.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.net.*;

public class GraphMenu extends JPopupMenu implements ActionListener {

  GraphView view;
  ApolloPanel ap;
  JMenuItem   colour;
  JMenuItem   windowSize;
  Point pos;

  public GraphMenu(ApolloPanel ap,GraphView view, Point pos) {
    super("Graph operations");
    this.ap   = ap;
    this.view = view;
    this.pos  = pos;

    menuInit();
  }

  public void menuInit() {
    //    ApolloFrame win = (ApolloFrame)
    //                      SwingUtilities.windowForComponent(ap);
    colour          = new JMenuItem("Plot colour ...");
    windowSize      = new JMenuItem("Plot window length ...");

    add(colour);
    add(windowSize);

    if (!(view.getScoreCalculator() instanceof WindowScoreCalculator)) {
      windowSize.setEnabled(false);
    }

    colour.addActionListener(this);
    windowSize.addActionListener(this);
  }


  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == colour) {

      Color colour = JColorChooser.showDialog(this,
                                              "Change plot colour",
                                              view.getPlotColour());
      if (colour != null) {
        view.setPlotColour(colour);
      }
      ap.repaint();
    } else if (e.getSource() == windowSize) {
      WindowScoreCalculator wsc = (WindowScoreCalculator)view.getScoreCalculator();
      //      System.out.println("wsc win size = " + wsc.getWinSize());
      new SliderWindow("Change plot window length","Window",wsc.getModel(),ap.getLocationOnScreen());
    }
  }
}
