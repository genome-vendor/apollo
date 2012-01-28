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

import apollo.dataadapter.*;
import apollo.util.GuiUtil;

/**
 * <p>This is a panel which composes a number of the standard DataSourceConfiguration
 * panels - one for the results, one for variations, one for sequences.
 * The point is to allow the user to gracefully enter configuration for all of these
 * different databases. </p>
 *
 * <p>Which database the user is configuring is determined by a drop-down at the
 * top of the panel, which determines which one of the set of configuration 
 * panels is visible. </p>
 *
 * <p> The set/getProperties reads the properties of ALL the panels, prefacing
 * the properties of all except the "default" panel with a string, like "sequence:"
 * or "default:". This should allow these properties to be easily cleaved
 * off later, when we're trying to configure those databases.
**/
public class CompositeDataSourceConfigurationPanel extends JPanel
{

  private HashMap choosers = new HashMap();
  private JComboBox chooserDropdown;
  private ActionListener chooserActionListener;
  private String previousChooserKey;
  private String[] chooserNames;
  private String defaultChooserName; //the one that's visible at the start.
  public ActionListener interactionListener;
  private boolean linkChooserInteractions;
  
  /**
   * <p>When the user hits "find" on the _default_ data config panel,
   * we need to find all core ensembl databases, as well as 
   * variations. We then need to set the dbs we found
   * into the models on the dropdowns in their respective panels,
   * as efficiently as possible...note that I make direct mysql-specific
   * access here. A better design (delegating all this to the adapters)
   * is welcome.
   * This listener does all that. </p>
  **/
  public class DatabaseFindListener implements ActionListener{
    public void actionPerformed(ActionEvent event){
      populateEnsemblDatabases();
    }//end actionPerformed
  }//end DatabaseFindListener 

  /**
   * Exposes the chosen panel, hides the one that was previously chosen
  **/
  public class ChooserActionListener implements ActionListener{
    public void actionPerformed(ActionEvent event){
      String currentKey = (String)getChooserDropdown().getSelectedItem();
      
      if(getPreviousChooserKey()!= null){
        getChooser(getPreviousChooserKey()).setVisible(false);
      }
      
      getChooser(currentKey).setVisible(true);
      setPreviousChooserKey(currentKey);
      JDialog dialog = 
        (JDialog)
        SwingUtilities.getAncestorOfClass(
          JDialog.class, 
          CompositeDataSourceConfigurationPanel.this
        );
      
      dialog.pack();
      
    }
  }
  

  /**
   * Automatically takes the first chooser name passed in as the default.
   * This also links the choice of database on the default chooser to the remaining 
   * choosers - if you hit the "find" button on the default chooser, it will also
   * find the variation databases
  **/
  public CompositeDataSourceConfigurationPanel(String[] chooserNames){
    this(chooserNames, true);
  }//end CompositeDataSourceConfigurationPanel
  
  public CompositeDataSourceConfigurationPanel(
    String[] chooserNames, 
    boolean linkChooserInteractions
  ){
    this.chooserNames = chooserNames;
    this.linkChooserInteractions = linkChooserInteractions;
    setDefaultChooserName(chooserNames[0]);
    initialiseChoosers(chooserNames);
    buildGUI();
  }//end CompositeDataSourceConfigurationPanel
  
  public CompositeDataSourceConfigurationPanel(){
    this(new String[]{"default", "sequence", "variation"});
  }
  
  /**
   * Create each chooser and load into hashmap
  **/
  private void initialiseChoosers(String[] chooserNames){
    DataSourceConfigurationPanel panel;
    
    for(int i=0; i<chooserNames.length; i++){
      panel = new DataSourceConfigurationPanel();
      panel.setInteractionListener(getInteractionListener());
      
      if(
        getLinkChooserInteractions() &&
        chooserNames[i].equals(getDefaultChooserName())
      ){
        //
        //detach the find buttons' default listener
        panel.getFindButton().removeActionListener(panel.getHostListener());
        //attach the listener we want to fire for the default panel.
        panel.getFindButton().addActionListener(new DatabaseFindListener());
      }//end if
      
      panel.setBorder(BorderFactory.createTitledBorder("Configuration for: "+chooserNames[i]+" data source"));
      getChoosers().put(chooserNames[i], panel);
    }//end for
  }//end initialiseChoosers
  
  /**
   * Return properties from all choosers, prefixing each with its key
  **/
  public Properties getPrefixedProperties(){
    Properties returnProperties = new Properties();
    DataSourceConfigurationPanel panel;
    Properties subProperties;
    Iterator names;
    String chooserName;
    String name;
    String value;
    Iterator chooserNames = getChoosers().keySet().iterator();
    
    while(chooserNames.hasNext()){
      chooserName = (String)chooserNames.next();
      panel = getChooser(chooserName);
      
      if(!chooserName.equals(getDefaultChooserName())){
        subProperties = panel.getProperties();
        names = subProperties.keySet().iterator();
        while(names.hasNext()){
          name = (String)names.next();
          value = (String)subProperties.getProperty(name);
          name = chooserName+"."+name;
          returnProperties.put(name, value);
        }//end while
        
      }else{
        returnProperties.putAll(panel.getProperties());
      }//end if
    }//end while
    
    return returnProperties;
  }//end getPrefixedProperties
  
