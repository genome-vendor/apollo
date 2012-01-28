package apollo.dataadapter.synteny.builder;
import apollo.dataadapter.*;
import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.NonFatalException;

import java.io.*;
import java.util.*;

import javax.swing.tree.TreePath;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import org.bdgp.io.*;
import org.bdgp.swing.*;

/**
 * This contains an internal representation of the parent & child
 * adapters that the builder allows you to compose. The main internals
 * are
 * <ul>
 * <li> A map of adapters () stored by key (a user-given name)</li>
 * <li> A map of adapter GUI's (), also stored by key. </li>
 * <li> Functions to read / write the model from disk (where it's stored as
 * adapterbuilder.conf) -- toXML(), fromXML().
 * </li> A central bit where the actual adapters and their gui's are created
 * based on the stored information, and then inserted into the maps - 
 * createAdaptersAndGUIs.</li>
 * </ul>
**/
public class Model {
  private List allowedAdapterClasses;
  private List allowedAdapterTypes;
  private HashMap adapterSets;
  private List adapterModels;
  private AdapterModel selectedAdapterModel;

  public static String MODEL = "model";
  
  public static String ADAPTER_CLASSES = "adapter_classes";
  public static String CLASS = "class";

  public static String ADAPTER_TYPES = "adapter_types";
  
  public static String ADAPTER = "adapter";
  public static String ADAPTER_SET = "adapter_set";
  public static String NAME = "name";
  public static String TYPE = "type";
  public static String PROPERTY = "property";
  public static String KEY = "key";
  public static String VALUE = "value";
  public static String DEFAULT = "default";
  
  public static String SELECTED_SET = "selected_set";
  public static String SELECTED_NODE = "selected_node";
  
  private HashMap adapterMap = new HashMap();
  private HashMap adapterGUIMap = new HashMap();
  private boolean reloadTree = true;
  private boolean reloadAdapterPanel = true;
  private boolean reloadGUIs = true;
  private String message;
  
  //
  //The following instance variables store graphical state: keep them on here for the
  //time being
  private TreePath _treePath;
  private String _selectedAdapterSet;
  
  
  public Model() {
    adapterModels = new ArrayList();
    allowedAdapterClasses = new ArrayList();
    allowedAdapterTypes = new ArrayList();
    adapterSets = new HashMap();
  }
  
  public List getAdapterModels(String setName){
  	List adapterModels = (List)getAdapterSets().get(setName);
    return adapterModels;
  }
  
  public void addAdapterModel(String setName, AdapterModel model){
    List set = (List)getAdapterSets().get(setName);
    if(set == null){
      set = new ArrayList();
      getAdapterSets().put(setName, set);
    }
    set.add(model);
  }

  public void addAdapterSet(String setName){
    List set = new ArrayList();
    getAdapterSets().put(setName, set);
  }

  /**
   * Creates an instance of a Model from an XML file (which must be in 
   * adapterbuilder.conf)
   * <ol>
   * <li> explicitly loads a file APOLLO_ROOT/conf/adapterbuilder.conf create a new Model instance:</li>
   * <li> populateAllowedAdapterClasses: vector read off XML populateAllowedAdapterTypes: vector read off XML</li>
   * <li> populate(the actual) AdapterModels: AdapterModel = name, type, class, Properties - get an array of these
   * into the return Model</li>
   * <li>create (the actual) Adapters and GUIs</li>
   * </ol>
   * This is called when the BuilderAdapter is first created.
   * @return apollo.dataadapter.synteny.builder.Model
   */
  public static Model fromXML(){
    String fileName = System.getProperty("APOLLO_ROOT");
    fileName = fileName +"/conf/adapterbuilder.conf";
    File file;
    DocumentBuilder builder;
    Document document;
    Model returnModel = new Model();
    AdapterModel adapterModel;
    
    Element root;
    NodeList nodeList;
    NodeList childNodeList;
    NodeList classNodeList;
    NodeList adapterNodeList;
    NodeList propertyNodeList;
    Node textNode;
    String name = null;
    String textOfInterest = null;
    String className = null;
    String type = null;
    String key = null;
    String value = null;
    Set adapterKeySet = null;

    try{
      file = new File(fileName);

      if(!file.exists()){
        throw new NonFatalDataAdapterException(
          "The file: <APOLLO_ROOT>/conf/adapterbuilder.conf doesn't exist - can't create a Model"
        );
      }

      builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      document = builder.parse(new File(fileName));
      
      //
      //Check that the root of the model is <model>
      root = document.getDocumentElement();
      
      if(!root.getTagName().equals(MODEL)){
        throw new NonFatalDataAdapterException(
          "I was expecting a config document with '<model>' root - I found a root of "+root.getTagName()
        );
      }
      
      
      returnModel.populateAllowedAdapterClasses(root);
      
      returnModel.populateAllowedAdapterTypes(root);
      
      returnModel.populateAdapterSetsAndModels(root);
      
      returnModel.populateNodeSelections(root);
      
      //returnModel.createAdaptersAndGUIs();
      
      return returnModel;
    }catch(SAXException exception){
      throw new NonFatalDataAdapterException(exception.getMessage());
    }catch(IOException exception){
      throw new NonFatalDataAdapterException(exception.getMessage());
    }catch(ParserConfigurationException exception){
      throw new NonFatalDataAdapterException(exception.getMessage());
    }
  }
  
