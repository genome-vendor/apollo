package apollo.gui.menus;

import apollo.config.Config;
import apollo.gui.*;
import apollo.gui.genomemap.FeatureView;
import apollo.gui.genomemap.FeatureTierManagerI;
import apollo.gui.genomemap.ViewI;
import apollo.config.PropertyScheme;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

public class ShowMenu extends JMenu implements ActionListener {

  ViewI        view;
  JMenuItem [] typeItems;

  public ShowMenu(ViewI view,Point pos) {
    super("Show hidden tier");
    this.view = view;
    menuInit();
  }

  private FeatureTierManagerI getViewManager() {
    if (view instanceof FeatureView) {
      FeatureView fv = (FeatureView)view;
      TierManagerI tm = fv.getTierManager();

      if (tm instanceof FeatureTierManagerI) {
        return (FeatureTierManagerI)tm;
      }
    }
    System.out.println("ERROR: No FFTM in ShowMenu");
    return null;
  }

  public void menuInit() {
    FeatureTierManagerI fftm = getViewManager();

    setEnabled(false);
    if (fftm != null) {
      Vector types = fftm.getHiddenTiers();

      if (types.size() != 0) {
        setEnabled(true);
        typeItems = new JMenuItem[types.size()];
        PropertyScheme scheme = Config.getPropertyScheme();
        for (int i=0; i<types.size(); i++) {
          String type = (String)types.elementAt(i);
          typeItems[i] = new JMenuItem(type);
          add(typeItems[i]);
          typeItems[i].setBackground(scheme.getTierProperty(type).getColour());
          typeItems[i].addActionListener(this);
        }
      }
    }
  }

  public void actionPerformed(ActionEvent e) {
    FeatureTierManagerI fftm = getViewManager();
    if (fftm != null) {

      for (int i=0; i<typeItems.length; i++) {
        if (e.getSource() == typeItems[i]) {
          fftm.setVisible(e.getActionCommand(),true);
        }
      }
      view.setInvalidity(true);
      view.getComponent().repaint(view.getBounds().x,
                                  view.getBounds().y,
                                  view.getBounds().width,
                                  view.getBounds().height);
    }
  }
}
