package apollo.dataadapter.synteny.builder;
import javax.swing.tree.*;

import apollo.dataadapter.NonFatalDataAdapterException;
import apollo.dataadapter.ensj.FatalException;
import apollo.dataadapter.ensj.NonFatalException;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.ArrayList;

public class Controller {
  public static String MOUSE_LEFT_CLICK_ON_TREE = "MOUSE_LEFT_CLICK_ON_TREE";
  public static String CREATE_BUTTON = "CREATE_BUTTON";
  public static String UPDATE_BUTTON = "UPDATE_BUTTON";
  public static String DELETE_BUTTON = "DELETE_BUTTON";
  public static String CREATE_SET_BUTTON = "CREATE_SET_BUTTON";
  public static String DELETE_SET_BUTTON = "DELETE_SET_BUTTON";
  public static String DROPDOWN_SELECT_SET = "DROPDOWN_SELECT_SET";
  public static String DROPDOWN_KEY_PRESSED = "DROPDOWN_KEY_PRESSED";
  
  Model model;
  View view;
  List listeners = new ArrayList();
  //
  //This is set to 'true' during a View.read(), and it stops
  //circular event-firing (eg view sets a dropdown value, which 
  //in turn fires another event, causing some unintented action 
  //(eg another read).
  private boolean _ignoreEvents;
  
  public Controller(Model model, View view) {
    this.model = model;
    this.view = view;
  }
  
  public void handleEvent(String eventKey){

    //this renders us deaf to further event firings till we've done this one
    //-- so no loops where our handling of this event fires further ones.
    setIgnoreEvents(true);
    
    try{
 // SMJS Added this call so that current state of selected adapter is
 //      saved before the event has a chance to blow away the adapter
 //      and recreate it from properties. Previously adapter state
 //      was often lost when that happened.
 //      I decided to add it here rather than in more specific
 //      locations because so many paths through the
 //      events can lead to adapter recreation
      getModel().updateSelectedModelFromAdapter();

      if(eventKey.equals(MOUSE_LEFT_CLICK_ON_TREE)){
        handleMouseLeftClickOnTree();
      }else if(eventKey.equals(CREATE_BUTTON)){
        handleCreateNode();
      }else if(eventKey.equals(UPDATE_BUTTON)){
        handleUpdateNode();
      }else if(eventKey.equals(DELETE_BUTTON)){
        handleDeleteNode();
      }else if(eventKey.equals(CREATE_SET_BUTTON)){
        handleCreateSet();
      }else if(eventKey.equals(DELETE_SET_BUTTON)){
        handleDeleteSet();
      }else if(eventKey.equals(DROPDOWN_SELECT_SET)){
        handleSetSelected();
      }else if(eventKey.equals(DROPDOWN_KEY_PRESSED)){
        handleDropDownKeyPress();
      }


    }catch(apollo.dataadapter.NonFatalDataAdapterException exception){
      if(getView()!= null){
        getModel().setMessage(exception.getMessage());
        getView().read(getModel());
      }else{
        throw exception;
      }
    }finally{
      setIgnoreEvents(false);   
    }
  }

  public void handleSetSelected(){

    String selectedSet = getModel().getSelectedAdapterSet();
    if(getView()!=null){
      selectedSet = getView().getSelectedSetName();
      if((selectedSet != null) && (selectedSet.trim().length()<=0)){
        selectedSet = null; 
      }
    }

    //
    //This odd safeguard because if you create a brand new set, the editable jcombobox
    //will (for some bizarre reason) fire an action event on loss of focus from the editable
    //component - causing the controller to think that something has been selected...
    if(getModel().getAdapterSets().get(selectedSet) == null){
      return;   
    }
        
    getModel().setSelectedAdapterSet(selectedSet);
    getModel().createAdaptersAndGUIs(selectedSet);
    
    getModel().setReloadAll();
    getView().read(getModel());
    
    if(selectedSet == null){
      getView().disableAdapterPanel();   
    }else{
      getView().enableAdapterPanel();
    }
    
    notifyListeners();
  }
  
  public void handleDropDownKeyPress(){
    
  }
  
  public void handleCreateSet(){


    String setName = getView().getNewSetName();
    if(getModel().getAdapterSets().get(setName) != null){
      throw new NonFatalDataAdapterException("An adapter set with this name already exists");   
    }
    getModel().addAdapterSet(setName);
    getModel().setSelectedAdapterSet(setName);

    getModel().toXML();
    setModel(Model.fromXML());
    
    getModel().createAdaptersAndGUIs(setName);    
    getModel().setReloadAll();    
    getView().read(getModel());
    getView().enableAdapterPanel();
    notifyListeners();
  }
  
  public void handleDeleteSet(){
    String set = getModel().getSelectedAdapterSet();
    getModel().getAdapterSets().remove(set);
    getModel().setSelectedAdapterSet(null);
    getModel().setSelectedAdapterName(null,null);
    getModel().toXML();
    setModel(Model.fromXML());
    
    getModel().setReloadAll();
    getView().disableAdapterPanel();
    getView().read(getModel());
    notifyListeners();
  }
  
  public void handleCreateNode(){
    //
    //Insert a new node following the selected node, if it's a child.
    //otherwise insert a new node at the beginning of the list.

    AdapterModel selectedModel = getModel().getSelectedAdapterModel();
    int selectedAdapterIndex;
    AdapterModel newModel;
    String set = getModel().getSelectedAdapterSet();

    
    selectedAdapterIndex = getModel().getSelectedAdapterIndex(set);
    newModel = getView().getNewAdapterModel(getModel());

    validateAdapterModel(newModel);
    
    getModel().getAdapterModels(set).add(selectedAdapterIndex+1, newModel);
    getModel().toXML();
    setModel(Model.fromXML());
    
    String setName = getModel().getSelectedAdapterSet();
    if(setName == null){
      throw new FatalException("Programming error: node updated without a selected set!");   
    }
    
    getModel().createAdaptersAndGUIs(setName);    
    getModel().setSelectedAdapterName(set,newModel.getName());

    getModel().setReloadAll();    
    getView().read(getModel());
    notifyListeners();
  }
  
