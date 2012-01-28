package apollo.dataadapter.ensj.view;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import apollo.util.GuiUtil;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.controller.*;

public class AnnotationsPanel extends JPanel implements ActionListener{
  
  private View _view;

  private JLabel _fileLabel = new JLabel("Input File");
  private JLabel _serverLabel = new JLabel("Annotation Server / Port");
  private JLabel _serverPortLabel = new JLabel(":");
  private JLabel _dataSetLabel = new JLabel("Annotation Dataset");
  private JLabel _annotationUserLabel = new JLabel("Author");
  private JLabel _annotationUserEmailLabel = new JLabel("Email");

  private JTextField _annotationUserTextField = new JTextField(20);

  //annotations to-from a server
  private JTextField _serverTextField  = new JTextField(20);
  private JTextField _serverPortTextField  = new JTextField(20);
  private JTextField _annotationUserEmailTextField  = new JTextField(20);
  private JCheckBox _editingEnabledCheckBox  = new JCheckBox("Editing Enabled: ");
  private JComboBox _dataSetDropdown = new JComboBox();
  
  //annotations to-fron a file
  private JComboBox _fileDropdown = new JComboBox();
  private JButton _fileChooseButton = new JButton("Choose...");
  private JFileChooser _fileChooser = new JFileChooser();
    
  public AnnotationsPanel(View view){
    _view = view;
    initialiseGUI();
  }

  private void initialiseGUI(){
    setLayout(new GridBagLayout());
    Dimension mySize = getServerTextField().getPreferredSize();
    getDataSetDropdown().setPreferredSize(mySize);
    getFileDropdown().setEditable(true);
    getFileDropdown().setPreferredSize(mySize);

    int row = 0;
    
    JPanel fileIOPanel = new JPanel();
    JPanel serverIOPanel = new JPanel();
    fileIOPanel.setBorder(BorderFactory.createTitledBorder("File Based Annotations"));
    serverIOPanel.setBorder(BorderFactory.createTitledBorder("Server Based Annotations"));


    fileIOPanel.setLayout(new GridBagLayout());
    row = 0;
    fileIOPanel.add(getFileLabel(), GuiUtil.makeConstraintAt(0,row,1));
    fileIOPanel.add(getFileDropdown(), GuiUtil.makeConstraintAt(1,row,1));
    fileIOPanel.add(getFileChooseButton(), GuiUtil.makeConstraintAt(2,row,1));
    getFileChooseButton().addActionListener(this);
    
    serverIOPanel.setLayout(new GridBagLayout());
    row=0;
    serverIOPanel.add(getAnnotationUserLabel(), GuiUtil.makeConstraintAt(0,row,1));
    serverIOPanel.add(getAnnotationUserTextField(), GuiUtil.makeConstraintAt(1,row,1));
    serverIOPanel.add(getEditingEnabledCheckBox(), GuiUtil.makeConstraintAt(2,row,3));
    row++;
    serverIOPanel.add(getAnnotationUserEmailLabel(), GuiUtil.makeConstraintAt(0,row,1));
    serverIOPanel.add(getAnnotationUserEmailTextField(), GuiUtil.makeConstraintAt(1,row,1));
    row++;
    serverIOPanel.add(getServerLabel(),  GuiUtil.makeConstraintAt(0,row,1));
    serverIOPanel.add(getServerTextField(), GuiUtil.makeConstraintAt(1,row,2));
    serverIOPanel.add(getServerPortLabel(), GuiUtil.makeConstraintAt(2,row,1));
    serverIOPanel.add(getServerPortTextField(), GuiUtil.makeConstraintAt(3,row,1));
    row++;
    serverIOPanel.add(getDataSetLabel(), GuiUtil.makeConstraintAt(0,row,1));
    serverIOPanel.add(getDataSetDropdown(), GuiUtil.makeConstraintAt(1,row,1));

    setLayout(new GridBagLayout());
    row=0;
    add(serverIOPanel, GuiUtil.makeConstraintAt(0,row,3));
    row++;
    add(fileIOPanel, GuiUtil.makeConstraintAt(0,row,3));

  }

  public void update(Model model){
    AnnotationsModel myModel = model.getAnnotationsModel();
    myModel.setAnnotationUser(getAnnotationUserTextField().getText());
    myModel.setEditingEnabled(getEditingEnabledCheckBox().isSelected());
    myModel.setServer(getServerTextField().getText());
    myModel.setServerPort(getServerPortTextField().getText());
    myModel.setSelectedDataSet((String)getDataSetDropdown().getSelectedItem());
    myModel.setSelectedFile((String)getFileDropdown().getSelectedItem());
  }

  public void read(Model model){
    AnnotationsModel myModel = model.getAnnotationsModel();
    
    getAnnotationUserTextField().setText(myModel.getAnnotationUser());
    getEditingEnabledCheckBox().setSelected(myModel.isEditingEnabled());
    getServerTextField().setText(myModel.getServer());
    getServerPortTextField().setText(myModel.getServerPort());
    getDataSetDropdown().setModel(new DefaultComboBoxModel(new Vector(myModel.getDataSets())));
    getDataSetDropdown().setSelectedItem(myModel.getSelectedDataSet());
    getFileDropdown().setSelectedItem(myModel.getSelectedFile());

  }

  private View getView(){
    return _view;
  }
  
  private JLabel getAnnotationUserLabel() {
    return _annotationUserLabel;
  }
 
  private JLabel getAnnotationUserEmailLabel() {
    return _annotationUserEmailLabel;
  }
 
  private JLabel getServerLabel() {
    return _serverLabel;
  }

  private JLabel getServerPortLabel(){
    return _serverPortLabel;
  }
  
  private JTextField getAnnotationUserTextField(){
    return _annotationUserTextField;
  }
  
  private JTextField getAnnotationUserEmailTextField(){
    return _annotationUserEmailTextField;
  }
  
  private JCheckBox getEditingEnabledCheckBox(){
    return _editingEnabledCheckBox;
  }
  
  private JLabel getDataSetLabel() {
    return _dataSetLabel;
  }

  private JComboBox getDataSetDropdown() {
    return _dataSetDropdown;
  }

  private JTextField getServerTextField(){
    return _serverTextField;
  }
  
  private JTextField getServerPortTextField(){
    return _serverPortTextField;
  }
  
  private JLabel getFileLabel() {
    return _fileLabel;
  }

  private JFileChooser getFileChooser() {
    return _fileChooser;
  }
  
  private JComboBox getFileDropdown(){
    return _fileDropdown;
  }

  private JButton getFileChooseButton(){
    return _fileChooseButton;
  }
  
  public void actionPerformed(ActionEvent event){
    getFileChooser().showOpenDialog(this);
    //blocks till the user makes a choice, then we query for result and
    //paste it back into the File dropdown -
    File chosenFile = getFileChooser().getSelectedFile();
    String name;
    
    if(chosenFile != null){
      name = chosenFile.getName();
      getFileDropdown().addItem(name);
      getFileDropdown().setSelectedItem(name);
    }
    
  }
}