  /**
   * Set (suitably prefixed) properties onto each chooser.
   * If it doesn't FIND any properties with the prefix it's looking for - 
   * e.g. "sequence" or "variation" etc, then it will just set the input properties
   * onto that chooser.
  **/
  public void setProperties(Properties properties){
    //    String property;
    DataSourceConfigurationPanel panel;
    Iterator names;
    Properties subProperties;
    String chooserName;
    String name;
    String value;
    String[] chooserNames = getChooserNames();
    boolean giveUp = false; //set this when we want to stop trying to set properties on subsequent adapters
    //    String signature = properties.getProperty("signature");
    //    String sig2 = properties.getProperty("variation.signature");
    
    //
    //Say the chooser has name "variation", then we'll filter out all
    //input properties beginning with "variation". If any exist, we will
    //set those onto the variation-chooser. Otherwise we'll just set all input
    //properties onto the variation-chooser...repeat for other config choosers
    for(int i=0; i<chooserNames.length && !giveUp; i++){
      chooserName = (String)chooserNames[i];
      
      names = properties.keySet().iterator();
      panel = getChooser(chooserName);
      subProperties = new Properties();
      chooserName = chooserName+".";
      
      while(names.hasNext()){
        name = (String)names.next();
        value = properties.getProperty(name);
        if(name.startsWith(chooserName)){
          name = name.substring(chooserName.length());
          subProperties.put(name, value);
        }//end if
      }//end while
      
      if(subProperties.size() >0){
        panel.setProperties(subProperties);
        //
        //are we seeing databases? If so, continue. Otherwise, stop - 
        //something is most probably not right...
        /*
        if(panel.getEnsemblDatabaseDropdown().getModel().getSize() <= 1){
          giveUp = true;
        }//end if
         */
      }else{
        panel.setProperties(properties);
        /*
        if(panel.getEnsemblDatabaseDropdown().getModel().getSize() <= 1){
          giveUp = true;
        }//end if
         */
      }
    }//end while
    
  }//end setProperties
  
  private void buildGUI(){
    chooserDropdown = new JComboBox();
    chooserDropdown.setPreferredSize(new Dimension(300,20));
    chooserDropdown.setModel(
      new DefaultComboBoxModel(new Vector(getChoosers().keySet()))
    );
    if(getChoosers().containsKey(getDefaultChooserName())){
      chooserDropdown.setSelectedItem(getDefaultChooserName());
      setPreviousChooserKey(getDefaultChooserName());
    }//end if
    chooserActionListener = new ChooserActionListener();
    chooserDropdown.addActionListener(
      getChooserActionListener()
    );
    
    JPanel dropdownPanel = new JPanel();
    dropdownPanel.setLayout(new FlowLayout());
    dropdownPanel.add(new JLabel("Data Source:"));
    dropdownPanel.add(chooserDropdown);
    
    setLayout(new GridBagLayout());
    add(dropdownPanel, GuiUtil.makeConstraintAt(0,0,1));
    for(int i=0;i<getChooserNames().length;i++){
      if(getDefaultChooserName().equals(getChooserNames()[i])){
        getChooser(getChooserNames()[i]).setVisible(true);
      }else{
        getChooser(getChooserNames()[i]).setVisible(false);
      }//end if
      add(getChooser(getChooserNames()[i]), GuiUtil.makeConstraintAt(0,i+1,1));
    }//end for
  }//end buildGUI

  private String[] getChooserNames(){
    return chooserNames;
  }//end getChooserNames
  
  private String getPreviousChooserKey(){
    return previousChooserKey;
  }
  
  private void setPreviousChooserKey(String key){
    previousChooserKey = key;
  }
  
  public DataSourceConfigurationPanel getChooser(String key){
    return (DataSourceConfigurationPanel)choosers.get(key);
  }//end getChooser

  private HashMap getChoosers(){
    return choosers;
  }//end getChoosers

  private JComboBox getChooserDropdown(){
    return chooserDropdown;
  }//end getChooserDropdown
  
  public String getSelectedChooser(){
    return (String)getChooserDropdown().getSelectedItem();
  }//end getSelectedChooser
  
  private ActionListener getChooserActionListener(){
    return chooserActionListener;
  }//end getChooserActionListener

  /**
   * The default interaction listener will do nothing.
  **/
  public ActionListener getInteractionListener(){
    if(interactionListener == null){
      interactionListener =  new ActionListener(){
        public void actionPerformed(ActionEvent e){
        }
      };
    }//end if

    return interactionListener;
  }//end getInteractionListener

