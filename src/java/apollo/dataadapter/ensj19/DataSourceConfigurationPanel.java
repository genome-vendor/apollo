package apollo.dataadapter.ensj19;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.sql.*;

import org.ensembl19.util.*;

import apollo.dataadapter.*;
import apollo.util.GuiUtil;

/**
 * This panel allows the user to configure the parameters necessary for the
 * EnsJ-interface to an ensembl database.
**/
public class 
  DataSourceConfigurationPanel 
extends 
  JPanel
{
  private JLabel hostLabel = new JLabel("Host");
  private JLabel portLabel = new JLabel("Port");
  private JLabel userLabel = new JLabel("User");
  private JLabel passwordLabel = new JLabel("Password");
  private JLabel ensemblDatabaseLabel = new JLabel("Ensembl Database Name");
  
  private JTextField jdbcDriverTextField = new JTextField(40);
  private JTextField hostTextField = new JTextField(40);
  private JTextField portTextField = new JTextField(6);
  private JTextField userTextField = new JTextField(40);
  private JPasswordField passwordTextField = new JPasswordField(40);
  private JTextField ensemblDriverTextField = new JTextField(40);
  private JComboBox ensemblDatabaseDropdown = new JComboBox();
  private JButton findButton = new JButton("Find...");
  
  private ActionListener hostListener;
  private ActionListener interactionListener;
  private ActionListener ensemblDBDropdownSelectionListener;

  //the name of the table which serves as a filter
  //for the mysql databases loaded in the ensembl-database-dropdown  
  private String prefix;
  
  private ActionListener externalDataChangeListener;
  
  /**
   * Triggered when the user pushes the "find" button
  **/
  public class HostListener implements ActionListener{
    public void actionPerformed(ActionEvent e){
      java.util.List databaseList = findDatabasesForMySQLHost();
      Vector inputVector = new Vector(databaseList);
      if(inputVector.size() >0){
        inputVector.add(0,"");
        getEnsemblDatabaseDropdown().removeActionListener(getEnsemblDBDropdownSelectionListener());
        getEnsemblDatabaseDropdown().setModel(new DefaultComboBoxModel(inputVector));
        DataSourceConfigurationPanel.this.ensemblDatabaseDropdown.setPopupVisible(true);        
        getEnsemblDatabaseDropdown().addActionListener(getEnsemblDBDropdownSelectionListener());
      }//end if
    }//end actionPerformed
  }
  
  public void detachEnsemblDatabaseDropdownListener(){
      getEnsemblDatabaseDropdown().removeActionListener(getEnsemblDBDropdownSelectionListener());
  }//end unhookEnsemblDatabaseDropdownListener
  
  public void attachEnsemblDatabaseDropdownListener(){
    getEnsemblDatabaseDropdown().addActionListener(getEnsemblDBDropdownSelectionListener());
  }//end attachEnsemblDatabaseDropdownListener

  /**
   * Sometimes we want to know when the user has started modifying the datasource,
   * from _outside_ this panel. This notifies external people that "something happened"
   * through an actionevent. The actionlistener added here is notified via the 
   * DataChangeListener inner class (which is itself triggered by a key press in a data-source
   * field).
  **/
  public void addExternalDataChangeListener(ActionListener listener){
    externalDataChangeListener = listener;
  }//end attachExternalDataChangeListener
  
  /**
   * Triggered when the user types into any data field
  **/
  public class DataChangeListener extends KeyAdapter{
    public void keyTyped(KeyEvent event){
      if(getEnsemblDatabaseDropdown().getModel().getSize() >0){
        detachEnsemblDatabaseDropdownListener();
        getEnsemblDatabaseDropdown().setModel(new DefaultComboBoxModel());
        attachEnsemblDatabaseDropdownListener();
        /*
        if(getExternalDataChangeListener()!= null){
          getExternalDataChangeListener().actionPerformed(new ActionEvent(this,this.hashCode(), "data changed"));
        }//end if
         */
      }//end if
    }//end keyPressed
  }//end DataChangeListener 

  /**
   * Triggered when the user selects a particular database
  **/
  public class EnsemblDBSelectionListener implements ActionListener{
    public void actionPerformed(ActionEvent event){
      if(getInteractionListener()!=null){
        getInteractionListener()
          .actionPerformed(
            new ActionEvent(
              getEnsemblDatabaseDropdown(), 
              0, 
              (String)getEnsemblDatabaseDropdown().getSelectedItem()
            )
          );
      }//end if

      //
      //If the user is changing datasources, we also need to blank out the chromosome dropdown,
      //and anything else that's listening externally.
      if(getExternalDataChangeListener()!= null){
        getExternalDataChangeListener().actionPerformed(new ActionEvent(this,this.hashCode(), "data changed"));
      }//end if
    }//end itemSelected
  }//end EnsemblDBSelectionListener

  public DataSourceConfigurationPanel(){
    prefix = "";
    buildGUI();
  }
  
  public DataSourceConfigurationPanel(String prefix){
    this.prefix = prefix;
    buildGUI();
  }
  
  /**
   * Prefix is used to 'namespace' a group of settings. It's presence allows
   * for multiple DataSourceConfigurationPanel settings to be stored in the
   * same properties object.
   * @return prefix parameter key prefix.
   */
  public String getPrefix() {
    return prefix;
  }

  private void buildGUI(){
    KeyListener dataChangeListener = new DataChangeListener();
    hostListener = new HostListener();
    int row = 0;
    
    setLayout(new GridBagLayout());

    add(hostLabel,GuiUtil.makeConstraintAt(0,row,1));
    add(hostTextField,GuiUtil.makeConstraintAt(1,row,3));
    hostTextField.addKeyListener(dataChangeListener);
    
    row++;
    add(portLabel,GuiUtil.makeConstraintAt(0,row,1));
    add(portTextField,GuiUtil.makeConstraintAt(1,row,3));
    portTextField.addKeyListener(dataChangeListener);
    
    row++;
    add(userLabel,GuiUtil.makeConstraintAt(0,row,1));
    add(userTextField,GuiUtil.makeConstraintAt(1,row,3));
    userTextField.addKeyListener(dataChangeListener);
    
    row++;
    add(passwordLabel,GuiUtil.makeConstraintAt(0,row,1));
    add(passwordTextField,GuiUtil.makeConstraintAt(1,row,3));
    passwordTextField.addKeyListener(dataChangeListener);
    
    row++;
    add(ensemblDatabaseLabel,GuiUtil.makeConstraintAt(0,row,1));
    ensemblDBDropdownSelectionListener = new EnsemblDBSelectionListener();
    ensemblDatabaseDropdown.addActionListener(ensemblDBDropdownSelectionListener);
    
    //I want the ensembl db-dropdown to be 2/3 the size of the other textfields.
    Dimension hostTextFieldDimension = hostTextField.getPreferredSize();
    int newX = new Double((new Integer(hostTextFieldDimension.width).doubleValue())*0.66).intValue();
    Dimension newDimension = new Dimension(newX, hostTextFieldDimension.height);
    ensemblDatabaseDropdown.setPreferredSize(newDimension);
    
    add(ensemblDatabaseDropdown,GuiUtil.makeConstraintAt(1,row,1));
    findButton.addActionListener(getHostListener());
    add(findButton,GuiUtil.makeConstraintAt(2,row,1));
    
  }//end buildGUI

  /**
   * This should be used to initialize the panel from historical etc values.
   * This sets the values for all the fields. Here are the keys which have to come in:
   *
   * <ul>
   * <li>jdbcDriver</li>
   * <li>host</li>
   * <li>port</li>
   * <li>user</li>
   * <li>password</li>
   * <li>ensemblDriver</li>
   * <li>ensemblDatabase</li>
   * </ul>
   * 
   * <p>If prefix is set then the keys must begin with prefix. e.g if
   * prefix="variation" then the input key corresponding to "host" above
   * would be "variation.host".</p>
   *
   * <p>I will first remove the actionlisteners, set the values for all input fields
   * except the ensembl database drop-down, then load the dropdown (if I can), then
   * set the selected value of the dropdown onto it (if it exists).</p>
   * 
  **/
  public void setProperties(Properties properties){
    setTextFieldFromProperty(getPrefix()+StateInformation.JDBC_DRIVER, properties, getJdbcDriverTextField() );
    
    setTextFieldFromProperty(getPrefix()+StateInformation.HOST, properties, getHostTextField() );
    
    setTextFieldFromProperty(getPrefix()+StateInformation.PORT, properties, getPortTextField() );

    setTextFieldFromProperty(getPrefix()+StateInformation.USER, properties, getUserTextField() );

    setTextFieldFromProperty(getPrefix()+StateInformation.PASSWORD, properties, getPasswordTextField() );

    setTextFieldFromProperty(getPrefix()+StateInformation.ENSEMBL_DRIVER, properties, getEnsemblDriverTextField() );

    getEnsemblDatabaseDropdown().removeActionListener(getEnsemblDBDropdownSelectionListener());

    if(
      properties.getProperty(getPrefix()+StateInformation.DATABASE) != null && 
      properties.getProperty(getPrefix()+StateInformation.DATABASE).trim().length() >0
    ){
      getEnsemblDatabaseDropdown().setSelectedItem(
        properties.getProperty(getPrefix()+StateInformation.DATABASE)
      );
    }//end if
    
    getEnsemblDatabaseDropdown().addActionListener(getEnsemblDBDropdownSelectionListener());
    
  }//end setProperties


  private void setTextFieldFromProperty(
    String key,
    Properties properties,
    JTextField textField
  ){
    if(properties.getProperty( key ) != null && properties.getProperty( key ).trim().length() >0){
      textField.setText(properties.getProperty( key ));
    }
  }
  

  /**
   * Returns the properties described in setProperties(). If prefix is set
   * then this is prepended to each key.
   * @see #setProperties setProperties()
   */
  public Properties getProperties(){
    Properties properties = new Properties();
    properties.put(
      getPrefix()+StateInformation.JDBC_DRIVER,
      getJdbcDriverTextField().getText()
    );
    
    properties.put(
      getPrefix()+StateInformation.HOST,
      getHostTextField().getText()
    );
    
    properties.put(
      getPrefix()+StateInformation.PORT,
      getPortTextField().getText()
    );
    
    properties.put(
      getPrefix()+StateInformation.USER,
      getUserTextField().getText()
    );
    
    properties.put(
      getPrefix()+StateInformation.PASSWORD,
      getPasswordTextField().getText()
    );
    
    properties.put(
      getPrefix()+StateInformation.ENSEMBL_DRIVER,
      getEnsemblDriverTextField().getText()
    );
    
    if(getEnsemblDatabaseDropdown().getSelectedItem() != null){
      properties.put(
        getPrefix()+StateInformation.DATABASE,
        getEnsemblDatabaseDropdown().getSelectedItem()
      );
    }//end if
    
    properties.put(
      prefix+StateInformation.CONNECTION_STRING,
      "jdbc:mysql://"+
      getHostTextField().getText()+
      ":"+
      getPortTextField().getText()
    );
    
    return properties;
  }//end setProperties

  public static void main(String[] args){
    JFrame myFrame = new JFrame();
    myFrame.setSize(200,200);
    myFrame.getContentPane().add(new DataSourceConfigurationPanel());
    myFrame.pack();
    myFrame.show();
  }
  
  private java.util.List findDatabasesForMySQLHost(){
    String jdbcDriver = jdbcDriverTextField.getText();
    String host = hostTextField.getText();
    String port = portTextField.getText();
    String url = "jdbc:mysql://" + host + ":" + port + "/";
    String user = userTextField.getText();
    String password = passwordTextField.getText();
    java.util.List returnList = new ArrayList();

    if(
      jdbcDriver == null ||
      jdbcDriver.trim().length() <= 0 ||
      host == null ||
      host.trim().length() <= 0 ||
      port == null ||
      port.trim().length() <= 0 ||
      user == null ||
      user.trim().length() <= 0
    ){
      return new ArrayList();
    }

    if(password == null){
      password = "";
    }
    
    try{
      Class.forName(jdbcDriver).newInstance();
    }catch(IllegalAccessException exception){
      JOptionPane.showMessageDialog(
        this,
        "Cannot access the driver class "+jdbcDriver+"\n"+exception.getMessage()
      );
    }catch(InstantiationException exception){
      JOptionPane.showMessageDialog(
        this,
        "Cannot create the driver class "+jdbcDriver+"\n"+exception.getMessage()
      );
    }catch(ClassNotFoundException exception){
      JOptionPane.showMessageDialog(
        this,
        "Cannot find the driver class "+jdbcDriver+"\n"+exception.getMessage()
      );
    }//end try

    try{
      java.sql.Connection conn = DriverManager.getConnection(url,user,password);
      Statement statement = conn.createStatement();
      //      DatabaseMetaData metadata = conn.getMetaData();
      
      ResultSet databases = statement.executeQuery("show databases");
      String databaseName;
      
      while(databases.next()){
        databaseName = databases.getString(1);
        returnList.add(databaseName);
      }//end while
      
    }catch(SQLException exception){
      JOptionPane.showMessageDialog(
        this,
        "I cannot connect to the MySQL instance on the host to find the databases: \n"+host+":"+port+" user: "+user+" - \n "+
        exception.getMessage()
      );
    }
    
    return returnList;
  }
  
  private JTextField getJdbcDriverTextField(){
    return jdbcDriverTextField;
  }//end getJdbcDriverTextField
  
  private JTextField getHostTextField(){
    return hostTextField;
  }
  
  private JTextField getPortTextField(){
    return portTextField;
  }
  
  private JTextField getUserTextField(){
    return userTextField;
  }
  
  private JPasswordField getPasswordTextField(){
    return passwordTextField;
  }
  
  private JTextField getEnsemblDriverTextField(){
    return ensemblDriverTextField;
  }
  
  public JComboBox getEnsemblDatabaseDropdown(){
    return ensemblDatabaseDropdown;
  }
  
  public String getSelectedEnsemblDatabase(){
    return (String)getEnsemblDatabaseDropdown().getSelectedItem();
  }
  
  public void setSelectedEnsemblDatabase(String database){
    getEnsemblDatabaseDropdown().setSelectedItem(database);
  }
  
  public JButton getFindButton(){
    return findButton;
  }
  
  public ActionListener getHostListener(){
    return hostListener;
  }
  
  public void setInteractionListener(ActionListener listener){
    interactionListener = listener;
  }
  
  public ActionListener getInteractionListener(){
    return interactionListener;
  }
  
  public ActionListener getEnsemblDBDropdownSelectionListener(){
    return ensemblDBDropdownSelectionListener;
  }
  
  public void setHostListener(ActionListener newValue){
    hostListener = newValue;
  }

  private boolean databaseHasKeyAndVersion(String key, String name){
    int keyIndex = -1;
    String numberSubstring = null;
    int endNumberIndex = -1;
    int version;

    keyIndex = name.indexOf(key);

    if(keyIndex >= 0){
      if(name.length() > keyIndex+key.length()){
        // find the substring "X_..."
        numberSubstring = name.substring(keyIndex+key.length()+1);
        // find the position of the next "_"
        endNumberIndex = numberSubstring.indexOf("_");
        // find the substring "X"
        numberSubstring = numberSubstring.substring(0,endNumberIndex);

        try{
          version = Integer.parseInt(numberSubstring);
        }catch(NumberFormatException exception){
          //
          //If that wasn't a number, we'll accept that we've found a core.
          return true;
        }

        if(version >= 9){
          return true;
        }else{
          return false;
        }//end if

      }else{
        //found the word "core" with no version. We'll take it.
        return true;
      }//end if
    }else{
      //didn't find the word "core" at all. No way!
      return false;
    }
  }//end databaseHasKeyAndVersion
  
  private ActionListener getExternalDataChangeListener(){
    return externalDataChangeListener;
  }//end getExternalDataChangeListener
}

