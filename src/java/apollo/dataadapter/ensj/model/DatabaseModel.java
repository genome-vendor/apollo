package apollo.dataadapter.ensj.model;
import java.util.*;
import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.view.*;
import apollo.dataadapter.ensj.controller.*;

public class DatabaseModel {

  private String _host;
  private String _port;
  private String _user;
  private String _password;
  private String _selectedEnsemblDatabase;
  private List _ensemblDatabases = new ArrayList();
  
  private String _sequenceHost;
  private String _sequencePort;
  private String _sequenceUser;
  private String _sequencePassword;
  private String _selectedSequenceEnsemblDatabase;
  private List _sequenceEnsemblDatabases = new ArrayList();
  
  private boolean _databaseListFound;

  public String getHost(){
    return _host;
  }

  public void setHost(String value){
    _host = value;
  }

  public String getPort(){
    return _port;
  }
  
  public void setPort(String value){
    _port = value;
  }
  
  public String getUser(){
    return _user;
  }
  
  public void setUser(String value){
    _user = value;
  }
  
  public String getPassword(){
    return _password;
  }
  
  public void setPassword(String value){
    _password = value;
  }
  
  public String getSelectedEnsemblDatabase(){
    return _selectedEnsemblDatabase;
  }
  
  public void setSelectedEnsemblDatabase(String value){
    _selectedEnsemblDatabase = value;
  }
  
  public List getEnsemblDatabases(){
    return _ensemblDatabases;
  }
  
  public void setEnsemblDatabases(List value){
    _ensemblDatabases = value;
  }
  
  public String getSequenceHost(){
    return _sequenceHost;
  }
  
  public void setSequenceHost(String value){
    _sequenceHost = value;
  }
  
  public String getSequencePort(){
    return _sequencePort;
  }
  
  public void setSequencePort(String value){
    _sequencePort = value;
  }
  
  public String getSequenceUser(){
    return _sequenceUser;
  }
  
  public void setSequenceUser(String value){
    _sequenceUser = value;
  }
  
  public String getSequencePassword(){
    return _sequencePassword;
  }
  
  public void setSequencePassword(String value){
    _sequencePassword = value;
  }
  
  public String getSelectedSequenceEnsemblDatabase(){
    return _selectedSequenceEnsemblDatabase;
  }
  
  public void setSelectedSequenceEnsemblDatabase(String value){
    _selectedSequenceEnsemblDatabase = value;
  }
  
  public List getSequenceEnsemblDatabases(){
    return _sequenceEnsemblDatabases;
  }
  
  public void setSequenceEnsemblDatabases(List value){
    _sequenceEnsemblDatabases = value;
  }
  
  public boolean isDatabaseListFound(){
    return _databaseListFound;
  }
  
  public void setDatabaseListFound(boolean value){
    _databaseListFound = value;
  }
}
