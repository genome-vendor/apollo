package apollo.dataadapter.gbrowse;

import java.sql.*;
import java.util.*;

import apollo.datamodel.*;
import apollo.dataadapter.mysql.*;


public class FeatureAdaptor extends BaseAdaptor {
  
    AnalysisAdaptor aa;

    public FeatureAdaptor(MySQLDatabase db) {
	setDatabase(db);
	
	aa = new AnalysisAdaptor(db);

    }

    public FeatureSetI fetchByReferenceStartEnd(String ref,int start, int end) {

	FeatureSet fset = new FeatureSet();
    
	String query = "SELECT fdata.*,fgroup.*,fatt.fattribute_value, f.fattribute_name " +
	               "FROM   fdata,fgroup,fattribute_to_feature fatt, fattribute f " + 
                       "WHERE  fdata.fref = '" + ref + "' " + 
	               "AND    fdata.gid = fgroup.gid " + 
                       "AND NOT (fdata.fstart > " + end + " "  +  
	               "OR  fdata.fstop < " + start + ")" +
	               "AND    fatt.fid = fdata.fid " + 
	               "AND    f.fattribute_id = fatt.fattribute_id";

	System.out.println("Query " + query);

	ResultSet rs = getDatabase().query(query);

	Hashtable groups = new Hashtable();
	Vector    tmpf   = new Vector();

	try {
	    while (rs.next()) {
		SeqFeatureI f = (SeqFeatureI)seqFeatureFromResultSet(rs,groups);
		tmpf.addElement(f);

	    }

	    Enumeration en = groups.elements();

	    while (en.hasMoreElements()) {
		Vector g = (Vector)en.nextElement();

		if (g.size() > 1) {

		    FeatureSet fset2 = new FeatureSet();

		    for (int i = 0; i < g.size(); i++) {
			fset2.addFeature((SeqFeatureI)g.elementAt(i));
		    }
		    fset.addFeature(fset2);
		} else {
		    
		    fset.addFeature((SeqFeatureI)g.elementAt(0));
		}
	    }
	    return fset;
	} catch (SQLException e) {
	    System.out.println("SQLException "+ e);
	}
	return null;
    }
  
    public SeqFeatureI seqFeatureFromResultSet(ResultSet rs,Hashtable groups) throws SQLException {
    
	int    id        = rs.getInt(1);
	String ref       = rs.getString(2);
	int    start     = rs.getInt(3);
	int    end       = rs.getInt(4);
	double bin       = rs.getDouble(5);
	int    typeid    = rs.getInt(6);
	double score     = rs.getDouble(7);
	String strand    = rs.getString(8);
	int    phase     = rs.getInt(9);
	int    groupid   = rs.getInt(10);
	int    hstart    = rs.getInt(11);
	int    hend      = rs.getInt(12);
	String gclass    = rs.getString(14);
	String name      = rs.getString(15);
	String value     = rs.getString(16);
	String key       = rs.getString(17);

	int strandno = 1;

	if (strand != null && strand.equals("-")) {
	    strandno = -1;
	}

	String analysis = aa.fetchByDbId(typeid);

	SeqFeature sf1 = new SeqFeature(start,end,analysis);

	sf1.setScore(score);
	sf1.setName(name);
	sf1.setId(name);
	sf1.setStrand(strandno);

	if (value != null) {
	    sf1.addProperty(key,value);
	}

	if (groups.get(new Integer(groupid)) == null) { 
	    Vector g = new Vector();

	    groups.put(new Integer(groupid),g);
	}

	Vector g = (Vector)groups.get(new Integer(groupid));

	if (hstart > 0) {
	    SeqFeature sf2 = new SeqFeature(hstart,hend,analysis);
	    
	    sf2.setScore(score);
	    sf2.setName(name);
	    sf2.setId(name);
	    sf2.setStrand(strandno);
      
	    FeaturePair fp = new FeaturePair(sf1,sf2);
	    g.addElement(fp);

	    return fp;
	} else {
	    g.addElement(sf1);
	    return sf1;

	}
    }

    public void printGFF(SeqFeatureI sf) {
	String strand = "+";
	if (sf.getStrand() == -1) {
	    strand = "-";
	}
	System.out.print(sf.getName()+ '\t' + sf.getFeatureType() + "\tsimilarity\t"  + sf.getLow() + '\t' +
			 sf.getHigh() + "\t" + sf.getScore() + "\t" + sf.getStrand() + "\t.");
	
	if (sf instanceof FeaturePair) {
	    FeaturePair fp = (FeaturePair)sf;
	    
	    System.out.println("\t" + fp.getHname() + "\t" + fp.getHlow() + "\t" + fp.getHhigh());
	} else {
	    System.out.println();
	}
    }
 
    public static void main(String[] args) {

	MySQLInstance mysql = new MySQLInstance("localhost","root","",3306);
	MySQLDatabase db    = mysql.fetchDatabaseByName("yeast");
	DBAdaptor     dba   = new DBAdaptor(db);
	FeatureAdaptor fa   = dba.getFeatureAdaptor();
	FeatureSetI    f    = fa.fetchByReferenceStartEnd(args[0],
							  Integer.parseInt(args[1]),
							  Integer.parseInt(args[2]));

	for (int i = 0; i < f.size(); i++) {
	    SeqFeatureI sf = f.getFeatureAt(i);
	    fa.printGFF(sf);
	}
    }
}
       
        