  private void populateNodeSelections(Element root){
    NodeList selectedNodes = root.getElementsByTagName(SELECTED_SET);
    Element selectedNode;
    Node textNode;
    String setName;
    String name;
    
    if(selectedNodes.getLength() > 0){
    	selectedNode = (Element)selectedNodes.item(0);
    	textNode = selectedNode.getFirstChild();
    	setName = textNode.getNodeValue();
      setSelectedAdapterSet(setName);
      
      selectedNodes = root.getElementsByTagName(SELECTED_NODE);
      
      if(selectedNodes.getLength() > 0){
        selectedNode = (Element)selectedNodes.item(0);
        textNode = selectedNode.getFirstChild();
        name = textNode.getNodeValue();
        setSelectedAdapterName(setName, name);
      }
    }
    
  }

  private void populateAllowedAdapterClasses(Element root){
    NodeList nodeList;
    NodeList childNodeList;
    String textOfInterest = null;
    
    //
    //Get each <adapter> node
    nodeList = root.getElementsByTagName(ADAPTER_CLASSES);
    if(nodeList.getLength() >0){
      childNodeList = nodeList.item(0).getChildNodes();
      for(int j=0; j<childNodeList.getLength(); j++){
        if(childNodeList.item(j).getNodeName().equals(CLASS)){
          textOfInterest = childNodeList.item(j).getFirstChild().getNodeValue();
          getAllowedAdapterClasses().add(textOfInterest);
        }
      }
    }
    
    getAllowedAdapterClasses().add("");
  }
  
  private void populateAllowedAdapterTypes(Element root){
    NodeList nodeList;
    NodeList childNodeList;
    String textOfInterest = null;
    
    getAllowedAdapterTypes().add("");
    
    //
    //Get each <adapter> node
    nodeList = root.getElementsByTagName(ADAPTER_TYPES);
    if(nodeList.getLength() >0){
      childNodeList = nodeList.item(0).getChildNodes();
      for(int j=0; j<childNodeList.getLength(); j++){
        if(childNodeList.item(j).getNodeName().equals(TYPE)){
          textOfInterest = childNodeList.item(j).getFirstChild().getNodeValue();
          getAllowedAdapterTypes().add(textOfInterest);
        }
      }
    }
  }
  
  private void populateAdapterSetsAndModels(Element root){
    NodeList nodeList;
    NodeList adapterSetNodeList;
    Element setNode;
    NodeList nameNodeList;
    Element nameNode;
    NodeList adapterModelsNodeList;

    String name = null;
    String textOfInterest = null;
    
    nodeList = root.getElementsByTagName(ADAPTER_SET);
    if(nodeList.getLength() > 0){
      for(int i = 0; i<nodeList.getLength(); i++){
        setNode = (Element)nodeList.item(i);
        
        //For each adapter set, isolate its name
        nameNodeList = setNode.getElementsByTagName(NAME);
        nameNode = (Element)nameNodeList.item(0);
        Node textNode = nameNode.getFirstChild();
        name = textNode.getNodeValue();
        addAdapterSet(name);
        //now isolate the 'adapter' nodes for that set
        adapterModelsNodeList = setNode.getElementsByTagName(ADAPTER);
      	addAdapterModelsForSet(name, adapterModelsNodeList);
      }
    }else{
    	//we are reading an 'old-style' file - no adapter sets, so create
      //a default set, and it will be converted into a 'new-style' set
      //from henceforth.
      adapterModelsNodeList = root.getElementsByTagName(ADAPTER);
      addAdapterModelsForSet(DEFAULT, adapterModelsNodeList);
    }
        
  	
  }
  
