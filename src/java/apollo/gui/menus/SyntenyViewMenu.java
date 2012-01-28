package apollo.gui.menus;

import apollo.config.Config;
import apollo.gui.*;
import apollo.gui.drawable.*;
import apollo.gui.genomemap.*;
import apollo.gui.synteny.SyntenyPanel;
import apollo.datamodel.*;
import apollo.dataadapter.*;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** This is no longer used - delete? part of SyntenyMenuManager which used
    to be used by SyntenyLinkMenu but no longer */
public class SyntenyViewMenu extends JMenu implements ActionListener,
  DrawableAnnotationConstants {

  JMenuItem            collapse;
  JMenuItem            expand;
  JMenuItem            zoom;
  JMenuItem            orientation;
  JMenuItem            navigator;
  JMenuItem            invertcolours;
  JCheckBoxMenuItem    reverseComplement;
  JCheckBoxMenuItem    textAvoid;
  JCheckBoxMenuItem    types;
  JCheckBoxMenuItem    gcplot;
  JCheckBoxMenuItem    threeD;
  JCheckBoxMenuItem    outline;
  //static TypePanel typesPanel = null;



  JCheckBoxMenuItem    showEdgeMatches;
  JMenuItem showAllTiers;
  JMenuItem hideAllTiers;

  JFrame   frame;

  SyntenyPanel sp;

  public SyntenyViewMenu(JFrame frame, SyntenyPanel sp) {
    super("View");
    this.frame = frame;
    this.sp = sp;

    //System.out.println("Panel " + sp);
//     if (typesPanel == null) {
//       MultiController mc = new MultiController();
//       for (int i=0;i<sp.getPanels().size();i++) {
//         StrandedZoomableApolloPanel szap = (StrandedZoomableApolloPanel)sp.getPanels().elementAt(i);
//         mc.addController(szap.getController());
//       }
//       //StrandedZoomableApolloPanel szap = (StrandedZoomableApolloPanel)sp.getPanels().elementAt(0);
//       //typesPanel = new TypePanel (szap.getController(), Config.getPropertyScheme());
//       typesPanel = new TypePanel (mc, Config.getPropertyScheme());
//     }
    menuInit();
  }

  public void menuInit() {
    collapse          = new JMenuItem("Collapse all tiers");
    expand            = new JMenuItem("Expand all tiers");
    zoom              = new JMenuItem("Zoom to selected");
    invertcolours     = new JMenuItem("Invert screen colours");
    types             = new JCheckBoxMenuItem("Show types panel");
    gcplot            = new JCheckBoxMenuItem("Show GC plot");
    reverseComplement = new JCheckBoxMenuItem("Reverse complement");
    textAvoid         = new JCheckBoxMenuItem("Avoid text overlaps");
    threeD            = new JCheckBoxMenuItem("Draw 3D rectangles");
    outline           = new JCheckBoxMenuItem("Draw outline rectangles");

    showEdgeMatches   = new JCheckBoxMenuItem("Show edge matches");
    showAllTiers      = new JMenuItem("Show all tiers");
    hideAllTiers      = new JMenuItem("Hide all tiers");

    types            .setState(false);
    gcplot           .setState(false);

    textAvoid        .setState(true);
    threeD           .setState(Config.getDraw3D());
    outline          .setState(Config.getDrawOutline());

    reverseComplement.setState(false);

    showEdgeMatches  .setState(true);

    showEdgeMatches  .setEnabled(true);


    add(reverseComplement);
    addSeparator();
    add(zoom);
    add(types);
    add(showEdgeMatches);
    add(textAvoid);

    add(gcplot);
    addSeparator();
    add(collapse);
    add(expand);
    add(showAllTiers);
    add(hideAllTiers);
    addSeparator();
    add(invertcolours);
    add(threeD);
    add(outline);

    collapse         .addActionListener(this);
    expand           .addActionListener(this);
    zoom             .addActionListener(this);
    types            .addActionListener(this);
    gcplot           .addActionListener(this);
    reverseComplement.addActionListener(this);
    textAvoid        .addActionListener(this);
    showEdgeMatches  .addActionListener(this);
    showAllTiers     .addActionListener(this);
    hideAllTiers     .addActionListener(this);
    invertcolours    .addActionListener(this);
    threeD           .addActionListener(this);
    outline          .addActionListener(this);

    collapse.setMnemonic('C');
    expand  .setMnemonic('E');
    zoom    .setMnemonic('Z');

    collapse.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                            ActionEvent.CTRL_MASK));

    expand  .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
                            ActionEvent.CTRL_MASK));

    zoom    .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                            ActionEvent.CTRL_MASK));

  }

  private TypePanel getTypePanel() {
    return TypePanel.getTypePanelInstance();
  }

  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == zoom) {
      sp.zoomToSelection();
      //    else if (e.getSource() == collapse) {
      //      frame.getOverviewPanel().expandAllTiers(false);
      //    } else if (e.getSource() == expand) {
      //      frame.getOverviewPanel().expandAllTiers(true);
      //    } else if (e.getSource() == reverseComplement) {
      //      frame.getOverviewPanel().setReverseComplement(reverseComplement.getState());
      //    } else if (e.getSource() == textAvoid) {
      //      frame.getOverviewPanel().setTextAvoidance(textAvoid.getState());
      //    } else if (e.getSource() == gcplot) {
      //      frame.getOverviewPanel().setGraphVisibility(gcplot.getState());
    }
    else if (e.getSource() == types) {
      
      //typesPanel.getFrame().setVisible(types.getState());
      getTypePanel().setVisible(types.getState());
      //typesPanel.getFrame().addWindowListener(new ItemWindowListener(types));
      getTypePanel().addWindowListener(new ItemWindowListener(types));
      //    } else if (e.getSource() == showEdgeMatches) {
      //      frame.getOverviewPanel().setEdgeMatching(showEdgeMatches.getState());
      //    } else if (e.getSource() == showAllTiers) {
      //      frame.getOverviewPanel().setAllTiersVisible(true);
      //    } else if (e.getSource() == hideAllTiers) {
      //      frame.getOverviewPanel().setAllTiersVisible(false);
      //    } else if (e.getSource() == invertcolours) {
      //      Color tmp = Config.getCoordForeground();
      //      Config.setFeatureBackground(tmp);
      //      Config.setCoordForeground(Config.getCoordBackground());
      //      Config.setEdgematchColor(Config.getCoordBackground());
      //      Config.setCoordBackground(tmp);
      //      frame.getOverviewPanel().setViewColours();
      //      frame.repaint();
      //    } else if (e.getSource() == threeD) {
      //      Config.setDraw3D(threeD.getState());
      //      frame.repaint();
      //    } else if (e.getSource() == outline) {
      //      Config.setDrawOutline(outline.getState());
      //      frame.repaint();
    }
    else {
      System.out.println("Unhandled menu item");
    }
  }

  class ItemWindowListener extends WindowAdapter {
    JCheckBoxMenuItem item;

    public ItemWindowListener(JCheckBoxMenuItem item) {
      this.item = item;
    }


    public void windowClosing(WindowEvent e) {
      item.setState(false);
      ((Window)e.getSource()).removeWindowListener(this);
    }
  }
}
