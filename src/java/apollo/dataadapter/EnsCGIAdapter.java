package apollo.dataadapter;

import java.util.*;
import java.io.*;
import java.util.Properties;

import apollo.seq.io.*;
import apollo.datamodel.*;
import apollo.datamodel.seq.SRSSequence;
import apollo.datamodel.seq.EnsCGISequence;
import apollo.config.Config;

import org.bdgp.io.*;
import org.bdgp.util.*;
import org.bdgp.swing.widget.*;

public class EnsCGIAdapter extends AbstractApolloAdapter {

  String path;
  String region;
  String chr;
  int start;
  int end;
  int mode;
  private SequenceI genomeSeq = null;

  IOOperation [] supportedOperations = {
                                         ApolloDataAdapterI.OP_READ_DATA,
                                         ApolloDataAdapterI.OP_READ_SEQUENCE
                                       };

  public void init() {}

  public String getName() {
    return "Ensembl - web site access for homo sapiens";
  }

  public String getType() {
    return "CGI server";
  }

  public DataInputType getInputType() {
    if (mode==EnsCGIAdapterGUI.RANGE)
      return DataInputType.BASEPAIR_RANGE;
    else
      return DataInputType.CONTIG;
  }
  public String getInput() {
    return region; // is this right??
  }

  public void setupAdapter(String path) throws ApolloAdapterException {
    this.path = path;
  }

  public IOOperation [] getSupportedOperations() {
    return supportedOperations;
  }

  public DataAdapterUI getUI(IOOperation op) {
    return new EnsCGIAdapterGUI(op);
  }

  public EnsCGIAdapter(String filename) {
    this.path = path;
  }

  public EnsCGIAdapter() {}

  public void setPath(String path) {
    this.path = path;
  }

  public void setRegion(String region) throws ApolloAdapterException {
    this.region = region;
    if (region.substring(0,4).equals("Chr ")) {
      RangeI loc = EnsCGIAdapterGUI.parseChrStartEndString(region);
      if (loc != null) {
        start = loc.getStart();
        end = loc.getEnd();
        chr = loc.getName();
        mode = EnsCGIAdapterGUI.RANGE;
      }
    } else {
      mode = EnsCGIAdapterGUI.REGION;
    }
  }
  public void setStart(String start) throws ApolloAdapterException {
    this.start = Integer.parseInt(start,10);
  }
  public void setChr(String chr) throws ApolloAdapterException {
    this.chr = chr;
  }
  public void setEnd(String end) throws ApolloAdapterException {
    this.end = Integer.parseInt(end,10);
  }
  public void setGetMode(int mode) {
    this.mode = mode;
  }

  public Properties getStateInformation() {
    Properties props = new Properties();

    props.put("path", path);
    return props;
  }

  public void setStateInformation(Properties props) {
    setPath(props.getProperty("path"));
  }

  public CurationSet getCurationSet() throws ApolloAdapterException {
    //super.getCurationSet(); -> clearOldData()
    //apollo.config.Style previousStyle = Config.getStyle();
    //Config.newDataAdapter(this);
    // is there anyway the input can fail? if so this needs to go after that
    super.clearOldData();  
    CurationSet curationSet = new CurationSet();
    //try {
      curationSet.setAnnots(new StrandedFeatureSet(new FeatureSet(), 
                                                   new FeatureSet()));
      curationSet.setResults((StrandedFeatureSetI)getAnalysisRegion(curationSet));
      //} catch (DataAdapterException e) {
      // If load fails set style back to what it was
      //if (previousStyle!=null) Config.setStyle(previousStyle);
      //throw e;
      //}
    curationSet.setName(chr);
    curationSet.setChromosome(chr);
    // assuming for now that the user types in something that starts
    // at 0 (for everything after that) and ends with the length
    // steve will correct me latter if i have this wrong
    curationSet.setLow(start);
    curationSet.setHigh(end);
    try {
      genomeSeq = getSequence(new DbXref(region,region,region));
      curationSet.setRefSequence (genomeSeq);
      System.out.println ("Annotated Sequence: " +
                          genomeSeq.getName() + " " +
                          curationSet.length() + " bases");
    } catch (Exception e) {
      throw new ApolloAdapterException("Load failed. Are you sure " + region
                                     + "is a real sequence?", e);
    }
    return curationSet;
  }

