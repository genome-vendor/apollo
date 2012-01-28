package apollo.dataadapter.synteny.builder;
import apollo.dataadapter.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.tree.*;

import apollo.util.GuiUtil;

public class View extends JPanel{
  private JSplitPane treeToAdapterSplitPane;
  private JSplitPane adapterToGUISplitPane;
  private JScrollPane treeScrollPane;
  
  private JTree tree;
  private Hashtable treeHash;
  private DefaultMutableTreeNode rootNode;
  
  private JPanel adapterPanel;
  private JPanel setPanel;
  private JPanel setAndAdapterPanel;
  private JScrollPane adapterPropertiesScrollPane;
  private JTable propertiesTable;
  private DefaultTableModel propertiesTableModel;

  private JComboBox setDropDown;
  private JButton createSetButton;
  private JButton deleteSetButton;
  private JLabel setLabel;

  private JTextField nameTextField;
  private JLabel nameLabel;
  private JComboBox adapterClassDropdown;
  private JLabel adapterClassLabel;
  private JComboBox adapterTypeDropdown;
  private JLabel adapterTypeLabel;
  private Controller controller;
  private JButton createButton;
  private JButton updateButton;
  private JButton deleteButton;
  
  private JPanel guiPanel;
  
  
  public View(){ 
  }
  
  public void initialiseGUI(){
    if(getController() == null){
      throw new NonFatalDataAdapterException("Error: attempt to initialise builder GUI without setting controller");
    }
    buildGUI();
  }
  
