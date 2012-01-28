package apollo.dataadapter.ensj.view;

import javax.swing.*;
import java.awt.*;
import apollo.util.GuiUtil;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.controller.*;

import org.apache.log4j.*;

public class View extends JPanel{
  protected final static Logger logger = LogManager.getLogger(View.class);

  
  private EnsJAdapterGUI _adapterGUI;
  private LocationPanel _locationPanel;
  private OptionsPanel _optionsPanel;
  private TypesPanel _typesPanel;
  private DatabasePanel _databasePanel;
  private AnnotationsPanel _annotationsPanel;
  
  public static final String SHOW_TYPES_LABEL = "Show / Hide Types...";
  public static final String SHOW_OPTIONS_LABEL = "Show / Hide Options...";
  public static final String SHOW_DATABASES_LABEL = "Show / Hide Databases...";
  public static final String SHOW_ANNOTATIONS_LABEL = "Show / Hide Annotations...";
  
  private JButton _typesButton = new JButton(SHOW_TYPES_LABEL);
  private JButton _optionsButton = new JButton(SHOW_OPTIONS_LABEL);
  private JButton _databaseButton = new JButton(SHOW_DATABASES_LABEL);
  private JButton _annotationsButton = new JButton(SHOW_ANNOTATIONS_LABEL);
  
  public View(EnsJAdapterGUI gui){
    _adapterGUI = gui;
  }
  
  public void initialiseView(){
    setLocationPanel(new LocationPanel(this));
    setTypesPanel(new TypesPanel(this));
    setOptionsPanel(new OptionsPanel(this));
    setDatabasePanel(new DatabasePanel(this));
    setAnnotationsPanel(new AnnotationsPanel(this));

    setLayout(new GridBagLayout());

    LocationPanel locationPanel = getLocationPanel();

    locationPanel.setBorder(BorderFactory.createTitledBorder("Location"));

    add(locationPanel, GuiUtil.makeConstraintAt(0,0,1));

    add( 
      createPanelContaining(getTypesButton(), getTypesPanel(), "Types"),
      GuiUtil.makeConstraintAt(0,1,1)
    );

    getAdapterGUI().addActionRouter(getTypesButton(), Controller.HIDE_OR_SHOW_TYPES);
    
    add( 
      createPanelContaining(getOptionsButton(), getOptionsPanel(), "Options"),
      GuiUtil.makeConstraintAt(0,2,1)
    );

    getAdapterGUI().addActionRouter(getOptionsButton(), Controller.HIDE_OR_SHOW_OPTIONS);
    
    add( 
      createPanelContaining(getDatabaseButton(), getDatabasePanel(), "Databases"),
      GuiUtil.makeConstraintAt(0,3,1)
    );

    getAdapterGUI().addActionRouter(getDatabaseButton(), Controller.HIDE_OR_SHOW_DATABASE);
    
    
    /* Remove annotations panel till otter function is revamped.
    add(
      createPanelContaining(getAnnotationsButton(), getAnnotationsPanel(), "ANNOTATIONS"),
      GuiUtil.makeConstraintAt(0,3,1)
    );

    getAdapterGUI().addActionRouter(getAnnotationsButton(), Controller.HIDE_OR_SHOW_ANNOTATIONS);
     */
  }
  
  private JPanel createPanelContaining(
    JButton hideButton, 
    JPanel panel, 
    String borderLabel
  ){
    JPanel newPanel = new JPanel();
    newPanel.setLayout(new GridBagLayout());
    newPanel.add(hideButton, GuiUtil.makeConstraintAt(0,0,1));
    newPanel.add(panel, GuiUtil.makeConstraintAt(0,1,1));
    newPanel.setBorder(BorderFactory.createTitledBorder(borderLabel));
    return newPanel;
  }
  
  public void update(Model model){
    getLocationPanel().update(model);
    getTypesPanel().update(model);
    getOptionsPanel().update(model);
    getDatabasePanel().update(model);
    getAnnotationsPanel().update(model);
  }
  