  // extID is ignored here, all data from file is returned
  public FeatureSetI getAnalysisRegion(CurationSet curationSet) throws ApolloAdapterException {
    try {
      //c.fireStatusEvent(new StatusEvent(this,"Showing region Chr: " + chr + " " + start + "-" + end));

      Hashtable var = new Hashtable();

      if (mode == EnsCGIAdapterGUI.RANGE) {
        var.put("chr",chr);
        var.put("vc_start",String.valueOf(new Integer(start)));
        var.put("vc_end", String.valueOf(new Integer(end)));
        var.put("est", "on");
        if (!Config.getDBHost().equals("")) {
          var.put("dbhost", Config.getDBHost());
        }
        if (!Config.getDBName().equals("")) {
          var.put("dbname", Config.getDBName());
        }
        if (!Config.getGPName().equals("")) {
          var.put("gp", Config.getGPName());
        }
      } else {
        var.put("contig",region);
      }


      //      String host = Config.getCGIHost();
      String host = path;
      int port = Config.getCGIPort();
      String cgistr = Config.getCGIScript();

      fireProgressEvent(new ProgressEvent(this,new Double(0.0),
                                          "Contacting Ensembl server..."));
      CGI cgi = new CGI(host,port,cgistr,var,System.out);

      fireProgressEvent(new ProgressEvent(this,new Double(25.0),
                                          "Requesting region..."));
      cgi.run();

      fireProgressEvent(new ProgressEvent(this, new Double(50.0),
                                          "Getting data..."));
      // First line should be genomic location
      RangeI returnedRange;
      try {
        String data = cgi.getInput().readLine();
        if (data != null && data.startsWith("GENOMIC RANGE: ")) {

          String region = data.substring("GENOMIC RANGE: ".length());
          returnedRange = EnsCGIAdapterGUI.parseChrStartEndString(region);
          System.out.println("Got range: " + returnedRange.getName() + " "+ returnedRange.getStart() + "-" + returnedRange.getEnd());
          if (mode == EnsCGIAdapterGUI.REGION) {
            start = returnedRange.getStart();
            end = returnedRange.getEnd();
            chr = returnedRange.getName();
          }
        } else {
          throw new ApolloAdapterException("Didn't find GENOMIC RANGE: line");
        }
      } catch (IOException ioex) {
        System.out.println("Exception " + ioex);
      }


      GFFFile gff = new GFFFile(cgi.getInput());

      fireProgressEvent(new ProgressEvent(this, new Double(75.0),
                                          "Populating data structures..."));

      for (int i = 0; i < gff.seqs.size(); i++) {
        SeqFeatureI sf = (SeqFeatureI) gff.seqs.elementAt(i);
        //	  System.out.println ("Adding ensembl feature" +
        //			      sf.getName() + ":" + sf.getType());
        setSequence(sf, curationSet);
      }

      StrandedFeatureSet fset = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
      FeatureSetBuilder fsb = new FeatureSetBuilder();
      fsb.makeSetFeatures (fset, gff.seqs, Config.getPropertyScheme());

      fireProgressEvent(new ProgressEvent(this, new Double(100.0),
                                          "Done..."));

      return fset;
    } catch (Exception e) {
      e.printStackTrace();
      throw new ApolloAdapterException(e);
    }
  }
  private void setSequence(SeqFeatureI sf, CurationSet curationSet) {

    if (sf instanceof FeaturePair) {
      FeaturePair pair = (FeaturePair)sf;
      String      name = pair.getHname();
      SequenceI   seq  = curationSet.getSequence(name);
      if (seq == null) {
        seq = new SRSSequence(name,Config.getController());
        //if (seq.getLength() == 0)
        //  seq = new Sequence(name,"");
        curationSet.addSequence(seq);

      }
      pair.getHitFeature().setRefSequence(seq);
    }
  }

  public static void main(String [] args) throws Exception {
    //Config.readConfig("/mnt/Users/jrichter/cvs/apollo/data/apollo.cfg");
    DataAdapterRegistry
    registry
    = new DataAdapterRegistry
      ();
    registry.installDataAdapter("apollo.dataadapter.EnsCGIAdapter");
    registry.installDataAdapter("apollo.dataadapter.SerialDiskAdapter");
    registry.installDataAdapter("apollo.dataadapter.gamexml.GAMEAdapter");
    DataAdapterChooser chooser = new DataAdapterChooser(
                                   registry
                                   ,
                                   ApolloDataAdapterI.OP_READ_DATA,
                                   "Load data",
                                   null,
                                   false);
    chooser.setPropertiesFile(new File("/mnt/Users/jrichter/cvs/apollo/src/java/apollo/dataadapter/junk.history"));
    chooser.show();

  }

  // returns an empty AnnotatedFeatureSet, because I don't know if GFF files can even
  // contain annotations, and if they can, I don't know how to fetch them
  public FeatureSetI getAnnotatedRegion() throws ApolloAdapterException {
    return new StrandedFeatureSet(new FeatureSet(), 
                                  new FeatureSet());
  }

  public SequenceI getSequence(String id) throws ApolloAdapterException {
    throw new NotImplementedException();
  }
  public SequenceI getSequence(DbXref dbxref) throws ApolloAdapterException {
    if (dbxref.getIdValue().equals(region)) {
      EnsCGISequence ecs = new EnsCGISequence(region,Config.getController(),
                                              new apollo.datamodel.Range(chr,start,end),path);
      ecs.getCacher().setMaxSize(1000000);
      return ecs;
    } else {
      throw new NotImplementedException();
    }
  }

  public SequenceI getSequence(DbXref dbxref, int start, int end)
  throws ApolloAdapterException {
    throw new NotImplementedException();
  }

  public Vector    getSequences(DbXref[] dbxref)
  throws ApolloAdapterException {
    throw new NotImplementedException();
  }
  public Vector    getSequences(DbXref[] dbxref, int[] start, int[] end)
  throws ApolloAdapterException {
    throw new NotImplementedException();
  }
  public void commitChanges(CurationSet curationSet)
  throws ApolloAdapterException {
    throw new NotImplementedException();
  }
  public String getRawAnalysisResults(String id) throws ApolloAdapterException {
    throw new NotImplementedException();
  }
}