  private void buildGUI(){
    setLayout(new BorderLayout());
    //
    //Adapter panel
    adapterClassDropdown = new JComboBox();
    adapterClassDropdown.setPreferredSize(new Dimension(500,20));
    adapterClassDropdown.setMinimumSize(adapterClassDropdown.getPreferredSize());
    adapterClassLabel = new JLabel("Adapter class");
    adapterTypeDropdown = new JComboBox();
    adapterTypeDropdown.setPreferredSize(adapterClassDropdown.getPreferredSize());
    adapterTypeDropdown.setMinimumSize(adapterClassDropdown.getPreferredSize());
    adapterTypeLabel = new JLabel("Adapter type:");
    nameTextField = new JTextField();
    nameTextField.setPreferredSize(adapterClassDropdown.getPreferredSize());
    nameTextField.setMinimumSize(adapterClassDropdown.getPreferredSize());
    nameLabel = new JLabel("Name");

    createButton = new JButton("Create");
    addButtonEventRouter(createButton, Controller.CREATE_BUTTON);

    updateButton = new JButton("Update");
    addButtonEventRouter(updateButton, Controller.UPDATE_BUTTON);

    deleteButton = new JButton("Delete");
    addButtonEventRouter(deleteButton, Controller.DELETE_BUTTON);

    //
    //Properties table, inside a scrollpane
    //-- I have elected not to display this widget for now!
    propertiesTableModel = new DefaultTableModel(new Object[][]{}, new Object[]{"Key","Value"});
    propertiesTable = new JTable(propertiesTableModel);
    adapterPropertiesScrollPane = new JScrollPane(propertiesTable);
    adapterPropertiesScrollPane.setPreferredSize(new Dimension(200,100));
    
    setAndAdapterPanel = new JPanel(new GridLayout(2,1));
        
    setPanel = new JPanel();
    setPanel.setBorder(BorderFactory.createTitledBorder("Adapter Sets"));
    setPanel.setLayout(new GridBagLayout());
    setDropDown = new JComboBox();
    addDropDownSelectionEventRouter(setDropDown, Controller.DROPDOWN_SELECT_SET);
    addDropDownKeystrokeEventRouter(setDropDown, Controller.DROPDOWN_KEY_PRESSED);

    setLabel = new JLabel("Sets");
    
    createSetButton = new JButton("Create Set");
    addButtonEventRouter(createSetButton, Controller.CREATE_SET_BUTTON);
    deleteSetButton = new JButton("Delete Set");
    addButtonEventRouter(deleteSetButton, Controller.DELETE_SET_BUTTON);
    
    getSetDropDown().setPreferredSize(adapterClassDropdown.getPreferredSize());
    getSetDropDown().setEditable(true);
    setPanel.add(getSetLabel(), GuiUtil.makeConstraintAt(0,0,1));
    setPanel.add(getSetDropDown(), GuiUtil.makeConstraintAt(1,0,2));
    setPanel.add(getCreateSetButton(), GuiUtil.makeConstraintAt(1,1,1));
    setPanel.add(getDeleteSetButton(), GuiUtil.makeConstraintAt(2,1,1));
    
    adapterPanel = new JPanel();
    adapterPanel.setBorder(BorderFactory.createTitledBorder("Adapters for Set"));
    adapterPanel.setLayout(new GridBagLayout());
    adapterPanel.add(getNameLabel(), GuiUtil.makeConstraintAt(0,0,1));
    adapterPanel.add(getNameTextField(), GuiUtil.makeConstraintAt(1,0,3));
    adapterPanel.add(getAdapterClassLabel(), GuiUtil.makeConstraintAt(0,1,1));
    adapterPanel.add(getAdapterClassDropdown(), GuiUtil.makeConstraintAt(1,1,3));
    //adapterPanel.add(getAdapterTypeLabel(), GuiUtil.makeConstraintAt(0,2,1));
    //adapterPanel.add(getAdapterTypeDropdown(), GuiUtil.makeConstraintAt(1,2,2));
    //adapterPanel.add(adapterPropertiesScrollPane, GuiUtil.makeConstraintAt(0,3,3));
    adapterPanel.add(getCreateButton(), GuiUtil.makeConstraintAt(1,4,1));
    adapterPanel.add(getUpdateButton(), GuiUtil.makeConstraintAt(2,4,1));
    adapterPanel.add(getDeleteButton(), GuiUtil.makeConstraintAt(3,4,1));
    
    setAndAdapterPanel.add(setPanel);
    setAndAdapterPanel.add(adapterPanel);
    
    //
    //JTree inside a scrollpane
    rootNode = new DefaultMutableTreeNode("Composite");
    tree = new JTree(rootNode);
    addMouseEventRouter(tree, getController());
    treeScrollPane = new JScrollPane(tree);
    treeScrollPane.setPreferredSize(new Dimension(150,250));
    
    //
    //Put JTree and Adapter panel into a horizontally oriented splitpane
    
    //
    //Put Adapter panel & properties table into a vertically oriented splitpane
    treeToAdapterSplitPane = 
      new JSplitPane(
        JSplitPane.HORIZONTAL_SPLIT,
        treeScrollPane, 
        setAndAdapterPanel
      );

    //
    //build up the panel housing the chosen gui's
    guiPanel = new JPanel();
    
    guiPanel.setLayout(new CardLayout());
    //guiPanel.setPreferredSize(new Dimension(500,400));

    JScrollPane cardScrollPane = new JScrollPane(guiPanel);
    cardScrollPane.setPreferredSize(new Dimension(500,400));
    
    //
    //Put Tree-adapter split pane into a horizontally oriented splitpane along with 
    //a panel on the bottom for the adapter gui's
    adapterToGUISplitPane = 
      new JSplitPane(
        JSplitPane.VERTICAL_SPLIT,
        treeToAdapterSplitPane, 
        //guiPanel
        cardScrollPane
      );
    
    
    //
    //add splitpane into THIS panel
    add(adapterToGUISplitPane, BorderLayout.CENTER);
    
  }
  
