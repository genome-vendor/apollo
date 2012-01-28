package apollo.dataadapter.ensj.model;
import java.util.*;
import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.view.*;
import apollo.dataadapter.ensj.controller.*;

public class AnnotationsModel {

  private String _annotationUser;
  private String _server;
  private String _serverPort;
  private String _annotationUserEmail;
  private boolean _editingEnabled;
  private String _selectedDataSet;
  private List _dataSets = new ArrayList();
  
  private String _selectedFile;
  private List _files = new ArrayList();
  private boolean _isFileChooserOpen;

  public String getAnnotationUser(){
    return _annotationUser;
  }
  
  public void setAnnotationUser(String value){
    _annotationUser = value;
  }

  public String getServer(){
    return _server;
  }
  
  public void setServer(String value){
    _server = value;
  }
  

  public String getServerPort(){
    return _serverPort;
  }
  
  public void setServerPort(String value){
    _serverPort = value;
  }

  public String getAnnotationUserEmail(){
    return _annotationUserEmail;
  }
  
  public void setAnnotationUserEmail(String value){
    _annotationUserEmail = value;
  }

  public boolean isEditingEnabled(){
    return _editingEnabled;
  }
  
  public void setEditingEnabled(boolean value){
    _editingEnabled = value;
  }

  public String getSelectedDataSet(){
    return _selectedDataSet;
  }
  
  public void setSelectedDataSet(String value){
    _selectedDataSet = value;
  }

  public List getDataSets(){
    return _dataSets;
  }
  
  public void setDataSets(List value ){
    _dataSets = value;
  }

  public boolean isFileChooserOpen(){
    return _isFileChooserOpen;
  }
  
  public void setFileChooserOpen(boolean value ){
    _isFileChooserOpen = value;
  }

  public String getSelectedFile(){
    return _selectedFile;
  }
  
  public void setSelectedFile(String value ){
    _selectedFile = value;
  }

  public List getFiles(){
    return _files;
  }
  
  public void setFiles(List value ){
    _files = value;
  }
}
