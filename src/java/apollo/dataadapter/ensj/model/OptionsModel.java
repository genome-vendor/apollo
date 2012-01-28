package apollo.dataadapter.ensj.model;
import java.util.*;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.view.*;
import apollo.dataadapter.ensj.controller.*;

public class OptionsModel {

  private String _resetGeneStartAndStop;
  private String _aggressiveGeneNaming;
  private String _addResultGenesAsAnnotations;
  private String _addSupportToTranscripts;
  private String _typePrefix;
  
  public static final String NONE = "NONE";
  private String _optionPanelToShow = NONE; 

  public String resetGeneStartAndStop(){
    return _resetGeneStartAndStop;
  }

  public void setResetGeneStartAndStop(String newValue){
     _resetGeneStartAndStop = newValue;
  }

  public String addResultGenesAsAnnotations(){
    return _addResultGenesAsAnnotations;
  }

  public void setAddResultGenesAsAnnotations(String newValue){
     _addResultGenesAsAnnotations = newValue;
  }

  public String aggressiveGeneNaming(){
    return _aggressiveGeneNaming;
  }

  public void setAggressiveGeneNaming(String newValue){
     _aggressiveGeneNaming = newValue;
  }

  public String addSupportToTranscripts(){
    return _addSupportToTranscripts;
  }

  public void setAddSupportToTranscripts(String newValue){
     _addSupportToTranscripts = newValue;
  }

  public String typePrefix(){
    return _typePrefix;
  }

  public void setTypePrefix(String newValue){
     _typePrefix = newValue;
  }

  public String getOptionPanelToShow(){
    return _optionPanelToShow; 
  }
  
  public void setOptionPanelToShow(String value){
    _optionPanelToShow = value;
  }

}
