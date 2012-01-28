package apollo.gui.menus;

import apollo.gui.*;
import apollo.gui.drawable.DrawableAnnotationConstants;
import apollo.gui.genomemap.StrandedZoomableApolloPanel;
import apollo.gui.synteny.CurationManager;
import apollo.gui.synteny.GuiCurationState;
import apollo.config.PropertyScheme;
import apollo.datamodel.*;
import apollo.dataadapter.*;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** This is the tiers menu in ApolloFrames menubar */
public class TiersMenu extends JMenu implements ActionListener,
  DrawableAnnotationConstants {

  JMenuItem            collapse;
  JMenuItem            expand;
  JMenuItem            showAllTiers;
  JMenuItem            hideAllTiers;
  JMenuItem            invertTiers;
  JMenuItem            shrinkTiers;
  JMenuItem            growTiers;
  // Making this public so TypePanel can change its state when it kills Types window.
  // Should do this more cleanly.
  public static JCheckBoxMenuItem    types;

  //ApolloFrame          frame;

  public TiersMenu() {//ApolloFrame frame) {
    super("Tiers");
    getPopupMenu().addPropertyChangeListener(new PopupPropertyListener());
    menuInit();
  }

  public void menuInit() {
    types             = new JCheckBoxMenuItem("Show types panel");
    collapse          = new JMenuItem("Collapse all tiers");
    expand            = new JMenuItem("Expand all tiers");
    showAllTiers      = new JMenuItem("Show all tiers");
    hideAllTiers      = new JMenuItem("Hide all tiers");
    invertTiers       = new JMenuItem("Invert tier order");
    shrinkTiers       = new JMenuItem("Decrease tier height (-)");
    growTiers         = new JMenuItem("Increase tier height (+)");

    types            .setState(getTypePanel().isVisible());
    getTypePanel().addWindowListener(new ItemWindowListener(types));
    add(types);
    addSeparator();

    add(collapse);
    add(expand);
    add(showAllTiers);
    add(hideAllTiers);
    add(invertTiers);
    addSeparator();
    add(shrinkTiers);
    add(growTiers);

    types            .addActionListener(this);
    collapse         .addActionListener(this);
    expand           .addActionListener(this);
    showAllTiers     .addActionListener(this);
    hideAllTiers     .addActionListener(this);
    invertTiers     .addActionListener(this);
    shrinkTiers      .addActionListener(this);
    growTiers        .addActionListener(this);

    collapse.setMnemonic('C');
    expand  .setMnemonic('E');
    shrinkTiers.setMnemonic('-');
    growTiers.setMnemonic('+');

    collapse.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                            ActionEvent.CTRL_MASK));

    expand  .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
                            ActionEvent.CTRL_MASK));
  }

  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == collapse) {
      //getSZAP().expandAllTiers(false);
      getActivePropScheme().expandAllTiers(false);
    } else if (e.getSource() == expand) {
      getActivePropScheme().expandAllTiers(true);
    } else if (e.getSource() == showAllTiers) {
      getActivePropScheme().setAllTiersVisible(true);
    } else if (e.getSource() == hideAllTiers) {
      getActivePropScheme().setAllTiersVisible(false);
    } else if (e.getSource() == invertTiers) {
      getSZAP().changeYOrientation();
    } else if (e.getSource() == shrinkTiers) {
      // System.out.println("Reducing yspace");
      getSZAP().getApolloPanel().changeTierHeights(-1);
    } else if (e.getSource() == growTiers) {
      // System.out.println("Increasing yspace");
      getSZAP().getApolloPanel().changeTierHeights(1);
    } else if (e.getSource() == types) {
      //JFrame typesFrame = frame.getTypesPanel().getFrame();
      //typesFrame.setVisible(types.getState());
      getTypePanel().setVisible(types.getState());
      // Fixes Linux-specific bug where Types panel wouldn't come back after you closed it
    }
  }

  private StrandedZoomableApolloPanel getSZAP() {
    return getActiveCurState().getSZAP();
  }
  private PropertyScheme getActivePropScheme() {
    return getActiveCurState().getPropertyScheme();
  }
  private GuiCurationState getActiveCurState() { 
    return CurationManager.getCurationManager().getActiveCurState();
  }

  private TypePanel getTypePanel() { 
    return TypePanel.getTypePanelInstance();
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
