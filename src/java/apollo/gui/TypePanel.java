package apollo.gui;

import apollo.config.Config;
import apollo.config.PropertyScheme;
import apollo.config.PropSchemeChangeEvent;
import apollo.config.PropSchemeChangeListener;
import apollo.config.TierProperty;
import apollo.datamodel.*;
import apollo.dataadapter.DataLoadEvent;
import apollo.dataadapter.DataLoadListener;
import apollo.gui.genomemap.ApolloPanel;
import apollo.gui.event.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;

import org.apache.log4j.*;

/**
 * A panel to show and allow edits to FeatureProperties.
 Has the frame that contains itself (a panel), a little confusing if you dont
 realize this.
 */
public class TypePanel extends ControlledPanel implements ControlledObjectI,
  FeatureSelectionListener,
  ActionListener,
  DataLoadListener,
  PropSchemeChangeListener {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(TypePanel.class);

  private static TypePanel typePanelSingleton;

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  PropertyScheme properties;
  Vector       panels;
  ApolloPanel  ap;
  Controller   controller;
  JScrollPane  sp;
  JPanel       mainP;
  boolean      autoRaise = false;
  JCheckBox    autoBox;
  boolean      doneInit = false;
  //JDialog       frame;
  JFrame       frame;
  String       styleFileName;     // Remember so that we'll know if style has changed

  /** for now there is only one types panel for all species, as there is presently
      only one tiers/style for multi species. Sima has requested a type panel
      for each species, so this will need to change when thats implemented. 
      For now this works. */
  public static TypePanel getTypePanelInstance() {
    if (typePanelSingleton == null) {
      // this needs to be redone if we dont have prop scheme yet
      typePanelSingleton = 
        new TypePanel(Controller.getMasterController(),Config.getPropertyScheme());
    }
    return typePanelSingleton;
  }
//   /** I think this is pase? */
//   public TypePanel(ApolloPanel ap, PropertyScheme properties) {
//     this.ap    = ap;
//     //init(ap.getController(),properties);
//     setVisible(false);
//     init(Controller.getMasterController(),properties);
//   }

  private TypePanel(Controller c, PropertyScheme properties) {
    //init(c,properties);
    setVisible(false);
    init(Controller.getMasterController(),properties);
  }

  private void init(Controller c, PropertyScheme properties) {
    this.properties = properties;

    panels = new Vector();

    setController(c);

    jbInit();
    
    // So that we know if style has changed
    styleFileName = Config.getStyle().getFileName();

    
    //frame = new JDialog((JFrame)SwingUtilities.getAncestorOfClass(JFrame.class,ap),"Types");
    frame = new JFrame("Types");
    
    frame.getContentPane().setLayout(new BorderLayout());
    getScrollPane().setPreferredSize(new Dimension(300,400));
    frame.getContentPane().add(this.getScrollPane(),BorderLayout.CENTER);
    //    frame.getContentPane().add(this,BorderLayout.CENTER);
    frame.pack();
  }

  /** Set location of type panel relative to ApolloFrame dimension */
  public void setLocationRelativeToFrame(Dimension frameDim) {
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    // set offset from frame 20,30
    int x = (screenSize.width - frameDim.width) / 2 + 20; 
    int y = (screenSize.height - frameDim.height) / 2 + 30;
    getFrame().setLocation(x,y);
  }

//   public TypePanel(ApolloPanel ap, PropertyScheme properties) {
//     this.ap    = ap;
//     this.properties = properties;

//     panels = new Vector();

//     setController(ap.getController());

//     jbInit();

//     frame = new JFrame/*JDialog*/(/*(JFrame)SwingUtilities.getAncestorOfClass(JFrame.class,ap),*/"Types");
//     frame.getContentPane().setLayout(new BorderLayout());
//     getScrollPane().setPreferredSize(new Dimension(250,400));
//     frame.getContentPane().add(this.getScrollPane(),BorderLayout.CENTER);
//     JLabel instructionLabel = new JLabel("Click middle mouse on a type to see subtypes");
//     instructionLabel.setForeground(Color.black);
//     instructionLabel.setBackground(Color.white);
//     frame.getContentPane().add(instructionLabel,BorderLayout.NORTH);
//     //    frame.getContentPane().add(this,BorderLayout.CENTER);
//     frame.pack();
//   }

  public void setVisible(boolean vis) {
    super.setVisible(vis);
    if (getFrame() == null) {
      return;
    }
    getFrame().setVisible(vis);
    // Fixes Linux-specific bug where Types panel wouldn't come back after you 
    // closed it
    if (!vis) {
      getFrame().removeNotify();
    }
    else if (getFrame().getState()==Frame.ICONIFIED) {
      getFrame().setState(Frame.NORMAL);
    }
  }
  public void addWindowListener(WindowListener wl) {
    getFrame().addWindowListener(wl);
  }

  public /*JDialog*/JFrame getFrame() {
    return this.frame;
  }
  public void setController(Controller c) {
    controller = c;
    controller.addListener(this);
  }

  public Controller getController() {
    return controller;
  }

  // Overide getControllerWindow so that the Controller doesn't get removed
  // when the panel is invisible
  //   public Object getControllerWindow() {
  //      return null;
  //   }

  public boolean needsAutoRemoval() {
    return false;
  }

  public void repaint() {
    if (frame != null && frame.isVisible() && autoRaise)
      frame.toFront();
    super.repaint();
  }

  public boolean handlePropSchemeChangeEvent(PropSchemeChangeEvent evt) {
    logger.debug("handling TCE in TypesPanel");
    for (int i=0; i < panels.size();i++) {
      PropertyPanel   pp = (PropertyPanel)panels.elementAt(i);
      // synchs up with its tier property
      pp.updateValues();
    }
    if (properties.size() > panels.size() && doneInit) {
      logger.debug("types.size != panels.size");

      int oldsize = panels.size();
      Vector tier_properties = properties.getAllTiers();
      for (int i = 0; i < tier_properties.size(); i++) {
        boolean found = false;
        TierProperty tp = (TierProperty) tier_properties.elementAt(i);
        for (int j=0; j<oldsize && !found; j++) {
          PropertyPanel   pp = (PropertyPanel)panels.elementAt(j);
          found = (pp.getProperty() == tp);
        }
        if (!found) {
          addPropertyPanel(tp);
        }
      }
      redraw();

    } else if (orderChanged()) {
      int oldsize = panels.size();
      for (int j=0; j<oldsize; j++) {
        remove((JPanel)panels.elementAt(j));
      }
      panels.removeAllElements();
      Vector tier_properties = properties.getAllTiers();
      for (int i = 0; i < tier_properties.size(); i++) {
        TierProperty tp = (TierProperty) tier_properties.elementAt(i);
        addPropertyPanel(tp);
      }
      redraw();
    }
    return true;
  }

  protected void redraw() {
    sp.invalidate();
    sp.validate();

    Point viewportPosition = new Point(0,0);
    sp.getViewport().setViewPosition(viewportPosition);
  }

  private boolean orderChanged() {
    if (properties.size() != panels.size()) {
      logger.warn("types.size != panels.size in orderChanged");
      return false;
    }

    Vector tier_properties = properties.getAllTiers();
    for (int i = 0; i < tier_properties.size(); i++) {
      TierProperty  tp = (TierProperty) tier_properties.elementAt(i);
      PropertyPanel pp = (PropertyPanel)panels.elementAt(i);
      if (tp != pp.getProperty()) {
        return true;
      }
    }
    return false;
  }

  public boolean handleFeatureSelectionEvent(FeatureSelectionEvent evt) {
    if (evt.getFeature() != null && getFrame().isVisible()) {
      int propHeight = ((JPanel)panels.elementAt(0)).getSize().height;
      int ind = properties.getTierInd(evt.getFeature().getTopLevelType());

      Point     viewportPosition = sp.getViewport().getViewPosition();
      Dimension extentSize       = sp.getViewport().getExtentSize();
      Dimension viewSize         = sp.getViewport().getViewSize();

      // +4 is for GridLayout vGap
      viewportPosition.y = Math.min((propHeight+4) * ind,
                                    viewSize.height - extentSize.height);


      sp.getViewport().setViewPosition(viewportPosition);
      if (ind < panels.size() && ind >= 0) {
        ((PropertyPanel)(panels.elementAt(ind))).flash();
      }
    }

    return true;
  }

  private void jbInit() {

    // !!!!!!!
    // Try this with vertical box layout
    // !!!!!!!
    doneInit = true;

    // 0 for rows allows us to add more rows later
    //mainP = new JPanel();
    GridLayout layout = new GridLayout(0,1,1,4);
    //mainP.setLayout(layout);
    setLayout(layout);
    Vector tier_properties = properties.getAllTiers();
    for (int i=0; i < tier_properties.size();i++) {
      addPropertyPanel((TierProperty) tier_properties.elementAt(i));
    }
    //sp = new JScrollPane(mainP, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    sp = new JScrollPane(this, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    sp.setPreferredSize(new Dimension(200,300));
  }

  public void actionPerformed(ActionEvent evt) {
    if (evt.getSource() == autoBox) {
      setAutoRaise(((JCheckBox)autoBox).isSelected());
    }
  }

  private void setAutoRaise(boolean state) {
    autoRaise = state;
  }

  private void addPropertyPanel(TierProperty fp) {
    if (logger.isDebugEnabled()) {
      logger.debug("adding property panel for " + fp.getTypesAsString());
    }
    PropertyPanel   pp = new PropertyPanel(ap,fp);
    pp.setPreferredSize(new Dimension(0, pp.getPreferredSize().height));
    //mainP.add(pp);
    add(pp);

    panels.addElement(pp);
  }

  public JScrollPane getScrollPane() {
    return sp;
  }

  /** DataLoadListener method. Region changing means a new data set is 
      being loaded. Close this window if we've switched to a different style. */
  public boolean handleDataLoadEvent(DataLoadEvent e) {
    String newStyleFileName = Config.getStyle().getFileName();
    if ((newStyleFileName.equals(styleFileName))) {  // Don't need to 
      return true;
    }

    clear();
    init(getController(),Config.getPropertyScheme());
    return true;
  }
  private void clear() {
    controller.removeListener(this);
    this.removeAll(); // remove all the property panels
    //getFrame().removeAll();
    properties = null;
    panels.clear();
    setVisible(false);
    this.getFrame().hide();
    this.getFrame().dispose();
    // Need to uncheck "Show types panel" checkbox in Tiers menu.
    apollo.gui.menus.TiersMenu.types.setState(false);
  }

}