  /**
   * Any listener passed in here is actually set as the interaction listener on
   * on each component configuration panel.
  **/
  public void setInteractionListener(ActionListener listener){
    interactionListener = listener;
    for(int i=0;i<getChooserNames().length;i++){
      getChooser(getChooserNames()[i]).setInteractionListener(listener);
    }//end for
  }//end setInteractionListener
  
  public static void main(String[] args){
    JFrame frame = new JFrame();
    frame.getContentPane().add(new CompositeDataSourceConfigurationPanel(), BorderLayout.CENTER);
    frame.pack();
    frame.show();
  }
  
  private void setDefaultChooserName(String name){
    defaultChooserName = name;
  }//end setDefaultChooserName
  
  private String getDefaultChooserName(){
    return defaultChooserName;
  }//end getDefaultChooserName
  
  public void populateEnsemblDatabases(){
    Properties defaultPanelProperties = 
      getChooser(getDefaultChooserName()).getProperties();
    
    String jdbcDriver = defaultPanelProperties.getProperty(StateInformation.JDBC_DRIVER);
    String host = defaultPanelProperties.getProperty(StateInformation.HOST);
    String port = defaultPanelProperties.getProperty(StateInformation.PORT);
    String url = "jdbc:mysql://" + host + ":" + port + "/";
    String user = defaultPanelProperties.getProperty(StateInformation.USER);
    String password = defaultPanelProperties.getProperty(StateInformation.PASSWORD);
    java.util.List coreList = new ArrayList();
    Iterator chooserNames;
    String chooserName;
    DataSourceConfigurationPanel chooser;

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
      return ;
    }

    if(password == null){
      password = "";
    }

    initialiseDatabaseDriver(defaultPanelProperties);
    
    try{
      java.sql.Connection conn = DriverManager.getConnection(url,user,password);
      Statement statement = conn.createStatement();
      ResultSet databases = statement.executeQuery("show databases");
      String databaseName;
      
      while(databases.next()){
        databaseName = databases.getString(1);
        coreList.add(databaseName);
      }//end while
      coreList.add(0,"");

      //
      //walk every chooser. If we find choosers labelled "default", "variation" etc
      //then populate them appropriately. Populate all other choosers with the "core"
      //list.
      chooserNames = getChoosers().keySet().iterator();
      while(chooserNames.hasNext()){
        chooserName = (String)chooserNames.next();
        chooser = (DataSourceConfigurationPanel)getChoosers().get(chooserName);
        
        chooser.detachEnsemblDatabaseDropdownListener();
        chooser
          .getEnsemblDatabaseDropdown()
          .setModel(new DefaultComboBoxModel(new Vector(coreList)));
        chooser.attachEnsemblDatabaseDropdownListener();
      }//end while
      

    }catch(SQLException exception){
      JOptionPane.showMessageDialog(
        null,
        "I cannot connect to the MySQL instance on the host to find the databases: \n"+host+":"+port+" user: "+user+" - \n "+
        exception.getMessage()
      );
    } 
  }//end populateEnsemblDatabases

  protected void initialiseDatabaseDriver(Properties defaultPanelProperties){
    String jdbcDriver = defaultPanelProperties.getProperty(StateInformation.JDBC_DRIVER);
    String host = defaultPanelProperties.getProperty(StateInformation.HOST);
    String port = defaultPanelProperties.getProperty(StateInformation.PORT);

    if(
      jdbcDriver == null ||
      jdbcDriver.trim().length() <= 0 ||
      host == null ||
      host.trim().length() <= 0 ||
      port == null ||
      port.trim().length() <= 0
    ){
      return ;
    }

    try{
      Class.forName(jdbcDriver).newInstance();
    }catch(IllegalAccessException exception){
      JOptionPane.showMessageDialog(
        null,
        "Cannot access the driver class "+jdbcDriver+"\n"+exception.getMessage()
      );
    }catch(InstantiationException exception){
      JOptionPane.showMessageDialog(
        null,
        "Cannot create the driver class "+jdbcDriver+"\n"+exception.getMessage()
      );
    }catch(ClassNotFoundException exception){
      JOptionPane.showMessageDialog(
        null,
        "Cannot find the driver class "+jdbcDriver+"\n"+exception.getMessage()
      );
    }//end try

  }//end initialiseDatabaseDriver
  

  /**
   * Whether to modify the contents of other choosers when the user
   * hits "find" on the default chooser
  **/
  private boolean getLinkChooserInteractions(){
    return linkChooserInteractions;
  }//end getLinkChooserInteractions
  
  /**
   * The input listener is added to each data source config panel. If the source of data is
   * changed, an action event is fired at the input listener (if it's present). This allows
   * any interested parties (like chromosome dropdowns) to reset themselves.
  **/
  public void addExternalDataSourceChangeListener(ActionListener listener){
    Iterator choosers = getChoosers().values().iterator();
    DataSourceConfigurationPanel chooser = null;
    while(choosers.hasNext()){
      chooser = (DataSourceConfigurationPanel)choosers.next();
      chooser.addExternalDataChangeListener(listener);
    }//end while
  }//end addExternalDataSourceChangeListener
}

