/* Written by Berkeley Drosophila Genome Project.
   Copyright (c) 2000 Berkeley Drosophila Genome Center, UC Berkeley. */

package apollo.dataadapter.gamexml;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileReader;
import java.io.File;
import java.io.BufferedReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.apache.log4j.*;

import org.bdgp.io.DataAdapterUI;
import org.bdgp.io.IOOperation;
import org.bdgp.util.ProgressEvent;
import org.bdgp.xml.XMLElement;

import apollo.config.ApolloNameAdapterI;
import apollo.config.Config;
import apollo.config.Style;
import apollo.dataadapter.AbstractApolloAdapter;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.DataInput;
import apollo.dataadapter.DataInputType;
import apollo.dataadapter.DataInputType.UnknownTypeException;
import apollo.dataadapter.Region;
import apollo.dataadapter.StateInformation;
import apollo.dataadapter.TransactionOutputAdapter;
import apollo.dataadapter.chado.ChadoTransactionTransformer;
import apollo.dataadapter.chadoxml.ChadoTransactionXMLWriter;
import apollo.datamodel.AnnotatedFeature;
import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.Comment;
import apollo.datamodel.CurationSet;
import apollo.datamodel.DbXref;
import apollo.datamodel.Exon;
import apollo.datamodel.ExonI;
import apollo.datamodel.FeaturePair;
import apollo.datamodel.FeaturePairI;
import apollo.datamodel.FeatureSet;
import apollo.datamodel.FeatureSetI;
import apollo.datamodel.Protein;
import apollo.datamodel.RangeI;
import apollo.datamodel.SeqFeature;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.Sequence;
import apollo.datamodel.SequenceI;
import apollo.datamodel.StrandedFeatureSet;
import apollo.datamodel.StrandedFeatureSetI;
import apollo.datamodel.Synonym;
import apollo.datamodel.Transcript;
import apollo.datamodel.seq.GAMESequence;
import apollo.editor.Transaction;
import apollo.editor.TransactionManager;
import apollo.main.Version;
import apollo.util.DateUtil;
import apollo.util.FastaHeader;
import apollo.util.HTMLUtil;
import apollo.util.IOUtil;
import apollo.main.*;

/**
 * Reader for GAME XML files.
 *
 * WARNING -- AElfred (and other SAX drivers) _may_ break large
 * stretches of unmarked content into smaller chunks and call
 * characters() for each smaller chunk
 * CURRENT IMPLEMENTATION DOES NOT DEAL WITH THIS
 * COULD CAUSE PROBLEM WHEN READING IN SEQUENCE RESIDUES
 * haven't seen a problem yet though -- GAH 6-15-98
 * GAMEAdapter is presently not a singleton. There is separate instances for synteny
 * and non-synteny/one-species. This may change in future.
 */
public class GAMEAdapter extends AbstractApolloAdapter {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(GAMEAdapter.class);

  // Preserve original filename so that at time of saving, we can fetch
  // the preamble from the original input file
  static String originalFilename = null;

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  /* This indicates no GUI so certain non-apollo, non-gui apps can use this
     adapter. */
  protected boolean NO_GUI = false;

  String region;

  /** Root of the parse tree for the XML input */
  XMLElement game_element = null;

  int element_count = 0;

  // CurationSet curation = null;
  SequenceI curated_seq = null;

  // These need to be global to allow easy layering
  StrandedFeatureSetI analyses = null;
  Hashtable all_analyses = null;

  /** Amount to pad left */
  private int padLeft=-1;
  private int padRight=-1;

  // Why bother having both of these if they're the same?
  private String NAME_LABEL_FOR_WRITING = "GAME XML format";
  private String NAME_LABEL_FOR_READING = "GAME XML format";

  IOOperation [] supportedOperations = {
    ApolloDataAdapterI.OP_READ_DATA,
    ApolloDataAdapterI.OP_WRITE_DATA,
    ApolloDataAdapterI.OP_READ_SEQUENCE,
    ApolloDataAdapterI.OP_APPEND_DATA
  };

  public GAMEAdapter() {
    setInputType(DataInputType.FILE);  // default--but when is it URL?
    // Set the name that is used in the Data Adapter Chooser dropdown list.
    // Can be ovverridden by an optional third arg in the DataAdapterInstall
    // line in apollo.cfg, e.g.
    // DataAdapterInstall	"apollo.dataadapter.gamexml.GAMEAdapter" "rice.style" "Rice annotations (GAME XML format)"
    setName(NAME_LABEL_FOR_READING);
  }

  public GAMEAdapter(DataInputType inputType, String input) {
    setInputType(inputType);
    setInput(input);
  }

  public GAMEAdapter(DataInputType inputType, String input, boolean noGUI) {
    NO_GUI = noGUI;
    setInputType(inputType);
    setInput(input);
  }

  public void init() {}

  /**
   * org.bdgp.io.DataAdapter method
   */
  public String getType() {
    return "GAME XML source (filename, URL, gene=cact, band=34A, or location=3L:12345-67890)";
  }

  public IOOperation [] getSupportedOperations() {
    return supportedOperations;
  }

  public void setName(String nameForReading) {
    super.setName(nameForReading);
    NAME_LABEL_FOR_READING = nameForReading;
  }


  public DataAdapterUI getUI(IOOperation op) {
    if (!super.operationIsSupported(op))
      return null; // shouldnt happen
    DataAdapterUI ui = super.getCachedUI(op);
    if (ui == null) {
      ui = new GAMEAdapterGUI(op);
      super.cacheUI(op,ui);
    }
    if (op.equals(OP_WRITE_DATA))
      // It would be great to put the GAME version string in the name, but
      // at the time the GAME adapter GUI is set up, we don't yet know which
      // GAME version we'll be using (because Config hasn't yet read
      // DO-ONE-LEVEL-ANNOTS from apollo.cfg)
      super.setName(NAME_LABEL_FOR_WRITING); // + " (version " + GAMESave.gameVersion  + ")");
    else
      super.setName(NAME_LABEL_FOR_READING);
    return ui;
  }

  /** Request to "pad" the input padLeft basepairs to the left(5' forward strand) -
      Could do this in AbstractApolloAdapter but only game adapter needs this now */
  public void setPadLeft(int padLeft) { this.padLeft = padLeft; }
  /** Request to "pad" the input padRight basepairs to the right(3' forward strand) */
  public void setPadRight(int padRight) { this.padRight = padRight; }

  private int getPadLeft() { 
    if (padLeft == -1) padLeft = getGAMEStyle().getDefaultPadding();
    return padLeft;
  }
  private int getPadRight() {
    if (padRight == -1) padRight = getGAMEStyle().getDefaultPadding();
    return padRight;
  }
    

  public Properties getStateInformation() {
    StateInformation props = new StateInformation();
    props.put(StateInformation.INPUT_TYPE,getInputType().toString());
    if (getInput() != null)
      props.put(StateInformation.INPUT_STRING,getInput());
    return props;
  }

  /** DataLoader calls this. input, inputType and optionally database set here.
      MovementPanel(nav bar) sets db to default db (it has no db chooser) */
  public void setStateInformation(Properties props) {
    String typeString = props.getProperty(StateInformation.INPUT_TYPE);
    try {
      DataInputType type = DataInputType.stringToType(typeString);
      String inputString = props.getProperty(StateInformation.INPUT_STRING);
      setDataInput(new DataInput(type,inputString));
    }
    catch (UnknownTypeException e) {
      logger.error("Cannot set game adapter state info", e);
   }
   
  }

  public void setRegion(SequenceI seq) throws ApolloAdapterException {
    if (seq != null) {
      this.curated_seq = seq;
      setRegion (seq.getName());
    }
  }

  /** Returns true as game data contains link data that can be used by 
      synteny */
  public boolean hasLinkData() { return true; }

  /** Save original file name so that if we save the data we can access the
   *  header info from the original file; and also so we can save the file name
   *  as a comment. */
  public void setOriginalFilename(String file) {
    logger.debug("GAMEAdapter.setOriginalFilename: setting original filename = " + file);
    this.originalFilename = file;
  }

  /** Tests all the input types - can enter filename as arg */
  public static void main(String [] args) throws ApolloAdapterException {
    GAMEAdapter databoy;

    // So I mistakedly made input type here FILE and it caused an endless loop
    // probably in getStreamFromFile (recursive call there)
    String url = "http://www.fruitfly.org/annot/gbunits/xml/AE003650.xml";
    databoy = new GAMEAdapter(DataInputType.URL,url);
    testAdapter(databoy);

    databoy = new GAMEAdapter(DataInputType.GENE,"cact");//args[0]);
    testAdapter(databoy);

    databoy = new GAMEAdapter(DataInputType.CYTOLOGY,"34A");
    testAdapter(databoy);

    databoy = new GAMEAdapter(DataInputType.SCAFFOLD,"AE003490");
    testAdapter(databoy);

    String file = "/users/mgibson/cvs/apollo/dev/sanger/data/josh";
    if (args.length > 0) file = args[0];
    databoy = new GAMEAdapter(DataInputType.FILE,file);
    testAdapter(databoy);

    String seq = "actggcgtgctgtgttattagtgatgatgtcgcaatcgtgaatcgatgcatgcacacatcgtgtgtgtggtctgcgaatatggcattccgtaaagtgccgcgcgtatgtcgcgcgattatgatgtatgctgctgatgtagctgtgatattctaatgagtgctgatcgtgatgtagtcgtagtctagctagctagtcgatcgtagctacgtagctagctagcttgtgtgcgcgcgctg";
    databoy = new GAMEAdapter(DataInputType.SEQUENCE,seq);
    testAdapter(databoy);
  }
  private static void testAdapter(GAMEAdapter databoy) {
    try {
      CurationSet curation = databoy.getCurationSet();
      apollo.dataadapter.debug.DisplayTool.showFeatureSet(curation.getResults());
    } catch ( ApolloAdapterException ex ) {
      logger.error("No data to read", ex);
    }
  }


  public void commitChanges(CurationSet curation) {
    commitChanges(curation, true, true);  // Default is to save both annots and results
  }

  /**
   * Writes XML
   * If the input type is not FILE, prompts user for a file to save to.
   */
  public void commitChanges(CurationSet curation, boolean saveAnnots, boolean saveResults) {
    // If this was called by DataLoader.putCurationSet because user selected
    // "Save first, then quit" from "Annotations are changed" dialog, we should
    // deal with non-files by prompting for a filename, like "Save As" in 
    // FileMenu.
    if (getInputType() != DataInputType.FILE) {
      apollo.main.DataLoader loader = new apollo.main.DataLoader();
      // Problem: saveFileDialog ends up calling commitChanges again, with
      // the filename filled in this time.  If that save fails, we will end
      // up returning (which normally is the right thing to do) and that will
      // result in us exiting w/o saving.  We can't easily catch the error,
      // because when we reenter commitChanges, we don't know that we came in
      // from this particular prompt-for-save situation.
      loader.saveFileDialog(curation);
      return;
    }

    String filename = apollo.util.IOUtil.findFile(getInput(), true);
    if (filename == null)
      filename = getInput();
    if (filename == null)
      return;

    // If file already exists, ask user to confirm that they want to overwrite
    // (I'd find this annoying, but Sima asked for it).
    // This question is configged in style (AskConfirmOverwrite), defaulting to true
    if (Config.getConfirmOverwrite()) {
      File handle = new File(filename);
      if (handle.exists()) {
        if (!LoadUtil.areYouSure(filename + " already exists--overwrite?")) {
          // If user said no, prompt them again to choose a filename
          apollo.main.DataLoader loader = new apollo.main.DataLoader();
          loader.saveFileDialog(curation);
          return;
        }
        else {  // Don't ask--just announce that you're overwriting it
          logger.info("GAMEAdapter overwriting existing file " + filename);
        }
      }
    }

    String msg = "Saving data to file " + filename;
    setInput(filename); // input type is FILE
    fireProgressEvent(new ProgressEvent(this, new Double(10.0),msg));
    // does this still write out the old transactions - if so need to take out?
    // or do we need backward compatibility?
    if (GAMESave.writeXML(curation,
                          filename,
                          saveAnnots, saveResults,
                          "Apollo version: " + Version.getVersion(),
                          false)) {
      // Transactions should be associated with any saved file to avoid asynchronization!
      // this is the new transactions (not the old ones)
      saveTransactions(curation, filename);
    }
    else {
      String message = "Failed to save GAME XML to " + filename;
      logger.error(message);
      JOptionPane.showMessageDialog(null,message,"Warning",JOptionPane.WARNING_MESSAGE);
    }
  }
  
