package apollo.dataadapter;

import java.util.*;
import java.io.*;
import java.net.*;
import javax.swing.JOptionPane;

import apollo.seq.io.*;
import apollo.datamodel.*;
import apollo.config.Config;

import org.apache.log4j.*;

import org.bdgp.io.*;
import org.bdgp.util.*;
import java.util.Properties;
import org.bdgp.swing.widget.*;

/**
 * I'm a variant of a GFFAdapter that does NO translation of the features
 * I read into sets of features (ie no post-processing of the data). 
 * That is, I will read features (or feature pairs)
 * and place them into a StrandedFeatureSet, place those into a curation set,
 * and hand out the curation set.
**/
public class BaseGFFAdapter extends AbstractApolloAdapter {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(BaseGFFAdapter.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  String filename;
//  String region; // not used

  IOOperation [] 
    supportedOperations = 
      {
        ApolloDataAdapterI.OP_READ_DATA,
        ApolloDataAdapterI.OP_WRITE_DATA
      };

  private GFFAdapterGUI gFFAdapterGUI;
  Sequence seq;
  String   seqfile;
  Properties stateInformation = new Properties();

  public void init() {}

  public String getType() {
    return "Ensembl GFF File";
  }

  public DataInputType getInputType() {
    return DataInputType.FILE;
  }
  public String getInput() {
    return filename;
  }

  public IOOperation [] getSupportedOperations() {
    return supportedOperations;
  }

  public DataAdapterUI getUI(IOOperation op) {
    gFFAdapterGUI = null;  // I'm hoping this will help prevent a memory leak
    gFFAdapterGUI = new GFFAdapterGUI(op);

    return gFFAdapterGUI;
  }

  // Must be instantiated with the name of a GFF file
  public BaseGFFAdapter(String filename) {
    this.filename = filename;
    setName("Ensembl GFF file format");
  }

  public BaseGFFAdapter() {
    setName("Ensembl GFF file format");
  }

  /**
   * determines whether the entered 'filename' is in fact a url.If it is then retrieves
   the GFF file via HTTP, to a temp file, and sets fileName to this temp file. Otherwise,
   simply sets fileName to the entered filename
   *
   */
  public void setFilename(String filename) throws  ApolloAdapterException{
    try{
      if (filename.startsWith("http:") == true) {
        InputStream stream = null;
        filename = filename.trim();
        stream = getStreamFromUrl(getUrlFromString(filename),"URL "+filename+" not found");
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));

        File f = File.createTempFile(filename.substring(filename.lastIndexOf("/"))+".",".gff");
        f.deleteOnExit();
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(f)));
        String s;

        while ((s = br.readLine()) != null) {
          //System.out.println(s);
          pw.println(s);
        }

        pw.close();

        this.filename = f.getAbsolutePath();

        f.deleteOnExit();

      } else {

        this.filename = filename;
      }
    }
    catch (IOException e){
      logger.error(e.getMessage(), e);
      throw new ApolloAdapterException(e);
    }
  }



  public String getFilename() {
    return filename;
  }

  public void setDataInput(DataInput dataInput) { // throw DataAdapterException?
    if (!dataInput.isFile()) // only takes files
      return;
    try {
      setFilename(dataInput.getFilename());
    } catch (ApolloAdapterException e) { // this should probably be thrown
      throw new RuntimeException(e.getMessage());
    }
    if (dataInput.hasSequenceFilename())
      setSequenceFilename(dataInput.getSequenceFilename());
  }


  /** Makes InputStream from URL - throws DataAdapterException with
   * notFoundMessage if URL is not found */
  private InputStream getStreamFromUrl(URL url, String notFoundMessage)
    throws ApolloAdapterException {

    InputStream stream=null;
    if (url == null) {
      String message = "Couldn't find url for " + filename;
      logger.error(message);
      throw new ApolloAdapterException(message);
    }
    if (url != null) {
      try {
        stream = url.openStream();
      } catch (IOException e) {
        logger.error(e.getMessage(), e);
        stream = null;
        throw new ApolloAdapterException(e);
      }
    }
    return stream;
  }

  /**
   * turns a string into a URL object
   */
  private URL getUrlFromString(String urlString) throws ApolloAdapterException{
    URL url;

    try {
      url = new URL(urlString);
    } catch ( MalformedURLException ex ) {
      logger.error(ex.getMessage(), ex);
      throw new ApolloAdapterException(ex);
    }

    return(url);
  }


