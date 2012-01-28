package apollo.dataadapter.ensj19;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

import org.ensembl19.gui.FileChooserWithHistory;

import apollo.util.GuiUtil;

/**
 * Presents the user with a choice of file or server input for the 
 * annotation source.
**/
public abstract class AnnotationSourceChooser extends JPanel {
  private JLabel inputFileLabel = new JLabel("Input File");
  private JLabel inputServerLabel = new JLabel("Annotation Server / Port");
  private JLabel inputDataSetLabel = new JLabel("Annotation Dataset");
  private JLabel outputFileLabel = new JLabel("Output File");
  private JLabel outputDataSetLabel = new JLabel("Output Dataset");
  private JLabel serverPortLabel = new JLabel(":");

  private JTextField annotationUserTextField;
  private JLabel annotationUserLabel = new JLabel("Author");
  private JTextField annotationUserEmailTextField;
  private JTextField serverPortTextField;
  private JLabel annotationUserEmailLabel = new JLabel("Email");
  private JCheckBox editingEnabledCheckBox;
  private FileChooserWithHistory inputFileTextField;
  private SourceChooserWithHistory inputServerTextField;
  private JComboBox inputDataSetList;
  private FileChooserWithHistory outputFileTextField;
  private JComboBox outputDataSetList;
  private JButton findButton;  

  public class RemoveDatasetsListenerForPort extends KeyAdapter{
    public void keyPressed(KeyEvent event){
      getInputDataSetList().setModel(new DefaultComboBoxModel());
    }
  }

  public class RemoveDatasetsListenerForServer implements ItemListener{
    public void itemStateChanged(ItemEvent event){
      getInputDataSetList().setModel(new DefaultComboBoxModel());
    }
  }
  

  public class FindDataSetsListener implements ActionListener{
    public void actionPerformed(ActionEvent event){
      java.util.List dataSetList;
      Vector vector;

      if(
        getInputServerTextField().getSelectedSource() != null &&
        getServerPortTextField().getText() != null &&
        getServerPortTextField().getText().trim().length() > 0
      ){
        dataSetList = 
          getDataSetsForServer(
            getInputServerTextField().getSelectedSource(), 
            getServerPortTextField().getText()
          );
        vector = new Vector(dataSetList);
        getInputDataSetList().setModel(
          new DefaultComboBoxModel(vector)
        );
      }//end if
    }
  }//end FindDataSetsListener

  public AnnotationSourceChooser(){
    this(new Vector(), new Vector(), new Vector());
  }
  
  public AnnotationSourceChooser(
    Vector inputFileHistory,
    Vector inputServerHistory,
    Vector outputFileHistory
  ){
    
    RemoveDatasetsListenerForPort keyListener = new RemoveDatasetsListenerForPort();
    RemoveDatasetsListenerForServer itemListener = new RemoveDatasetsListenerForServer();
    
    inputFileTextField = new FileChooserWithHistory(null, inputFileHistory, this, "xml");
    outputFileTextField = new FileChooserWithHistory(null, outputFileHistory, this, "xml");

    inputServerTextField = new SourceChooserWithHistory(inputServerHistory);
    inputDataSetList = new JComboBox();
    inputDataSetList.setPreferredSize(inputServerTextField.getSourceHistoryList().getPreferredSize());
    inputDataSetList.setEditable(false);
    serverPortTextField = new JTextField(6);
    
    outputDataSetList = new JComboBox();
    outputDataSetList.setPreferredSize(inputServerTextField.getSourceHistoryList().getPreferredSize());
    outputDataSetList.setEditable(true);

    annotationUserTextField = new JTextField();
    annotationUserTextField.setPreferredSize(
      inputServerTextField.getSourceHistoryList().getPreferredSize()
    );

    annotationUserEmailTextField = new JTextField();
    annotationUserEmailTextField.setPreferredSize(
      inputServerTextField.getSourceHistoryList().getPreferredSize()
    );
    
    editingEnabledCheckBox = new JCheckBox("Editing Enabled");
    findButton = new JButton("Find...");
    findButton.addActionListener(new AnnotationSourceChooser.FindDataSetsListener());
    
    serverPortTextField.addKeyListener(keyListener);
    inputServerTextField.addItemListener(itemListener);
    
    layoutComponents();
  }//end AnnotationSourceChooser
  
