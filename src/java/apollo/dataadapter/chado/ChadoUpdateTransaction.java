/*
 * Created on Oct 9, 2004
 *
 */
package apollo.dataadapter.chado;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author wgm
 */
public class ChadoUpdateTransaction extends ChadoTransaction {
  // A new map for setting updating values
  private Map updateProp;
  
  public ChadoUpdateTransaction() {
  }

  public ChadoUpdateTransaction(String table,String uniquename) {
    setTableName(table);
    addProperty("uniquename",uniquename);
  }
  
  public ChadoTransaction.Operation getOperation() {
    return UPDATE;
  }
  
  public void addUpdateProperty(String name, String value) {
    if (updateProp == null)
      updateProp = new HashMap();
    updateProp.put(name, value);
  }
  
  public Map getUpdateProperies() {
    return updateProp;
  }
}
