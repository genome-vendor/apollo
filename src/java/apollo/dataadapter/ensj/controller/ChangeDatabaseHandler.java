package apollo.dataadapter.ensj.controller;

import java.util.*;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.view.*;

/**
 * The user has typed into the host, port etc database fields,
 * so we clear off the ensembl-db list selected ensembl-db.
 * This also means that the list of seq regions and coord systems
 * has to be cleared.
**/
public class ChangeDatabaseHandler extends EventHandler{
  
  public ChangeDatabaseHandler(Controller controller, String key) {
    super(controller, key);
  }
  
  public void doAction(Model model){
    if(!model.getDatabaseModel().isDatabaseListFound()){
      log("Returning - database list is yet to be found");
      return;
    }
    
    doUpdate();
    
    model.getDatabaseModel().setDatabaseListFound(false);
    model.getDatabaseModel().setEnsemblDatabases(new ArrayList());
    model.getDatabaseModel().setSelectedEnsemblDatabase(null);
    
    model.getLocationModel().setCoordSystems(new ArrayList());
    model.getLocationModel().setSelectedCoordSystem(null);
    model.getLocationModel().setSeqRegions(new ArrayList());
    model.getLocationModel().setSelectedSeqRegion(null);
    model.getLocationModel().setCoordSystemInitialised(false);
    model.getLocationModel().setSeqRegionInitialised(false);
    model.getLocationModel().setStart(null);
    model.getLocationModel().setEnd(null);
    
    model.getTypesModel().setGeneCount(null);
    model.getTypesModel().setDnaProteinAlignmentCount(null);
    model.getTypesModel().setDnaDnaAlignmentCount(null);
    model.getTypesModel().setAbInitioPredictionCount(null);
    model.getTypesModel().setRepeatCount(null);
    model.getTypesModel().setContigCount(null);
    model.getTypesModel().setSimpleFeatureCount(null);
    model.getTypesModel().setProteinAnnotationCount(null);
    model.getTypesModel().setDitagFeatureCount(null);
    
    model.getTypesModel().setGeneTypes(new ArrayList());
    model.getTypesModel().setDnaDnaAlignTypes(new ArrayList());
    model.getTypesModel().setDnaProteinAlignTypes(new ArrayList());
    model.getTypesModel().setSimpleFeatureTypes(new ArrayList());
    model.getTypesModel().setPredictionTypes(new ArrayList());
    model.getTypesModel().setDitagFeatureTypes(new ArrayList());
    
    model.getTypesModel().setGeneTypeCountInitialised(false);
    model.getTypesModel().setDnaProteinAlignmentTypeCountInitialised(false);
    model.getTypesModel().setDnaDnaAlignmentTypeCountInitialised(false);
    model.getTypesModel().setSimpleFeatureTypeCountInitialised(false);
    model.getTypesModel().setSimplePeptideTypeCountInitialised(false);
    model.getTypesModel().setAbInitioTypeCountInitialised(false);
    model.getTypesModel().setDitagFeatureTypeCountInitialised(false);
    
    doRead();
  }
}
