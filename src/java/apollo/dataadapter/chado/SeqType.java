package apollo.dataadapter.chado;

import java.util.ArrayList;
import java.util.List;

import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.chado.jdbc.JdbcChadoAdapter;

import org.apache.log4j.*;

public class SeqType {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(SeqType.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private String name;
  private boolean hasStartAndEnd = false;
  private boolean isTopLevelFeatType = false;
  private boolean queryForValues = false;
  private List locationTopLevelSeqIds;
  private List valueList;

  SeqType() {}
  SeqType(String name) { setName(name); }

  void setName(String nm) { 
    name = nm;
    if (name.equals("chromosome"))
      hasStartAndEnd = true; // for now for backward compatibility - phase out
  }

  public String getName() { return name; }

  /** if queryForValues is true and name is not present in cvterm table, 
      will return empty list... */
  List getLocationTopLevelSeqIds(ChadoDatabase db) {
    if (!hasStartAndEnd) return new ArrayList(0); // shouldnt be called in this case
    
    if (locationTopLevelSeqIds != null) // check cache
      return locationTopLevelSeqIds;
      
    // QUERY DB
    if (queryForValues) {
      try {
        // if 'name' is not present in cvterm table, will return empty list
        locationTopLevelSeqIds = db.getJdbcChadoAdapter().getFeatNamesByType(name);
        // print message if empty list?
        if (locationTopLevelSeqIds.size() == 0) {
          String m = "No features returned for type "+name+" either check db for "
            +name+" in cvterm and feature tables or amend your config file "+
            " (chado-adapter.xml), change seq type or config values.";
          logger.error(m);
        }
      }
      catch (ApolloAdapterException e) { // thrown by getJdbcChadoAdapter
        logger.error("can't connect to database", e);
        List locationTopLevelSeqIds = new ArrayList();
        locationTopLevelSeqIds.add("No database connection");
        return locationTopLevelSeqIds;
      }
    }
    // GET FROM CONFIG
    else {
      if (hasValues())
        locationTopLevelSeqIds = getValues();
      else {
        locationTopLevelSeqIds = new ArrayList(1);
        locationTopLevelSeqIds.add("No values specified in chado adapter config");
      }
    }
    return locationTopLevelSeqIds;
  }

  public boolean isTopLevelFeatType() { return isTopLevelFeatType; }

  public void setHasStartAndEnd(boolean sae) { hasStartAndEnd = sae; }
  public boolean hasStartAndEnd() { return hasStartAndEnd; }

  public void setQueryForValues(boolean q) { queryForValues = q; }

  public void setIsTopLevelFeatType(boolean tl) { isTopLevelFeatType = tl; }

  /** values is a list of items for the user to choose from (e.g. chromosome list) */
  void setValues(List values) { valueList = values; }
  private List getValues() { return valueList; }
  private boolean hasValues() { return valueList != null; }
}