  private void saveTransactions(CurationSet curation, String fileName) {
    if (curation.getTransactionManager() == null) 
      return; // Guess no transaction support

    // Save APOLLO/GAME TRANSACTIONS if desired
    try {
      // Make sure Transaction instances coalesced
      curation.getTransactionManager().coalesce();
      if (Config.outputTransactionXML()
          && !getGAMEStyle().transactionsAreInGameFile()) {
        // Save transactions to a GAME transaction format
        saveApolloTransactions(curation, fileName);
      }
    }
    catch(Exception e) {
      logger.error("GAMEAdapter encountered Exception in saveTransactions() (game)", e);
      JOptionPane.showMessageDialog(null, 
                                    "Apollo Transactions cannot be saved.",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
    }

    try {
      // Save transactions in Chado transaction format, if desired
      if (Config.isChadoTnOutputNeeded()) {
        // Save transactions to chado transaction format
        TransactionOutputAdapter output = new ChadoTransactionXMLWriter();
        // change this...
        if (curation.isChromosomeArmUsed()) {
          output.setMapID(curation.getChromosome());
          output.setMapType("chromosome_arm");
        }
        else {
          output.setMapID(curation.getChromosome());
          output.setMapType("chromosome");
        }
        output.setTransformer(new ChadoTransactionTransformer());
        output.setTarget(fileName);
        output.commitTransactions(curation.getTransactionManager());
      }
    }
    catch(Exception e) {
      logger.error("GAMEAdapter encountered Exception in saveTransactions() (chado)", e);
      JOptionPane.showMessageDialog(null, 
                                    "Chado Transactions cannot be saved.",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
    }
    
  }
  
  /**
   * A rather strange implemenation. This method is based on 
   * GAMESave.writeGenomePosition(CurationSet).
   * @param curation
   * @return
   * 12/2005: Not currently used
   */
//   private boolean isChromosomeArmUsed(CurationSet curation) {
//     SequenceI genomic_seq = curation.getRefSequence();
//     return (genomic_seq != null && 
//             genomic_seq.getName() != null &&
//             !curation.getName().equals(genomic_seq.getName()));
//   }
  
  private void saveApolloTransactions(CurationSet curationSet, String fileName) {
    try {
      TransactionXMLAdapter tnAdapter = new TransactionXMLAdapter();
      tnAdapter.setFileName(fileName);
      tnAdapter.save(curationSet.getTransactionManager().getTransactions());
    }
    catch(IOException e) {
      logger.error("GAMEAdapter encountered Exception in saveApolloTransactions()", e);
    }
  }

  /** if file starts with http: then its really an url so correct the type 
   bug fix for being able to load urls from file adapter and with -x */
  public DataInput getDataInput() {
    super.getDataInput().setTypeToFileOrUrl();
    return super.getDataInput();
  }


  /** from ApolloDataAdapterI interface. dataInput should be set
   * previous to this
   */
  public CurationSet getCurationSet() throws ApolloAdapterException {

    CurationSet curation = new CurationSet();
    try {
      fireProgressEvent(new ProgressEvent(this, new Double(5.0),
                                          "Finding data..."));
      // Throws DataAdapter exception if faulty input (like bad filename)
      InputStream xml_stream;
      //System.out.println("opening GAME XML file " + getDataInput());
      logger.debug("opening GAME XML file " + getDataInput());
      xml_stream = xmlInputStream(getDataInput());
      BufferedInputStream bis = new BufferedInputStream(xml_stream);

      /* Now that we know input is not faulty (exception not thrown if we got
         here) we can clear out old data. If we dont do clearing we get 
         exceptions on style changes as apollo tiers get in an inconsistent 
         state between  old and new data */
      if (!NO_GUI) {
        super.clearOldData();
      }

      // All "progress" percentages are made up--we can't actually tell
      // how far along we are, but we want to give the user an idea that
      // progress is being made.
      fireProgressEvent(new ProgressEvent(this, new Double(10.0),
                                          "Reading GAME XML..."));

      XMLParser parser = new XMLParser();
      // Reads all the XML into a tree--game_element is the root.
      // (It would be more memory-efficient to read and process elements one at a
      // time, rather than all at once.)
      game_element = parser.readXML(bis);
      if (game_element == null) {
        String msg = "GAME XML input stream was empty--nothing loaded.";
        logger.error(msg);
        throw new ApolloAdapterException(msg);
      }
      xml_stream.close();

      fireProgressEvent(new ProgressEvent(this, new Double(20.0),
                                          "Getting genomic sequence..."));
      getFocusSequence(curation);
      /* getFocusSequence should have gotten a curated_seq. Even in entries
         with no sequence residues there is a seq entry with "focus" and no residues.
         If there is no entry with "focus" attribute getSequences will fail
         to make a curated_seq. Another approach would be to change adapter
         to allow for a null curated_seq.
         At the moment ya get a null pointer. */
      if (curated_seq==null) {
        String m = ("Failed to load genomic sequence for the entry.\n" +
                    "One possible cause could be the absence of a 'seq' element with a\n" +
                    "'focus=true' attribute, which is required (even if it has no 'residues').\n");
        logger.error(m);
        throw new ApolloAdapterException(m);
      }
      curation.setRefSequence (curated_seq);
      logger.debug("Set reference seq to " + curated_seq.getName() + ", length = " + curation.getRefSequence().getLength());

      fireProgressEvent(new ProgressEvent(this, new Double(25.0),
                                          "Setting genome position..."));
      getGenomePosition (curation);

      fireProgressEvent(new ProgressEvent(this, new Double(30.0),
                                          "Reading annotations and results..."));
      
      // Read them all at once for greater efficiency (less memory use)
      getResultsAndAnnots(curation);

      // this is old transactions - dont need anymore? comment out?
      // loadTransactions();
      //      System.out.println("Ignoring any old-style transactions in GAME XML input file");

      // Set up TransactionManager for curation
      //      TransactionManager manager = new TransactionManager();
      // if loading from file, load associated transactions
      if (getDataInput().isFile()) {
        fireProgressEvent(new ProgressEvent(this, new Double(85.0),
                                            "Reading transaction file..."));
        //java.util.List transactions = 
        TransactionXMLAdapter.loadTransactions(getInput(), curation, getCurationState());
        //manager.setTransactions(transactions);
      }
      //curation.setTransactionManager(manager);
      
      fireProgressEvent(new ProgressEvent(this, new Double(90.0),
                                          "Cleaning up..."));
      logger.info("Completed XML parse of " + curation.getName());
      /* don't delete this. need to eradicate the huge
         hash tables generated when the xml is parsed */
      parser.clean ();
      game_element = null;
      // parser.clean() already calls System.gc--do we really need to call it again?
      //      System.gc();

      fireProgressEvent(new ProgressEvent(this, new Double(95.0),
                                          "Drawing..."));
      // file not found -> data adapter exception
    } catch (ApolloAdapterException dae) { 
      // why create new one? - then original stack trace gets lost
      //throw new DataAdapterException(dae.getMessage());
      throw dae;
    } catch ( Exception ex2 ) {
      // uncommenting the comment and filtering FileNotFound, this is handy 
      // for  file parsing errors
      logger.error("GAMEAdapter encountered Exception in getCurationSet()", ex2);;
      throw new ApolloAdapterException(ex2.getMessage());
    }
    //super.notifyLoadingDone(); // Disposes loading message frame
    
    curation.setInputFilename(originalFilename);
    return curation;
  }

  /** Called if user is layering additional GAME data on top of whatever's
      already been loaded */
  public Boolean addToCurationSet() throws ApolloAdapterException {
    // Existing curation_set is stored in superclass
    boolean okay = false;
    try {
      fireProgressEvent(new ProgressEvent(this, new Double(5.0),
                                          "Finding data..."));
      // Throws DataAdapter exception if faulty input (like bad filename)
      InputStream xml_stream;
      xml_stream = xmlInputStream(getDataInput());
      BufferedInputStream bis = new BufferedInputStream(xml_stream);
      fireProgressEvent(new ProgressEvent(this, new Double(10.0),
                                          "Reading XML..."));

      XMLParser parser = new XMLParser();
      // Reads all the XML into a tree--game_element is the root.
      // (It would be more memory-efficient to read and process elements one at a
      // time, rather than all at once.)
      game_element = parser.readXML(bis);
      if (game_element == null)
        throw new ApolloAdapterException("GAME XML input stream was empty--nothing loaded.");
      xml_stream.close();

      // Should we check whether we already have genomic seq
      // before trying to load a new one?
      //      SequenceI seq = curation_set.getRefSequence();
      fireProgressEvent(new ProgressEvent(this, new Double(20.0),
                                          "Getting genomic sequence..."));
      // Existing curation_set is stored in superclass
      getFocusSequence(curation_set);
      // curated_seq was set by getFocusSequence
      curation_set.setRefSequence (curated_seq);

      fireProgressEvent(new ProgressEvent(this, new Double(25.0),
                                          "Setting genome position..."));
      // Existing curation_set is stored in superclass
      getGenomePosition (curation_set);

      fireProgressEvent(new ProgressEvent(this, new Double(30.0),
                                          "Setting annotations and results..."));

      // Read them all at once for greater efficiency (less memory use)
      getResultsAndAnnots(curation_set);

      //TransactionManager manager = curation_set.getTransactionManager();
      if (getDataInput().isFile()) {
        fireProgressEvent(new ProgressEvent(this, new Double(90.0),
                                            "Reading transaction file..."));
        //java.util.List transactions = 
        TransactionXMLAdapter.loadTransactions(getInput(), curation_set, getCurationState());
        //manager.setTransactions(transactions);
      }
      
      fireProgressEvent(new ProgressEvent(this, new Double(90.0),
                                          "Cleaning up..."));
      logger.info("Completed XML parse of " + curation_set.getName());
      /* don't delete this. need to eradicate the huge
         hash tables generated when the xml is parsed */
      parser.clean ();
      game_element = null;

      fireProgressEvent(new ProgressEvent(this, new Double(95.0),
                                          "Drawing..."));
      okay = true;
      // file not found -> data adapter exception
    } catch (ApolloAdapterException dae) { 
      throw dae;
    } catch ( Exception ex2 ) {
      logger.error("Exception in addToCurationSet", ex2);
      throw new ApolloAdapterException(ex2.getMessage());
    }
    
    return new Boolean(okay);
  }

  // should url be a separate input type? or in file
  private InputStream xmlInputStream(DataInput dataInput) 
    throws ApolloAdapterException {
    InputStream stream = null;

    DataInputType type = dataInput.getType();
    String input = dataInput.getInputString();

    // if the style isnt game need to create style - it has the urls
    //String getStyleFile = Config.findFile("game.style");
    //Style getStyle = new Style(getStyleFile);

    if (type == DataInputType.FILE) { // could be file or http/url
      stream = getStreamFromFile(input);
      setOriginalFilename(input);
    }
    else if (type == DataInputType.URL) {
      URL url = makeUrlFromString(input);
      if (url == null) {
        String message = "Couldn't find URL for " + getInput();
        logger.error(message);
        throw new ApolloAdapterException(message);
      }
      logger.info("Trying to open URL " + input + " to read GAME XML...");
      stream = apollo.util.IOUtil.getStreamFromUrl(url, "URL "+input+" not found");
      setOriginalFilename(url.toString());
    }
    // 7/2005: when you send a request to Indiana for data, if the URL is bad
    // then an exception will be thrown in getStreamFromUrl when it tries to open a
    // connection to the URL, and then it will use the error string provided.
    // If the URL is ok but the cgi doesn't respond, you will see the "Searching"
    // message in the GUI for a while, and then eventually you will get the "Bad URL"
    // error message.
    // If the gene or region you request is not found, the input stream will get
    // opened successfully but then will turn out to have nothing in it
    // (bis.available() == 1).
    else if (type == DataInputType.GENE) {
      String err = "Can't connect to URL for request gene="+input+"--server not responding.";
      String notfound = "Gene " + input + " not found (or server not responding)";
      logger.info("Looking up GAME XML data for gene " + input + "...");
      stream = apollo.util.IOUtil.getStreamFromUrl(getURLForGene(input), err, notfound);
      setOriginalFilename((getURLForGene(input)).toString());
    }
    else if (type == DataInputType.CYTOLOGY) {
      String err = "Can't connect to URL for band="+input+"--server not responding.";
      String notfound = "Cytological band " + input + " not found (or server not responding)";
      logger.info("Looking up GAME XML data for band " + input + "...");
      stream = apollo.util.IOUtil.getStreamFromUrl(getURLForBand(input),err, notfound);
      setOriginalFilename((getURLForBand(input)).toString());
    }
    else if (type == DataInputType.SCAFFOLD) {
      String err = "Can't connect to URL for scaffold="+input+"--server not responding.";
      String notfound = "Scaffold " + input + " not found (or server not responding)";
      logger.info("Looking up GAME XML data for scaffold " + input + "...");
      stream = apollo.util.IOUtil.getStreamFromUrl(getURLForScaffold(input),err, notfound);
      setOriginalFilename((getURLForScaffold(input)).toString());
    }
    else if (type == DataInputType.BASEPAIR_RANGE) {
      String err = "Can't connect to URL for requested region--server not responding.";
      String notfound = "Region " + dataInput.getRegion() + " not found (or server not responding)";
      logger.info("Looking up GAME XML data for range " +  dataInput.getRegion() + "...");
      stream = apollo.util.IOUtil.getStreamFromUrl(getURLForRange(dataInput.getRegion()),err, notfound);
      setOriginalFilename((getURLForRange(dataInput.getRegion())).toString());
    }
//     // No longer available
//     else if (type == DataInputType.SEQUENCE) {
//       String err = "No blast hits for sequence "+input; // ??
//       stream = apollo.util.IOUtil.getStreamFromUrl(getURLForSequence(input),err);
//     }

    return stream;
  }

  /** Only files should be sent this way - if an URL is sent here it wont work
      and something is wrong */
  private InputStream getStreamFromFile(String filename) throws ApolloAdapterException {
    InputStream stream=null;
    // returns null for urls
    String path = apollo.util.IOUtil.findFile(filename, false);

    try {
      logger.info("Trying to open GAME XML file " + filename);
      stream = new FileInputStream (path);
    } catch (Exception e) { // FileNotFoundException
      stream = null;
      //new Throwable().printStackTrace();
      throw new ApolloAdapterException("Error: could not open GAME XML file " + 
                                       filename + " for reading.");//+e+path);
    }

    // Check whether this is, in fact, GAME XML by looking for the <game> line
    BufferedReader in;
    try {
      in = new BufferedReader(new FileReader(path));
    } catch (Exception e) { // FileNotFoundException
      stream = null;
      throw new ApolloAdapterException("Error: could not open GAME XML file " + 
                                       path + " for reading.");
    }
    for (int i=0; i < 12; i++) {  // 12 lines should be enough to find the <game> line, right?
      String line;
      try {
        line = in.readLine();
      } catch (Exception e) {
        in = null;
        throw new ApolloAdapterException("Error: file " + filename + 
                                         " is empty.");
      }
      if (line == null) {
        in = null;
        throw new ApolloAdapterException("Error: file " + filename + 
                                         " does not appear to contain GAME XML.");
      }
      if (line.toLowerCase().indexOf("<game") >= 0) {  // looks good
        in = null;
        return stream;
      }
    }
    // Didn't find <game> line
    in = null;
    throw new ApolloAdapterException("Error: file " + filename + 
                                     " does not appear to contain GAME XML.");
  }

   /** Retrieve style from config
    *  (Need to do it this way because style hasn't been set yet when we're about
    *  to open a GAME file.) */
   private Style getGAMEStyle() {
     return Config.getStyle(getClass().getName()); 
   }

  /** make URL from urlString, replace %DATABASE% with selected database */
  public URL makeUrlFromString(String urlString) {
    URL url;
    urlString = fillInDatabase(urlString);
    urlString = fillInPadding(urlString);
    try {
      url = new URL(urlString);
    } catch ( MalformedURLException ex ) {
      logger.error("caught exception creating URL " + urlString, ex);
      return(null);
    }
    return(url);
  }

  /** Replace %DATABASE% field with selected database.
   *  Note: some of this is FlyBase-specific! */
  public String fillInDatabase(String urlString) {
    String dbField = getGAMEStyle().getDatabaseURLField();
    if (dbField == null) return urlString;
    int index = urlString.indexOf(dbField);
    if (index == -1) {
      return urlString;
    }
    StringBuffer sb = new StringBuffer(urlString);
    // getDatabase in AbstractApolloAdapter
    String dbname = getDatabase();
    // The Sequence tab for r3.2 returns the string "(Not available for r3.2)"
    // Just punt and return an empty URL.
    // Will not be handled particularly gracefully, but hey, we warned them.
    if (dbname.indexOf("ot available") > 0)
      return "";
    // Spaces in dbname (e.g. "r3.2 (by scaffold only)") seem to screw things up.
    // We don't really need the stuff after the space anyway, so get rid of it.
    if (dbname.indexOf(" ") > 0)
      dbname = dbname.substring(0, dbname.indexOf(" "));
    sb.replace(index,index+dbField.length(),dbname);
    return sb.toString();
  }

  public String getDatabase() {
    if (super.getDatabase() != null) {
      return super.getDatabase();
    }
    else {
      return getGAMEStyle().getDefaultDatabase();
    }
  }

  /** Replace %PadLeft/Right% with pad ints */
  public String fillInPadding(String urlString) {
    StringBuffer sb = new StringBuffer(urlString);
    pad(sb,getGAMEStyle().getPadLeftURLField(),getPadLeft());
    pad(sb,getGAMEStyle().getPadRightURLField(),getPadRight());
    return sb.toString();
  }
  private static void pad(StringBuffer urlBuff,String field,int pad) {
    if (field==null) return;
    int index = urlBuff.indexOf(field);
    if (index == -1) return;
    urlBuff.replace(index,index+field.length(),pad+"");
  }

  private URL getURLForScaffold(String scaffold) {
    String query = getGAMEStyle().getScaffoldUrl()+scaffold;
    URL url = makeUrlFromString(query);
    String msg = "Searching for location of scaffold " + scaffold + "...";
    fireProgressEvent(new ProgressEvent(this, new Double(2.0),msg));
    return(url);
  }

  private URL getURLForGene(String gene) {
    // This CGI actually does a redirect to the relevant XML, so the URL we
    // return here will yield the XML when opened.
    String query = getGAMEStyle().getGeneUrl()+gene;

    URL url = makeUrlFromString(query);
    String msg = "Searching for location of gene " + gene + "...";
    fireProgressEvent(new ProgressEvent(this, new Double(2.0),msg));
    return(url);
  }

  private URL getURLForBand(String band) {
    String query = getGAMEStyle().getBandUrl() + band; // check config
    URL url = makeUrlFromString(query);
    fireProgressEvent(new ProgressEvent(this, new Double(2.0),
                                        "Searching for cytological location "+
                                        band + "--please be patient"));
    return(url);
  }

//   // ! Proof-of-concept method gets the URL for the XML for the scaffold 
//   // that has the best blast hit to the given sequence (uses the fruitfly
//   // blast server)
//   // 2005 No longer used
//   private URL getURLForSequence(String seq) {
//     String query = getGAMEStyle().getSeqUrl() + seq;
//     URL url = makeUrlFromString(query);
//     fireProgressEvent(new ProgressEvent(this, new Double(5.0),
//                                         "Running BLAST query--please be patient..."));
//     return(url);
//   }

  /** parse range string "Chr 2L 10000 20000" -> "2L:10000:20000"   */
  private URL getURLForRange(Region region) {
    // in GameSynteny case already in colon format! - check for this
    String rangeForUrl = region.getColonDashString();
    String query = getGAMEStyle().getRangeUrl() +rangeForUrl;
    URL url = makeUrlFromString(query);
    fireProgressEvent(new ProgressEvent(this,new Double(5),
                                        "Searching for region "+rangeForUrl));
    return url;
  }


  /*
    Utility routines.
  */

  /** Creates a gene from annot_element. This can make real genes and
      also put non genes into Gene objects */
  private AnnotatedFeatureI getAnnot(XMLElement annot_element,
                                     CurationSet curation,
                                     StrandedFeatureSetI annots) {
    
    AnnotatedFeatureI annot = new AnnotatedFeature();
    annot.setId(annot_element.getID());

    if ((annot_element.getAttribute ("problem") != null) &&
        (annot_element.getAttribute ("problem").equals ("true"))) {
      annot.setIsProblematic (true);
    }

    String nickname = null;
    String desc = null;

    Vector elements = annot_element.getChildren();
    for (int i = 0; i < elements.size(); i++) {
      XMLElement element = (XMLElement) elements.elementAt(i);
      if (element.getType().equals ("name")) {
        if (element.getCharData() != null)
          annot.setName (element.getCharData());
      } else if (element.getType().equals ("type")) {
        annot.setTopLevelType (element.getCharData());
        annot.setFeatureType (element.getCharData());
      } else if (element.getType().equals ("author")) {
        // Normally "author" appears at the transcript level, but for
        // one-level annots it might appear here.
        annot.setOwner(element.getCharData());
      } else if (element.getType().equals ("synonym") ||
                 element.getType().equals ("nickname")) {
        //annot.addSynonym (element.getCharData());
        Synonym syn = getSynonym(element);
        annot.addSynonym(syn);
        nickname = element.getCharData(); // in case we dont get a name
      } else if (element.getType().equals ("comment")) {
        setComment (element, annot);
      } else if (element.getType().equalsIgnoreCase("gene")) {
        setGeneInfo (element, annot);
      } else if (element.getType().equals ("description")) {
        annot.setDescription (element.getCharData());
        desc = element.getCharData();
      } else if (element.getType().equals ("dbxref")) {
        setAnnotXref (element, annot);
      // aspect always has 1 child, dbxref
      } else if (element.getType().equals ("aspect")) {
        XMLElement dbx = (XMLElement) element.getChildren().elementAt(0);
        setAnnotXref (dbx, annot);
      } else if (element.getType().equals ("feature_set")) {
        Transcript transcript = getTranscript(element, annot, curation);
        annot.addFeature(transcript);
        // reset id to use meaningful one
        String id = transcript.getId();
        if (id == null || id.length() == 0 || id.startsWith("feature_set:")) {
          ApolloNameAdapterI nameAdapter = getCurationState().getNameAdapter(annot);
          // this is wrong - curation doesnt have annots yet - null
          //id = nameAdapter.generateId(curation.getAnnots(), 
          id = nameAdapter.generateId(annots, // annots that are being built up
                                      curation.getName(), 
                                      transcript);
        }
        transcript.setId(id);
      } else if (element.getType().equals ("property")) {
        // In GAME, internal synonyms are saved as properties, not as synonyms.
        // Add internal_synonyms to the vector of synonyms (will be saved as
        // a property if/when user writes out GAME.)
        String type_tag = getTypeTag (element);
        if (type_tag.equals ("internal_synonym")) {
          String value = getTypeValue(element);
          Synonym syn = new Synonym(value);
          syn.addProperty("is_internal", "1");
          annot.addSynonym(syn);
        }
        else
          addOutput (annot, element);
      }

      // DO_ONE_LEVEL_ANNOTS config
      else if (GAMESave.isOneLevelAnnot(annot) 
               && element.getType().equals ("seq_relationship")) {
        setRange(element,annot,curation.getRefSequence(),curation.getStart()-1);
      }
    }

    // make sure the name is set to something, no matter what
    if (annot.getName() == null) {
      if (nickname != null)
        annot.setName(nickname);
      else if (desc != null)
        annot.setName (desc);
      else
        annot.setName ("");
    }
    repairGreek (annot);

    // this is bad for 2 reasons. first of all the -R is flybase specific. second of
    // all another datasource may not want this "correction" at all. I believe
    // flybase doesnt need this anymore - it was to correct past data that didnt 
    // have transcript names but thats no longer. if we do still need it it should
    // both be configged and the name adapter should be used.
//     int trans_count = annot.size();
//     ApolloNameAdapterI dubber = getCurationState().getNameAdapter(gene);
//     for (int i = 0; i < trans_count; i++) {
//       // Transcript name now is based on on the parent's name, but use
//       // the transcript name in the XML to get the suffix.
//       // Do this at the end, rather than when we see the name element, so
//       // we can also set the cDNA and peptide names (the cDNA and peptide
//       // hadn't been read in yet when we saw the transcript name).
//       /* but no not call this until the feature has been 
//          added to the gene, because it relies upon the index
//          of the transcript to determine a new name (if needed)
//       */
//       Transcript transcript = (Transcript) gene.getFeatureAt(i);
//       String transcript_name = transcript.getName();
//       int index = (transcript_name != null ?
//                    transcript_name.lastIndexOf("-R") : -1);
//       if (index > 0)
//         transcript_name = transcript_name.substring(0, index);
//       else
//         transcript_name = "";
//       if (!transcript_name.equals(gene.getName())) {
//         System.out.println ("Renaming transcript " +
//                             transcript.getName() + " to use parent gene name " +
//                             gene.getName());
//         dubber.setTranscriptNameFromAnnot(transcript, gene);
//       }
//     }
    return annot;
  }

  private Synonym getSynonym(XMLElement synElement) {
    String name = synElement.getCharData();
    Synonym syn = new Synonym(name);
    if (synElement.hasAttribute("owner"))
      syn.setOwner(synElement.getAttribute("owner"));
    // if (synElement.hasAttribute("isInternal")) 
    //    boolean isInternal = synElement.getAttribute("isInternal").equals("true");
      // syn.setIsInternal(isInternal);
    return syn;
  }

  private void setComment (XMLElement comment_element,
         AnnotatedFeatureI sf) {
    String text = null;
    String person = null;
    Date date = null;

    Vector elements = comment_element.getChildren();
    for (int i = 0; i < elements.size(); i++) {
      XMLElement element = (XMLElement) elements.elementAt(i);
      if (element.getType().equals ("text")) {
        text = element.getCharData();
      }
      else if (element.getType().equals ("person")) {
        person = element.getCharData();
      }
      else if (element.getType().equals ("date")) {
        date = DateUtil.makeADate (element.getCharData());
      }
    }
    if (text != null) {
      Comment comment = new Comment();
      // text must be set or comment will not be added
      comment.setText(text);
      sf.addComment(comment);

      if (comment_element.getAttribute ("id") != null) {
        comment.setId (comment_element.getAttribute ("id"));
      }
      if ((comment_element.getAttribute ("internal") != null &&
           comment_element.getAttribute ("internal").equals ("true")) ||
          // Left off leading i of internal in case it was Internal
          (text.indexOf("nternal view only") >= 0)) {
        comment.setIsInternal (true);
      }
      String clue = "no atg translation start identified";
      if (text.toLowerCase().indexOf(clue) >= 0) {
        ((FeatureSetI) sf).setMissing5prime (true);
      }
      /*
      clue = "Unconventional translation start identified";
      if (text.toLowerCase().indexOf(clue) >= 0) {
      ((FeatureSetI) sf).setUnconventionalStart (true);
      }
      */
      if (person != null)
        comment.setPerson (person);
      if (date != null)
        comment.setTimeStamp (date.getTime());
    }
  }

  private Transcript getTranscript (XMLElement transcript_element,
                                    AnnotatedFeatureI gene, 
                                    CurationSet curation) {
    Transcript transcript = new Transcript();
    transcript.setId (transcript_element.getID());
    /* These have to be caught and held onto until the 
       exons have been added and then it is safe to add them */
    int minus1_frameshift = 0;
    int plus1_frameshift = 0;
    String readthrough_stop = null;

    // Might also call setIsProblematic in addOutput as well.
    if ((transcript_element.getAttribute ("problem") != null) &&
  (transcript_element.getAttribute ("problem").equals ("true"))) {
      transcript.setIsProblematic (true);
    }

    String name = null;
    String desc = null;

    /*
      name?, type?, seq_relationship*, author?, date?,
      evidence*, description?, feature_span*
    */

    SeqFeatureI translateStart = null; // potential feat for start, may be prot
    SeqFeatureI prot = null; // potential prot feat if prot in game

    Vector elements = transcript_element.getChildren();
    for (int i = 0; i < elements.size(); i++) {
      XMLElement element = (XMLElement) elements.elementAt(i);
      if (element.getType().equals ("name")) {
        if (element.getCharData() != null) {
          name = element.getCharData();
          transcript.setName(name);
        }
      } else if (element.getType().equals ("type")) {
        // biotype should be the parent's biotype
        // transcript.setBioType(element.getCharData());
        // shouldnt this also set the type?
        // transcript.setType(element.getCharData()); // ???
        // type should just be transcript
        // transcript.setType("transcript"); this is unnecessary
      } else if (element.getType().equals ("synonym")) {
        //transcript.addSynonym (element.getCharData());
        transcript.addSynonym(getSynonym(element));
      } else if (element.getType().equals ("comment")) {
        setComment (element, transcript);
      } else if (element.getType().equals ("author")) {
        transcript.setOwner(element.getCharData());
      } else if (element.getType().equals ("date")) {
        Date date = DateUtil.makeADate (element.getCharData());
        if (date != null)
          transcript.addProperty ("date", date.toString());
        else
          transcript.addProperty ("date", element.getCharData());
      } else if (element.getType().equals ("description")) {
        desc = element.getCharData();
        transcript.setDescription (desc);
      } else if (element.getType().equals ("seq_relationship")) {
        setRange (element, transcript, curation.getRefSequence(),
                  curation.getStart() - 1);
        gene.setStrand (transcript.getStrand());
      } else if (element.getType().equals ("evidence")) {
        addEvidence (transcript, element);
      } else if (element.getType().equals ("seq")) {
        addTranscriptSequence (curation, transcript, element);
      }
      else if (element.getType().equals ("output") ||
               element.getType().equals ("property")) {
        String type_tag = getTypeTag (element);
        if (type_tag.equals ("plus_1_translational_frame_shift") ||
            type_tag.equals ("plus1_translational_frameshift")) {
          String value = getTypeValue(element);
          plus1_frameshift = Integer.parseInt(value);
        }
        else if (type_tag.equals ("minus_1_translational_frame_shift") ||
                 type_tag.equals ("minus1_translational_frameshift")) {
          String value = getTypeValue(element);
          minus1_frameshift = Integer.parseInt(value);
        }
        // This is a tag that can appear in ChadoXML files.  It has not, historically,
        // appeared in GAME XML, but no reason why it can't.
        else if (type_tag.equalsIgnoreCase("stop_codon_redefinition_as_selenocysteine")) {
          String value = getTypeValue(element);
          if (value.toLowerCase().startsWith("t") ||
              value.equalsIgnoreCase("U"))
            readthrough_stop = "U";
        }
        else if (type_tag.equals ("readthrough_stop_codon")) {
          // could be true/false or a specific residue
          readthrough_stop = getTypeValue(element);
        }
        // In GAME, internal synonyms are saved as properties, not as synonyms
        else if (type_tag.equals ("internal_synonym")) {
          String value = getTypeValue(element);
          Synonym syn = new Synonym(value);
          syn.addProperty("is_internal", "1");
          transcript.addSynonym(syn);
        }
        else
          addOutput (transcript, element);
      } 
      // feature_span can be exon, prot(1.1), or translation_start(1.0)
      else if (element.getType().equals("feature_span")) {
        SeqFeatureI featSpan = addTranscriptFeatSpan(element,gene,transcript,curation);
        if (isTranslationStart(featSpan)) // protein(1.1) or translation_start(1.0)
          translateStart = featSpan;
        if (featSpan.isProtein())
          prot = featSpan;
//         ExonI exon = getExon (element, gene, transcript, curation);
//         // These exons are clearly ignored so why even parse them? [Mark]
//         // That is not true--we need to save the start codon to 
//         // handle dicistronics.  --NH 
//         if ( ! exon.getFeatureType().equals ("translate offset") &&
//              ! exon.getFeatureType().equals ("start codon")) {
//              //! exon.getFeatureType().equals ("start_codon")) { // why twice?
//           transcript.addExon(exon);
//           // as wierd as this is the protein name is on the start codon
//           // 12/2003: Exons no longer have names.
//         }
//         else {  // We found a start codon
//           translateStart = exon;
//         }
      }
      else {
        if (element.getCharData() != null &&
            !(element.getCharData().equals("")))
          logger.warn(transcript_element.getType() +
                      ": Either intentionally ignoring or " +
                      "inadvertently forgetting to parse " +
                      element.getType() + "=" + element.getCharData());
      }
    }

    // TRANSLATION
    // translateStart is either a Protein(1.1) or a trans start SeqFeature
    // i think this is wrong - even if theres no translateStart shouldnt apollo
    // try to figure translation if its a protein coding gene?
    // suzi says this was taken out as on large data sets it was slow
    if (translateStart != null && gene.isProteinCodingGene()) {
      // Use the flag to set the translation stop as well
      // if no missing_start_cod getProp returns ""
      // missing start codon does not mean start will be first codon
      // calcTransSttFLPep will calc longest pep start - may or may not be missing
      // start - may be real start - so miss_s_c prop is really just hint and does not
      // ensure start is actually missing - however missing_start_codon is only written
      // be GAMESave if trans.isMissing5Prime - so in practice start will be missing
      // but it doesnt hurt to recalc to be sure i guess
      // i guess missing_start_codon also overrides explicitly set start
      if (transcript.getProperty("missing_start_codon").equals("true")) {
        transcript.calcTranslationStartForLongestPeptide();
      }
      else {
        // returns false if not in range,true param is for setting translation end
        boolean foundTranslationStart 
          = transcript.setTranslationStart(translateStart.getStart(), true);
        // for whatever reason translation start is not in range, so recalc
        if (!foundTranslationStart) {
          transcript.calcTranslationStartForLongestPeptide();
        }
        else if (transcript.isTransSpliced()) {
          transcript.sortTransSpliced();
        }
      }

      // peptides are no longer done on fly. need to set up in adapter.
      setProteinNameAndId(transcript);
    }

    transcript.setPlus1FrameShiftPosition(plus1_frameshift);
    transcript.setMinus1FrameShiftPosition(minus1_frameshift);
    if (readthrough_stop != null)
      transcript.setReadThroughStop(readthrough_stop);

    if (name == null) {
      if (desc != null)
        transcript.setName (desc);  // ?
      else
        transcript.setName ("");
    }
    return transcript;
  }

  /** Return true if span is either a protein(1.1)
      or a translation start seq feat(1.0) */
  private boolean isTranslationStart(SeqFeatureI span) {
    if (span.isProtein())
      return true;
    // is translate offset pase?
    return span.getFeatureType().matches("start_codon|translate offset");
  }


  /** generate name & id if not set above from game prot 1.1 */
  private void setProteinNameAndId(Transcript transcript) {
    Protein prot = transcript.getProteinFeat(); // if not already there, creates one
    // if have 1 but not the other name/id then just use the other
    if (prot.hasName() && !prot.hasId())
      prot.setId(prot.getName());
    else if (prot.hasId() && !prot.hasName())
      prot.setName(prot.getId());
    
    // if we dont have name & id (from prot game 1.1) generate them
    if (!prot.hasName() && !prot.hasId()) {
      ApolloNameAdapterI na = getNameAdapter(transcript);
      String name = na.generatePeptideNameFromTranscriptName(transcript.getName());
      String id = na.generatePeptideIdFromTranscriptId(transcript.getId());
      //transcript.generatePeptideSequence(id,name);
      prot.setName(name);
      prot.setId(id);
    }
  }
  

  private String setRange (XMLElement range_element,
                           SeqFeatureI feat,
                           SequenceI refSeq,
                           int offset) {
    String align_str = null;

    if (refSeq != null)
      feat.setRefSequence (refSeq);

    Vector elements = range_element.getChildren();
    XMLElement span_element = null;
    for (int i = 0; i < elements.size(); i++) {
      XMLElement element = (XMLElement) elements.elementAt(i);
      if (element.getType().equals ("span")) {
        span_element = element;
      } else if (element.getType().equals ("alignment")) {
        align_str = element.getCharData();
      }
    }

    if (span_element == null) {
      logger.error("XML is messed up, span element is missing");
      return align_str;
    }

    setSpan (span_element, feat, offset);

    return align_str;
  }

  /** GAME spans are relative to the GAME map. Apollo SeqFeature have absolute
      coordinates NOT cur set relative. The offset(cur set start) gets added to
      each feature to compensate for this. */
  private void setSpan (XMLElement span_element,
                        RangeI feat,
                        int offset) {
    int start = 0;
    int end = 0;
    Vector elements = span_element.getChildren();
    for (int i = 0; i < elements.size(); i++) {
      XMLElement element = (XMLElement) elements.elementAt(i);
      if (element.getType().equals ("start")) {
        start = Integer.parseInt (element.getCharData());
      } else if (element.getType().equals ("end")) {
        end = Integer.parseInt (element.getCharData());
      }
    }
    int strand = (start < end) ? 1 : -1;
    feat.setStrand (strand);
    feat.setStart (start + offset);
    feat.setEnd (end + offset);
  }

  private SequenceI getSpanSequence (XMLElement range_element,
                                     CurationSet curation) {
    SequenceI seq = null;

    String seq_id = range_element.getAttribute ("seq");
    if (seq_id == null)
      seq_id = range_element.getAttribute ("id");

    if (seq_id != null) {
      seq = createSequence (seq_id, curation, true);
    }
    return seq;
  }

  // This doesn't work, but it doesn't really matter, since we don't use evidence
  // anymore anyway.  (Apollo never really kept track of it properly.)  --NH, 7/29/2003

    /* this works fine, but it is true that evidence is no longer relevant --SUZ 8/30/2003*/
  private void addEvidence (AnnotatedFeatureI ga, XMLElement ev_element) {
    String result_id = ev_element.getAttribute ("id");

    if (result_id == null || result_id.equals("")) {
      result_id = ev_element.getAttribute ("result");
    }
    if (result_id == null || result_id.equals("")) {
      result_id = ev_element.getAttribute ("result_id");
    }
    if (result_id != null && ! result_id.equals("")) {
      ga.addEvidence (result_id);
    }
  }

  /** Transcripts have feature_spans elements which can be exons, proteins, or
      translation_start - "add" isnt quit the right concept - gleen? */
  private SeqFeatureI addTranscriptFeatSpan(XMLElement spanElement,
                                            AnnotatedFeatureI gene,
                                            Transcript transcript,
                                            CurationSet curation) {

    SeqFeatureI span = new SeqFeature();
    ExonI exon=null; // will get filled in if type is exon

    // I dont think span elements have type attributes anymore, but just in case
    if (spanElement.getAttribute ("type") != null)
     span.setFeatureType(spanElement.getAttribute ("type"));

    // proteins have ids (exons shouldnt) (attribute)
    span.setId(spanElement.getID());

    boolean typeIsNotInGame = true;
    /* <!ELEMENT feature_span (type?, seq_relationship*, evidence*)>  */
    while (spanElement.numChildren() > 0) {
      //for (int i = 0; i < elements.size(); i++) {
      XMLElement element = spanElement.popChild();//(XMLElement)elements.elementAt(i);
      if (element.getType().equals ("type")) {
        // exon.setType("exon"); ??
        // Sanitize exon type in case it was a weird type put there by some
        // old buggy Apollo version.
        // 6/2005: I think it's safe now to get rid of this "sanitizing"
        // step, and in fact we might not want to do that in case, in the
        // future, the GAME data included exon-level features of different types.
        //        exon.setType(sanitizeExonType(element.getCharData()));
        String type = element.getCharData();
        if (type.equals("exon") || type.equals("gene")) {
          exon = new Exon(span);
          span = exon;
          if (type.equals("gene")) {
            logger.error("Error in game file - exon with type 'gene'");
          }
        }
        else if (type.equals("polypeptide"))
          span = new Protein(span,transcript);
        // "start_codon"??? - stays a SeqFeatureI - RangeI?
        span.setFeatureType(type);
        typeIsNotInGame = false;
      }
      else if (element.getType().equals ("seq_relationship")) {
        // I believe the minus one here is because the curation set is 
        // base-oriented and so are the features. (This wouldnt be necasary
        // if they were both interbase, base-oriented is annoying like that)
        setRange (element, span, curation.getRefSequence(),
                  curation.getStart() - 1);
        transcript.setStrand (span.getStrand());
        gene.setStrand (span.getStrand());
      } 
      // I think evidence is pase
      else if (element.getType().equals ("evidence") && span.hasAnnotatedFeature()) {
        addEvidence (span.getAnnotatedFeature(), element);
      }
      else if (element.getType().equals ("name")) {
        // ignore the name this is for identifying the
        // same exon used in multiple transcripts
        // Why ignore it?  Why not take it if it's supplied?  --NH
        // in fact its now needed for prot span
        span.setName (element.getCharData());
      }
      else {
        String data = element.getCharData();
        if (data != null && !data.equals("")) {
          String m=spanElement.getType()+": Either intentionally ignoring or " +
            "inadvertently forgetting to parse span type "+element.getType()+"="+data;
          logger.warn(m);
        }
      }
    }

    // done with populating span from xml, now post process...

    // so heres a funny thing. in game 1.0 for 1 level annots that are represented
    // with 3 levels the fake "exon" doesnt actually have a type - perhaps because its
    // embarrassed that it even exists - but nonetheless even though its not an exon
    // for apollos sake it needs to be represented as an exon. if we are configured for
    // game 1.1 then this exons will be converted into a 1 level annot (eventually...)
    if (typeIsNotInGame) {
      exon = new Exon(span); // sets feat type to "exon"
      span = exon;
      logger.debug("GAME transcript feature_span has no type. setting to 'exon'");
    }

    // EXON - can do exon stuff here
    if (span.isExon()) {
      addExon(exon,transcript,gene,curation); // exon is the ExonI that is the span
    }
    
    // for trans start & protein need whole transcript before fiddling (translate)
   
    return span;
  }

  private void addExon(ExonI exon,Transcript transcript,AnnotatedFeatureI gene,
                       CurationSet curation) {
    transcript.addExon(exon); // exon is the span (ExonI)
    // Generate an id for exon - this should come from name adapter...
    String id = gene.getId() + ":" + exon.getStart() + "-" + exon.getEnd();
    exon.setId(id);
    
    // If exon doesn't already have a name, generate one -- why???
    if (exon.getName() == null || exon.getName().equals("")) {
      ApolloNameAdapterI nameAdapter = getCurationState().getNameAdapter(gene);
      // The annots and name aren't actually used, but need to be there
      nameAdapter.generateName(curation.getAnnots(), curation.getName(), exon);
    }
  }

  // replaced by addTranscriptFeatSpan
//   /** This is called by getTranscript when it experiences "feature_span" elements.
//       Whats kind of funny here is that protein & start_codon elements are also
//       feature_spans - so in other words getExon might be making non exons */
//   private ExonI getExon (XMLElement exon_element,
//                          AnnotatedFeatureI gene,
//                          Transcript transcript,
//                          CurationSet curation)	{
//     ExonI exon = new Exon();
//     // The generic id cannot be used for the chado transaction.
//     // Have to regenerate id based on its range -- Guanming
//     //exon.setId (exon_element.getID());

//     // this may arrive either as an attribute or an element
//     // so check for both
//     // Exons biotype needs to be the genes type
//     //exon.setBioType (exon_element.getAttribute ("type"));
//     // This may be translate offset,start codon, or start_codon, which
//     // i dont think get used so should probably not even bother parsing at
//     // that point??  [Mark]
//     // That is not true--we need to save those types to
//     // handle dicistronic transcripts.  --NH
//     if (exon_element.getAttribute ("type") != null)
//       // Sanitize exon type in case it was a weird type put there by some
//       // old buggy Apollo version.
//       // 6/2005: I think it's safe now to get rid of this "sanitizing"
//       // step, and in fact we might not want to do that in case, in the
//       // future, the GAME data included exon-level features of different types.
//       //      exon.setType(sanitizeExonType(exon_element.getAttribute ("type")));
//       exon.setFeatureType(exon_element.getAttribute ("type"));

//     /*
//       <!ELEMENT feature_span (type?, seq_relationship*, evidence*)>
//     */

//     Vector elements = exon_element.getChildren();
//     for (int i = 0; i < elements.size(); i++) {
//       XMLElement element = (XMLElement) elements.elementAt(i);
//       if (element.getType().equals ("type")) {
//         // exons dont have distinct biotype from gene
//         //exon.setBioType(element.getCharData());
//         // exon.setType("exon"); ??
//         // Sanitize exon type in case it was a weird type put there by some
//         // old buggy Apollo version.
//         // 6/2005: I think it's safe now to get rid of this "sanitizing"
//         // step, and in fact we might not want to do that in case, in the
//         // future, the GAME data included exon-level features of different types.
//         //        exon.setType(sanitizeExonType(element.getCharData()));
//         exon.setFeatureType(element.getCharData());
//       } else if (element.getType().equals ("seq_relationship")) {
//         // I believe the minus one here is because the curation set is 
//         // base-oriented and so are the features. (This wouldnt be necasary
//         // if they were both interbase, base-oriented is annoying like that)
//         setRange (element, exon, curation.getRefSequence(),
//                   curation.getStart() - 1);
//         transcript.setStrand (exon.getStrand());
//         gene.setStrand (exon.getStrand());
//       } else if (element.getType().equals ("evidence")) {
//         addEvidence (exon, element);
//       } else if (element.getType().equals ("name")) {
//         // ignore the name this is for identifying the
//         // same exon used in multiple transcripts
//         // Why ignore it?  Why not take it if it's supplied?  --NH
//         exon.setName (element.getCharData());
//       } else {
//         if (element.getCharData() != null &&
//             !(element.getCharData().equals("")))
//           System.out.println (exon_element.getType() +
//                               ": Either intentionally ignoring or " +
//                               "inadvertently forgetting to parse exon type " +
//                               element.getType() +
//                               (element.getCharData() == null ?
//                                "" : "="+element.getCharData()));
//       }
//     }
//     // Generate an id for exon
//     String id = gene.getId() + ":" + exon.getStart() + "-" + exon.getEnd();
//     exon.setId(id);

//     // If exon doesn't already have a name, generate one -- why???
//     if (exon.getName() == null || exon.getName().equals("")) {
//       ApolloNameAdapterI nameAdapter = getCurationState().getNameAdapter(gene);
//       // The annots and name aren't actually used, but need to be there
//       nameAdapter.generateName(curation.getAnnots(), curation.getName(), exon);
//     }
//     return exon;
//   }

  // not used anymore
//   /** Sanitize exon type in case it was a weird type put there by some
//    *  old buggy Apollo version.
//    *  For example, Harvard gave me a file (AE003511.Jun.crosby.p.xml) that
//    *  has a transcript that contains exons of type "transcript" and one of
//    *  type "sim4:na_gb.dros".  Change these to type "exon".
//    *  Please note that if someone in the future tries to include new exon
//    *  types in the XML ("polyA site" or whatever), this method will wipe out
//    *  those new type and convert them to "exon".  Maybe for safety we should
//    *  have a list of valid exon types in the style file?
//    *  6/2005: This method is no longer called. */
//   private String sanitizeExonType(String type) {
//     if (type.equals("exon") ||
//         type.equals("start_codon") || 
//         type.equals("start codon") || 
//         type.equals("translate offset") ||
//         type.equals("translate_offset"))
//       return type;
//     else {
//       // Tried passing transcript and printing its name, but name didn't seem
//       // to be filled in yet, nor are exon start and end, so all we can do is
//       // print the type itself, I guess.
//       System.out.println("Converting weird exon type '" + type + "' to 'exon'");
//       return "exon";
//     }
//   }

  /** Loop through children of root XML element game_element. For those of type
      "computational_analysis", create FeatureSets for each strand and add them
      to a newly created StrandedFeatureSet which is saved in the curation set.
      Children of type "annotation" are saved as annotations. */
  private void getResultsAndAnnots(CurationSet curation)
    throws ApolloAdapterException {

    // Set up empty annotations and results (or retrieve them, if we're layering)
    StrandedFeatureSetI annotations = curation.getAnnots();
    if (annotations == null) {
      annotations = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
      annotations.setName ("Annotations");
      annotations.setFeatureType ("Annotation");
      curation.setAnnots(annotations);
    }

    // analyses is global
    analyses = curation.getResults();
    if (analyses == null) {
      analyses = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
      analyses.setName ("Analyses");
      curation.setResults(analyses);
      all_analyses = new Hashtable();
    }

    int count = 0;
    int total = game_element.numChildren();
    while (game_element.numChildren() > 0) {
      try {
        ++count;
        // Some real progress!
        fireProgressEvent(new ProgressEvent(this, new Double(30.0 + ((double)count/(double)total)*55.0),
                                            "Parsing XML element #" + count));

        // Pop off the next child of the root element and handle it
        // 3/2005: Theoretically, this should reduce the maximum memory use,
        // as children are removed from the XML parse tree as Apollo datamodels
        // are filled in.  In practice, it doesn't seem to help much, but it
        // shouldn't hurt.
        // To really improve memory use, we shouldn't build a parse tree of
        // the entire xml input--we should handle each element as we read it.
        // But that's a bigger reorganization than I'm prepared to do right now.
        XMLElement element = game_element.popChild();
        if (element.getType().equals ("computational_analysis")) {
          addAnalysis(element,
                      analyses,
                      curation,
                      all_analyses);
        }
        else if (element.getType().equalsIgnoreCase("annotation")) {
          // This needs to be changed for annots that are not genes should be
          // (Huh? --NH)
          AnnotatedFeatureI annot = getAnnot(element,curation,annotations); 
          annotations.addFeature(annot);
        }
        else if (element.getType().equals ("seq")) {
          getSequence(element, curation);
        }
        else if (element.getType().equals ("map_position")) {
          // already handled
        }
        // gameTransactions -> apolloTransactions, game backwards compatibility
        else if (element.getType().equals("gameTransactions")
                 || element.getType().equals("apolloTransactions") ) {
          getTransactions(element, curation);
        }
        else {
          logger.warn("Warning--don't know how to handle xml element of type " + element.getType());
        }
      } catch (ApolloAdapterException dae) { 
        throw dae;
      } catch ( Exception ex2 ) {
        logger.error("Caught exception while parsing XML", ex2);
        throw new ApolloAdapterException(ex2.getMessage());
      }
    }

  }

  /** Create forward and reverse strand feature sets for analysis of analysis element,
      which are added to analyses. Add all sub features to the appropriate stranded
      FeatureSet. */
  private void addAnalysis (XMLElement analysis_element,
                            StrandedFeatureSetI analyses,
                            CurationSet curation,
                            Hashtable all_analyses) {
    String analysis_id = analysis_element.getID();
    String prog = "";
    String db = "";
    String type = null;
    String date = null;
    String version = null;

    Vector elements = analysis_element.getChildren();
    for (int i = 0; i < elements.size(); i++) {
      XMLElement element = (XMLElement) elements.elementAt(i);
      if (element.getType().equals ("program")) {
        prog = element.getCharData();
      }
      else if (element.getType().equals ("database")) {
        // 6/15/04: Although appending ":dummy" to the program name doesn't
        // give us any useful info, it avoids some confusion--if we don't add
        // :dummy to promoter results, for example, they get confused with
        // promoter annotations.
        //             &&  !element.getCharData().equals("dummy")) {
        db = element.getCharData();
      }
      else if (element.getType().equals ("type")) {
        type = element.getCharData();  // will be saved as biotype--type is constructed from program and db
      }
      else if (element.getType().equals ("date")) {
        date = element.getCharData();
      }
      else if (element.getType().equals ("version")) {
        version = element.getCharData();
      }
    }

    // Create FeatureSets for analyses
    FeatureSetI forward_analysis = initAnalysis (analyses,
                                   1,
                                   prog,
                                   db,
                                   analysis_id,
                                   type,
                                   date,
                                   version,
                                   all_analyses);

    FeatureSetI reverse_analysis = initAnalysis (analyses,
                                   -1,
                                   prog,
                                   db,
                                   analysis_id,
                                   type,
                                   date,
                                   version,
                                   all_analyses);

    String analysis_type = getAnalysisType(prog, db);

    for (int i = 0; i < elements.size(); i++) {
      XMLElement element = (XMLElement) elements.elementAt(i);
      if (element.getType().equals ("result_set")) {
        FeatureSetI result = getResult(element,
                                       analysis_type,
                                       curation);
        result.setProgramName (prog);
        result.setDatabase (db);
        if (result.getStrand() == 1) {
          forward_analysis.addFeature(result);
          if (!forward_analysis.hasFeatureType())
            forward_analysis.setFeatureType(result.getFeatureType());
        } else {
          reverse_analysis.addFeature(result);
          if (!reverse_analysis.hasFeatureType()) {
            logger.info("Setting analysis type to " + result.getFeatureType());
            reverse_analysis.setFeatureType(result.getFeatureType());
          }
        }
      }
      else if (element.getType().equals ("property")) {
        addOutput (forward_analysis, element);
        addOutput (reverse_analysis, element);
      }
      else if (element.getType().equals("result_span")) {
        /* the following only occurs in GAMEII style where
           analyses that do not generate subfeatures, like tRNAscan,
           store the spans directly under the analysis */
        SeqFeatureI span = getSpan(element, analysis_type, curation);
        span.setProgramName (prog);
        span.setDatabase (db);
        if (span.getStrand() == 1) {
          forward_analysis.addFeature(span);
          if (!forward_analysis.hasFeatureType())
            forward_analysis.setFeatureType(span.getFeatureType());
        } else {
          reverse_analysis.addFeature(span);
          if (!reverse_analysis.hasFeatureType())
            reverse_analysis.setFeatureType(span.getFeatureType());
        }
      }
    }
    
    // Add in stranded analyses only if features were found on that strand
    // otherwise its scrapped (not added to all_analyses and analyses)
    boolean fwd = addAnalysisIfHasFeatures(forward_analysis,all_analyses,analyses);
    boolean rev = addAnalysisIfHasFeatures(reverse_analysis,all_analyses,analyses);
    // If we have analysis for both strands, set pointers to analysis "twin" on 
    // opposite strand, this is needed for results that need to be moved to the
    // opposite strand
    if (fwd && rev) {
      forward_analysis.setAnalogousOppositeStrandFeature(reverse_analysis);
      reverse_analysis.setAnalogousOppositeStrandFeature(forward_analysis);
    }
  }

  private String getAnalysisType(String prog, String db) {
    String analysis_type;
    if (!(prog == null) && !prog.equals("") && !(db == null) && !db.equals(""))
      analysis_type = prog + ":" + db;
    else if (!(prog == null) && !prog.equals(""))
      analysis_type = prog;
    else if (!(db == null) && !db.equals(""))
      analysis_type = db;
    else
      analysis_type = RangeI.NO_TYPE;
    return analysis_type;
  }

  /** Add in stranded analyses only if features were found on that strand
      otherwise its scrapped (not added to all_analyses and analyses) 
      and false is returned */
  private boolean addAnalysisIfHasFeatures(FeatureSetI analysis,
                                           Hashtable all_analyses,
                                           StrandedFeatureSetI analyses) {
    if (all_analyses == null) {
      //      logger.warn("addAnalysisIfHasFeatures: analysis hash was null!");
      return false;
    }
    if (all_analyses.containsKey(analysis.getName())) {
      return true;
    }
    if (analysis.size() > 0) {
      all_analyses.put(analysis.getName(),analysis);
      analyses.addFeature(analysis);
      return true;
    }
    return false; // no feats for analysis
  }

  /** Creates a new FeatureSet for the analysis on strand. If an analysis with prog,db, 
  and strand has already been created, the existing one is returned. If new a
  FeatureSet is created, added to all_analyses hash, added to analyses, and returned. */
  private FeatureSetI initAnalysis (StrandedFeatureSetI analyses,
                                    int strand,
                                    String prog,
                                    String db,
                                    String analysis_id,
                                    String type,
                                    String date,
                                    String version,
                                    Hashtable all_analyses) {
    String analysis_type = getAnalysisType(prog, db);
    String analysis_name;
    if (!analysis_type.equals(RangeI.NO_TYPE))
      analysis_name = (analysis_type +
                       (strand == 1 ? "-plus" :
                        (strand == -1 ? "-minus" : "")));
    else
      analysis_name = (RangeI.NO_NAME +
                       (strand == 1 ? "-plus" :
                        (strand == -1 ? "-minus" : "")));

    // check all_analyses cache to see if we've already created this analysis
    if (all_analyses == null)
      all_analyses = new Hashtable();
    FeatureSetI analysis = (FeatureSetI) all_analyses.get(analysis_name);
    // analysis has not been already created - go ahead and make a new one
    if (analysis == null) {
      analysis = new FeatureSet();
      analysis.setId (analysis_id);
      analysis.setProgramName(prog);
      analysis.setDatabase(db);
      analysis.setFeatureType (analysis_type);
      if (!analysis_type.equals(RangeI.NO_TYPE))
        analysis.setName (analysis_name);
      analysis.setStrand (strand);
      // analysis.setHolder (true);
      if (type != null)
        analysis.setTopLevelType (type);
      if (date != null)
        analysis.addProperty ("date", date);
      if (version != null)
        analysis.addProperty ("version", version);
      // Change - analysis added later on, but only if it has features
      //all_analyses.put (analysis_name, analysis);
      //analyses.addFeature(analysis);
    }
    return analysis;
  }


  private FeatureSetI getResult(XMLElement result_element,
                                String analysis_type,
                                CurationSet curation) {

    FeatureSetI result = new FeatureSet();

    result.setId (result_element.getID());
    result.setFeatureType (analysis_type);
    result.setName ("");

    SeqFeatureI translate_start = null;

    Vector elements = result_element.getChildren();
    for (int i = 0; i < elements.size(); i++) {
      XMLElement element = (XMLElement) elements.elementAt(i);

      if (element.getType().equals ("score")) {
        setScore (element, result);
      } else if (element.getType().equals ("name")) {
        if (element.getCharData() == null)
          logger.warn("Hey, " + result.getFeatureType() +
                      " id " + result.getId() +
                      " has empty name element");
        else
          result.setName (element.getCharData());
      } else if (element.getType().equals ("type")) {
        result.setTopLevelType (element.getCharData());
      } else if (element.getType().equals ("output")) {
        addOutput (result, element);
      } else if (element.getType().equals ("seq_relationship")) {
        String rel_type = element.getAttribute ("type");
        if (rel_type.equals ("sbjct") ||
            rel_type.equals ("subject")) {
          SequenceI seq = getSpanSequence (element, curation);
          result.setHitSequence(seq);
          if (seq.getName() == null ||
              seq.getName().equals ("")) {
            try {
              throw (new Exception ("Missing id for seq " + seq.getAccessionNo()));
            } catch (Exception e) {
              logger.error("Exception getting accessionNo of SpanSequence", e);
              return null;
            }
          }
          if ( result.getName().equals (""))
            result.setName (seq.getName());
        }
        else {
          setRange (element, result, curation.getRefSequence(),
                    curation.getStart() - 1);
        }
      } else if (element.getType().equals ("result_set")) {
        FeatureSetI span = getResult(element, analysis_type, curation);
        addToResult (result, span);
      } else if (element.getType().equals ("result_span")) {
        SeqFeatureI span = getSpan(element, analysis_type, curation);
        if ( span.getTopLevelType().equals ("") ||
             (! span.getTopLevelType().equals ("translate offset") &&
              ! span.getTopLevelType().equals ("start codon"))) {
          addToResult (result, span);
        } else {
          translate_start = span;
        }
      } else {
        if (element.getCharData() != null &&
            !(element.getCharData().equals("")))
          logger.warn(result_element.getType() +
                      ": Either intentionally ignoring or " +
                      "inadvertently forgetting to parse " +
                      element.getType() +
                      (element.getCharData() == null ?
                       "" : "="+element.getCharData()));
      }
    }

    //Handle predicted starts (ie predicted UTRs)
    if (translate_start != null) {
      //Set predicted start and calculate the longest orf from that start
      //Great, let's sort the sub features.
      result.sort(result.getStrand());
      result.setProteinCodingGene(true); 
      if (!result.setTranslationStart(translate_start.getStart(), true))
        logger.error("unable to set translation start of " + result.toString());
      
    }

    /* This is very FlyBase/BDGP specific, but since it will do
       nothing in other cases it is a relatively benign hack. 
       Better here, on loading, than in the more generic glyph
       drawing. Any way, this snippet checks the description of
       the aligned sequence to see if there is any information
       there to indicate the actual point of insertion. Doing
       this so that it may be cleanly propagated to any new 
       annotations of P element insertions derived from this
       alignment */
    sniffOutInsertionSite(result);

    return result;
  }

  private void addToResult (FeatureSetI result, SeqFeatureI span) {
    if (span.getStrand() != result.getStrand() &&
        result.getStrand() == 0) {
      result.setStrand(span.getStrand());
    }

    if (!result.hasFeatureType()) {
      result.setFeatureType (span.getTopLevelType());
    }

    if (span.getStrand() == result.getStrand()) {
      if (result.getName().equals ("") || 
          result.getName().equals(RangeI.NO_NAME))
        result.setName (span.getName());
      else if (span.getName().equals("")) {
        span.setName ((result.getName().equals("") ?
                       result.getId() : result.getName()) + " span " +
                      result.size());
      }
      if (result.getHitSequence() == null && (span instanceof FeaturePairI))
        result.setHitSequence(((FeaturePairI) span).getHitSequence());
      result.addFeature(span);
    }
    else {
      logger.warn("Ignored result_span " +
                  span.getName() + " of type " + 
                  span.getTopLevelType() + ": " +
                  span.getStart() + "-" +
                  span.getEnd() + " because span_strand " +
                  span.getStrand()  + " doesn't match result strand " +
                  result.getStrand());
    }
  }

  /** Creates and returns a SeqFeature or a FeaturePair for the span XMLElement -
      if we have a seq_relationship then its a FeaturePair */
  private SeqFeatureI getSpan (XMLElement span_element,
                               String analysis_type,
                               CurationSet curation) {
    SeqFeatureI query = new SeqFeature();
    SeqFeatureI hit = new SeqFeature();

    SeqFeatureI span = null;

    query.setName ("");
    hit.setName ("");

    query.setFeatureType (analysis_type);
    hit.setFeatureType (analysis_type);

    // this may arrive either as an attribute or an element
    // so check for both
    // I think the biotype should be the same as the type for results MG
    // type is analysis_type
    //query.setBioType (span_element.getAttribute ("type"));
    //hit.setBioType (span_element.getAttribute ("type"));    
    query.setTopLevelType(analysis_type);
    hit.setTopLevelType(analysis_type);
    
    query.setId (span_element.getID());
    hit.setId (span_element.getID());

    String query_str = null;
    String sbjct_str = null;

    Vector elements = span_element.getChildren();
    for (int i = 0; i < elements.size(); i++) {
      XMLElement element = (XMLElement) elements.elementAt(i);
      if (element.getType().equals ("type")) {
        // Use parent analysis type for child result spans
        if (!element.getCharData().equals(analysis_type))
          //          logger.warn("Ignoring unknown result span type " + element.getCharData() +
          //                      "; using parent analysis type " + analysis_type);
          query.setTopLevelType(analysis_type);
        if (element.getCharData().equals("start codon"))//Predicted Start
          query.setTopLevelType("start codon");
        hit.setTopLevelType(analysis_type);
      } else if (element.getType().equals ("name")) {
        query.setName(element.getCharData());
        hit.setName(element.getCharData());
      } else if (element.getType().equals ("score")) {
        setScore (element, query);
        setScore (element, hit);
      } else if (element.getType().equals ("output")) {
        /* FeaturePair hardwiring has been taken out, 
           so a result leaf can be a non FeaturePair, i.e. a SeqFeature.
           The problem is it now doesnt set span up as a FeaturePair 
           until seq_relationship which may come after the problematic cigar. */
        /* Only set up as FeaturePair if we havent already done so in 
           seq_relationship
           seq_relationship and cigars are the 2 items that flag the feature
           as a FeaturePair */
        String type_tag = getTypeTag (element);
        if (type_tag.equals ("cigar")) {
          span = new FeaturePair(query, hit);
          setTagValue (span, element, type_tag);
        } 
        // Another thing represented by outputs are ResultTags:
        //      <output>
        //        <type>tag</type>
        //        <value>comment: incomplete CDS</value>
        //      </output>
        else 
          setTagValue (query, element, type_tag);
      }
      // seq_relationship -> FeaturePair
      else if (element.getType().equals ("seq_relationship")) {
        /* we know its a feature pair if we have seq_rel--- WRONG!!!!
           It is only a feature pair if there is a seq_relationship
           to the subject. A seq_relationship to the query is the
           normal state of affairs for any feature. And for gene
           prediction algorithms this is all that there is. Creating
           mis-leading FeaturePairs screws code up in other places.
           Suz June 2004 */
        String rel_type = element.getAttribute ("type");
        // SUBJECT of seq_relationship
        if (rel_type.equals ("sbjct") ||
            rel_type.equals ("subject")) {
          /* Now we know there is a hit feature */
          if (span == null) { 
            span = new FeaturePair(query, hit);
          }
          sbjct_str = setRange (element, hit,
                                getSpanSequence (element, curation),
                                0);
          hit.setName (hit.getRefSequence().getName());
          query.addProperty ("description",
                            hit.getRefSequence().getDescription());
          query.setName (hit.getName());
          if (sbjct_str != null && !sbjct_str.equals("")) {
            //hit.addProperty("alignment",sbjct_str);
            //span.setExplicitHitAlignment(sbjct_str);
            hit.setExplicitAlignment(sbjct_str);
          }
        }
        // QUERY of seq_relationship
        else {
          query_str = setRange (element, query,
                                curation.getRefSequence(),
                                curation.getStart() - 1);
          if (query_str != null && !query_str.equals("")) {
            //query.addProperty("alignment",query_str);
            if (span == null)
              /* assume that there will be a subject alignment
                 since the alignment string is present */
              span = new FeaturePair(query, hit);
            span.setExplicitAlignment(query_str);
          }
        }
      } else {
        if (element.getCharData() != null &&
            !(element.getCharData().equals("")))
          logger.warn(span_element.getType() +
                      ": Either intentionally ignoring or " +
                      "inadvertently forgetting to parse span type " +
                      element.getType() +
                      (element.getCharData() == null ?
                       "" : "="+element.getCharData()));
      }
    }    
    if (span == null)
      span = query;
    return span;
  }

  /** Find the focus (genomic) sequence and use it to set the current region. */
  private void getFocusSequence(CurationSet curation) 
    throws ApolloAdapterException {
    Vector elements = game_element.getChildren();
    boolean found = false;
    for (int i = 0; i < elements.size(); i++) {
      XMLElement seq_element = (XMLElement) elements.elementAt(i);
      if (seq_element.getType().equals ("seq")) {
        // I think the "focus" attribute says that this sequence is the 
        // region, the main genomic sequence. If no sequences in the xml
        // have this tag we will be left without a region and will get 
        // a null pointer.
        SequenceI seq;
        String focus = seq_element.getAttribute("focus");
        if (focus != null && focus.equals ("true")) {
          if (found)
            logger.warn("Found duplicate focus sequence " + seq_element.getID());
          seq = new GAMESequence (seq_element.getID(), 
                                  Config.getController(),
                                  "");
          seq.setAccessionNo (seq_element.getID());
          seq.setResidueType (SequenceI.DNA);
          curation.addSequence(seq);
          curation.setName (seq.getName());
          this.setRegion (seq);
          found = true;
          parseSequence(seq, seq_element);
        }
        // If it's not the focus sequence, ignore it--it will be handled later.
      }
    }
  }

  /** We've already handled the focus (genomic) sequence, so don't deal with it here. */
  private void getSequence(XMLElement seq_element, CurationSet curation)
    throws ApolloAdapterException {
    SequenceI seq;
    String focus = seq_element.getAttribute("focus");
    if (focus != null && focus.equals ("true")) {
      return;
    }
    seq = createSequence (seq_element.getID(), curation, false);
    parseSequence(seq, seq_element);
  }

  /** Finish filling in the fields in seq */
  private void parseSequence(SequenceI seq, XMLElement seq_element) {
    String lengthString = seq_element.getAttribute("length");
    int length = 0;
    if (lengthString != null) {
      length = Integer.parseInt (lengthString);
      seq.setLength (Integer.parseInt (lengthString));
    }
    String checksum = seq_element.getAttribute("md5checksum");
    if (checksum != null && !checksum.equals ("")) {
      seq.setChecksum (checksum);
    }
    Vector seq_elements = seq_element.getChildren();
    for (int j = 0; j < seq_elements.size(); j++) {
      XMLElement element = (XMLElement) seq_elements.elementAt(j);
      if (element.getType().equals ("name")) {
        FastaHeader fasta = new FastaHeader(element.getCharData());
        seq.setName (fasta.getSeqId());
      } else if (element.getType().equals ("description")) {
        setSeqDescription(seq, element.getCharData(), seq_element.getID());
      } else if (element.getType().equals ("dbxref")) {
        addSeqXref (element, seq);
      } else if (element.getType().equals ("residues")) {
        seq.setResidues(element.getCharData());
      } else if (element.getType().equals ("organism")) {
        seq.setOrganism(element.getCharData());
      } else if (element.getType().equals ("potential_sequencing_error")) {
        addSequenceEdit(element, seq);
      } else {
        logger.warn("Not dealing with seq element " +
                    element.getType());
      }
    }
  }

  private void addSeqXref (XMLElement dbxref_element, SequenceI seq)	{
    Vector elements = dbxref_element.getChildren();
    String db = null;
    String acc = null;
    for (int i = 0; i < elements.size(); i++) {
      XMLElement element = (XMLElement) elements.elementAt(i);
      if (element.getType().equals ("xref_db")) {
        db = element.getCharData();
      }
      if (element.getType().equals ("db_xref_id")) {
        acc = element.getCharData();
      }
      seq.addDbXref (db, acc);
    }
  }

  /** Given a FASTA-style header line, set the sequence's description and
   * extract the date.
   * For fly sequences, the expected format of the header line is
   * >gi||gb|AB003910|AB003910 Fruitfly DNA for 88F actin, complete cds. (08-JUN-1999)
   * Date is that thing at the end, obviously.
   * Another format we see sometimes is
   * RE70410.5prime BI486922 [similar by BLASTN (3.2e-110) to l(3)82Fd "FBan0010199 GO:[]  located on: 3R 82F8-82F10;" 05/17/2001]
   * Using the expected dateFormat (defined in DateUtil)
   * parse the date string and return it as a Date object.
   */
  public static void setSeqDescription(SequenceI seq, 
                                       String description, 
                                       String seq_id) {
    if (description != null && description.startsWith(seq_id))
      description = (seq_id.length() < description.length() ?
                     description.substring(seq_id.length()) : "");
    String current_desc = seq.getDescription();
    if (current_desc != null)
      description = current_desc + " " + description;
    seq.setDescription (description);
    Date date = null;
    if (description != null && description.length() > 5) {
      int index = (description.trim()).lastIndexOf(' ');
      if (index > 0) {
        String possibleDate = (description.substring(index)).trim();
        if (possibleDate.indexOf("]") > 0)
          possibleDate = possibleDate.substring(possibleDate.indexOf("]")+1);
        if (possibleDate.indexOf("(") >= 0)
          possibleDate = possibleDate.substring(possibleDate.indexOf("(")+1);
        if (possibleDate.indexOf(")") >= 0)
          possibleDate = possibleDate.substring(0, possibleDate.indexOf(")"));
        date = DateUtil.makeADate(possibleDate);
//        if (date != null && !date.equals("")) // DEL
//          logger.warn("Date = " + possibleDate + " for header " + description); // DEL
      }
    }
    seq.setDate(date);
  }
    
  private void addSequenceEdit (XMLElement edit_element, SequenceI seq)	{
    Vector elements = edit_element.getChildren();
    String edit_type = null;
    int position = 0;
    String base = null;
    for (int i = 0; i < elements.size(); i++) {
      XMLElement element = (XMLElement) elements.elementAt(i);
      if (element.getType().equals ("type")) {
        edit_type = element.getCharData();
      }
      if (element.getType().equals ("position")) {
        position = Integer.parseInt(element.getCharData());
      }
      if (element.getType().equals ("base")) {
        base = element.getCharData();
      }
    }
    if (edit_type != null && position > 0)
      seq.addSequencingErrorPosition (edit_type, position, base);
  }

  private void getGenomePosition (CurationSet curation)
    throws ApolloAdapterException {
    int curation_strand = 1;
    String seq_id = null;
    int start = 0;
    int end = 0;

    Vector elements = game_element.getChildren();
    for (int i = 0; i < elements.size(); i++) {
      XMLElement map_element = (XMLElement) elements.elementAt(i);
      if (map_element.getType().equals ("map_position")) {
        if (map_element.getAttribute("type") != null) {
          curation.setFeatureType (map_element.getAttribute ("type"));
        }
        if (map_element.getAttribute("seq") != null) {
          seq_id = map_element.getAttribute ("seq");
          // this is logically inconsistent.
          // in fact the region specified is
          // the sequence that is produced,
          // not the sequence that this region
          // is coming from. In other words,
          // this it is NOT the reference sequence,
          // but a special case of a produced sequence
          // (a substring) called the 'region_seq'
          if (! seq_id.equals (curation.getName()) )
            logger.warn("getGenomePosition: map position is not for " +
                        seq_id +
                        ", but is for " +
                        curation.getName() + "??");
        }
        Vector map_elements = map_element.getChildren();
        for (int j = 0; j < map_elements.size(); j++) {
          XMLElement element = (XMLElement) map_elements.elementAt(j);
          if (element.getType().equals ("arm")) {
            String arm = element.getCharData ();
            // Really?  We ignore whatever the seq_id is and use the arm?
            seq_id = arm;
            // arm sometimes comes with version number suffix like 3R.3 -
            // need to get rid of version suffix for chrom string
            // if it exists (new version not there)
            // it seems funny that this is inconsistent
            String chromosome = arm;
            int dot_index = chromosome.indexOf(".");
            if (dot_index != -1) // if dot exists strip off suffix
              chromosome = chromosome.substring(0,dot_index);
            curation.setChromosome(chromosome);
          } else if (element.getType().equals ("chromosome")) {
            curation.setChromosome(element.getCharData ());
          } else if (element.getType().equals ("organism")) {
            // this will actually set it for the curated sequence
            String organism = element.getCharData();
            curation.setOrganism(organism);
            // this allows for different styles for different organisms
            // doesnt solve issue of different urls for different orgs
            if (Config.hasStyleForSpecies(organism)) {
              Config.setStyleForSpecies(organism);
              setStyle(Config.getStyle());
            }
          } else if (element.getType().equals ("span")) {
            Vector map_pos_elements = element.getChildren();
            start = 0;
            end = 0;
            XMLElement map_pos_element = null;
            for (int k = 0; k < map_pos_elements.size(); k++) {
              map_pos_element = (XMLElement) map_pos_elements.elementAt(k);
              if (map_pos_element.getType().equals ("start")) {
                start = Integer.parseInt (map_pos_element.getCharData());
              } else if (map_pos_element.getType().equals ("end")) {
                end = Integer.parseInt (map_pos_element.getCharData());
              }
            }
            if (end == start) {
              logger.warn("top level end == start = " + end + ".  Using seq length  " +
                          curated_seq.getLength()
                          + " to set end position.");
              // Set the end by using the start and the sequence length
              end = start + curated_seq.getLength() + 1;
              map_pos_element.setCharData((new Integer(end)).toString());
            }
            // End of code dealing with GAME bug
            // handle reversed regions where start > end.
            // This means gadfly already revcomp-ed and flipped all features.
            else if (start > end) {
              // For now, complain and give up.
              String message = "Problem in XML: map_position start is bigger than end.\nI don't yet know how to handle reversed regions--sorry.";
              logger.error(message);
              throw new ApolloAdapterException(message);
            }
            setSpan (element, curation, 0);
          }
        }
      }
    }

    // 2/3/2004: Only assign seq_id now if it was null.
    // This was handled together with shifting the range to be positive:
    // if (seq_id == null || start <= 0 || end <= 0) {
    // [reassign seq_id, start, and end]
    // and I don't see why those should be lumped together.
    // Am I missing something?  --NH
    if (seq_id == null)
      seq_id = curated_seq.getName();

    if (start <= 0 || end <= 0) {
      start = 1;
      end = curated_seq.getLength();
      /* otherwise this happened in setSpan above */
      curation.setStrand (curation_strand);
      curation.setStart(start);
      curation.setEnd (end);
    }

    /*In Apollo version 1.3.0, the display ID of the ref sequence was set to seq_id
      (which ends up being the chromosome arm with version, e.g. 2L.3) when the
      reference sequence was created here.  Now, the reference sequence was 
      created earlier, but its ID was the same as the curation set ID 
      (e.g. AE003603.Sept), which was messing up the title.  So we now need 
      to set the Display ID of this reference sequence, or else the title of
      the main Apollo window is wrong. I don't think this will break anything 
      else. Do we want to remove the version (.3) from the seq_id? */
    curated_seq.setName(seq_id);
  } // end of getGenomePosition

  /** Parses out dbxref and name from "gene" xml element. This name is used
      as display name in apollo. Changed input from Gene to AnnotatedFeatureI
      for non gene types. rename setAnnotInfo or something more specific?
  */
  private void setGeneInfo (XMLElement gene_element, AnnotatedFeatureI gene) {
    Vector gene_elements = gene_element.getChildren();
    for (int i = 0; i < gene_elements.size(); i++) {
      XMLElement element = (XMLElement) gene_elements.elementAt(i);
      if (element.getType().equals ("dbxref")) {
        setAnnotXref (element, gene);
      } else if (element.getType().equals ("name")) {
        if (element.getCharData() != null &&
            !element.getCharData().equals (""))
          gene.setName (element.getCharData());
      }
    }
  }

  private void setAnnotXref (XMLElement element, AnnotatedFeatureI gene) {
    String db = "";
    String id = "";
    Vector xref_elements = element.getChildren();
    for (int j = 0; j < xref_elements.size(); j++) {
      XMLElement xref_element =
        (XMLElement) xref_elements.elementAt(j);
      if (xref_element.getType().equals ("xref_db")) {
        db = xref_element.getCharData();
      }
      if (xref_element.getType().equals ("db_xref_id")) {
        id = xref_element.getCharData();
      }
    }
    if (!db.equals("") && !id.equals("")) {
      gene.addDbXref (new DbXref ("id", id, db));
    }
  }

  private void setScore (XMLElement score_element, SeqFeatureI feat) {
    String scoreString = score_element.getCharData();
    double score = Double.valueOf(scoreString).doubleValue();
    feat.setScore(score);
  }

  private void addTranscriptSequence (CurationSet curation,
                                      Transcript transcript,
                                      XMLElement seq_element) {
    String seq_id = seq_element.getID();
    SequenceI seq = createSequence (seq_id, curation, false);
    String res_type = seq_element.getAttribute ("type");

    String lengthString = seq_element.getAttribute("length");
    int length = 0;
    if (lengthString != null) {
      length = Integer.parseInt (lengthString);
      seq.setLength (Integer.parseInt (lengthString));
    }
    String checksum = seq_element.getAttribute("md5checksum");
    if (checksum != null && !checksum.equals ("")) {
      seq.setChecksum (checksum);
    }
    Vector elements = seq_element.getChildren();
    for (int i = 0; i < elements.size(); i++) {
      XMLElement element = (XMLElement) elements.elementAt(i);
      if (element.getType().equals ("name")) {
        FastaHeader fasta = new FastaHeader(element.getCharData());
        seq.setName (fasta.getSeqId());
      }
      else if (element.getType().equals ("description")) {
        seq.setDescription (element.getCharData());
      }
      else if (element.getType().equals ("dbxref")) {
        addSeqXref (element, (Sequence)seq);
      }
      else if (element.getType().equals ("residues")) {
        seq.setResidues(element.getCharData());
      }
      else if (element.getType().equals ("organism")) {
        seq.setOrganism(element.getCharData());
      }
      else {
        logger.warn("Not dealing with seq element " + element.getType());
      }
    }
    if (res_type != null && res_type.equalsIgnoreCase (SequenceI.AA)) {
      seq.setResidueType (SequenceI.AA);
      transcript.setPeptideSequence (seq);
      // If we're configged to refresh the peptides, we'll set peptide validity
      //  to false in case it was wrong in the XML (older versions of Apollo didn't
      // always update the peptides when it should have).
      // (Use ! because we want validity set to FALSE if refreshPeptides is true.)
      transcript.setPeptideValidity(!(Config.getRefreshPeptides()));  
    }
    else {
      transcript.set_cDNASequence (seq);
    }
  }

  private SequenceI createSequence (String header, 
                                    CurationSet curation,
                                    boolean alert) {
    FastaHeader fasta = new FastaHeader(header);
    String seq_id = fasta.getSeqId();
    SequenceI seq = (SequenceI) curation.getSequence (seq_id);
    if (seq == null) {
      seq = fasta.generateSequence();
      curation.addSequence(seq);
      // 3/2005: Now that the r4 data doesn't include hit seqs, we see this
      // message way too often, so it gets assigned to the most detailed log level:
      if (alert) {
        logger.trace("Had to create sequence " + seq.getName());
      }
    }
    return seq;
  }

  private boolean addOutput (SeqFeatureI sf, XMLElement output_element) {
    String type = getTypeTag (output_element);
    return (setTagValue (sf, output_element, type));
  }

  private String getTypeTag (XMLElement output_element) {
    Vector elements = output_element.getChildren();
    String type = null;
    for (int i = 0; i < elements.size() && type == null; i++) {
      XMLElement element = (XMLElement) elements.elementAt(i);
      if (element.getType().equals ("type")) {
        type = element.getCharData();
      }
    }
    return type;
  }

  private String getTypeValue (XMLElement output_element) {
    Vector elements = output_element.getChildren();
    String value = null;
    for (int i = 0; i < elements.size() && value == null; i++) {
      XMLElement element = (XMLElement) elements.elementAt(i);
      if (element.getType().equals ("value")) {
        value = element.getCharData();
      }
    }
    return value;
  }

  /** Deals with a handful of tag-value pairs: total_score/score, amino-acid, 
   *  non-canonical_splice_site, cigar, symbol, problem and ResultTags (output type=tag)
   *  <output>
   *     <type>tag</type>
   *     <value>comment: incomplete CDS</value>
   *  </output>
   */
  private boolean setTagValue (SeqFeatureI sf,
                               XMLElement output_element,
                               String type) {
    String value = getTypeValue(output_element);
    boolean valid = true;
    if (type != null && value != null) {
      try {
        /* this will also parse integers of course */
        double score = Double.valueOf(value).doubleValue();
        // genscan has >1 kind of score
        if (type.equals ("total_score")) {
          type = "score";
        }
        else
          sf.addScore (type, score);
      } catch ( Exception ex ) {
        // Ok, it wasn't a number..
        if (type.equals ("amino-acid")) {
          sf.setName(value);
        }
        else if (type.equals ("non-canonical_splice_site") &&
                 (sf instanceof Transcript)) {
          ((Transcript) sf).nonConsensusSplicingOkay(value.equals("approved"));
        }
        else if (type.equals ("cigar")) {
          if (sf instanceof FeaturePairI)
            ((FeaturePairI) sf).setCigar(value);
          else 
            valid = false;
        }
        else if (type.equals ("symbol")) {
          String current_name = sf.getName();
          if (current_name == null)
            sf.setName(value);
          else if (current_name.equals("") || 
                   current_name.equals(RangeI.NO_NAME))
            sf.setName(value);
          // otherwise just ignore this value
        } else {
          sf.addProperty (type, value);
          if (type.equals ("problem") && value.equals("true")) {
            if (sf instanceof AnnotatedFeatureI)
              ((AnnotatedFeatureI)sf).setIsProblematic(true);
          }
        }
      }
    }
    return valid;
  }

  /** This is very FlyBase/BDGP specific, but since it will do
      nothing in other cases it is a relatively benign hack. 
      Better here, on loading, than in the more generic glyph
      drawing. Anyway, this snippet checks the description of
      the aligned sequence to see if there is any information
      there to indicate the actual point of insertion. Doing
      this so that it may be cleanly propagated to any new 
      annotations of P element insertions derived from this
      alignment */
  private static String sesame = "inserted at base ";

  private void sniffOutInsertionSite(FeatureSetI result) {
    SequenceI seq = result.getHitSequence();
    if (seq == null && result.size() > 0) {
      // try the child to be sure
      SeqFeatureI sf = result.getFeatureAt(0);
      if (sf instanceof FeaturePairI) {
        seq = ((FeaturePairI) sf).getHitSequence();
      }
    }
    if (seq != null && seq.getDescription() != null) {
      // parse out the insertion site position
      String desc = seq.getDescription();
      int index = desc.indexOf (sesame);
      if (index >= 0) {
        desc = desc.substring (index + sesame.length());
        index = desc.indexOf (" ");
        if (index > 0)
          desc = desc.substring (0, index);
        try {
          int site = Integer.parseInt (desc);
          if (site < result.length())
            result.addProperty("insertion_site", desc);
        } catch (Exception e) { }
      }
    }
  }

  private void repairGreek (SeqFeatureI sf) {
    // Fix gene name
    sf.setName(HTMLUtil.replaceSGMLWithGreekLetter(sf.getName()));
    // Fix any synonyms that have weird greek symbols, too
    AnnotatedFeature gene = (AnnotatedFeature)sf;
    Vector syns = gene.getSynonyms();
    for (int i = 0; i < syns.size(); i++) {
      Synonym syn = (Synonym) syns.elementAt (i);
      String fixed = HTMLUtil.replaceSGMLWithGreekLetter(syn.getName());
      if (!(syn.getName().equals(fixed))) {
        logger.debug("Replacing synonym " + syn + " for gene " + gene.getName() + " with " + fixed);
        // Do we need to delete and add?  Can we just change the synonym name?
        // Or do we need the delete/add for transactional purposes?
        gene.deleteSynonym(syn.getName());
        syn.setName(fixed);
        gene.addSynonym(syn);
      }
    }
  }

  private void getTransactions(XMLElement transactionsElement, CurationSet curation) {
    // should i check the config for this - or since they are in the game just grab em?
    TransactionXMLAdapter adap = new TransactionXMLAdapter();
    //List transactions = 
    // sets curations transactions
    adap.getTransFromTopElement(transactionsElement,curation,getCurationState());
    //if (transactions != null) curation.setTransactions(transactions);
  }

}
