package apollo.dataadapter.ensj.model;
import java.util.*;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.view.*;
import apollo.dataadapter.ensj.controller.*;

public class LocationModel {

  private String _stableID;
  private List _stableIDHistory = new ArrayList();
  private List _coordSystems  = new ArrayList();
  private String _selectedCoordSystem;
  private List _seqRegions  = new ArrayList();
  private String _selectedSeqRegion;
  private String _start;
  private String _end;
  private List _locationHistory  = new ArrayList();
  private String _selectedHistoryLocation;
  private boolean _stableIDLocation;
  private HashMap _seqRegionToLengthsMap = new HashMap();
  private boolean _coordSystemInitialised;
  private boolean _seqRegionInitialised;
  
  public boolean isCoordSystemInitialised(){
    return _coordSystemInitialised;
  }
  
  public void setCoordSystemInitialised(boolean value){
    _coordSystemInitialised = value;
  }
  
  public boolean isSeqRegionInitialised(){
    return _seqRegionInitialised;
  }
  
  public void setSeqRegionInitialised(boolean value){
    _seqRegionInitialised = value;
  }
  
  public boolean isStableIDLocation(){
    return _stableIDLocation;
  }
  
  public void setStableIDLocation(boolean value){
    _stableIDLocation = value;
  }

  public String getStableID(){
    return _stableID;
  }

  public void setStableID(String value){
    _stableID = value;
  }

  public List getCoordSystems(){
    return _coordSystems;
  }

  public void setCoordSystems(List value){
    _coordSystems = value;
  }

  public String getSelectedCoordSystem(){
    return _selectedCoordSystem;
  }

  public void setSelectedCoordSystem(String value){
    _selectedCoordSystem = value;
  }

  public List getSeqRegions(){
    return _seqRegions;
  }

  public void setSeqRegions(List value){
    _seqRegions = value;
  }
  
  public String getSelectedSeqRegion(){
    return _selectedSeqRegion;
  }

  public void setSelectedSeqRegion(String value){
    _selectedSeqRegion = value;
  }
  
  public String getStart(){
    return _start;
  }

  public void setStart(String value){
   _start = value;
  }
  
  public String getEnd(){
    return _end;
  }

  public void setEnd(String value){
    _end = value;
  }
  
  public List getStableIDHistory(){
    return _stableIDHistory;
  }

  public void setStableIDHistory(List value){
    _stableIDHistory = value;
  }
  
  public List getLocationHistory(){
    return _locationHistory;
  }

  public void setLocationHistory(List value){
    _locationHistory = value;
  }
  
  
  public String getSelectedHistoryLocation(){
    return _selectedHistoryLocation;
  }

  public void setSelectedHistoryLocation(String value){
    _selectedHistoryLocation = value;
  }
  
  public HashMap getSeqRegionToLengthsMap(){
    return _seqRegionToLengthsMap;
  }
  
  public void setSeqRegionToLengthsMap(HashMap value){
    _seqRegionToLengthsMap = value;
  }
}
