package apollo.dataadapter.chado.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.*;

import apollo.datamodel.CurationSet;
import apollo.datamodel.SeqFeatureI;
//import apollo.editor.AnnotationChangeLog;
import apollo.editor.Transaction;
import apollo.editor.TransactionManager;
import apollo.editor.TransactionOperation;

// not sure where this came from (guanming?) but im pretty sure its not used nor
// fully implemented - all its doing is deletes

public class FlybaseWrite { // implements ChadoWrite
    
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(FlybaseWrite.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private Connection connection;

  FlybaseWrite(Connection conn) {
    connection = conn;
  }

  void writeCurationSet(CurationSet curationSet) {
    //AnnotationChangeLog acl = curationSet.getAnnotationChangeLog();
    TransactionManager tm = curationSet.getTransactionManager();
    for (int i=0; i<tm.size(); i++) {
      Transaction trans = tm.getTransaction(i);
      if (trans.isDelete()) {
        doDelete(trans,curationSet);
      }
      // else if ...
    }
  }

  private void doDelete(Transaction trans, CurationSet curSet) {
    SeqFeatureI delFeat = trans.getDeletedFeature();
    String featName = delFeat.getName();
    // Im assuming triggers will take care of everything associated that needs to
    // get deleted
    String sql = "DELETE FROM feature WHERE uniquename = '"+featName+"'";
    logger.debug("doDelete SQL: " + sql);
    try {
      connection.createStatement().executeUpdate(sql);
      logger.debug("doDelete deleted feature " + featName);
    } catch (SQLException e) {
      logger.error("Error doing delete "+sql+" "+e, e);
    }
  }
}