  public void handleUpdateNode(){
    getView().update(getModel());
    AdapterModel selectedModel = getModel().getSelectedAdapterModel();
    

    try{
      validateAdapterModel(selectedModel);
    }catch(apollo.dataadapter.NonFatalDataAdapterException exception){
      setModel(Model.fromXML());
      getModel().setReloadAll();
      getView().read(getModel());
      notifyListeners();
      throw exception;
    }
    
    getModel().toXML();
    setModel(Model.fromXML());
    //The model no longer automatically re-creates the adapters
    //and guis: you have to do this for it when you think it needs to.
    String setName = getModel().getSelectedAdapterSet();
    if(setName == null){
      throw new FatalException("Programming error: node updated without a selected set!");   
    }
    getModel().createAdaptersAndGUIs(setName);
    
    getModel().setReloadAll();
    getView().read(getModel());
    notifyListeners();
  }
  
  public void handleDeleteNode(){
    getModel().setReloadAll();
    String set = getModel().getSelectedAdapterSet();
    if(
      getModel().getSelectedAdapterModel().getType().equals(AdapterModel.MAIN_TYPE) &&
      getModel().getAdapterModels(set).size() > 1
    ){
      throw new NonFatalDataAdapterException("Cant remove main adapter - it still has child adapters");
    }
    getModel().getAdapterModels(set).remove(getModel().getSelectedAdapterModel());
    getModel().toXML();
    setModel(Model.fromXML());

    String setName = getModel().getSelectedAdapterSet();
    if(setName == null){
      throw new FatalException("Programming error: node updated without a selected set!");   
    }
    getModel().createAdaptersAndGUIs(setName);
    
    getModel().setReloadAll();
    getView().read(getModel());
    notifyListeners();
  }
  
  public void handleMouseLeftClickOnTree(){
    //
    //Find the node, grab the adapter model by name, set the name, class & type into the text fields.
    DefaultMutableTreeNode node = view.getSelectedNode();
    String selectedNodeKey = null;
    String set = getModel().getSelectedAdapterSet();
    if(node != null){
      selectedNodeKey = (String)node.getUserObject();
      getModel().setSelectedAdapterName(set, selectedNodeKey);
      getModel().setReloadTree(false);
      getModel().setReloadGUIs(false);
      getView().read(getModel());
    }
  }

  private void setModel(Model model){
    this.model = model;
  }
  
  public Model getModel(){
    return model;
  }
  
  public View getView(){
    return view;
  }
  
  public static void main(String[] args){
    javax.swing.JFrame frame = new javax.swing.JFrame("Test");
    View view = new View();
    Model model = Model.fromXML();
    Controller controller = new Controller(model, view);
    view.setController(controller);
    view.initialiseGUI();
    frame.getContentPane().add(view, java.awt.BorderLayout.CENTER);
    frame.pack();
    frame.show();
    
    view.read(model);
  }
  
  
  public void addActionListener(ActionListener listener){
    listeners.add(listener);
  }
  
  public void removeActionListener(ActionListener listener){
    listeners.remove(listener);
  }
  
  public void clearActionListeners(){
    listeners.clear();
  }
  
  private List getListenerList(){
    return listeners;
  }  
  
  private void notifyListeners(){
    for(int i=0; i<getListenerList().size(); i++){
      ((ActionListener)getListenerList().get(i))
        .actionPerformed(
          new ActionEvent(this, 0, "Model reread")
        );
    }
  }
  
  private void validateAdapterModel(AdapterModel model){
    AdapterModel mainModel = null;
    String set = getModel().getSelectedAdapterSet();
    if(
      model.getName() == null ||
      model.getName().trim().length() <= 0
    ){
      throw new apollo.dataadapter.NonFatalDataAdapterException("New node must have a non-empty name");
    }
    
    for(int i=0; i<getModel().getAdapterModels(set).size(); i++){
      if(
        !model.equals(getModel().getAdapterModels(set).get(i)) &&
        model.getName().equals(((AdapterModel)getModel().getAdapterModels(set).get(i)).getName())
      ){
        throw new apollo.dataadapter.NonFatalDataAdapterException("New node must have a unique name");
      }
    }
    
    for(int i=0; i<getModel().getAdapterModels(set).size(); i++){
      if(
        ((AdapterModel)getModel().getAdapterModels(set).get(i)).getType().equals(AdapterModel.MAIN_TYPE)
      ){
        mainModel = (AdapterModel)getModel().getAdapterModels(set).get(i);
      }
    }
    
    //If we didn't find any main model, then you have to create one!
    if(mainModel == null && !model.getType().equals(AdapterModel.MAIN_TYPE)){
      throw new apollo.dataadapter.NonFatalDataAdapterException(
        "Programming error: The system must create one (and only one) 'MAIN_TYPE' adapter in the set"
      );
    }
    
    if(
      model.getType().equals(AdapterModel.MAIN_TYPE) &&
      mainModel != null &&
      !model.equals(mainModel)
    ){
      throw new apollo.dataadapter.NonFatalDataAdapterException(
        "Programming error: New node cannot have 'MAIN' type, as there is already a 'MAIN' adapter"
      );
    }
          
  }
  
  public void setIgnoreEvents(boolean value){
    _ignoreEvents = value; 
  }
  
  public boolean ignoreEvents(){
    return _ignoreEvents; 
  }
}