  private void addAdapterModelsForSet(String setName, NodeList nodeList){
    NodeList childNodeList;
    NodeList adapterNodeList;
    NodeList propertyNodeList;
    Node textNode;
    String name = null;
    String textOfInterest = null;
    String className = null;
    String type = null;
    AdapterModel adapterModel = null;
    String key = null;
    String value = null;
    Properties adapterProperties;
    
    for(int i = 0; i<nodeList.getLength(); i++){
      name = "dummy_name";
      className = "dummy_class";
      type = "dummy_type";
      adapterProperties = new Properties();
      
      adapterNodeList = nodeList.item(i).getChildNodes();
      
      for(int j = 0; j<adapterNodeList.getLength(); j++){
        if(adapterNodeList.item(j).getNodeName().equals(NAME)){
          textNode = adapterNodeList.item(j).getFirstChild();
          if(textNode != null){
            name = textNode.getNodeValue();
          }
        }

        if(adapterNodeList.item(j).getNodeName().equals(TYPE)){
          textNode = adapterNodeList.item(j).getFirstChild();
          if(textNode != null){
            type = textNode.getNodeValue();
          }
        }

        if(adapterNodeList.item(j).getNodeName().equals(CLASS)){
          textNode = adapterNodeList.item(j).getFirstChild();
          if(textNode != null){
            className = textNode.getNodeValue();
          }
        }

        if(adapterNodeList.item(j).getNodeName().equals(PROPERTY)){
          key = null;
          value = null;
          propertyNodeList = adapterNodeList.item(j).getChildNodes();
          
          for(int k=0; k<propertyNodeList.getLength(); k++){
            if(propertyNodeList.item(k).getNodeName().equals(KEY)){
              textNode = propertyNodeList.item(k).getFirstChild();
              if(textNode != null){
                key = textNode.getNodeValue();
              }//end if
            }//end if
            
            if(propertyNodeList.item(k).getNodeName().equals(VALUE)){
              textNode = propertyNodeList.item(k).getFirstChild();
              if(textNode != null){
                value = textNode.getNodeValue();
              }//end if
            }//end if
          }//end for PROPERTY NODE CHILDREN
          
          if(key != null && value != null){
            adapterProperties.setProperty(key, value);
          }
        }//end if PROPERTY NODE

      }//end for ADAPTER NODE CHILDREN

      if((name == null) || (name.trim().length() < 0) || (type == null) || (type.trim().length() < 0)){
        throw new NonFatalDataAdapterException(
          "Found an adapter model with a null/missing name or type"
        );
      }//end if

      adapterModel = new AdapterModel(name, className, type);
      adapterModel.setProperties(adapterProperties);

      addAdapterModel(setName, adapterModel);
    }//end for ADAPTER NODE CHILDREN
  }

