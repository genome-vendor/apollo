package apollo.gui.menus;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

import apollo.config.FeatureProperty;
import apollo.config.TierProperty;
import apollo.gui.*;
import apollo.datamodel.*;
import apollo.dataadapter.*;

public class ThresholdMenu extends JPopupMenu implements ActionListener {
  PropertyPanel type_panel;
  TierProperty tier_prop;
  Hashtable   threshItems = new Hashtable();
  JMenuItem   collapseTier;
  //  Point pos;  // not used

  ApolloDataAdapterI adapter;

  public ThresholdMenu(PropertyPanel pp,
                       TierProperty tp,
                       Point pos) {
    super("Pick colors");  // ?
    this.type_panel   = pp;
    this.tier_prop = tp;
    //    this.pos  = pos;

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
      threshItems.put (item, fp);
      item.addActionListener(this);
    }
  }

  public void actionPerformed(ActionEvent e) {
    if (e.getSource() instanceof JMenuItem) {
      JMenuItem item = (JMenuItem) e.getSource();
      FeatureProperty fp = (FeatureProperty) threshItems.get (item);

      DefaultBoundedRangeModel model = 
	new DefaultBoundedRangeModel(fp.getThreshold() < 0.0 ? (int)fp.getMinScore() : (int)fp.getThreshold(),
				     0, 
				     (int)fp.getMinScore(),
				     (int)fp.getMaxScore());
      new SliderWindow("Set threshold (" + fp.getDisplayType() + " type)","Threshold for type",model,type_panel.getLocationOnScreen());
      model.addChangeListener(new ThresholdListener(fp));

    }
  }

  class ThresholdListener implements ChangeListener {
    FeatureProperty fp;
    public ThresholdListener(FeatureProperty fp) {
      this.fp = fp;
    }

    public void stateChanged(ChangeEvent evt) {
      if (evt.getSource() instanceof DefaultBoundedRangeModel) {
        fp.setThreshold((float)((DefaultBoundedRangeModel)evt.getSource()).getValue());
      }
    }
  }
}
