package apollo.gui;

import apollo.datamodel.*;
import apollo.config.FeatureProperty;
import apollo.config.TierProperty;
import apollo.gui.genomemap.ApolloPanel;
import apollo.gui.event.*;
import apollo.gui.menus.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.Vector;

/**
 * A panel which renders a configuration tab dialog for a FeatureProperty
 */
public class FeaturePropertyPanel extends JPanel
    //  implements ActionListener,
  //MouseInputListener {
  {
  JPanel       jp;
  JFrame       frame;
  ApolloPanel  ap;
  FeatureProperty featureProperty;

  public JFrame getFrame() {
    return frame;
  }

  public FeaturePropertyPanel(FeatureProperty featureProperty, ApolloPanel ap) {
    super(new GridLayout(1, 1));

    this.ap       = ap;
    this.featureProperty       = featureProperty;

    JTabbedPane tabbedPane = new JTabbedPane();
      
    JComponent panel1 = new GlyphChooser(featureProperty);
    tabbedPane.addTab("Glyph", null, panel1,
              "Sets glyph for drawing features");
    tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);
      
    JComponent panel2 = new TableColumnChooser(featureProperty);
    tabbedPane.addTab("Table columns", null, panel2,
              "Modify table column display and ordering");
    tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);
      
    //Add the tabbed pane to this panel.
    add(tabbedPane);
      
    //The following line enables to use scrolling tabs.
    tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

    frame = new JFrame("Feature Properties for " + featureProperty.getDisplayType());

    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(this,BorderLayout.CENTER);
    frame.pack();

  }
  
  protected JComponent makeTextPanel(String text) {
    JPanel panel = new JPanel(false);
    JLabel filler = new JLabel(text);
    filler.setHorizontalAlignment(JLabel.CENTER);
    panel.setLayout(new GridLayout(1, 1));
    panel.add(filler);
    return panel;
  }

  public FeatureProperty getProperty() {
    return this.featureProperty;
  }

}
