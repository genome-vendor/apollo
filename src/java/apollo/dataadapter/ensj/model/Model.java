package apollo.dataadapter.ensj.model;
import java.util.*;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.view.*;
import apollo.dataadapter.ensj.controller.*;

public class Model {

  private LocationModel _locationModel = new LocationModel();
  private TypesModel _typesModel = new TypesModel();
  private OptionsModel _optionsModel = new OptionsModel();
  private DatabaseModel _databaseModel = new DatabaseModel();
  private AnnotationsModel _annotationsModel = new AnnotationsModel();
  private boolean _typesPanelVisible;
  private boolean _optionsPanelVisible;
  private boolean _databasePanelVisible;
  private boolean _annotationsPanelVisible;
  private boolean _locationsPanelVisible = true;

  public LocationModel getLocationModel(){
    return _locationModel;
  }

  public void setLocationModel(LocationModel value){
    _locationModel = value;
  }

  public TypesModel getTypesModel(){
    return _typesModel;
  }

  public void setTypesModel(TypesModel value){
    _typesModel = value;
  }

  public OptionsModel getOptionsModel(){
    return _optionsModel;
  }

  public void setOptionsModel(OptionsModel value){
    _optionsModel = value;
  }

  public DatabaseModel getDatabaseModel(){
    return _databaseModel;
  }

  public void setDatabaseModel(DatabaseModel value){
    _databaseModel = value;
  }
  
  public AnnotationsModel getAnnotationsModel(){
    return _annotationsModel;
  }

  public void setAnnotationsModel(AnnotationsModel value){
    _annotationsModel = value;
  }
  
  public boolean isTypesPanelVisible(){
    return _typesPanelVisible;
  }
  
  public void setTypesPanelVisible(boolean value){
    _typesPanelVisible = value;
  }
  
  public boolean isOptionsPanelVisible(){
    return _optionsPanelVisible;
  }
  
  public void setOptionsPanelVisible(boolean value){
    _optionsPanelVisible = value;
  }
  
  public boolean isDatabasePanelVisible(){
    return _databasePanelVisible;
  }

  public void setDatabasePanelVisible(boolean value){
    _databasePanelVisible = value;
  }
  
  public boolean isAnnotationsPanelVisible(){
    return _annotationsPanelVisible;
  }

  public void setAnnotationsPanelVisible(boolean value){
    _annotationsPanelVisible = value;
  }

  public boolean isLocationsPanelVisible(){
    return _locationsPanelVisible;
  }

  public void setLocationsPanelVisible(boolean value){
    _locationsPanelVisible = value;
  }
}
