package apollo.gui.evidencepanel;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JSplitPane;

import apollo.gui.ApolloFrame;
import apollo.config.Config;
import apollo.gui.Orientations;
import apollo.gui.synteny.SyntenyPanel;

/** EvidencePanelOrientationManager class deals with orientation of evidence 
    panel(s). Also contains Action for orientation menu item. 
    ApolloFrame helper class. I made this a singleton but im wondering if a helper
    class should really be a singleton except that the class it helps is also a 
    singleton. */
public class EvidencePanelOrientationManager {

  private int orientation = Orientations.HORIZONTAL;
  private OrientAction action; // inner class
  private ApolloFrame apolloFrame;
  private static EvidencePanelOrientationManager singleton;

  public static EvidencePanelOrientationManager getSingleton() {
    if (singleton == null)
      singleton = new EvidencePanelOrientationManager();
    return singleton;
  }

  private EvidencePanelOrientationManager() {
    this.apolloFrame = ApolloFrame.getApolloFrame();
    setOrientationFromString(Config.getFrameOrientation());
    action = new OrientAction(getOrientationMenuString());
    action.setEnabled(true);
  }

  private EvidencePanelContainer getEvidencePanelContainer() {
    return EvidencePanelContainer.getSingleton();
  }

  private JSplitPane getSplitPane() {
    return apolloFrame.getSplitPane();
  }

  private SyntenyPanel getSyntenyPanel() {
    return apolloFrame.getSyntenyPanel(); 
  }

  private int getPreferredFrameHeight() {
    return apolloFrame.getPreferredFrameHeight();
  }

  /** orientation string must be "vertical" or "horizontal" 
      (from apollo.cfg) */
  private void setOrientationFromString(String orientString) {
    if (orientString.equalsIgnoreCase("vertical")) {
      orientation = Orientations.VERTICAL;
    }
    else if (orientString.equalsIgnoreCase("horizontal")) {
      orientation = Orientations.HORIZONTAL;
    }
  }

  public Action getAction() {
    return action;
  }

  public void orientEvidencePanels(String orientString) {
    setOrientationFromString(orientString);
    orientEvidencePanels();
  }

  /** For Orientation change or initialization - redo layout */
  public void orientEvidencePanels() {
    if (orientation == Orientations.HORIZONTAL) {
      setHorizontalLayout();
    } else if (orientation == Orientations.VERTICAL) {
      setVerticalLayout();
    }
    // change name of menu item
    getAction().putValue(Action.NAME,getOrientationMenuString());
  }

  private void setVerticalLayout() {
    // have to do this because top(szap) becomes left otherwise
    getSplitPane().remove(getSyntenyPanel());
    getSplitPane().remove(getEvidencePanelContainer());
    getSplitPane().setOrientation(JSplitPane.HORIZONTAL_SPLIT);
    getSplitPane().setLeftComponent(getEvidencePanelContainer());
    getSplitPane().setRightComponent(getSyntenyPanel());
      
    //    getSplitPane().setDividerLocation(250); // doesn't seem to have any effect
      
    getEvidencePanelContainer().setOrientation(Orientations.VERTICAL);
  }

  private void setHorizontalLayout() {
    getSplitPane().remove(getSyntenyPanel());
    getSplitPane().remove(getEvidencePanelContainer());
    getSplitPane().remove(getSyntenyPanel());
    getSplitPane().setOrientation(JSplitPane.VERTICAL_SPLIT);
    getSplitPane().setTopComponent(getSyntenyPanel());
    getSplitPane().setBottomComponent(getEvidencePanelContainer());
    //    getSplitPane().setDividerLocation(getPreferredFrameHeight() - 250);
    int evidencePanelHeight = Config.getStyle().getEvidencePanelHeight();
    getSplitPane().setDividerLocation(getPreferredFrameHeight() - evidencePanelHeight);
      
    getEvidencePanelContainer().setOrientation(Orientations.HORIZONTAL);
  }

  private String getOrientationMenuString() {
    if (orientation == Orientations.HORIZONTAL) {
      return "Make evidence panel vertical";
    } else {
      return "Make evidence panel horizontal";
    }
  }

  /** inner class Action for orientation menu item */
  private class OrientAction extends AbstractAction {
    public OrientAction(String name)  {
      super(name);
    }
    /** Flips the orientation */
    public void actionPerformed(ActionEvent evt) {
      if (orientation == Orientations.HORIZONTAL) {
        orientation = Orientations.VERTICAL;
      } else {
        orientation = Orientations.HORIZONTAL;
      }
      orientEvidencePanels();
    }
  }
} 
