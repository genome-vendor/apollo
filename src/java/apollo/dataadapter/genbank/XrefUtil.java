package apollo.dataadapter.genbank;

import java.util.Vector;

import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.DbXref;

public class XrefUtil {
  String id_regex = "";
  Vector dbkeys;
  String dbname = "";

  public XrefUtil(String user_input) {
    try {
      String[] tokens = user_input.split(":");
      String keys = tokens[0];
      dbname = tokens[1];
      id_regex = tokens[2];
      
      tokens = keys.split(",");
      for (int i = 0; i < tokens.length; i++) {
        if (dbkeys == null)
          dbkeys = new Vector (tokens.length);
        dbkeys.addElement(tokens[i]);
      }
    } catch (Exception e) {
      System.out.println ("Failed parsing " + user_input + " because of " +
                          e.getMessage());
      System.out.println ("dbname " + dbname + " id format " + id_regex);
    }
  }

  protected String getGenBankXref(AnnotatedFeatureI gene) {
    StringBuffer buf = new StringBuffer();
    Vector xrefs = gene.getDbXrefs();
    for (int i = 0; i < xrefs.size(); i++) {
      // Look at every database cross reference for the gene
      DbXref xref = (DbXref) xrefs.elementAt (i);
      boolean match = false;
      for (int j = 0; j < dbkeys.size() && !match; j++) {
        /* see if then database name matches any form that is
           expected for this particular output */
        String db_key = (String) dbkeys.elementAt(j);
        match = xref.getDbName().equalsIgnoreCase(db_key);
      }
      /* if it does match then also make sure that the id format
         matches as well */
      if (match) {
        String id = xref.getIdValue();
        match = id.matches(id_regex);
        if (match)
          if (id.indexOf(':') > 0)
            buf.append ("\t\t\tdb_xref\t" + id + "\n");
          else
            buf.append ("\t\t\tdb_xref\t" + dbname + ":" + id + "\n");
      }
    }
    return buf.toString();
  }

}
