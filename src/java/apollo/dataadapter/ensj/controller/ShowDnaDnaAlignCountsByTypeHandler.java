package apollo.dataadapter.ensj.controller;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.view.*;
import java.sql.*;
import java.util.*;

/**
 * Check to see if our dna-protein align counts have already been initialised. If not,
 * run sql to get them. Populate the table of protein-align types, counts (by type)
 * and set the 'dna-protein align counts by type initialised' flag to true. Tell the gui
 * to display that types list.
**/
public class ShowDnaDnaAlignCountsByTypeHandler extends EventHandler{
  
  public ShowDnaDnaAlignCountsByTypeHandler(Controller controller, String key) {
    super(controller, key);
  }
  
  public void doAction(Model model){
    TypesModel myModel = model.getTypesModel();
    HashMap typeCounts;
    log("Fetching & showing dna-dna align counts by type");
    
    doUpdate();
    
    if(myModel.getTypePanelToShow().equals(myModel.DNADNA)){
      myModel.setTypePanelToShow(myModel.NONE);
    }else{
      log("Setting type panel to show type "+myModel.DNADNA);
      myModel.setTypePanelToShow(myModel.DNADNA);
    }
    
    if(!myModel.isDnaDnaAlignmentTypeCountInitialised()){
      typeCounts = getCountsByType(model);
      myModel.setDnaDnaAlignmentTypeCounts(typeCounts);
      myModel.setDnaDnaAlignTypes(new ArrayList(typeCounts.keySet()));
      myModel.setDnaDnaAlignmentTypeCountInitialised(true);
    }
    
    doRead();
  }
  
  public HashMap getCountsByType(Model model){
    HashMap returnMap = new HashMap();
    Connection connection = getConnectionForModel(model);
    String sql =
     "select logic_name, count(*) from dna_align_feature, analysis where dna_align_feature.analysis_id = analysis.analysis_id "+
     "group by dna_align_feature.analysis_id";


    ResultSet results;

    try{

      results = connection.createStatement().executeQuery(sql);
      while(results.next()){
        returnMap.put(results.getString(1), new Integer(results.getInt(2)));
      }

      log("Found "+returnMap.keySet().size()+" Different types");
    }catch(SQLException exception){
      throw new NonFatalException(exception.getMessage(), exception);
    }

    return returnMap;
  }
  
}