  private void layoutComponents(){
    int row = 0;
    
    JPanel fileIOPanel = new JPanel();
    JPanel serverIOPanel = new JPanel();
    fileIOPanel.setBorder(BorderFactory.createTitledBorder("File Based Annotations"));
    serverIOPanel.setBorder(BorderFactory.createTitledBorder("Server Based Annotations"));
    

    fileIOPanel.setLayout(new GridBagLayout());
    row = 0;
    fileIOPanel.add(getInputFileLabel(), GuiUtil.makeConstraintAt(0,row,1));
    fileIOPanel.add(getInputFileTextField(), GuiUtil.makeConstraintAt(1,row,2));
    row++;
    fileIOPanel.add(getOutputFileLabel(), GuiUtil.makeConstraintAt(0,row,1));
    fileIOPanel.add(getOutputFileTextField(), GuiUtil.makeConstraintAt(1,row,2));
    
    serverIOPanel.setLayout(new GridBagLayout());
    row=0;
    serverIOPanel.add(getAnnotationUserLabel(), GuiUtil.makeConstraintAt(0,row,1));
    serverIOPanel.add(getAnnotationUserTextField(), GuiUtil.makeConstraintAt(1,row,1));
    serverIOPanel.add(getEditingEnabledCheckBox(), GuiUtil.makeConstraintAt(2,row,3));
    row++;
    serverIOPanel.add(getAnnotationUserEmailLabel(), GuiUtil.makeConstraintAt(0,row,1));
    serverIOPanel.add(getAnnotationUserEmailTextField(), GuiUtil.makeConstraintAt(1,row,1));
    row++;
    serverIOPanel.add(getInputServerLabel(),  GuiUtil.makeConstraintAt(0,row,1));
    serverIOPanel.add(getInputServerTextField(), GuiUtil.makeConstraintAt(1,row,2));   
    serverIOPanel.add(getServerPortLabel(), GuiUtil.makeConstraintAt(2,row,1));   
    serverIOPanel.add(getServerPortTextField(), GuiUtil.makeConstraintAt(3,row,1));   
    row++;
    serverIOPanel.add(getInputDataSetLabel(), GuiUtil.makeConstraintAt(0,row,1));
    serverIOPanel.add(getInputDataSetList(), GuiUtil.makeConstraintAt(1,row,1));
    serverIOPanel.add(getFindButton(), GuiUtil.makeConstraintAt(3,row,1));
    
    //
    // Don't add separate widgets for output server/dataset. These will be 
    //defaulted from the input-server name and dataset.
    //
    setLayout(new GridBagLayout());
    row=0;
    add(serverIOPanel, GuiUtil.makeConstraintAt(0,row,3));
    row++;
    add(fileIOPanel, GuiUtil.makeConstraintAt(0,row,3));

  }//end layout
  
  private JLabel getAnnotationUserLabel() {
    return annotationUserLabel;
  }//end getAnnotationUserLabel
 
  private JTextField getAnnotationUserTextField(){
    return annotationUserTextField;
  }//end getAnnotationUserTextField
  
  private JLabel getAnnotationUserEmailLabel() {
    return annotationUserEmailLabel;
  }//end getAnnotationUserLabel
 
  private JTextField getAnnotationUserEmailTextField(){
    return annotationUserEmailTextField;
  }//end getAnnotationUserTextField
  
  public String getAnnotationUser(){
    return getAnnotationUserTextField().getText();
  }//end getAnnotationUser
  
  public void setAnnotationUser(String user){
    getAnnotationUserTextField().setText(user);
  }//end setAnnotationUser
  
  public String getAnnotationUserEmail(){
    return getAnnotationUserEmailTextField().getText();
  }//end getAnnotationUser
  
  public void setAnnotationUserEmail(String email){
    getAnnotationUserEmailTextField().setText(email);
  }//end setAnnotationUser
  
  private JCheckBox getEditingEnabledCheckBox(){
    return editingEnabledCheckBox;
  }//end getEditingEnabledCheckBox
  
  private JLabel getInputServerLabel() {
    return inputServerLabel;
  }//end getInputServerLabel
 
  private void setInputServerLabel(JLabel inputServerLabel) {
    this.inputServerLabel = inputServerLabel; 
  }//end setInputServerLabel
  
  private JLabel getInputDataSetLabel() {
    return inputDataSetLabel;
  }//end getInputDataSetLabel

  private void setInputDataSetLabel(JLabel newValue) {
    inputDataSetLabel = newValue;
  }//end setInputDataSetLabel
  
  private JLabel getInputFileLabel() {
    return this.inputFileLabel;
  }//end getInputFileLabel
  
  private void setInputFileLabel(JLabel inputFileLabel) {
    this.inputFileLabel = inputFileLabel;
  }//end setInputFileLabel
  
  private JLabel getOutputFileLabel() {
    return this.outputFileLabel;
  }//end getOutputFileLabel
  
  private void setOutputFileLabel(JLabel outputFileLabel) {
    this.outputFileLabel = outputFileLabel;
  }//end setOutputFileLabel
  
  private JLabel getOutputDataSetLabel() {
    return this.outputDataSetLabel;
  }//end getOutputDataSetLabel
  
  private void setOutputDataSetLabel(JLabel outputDataSetLabel) {
    this.outputDataSetLabel = outputDataSetLabel;
  }//end setOutputDataSetLabel

