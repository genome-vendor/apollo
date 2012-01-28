package apollo.dataadapter;

import java.util.*;
import java.io.*;
import apollo.seq.io.*;
import apollo.datamodel.*;
import apollo.config.Config;
import apollo.dataadapter.*;

import org.bdgp.io.*;
import org.bdgp.util.*;
import java.util.Properties;
import org.bdgp.swing.widget.*;

import apollo.dataadapter.mysql.*;
import apollo.dataadapter.gbrowse.*;

public class GbrowseAdapter  extends AbstractApolloAdapter {
    private DataAdapterUI ui;
    
    private MySQLInstance mysql;
    private MySQLDatabase db;
    private DBAdaptor     dba;

    public String         host = "localhost";
    public String         user = "root";
    public String         pass = "";
    public String         dbname = "yeast";
    public int            port   = 3306;

    public String         region;

    IOOperation [] supportedOperations = {
	ApolloDataAdapterI.OP_READ_DATA,
	//ApolloDataAdapterI.OP_READ_SEQUENCE
    };

    public void init() {
    }
    public String getName() {
	return "Gbrowse";
    }

    public String getType() {
	return "Direct gbrowse database access";
    }

    public String getInput() {
	return region; 
    }
    public DataInputType getInputType() {
	return DataInputType.BASEPAIR_RANGE;
    }

    public IOOperation [] getSupportedOperations() {
	return supportedOperations;
    }

    public DataAdapterUI getUI(IOOperation op) {
	if (ui == null) {
	    ui = new GbrowseAdapterGUI(op);
	} 
	return ui;
    }

    public GbrowseAdapter() {}

    public CurationSet getCurationSet() throws apollo.dataadapter.ApolloAdapterException {
	GbrowseAdapterGUI gui = (GbrowseAdapterGUI)getUI(ApolloDataAdapterI.OP_READ_DATA);

	String chr = gui.getSelectedChr();
	int    chrstart = Integer.parseInt(gui.getSelectedStart());
	int    chrend   = Integer.parseInt(gui.getSelectedEnd());

	mysql = new MySQLInstance(host,user,pass,port);
	db    = mysql.fetchDatabaseByName(dbname);
	dba   = new DBAdaptor(db);

	FeatureAdaptor fa   = dba.getFeatureAdaptor();
	FeatureSetI    f    = fa.fetchByReferenceStartEnd(chr,chrstart,chrend);

	super.clearOldData();

	CurationSet curationSet = new CurationSet();

	curationSet.setChromosome(chr);
	curationSet.setLow(chrstart);
	curationSet.setHigh(chrend);
	curationSet.setStrand( 0 );

	StrandedFeatureSet sfset = new StrandedFeatureSet(new FeatureSet(),new FeatureSet());

	for (int i = 0; i < f.size(); i++) {
	    SeqFeatureI sf = f.getFeatureAt(i);

	    sfset.addFeature(sf);
	}

	curationSet.setResults(sfset);
	curationSet.setAnnots(new StrandedFeatureSet(new FeatureSet(),
                                                     new FeatureSet()));
	curationSet.setName(chr + "." + chrstart + "-" + chrend);

	//super.notifyLoadingDone();

        return curationSet;
    }
  public Properties getStateInformation() {
    Properties props = new Properties();
    return props;
  }

  public void setStateInformation(Properties props) {
  }
  public SequenceI getSequence(String id) throws ApolloAdapterException {
    throw new NotImplementedException();
  }
  public SequenceI getSequence(DbXref dbxref) throws ApolloAdapterException {
    throw new NotImplementedException();
  }

  public SequenceI getSequence(DbXref dbxref, int start, int end)
  throws apollo.dataadapter.ApolloAdapterException {
    throw new NotImplementedException();
  }

  public Vector    getSequences(DbXref[] dbxref)
  throws apollo.dataadapter.ApolloAdapterException {
    throw new NotImplementedException();
  }
  public Vector    getSequences(DbXref[] dbxref, int[] start, int[] end)
  throws apollo.dataadapter.ApolloAdapterException {
    throw new NotImplementedException();
  }
  public void commitChanges(CurationSet curationSet)
  throws apollo.dataadapter.ApolloAdapterException {
    throw new NotImplementedException();
  }
  public String getRawAnalysisResults(String id) throws apollo.dataadapter.ApolloAdapterException {
    throw new NotImplementedException();
  }

    public void setRegion(String region) {
	this.region = region;

    }

  protected String getRegion(){
    return region;
  }

  protected SequenceI getReferenceSequence(){
      return null;
  }

}





