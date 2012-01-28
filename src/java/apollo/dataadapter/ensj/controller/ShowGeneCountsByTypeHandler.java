package apollo.dataadapter.ensj.controller;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.view.*;
import java.sql.*;
import java.util.*;

/**
 * Check to see if our gene counts have already been initialised. If not,
 * run sql to get them. Populate the table of gene types, gene counts (by type)
 * and set the 'gene counts by type initialised' flag to true. Tell the gui
 * to display the gene types list.
**/
public class ShowGeneCountsByTypeHandler extends EventHandler{
  
  public ShowGeneCountsByTypeHandler(Controller controller, String key) {
    super(controller, key);
  }
  
  public void doAction(Model model){
    TypesModel myModel = model.getTypesModel();
    HashMap geneTypeCounts;
    log("Fetching & showing gene counts by type");
    
    doUpdate();
    
    if(myModel.getTypePanelToShow().equals(myModel.GENE)){
      log("Setting NO type panel to show (hiding gene type panel)");
      myModel.setTypePanelToShow(myModel.NONE);
    }else{
      log("Setting type panel to show type "+myModel.GENE);
      myModel.setTypePanelToShow(myModel.GENE);
    }
    
    if(!myModel.isGeneTypeCountInitialised()){
      log("Initialising gene type count");
      geneTypeCounts = getGeneCountsByType(model);
      myModel.setGeneTypeCounts(geneTypeCounts);
      ArrayList geneTypes = new ArrayList(geneTypeCounts.keySet());
      Collections.sort(geneTypes);
      myModel.setGeneTypes(geneTypes);
      myModel.setGeneTypeCountInitialised(true);
    }
    
    doRead();
  }
  
  public HashMap getGeneCountsByType(Model model){
    HashMap returnMap = new HashMap();
    Connection connection = getConnectionForModel(model);
    //String sql =
    // "select logic_name, count(*) from gene, analysis where gene.analysis_id = analysis.analysis_id "+
    // "group by gene.analysis_id";

    // This code is a bit hacky at the moment because its trying to cope with the transition from schema 20+ to schema 32+ where the
    // gene table columns changed
    int schema = EnsJConnectionUtil.getEnsemblSchema(connection);

    String sql;
    if (schema > 32) {
      sql = "select biotype,count(*),status,source from gene group by biotype,source,status order by biotype,source,status";
    } else if (schema == 32) {
      sql = "select biotype,count(*),confidence,source from gene group by biotype,source,confidence order by biotype,source,confidence";
    } else {
      sql = "select type,count(*) from gene group by type order by type";
    }


    ResultSet results;
    
    try{
      
      results = connection.createStatement().executeQuery(sql);
      while(results.next()){
        String typeStr;
        
        if (schema > 30) {
          typeStr = (results.getString(1) == null ? "" : results.getString(1)) + "_" + 
                    (results.getString(3) == null ? "" : results.getString(3)) + "_" + 
                    (results.getString(4) == null ? "" : results.getString(4));
  
        } else {
          typeStr = results.getString(1);
        }

        //System.out.println("Type string = " + typeStr);
        returnMap.put(typeStr, new Integer(results.getInt(2)));
      }
      
      log("Found "+returnMap.keySet().size()+" Different types");
      connection.close();
    }catch(SQLException exception){
      throw new NonFatalException(exception.getMessage(), exception);
    }

    
    return returnMap;
  }
  
}
