package apollo.dataadapter.ensj.controller;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.view.*;
import java.sql.*;
import java.util.*;

/**
 * Handle stuff when a new ensembldb is selected - in this case finding new
 * total feature counts, and resetting the 'isInitialised' flags for the type-specific counts.
**/
public class SelectNewEnsemblDatabaseHandler extends EventHandler{
  
  public SelectNewEnsemblDatabaseHandler(Controller controller, String key) {
    super(controller, key);
  }
  
  public void doAction(Model model){
    TypesModel myModel = model.getTypesModel();
    
    doUpdate();
    
    log("databasemodel " + model.getDatabaseModel().hashCode()+" Select new ensembldb: Selected database: "+model.getDatabaseModel().getSelectedEnsemblDatabase());
    
    Connection connection = getConnectionForModel(model);

    int schema = EnsJConnectionUtil.getEnsemblSchema(connection);

    model.getLocationModel().setCoordSystems(new ArrayList());
    model.getLocationModel().setSelectedCoordSystem(null);
    model.getLocationModel().setSeqRegions(new ArrayList());
    model.getLocationModel().setSelectedSeqRegion(null);
    model.getLocationModel().setCoordSystemInitialised(false);
    model.getLocationModel().setSeqRegionInitialised(false);
    model.getLocationModel().setStart(null);
    model.getLocationModel().setEnd(null);
    
    myModel.setGeneCount(String.valueOf(getRowCountForTable("gene", connection)));
    myModel.setDnaProteinAlignmentCount(handleNull(String.valueOf((getRowCountForTable("protein_align_feature", connection)))));
    myModel.setDnaDnaAlignmentCount(handleNull(String.valueOf((getRowCountForTable("dna_align_feature", connection)))));
    myModel.setAbInitioPredictionCount(handleNull(String.valueOf((getRowCountForTable("prediction_transcript", connection)))));
    myModel.setRepeatCount(handleNull(String.valueOf((getRowCountForTable("repeat_feature", connection)))));
    myModel.setSimpleFeatureCount(handleNull(String.valueOf((getRowCountForTable("simple_feature", connection)))));
    if (schema > 39) {
      myModel.setDitagFeatureCount(handleNull(String.valueOf((getRowCountForTable("ditag_feature", connection)))));
    }
    myModel.setContigCount(
	 handleNull(
	      String.valueOf(
		      getCountForQuery("select count(*) from seq_region sr, coord_system cs where cs.name='contig' and sr.coord_system_id=cs.coord_system_id", connection))));
    myModel.setProteinAnnotationCount(handleNull(String.valueOf((getRowCountForTable("protein_feature", connection)))));
    
    model.getTypesModel().setGeneTypes(new ArrayList());
    model.getTypesModel().setDnaDnaAlignTypes(new ArrayList());
    model.getTypesModel().setDnaProteinAlignTypes(new ArrayList());
    model.getTypesModel().setSimpleFeatureTypes(new ArrayList());
    model.getTypesModel().setDitagFeatureTypes(new ArrayList());
    model.getTypesModel().setPredictionTypes(new ArrayList());

    model.getTypesModel().setSelectedGeneTypes(new ArrayList());
    model.getTypesModel().setSelectedDnaDnaAlignTypes(new ArrayList());
    model.getTypesModel().setSelectedDnaProteinAlignTypes(new ArrayList());
    model.getTypesModel().setSelectedSimpleFeatureTypes(new ArrayList());
    model.getTypesModel().setSelectedDitagFeatureTypes(new ArrayList());
    model.getTypesModel().setSelectedPredictionTypes(new ArrayList());
    
    model.getTypesModel().setTypePanelToShow(TypesModel.NONE);

    model.getTypesModel().setGeneTypeCountInitialised(false);
    model.getTypesModel().setDnaProteinAlignmentTypeCountInitialised(false);
    model.getTypesModel().setDnaDnaAlignmentTypeCountInitialised(false);
    model.getTypesModel().setSimpleFeatureTypeCountInitialised(false);
    model.getTypesModel().setDitagFeatureTypeCountInitialised(false);
    model.getTypesModel().setSimplePeptideTypeCountInitialised(false);
    model.getTypesModel().setAbInitioTypeCountInitialised(false);
    
    closeConnection(connection);
    doRead();
  }
  
  private String handleNull(String input){
    if(input == null){
      return "";
    }else{
      return input;
    }
  }
}
