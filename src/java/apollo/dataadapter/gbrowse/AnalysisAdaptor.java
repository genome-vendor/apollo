package apollo.dataadapter.gbrowse;

import java.sql.*;
import java.util.*;

import apollo.datamodel.*;
import apollo.dataadapter.mysql.*;

public class AnalysisAdaptor extends BaseAdaptor {
  
    Hashtable idHash;

    public AnalysisAdaptor(MySQLDatabase db) {
      setDatabase(db);
    }
    
    public Hashtable fetchAllAnalysis() {
	
	if (idHash != null) {
	    return idHash;
	}

	idHash   = new Hashtable();
      
	String query = "select * from ftype";
      
	ResultSet rs = getDatabase().query(query);

	try {  
	    while (rs.next()) {
		int id       = rs.getInt(1);
		String analysis = rs.getString(2);

		idHash.put(new Integer(id),analysis);
		
	    }
	    
	    return idHash;
	    
	} catch (SQLException e) {
	    System.out.println("ERROR: Problem getting analysis");
	}
	return null;
    }
  
    public String fetchByDbId(int id) {
    
	fetchAllAnalysis();
    
	return (String)idHash.get(new Integer(id));
	
    }
  
}