  public void read(Model model){
    // This is horrible - its a way of figuring out if this panel is part of a 
    // builder dialog or a straight ensj dialog
    // In the builder pack() calls are not wanted because the panel is displayed in
    // a scrollpane
    // In the ensj adapter we DO want pack calls to do the nice resizing on hide/show
    JScrollPane scrollpane = (JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
    
    if(model.isTypesPanelVisible()){
      getTypesPanel().setVisible(true);
    }else{
      getTypesPanel().setVisible(false);
    }
    
    if(model.isOptionsPanelVisible()){
      getOptionsPanel().setVisible(true);
    }else{
      getOptionsPanel().setVisible(false);
    }
    
    if(model.isDatabasePanelVisible()){
      getDatabasePanel().setVisible(true);
    }else{
      getDatabasePanel().setVisible(false);
    }

    
    if(model.isAnnotationsPanelVisible()){
      getAnnotationsPanel().setVisible(true);
    }else{
      getAnnotationsPanel().setVisible(false);
    }

    if(model.isLocationsPanelVisible()){
      getLocationPanel().setVisible(true);
    }else{
      getLocationPanel().setVisible(false);
    }

    // Why was this commented out?  It's needed to allow panel to expand
    // when user shows some of the hidden parts--otherwise panel stays small
    // and you can't see everything.  --NH, 6/2005
 
    // SMJS See comment above for the reason for the scrollpane check
    if(scrollpane == null){
      JDialog dialog = (JDialog)SwingUtilities.getAncestorOfClass(JDialog.class, this);
      if (dialog != null) {
        dialog.pack();
      }
    }
    
    getLocationPanel().read(model);
    getTypesPanel().read(model);
    getOptionsPanel().read(model);
    getDatabasePanel().read(model);
    getAnnotationsPanel().read(model);
  }

  private LocationPanel getLocationPanel(){
    return _locationPanel;
  }
  
  private void setLocationPanel(LocationPanel panel){
    _locationPanel = panel;
  } 

  private TypesPanel getTypesPanel(){
    return _typesPanel;
  }
  
  private void setTypesPanel(TypesPanel panel){
    _typesPanel = panel;
  } 

  private OptionsPanel getOptionsPanel(){
    return _optionsPanel;
  }
  
  private void setOptionsPanel(OptionsPanel panel){
    _optionsPanel = panel;
  } 

  private DatabasePanel getDatabasePanel(){
    return _databasePanel;
  }
  
  private void setDatabasePanel(DatabasePanel panel){
    _databasePanel = panel;
  } 

  private AnnotationsPanel getAnnotationsPanel(){
    return _annotationsPanel;
  }
  
  private void setAnnotationsPanel(AnnotationsPanel panel){
    _annotationsPanel = panel;
  } 
  
  private JButton getTypesButton(){
    return _typesButton;
  }
  
  private JButton getOptionsButton(){
    return _optionsButton;
  }
  
  private JButton getDatabaseButton(){
    return _databaseButton;
  }
  
  private JButton getAnnotationsButton(){
    return _annotationsButton;
  }
  
  public EnsJAdapterGUI getAdapterGUI(){
    return _adapterGUI;
  }
  

  static boolean dialogShowing = false;
  public void displayMessage(String message){
// Note this is done with invokeLater because it can be called from within 
// event handling code.  Without it Swing 1.5 on linux sometimes left zombie popup menus for the 
// LocationPanel comboboxes. invokeLater could be called from the event dispatch thread, but that
// would cause all the error messages for a load (lots if a connection goes away) to be displayed
    if (!dialogShowing) {
      final String finalMessage = message;
      dialogShowing = true;;
      SwingUtilities.invokeLater (new Runnable() {
        public void run() {
          JOptionPane.showMessageDialog(View.this, finalMessage);
          dialogShowing = false;
        }
      });
    } else {
      logger.error("Not showing dialog because one already showing. Here's the message:");
      logger.error(message);
    }
  }
}