  public void createAdaptersAndGUIs(String setName){
    Set adapterKeySet = null;
    
    getAdapterMap().clear();
    getAdapterGUIMap().clear();
      
    String key;
    String className=null;    
    Class adapterClass;
    AdapterModel model;
    org.bdgp.swing.AbstractDataAdapterUI gui;
    
    //
    //walk the models, create an adapter for each, and a gui for each.
    //add to the respective maps.
    
    if(setName == null){
      return;   
    }
    
    try{
      for(int i=0; i<getAdapterModels(setName).size(); i++){
        model = ((AdapterModel)getAdapterModels(setName).get(i));
        key = model.getName();
        className = model.getAdapterClassName();
        adapterClass = Class.forName(className);
        AbstractApolloAdapter adapter = (AbstractApolloAdapter)adapterClass.newInstance();
        
        gui = (org.bdgp.swing.AbstractDataAdapterUI) adapter.getUI(ApolloDataAdapterI.OP_READ_DATA);
        gui.setDataAdapter(adapter);
        getAdapterMap().put(key, adapter);
        getAdapterGUIMap().put(key, gui);

        if(model.getType().equals(AdapterModel.CHILD_TYPE) && gui instanceof apollo.dataadapter.ensj.EnsJAdapterGUI){
          EnsJAdapterGUI egui = (EnsJAdapterGUI)gui;
          egui.getController().handleEventForKey(apollo.dataadapter.ensj.controller.Controller.HIDE_OR_SHOW_DATABASE);
          egui.getController().handleEventForKey(apollo.dataadapter.ensj.controller.Controller.HIDE_OR_SHOW_TYPES);
          egui.getController().handleEventForKey(apollo.dataadapter.ensj.controller.Controller.HIDE_OR_SHOW_LOCATION);
        }
        
        Properties propertiesFromAdapterModel = model.getProperties();
        if (propertiesFromAdapterModel.size() > 0) {
          gui.setProperties(propertiesFromAdapterModel);
        }
      }
    }catch(IllegalAccessException exception){
      throw new NonFatalDataAdapterException("Couldn't access adapter class: "+className);
    }catch(InstantiationException exception){
      throw new NonFatalDataAdapterException("Couldn't instantiate adapter class: "+className);
    }catch(ClassNotFoundException exception){
      throw new NonFatalDataAdapterException("Couldn't find adapter class: "+className);
    }
  }
  
  public List getAllowedAdapterClasses(){
    return allowedAdapterClasses;
  }
  
  public List getAllowedAdapterTypes(){
    return allowedAdapterTypes;
  }

  private void setAllowedAdapterClasses(List newValue){
    allowedAdapterClasses = newValue;
  }
  
  private void setAllowedAdapterTypes(List newValue){
    allowedAdapterTypes = newValue;
  }

  public HashMap getAdapterMap(){
    return adapterMap;
  }
  
  public HashMap getAdapterGUIMap(){
    return adapterGUIMap;
  }

  public void toXML(){
    String fileName = System.getProperty("APOLLO_ROOT");
    fileName = fileName +"/conf/adapterbuilder.conf";
    File file;
    StringBuffer buffer = new StringBuffer();
    AdapterModel adapterModel;
    boolean result;
    FileWriter writer;
    String setName;
    Iterator adapterSets;
    List adapterModels;
    
    try{
      file = new File(fileName);
      if(!file.exists()){
        result = file.createNewFile();
        if(!result){
          throw new NonFatalDataAdapterException("Failed to create builder config file: "+fileName);
        }
      }

      buffer.append("<model>\n");

      buffer.append("\t<").append(ADAPTER_CLASSES).append(">\n");
      for(int i=0; i<getAllowedAdapterClasses().size(); i++){
        if(getAllowedAdapterClasses().get(i) != null && ((String)getAllowedAdapterClasses().get(i)).trim().length()>0){
          buffer
            .append("\t\t<").append(CLASS).append(">")
            .append(getAllowedAdapterClasses().get(i))
            .append("</").append(CLASS).append(">\n");
        }
      }
      buffer.append("\t</"+ADAPTER_CLASSES+">\n");
      
      buffer.append("\t<").append(ADAPTER_TYPES).append(">\n");
      for(int i=0; i<getAllowedAdapterTypes().size(); i++){
        if(getAllowedAdapterTypes().get(i) != null && ((String)getAllowedAdapterTypes().get(i)).trim().length()>0){
          buffer
            .append("\t\t<").append(TYPE).append(">")
            .append(getAllowedAdapterTypes().get(i))
            .append("</").append(TYPE).append(">\n");
        }
      }
      buffer.append("\t</"+ADAPTER_TYPES+">\n");
      
      if(getSelectedAdapterSet() != null){
        buffer.append("\t<").append(SELECTED_SET).append(">")
              .append(getSelectedAdapterSet())
              .append("</").append(SELECTED_SET).append(">\n");
        if(getSelectedAdapterModel() != null){
          buffer.append("\t<").append(SELECTED_NODE).append(">")
            .append(getSelectedAdapterModel().getName())
            .append("</").append(SELECTED_NODE).append(">\n");
        }
      }

      adapterSets = getAdapterSets().keySet().iterator();
      while(adapterSets.hasNext()){
        setName = (String)adapterSets.next();
        buffer.append("\t<"+ADAPTER_SET+">\n");
        buffer.append("\t\t<").append(NAME).append(">").append(setName).append("</").append(NAME).append(">\n");
        adapterModels = (List)getAdapterSets().get(setName);
        for(int i=0; i < adapterModels.size(); i++){
          adapterModel = (AdapterModel)adapterModels.get(i);
          buffer.append(adapterModel.toXML("\t\t"));
        }
        
        buffer.append("\t</"+ADAPTER_SET+">\n");
      }

      buffer.append("</"+MODEL+">\n");;
      writer = new FileWriter(file);
      writer.write(buffer.toString());
      writer.flush();
      writer.close();
      
    }catch(IOException exception){
      throw new NonFatalDataAdapterException("Problem writing to configuration file: "+fileName);
    }
  }
  