  public void read(Model model){
    AdapterModel selectedModel = null;
    CardLayout layout;
    AdapterModel mainModel;
    String adapterKey;
    Properties modelProperties;
    java.util.List keyList;
    Iterator keyIterator;
    String key;
    String value;
    int row = 0;
  
    if(model.getMessage() != null){
      JOptionPane.showMessageDialog(this,model.getMessage());
      model.setMessage(null);
    }
    
    //
    //populate the two dropdowns with allowed classes and types.
    if(model.reloadAdapterPanel()){

      java.util.List sortedSets = new ArrayList(model.getAdapterSets().keySet());
      
      Collections.sort(sortedSets);
      sortedSets.add(0,null);
      getSetDropDown().setModel(
        new DefaultComboBoxModel(
          new Vector(sortedSets)
        )
      );
      
      String selectedSet = model.getSelectedAdapterSet();
      if(selectedSet != null){
        getSetDropDown().setSelectedItem(selectedSet); 
      }else{
        getSetDropDown().setSelectedIndex(0); 
      }
        
      getAdapterClassDropdown().setModel(
        new DefaultComboBoxModel(
          new Vector(model.getAllowedAdapterClasses())
        )
      );

      getAdapterTypeDropdown().setModel(
        new DefaultComboBoxModel(
          new Vector(model.getAllowedAdapterTypes())
        )
      );

      //
      //Get the selected adaptermodel and set name, class and type into the fields.
      selectedModel = model.getSelectedAdapterModel();
      if(selectedModel != null){
        
        getNameTextField().setText(selectedModel.getName());
        getAdapterClassDropdown().setSelectedItem(selectedModel.getAdapterClassName());
        getAdapterTypeDropdown().setSelectedItem(selectedModel.getType());
        
        row = 0;
        modelProperties = selectedModel.getProperties();
        keyList = new ArrayList(modelProperties.keySet());
        Collections.sort(keyList);
        keyIterator = keyList.iterator();
        
        ((DefaultTableModel)
          getPropertiesTable().getModel()
        ).setRowCount(
          modelProperties.keySet().size()
        );

        while(keyIterator.hasNext()){
          key = (String)keyIterator.next();
          value = modelProperties.getProperty(key);
          getPropertiesTable().getModel().setValueAt(key, row, 0);
          getPropertiesTable().getModel().setValueAt(value, row, 1);
          row++;
        }
      }else{
        getNameTextField().setText("");
        getAdapterClassDropdown().setSelectedIndex(0);
        getAdapterTypeDropdown().setSelectedIndex(0);
        ((DefaultTableModel)getPropertiesTable().getModel()).setRowCount(0);
      }
    }
     
    
    if(model.reloadTree()){
      //find the main type in the model.
      //create a box for it & add to layout.
      treeHash = new Hashtable();

      getRootNode().removeAllChildren();
      ((DefaultTreeModel)getTree().getModel()).reload();
      if(model.getSelectedAdapterSet() != null){
        mainModel = getMainAdapterModel(model);
        java.util.List childModels = getChildAdapterModels(model);

        if(mainModel != null){
          createMainModelTreeNode(mainModel);
        }
        
        for(int i=0; i<childModels.size(); i++){
          createChildTreeNode((AdapterModel)childModels.get(i));
        }

        ((DefaultTreeModel)getTree().getModel()).reload();
        int numberOfRows = getTree().getRowCount();
        for(int i=0; i<numberOfRows; i++){
            getTree().expandRow(i);
        }
        if (model.getSelectedAdapterModel() != null) {
          findNamedNodeInTree(model.getSelectedAdapterModel().getName());
        }
      }
    }
    
    if(model.reloadGUIs()){
      //
      //Add adapter gui panels into the layout.
      getGUIPanel().removeAll();

      if(model.getSelectedAdapterSet() != null){
        String setName = model.getSelectedAdapterSet(); 
        if(setName != null){
          java.util.List adapterModels = model.getAdapterModels(setName); 
          for(int i=0; i<adapterModels.size(); i++){
            adapterKey = ((AdapterModel)adapterModels.get(i)).getName();
            getGUIPanel()
              .add(
                (Component)
                model.getAdapterGUIMap().get(adapterKey),
                adapterKey
              );
          }

        }
      }
      
      
      getGUIPanel().invalidate();
      getGUIPanel().validate();
      getGUIPanel().repaint();

    }
    
    String setName = model.getSelectedAdapterSet(); 
    if(setName != null){
      java.util.List adapterModels = model.getAdapterModels(setName);  
      layout = (CardLayout)getGUIPanel().getLayout();
      if(adapterModels.size() > 0 && selectedModel != null){
        layout.show(getGUIPanel(), selectedModel.getName());
      }
    }

    model.setReloadAll();

  }

  protected DefaultMutableTreeNode findTreeNode (String name) {
    return ((treeHash == null || name == null) ?
            null : (DefaultMutableTreeNode)treeHash.get(name));
  }

