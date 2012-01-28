package apollo.gui.menus;

import apollo.config.FeatureProperty;
import apollo.config.TierProperty;
import apollo.gui.*;
import apollo.datamodel.*;
import apollo.dataadapter.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

public class PropertyMenu extends JPopupMenu implements ActionListener {
  PropertyPanel type_panel;
  TierProperty tier_prop;
  Hashtable   color_items = new Hashtable();
  JMenuItem   collapseTier;
  Point pos;

  ApolloDataAdapterI adapter;

  public PropertyMenu(PropertyPanel pp,
                      TierProperty tp,
                      Point pos) {
    super("Pick colors");
    this.type_panel   = pp;
    this.tier_prop = tp;
    this.pos  = pos;

    menuInit();
  }

  public void menuInit() {
    Vector fpVect = tier_prop.getFeatureProperties();
    for (int i = 0; i < fpVect.size(); i++) {
      FeatureProperty fp = (FeatureProperty) fpVect.elementAt(i);
      JMenuItem item = new JMenuItem(fp.getDisplayType());
      item.setBackground(fp.getColour());

      add(item);
      item.setEnabled(true);
      color_items.put (item, fp);
      item.addActionListener(this);
    }
  }

  public void actionPerformed(ActionEvent e) {
    if (e.getSource() instanceof JMenuItem) {
      JMenuItem item = (JMenuItem) e.getSource();
      FeatureProperty fp = (FeatureProperty) color_items.get (item);

      Color color = JColorChooser.showDialog(this,
                                             "Choose color for " + fp.getDisplayType() + " features",
                                             fp.getColour());
      if (color != null) {
        fp.setColour(color);
        item.setBackground(color);
      }
    }
  }

}