// this is not used! im commenting it out
//   public void setRegion(String region) throws DataAdapterException {
//     this.region = region;
//   }

  public Properties getStateInformation() {
    Properties props = new Properties();
    props.put(StateInformation.DATA_FILE_NAME, filename);
    props.put(StateInformation.SEQUENCE_FILE_NAME, getSequenceFilename());
    return props;
  }

  public void setStateInformation(Properties props) {
    logger.info("GFF adapter - set state info - props: "+props);
    stateInformation.putAll(props);
    String name = props.getProperty(StateInformation.DATA_FILE_NAME);
    if(name != null && name.trim().length() > 0){
      try{
        setFilename(name);
      }catch(apollo.dataadapter.ApolloAdapterException exception){
        throw new NonFatalDataAdapterException(exception.getMessage());
      }
    }
    
    name = props.getProperty(StateInformation.SEQUENCE_FILE_NAME);
    if(name != null && name.trim().length() > 0){
      setSequenceFilename(name);
    }
  }

  public CurationSet getCurationSet() throws ApolloAdapterException {
    String file;
    try{
      CurationSet curationSet = new CurationSet();
      curationSet.setAnnots(getAnnotRegion());
      curationSet.setResults(getAnalysisRegion());
      curationSet.setName(filename);
      curationSet.setStart (curationSet.getResults().getLow());
      curationSet.setEnd (curationSet.getResults().getHigh());
      
      if(getSequenceFilename() !=null &&
         getSequenceFilename().trim().length() >0){
        file = findFile(getSequenceFilename());
        if (file != null ) {
          setSequenceFilename(file);
    try {
      readSequence(file, curationSet);
    }
    catch (Exception e) {
      String message = "Couldn't parse sequence file " + file +
              "\nAre you sure it's in FASTA format?\n";
      logger.warn(message);
      JOptionPane.showMessageDialog(null,message,"WARNING",
                                          JOptionPane.WARNING_MESSAGE);
    }
        } else {
          throw new ApolloAdapterException("couldn t find file for name: "+file);
        }
      } 
      
      //super.notifyLoadingDone();
      return curationSet;
    }catch(org.bdgp.io.DataAdapterException exception){
      throw new ApolloAdapterException(exception.getMessage(), exception);
    }
  }
  
  protected GFFFile getGFFFile() throws org.bdgp.io.DataAdapterException{
    try {
      String fullFilePath = findFile(filename);
      filename = fullFilePath;
      return new GFFFile(fullFilePath,"File");
    } catch (IOException e) {
      throw new org.bdgp.io.DataAdapterException(e, "Can't read file " + filename + " " + e);
    }
  }

  protected String findFile(String name) throws org.bdgp.io.DataAdapterException {

    try {
      fireProgressEvent(new ProgressEvent(this, new Double(1.0),
                                          "Finding file..."));

      // If we couldn't find filename, and it's a relative, rather than absolute, path,
      // try prepending APOLLO_ROOT.  --NH, 01/2002
      // but check that its not already prepended
      File input = new File(name);

      if (input == null || !(input.canRead())) {
        String rootdir = System.getProperty("APOLLO_ROOT");
        if (!(name.startsWith("/")) && !(name.startsWith("\\"))
            && !name.startsWith(rootdir)) {
          String absolute = rootdir + "/" + name;
          input = new File(absolute);
          if (input != null && input.canRead())
            name = absolute;
          else {
            // Try sticking "data/" in after APOLLO_ROOT
            absolute = rootdir + "/data/" + name;
            input = new File(absolute);
            if (input != null && input.canRead())
              name = absolute;
            else {
              throw new org.bdgp.io.DataAdapterException("Error: could not open file " + name + " for reading.");
            }
          }
        }
      }

      super.clearOldData(); // filename ok - clear out old data

      fireProgressEvent(new ProgressEvent(this, new Double(1.0),
                                          "Beginning parse..."));

      return name;

    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new org.bdgp.io.DataAdapterException(e, e.getMessage());
    }
  }//end  getGFFFile

  // extID is ignored here, all data from file is returned
  public StrandedFeatureSetI getAnalysisRegion()
  throws org.bdgp.io.DataAdapterException {
    try{
      GFFFile gff = getGFFFile();

      fireProgressEvent(new ProgressEvent(this, new Double(50.0),
                                          "Populating data structures..."));

      StrandedFeatureSet fset = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());

      Vector features = gff.seqs;

      for(int i=0; i<features.size(); i++){
        fset.addFeature((SeqFeatureI)features.get(i));
      }//end for

      fireProgressEvent(new ProgressEvent(this, new Double(100.0),
                                          "Done..."));
      return fset;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new org.bdgp.io.DataAdapterException(e, e.getMessage());
    }
  }

  // extID is ignored here, all data from file is returned
  public StrandedFeatureSetI getAnnotRegion() {
    StrandedFeatureSetI annotations
      = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
    return annotations;
  }

  public static void main(String [] args) throws Exception {
    DataAdapterRegistry
    registry
    = new DataAdapterRegistry
      ();
    registry.installDataAdapter("apollo.dataadapter.GFFAdapter");
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
  public FeatureSetI getAnnotatedRegion() throws org.bdgp.io.DataAdapterException {
    return getAnnotRegion();
  }

  public SequenceI getSequence(String id) throws ApolloAdapterException {
    throw new NotImplementedException();
  }
  public SequenceI getSequence(DbXref dbxref) throws ApolloAdapterException {
    throw new NotImplementedException();
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
  public String getRawAnalysisResults(String id) throws ApolloAdapterException {
    throw new NotImplementedException();
  }

  public void commitChanges(CurationSet curation) throws ApolloAdapterException {
    FileOutputStream fos = null;

    try {
      // If we have a relative path, save in APOLLO_ROOT/data
      if (!(filename.startsWith("/"))) {
        String rootdir = System.getProperty("APOLLO_ROOT");
        if (filename.startsWith("data/"))
          filename = rootdir + "/" + filename;
        else
          filename = rootdir + "/data/" + filename;
      }

      fos = new FileOutputStream(filename);

      if (fos != null) {
        writeGFF(curation, fos);
        logger.info("Saved GFF to " + filename);
      }
    
      if (seqfile != null) {
        SequenceI seq = curation.getRefSequence();

        if (seq != null) {
          saveSequence(seq);
        }
      }
    } catch ( Exception ex ) {
      logger.error("caught exception opening " + filename + ": " + ex.getMessage(), ex);
      throw new ApolloAdapterException("caught exception opening " + ex.getMessage(), ex);
    }
    
    try {
      fos.close();
    } catch ( Exception ex ) {
      logger.error("caught exception closing " + filename + ": " + ex.getMessage(), ex);
      throw new ApolloAdapterException("caught exception closing " + ex.getMessage(), ex);
    }
    
  }

  public void writeGFF(CurationSet curation, OutputStream os) {
    // might want to add buffering via BufferedOutputStream in here
    DataOutputStream dos;

    if (os instanceof DataOutputStream) {
      dos = (DataOutputStream)os;
    } else {
      dos = new DataOutputStream(os);
    }

    try {
      dos.writeBytes(writeAnnotations(curation));

      dos.writeBytes(writeAnalyses(curation));

    } catch ( Exception ex ) {
      logger.error("caught exception committing GFF: " + ex.getMessage(), ex);
    }

  }


  public String writeAnnotations (CurationSet curation) {

    StringBuffer buf = new StringBuffer();

    StrandedFeatureSetI annots = curation.getAnnots();

    if (annots != null) {
      FeatureSetI forward = annots.getForwardSet();
      for (int i = 0; i < forward.size(); i++) {
  if (forward.getFeatureAt(i) instanceof AnnotatedFeatureI) {
    AnnotatedFeatureI sf = (AnnotatedFeatureI) forward.getFeatureAt(i);
    buf.append (writeAnnotation (sf));
  }
      }
      FeatureSetI reverse = annots.getReverseSet();
      for (int i = 0; i < reverse.size(); i++) {
  if (reverse.getFeatureAt(i) instanceof AnnotatedFeatureI) {
    AnnotatedFeatureI sf = (AnnotatedFeatureI) reverse.getFeatureAt(i);
    buf.append (writeAnnotation (sf));
  }
      }
    }
    return buf.toString();
  }

  public String writeAnnotation (AnnotatedFeatureI sf) {

    StringBuffer buf = new StringBuffer();

    boolean isa_gene = sf.isProteinCodingGene();

    Vector transcripts = sf.getFeatures ();
    for (int i = 0; i < transcripts.size (); i++) {
      Transcript fs = (Transcript) transcripts.elementAt (i);
      buf.append (writeTranscript(fs, isa_gene));
    }

    return buf.toString();
  }

  public String writeTranscript(Transcript trans,
                                boolean isa_gene) {

    StringBuffer buf = new StringBuffer();

    buf.append(GFFFile.print(trans));
    return buf.toString();
  }

  public String writeAnalyses (CurationSet curation) {
    StringBuffer buf = new StringBuffer();

    StrandedFeatureSetI analyses = curation.getResults();

    if (analyses != null) {
      FeatureSetI forward_analyses = analyses.getForwardSet();
      for (int i = 0; i < forward_analyses.size(); i++) {
  FeatureSetI analysis
    = (FeatureSetI) forward_analyses.getFeatureAt(i);
  buf.append (writeAnalysis (analysis));
      }
      
      FeatureSetI reverse_analyses = analyses.getReverseSet();
      for (int i = 0; i < reverse_analyses.size(); i++) {
  FeatureSetI analysis
    = (FeatureSetI) reverse_analyses.getFeatureAt(i);
  buf.append (writeAnalysis (analysis));
      }
    }

    return buf.toString();
  }

  public String writeAnalysis (FeatureSetI sf) {
    StringBuffer buf = new StringBuffer();

    /*
        FeatureProperty property
        = Config.getPropertyScheme().getFeatureProperty(sf.getType());
        buf.append(indent + "  <type>"+ property.getType() +"</type>\n");
    */

    buf.append(GFFFile.print(sf));
    return buf.toString();
  }
  
  public Sequence getSequence() {
    return seq;
  }

  public void readSequence(String file, CurationSet curation) 
    throws org.bdgp.io.DataAdapterException{
    try { // throws IOException
      FastaFile ff = new FastaFile(file, "File", curation);
      seq = (Sequence)ff.getSeqs().elementAt(0);
    } catch (IOException e) {
      throw new org.bdgp.io.DataAdapterException(e,
                                                 "Error reading sequencefile "
                                                 + file + " " + e);
    }
  }

  public void setSequenceFilename(String seqfile) {
    this.seqfile = seqfile;
  }

  public String getSequenceFilename() {
    return seqfile;
  }

  public void saveSequence(SequenceI seq) throws org.bdgp.io.DataAdapterException {

    try {
      //      File outfile = new File(seqfile);
      // This doesn't work if seqfile doesn't exist yet
      //      FileOutputStream     fos = new FileOutputStream(outfile);

      String filename = apollo.util.IOUtil.findFile(seqfile, true);
      if (filename == null || filename.equals(""))
        return;

      FileOutputStream fos = null;
      try {
        fos = new FileOutputStream(filename);
      } catch ( Exception ex ) {
        logger.error("Couldn't write to " + seqfile, ex);
      }
      if (fos == null)
        return;

      BufferedOutputStream bos = new BufferedOutputStream(fos);
      PrintStream ps           = new PrintStream(bos);

      SequenceI[] sa = new SequenceI[1];
      sa[0] = seq;

      if (seq.getDescription() != null) {
        seq.setName(seq.getName() + " " + seq.getDescription());
      }

      ps.print(FastaFile.print(sa));
      ps.flush();
      ps.close();
      logger.info("Saved sequence to " + filename);
    } catch (Exception e) {
      logger.error("Exception trying to save sequence to " + seqfile, e);
    }
  }

  public void clearStateInformation() {
    stateInformation = new Properties();
  }
  
}