  public void findNamedNodeInTree (String name) {
    DefaultMutableTreeNode dmtn = null;
    if (name != null) {
      dmtn  = findTreeNode(name);
      if (dmtn!=null) {
        DefaultTreeModel       model = (DefaultTreeModel)tree.getModel();
        TreeNode [] objs = model.getPathToRoot(dmtn);

        TreePath treep = new TreePath(objs);

        tree.paintImmediately(0,0,tree.getSize().width,tree.getSize().height);

        TreeSelectionModel selModel = tree.getSelectionModel();
        selModel.addSelectionPath(treep);

        tree.scrollPathToVisible(treep);
      }
    } else {
      tree.setSelectionPath(null);
    }
  }


  public DefaultMutableTreeNode getSelectedNode(){
    if(getTree().getSelectionPath() != null){
      return (DefaultMutableTreeNode)getTree().getSelectionPath().getLastPathComponent();
    }else{
      return null;
    }
  }
  
  public TreePath getSelectedTreePath(){
    if(getTree().getSelectionPath() != null){
      return getTree().getSelectionPath();
    }else{
      return null;
    }
  }

  private Controller getController(){
    return controller;
  }
  
  public void setController(Controller newValue){
    controller = newValue;
  }
  
  private void createMainModelTreeNode(AdapterModel mainModel){
    MutableTreeNode node = new DefaultMutableTreeNode(mainModel.getName());
    ((DefaultMutableTreeNode)getTree().getModel().getRoot()).add(node);

    treeHash.put(mainModel.getName(),node);
  }
  
  private void createChildTreeNode(AdapterModel childModel){
    DefaultMutableTreeNode mainNode = 
      (DefaultMutableTreeNode)
      ((DefaultMutableTreeNode)getTree().getModel().getRoot()).getChildAt(0);
    
    DefaultMutableTreeNode node = new DefaultMutableTreeNode(childModel.getName());
    mainNode.add(node);
    treeHash.put(childModel.getName(),node);
  }
  
  public void update(Model model){
    model.getSelectedAdapterModel().setName(getNameTextField().getText());
    model.getSelectedAdapterModel().setAdapterClassName(
      (String)
      getAdapterClassDropdown().getSelectedItem()
    );
    model.getSelectedAdapterModel().setType(
      (String)
      getAdapterTypeDropdown().getSelectedItem()
    );
  }
  
  public AdapterModel getNewAdapterModel(Model model){
    String newType;
    String set = model.getSelectedAdapterSet();
    
    if(model.getAdapterModels(set).size() > 0) {
    	  newType = AdapterModel.CHILD_TYPE;
    }else {
    	  newType = AdapterModel.MAIN_TYPE;
    }
    
    AdapterModel newModel = 
    	  new AdapterModel(
        getNameTextField().getText(), 
        (String)getAdapterClassDropdown().getSelectedItem(),
        newType
      );
       
    return newModel;
  }
  
  public String getNewSetName(){
    return getSetDropDown().getSelectedItem().toString(); 
  }
  
  public String getSelectedSetName(){
    if(getSetDropDown() != null && getSetDropDown().getSelectedItem()!=null){
    	return getSetDropDown().getSelectedItem().toString();
    }else{
      return null;   
    }
  }
  
  private java.util.List getChildAdapterModels(Model model){
    String set = model.getSelectedAdapterSet();
    Iterator adapterModels = model.getAdapterModels(set).iterator();
    java.util.List returnList = new ArrayList();
    AdapterModel adapterModel;
    while(adapterModels.hasNext()){
      adapterModel = (AdapterModel)adapterModels.next();
      if(adapterModel.getType().equals(AdapterModel.CHILD_TYPE)){
        returnList.add(adapterModel);
      }
    }
    
    return returnList;
  }
  
  private AdapterModel getMainAdapterModel(Model model){
    String set = model.getSelectedAdapterSet();
    Iterator adapterModels = model.getAdapterModels(set).iterator();
    AdapterModel adapterModel;
    while(adapterModels.hasNext()){
      adapterModel = (AdapterModel)adapterModels.next();
      if(adapterModel.getType().equals(AdapterModel.MAIN_TYPE)){
        return adapterModel;
      }
    }
    
    return null;
  }
  
