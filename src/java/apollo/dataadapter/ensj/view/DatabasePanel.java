package apollo.dataadapter.ensj.view;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import apollo.util.GuiUtil;

import apollo.gui.PopupPropertyListener;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.controller.*;

public class DatabasePanel extends JPanel{
  private JLabel _hostLabel = new JLabel("Host");
  private JLabel _portLabel = new JLabel("Port");
  private JLabel _userLabel = new JLabel("User");
  private JLabel _passwordLabel = new JLabel("Password");
  private JLabel _ensemblDatabaseLabel = new JLabel("Ensembl Database Name");
  
  private JTextField _hostTextField = new JTextField(40);
  private JTextField _portTextField = new JTextField(6);
  private JTextField _userTextField = new JTextField(40);
  private JPasswordField _passwordTextField = new JPasswordField(40);
  private UpdateWorkRoundComboBox _ensemblDatabaseDropdown = new UpdateWorkRoundComboBox();
  
  private JLabel _sequenceHostLabel = new JLabel("Host");
  private JLabel _sequencePortLabel = new JLabel("Port");
  private JLabel _sequenceUserLabel = new JLabel("User");
  private JLabel _sequencePasswordLabel = new JLabel("Password");
  private JLabel _sequenceEnsemblDatabaseLabel = new JLabel("Ensembl Database Name");
  
  private JTextField _sequenceHostTextField = new JTextField(40);
  private JTextField _sequencePortTextField = new JTextField(6);
  private JTextField _sequenceUserTextField = new JTextField(40);
  private JPasswordField _sequencePasswordTextField = new JPasswordField(40);
  private JComboBox _sequenceEnsemblDatabaseDropdown = new JComboBox();
  
  private View _view;

  public DatabasePanel(View view){
    _view = view;
    initialiseGUI();
  }

  private void initialiseGUI(){
    setLayout(new GridBagLayout());
    add(createCoreDatabasePanel(),GuiUtil.makeConstraintAt(0,0,1));
  }
  
  private JPanel createCoreDatabasePanel(){
    int row = 0;
  
    JPanel corePanel = new JPanel();
    Dimension fieldSize = new Dimension(400,25);
    
    corePanel.setLayout(new GridBagLayout());

    corePanel.add(getHostLabel(),GuiUtil.makeConstraintAt(0,row,1));
    corePanel.add(getHostTextField(),GuiUtil.makeConstraintAt(1,row,3));
    getHostTextField().setPreferredSize(fieldSize);
    getHostTextField().setMinimumSize(fieldSize);
    getView().getAdapterGUI().addKeyRouter(getHostTextField(), Controller.CHANGE_DATABASE);
    
    row++;
    corePanel.add(getPortLabel(),GuiUtil.makeConstraintAt(0,row,1));
    corePanel.add(getPortTextField(),GuiUtil.makeConstraintAt(1,row,3));
    getPortTextField().setPreferredSize(fieldSize);
    getPortTextField().setMinimumSize(fieldSize);
    getView().getAdapterGUI().addKeyRouter(getPortTextField(), Controller.CHANGE_DATABASE);
    
    row++;
    corePanel.add(getUserLabel(),GuiUtil.makeConstraintAt(0,row,1));
    corePanel.add(getUserTextField(),GuiUtil.makeConstraintAt(1,row,3));
    getUserTextField().setPreferredSize(fieldSize);
    getUserTextField().setMinimumSize(fieldSize);
    getView().getAdapterGUI().addKeyRouter(getUserTextField(), Controller.CHANGE_DATABASE);
    
    row++;
    corePanel.add(getPasswordLabel(),GuiUtil.makeConstraintAt(0,row,1));
    corePanel.add(getPasswordTextField(),GuiUtil.makeConstraintAt(1,row,3));
    getPasswordTextField().setPreferredSize(fieldSize);
    getPasswordTextField().setMinimumSize(fieldSize);
    getView().getAdapterGUI().addKeyRouter(getPasswordTextField(), Controller.CHANGE_DATABASE);
    
    row++;
    corePanel.add(getEnsemblDatabaseLabel(),GuiUtil.makeConstraintAt(0,row,1));
    
    //I want the ensembl db-dropdown to be 2/3 the size of the other textfields.
    Dimension hostTextFieldDimension = getHostTextField().getPreferredSize();
    int newX = new Double((new Integer(hostTextFieldDimension.width).doubleValue())*0.66).intValue();
    Dimension newDimension = new Dimension(newX, hostTextFieldDimension.height);
    
    getEnsemblDatabaseDropdown().setPreferredSize(fieldSize);
    getEnsemblDatabaseDropdown().setMinimumSize(fieldSize);
    getEnsemblDatabaseDropdown().getPopup().addPropertyChangeListener(new PopupPropertyListener());

    getView().getAdapterGUI().addPopupRouter(getEnsemblDatabaseDropdown(), Controller.FIND_ENSEMBL_DATABASE_NAMES);
    getView().getAdapterGUI().addActionRouter(getEnsemblDatabaseDropdown(), Controller.SELECT_NEW_ENSEMBL_DATABASE);
    
    corePanel.add(getEnsemblDatabaseDropdown(),GuiUtil.makeConstraintAt(1,row,1));
    
    return corePanel;
  }//end buildGUI
  