  private FileChooserWithHistory getInputFileTextField() {
    return this.inputFileTextField;
  }
  
  private void setInputFileTextField(FileChooserWithHistory inputFileTextField) {
    this.inputFileTextField = inputFileTextField;
  }
  
  private FileChooserWithHistory getOutputFileTextField() {
    return this.outputFileTextField;
  }
  
  private void setOutputFileTextField(FileChooserWithHistory outputFileTextField) {
    this.outputFileTextField = outputFileTextField;
  }

  private JComboBox getInputDataSetList() {
    return this.inputDataSetList;
  }

  private void setInputDataSetList(JComboBox inputDataSetList) {
    this.inputDataSetList = inputDataSetList;
  }
  
  private SourceChooserWithHistory getInputServerTextField() {
    return this.inputServerTextField;
  }

  private void setInputServerTextField(SourceChooserWithHistory inputServerTextField) {
    this.inputServerTextField = inputServerTextField;
  }
  
  private JComboBox getOutputDataSetList() {
    return this.outputDataSetList;
  }
  
  private void setOutputDataSetList(JComboBox outputDataSetList) {
    this.outputDataSetList = outputDataSetList;
  }

  private JTextField getServerPortTextField(){
    return serverPortTextField;
  }//end getServerPortTextField
  
  private JLabel getServerPortLabel(){
    return serverPortLabel;
  }//end getServerPortLabel
  
  public String getSelectedInputFileName(){
    return getInputFileTextField().getSelected();
  }//end getSelectedInputFileName
  
  public String getSelectedOutputFileName(){
    if(
      getOutputFileTextField().getSelected() == null ||
      getOutputFileTextField().getSelected().trim().length() <=0
    ){
      return getSelectedInputFileName();
    }
    
    return getOutputFileTextField().getSelected();
  }//end getSelectedOutputFileName

  public String getSelectedInputServerName(){
    return (String)getInputServerTextField().getSourceHistoryList().getSelectedItem();
  }//end getSelectedInputServerName
  
  public void setSelectedInputServerName(String name){
    getInputServerTextField().getSourceHistoryList().setSelectedItem(name);
  }//end setSelectedInputServerName
  
  public String getSelectedInputDataSetName(){
    return (String)getInputDataSetList().getSelectedItem();
  }//end getSelectedDataSetServerName
  
  public void setSelectedInputDataSetName(String name){
    getInputDataSetList().setSelectedItem(name);
  }//end getSelectedDataSetServerName
  
  public String getSelectedOutputDataSetName(){
    if(
      getOutputDataSetList().getSelectedItem() == null ||
      ((String)getOutputDataSetList().getSelectedItem()).trim().length() <= 0
    ){
      return getSelectedInputDataSetName();
    }
    return (String)getOutputDataSetList().getSelectedItem();
  }//end getSelectedOutputDataSetName
  
  public boolean isEditingEnabled(){
    return getEditingEnabledCheckBox().isSelected();
  }//end isEditingEnabled
  
  public Vector getInputFileHistory(){
    return getInputFileTextField().getHistory();
  }//end setInputFileHistory
  
  public void setInputFileHistory(Vector historyVector){
    getInputFileTextField().setHistory(historyVector);
  }//end setInputFileHistory
  
  public Vector getInputServerHistory(){
    return getInputServerTextField().getSourceHistory();
  }//end getInputServerHistory

  public void setInputServerHistory(Vector historyVector){
    getInputServerTextField().setSourceHistory(historyVector);
  }//end setInputServerHistory

  public Vector getOutputFileHistory(){
    return getOutputFileTextField().getHistory();
  }//end setOutputFileHistory
  
  public void setOutputFileHistory(Vector historyVector){
    getOutputFileTextField().setHistory(historyVector);
  }//end setOutputFileHistory
  
  public void setSelectedInputFileName(String name){
    getInputFileTextField().setSelected(name);
  }//end setSelectedInputFileName
  
  public void setSelectedOutputFileName(String name){
    getOutputFileTextField().setSelected(name);
  }//end setSelectedOutputFileName

  public String getSelectedServerPort(){
    return getServerPortTextField().getText();
  }//end getSelectedServerPort
  
  public void setSelectedServerPort(String port){
    getServerPortTextField().setText(port);
  }//end setSelectedServerPort
  
  public static void main(String[] args){
    JFrame testFrame = new JFrame("test");
    testFrame.getContentPane().add(
      new OtterAnnotationSourceChooser(), 
      BorderLayout.CENTER
    );
    testFrame.pack();
    testFrame.show();
  }

  protected abstract java.util.List getDataSetsForServer(String serverURL, String portString);

  public JButton getFindButton() {
    return this.findButton;
  }//end getFindButton
  
  public void setFindButton(JButton testButton) {
    this.findButton = testButton;
  }//end getFindButton
}//end AnnotationSourceChooser