  private JPanel getGUIPanel(){
    return guiPanel;
  }
  
  private DefaultMutableTreeNode getRootNode(){
    return rootNode;
  }
  
  private JScrollPane getTreeScrollPane(){
    return treeScrollPane;
  }
  
  private JTree getTree(){
    return tree;
  }
  
  private JPanel getAdapterPanel(){
    return adapterPanel;
  }
  
  public void disableAdapterPanel(){
    getNameTextField().setEnabled(false);
    getAdapterClassDropdown().setEnabled(false);
    getCreateButton().setEnabled(false);
    getDeleteButton().setEnabled(false);
    getUpdateButton().setEnabled(false);
  }
  
  public void enableAdapterPanel(){
    getNameTextField().setEnabled(true);
    getAdapterClassDropdown().setEnabled(true);
    getCreateButton().setEnabled(true);
    getDeleteButton().setEnabled(true);
    getUpdateButton().setEnabled(true);
  }
  
  private JScrollPane getAdapterPropertiesScrollPane(){
    return adapterPropertiesScrollPane;
  }
  
  private JTable getPropertiesTable(){
    return propertiesTable;
  }

  private JLabel getNameLabel(){
    return nameLabel;
  }
  
  private JTextField getNameTextField(){
    return nameTextField;
  }
  
  private JComboBox getAdapterClassDropdown(){
    return adapterClassDropdown;
  }
  
  private JComboBox getAdapterTypeDropdown(){
    return adapterTypeDropdown;
  }
  
  private JLabel getAdapterClassLabel(){
    return adapterClassLabel;
  }
  
  private JLabel getAdapterTypeLabel(){
    return adapterTypeLabel;
  }

  private JSplitPane gettreeToAdapterSplitPane(){
    return treeToAdapterSplitPane;
  }
  
  private JSplitPane getAdapterToGUISplitPane(){
    return adapterToGUISplitPane;
  }

  private JButton getCreateButton(){
    return createButton;
  }
  
  private JButton getUpdateButton(){
    return updateButton;
  }
  
  private JButton getDeleteButton(){
    return deleteButton;
  }
  
  private void addButtonEventRouter(JButton button, final String eventKey){
    ActionListener listener = 
      new ActionListener(){
        public void actionPerformed(ActionEvent event){
          if(getController().ignoreEvents()){
            return; 
          }
          getController().handleEvent(eventKey);
        }
      };
      
    button.addActionListener(listener);
  }
  
  private void addDropDownSelectionEventRouter(JComboBox dropDown, final String eventKey){
    ActionListener listener = 
      new ActionListener(){
        public void actionPerformed(ActionEvent event){
          if(getController().ignoreEvents()){
            return; 
          }
          getController().handleEvent(eventKey);
        }
      };
      
    dropDown.addActionListener(listener);
  }
  
  private void addDropDownKeystrokeEventRouter(JComboBox dropDown, final String eventKey){
    KeyListener listener = 
      new KeyListener(){
        public void keyTyped(KeyEvent event){
          if(getController().ignoreEvents()){
            return; 
          }
          getController().handleEvent(eventKey);
        }
        public void keyPressed(KeyEvent event){
          if(getController().ignoreEvents()){
            return; 
          }
          getController().handleEvent(eventKey);
        }
        public void keyReleased(KeyEvent event){
          if(getController().ignoreEvents()){
            return; 
          }
          getController().handleEvent(eventKey);
        }
      };
      
    dropDown.addKeyListener(listener);
  }
  
  private void addMouseEventRouter(JTree tree, final Controller controller){
    MouseListener mouseListener = 
      new MouseAdapter() {
         public void mousePressed(MouseEvent event){
           if(getController().ignoreEvents()){
             return; 
           }
           getController().handleEvent(Controller.MOUSE_LEFT_CLICK_ON_TREE);
         }
      };
      
    tree.addMouseListener(mouseListener);
  }
  
  private JComboBox getSetDropDown(){
    return setDropDown; 
  }
  
  private JButton getCreateSetButton(){
    return createSetButton; 
  }
  
  private JButton getDeleteSetButton(){
    return deleteSetButton; 
  }
  
  private JLabel getSetLabel(){
    return setLabel; 
  }
}