  private JPanel createSequenceDatabasePanel(){
    int row = 0;
  
    JPanel sequencePanel = new JPanel();
    sequencePanel.setLayout(new GridBagLayout());

    sequencePanel.add(getSequenceHostLabel(),GuiUtil.makeConstraintAt(0,row,1));
    sequencePanel.add(getSequenceHostTextField(),GuiUtil.makeConstraintAt(1,row,3));
    
    row++;
    sequencePanel.add(getSequencePortLabel(),GuiUtil.makeConstraintAt(0,row,1));
    sequencePanel.add(getSequencePortTextField(),GuiUtil.makeConstraintAt(1,row,3));
    
    row++;
    sequencePanel.add(getSequenceUserLabel(),GuiUtil.makeConstraintAt(0,row,1));
    sequencePanel.add(getSequenceUserTextField(),GuiUtil.makeConstraintAt(1,row,3));
    
    row++;
    sequencePanel.add(getSequencePasswordLabel(),GuiUtil.makeConstraintAt(0,row,1));
    sequencePanel.add(getSequencePasswordTextField(),GuiUtil.makeConstraintAt(1,row,3));
    
    row++;
    sequencePanel.add(getSequenceEnsemblDatabaseLabel(),GuiUtil.makeConstraintAt(0,row,1));
    
    //I want the ensembl db-dropdown to be 2/3 the size of the other textfields.
    Dimension hostTextFieldDimension = getSequenceHostTextField().getPreferredSize();
    int newX = new Double((new Integer(hostTextFieldDimension.width).doubleValue())*0.66).intValue();
    Dimension newDimension = new Dimension(newX, hostTextFieldDimension.height);
    getSequenceEnsemblDatabaseDropdown().setPreferredSize(newDimension);
    
    sequencePanel.add(getSequenceEnsemblDatabaseDropdown(),GuiUtil.makeConstraintAt(1,row,1));
    
    return sequencePanel;
  }//end buildGUI
  
  public void update(Model model){
    DatabaseModel myModel = model.getDatabaseModel();
    myModel.setHost(getHostTextField().getText());
    myModel.setPort(getPortTextField().getText());
    myModel.setUser(getUserTextField().getText());
    myModel.setPassword(getPasswordTextField().getText());
    myModel.setSelectedEnsemblDatabase((String)getEnsemblDatabaseDropdown().getSelectedItem());
  }

  public void read(Model model){
    DatabaseModel myModel = model.getDatabaseModel();
    getHostTextField().setText(myModel.getHost());
    getPortTextField().setText(myModel.getPort());
    getUserTextField().setText(myModel.getUser());
    getPasswordTextField().setText(myModel.getPassword());
    getEnsemblDatabaseDropdown().setModel(new DefaultComboBoxModel(new Vector(myModel.getEnsemblDatabases())));
    getEnsemblDatabaseDropdown().setSelectedItem(myModel.getSelectedEnsemblDatabase());
  }

  private JTextField getHostTextField(){
    return _hostTextField;
  }

  private JTextField getPortTextField(){
    return _portTextField;
  }

  private JTextField getUserTextField(){
    return _userTextField;
  }

  private JPasswordField getPasswordTextField(){
    return _passwordTextField;
  }

  public UpdateWorkRoundComboBox getEnsemblDatabaseDropdown(){
    return _ensemblDatabaseDropdown;
  }

  private JLabel getHostLabel(){
    return _hostLabel;
  }

  private JLabel getPortLabel(){
    return _portLabel;
  }

  private JLabel getUserLabel(){
    return _userLabel;
  }

  private JLabel getPasswordLabel(){
    return _passwordLabel;
  }

  public JLabel getEnsemblDatabaseLabel(){
    return _ensemblDatabaseLabel;
  }

  private JTextField getSequenceHostTextField(){
    return _sequenceHostTextField;
  }

  private JTextField getSequencePortTextField(){
    return _sequencePortTextField;
  }

  private JTextField getSequenceUserTextField(){
    return _sequenceUserTextField;
  }

  private JPasswordField getSequencePasswordTextField(){
    return _sequencePasswordTextField;
  }

  public JComboBox getSequenceEnsemblDatabaseDropdown(){
    return _sequenceEnsemblDatabaseDropdown;
  }

  private JLabel getSequenceHostLabel(){
    return _sequenceHostLabel;
  }

  private JLabel getSequencePortLabel(){
    return _sequencePortLabel;
  }

  private JLabel getSequenceUserLabel(){
    return _sequenceUserLabel;
  }

  private JLabel getSequencePasswordLabel(){
    return _sequencePasswordLabel;
  }

  public JLabel getSequenceEnsemblDatabaseLabel(){
    return _sequenceEnsemblDatabaseLabel;
  }

  private View getView(){
    return _view;
  }
}