  public void setSelectedAdapterName(String setName, String name){
    if(setName != null){
      List adapterModels = getAdapterModels(setName);
      for(int i=0; i<adapterModels.size(); i++){
        if(((AdapterModel)adapterModels.get(i)).getName().equals(name)){
          selectedAdapterModel = (AdapterModel)adapterModels.get(i);
        }
      }
    }else{
      selectedAdapterModel = null;   
    }
  }

  public void updateSelectedModelFromAdapter() {
    try{
      if (selectedAdapterModel != null) {
        ApolloDataAdapterGUI adag = (ApolloDataAdapterGUI) getAdapterGUIMap().get(selectedAdapterModel.getName());
        if (adag != null) {
          selectedAdapterModel.setProperties(adag.createStateInformation());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public java.util.List getChildAdapterModels(String setName){
    Iterator adapterModels = getAdapterModels(setName).iterator();
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

  public AdapterModel getMainAdapterModel(String setName){
    Iterator adapterModels = getAdapterModels(setName).iterator();
    AdapterModel adapterModel;
    while(adapterModels.hasNext()){
      adapterModel = (AdapterModel)adapterModels.next();
      if(adapterModel.getType().equals(AdapterModel.MAIN_TYPE)){
        return adapterModel;
      }
    }
    
    return null;
  }
  
  public AbstractDataAdapterUI getMainAdapterGUI(String setName){
    String key = getMainAdapterModel(setName).getName();
    return (AbstractDataAdapterUI)getAdapterGUIMap().get(key);
  }
  
  public List getChildAdapterGUIs(String setName){
    List list = getChildAdapterModels(setName);
    List returnList = new ArrayList();
    String key;
    for(int i=0; i<list.size(); i++){
      key = ((AdapterModel)list.get(i)).getName();
      returnList.add(getAdapterGUIMap().get(key));
    }
    return returnList;
  }
  
  public AdapterModel getModelWithName(String setName, String name){
    Iterator adapterModels = getAdapterModels(setName).iterator();
    AdapterModel adapterModel;
    while(adapterModels.hasNext()){
      adapterModel = (AdapterModel)adapterModels.next();
      if(adapterModel.getName().equals(name)){
        return adapterModel;
      }
    }
    
    return null;
  }
  
  public AdapterModel getSelectedAdapterModel(){
    return selectedAdapterModel;
  }
  
  public int getSelectedAdapterIndex(String setName){
    return getAdapterModels(setName).indexOf(getSelectedAdapterModel());
  }
  
  public String getSelectedAdapterSet(){
	return _selectedAdapterSet;
  }
  
  public boolean reloadTree(){
    return reloadTree;
  }

  public void setReloadTree(boolean newValue){
    reloadTree = newValue;
  }

  public boolean reloadAdapterPanel(){
    return reloadAdapterPanel;
  }

  public void setReloadAdapterPanel(boolean newValue){
    reloadAdapterPanel = newValue;
  }

  public boolean reloadGUIs(){
    return reloadGUIs;
  }

  public void setReloadGUIs(boolean newValue){
    reloadGUIs = newValue;
  }

  public void setReloadAll(){
    setReloadTree(true);
    setReloadAdapterPanel(true);
    setReloadGUIs(true);
  }

  public void setMessage(String message){
    this.message = message;
  }

  public String getMessage(){
    return message;
  }

  public void setSelectedAdapterSet(String set){
  	_selectedAdapterSet = set;
  }

  private void setAdapterSets(HashMap adapterSets) {
    this.adapterSets = adapterSets;
  }

  public HashMap getAdapterSets() {
    return adapterSets;
  }
}
