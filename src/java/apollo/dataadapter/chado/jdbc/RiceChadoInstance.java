/*
 * Created on Sep 21, 2004
 *
 */
package apollo.dataadapter.chado.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A ChadoInstance
 * @author wgm
 */
public class RiceChadoInstance extends FlybaseChadoInstance {

  public RiceChadoInstance() {
  }
  
  public RiceChadoInstance(JdbcChadoAdapter adapter) {
    super(adapter);
  }
  
  /** Flybases feature cv is SO */
//   public String getFeatureCVName() {
//     return "Sequence Ontology Feature Annotation";
//   }

//   /** fbs relationship cv is "relationship type" */
//   public String getRelationshipCVName() {
//     return "Relationship Ontology";
//   }

//   /** rice is autocreated cv for properties apparently */
//   public String getPropertyTypeCVName() {
//     //return "property type";
//     return "autocreated";
//   }

  // --> chado-adapter.xml
//   /** fb: "partof" */
//   public String getPartOfCvTerm() {
//     return "part_of";
//   }

//   /** These should come from config - chado-adapter.xml */
//   protected String[] getHitPrograms() {
//     return new String[]{"blat"};
//   }
  
//   /** These should come from config - chado-adapter.xml */
//   public String[] getPredictionPrograms() {
//     //if (super.getPredictionPrograms() != null)
//     return super.getPredictionPrograms();
//     //return new String[]{"FgenesH"};
//     //return null;
//   }


// polypeptide type is now a configurable - and rice now uses "polypeptide" so 
// this hack is no longer needed! this actually makes RiceChadoInstance
// irrelevant - i guess we could get rid of it and config to FlybaseChadoInstance
// the goal is to make FlybaseCI irrelevant as well and have everything configurable
// from the xml file

//   /* 
//    * A very, very bad way to get the cvterm_id for protein. The reason for this
//    * because there is no "protein" in SOFA, however, no "polypeptide" in SO.
//    * @return the cvterm_id for the specified name.
//    rice is using SO's protein for proteins (SOFA doesnt have protein) but is using
//    SOFA terms for everything else - which leads to this kludge. rice should be using all
//    SOFA terms!
//    */
//   protected Long getFeatureCVTermId(String name) {
//     if (name.equals("protein")) {
//       // A special case
//       Connection conn = getChadoAdapter().getConnection();
//       String query = "SELECT cvterm_id FROM cvterm, cv WHERE " +
//                      "cvterm.cv_id = cv.cv_id AND cvterm.name='protein' " +
//                      "AND cv.name='Sequence Ontology'";
//       Long rtn = null;
//       try {
//         Statement stat = conn.createStatement();
//         ResultSet result = stat.executeQuery(query);
//         while (result.next())
//           rtn = new Long(result.getLong(1));
//       }
//       catch(SQLException e) {
//         logger.error("RiceChadoInstance.getFeatureCVTermId(): " + e, e);
//       }
//       return rtn;
//     }
//     else
//       return super.getFeatureCVTermId(name);
//   }
  
}
