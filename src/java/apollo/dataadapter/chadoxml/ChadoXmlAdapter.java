package apollo.dataadapter.chadoxml;

import java.io.File;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Stack;
import java.util.Vector;
import javax.swing.JOptionPane;
import org.apache.log4j.*;
import org.bdgp.io.DataAdapterUI;
import org.bdgp.io.IOOperation;
import org.bdgp.util.ProgressEvent;
import org.bdgp.xml.XMLElement;
import apollo.config.Config;
import apollo.main.Version;
import apollo.config.PropertyScheme;
import apollo.config.FeatureProperty;
import apollo.config.ApolloNameAdapterI;
import apollo.config.GmodNameAdapter;
import apollo.dataadapter.AbstractApolloAdapter;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.DataInput;
import apollo.dataadapter.DataInputType;
import apollo.dataadapter.StateInformation;
import apollo.dataadapter.DataInputType.UnknownTypeException;
import apollo.dataadapter.chadoxml.ChadoXmlUtils;
import apollo.dataadapter.gamexml.GAMEAdapter;
import apollo.dataadapter.gamexml.XMLParser;
import apollo.dataadapter.gamexml.TransactionXMLAdapter;
import apollo.datamodel.*;
import apollo.datamodel.seq.GAMESequence;  // for now
import apollo.editor.TransactionManager;
import apollo.util.FastaHeader;
import apollo.util.HTMLUtil;
import apollo.util.IOUtil;
import apollo.main.LoadUtil;

/** 
 * A FlyBase-specific reader for Chado XML files. 
 * Currently handles only unmacroized Chado XML. */

public class ChadoXmlAdapter extends AbstractApolloAdapter {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ChadoXmlAdapter.class);

  // Preserve original filename so that at time of saving, we can fetch
  // the preamble (the stuff before <chado>) from the original input file
  static String originalFilename = null;

  /** For labeling flat fields (which are stored as properties) so they
   *  can be written out properly.
   *  (Should this go somewhere global so that other adapters can see it?) */
  public static String FIELD_LABEL = "field:";

  /** Used by org.bdgp.io.DataAdapter */
  private final static IOOperation[] supportedOperations = 
  {ApolloDataAdapterI.OP_READ_DATA,
   ApolloDataAdapterI.OP_WRITE_DATA,
   ApolloDataAdapterI.OP_APPEND_DATA
  };

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private ChadoXmlAdapterGUI gui;  // set by getUI
  String filename = null;

  /** These need to be global to allow easy layering of new data */
  StrandedFeatureSetI analyses = null;
  Hashtable all_analyses = null;

  /** Every result has a block for the (genomic) query, including a dbxref--only
   *  save this the first time. */
  boolean savedGenomicDbxref = false;

  boolean warnedAboutFeatureCvterm = false;

  /** This indicates no GUI so certain non-apollo, non-gui apps can use this
   *  adapter. (But who sets this?) */
  boolean NO_GUI = false;

  boolean genomicRegionSet = false;

  /** Must have empty constructor to work with org.bdgp.io.DataAdapterRegistry
      instance creation from config string */
  public ChadoXmlAdapter() {
    setName("Chado XML file (FlyBase v1.0, no macros)");
  }

  /** From org.bdgp.io.VisualDataAdapter interface */
  public DataAdapterUI getUI(IOOperation op) {
    if (gui == null)
      gui = new ChadoXmlAdapterGUI();
    gui.setIOOperation(op);
    return gui;
  }

  /** From org.bdgp.io.DataAdapter interface */
  public IOOperation[] getSupportedOperations() {
    return supportedOperations;
  }

  /** Used by GenericFileAdapterGUI */
  public String getType() {
    return getName();
  }

  public void setInput(String inputfile) {
    this.filename = inputfile;
  }
  public String getInput() {
    return filename;
  }

  public void setDataInput(DataInput dataInput) {
    super.setDataInput(dataInput);
    setInput(dataInput.getInputString());
  }

  /** Save original file name so that if we save the data we can access the
   *  header info from the original file; and also so we can save the file name
   *  as a comment. */
  public void setOriginalFilename(String file) {
    logger.debug("setOriginalFilename: setting original filename = " + file);
    this.originalFilename = file;
  }
    

  /** Open the requested file as a stream; check that file really does contain
   *  ChadoXML before returning stream. */
  private InputStream chadoXmlInputStream(String filename) throws ApolloAdapterException {
    InputStream stream = null;
    logger.info("locating Chado XML datasource...");
    if (filename.startsWith("http")) {
      URL url;
      try {
        logger.debug("chadoXmlInputStream: type is URL");
        url = new URL(filename);
        stream = apollo.util.IOUtil.getStreamFromUrl(url, "URL "+url+" not found");
        setOriginalFilename(filename);
        return stream;
      }
      catch (Exception e) {
        stream = null;
        throw new ApolloAdapterException("Error: could not open ChadoXML URL " + 
                                         filename + " for reading.");
      }
    }

    //    else it's a file
    String path = apollo.util.IOUtil.findFile(filename, false);
    try {
      stream = new FileInputStream (path);
      setOriginalFilename(path);
    } catch (Exception e) { // FileNotFoundException
      stream = null;
      throw new ApolloAdapterException("could not open ChadoXML file " + filename + " for reading.");
    }

    BufferedReader in;
    try {
      in = new BufferedReader(new FileReader(path));
    } catch (Exception e) { // FileNotFoundException
      stream = null;
      throw new ApolloAdapterException("Error: could not open ChadoXML file " + 
                                       path + " for reading.");
    }

    // Check whether this appears to be chadoXML
    if (!appearsToBeChadoXML(filename, in)) {
      // Didn't find <chado> line
      throw new ApolloAdapterException("File " + filename + 
                                       "\ndoes not appear to contain chadoXML--couldn't find <chado> line.\n");
    }
    return stream;
  }

  /** This is the main method for reading the data.  The filename should already
   *  have been set. */
  public CurationSet getCurationSet() throws ApolloAdapterException {
    try {
      fireProgressEvent(new ProgressEvent(this, new Double(5.0),
                                          "Finding data..."));
      // Throws DataAdapter exception if faulty input (like bad filename)
      InputStream xml_stream;
      xml_stream = chadoXmlInputStream(getInput());
      return(getCurationSetFromInputStream(xml_stream));
    } catch (ApolloAdapterException dae) { 
      logger.error("Error while parsing " + getInput(), dae);
      throw dae;
    } catch ( Exception ex2 ) {
      logger.error("Error while parsing " + getInput(), ex2);
      throw new ApolloAdapterException(ex2.getMessage());
    }
  }

  public CurationSet getCurationSetFromInputStream(InputStream xml_stream) throws ApolloAdapterException {  
    genomicRegionSet = false;
    CurationSet curation = null;
    try {
      BufferedInputStream bis = new BufferedInputStream(xml_stream);
      /* Now that we know input is not faulty (exception not thrown if
         we got here) we can clear out old data. If we don't clear we
         get exceptions on style changes as apollo tiers get in an
         inconsistent state between old and new data. */
      if (!NO_GUI)
        super.clearOldData();

      // All "progress" percentages are made up--we can't actually tell
      // how far along we are, but we want to give the user an idea that
      // progress is being made.
      fireProgressEvent(new ProgressEvent(this, new Double(10.0),
                                          "Reading XML..."));

      XMLParser parser = new XMLParser();
      XMLElement rootElement = parser.readXML(bis);
      if (rootElement == null) {
        String msg = "XML input stream was empty--nothing loaded.";
        logger.error(msg);
        throw new ApolloAdapterException(msg);
      }
      xml_stream.close();

      fireProgressEvent(new ProgressEvent(this, new Double(40.0),
                                          "Populating data models..."));
      curation = new CurationSet();
      populateDataModels(rootElement, curation);

      logger.info ("Completed XML parse of " + curation.getName());
      /* need to eradicate the huge hash tables generated when the xml is parsed */
      parser.clean ();
      rootElement = null;
      // parser.clean() already calls System.gc--do we really need to call it again?
      //      System.gc();

      // Set up TransactionManager for curation
      //TransactionManager manager = new TransactionManager();
      fireProgressEvent(new ProgressEvent(this, new Double(90.0),
                                          "Reading transaction file..."));
      //java.util.List transactions = 
      TransactionXMLAdapter.loadTransactions(getInput(), curation, getCurationState());
      //manager.setTransactions(transactions);
      //curation.setTransactionManager(manager);

      fireProgressEvent(new ProgressEvent(this, new Double(95.0),
                                          "Drawing..."));
      // file not found -> data adapter exception
    } catch (ApolloAdapterException dae) { 
      logger.error("Error while parsing " + getInput(), dae);
      throw dae;
    } catch ( Exception ex2 ) {
      logger.error("Error while parsing " + getInput(), ex2);
      throw new ApolloAdapterException(ex2.getMessage());
    }

    curation.setInputFilename(originalFilename);
    return curation;
  }

  /** Like getCurationSet--used for layering new data. */
  public Boolean addToCurationSet() throws ApolloAdapterException {
    // Existing curation_set is stored in superclass
    boolean okay = false;
    if (curation_set == null) {
      String message = "Can't layer ChadoXML data on top of non-ChadoXML data.";
      logger.error(message);
      JOptionPane.showMessageDialog(null,message,"Error",JOptionPane.WARNING_MESSAGE);
      return new Boolean(false);
    }

    // !! Can we get rid of most of this and call getCurationSetFromInputStream() instead? 
    try {
      fireProgressEvent(new ProgressEvent(this, new Double(5.0),
                                          "Finding new data..."));
      // Throws DataAdapter exception if faulty input (like bad filename)
      InputStream xml_stream;
      xml_stream = chadoXmlInputStream(getInput());
      BufferedInputStream bis = new BufferedInputStream(xml_stream);

      // All "progress" percentages are made up--we can't actually tell
      // how far along we are, but we want to give the user an idea that
      // progress is being made.
      fireProgressEvent(new ProgressEvent(this, new Double(10.0),
                                          "Reading XML..."));

      XMLParser parser = new XMLParser();
      XMLElement rootElement = parser.readXML(bis);
      if (rootElement == null) {
        String msg = "XML input stream was empty--nothing loaded.";
        logger.warn(msg);
        throw new ApolloAdapterException(msg);
      }
      xml_stream.close();

      fireProgressEvent(new ProgressEvent(this, new Double(40.0),
                                          "Populating data models..."));
      populateDataModels(rootElement, curation_set);

      logger.info ("Completed XML parse of file " + getInput() + " for region " + curation_set.getName());
      /* need to eradicate the huge hash tables generated when the xml is parsed */
      parser.clean ();
      rootElement = null;

      //TransactionManager manager = curation_set.getTransactionManager();
      fireProgressEvent(new ProgressEvent(this, new Double(90.0),
                                          "Reading transaction file..."));
      //java.util.List transactions = --> sets curs sets transactions
      TransactionXMLAdapter.loadTransactions(getInput(), curation_set, getCurationState());
      //manager.setTransactions(transactions);

      fireProgressEvent(new ProgressEvent(this, new Double(95.0),
                                          "Drawing..."));
      okay = true;
    } 
    // file not found -> data adapter exception
    catch (ApolloAdapterException dae) { 
      logger.error("Error while parsing " + getInput(), dae);
      throw dae;
    } catch ( Exception ex2 ) {
      logger.error("Error while parsing " + getInput(), ex2);
      throw new ApolloAdapterException(ex2.getMessage());
    }
    
    //    return curation;
    return new Boolean(okay);
  }

  /** Starting with the root XML element, go through the parse tree and
   *  populate the Apollo datamodels for annotations and results. */
  private void populateDataModels(XMLElement rootElement, CurationSet curation)
    throws ApolloAdapterException {
    String seq_id = "";
    int start = -1;
    int end = -1;
    String dna = "";
    String arm = "";

    // Set up empty annotations and results (or retrieve them, if we're layering)
    StrandedFeatureSetI annotations = curation.getAnnots();
    if (annotations == null) {
      annotations = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
      annotations.setName("Annotations");
      annotations.setFeatureType("Annotation");
      curation.setAnnots(annotations);
    }

    // Set up empty annotations and results (or retrieve them, if we're layering)
    StrandedFeatureSetI results = curation.getResults();
    if (results == null) {
      results = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
      analyses = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
      all_analyses = new Hashtable();
    }

    // For memory use, it would be much better if we could just get one
    // child at a time instead of all at once.
    int total = rootElement.numChildren();
    int count = 0;
    while (rootElement.numChildren() > 0) {
      try {
        ++count;
        fireProgressEvent(new ProgressEvent(this, new Double(40.0 + ((double)count/(double)total)*55.0),
                                            "Parsing XML element #" + count));

        // Pop off the next child of the root element and handle it
        // 3/2005: Theoretically, this should reduce the maximum memory use,
        // as children are removed from the XML parse tree as Apollo datamodels
        // are filled in.  In practice, it doesn't seem to help much, but it
        // shouldn't hurt.
        // To really improve memory use, we shouldn't build a parse tree of
        // the entire xml input--we should handle each element as we read it.
        // But that's a bigger reorganization than I'm prepared to do right now.
        XMLElement element = rootElement.popChild();

        // What happens if we don't have an _appdata?  How do we get the 
        // region id, start, end, and residues?
        if (element.getAttribute("name") != null) {
          try {
            //   <_appdata  name="title">CG9397</_appdata>
            if (element.getAttribute("name").equalsIgnoreCase("title")) {
              seq_id = element.getCharData();
            }
            else if (element.getAttribute("name").equalsIgnoreCase("arm")) {
              arm = element.getCharData();
              // If we didn't get a title for this region, use the chromosome arm
              if (seq_id.equals(""))
                seq_id = arm;
	      if (arm != null && !arm.equals(""))
                curation.setChromosome(arm);  // trim crud off end of arm name?
              // If there is no arm supplied, should we set the chromosome name to
              // some default?
            }
            else if (element.getAttribute("name").equalsIgnoreCase("fmin")) {
              try {
                start = Integer.parseInt (element.getCharData());
              } catch (Exception e) {
                logger.error("Couldn't parse integer from fmin " + element.getCharData() + " in XML element " + count, e);
              }
            }
            else if (element.getAttribute("name").equalsIgnoreCase("fmax")) {
              try {
                end = Integer.parseInt (element.getCharData());
              } catch (Exception e) {
                logger.error("Couldn't parse integer from fmax " + element.getCharData() + " in XML element " + count, e);
              }
            }
            // Apparently it is important to have a "residues" _appdata record
            // even if you don't have residues, because this is also where the
            // curation start/end get set.
            else if (element.getAttribute("name").equalsIgnoreCase("residues")) {
              dna = element.getCharData();
            }
          } catch (Exception e) {
            logger.warn("error parsing map element " + ChadoXmlUtils.printXMLElement(element), e);
          }
        }
        else if (element.getType().equalsIgnoreCase("feature")) {
          // If we hit a feature, we must be done reading the genomic region info
          // (the _appdata records), so set up the genomic ref seq and range (if it
          // hasn't already been set up).
          if (!genomicRegionSet)
            createGenomicRegion(start, end, seq_id, dna, curation, annotations, results);

          FeatureSetI feature = processFeature(element, curation);
          logger.debug("Processed feature " + feature.getName());
          if (feature instanceof AnnotatedFeatureI) {
            annotations.addFeature(feature);
          }
        }
        else if (element.getType().equalsIgnoreCase("cv") ||
                 element.getType().equalsIgnoreCase("cvterm") ||
                 element.getAttribute("lookup") != null) {
          String error = "This file includes macros!  I can't deal with macros!\nOffending element: " + ChadoXmlUtils.printXMLElement(element);
          logger.error(error);
          throw new ApolloAdapterException(error);
        }
        else
          logger.error("Unknown top-level element " + element.getType());
      } catch (ApolloAdapterException dae) { 
        throw dae;
      } catch ( Exception ex2 ) {
        logger.error("Caught exception while parsing XML", ex2);
        throw new ApolloAdapterException(ex2.getMessage());
      }
    }

    // Complain if we didn't get fmin and fmax from _appdata field
    if (curation.getStart() == curation.getEnd()) {
      String error = "Error: input from " + originalFilename + "\nis not valid Chado XML--couldn't find _appdata fields.\nProbably the input was not actually Chado XML but was actually some other format.\n";
      logger.error(error);
      // This will cause a popup window with the error message
      throw new ApolloAdapterException(error);
    }

    curation.setResults(analyses);  // analyses was filled in as results were parsed
  }

  /** Sets up a refsequence for curation set (and also sets the genomic range).
   *  Right now, this relies on there being _appdata records at the beginning of
   *  the chadoXML file:
   * <chado  dumpspec="dumpspec_apollo.xml" date="Wes Sept 24 12:45:36 EDT 2003">
   *    <_appdata  name="title">FBgn0000826</_appdata>
   *    <_appdata  name="arm">X</_appdata>
   *    <_appdata  name="fmin">1212808</_appdata>
   *    <_appdata  name="fmax">1214934</_appdata>
   *    <_appdata  name="residues">GAGAAGCAACACTTCAGTCTGACCAAAATCCTCAAGA...</_appdata>
   *
   * This is an idiosyncratic FlyBase way of representing the region info.
   * It would be nice if there were a more standardized way of doing this, or
   * if Apollo could at least estimate the fmin/fmax by looking at the features
   * and finding the lowest/highest endpoints.
   * Unfortunately, the genomic region info currently needs to be set up BEFORE we read
   * the features. */
  private void createGenomicRegion(int start, int end, String seq_id, String dna,
                                   CurationSet curation, StrandedFeatureSetI annotations, StrandedFeatureSetI results) {
    // Create genomic sequence.
    // Use GAMESequence for now.
    GAMESequence curated_seq = new GAMESequence(seq_id, Config.getController(), dna);
    // ! Complain if dna length is not equal to end-start?
    curated_seq.setLength(Math.abs(end-start)+1);
    // The genomic sequence usually just has one identifier, so use it as name and accession.
    curated_seq.setName(seq_id);
    curated_seq.setAccessionNo(seq_id);
    curated_seq.setResidueType(SequenceI.DNA);
    curation.setRefSequence(curated_seq);
    curation.addSequence(curated_seq);

    annotations.setRefSequence(curated_seq);
    results.setRefSequence(curated_seq);

    // Set range for genomic region (curation)

    // need to add 1 to start.  chado is interbase - apollo is base oriented.
    ++start;

    // Need setLow and setHigh for curation.  Don't need setRange.
    curation.setLow(start);
    curation.setHigh(end);
    // Strand is not specified in the _appdata fields--do we need it?
    int strand = (start < end) ? 1 : -1;
    curation.setStrand(strand);
    curation.setName(seq_id);

    genomicRegionSet = true;
  }

  /** Returns a feature that is either an annotation or a result */
  private FeatureSetI processFeature(XMLElement xml, CurationSet curation) throws ApolloAdapterException {
    Vector elements = xml.getChildren();
    boolean isResult = true;
    String type = "";
    String name = "";
    String uniquename = "";

    for (int i = 0; i < elements.size(); i++) {
      XMLElement child = (XMLElement) elements.elementAt(i);
      if (child.getType().equalsIgnoreCase("is_analysis")) {
        if (child.getCharData().equals("0"))
          isResult = false;
        else
          isResult = true;
      }
      else if (child.getType().equalsIgnoreCase("type_id")) {
        type = getDataType(child);
      }
      else if (child.getType().equalsIgnoreCase("name"))
        name = child.getCharData();
      else if (child.getType().equalsIgnoreCase("uniquename"))
        uniquename = child.getCharData();
    }

    if (isResult) {
      FeatureSetI result = new FeatureSet();
      result.addProperty("is_analysis", "1");
      result.setId(uniquename);  // Need?
      result.setName(name);  // Need?
      processResult(xml, result,curation);
      logger.debug("After processResult, id is " + result.getId() + " for result " + result.getName());
      // Actually, this isn't the right result type--the result type is set in processResult
      //      result.setType(type);
      logger.debug("processFeature: returning result " + result);
      return result;
    }
    else {  // annotation
      AnnotatedFeature annot = new AnnotatedFeature();
      annot.setId(uniquename);
      annot.setName(name);
      logger.debug("Before processAnnot, id = " + uniquename + ", name = " + name); // DEL
      annot.setFeatureType(type);
      annot.setTopLevelType(type); //?
      processAnnot(xml, annot, curation);
      if (logger.isDebugEnabled()) {
        logger.debug("processFeature: returning annotation " + annot + " (type " + type + "): id = " + annot.getId() + ", name = " + annot.getName());
      }
      return annot;
    }
  }

  /** Read in and return a result.
   *  xml is <feature> node. */
  private FeatureSetI processResult(XMLElement xml, FeatureSetI result,
                                    CurationSet curation) {
    Vector elements = xml.getChildren();
    for (int i = 0; i < elements.size(); i++) {
      XMLElement child = (XMLElement) elements.elementAt(i);
      // I don't think featurelocs ever appear at this level
      if (child.getType().equalsIgnoreCase("featureloc")) {
        logger.warn("How weird--a featureloc right under a result for " + ChadoXmlUtils.printXMLElement(xml));
        return null;
      }
      else if (child.getType().equalsIgnoreCase("name")) {
        result.setName(child.getCharData());
      }
      else if (child.getType().equalsIgnoreCase("uniquename")) {
        result.setId(child.getCharData());
      }
      // The fields that are in analysisfeature are stored in apollo at the analysis
      // level, not the result set level, but we'll just save them as properties
      // and then copy them up to the analysis level later.
      else if (child.getType().equalsIgnoreCase("analysisfeature")) {
        handleAnalysisType(child, result);
      }
      // Child of feature set--feature span (might be a singleton or a feature pair)
      else if (child.getType().equalsIgnoreCase("feature_relationship")) {
        SeqFeatureI seqFeat = getSeqFeature(child, result,curation);
        logger.debug("result name = " + result.getName() + ", seq feat name = " + seqFeat.getName());
        result.addFeature(seqFeat);
        result.setStrand(seqFeat.getStrand());
      }
      // if it's a flat field, e.g. <timelastmodified>2004-10-08 16:12:11.477648</timelastmodified>
      // then attach it to the result as a property
      else {
        if (!child.getCharData().equals("")) {
          addField(result, child);
        }
      }
    }

    addResultToAnalysis(result);
    return result;
  }

  /** Read in info for annotation (skeleton is already set up). */
  private AnnotatedFeatureI processAnnot(XMLElement xml, AnnotatedFeature annot, CurationSet curation) throws ApolloAdapterException {
    // Make sure we got a featureloc for this feature
    boolean gotFeatureloc = false;

    annot.addProperty("is_analysis", "0");

    Vector elements = xml.getChildren();
    for (int i = 0; i < elements.size(); i++) {
      XMLElement child = (XMLElement) elements.elementAt(i);
      if (child.getType().equalsIgnoreCase("dbxref_id")) {
        // true means this is a primary dbxref (not a feature_dbxref)
        DbXref xref = getDbxref(child, annot, true);
        if (xref != null) {
          String dbxref = xref.getIdValue();
          if (annot.getId() != null && 
              !(dbxref.equals(annot.getId()))) {
            logger.warn("annot's dbxref_id " + dbxref + " doesn't match its uniquename " + annot.getId());
            // Don't do this here--do this later if annot name is not set.
            //          logger.warn("Using " + annot.getId() + " as the annotation name (unless it is later overridden by a <name> record)");  // ,\nand " + dbxref + " as the id.");
            //          annot.setName(annot.getId());
          }
          else
            // For now, use the primary dbxref as the annot id--it may get overridden later
            // if we see a <uniquename> record (but usually that matches the primary dbxref).
            annot.setId(dbxref);
        }
      }
      // 7/2005: Apparently some features have a uniquename that is not the same
      // as their dbxref_id.
      else if (child.getType().equalsIgnoreCase("uniquename")) {
        if (annot.getId() != null && !(annot.getId().equals(child.getCharData()))) {
          logger.warn("annot's primary xref id " + annot.getId() + " doesn't match its uniquename " + child.getCharData());
          // Is this really the right thing to do?
          //          logger.debug("Using new uniquename " + child.getCharData() + " as the annotation name (unless it is later overridden by a <name> record).");
          //          annot.setName(child.getCharData());
          logger.warn("Using new uniquename " + child.getCharData() + " as the annotation id");
        }
        annot.setId(child.getCharData());
      }
      else if (child.getType().equalsIgnoreCase("featureloc")) {
        gotFeatureloc = true;
        handleFeatureLoc(child, annot, curation);
      }
      else if (child.getType().equalsIgnoreCase("feature_relationship")) {
        //        logger.debug("processAnot: getting transcript of annot " + annot.getName() + " (" + annot.getId() + "), range " + annot.getStart() + "-" + annot.getEnd()); // DEL
        Transcript transcript = getTranscript(child, curation);

        // In ChadoXML, certain types of annotations (pseudogenes, tRNAs, etc.) are called
        // "gene" at the annot level, but at the transcript level you see "pseudogene" or "tRNA"
        // instead of "mRNA" (protein coding genes have "mRNA").  Apollo needs these types at
        // the annot level.
        // As transcripts were read in, their biotypes should have been set appropriately.
        // Need to record it here, because when we do annot.addFeature(transcript), transcript's
        // biotype will henceforth inherit its parent's biotype.
        // (Note that this will not happen for exonless "transcripts", because
        // they will be caught by the test in the previous "else if".)
        String transcriptType = transcript.getTopLevelType();
        annot.addFeature(transcript);
        annot.setStrand(transcript.getStrand());
        annot.setFeatureType(transcriptType);
        //          logger.debug("Set annotation " + annot.getName() + "'s type to " + transcriptType + " based on type of transcript " + transcript.getName()); // DEL
        annot.setTopLevelType(transcriptType);
        //	}
      }
      else if (child.getType().equalsIgnoreCase("feature_synonym")) {
        Synonym syn = getSynonym(child);
        annot.addSynonym(syn);
      }
      else if (child.getType().equalsIgnoreCase("featureprop")) {
        getProperty(child, annot);
      }
      else if (child.getType().equalsIgnoreCase("feature_dbxref")) {
        // Add this as a dbxref
        getDbxref(child, annot, false);
      }
      else if (child.getType().equalsIgnoreCase("feature_cvterm")) {
        if (!warnedAboutFeatureCvterm) {
          logger.warn("not handling feature_cvterm(s) for annot " + annot.getId());
          warnedAboutFeatureCvterm = true;
        }
      }
      else if (child.getType().equalsIgnoreCase("feature_pub")) {
        logger.warn("not handling feature_pub for annot " + annot.getId());
      }
      else if (child.getType().equalsIgnoreCase("organism_id")) {
        getOrganism(child, annot);
	// If curation set doesn't have an organism defined, use this annot's
	if (curation.getOrganism() == null || curation.getOrganism().equals(""))
	    curation.setOrganism(annot.getProperty("organism"));
      }

      // Else it's a field we don't know about.
      // If it's a flat field, e.g. <timelastmodified>2004-10-08 16:12:11.477648</timelastmodified>
      // then attach it as a property
      else {
        if (!child.getCharData().equals("")) {
          addField(annot, child);
        }
      }
    }

    // The data from the FlyBase server includes many transposable elements
    // with no featureloc (i.e. no start/end position).  These were showing up
    // spanning the whole genome.  Now the start/end is forced to 0 (which might
    // or might not be visible within the currently displayed range).
    if (!gotFeatureloc) {
      logger.warn(annot.getFeatureType() + " annotation " + annot.getName() + " (" + annot.getId() + ") has no start/end positions!");
      if (annot.getStrand() == 0) {
        // Temporarily force it onto + strand so it won't show up on both strands
        annot.addProperty("unstranded", "true");
        annot.setStrand(1);
      }
      // Set start/end to 0
      annot.setStart(0);
      annot.setEnd(0);
      return annot;
    }

    // Now that we're treating one-level annots as one-levels (rather than
    // promoting them to three levels), we expect them to be identified as such
    // in the tiers file:
    // number_of_levels : 1
    // If the user has an out-of-date tiers file without this line for a
    // one-level type, the one-level annot will not show up.  Pop up a warning.
    warnIfOneLevelDiscrepancy(annot);

    // If annot is unstranded, it won't show up--temporarily force it onto a strand.
    forceStrandIfNeeded(annot);

    // If annot doesn't have a name, use its ID as a name
    // (need a name for it to display as a label).
    if (annot.getName().equals("no_name") && annot.getId() != null) {
      logger.warn("annot with uniquename " + annot.getId() + " has no name--using uniquename as name.");
      annot.setName(annot.getId());
    }

    return annot;
  }

  /** Now that we're treating one-level annots as one-levels (rather than
    * promoting them to three levels), we expect them to be identified as such
    * in the tiers file:
    * number_of_levels : 1
    * If the user has an out-of-date tiers file without this line for a
    * one-level type, the one-level annot will not show up.  Pop up a warning. */
  private void warnIfOneLevelDiscrepancy(SeqFeature annot) {
    if ((annot.getFeatures() == null || annot.getFeatures().size() == 0)) {
      FeatureProperty fp = Config.getPropertyScheme().getFeatureProperty(annot.getFeatureType());
      if (fp.getNumberOfLevels() != 1) {
        // errorDialog prints the message to stderr if we're running in command-line mode
        apollo.util.IOUtil.errorDialog("Annotation " + annot.getName() + " is one-level, but type " + annot.getFeatureType() + "\ndoes not have 'number_of_levels: 1' in tiers file " + Config.getStyle().getTiersFile() + ".\nEither the data is buggy or your tiers file is out of date.");
      }
    }
  }

  /** Wordy chadoXML way to store feature type is
   *    <type_id>       <cvterm>        <cv_id>          <cv>            <name>SO</name>          </cv>        </cv_id>
   *    <name>gene</name>      </cvterm>    </type_id>
   *   So the actual type name (in this case, gene) is in the "name" element. */
  private String getDataType(XMLElement xml) {
    // The xml element could be the parent of the type_id node.
    if (!(xml.getType().equalsIgnoreCase("type_id"))) {
      xml = getChild(xml, "type_id");
      if (xml == null)
        return null;
    }

    XMLElement child = (XMLElement) xml.getChildren().firstElement();  // cvterm
    XMLElement grandchild2 = getChild(child, "name");
    return(grandchild2.getCharData());
  }

  /** <feature_dbxref> (optional layer--expect this if is_primary_dbxref is false)
   *   <dbxref_id>      
   *     <dbxref>        
   *       <accession>CG10833</accession>
   *       <db_id>
   *         <db>
   *           <contact_id>  <contact>  <description>dummy</description>  (optional)
   *           <name>FlyBase</name>
   *   </dbxref_id>
   *   <is_current>   (only for feature_dbxrefs)
   *   </feature_dbxref> (optional) 
   *  Adds a dbxref to feature (if feature is not null) and also 
   *  returns the dbxref. */
  private DbXref getDbxref(XMLElement xml, SeqFeature feature, boolean is_primary_dbxref) {
    Vector children = xml.getChildren();
    if (children == null) {
      logger.warn("getDbxref: no children of " + ChadoXmlUtils.printXMLElement(xml));
      return null;
    }
    // First child is dbxref if this is a primary dbxref (came in as dbxref_id).
    XMLElement child = (XMLElement) children.firstElement();
    // If not, we need to get the child dbxref of the child (the child is dbxref_id).
    if (!is_primary_dbxref)
      child = getChild(child, "dbxref");

    Vector grandchildren = child.getChildren();
    if (grandchildren == null) {
      logger.warn("getDbxref: no grandchildren of " + ChadoXmlUtils.printXMLElement(xml));
      return null;
    }
    XMLElement grandchild = (XMLElement) grandchildren.firstElement();  // accession (is it always first??)
    if (!grandchild.getType().equalsIgnoreCase("accession")) {
      logger.warn("Grandchild of dbxref_id is not accession: " + ChadoXmlUtils.printXMLElement(grandchild));
      return null;
    }
    String acc = grandchild.getCharData();
    XMLElement db_xml = getChild(child, "db_id");
    String db = "";
    if (db_xml != null)
      db = getDb(db_xml);

    int isCurrent = 1;
    if (!is_primary_dbxref) {
      XMLElement current = getChild(xml, "is_current");
      if (current != null) {
        try {
          isCurrent = Integer.parseInt(current.getCharData());
        } catch (Exception e) {
          logger.warn("Couldn't parse integer from is_current " + current + " for acc " + acc, e); 
        }
      }
    }
    if (!(db==null) && !db.equals("") && !(acc==null) && !acc.equals("")) {
      DbXref xref = new DbXref ("id", acc, db);
      if (is_primary_dbxref) {
        //        logger.debug("Saved primary dbxref " + acc + " for " + feature); // DEL
        xref.setIsPrimary(true);
        xref.setIsSecondary(false);
      }
      // If we're going to add this as a secondary dbxref, first check if it was
      // already saved as a primary dbxref (those should come first).
      else {
        DbXref primary = feature.getPrimaryDbXref();
        // If we already have this one as a primary xref, set isSecondary true also,
        // and readd this xref (addDbXref will clobber the old one because it
        // uses a hash to store them).
        if (primary != null && primary.getIdValue().equals(acc)) {
          primary.setIsSecondary(true);
          xref = primary;
        }
      }
      xref.setCurrent(isCurrent);
      if (feature != null)
        feature.addDbXref(xref); 
      return xref;
    }
    return null;
  }

  /** <db_id>
   *    <db>
   *       <contact_id>  <contact>  <description>dummy</description>  (optional)
   *       <name>FlyBase</name> */
  private String getDb(XMLElement xml) {
    XMLElement name = getGrandchild(xml, -1, "name");
    if (name == null)
      return "";
    else
      return(name.getCharData());
  }

  /** Sets start, end, and strand for feature, based on featureloc record. */
  private void handleFeatureLoc(XMLElement xml, SeqFeatureI feature, CurationSet curation) {
    Vector children = xml.getChildren();
    int start = -1, end = -1, strand = 1;
    for (int i = 0; i < children.size(); i++) {
      XMLElement child = (XMLElement) children.elementAt(i);
      try {
        if (child.getType().equalsIgnoreCase("fmin")) {
          // Need to add 1 to start.  chado is interbase - apollo is base oriented.
          try {
            start = Integer.parseInt (child.getCharData()) + 1;
            feature.setLow(start);
          } catch (Exception e) {
            logger.error("Couldn't parse integer from fmin " + child.getCharData(), e);
          }
        }
        else if (child.getType().equalsIgnoreCase("fmax")) {
          try {
            end = Integer.parseInt (child.getCharData());
            feature.setHigh(end);
          } catch (Exception e) {
            logger.error("Couldn't parse integer from fmax " + child.getCharData(), e);
          }
        }
        // Chado XML explicitly notes the strand, rather than having fmin > fmax.
        else if (child.getType().equalsIgnoreCase("strand")) {
          try {
            strand = Integer.parseInt (child.getCharData());
            feature.setStrand(strand);
          } catch (Exception e) {
            logger.error("Couldn't parse integer from strand " + child.getCharData(), e);
          }
        }
        // Most featurelocs have rank=0.  Subject (hit) features have rank=1.
        // Exons also have this rank=0, which we don't want--it will obscure
        // the real rank!
        else if (isAnalysis(feature) && child.getType().equalsIgnoreCase("rank")) {
          //          logger.debug("adding rank = " + child.getCharData() + " to analysis feature " + feature.getName() + "(is_analysis = " + feature.getProperty("is_analysis")); // DEL
          feature.addProperty(child.getType(), child.getCharData());
        }
        // Result features have residue_info in their featurelocs.
        else if (child.getType().equalsIgnoreCase("residue_info")) {
          String residues = child.getCharData();
          feature.setExplicitAlignment(residues);
        }
        // Annotation features (e.g. exons) have nothing useful in their
        // srcfeature_ids, and in fact have uninteresting names (the chrom arm name)
        // that we don't want to have clobber the real name.  So only look at srcfeature_ids
        // for analysis results.
        // !! Change
        else if (child.getType().equalsIgnoreCase("srcfeature_id")) {
          if (isAnalysis(feature))
            handleSrcFeature(child, feature, curation);
        }
        // Save these as properties
        else if (child.getType().equalsIgnoreCase("is_fmin_partial") ||
                 child.getType().equalsIgnoreCase("is_fmax_partial")) {
          feature.addProperty(child.getType(), child.getCharData());
          //	  if (child.getType().equalsIgnoreCase("is_fmax_partial")) // DEL
          //	      logger.debug("Saved " + child.getType() + "=" + feature.getProperty(child.getType()) + " for " + feature.getName()); // DEL
        }
      } catch (Exception e) {
        logger.error("Exception handling featureloc " + ChadoXmlUtils.printXMLElement(xml), e);
      }
    }
  }

  /** Handle the scrfeature_id record, which is for a query or subject.
   *  <srcfeature_id>
   *    <feature>
   *     <dbxref_id>
   *     <name>RH26018.3prime</name>
   *     <organism_id>
   *     <residues>
   *     <type_id>
   *     <featureprop>
   *      <name>description</name>
   *      <value>gb|CK135150|bdgp|RH26018.3prime DESCRIPTION:&quot;RH26018.3prime RH Drosophila melanogaster normalized Head pFlc-1 Drosophila melanogaster cDNA clone RH26018 3, mRNA sequence.&quot; organism:&quot;Drosophila melanogaster&quot; (02-DEC-2003)</value>
   *    </feature> */
  void handleSrcFeature(XMLElement xml, SeqFeatureI feat, CurationSet curation) {
    // First (only?) child of <srcfeature_id> should be <feature>
    XMLElement xml_feature = getChild(xml, "feature");
    if (xml == null) {
      logger.error("handleSrcFeature: couldn't find feature child of " + ChadoXmlUtils.printXMLElement(xml));
      return;
    }

    Vector children = xml_feature.getChildren();
    for (int i = 0; i < children.size(); i++) {
      XMLElement child = (XMLElement) children.elementAt(i);
      try {
        if (child.getType().equalsIgnoreCase("is_analysis")) {
          // ignore
        }
        // This isn't the name of this feature; it's the name of its
        // reference feature.
        else if (child.getType().equalsIgnoreCase("name")) {
          feat.replaceProperty("ref_name", child.getCharData());
          // While we're at it (is this a good place??) set the seq's display id
	  SequenceI seq = feat.getRefSequence();  // in case it was already created
	  if (seq == null) {
            seq = new Sequence(feat.getName(), "");
            feat.setRefSequence(seq);
            //             logger.debug("Created new ref sequence for " + feat.getName() + " with " + seq.getResidues().length() + " residues"); // DEL
	  }
          seq.setName(child.getCharData());
        }
        else if (child.getType().equalsIgnoreCase("uniquename")) {
          feat.replaceProperty("ref_id", child.getCharData());
        }
        else if (child.getType().equalsIgnoreCase("dbxref_id")) {
          // Use the dbxref of the FIRST query we happen to see to set the curation set's
          // refseq's dbxref (if it hasn't already been set)
          if (!savedGenomicDbxref && feat.getProperty("rank").equals("0")) {
            DbXref xref = getDbxref(child, null, true);
            if (xref != null) {
              curation.getRefSequence().addDbXref(xref);
              //              logger.debug("Set genomic dbxref from analysis query feature " + feat.getId()); // DEL
              savedGenomicDbxref = true;
            }
          }
        }
        else if (child.getType().equalsIgnoreCase("organism_id")) {
          getOrganism(child, feat);
        }
        else if (child.getType().equalsIgnoreCase("type_id")) {
          String type = getDataType(child);
          // This isn't really the type--just save it as a property
          //          feat.setType(type);
          feat.addProperty("ref_type", type);
        }
        else if (child.getType().equalsIgnoreCase("residues")) {
          // This is not the alignment--this is the whole sequence.
          // Make a seq record with it.
          String residues = child.getCharData();
	  SequenceI seq = feat.getRefSequence();  // in case it was already created
	  if (seq == null) {
            seq = new Sequence(feat.getName(), residues);
            feat.setRefSequence(seq);
	  }
          else
            seq.setResidues(residues);
	}
        // featureprops that appear here can include description and feature tags
        else if (child.getType().equalsIgnoreCase("featureprop")) {
          getProperty(child, feat);
        }
      } catch (Exception e) {
        logger.error("Exception handling srcfeature_id " + ChadoXmlUtils.printXMLElement(child) + ": " + e, e);
      }
    }
  }

  private boolean isAnalysis(SeqFeatureI feat) {
    if (feat.getProperty("is_analysis").equals("") || 
        feat.getProperty("is_analysis").equals("0") ||
        feat.getProperty("is_analysis").equals(FIELD_LABEL + "0"))
      return false;
    else
      return true;
  }

  /** Add a flat field (e.g. <is_obsolete>1</is_obsolete>) as a property,
      marking it as a field if appropriate. */
  private void addField(SeqFeatureI feat, XMLElement xml) {
    String type = xml.getType();
    String value = xml.getCharData();
    if (!ChadoXmlWrite.isSpecialProperty(type))
      // Mark it as being a field so that it can be written out properly
      value = FIELD_LABEL + value;
    feat.addProperty(type, value);
  }

  /** For parsing children (transcripts/exons) of an annotation.
   *   xml node passed in is feature_relationship.
   *   <feature_relationship>
   *     <subject_id>
   *       <feature>
   *         <uniquename>CG10833-RA</uniquename>          
   *         <feature_relationship>
   *           [exons] 
   *         <featureloc> */
  private Transcript getTranscript(XMLElement xml, CurationSet curation) throws ApolloAdapterException {
    Transcript transcript = new Transcript();
    transcript.setRefSequence(curation.getRefSequence());
    transcript.addProperty("is_analysis", "0");
    Vector children = xml.getChildren();
    Vector grandchildren = null;
    for (int i = 0; i < children.size(); i++) {
      XMLElement child = (XMLElement) children.elementAt(i);
      if (child.getType().equalsIgnoreCase("subject_id")) {
        grandchildren = child.getChildren();
        // First grandchild should be <feature>
        if (grandchildren == null) {
          logger.error("getTranscript: no grandchildren for " + ChadoXmlUtils.printXMLElement(xml));
          return transcript;
        }

        // First (only?) child of <subject_id> should be <feature>
        XMLElement xml_feature = (XMLElement) grandchildren.firstElement();
        if (!xml_feature.getType().equalsIgnoreCase("feature")) {
          logger.error("getTranscript: found non-feature child " + ChadoXmlUtils.printXMLElement(xml_feature) + "\n of subject_id " + ChadoXmlUtils.printXMLElement(child));
          return null;
        }
        Vector transcript_parts = xml_feature.getChildren();
        for (int j = 0; j < transcript_parts.size(); j++) {
          XMLElement tp = (XMLElement) transcript_parts.elementAt(j);
          // For transcripts, note that <uniquename> is same as dbxref_id
          if (tp.getType().equalsIgnoreCase("dbxref_id")) {
            DbXref xref = getDbxref(tp, transcript, true);
            if (xref != null)
              transcript.setId(xref.getIdValue());
          }
          else if (tp.getType().equalsIgnoreCase("uniquename")) {
        	//<uniquename> is required in the Chado spec, so if it's null, throw an exception
        	if (tp.getCharData() == null) {
        		String errMsg = "<uniquename> cannot be null";
        		logger.error(errMsg);
        		throw new ApolloAdapterException(errMsg);
        	}
            if (!(tp.getCharData().equals(transcript.getId()))) {
              logger.warn("uniquename " + tp.getCharData() + " doesn't match transcript's dbxref_id " + transcript.getId());
              transcript.setId(tp.getCharData());
            }
          }
          else if (tp.getType().equalsIgnoreCase("name")) {
            transcript.setName(tp.getCharData());
          }
          // In ChadoXML, the type_id of transcripts is "mRNA" if it's a coding gene,
          // or it might be "pseudogene", "tRNA", etc.  These types will need to be propagated
          // up to the parent annotation.
          else if (tp.getType().equalsIgnoreCase("type_id")) {
            String type = getDataType(tp);
            if (type.equalsIgnoreCase("mRNA"))
              transcript.setTopLevelType("gene");  // type of parent
            else
              transcript.setTopLevelType(type);
            //            logger.debug("Type of transcript " + transcript.getName() + " is " + transcript.getBioType()); // DEL
          }
          else if (tp.getType().equalsIgnoreCase("md5checksum")) {
            String checksum = tp.getCharData();
            if (checksum != null && !checksum.equals ("")) {
              SequenceI seq = transcript.get_cDNASequence();
              if (seq == null) {
                seq = new Sequence(transcript.getId(), "");
                transcript.set_cDNASequence(seq);
              }
              seq.setChecksum (checksum);
            }
          }
          else if (tp.getType().equalsIgnoreCase("residues")) {
            String dna = tp.getCharData();
            SequenceI seq = transcript.get_cDNASequence();
            if (seq == null) {
              seq = new Sequence(transcript.getId(), dna);
              transcript.set_cDNASequence(seq);
            }
            else
              seq.setResidues(dna);

            // We don't YET have the range for the mRNA seq--featureloc
	    // for transcript comes later, after all the exons
//            seq.setRange(new Range(0, dna.length()));
            seq.setLength(dna.length());  // Need?
            //            seq.setDisplayId(seq_id);
            if (transcript.getId() != null && !transcript.getId().equals(""))
              seq.setAccessionNo(transcript.getId());
            seq.setResidueType(SequenceI.DNA);
            //            logger.debug("Added cDNA seq " + seq.getDisplayId() + ": range = " + seq.getRange().getStart() + "-" + seq.getRange().getEnd()); // DEL
            curation.addSequence(seq);
          }
          else if (tp.getType().equalsIgnoreCase("seqlen")) {
            // Ignore?  Or check that it equals the length of the residue string?
          }
	  // feature_relationship is usually for a child exon
          else if (tp.getType().equalsIgnoreCase("feature_relationship")) {
            // <feature_relationship>  <rank>
            //                         <subject_id>  <feature>  <type_id>
            XMLElement rank = getChild(tp, "rank");
            if (rank != null)
              transcript.addProperty(tp.getType(), tp.getCharData());

            XMLElement subject_id = getChild(tp, "subject_id");
            String type = getDataType(getGrandchild(subject_id, "type_id"));
            if (type.equalsIgnoreCase("exon")) {
              // Why are we using the curation refsequence??
              ExonI exon = getExon(tp, curation.getRefSequence());
              transcript.addExon(exon);
              // Check whether strand clashes with existing strand?
              if (transcript.getStrand() != 0 && exon.getStrand() != transcript.getStrand())
                logger.warn("strand for " + exon + " doesn't match strand " + transcript.getStrand() + " for transcript " + transcript); // DEL
              transcript.setStrand (exon.getStrand());
            }
            else if (type.equalsIgnoreCase("protein")) {
              addPeptide(getChild(subject_id, 0, null), transcript, curation);
            }
            else {
              logger.error("Don't know how to handle child type " + type + " for transcript " + transcript.getId());
            }
          }
          else if (tp.getType().equals("featureloc")) {
            handleFeatureLoc(tp, transcript, curation);
            // This is happening with the new chadoXML ARGS examples
            if (transcript.get_cDNASequence() == null) {
              //              logger.warn("got featureloc for cDNA but transcript " + transcript.getId() + " (" + transcript.getLow() +"-"+ transcript.getHigh() + ") doesn't yet have a cDNA sequence--creating one"); 
              // create one now
	      Sequence seq = new Sequence(transcript.getId(), "");
              transcript.set_cDNASequence(seq);
            }
	    else {
              // NOW we can do this--when we created the sequence, we didn't
              // yet have the mRNA featureloc.
              transcript.get_cDNASequence().setRange(new Range(transcript.getLow(), transcript.getHigh()));
	    }
	  }
          else if (tp.getType().equalsIgnoreCase("feature_dbxref")) {
            // Add this as a feature_dbxref (not primary)
            getDbxref(tp, transcript, false);
          }
          else if (tp.getType().equalsIgnoreCase("feature_synonym")) {
            Synonym syn = getSynonym(tp);
            transcript.addSynonym(syn);
          }
          else if (tp.getType().equalsIgnoreCase("featureprop")) {
            getProperty(tp, transcript);
          }
          else if (tp.getType().equalsIgnoreCase("feature_cvterm")) {
            if (!warnedAboutFeatureCvterm) {
              logger.warn("not handling feature_cvterm for transcript " + transcript.getId());
              warnedAboutFeatureCvterm = true;
            }
          }
          else if (tp.getType().equalsIgnoreCase("feature_pub")) {
            logger.warn("not handling feature_pub for transcript " + transcript.getId());
          }
          else if (tp.getType().equalsIgnoreCase("organism_id")) {
            getOrganism(tp, transcript);
          }
          // If it's a flat field, e.g. <timelastmodified>2004-10-08 16:12:11.477648</timelastmodified>
          // then attach it as a property
          else {
            if (!(tp.getCharData().equals(""))) {
              addField(transcript, tp);
            }
          }
        }
      }
    }

    // We now can finally deal with transcripts missing their start codon (5prime)--
    // we couldn't do anything when we got that info in the peptide record because we
    // hadn't read in the cDNA seq and range yet.
    if (transcript.isMissing5prime()) {
      //      logger.debug("ChadoXMLAdapter: transcript is missing 5prime (start)--calling setTranslationStartAtFirstCodon() on " + transcript.getId()); // DEL
      transcript.calcTranslationStartForLongestPeptide();
    }

    return transcript;
  }

  /** This does mostly the same thing as getTranscript (at the next level down).
   *   xml element is feature_relationship.
   *   <uniquename>CG10833-RA</uniquename>          
   *   <feature_relationship>
   *   <rank>6</rank>
   *   <subject_id>
   *   <feature>
   *   <name>Cyp28d1:6</name>
   *   ...
   *   <featureloc>
   *   <fmax>5212450</fmax>
   *   <fmin>5212212</fmin> */
  private ExonI getExon(XMLElement xml, SequenceI refSeq) {
    Exon exon = new Exon();
    exon.setRefSequence(refSeq);
    exon.addProperty("is_analysis", "0");
    Vector children = xml.getChildren();

    for (int i = 0; i < children.size(); i++) {
      XMLElement child = (XMLElement) children.elementAt(i);
      if (child.getType().equalsIgnoreCase("rank")) {
        exon.addProperty(child.getType(), child.getCharData());
      }
      else if (child.getType().equalsIgnoreCase("subject_id")) {
        Vector grandchildren = child.getChildren();
        // First grandchild should be <feature>
        if (grandchildren == null) {
          logger.warn("getExon: no grandchildren for " + ChadoXmlUtils.printXMLElement(xml));
          return exon;
        }

        // First (only?) child of <subject_id> should be <feature>
        XMLElement xml_feature = (XMLElement) grandchildren.firstElement();
        if (!xml_feature.getType().equalsIgnoreCase("feature")) {
          logger.warn("Wrong child " + ChadoXmlUtils.printXMLElement(xml_feature) + " of subject_id " + ChadoXmlUtils.printXMLElement(child));
          return exon;
        }
        Vector exon_parts = xml_feature.getChildren();
        for (int j = 0; j < exon_parts.size(); j++) {
          XMLElement ep = (XMLElement) exon_parts.elementAt(j);
          if (ep.getType().equalsIgnoreCase("name")) {
            exon.setName(ep.getCharData());
            //            logger.debug("Exon " + exon.getName() + " has rank " + exon.getProperty("rank"));  // DEL
          }
          // exon id is <uniquename>--not dbxref as for gene.
          else if (ep.getType().equalsIgnoreCase("uniquename")) {
            exon.setId(ep.getCharData());
          }
          else if (ep.getType().equals("featureloc"))
            handleFeatureLoc(ep, exon, null);  // Don't have curation_set, but we shouldn't need it
          else if (ep.getType().equalsIgnoreCase("organism_id")) {
            getOrganism(ep, exon);
          }
          // If it's a flat field, e.g. <timelastmodified>2004-10-08 16:12:11.477648</timelastmodified>
          // then attach it as a property
          else {
            if (!ep.getCharData().equals("")) {
              addField(exon, ep);
            }
          }
        }
      }
    }
    return exon;
  }

  /** Use peptide's featureloc to set translation start and end
      under the xml element passed in:
      <uniquename>CG9397-PA</uniquename>
      <residues>
      <featureloc> */
  private void addPeptide(XMLElement xml, Transcript transcript, CurationSet curation) {
    String seq_id = "";
    Sequence seq = new Sequence(seq_id, "");
    AnnotatedFeatureI protFeat = transcript.getProteinFeat();

    Vector children = xml.getChildren();
    for (int i = 0; i < children.size(); i++) {
      XMLElement child = (XMLElement) children.elementAt(i);
      // uniquename should be same as dbxref_id
      if (child.getType().equalsIgnoreCase("uniquename")) {
        seq_id = child.getCharData();
        if (seq_id != null && !seq_id.equals(""))
          seq.setAccessionNo(seq_id);
      }
      // Peptides don't usually have a dbxref_id, do they?
      else if (child.getType().equalsIgnoreCase("dbxref_id")) {
        //        seq_id = getDbxref(child, protFeat, true);
        DbXref xref = getDbxref(child, (SeqFeature)protFeat, true);
        if (xref != null) {
          seq_id = xref.getIdValue();
          if (seq_id != null && !seq_id.equals(""))
            seq.setAccessionNo(seq_id);
        }
      }
      else if (child.getType().equalsIgnoreCase("feature_dbxref")) {
        getDbxref(child, (SeqFeature)protFeat, false);  // false==not primary dbxref
      }
      else if (child.getType().equalsIgnoreCase("residues")) {
        seq.setResidues(child.getCharData());
        seq.setResidueType(SequenceI.AA);
      }
      else if (child.getType().equalsIgnoreCase("name")) {
        seq.setName(child.getCharData());
      }
      else if (child.getType().equalsIgnoreCase("md5checksum")) {
        String checksum = child.getCharData();
        if (checksum != null && !checksum.equals(""))
          seq.setChecksum (checksum);
      }
      else if (child.getType().equalsIgnoreCase("featureloc")) {
        // Handle featureloc here (rather than calling handleFeatureloc())
        // because we need to use setRange rather than setStart and setEnd
        // (as with results)
        Vector fchildren = child.getChildren();
        int start = -1, end = -1, strand = 1;
        for (int j = 0; j < fchildren.size(); j++) {
          XMLElement fchild = (XMLElement) fchildren.elementAt(j);
          try {
            if (fchild.getType().equalsIgnoreCase("fmin")) {
              // need to add 1 to start.  chado is interbase - apollo is base oriented.
              try {
                start = Integer.parseInt(fchild.getCharData()) + 1;
              } catch (Exception e) {
                logger.error("Couldn't parse integer from fmin " + child.getCharData(), e);
              }
            }
            else if (fchild.getType().equalsIgnoreCase("fmax")) {
              try {
                end = Integer.parseInt(fchild.getCharData());
              } catch (Exception e) {
                logger.error("Couldn't parse integer from fmax " + child.getCharData(), e);
              }
            }
            else if (fchild.getType().equalsIgnoreCase("strand")) {
              try {
                strand = Integer.parseInt(fchild.getCharData());
              } catch (Exception e) {
                logger.error("Couldn't parse integer from strand " + child.getCharData(), e);
              }
            }
            else if (fchild.getType().equalsIgnoreCase("is_fmin_partial") ||
                     fchild.getType().equalsIgnoreCase("is_fmax_partial")) {
              protFeat.addProperty(fchild.getType(), fchild.getCharData());
            }
          } catch (Exception e) {
            logger.error("Exception handling featureloc " + ChadoXmlUtils.printXMLElement(fchild), e);
          }
        }

        seq.setRange(new Range(start,end));
        //        logger.debug("Set peptide range to " + start + "-" + end + " for " + seq.getName()); // DEL
        transcript.setPeptideSequence(seq);

        curation.addSequence(seq);

        boolean missing5prime = false;
        boolean missing3prime = false;

        if ((protFeat.getProperty("is_fmin_partial").equals("1") && strand == 1) ||
            (protFeat.getProperty("is_fmax_partial").equals("1") && strand == -1))
          missing5prime = true;
        if ((protFeat.getProperty("is_fmax_partial").equals("1") && strand == 1) ||
            (protFeat.getProperty("is_fmin_partial").equals("1") && strand == -1))
          missing3prime = true;

        // Set start/stop of translation.
        // Check if we're missing start codon (missing3prime).
        if (transcript.getProperty("missing_start_codon").equals("true") ||
            missing5prime) {
          transcript.setMissing5prime(true);
          // We can't call setTranslationStartAtFirstCodon() here because
          // we don't have the cDNA sequence yet--need to call after we're done
	  // reading the transcript.
//	  logger.debug("Calling setTranslationStartAtFirstCodon() on " + transcript.getId(); // DEL
//	  transcript.setTranslationStartAtFirstCodon();
          logger.debug(seq.getName() + " has now been marked as missing start codon");
        }
        else if (!missing3prime) {
          boolean foundTranslationStart = false;
          // I'm surprised we need to worry about strand here--in most cases,
          // with ChadoXML, we don't.
          if (strand == 1)
            // false means don't set end from start
            foundTranslationStart = transcript.setTranslationStart(start, false);
          else
            foundTranslationStart = transcript.setTranslationStart(end, false);

          // Can we really call setTranslationStartAtFirstCodon() here?
          // We haven't read the cDNA yet, just the peptide.
          if (!foundTranslationStart) {
            logger.warn("couldn't set translation start to " + (strand == 1 ? start : end)  + " for transcript " + transcript.getName());
            //            transcript.setTranslationStartAtFirstCodon(); 
          }
          // I don't think this works right.
          else if (transcript.isTransSpliced()) {
            logger.debug("Dealing with trans-spliced transcript " + transcript.getName());
            transcript.sortTransSpliced();
          }
        }

	// Set translation end
        if (transcript.getProperty("missing_stop_codon").equals("true") ||
            missing3prime) {
          logger.debug(seq.getName() + " is marked as missing stop codon.");
          transcript.setMissing3prime(true);
        }
        else {
          // We have to add 1 to match how Apollo does it in FeatureSet.
          // 1 will be subtracted again when it's written out.
          // Yuck.
          if (strand == 1) {
            transcript.setTranslationEnd(end+1);
          }
          else {
            transcript.setTranslationEnd(start-1);
          }
        }
      }

      else if (child.getType().equalsIgnoreCase("feature_synonym")) {
        Synonym synonym = getSynonym(child);
        //        // Can't attach synonyms to a sequence.  Mark says it's ok if we don't save protein synonyms.
        //        // (The writer will print out the autosynonym of the protein's own accession number.)
        //        if (!synonym.getName().equals(seq.getAccessionNo()))
        //          logger.warn("can't add synonym " + synonym + " to peptide " + seq.getAccessionNo());
        protFeat.addSynonym(synonym);
      }
      else if (child.getType().equalsIgnoreCase("organism_id")) {
        getOrganism(child, seq);
      }
      // If it's a flat field, e.g. <timelastmodified>2004-10-08 16:12:11.477648</timelastmodified>
      // then attach it as a property
      else {
        if (!child.getCharData().equals("")) {
          addField(protFeat, child);
        }
      }
    }
  }

  /** Some features are unstranded--put on the best-guess strand, add
    * property indicating that. */
  private void forceStrandIfNeeded(AnnotatedFeatureI annot) {
    if (annot.getStrand() == 0) {
      annot.addProperty("unstranded", "true");
      int strand = 1;
      if (annot.getStart() > annot.getEnd()) {
        strand = -1;
        // Unfortunately, these unstranded features break the normal rule in Chado
        // that fmax > fmin, so now we have to swap them back.
        int temp = annot.getStart();
        annot.setStart(annot.getEnd());
        annot.setEnd(temp);
        logger.info("Had to swap start and end for unstranded feature " + annot.getName() + " because start>end");
      }
        
      annot.setStrand(strand);
      logger.info("Annot " + annot.getName() + " (" + annot.getId() + ") is unstranded--showing on " + strand + " strand.  start = " + annot.getStart() + ", end = " + annot.getEnd());
    }
  }

  /*  <feature_synonym>
   *    <is_current>1</is_current>
   *    <is_internal>0</is_internal>
   *    <pub_id> [etc]
   *    <synonym_id>
   *      <synonym>
   *        <name>CG10833-PA</name>
   *        <synonym_sgml>CG10833-PA</synonym_sgml>
   * Creates a Synonym data structure and returns it. */
  private Synonym getSynonym(XMLElement xml) {
    Synonym syn = new Synonym();
    Vector children = xml.getChildren();
    for (int i = 0; i < children.size(); i++) {
      XMLElement child = (XMLElement) children.elementAt(i);
      if (child.getType().equalsIgnoreCase("synonym_id")) {
        XMLElement nameElement = getGrandchild(child, "name");
        String name = nameElement.getCharData();
        name = HTMLUtil.replaceSGMLWithGreekLetter(name);
        syn.setName(name);

        // Add a synonym_sgml property to every synonym.
        // If synonym_sgml is not included in the synonym record, just use the synonym name itself.
        XMLElement sgml = getGrandchild(child, "synonym_sgml");
        if (sgml != null)
          syn.addProperty("synonym_sgml", sgml.getCharData());
        else
          syn.addProperty("synonym_sgml", name);
      }
      else if (child.getType().equalsIgnoreCase("pub_id")) {
        //        syn.addProperty("author", getSynonymAuthor(child));
        syn.setOwner(getSynonymAuthor(child));
        String pubType = getPubType(child);
        if (pubType != null)
          syn.addProperty("pub_type", pubType);
      }
      else if (child.getCharData() != null && !(child.getCharData().equals("")))
        syn.addProperty(child.getType(), child.getCharData());
    }
    return syn;
  }

  /** No longer used */
//   private boolean synonymIsInternal(XMLElement xml) {
//     XMLElement internal = getChild(xml, "is_internal");
//     if (internal != null)
//       return (internal.getCharData().equals("1"));
//     else
//       return false;
//   }

  /** No longer used */
//   private boolean synonymIsCurrent(XMLElement xml) {
//     XMLElement current = getChild(xml, "is_current");
//     if (current != null)
//       return (current.getCharData().equals("1"));
//     else
//       return true;  // default is 1 (true)
//   }

  /* <featureprop>  <rank>  <type_id> ...<name>property type</name>  ... <name>sp_status</name>
   * </cvterm> </type_id> <value>Perfect match to SwissProt real</value>  </featureprop>
   * Certain special properties (e.g. "owner") are dealt with specially. */
  private String getProperty(XMLElement xml, SeqFeatureI feature) {
    XMLElement type_id = getChild(xml, "type_id");
    String val = null;
    if (type_id == null) {
      logger.debug("getProperty: couldn't find grandchild type_id of node " + ChadoXmlUtils.printXMLElement(xml));
      return null;
    }

    String prop = getDataType(type_id);

    XMLElement value = getChild(xml, "value");
    if (value == null)
      return null;
    val = value.getCharData();

    // If it's a special property, handle it separately
    if (handleSpecialProp(xml, prop, val, feature))
      return val;

    // If not a special property, just save as a property
    if (feature != null) {
      feature.addProperty(prop, val);
      //      logger.debug("Added property " + prop + "=" + val + " to " + feature.getName()); // DEL
    }
    return val;
  }

  /** Certain special properties (e.g. "comment", "owner") are dealt with specially
   *  rather than being saved as generic properties. 
   *  Returns true if the property is indeed special (and thus is dealt with here). */
  private boolean handleSpecialProp(XMLElement xml, String prop, String val, SeqFeatureI feature) {
	  
	// If the XML element is empty for featureprop/value, val will be null so shouldn't do any string
	// manipulations on it - just return false
	if (val == null) {
		logger.info("empty <value> for <featureprop> - skipping this featureprop");
		return false;
	}
	  
    // Comments are represented as featureprops with a featureprop_pub that encodes
    // (wordily, of course) the curator's name
    if (prop.equalsIgnoreCase("comment")) {
      if (!(feature instanceof AnnotatedFeatureI)) {
        logger.error("Can't add comment " + val + " to non-annotation feature " + feature);
        feature.addProperty(prop, val);
        return true;
      }

      Comment comment = new Comment();
      comment.setText(val);
      if (val.indexOf("nternal view only") > 0)
        comment.setIsInternal(true);
      String curator = getCurator(getChild(xml, "featureprop_pub"));
      comment.setPerson(curator);
      // Date is encoded in value
      // <value>short CDS provisional (internal view only)::DATE:2003-12-18 12:59:04::TS:1071770344229</value>
      if (val.indexOf("TS") > 0) {
        String timestring = val.substring(val.indexOf("TS")+3);
        try {
          long time = Long.parseLong(timestring);
          comment.setTimeStamp(time);
        }
        catch (NumberFormatException e) {
          logger.warn("error parsing timestamp " + timestring + " from comment " + val);
        }
        //        logger.debug("Added comment " + val + " (author " + curator + ") to " + feature.getId()); // DEL
      }
      ((AnnotatedFeatureI)feature).addComment(comment);
      return true;
    }
    else if (prop.equalsIgnoreCase("owner")) {
      if (feature instanceof AnnotatedFeatureI) {
        ((AnnotatedFeatureI)feature).setOwner(val);
        return true;
      }
    }
    // Problematic annotation (val should be boolean)
    else if (prop.equalsIgnoreCase("problem") && (feature instanceof AnnotatedFeatureI)) {
      if (val.equals("true") || val.equals("t"))
        ((AnnotatedFeatureI)feature).setIsProblematic(true);
      else
        logger.warn("non-boolean value for problem for annotation or transcript " + feature.getName() + "--can't save it.");
      // Don't return true because we also want to save it as a generic property
      // 1/2006: Why?  We've already saved it as an Apollo property.
      return true;
    }
    // A "problem" property on a result is a ResultTag ("tag") inside Apollo
    else if (prop.equalsIgnoreCase("problem")) {
      ((SeqFeature)feature).addProperty("tag", val);
      return true;
    }
    else if (prop.equalsIgnoreCase("description")) {
      addDescription(feature, val);
      // Don't return true because we also want to save it as a property
    }
    // non_canonical_start_codon ignored in input--calculated by Apollo
    else if (prop.equalsIgnoreCase("non_canonical_start_codon") &&
             (feature instanceof Transcript)) {
      logger.warn("not yet doing anything with non_canonical_start_codon property--\nnormally this is derived by Apollo.");
      return true;  // new as of 1/3/2006
    }
    else if (prop.equalsIgnoreCase("non_canonical_splice_site") &&
             (feature instanceof Transcript)) {
      logger.info("Marking non_canonical_splice_site " + 
                  (val.equalsIgnoreCase("approved") ? "approved" : "unapproved") +
                  " for transcript " + feature.getName());
      ((Transcript) feature).nonConsensusSplicingOkay(val.equalsIgnoreCase("approved"));
      return true;  // new as of 1/3/2006
    }
    else if ((prop.equalsIgnoreCase("plus_1_translational_frame_shift") ||
              prop.equalsIgnoreCase("plus1_translational_frame_shift") ||
              prop.equalsIgnoreCase("plus_1_translational_frameshift")) &&
             (feature instanceof Transcript)) {
      try {
        int plus1_frameshift = Integer.parseInt(val);
        logger.info("Marking plus_1_translational_frameshift = " + plus1_frameshift + " for transcript " + feature.getName());
        ((Transcript)feature).setPlus1FrameShiftPosition(plus1_frameshift);
      }
      catch (Error e) {
        logger.error("Couldn't parse plus_1_translational_frameshift value " + val + "--not an integer", e);
      }
      return true;
    }
    else if ((prop.equalsIgnoreCase("minus_1_translational_frame_shift") ||
              prop.equalsIgnoreCase("minus1_translational_frame_shift") ||
              prop.equalsIgnoreCase("minus_1_translational_frameshift")) &&
             (feature instanceof Transcript)) {
      try {
        int minus1_frameshift = Integer.parseInt(val);
        logger.info("Marking minus_1_translational_frameshift = " + minus1_frameshift + " for transcript " + feature.getName());
        ((Transcript)feature).setMinus1FrameShiftPosition(minus1_frameshift);
      }
      catch (Exception e) {
        logger.error("Couldn't parse minus_1_translational_frameshift value " + val + "--not an integer", e);
      }
      return true;
    }
    // This is what used to be called readthrough_stop_codon; now stop_codon_readthrough is a more
    // general property of which stop_codon_redefinition_as_selenocysteine is just one
    // possible case.
    else if (prop.equalsIgnoreCase("stop_codon_redefinition_as_selenocysteine") &&
             (feature instanceof Transcript)) {
      boolean seleno = (val.toLowerCase().startsWith("t") ||
                        val.equalsIgnoreCase("U")); // Maybe they decided to stick the selenocysteine residue in there
      if (seleno) {
        logger.info("Got stop_codon_redefinition_as_selenocysteine for transcript " + feature.getName());
        ((Transcript)feature).setReadThroughStop("U");
        return true;
      }
    }
    // The more generic version of stop_codon_redefinition_as_selenocysteine can have
    // as its value either true/false or a specific residue.
    else if ((feature instanceof Transcript) &&
             ((prop.equalsIgnoreCase("stop_codon_readthrough") ||
               // readthrough_stop_codon is obsolete (but still accepted) version of this property
               prop.equalsIgnoreCase("readthrough_stop_codon")))) {  
      logger.info("Got stop_codon_readthrough = " + val + " for transcript " + feature.getName());
      ((Transcript)feature).setReadThroughStop(val);
      //      feature.addProperty(prop, val);  // Why?
      return true;
    }
    // Deal with missing_start_codon
    // (do it here rather than in addPeptide because these properties
    // come AFTER the peptide record).
    // Missing start codons also can be indicated by is_fmin_partial=1, but that
    // appears in the featureloc, not as a property.
    else if ((feature instanceof Transcript) &&
             (prop.equalsIgnoreCase("missing_start_codon") && val.equalsIgnoreCase("true"))) {
      logger.info("Marking missing_start_codon for transcript " + feature.getName());
      ((Transcript)feature).calcTranslationStartForLongestPeptide();
      return true;
    }
    else if ((feature instanceof Transcript) &&
             (prop.equalsIgnoreCase("missing_stop_codon") && val.equalsIgnoreCase("true"))) {
      logger.info("Marking missing_stop_codon for transcript " + feature.getName());
      ((Transcript)feature).setMissing3prime(true);
      return true;  // new as of 1/3/2006
    }

    return false;
  }

  /** Descriptions are actually added to the ref sequence of the feature. */
  private void addDescription(SeqFeatureI feature, String description) {
    // Add the description to feature's ref seq
    SequenceI seq = feature.getRefSequence();
    if (seq == null) {  // Shouldn't have to do this once we start saving hit sequences
      seq = new Sequence(feature.getName(), "");
      //      logger.debug("Created new ref sequence for  " + feature.getId() + " with no residues to capture description"); // DEL
      feature.setRefSequence(seq);
    }
    // setSeqDescription also extracts the date, if any, from the description
    // and calls seq.setDate on it.
    GAMEAdapter.setSeqDescription(seq, description, feature.getName());
  }
    
  /** <featureprop_pub>  <pub_id>  <pub>  <type_id> pub type = curator </type_id>
   *  <uniquename>curatorname</uniquename> */
  private String getCurator(XMLElement xml) {
    XMLElement pub = getGrandchild(xml, 0, null);
    if (pub == null)
      return null;
    // Ignore type_id--it just says "curator"
    XMLElement curator = getChild(pub, "uniquename");
    if (curator == null)
      return null;
    return(curator.getCharData());
  }

  /** <pub_id>  <pub>  <type_id> </type_id>
   *  <uniquename>author</uniquename> */
  private String getSynonymAuthor(XMLElement xml) {
    XMLElement pub = getChild(xml, 0, null);
    if (pub == null)
      return null;
    // Ignore type_id--it just says "curator"
    XMLElement curator = getChild(pub, "uniquename");
    if (curator == null)
      return null;
    return(curator.getCharData());
  }

  /**   <pub_id>
   *         <pub>
   *           <is_obsolete>0</is_obsolete>
   *           <type_id>
   *             <cvterm>
   *               <cv_id>
   *                 <cv>
   *                   <name>pub type</name>
   *                 </cv>
   *               </cv_id>
   *               <is_obsolete>0</is_obsolete>
   *               <is_relationshiptype>0</is_relationshiptype>
   *               <name>publication</name>
   *             </cvterm>
   *           </type_id>
   *           <uniquename>FBrf0132177</uniquename>
   *         </pub>
   *       </pub_id>
   * The thing we're looking for here is <name>publication</name> */
  private String getPubType(XMLElement xml) {
    XMLElement pub = getChild(xml, 0, null);
    if (pub == null)
      return null;
    XMLElement type = getChild(pub, "type_id");
    if (type == null)
      return null;
    // gets the <name> element out of the type_id record
    return (getDataType(type));
  }

  /**    <organism_id>
   *         <organism>
   *         <genus>Drosophila</genus>
   *         <species>melanogaster</species>
   *  For now, save as a property (organism="Drosophila melanogaster"), since
   *  features don't have an Organism field. */
  private void getOrganism(XMLElement xml, SeqFeatureI feat) {
    XMLElement genus = getGrandchild(xml, "genus");
    XMLElement species = getGrandchild(xml, "species");
    if (genus != null && species != null) {
      feat.addProperty("organism", genus.getCharData() + " " + species.getCharData());
      //      logger.debug("Added organism prop to " + feat.getName() + ": " + genus.getCharData() + " " + species.getCharData()); // DEL
    }
  }

  /** Sequences DO have an organism field */
  private void getOrganism(XMLElement xml, SequenceI seq) {
    XMLElement genus = getGrandchild(xml, "genus");
    XMLElement species = getGrandchild(xml, "species");
    if (genus != null && species != null) {
      seq.setOrganism(genus.getCharData() + " " + species.getCharData());
    }
  }

  /** Get the rank from a featureprop record.
   *      <featureprop>
   *        <rank>0</rank>
   *        <type_id> 
   *  Returns the rank as a short (default is 0 if it can't be parsed). 
   * 12/2005: No longer used (not roundtripping featureprop ranks). */
//   private short getRank(XMLElement xml) {
//     XMLElement rank = getChild(xml, "rank");
//     if (rank == null) {
//       logger.warn("couldn't get rank for featureprop " + ChadoXmlUtils.printXMLElement(xml));
//       return 0;
//     }
//     try {
//       return Short.parseShort(rank.getCharData());
//     } catch (Exception e) {
//       logger.error("Exception parsing rank " + ChadoXmlUtils.printXMLElement(rank), e);
//       return 0;
//     }
//   }

  /** For parsing children (SeqFeatures) of a computational result.
   *  These SeqFeatures are analagous to exons.
   *
   *  xml node passed in is feature_relationship.
   *      <feature_relationship>
   *      <subject_id>
   *      <feature>
   *      <uniquename>:7323967</uniquename>
   *      <analysisfeature>
   *      <feature_relationship>
   *      <featureloc> */
  private SeqFeatureI getSeqFeature(XMLElement xml, SeqFeatureI result,
                                    CurationSet curation) {
    // This SeqFeature may be a singleton (SeqFeature) or a pair (FeaturePair).
    // Pairs have two featurelocs.
    // If feature turns out to be a pair, feature will be created from query and subject.
    // Otherwise, it will just be set to query.
    SeqFeature feature1 = new SeqFeature();
    feature1.addProperty("is_analysis", "1");
    boolean sawFeatureLoc = false;
    SeqFeature feature2 = null;  // Create it later if nedeed
    SeqFeature current_feature = feature1;

    Vector children = xml.getChildren();
    for (int i = 0; i < children.size(); i++) {
      XMLElement child = (XMLElement) children.elementAt(i);
      if (child.getType().equalsIgnoreCase("subject_id")) {
        Vector grandchildren = child.getChildren();
        // First grandchild should be <feature>
        if (grandchildren == null) {
          logger.error("getSeqFeature: no grandchildren for " + ChadoXmlUtils.printXMLElement(xml));
          return feature1;
        }

        // First (only?) child of <subject_id> should be <feature>
        XMLElement xml_feat = (XMLElement) grandchildren.firstElement();
        if (!xml_feat.getType().equalsIgnoreCase("feature")) {
          logger.error("Wrong child " + ChadoXmlUtils.printXMLElement(xml_feat) + " of subject_id " + ChadoXmlUtils.printXMLElement(child));
          return feature1; // Return it even though we got nowhere?
        }

        Vector feat_parts = xml_feat.getChildren();
        for (int j = 0; j < feat_parts.size(); j++) {
          XMLElement tp = (XMLElement) feat_parts.elementAt(j);
          if (tp.getType().equalsIgnoreCase("organism_id")) {
            getOrganism(tp, current_feature);
          }
          if (tp.getType().equalsIgnoreCase("type_id")) {
            String type_id = getDataType(child);
            current_feature.addProperty("type_id", type_id);
          }
          // Haven't see dbxref_id appear here.
          else if (tp.getType().equalsIgnoreCase("dbxref_id")) {
            // dbxref_id by itself is the ID/accession of this feature
            //            logger.debug("Got dbxref_id record. feature1 = " + feature1 + ", feature2 = " + feature2 + ", current_feature = " + current_feature + ", current_feature id = " + current_feature.getId()); // DEL
            if (current_feature.getId() == null) {
              DbXref xref = getDbxref(tp, current_feature, false);
              if (xref != null)
                current_feature.setId(xref.getIdValue());
            }
          }
          else if (tp.getType().equalsIgnoreCase("uniquename")) {
            current_feature.setId(tp.getCharData());
            //            logger.debug("Uniquename for " + ((current_feature == feature1) ? "subject" : "query") + " is now " + current_feature.getId()); // DEL
          }
          else if (tp.getType().equalsIgnoreCase("residue_info")) {
            String residues = tp.getCharData();
            current_feature.setExplicitAlignment(residues);
          }
          else if (tp.getType().equalsIgnoreCase("analysisfeature")) {
            // Just get the score--the rest of the stuff in analysisfeature can be ignored
            // because it is a copy of the parent's
            getScore(tp, current_feature);
            //            logger.debug("Got score " + current_feature.getScore() + " for current_feature " + current_feature.getId()); // DEL
          }
          // If this feature is a pair, there will be TWO featurelocs.
          // If we already have a subject, this is now the query.
          else if (tp.getType().equalsIgnoreCase("featureloc")) {
            if (!sawFeatureLoc) {
              sawFeatureLoc = true;
            }
            else  { // already have one featureloc--this is the other
              feature2 = new SeqFeature();
              //              logger.debug("\nFinished reading subject " + current_feature.getId() + " (strand = " + current_feature.getStrand() + ")"); // DEL
              feature2.addProperty("is_analysis", "1");
              current_feature = feature2;
            }
            handleFeatureLoc(tp, current_feature, curation);
            //            logger.debug("Got featureloc for " + current_feature.getId() + ", start = " + current_feature.getStart() + ", strand = " + current_feature.getStrand()); // DEL
          }
          // Otherwise, save it as a property
          else {
            current_feature.addProperty(child.getType(), child.getCharData());
          }
        }
      }
    }

    // Pass type down from result parent
    feature1.setFeatureType(result.getFeatureType());

    if (feature2 == null) {
      //      logger.debug("NOT a feature pair: " + feature1.getName());  // DEL
      //      logger.debug(result.getName() + " is not a feature pair--seting name for " + feature1.getId() + " to " + result.getName() + " (was " + feature1.getName() + ")"); // DEL
      feature1.setName(result.getName());  // Do here?
      return feature1;
    }

    // Decide whether feature1 or feature2 is the query
    // (usually it's feature2).
    SeqFeature query = feature2;
    SeqFeature subject = feature1;

    // Subject has rank=1 and query has rank=0.
    // See if they need to be switched
    if (feature2.getProperty("rank").equals("1")) {
      //      logger.debug("Query ("+ feature1.getName() + ") came before subject (" + feature2.getName() + "); switching"); // DEL
      query = feature2;
      subject = feature1;
    }
    // Set bioType too??

    // Add the subject sequence (if any)
    if (subject.getRefSequence() != null)
      curation.addSequence(subject.getRefSequence());

    // Copy some fields from subject to query.  (Any from query to subject?)
    query.setFeatureType(subject.getFeatureType());
    query.setScore(subject.getScore());

    //      logger.debug("Result is a feature pair: " + query.getName()); // DEL
    FeaturePair pair = new FeaturePair(query, subject);
    // Alignment of pair is query alignment (why?)
    pair.setExplicitAlignment(query.getExplicitAlignment());
    pair.setName(subject.getName());
    pair.setId(subject.getId());
    //      pair.setRefSequence(subject.getRefSequence()); // ?
    // Copy all relevant featureprops from subject to pair 
    copyProperties(subject, pair);

    // tags must be added to RESULT, not just to feature pair
    // Note that the tag would have appeared in the ChadoXML for the
    // feature pair as a "problem" featureprop, which was stored by
    // Apollo as a "tag" property on the feature pair.  It needs to be
    // passed down to the query and subject in order for the tagged
    // result to appear crosshatched in the display.
    if (!subject.getProperty("tag").equals("")) {
//      logger.debug("Subject seq " + subject.getName() + " has tag=" + subject.getProperty("tag"));
      result.addProperty("tag", subject.getProperty("tag"));
    }

    return pair;
  }

  /** Extract analysis type fields (program, etc.) and set in result.
   *    Note that because Apollo's datamodels don't explicitly support these fields
   *    at the result set level (some of them live at the Analysis level), we will
   *    just save them all as properties.
   *      <analysisfeature>
   *      <analysis_id>
   *      <analysis>
   *      <program>sim4</program>
   *      <programversion>1.0</programversion>
   *      <sourcename>na_dbEST.same.dmel</sourcename>
   *      <sourceversion>1.0</sourceversion>
   *      <timeexecuted>2004-07-15 20:17:40</timeexecuted> 
   *      </analysis>
   *      </analysis_id>
   *      Result spans also have a <rawscore> field here (result sets don't).
   *      <rawscore>-0.77</rawscore>
  */
  private void handleAnalysisType(XMLElement xml, FeatureSetI result) {
    XMLElement analysis = getGrandchild(xml, 0, null);
    if (analysis == null) {
      result.setFeatureType("unknown_type");
      return;
    }

    String program = "";
    String db = "";
    Vector fields = analysis.getChildren();
    for (int i = 0; i < fields.size(); i++) {
      XMLElement field = (XMLElement) fields.elementAt(i);
      String name = field.getType();
      String value = field.getCharData();
      //      logger.debug("Stored property " + name + " = " + value + " for result " + result);
      result.addProperty(name, value);
      // A few of these do need to be in results
      if (name.equalsIgnoreCase("program")) {
        program = value;
        result.setProgramName(program);
      }
      else if (name.equalsIgnoreCase("sourcename")) {
        db = value;
        result.setDatabase(db);
      }
    }

    result.setFeatureType(constructAnalysisType(program, db));
  }

  /** Build Apollo-style analysis type name from program and sourcename.
   *  (From GAMEAdapter (called getAnalysisType there).) */
  private String constructAnalysisType(String prog, String db) {
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

  /** Result spans have a <rawscore> field under analysisfeature (result sets don't).
   *  We can ignore the rest of the stuff in analysisfeature because it's a duplicate
   *  of the parent result set's.
   *      <analysisfeature>
   *      <analysis_id>
   *      <analysis>
   *      <program>sim4</program>
   *      [etc]
   *      </analysis>
   *      </analysis_id>
   *      <rawscore>-0.77</rawscore>  */
  private void getScore(XMLElement xml, SeqFeatureI result) {
    // Get score--child of analysisfeature (only for result spans, not result sets)
    XMLElement xml_score = getChild(xml, "rawscore");
    if (xml_score != null) {
      try {
        double score = Double.parseDouble(xml_score.getCharData());
        result.setScore(score);
        //        logger.debug("Saved score " + score + " for result " + result.getId()); // DEL
      } catch (Exception e) {
        logger.error("Exception parsing score " + ChadoXmlUtils.printXMLElement(xml_score), e);
      }
    }
  }

  /** Create forward and reverse strand feature sets for analysis of
   *  analysis element, which are added to analyses. Add all sub
   *  features to the appropriate stranded FeatureSet.  (Adapted from
   *  GAMEAdapter.)
   *
   *  Note that some of the fields that appear at the Result Set level
   *  in Chado XML (e.g. program, sourcename (i.e. database)) live at
   *  the Analysis level in the Apollo datamodels, so we copy them to
   *  the Analysis, but this assumes that that all the results of the
   *  same analysis type (program+db) in this file will have the same
   *  other fields (e.g. programversion) as well--that we won't have
   *  (in the same file) some results representing blastx against
   *  aa_SPTR of dec 2003 and some representing blastx against aa_SPTR
   *  of june 2004, for example.  This should be a harmless assumption
   *  because it will only affect the output when we're using Apollo to
   *  convert from Chado XML to GAME XML. */
  private void addResultToAnalysis(FeatureSetI result) {
    //    String type = null;
    // handleAnalysisType stored all the <analysisfeature> fields as properties
    // of this result.
    // (Actually, prog and db are also stored explicitly, but no harm in getting them the general way)
    String prog = result.getProperty("program");
    String db = result.getProperty("sourcename");
    String date = result.getProperty("timeexecuted");
    String programversion = result.getProperty("programversion");
    String sourceversion = result.getProperty("sourceversion");

    // Find or create FeatureSet for this analysis type (existing one will be
    // returned if found)
    FeatureSetI forward_analysis = 
      // 1 is forward strand
      initAnalysis (analyses, 1, prog, db, date, programversion, sourceversion, all_analyses);

    FeatureSetI reverse_analysis = 
      initAnalysis (analyses, -1, prog, db, date, programversion, sourceversion, all_analyses);

    if (result.getStrand() == 1) {
      forward_analysis.addFeature(result);
      //      logger.debug ("Added result " + result + " to forward analysis " + forward_analysis.getName() + ", which has " + forward_analysis.size() + " features"); // DEL
      if (!forward_analysis.hasFeatureType())
        forward_analysis.setFeatureType(result.getFeatureType());
    } else {
      //      logger.debug ("Added result " + result + " to reverse analysis " + reverse_analysis.getName() + ", which has " + reverse_analysis.size() + " features"); // DEL
      reverse_analysis.addFeature(result);
      if (!reverse_analysis.hasFeatureType()) {
        //        logger.debug ("For result " + result + ", setting analysis type to " + result.getType());  // DEL
        reverse_analysis.setFeatureType(result.getFeatureType());
      }
    }

    // Add in stranded analyses only if features were found on that strand;
    // otherwise it's scrapped (not added to all_analyses and analyses)
    boolean fwd = addAnalysisIfHasFeatures(forward_analysis,all_analyses,analyses);
    boolean rev = addAnalysisIfHasFeatures(reverse_analysis,all_analyses,analyses);
    // If we have analysis for both strands, set pointers to analysis "twin" on 
    // opposite strand (this is needed later for results that need to be moved to the
    // opposite strand).
    if (fwd && rev) {
      forward_analysis.setAnalogousOppositeStrandFeature(reverse_analysis);
      reverse_analysis.setAnalogousOppositeStrandFeature(forward_analysis);
    }
  }

  /** Creates a new FeatureSet for the analysis on strand. If an analysis with prog,db, 
   *  and strand has already been created, the existing one is returned. If new a
   *  FeatureSet is created, added to all_analyses hash, added to analyses, and returned. 
   *  (Adapted from GAMEAdapter.) 
   *  Note that many of the arguments are not actually used. */
  private FeatureSetI initAnalysis (StrandedFeatureSetI analyses,
                                    int strand,
                                    String prog,
                                    String db,
                                    String date,
                                    String programversion,
                                    String sourceversion,
                                    Hashtable all_analyses) {
    String analysis_type = constructAnalysisType(prog, db);
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
      //      analysis.setId (analysis_id);
      analysis.setProgramName(prog);
      analysis.setDatabase(db);
      analysis.setFeatureType (analysis_type);
      if (!analysis_type.equals(RangeI.NO_TYPE))
        analysis.setName (analysis_name);
      analysis.setStrand (strand);
      //      logger.debug("Initialized analysis type " + analysis_name); // DEL
      // analysis.setHolder (true);
      //       if (type != null)
      //         analysis.setBioType (type);
      //       if (date != null)
      //         analysis.addProperty ("date", date);
      //       if (version != null)
      //         analysis.addProperty ("version", version);
      // Analysis is added later on, but only if it has features
    }
    return analysis;
  }

  /** Add in stranded analyses only if features were found on that strand;
   *  otherwise it's scrapped (not added to all_analyses and analyses) 
   *  and false is returned */
  private boolean addAnalysisIfHasFeatures(FeatureSetI analysis,
                                           Hashtable all_analyses,
                                           StrandedFeatureSetI analyses) {
    if (all_analyses == null) {
      //      logger.debug("addAnalysisIfHasFeatures: analysis hash was null!");
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

  private static void copyProperties(SeqFeatureI from, SeqFeatureI to) {
    Hashtable props = from.getProperties();
    Enumeration e = props.keys();
    while (e.hasMoreElements()) {
      String type = (String) e.nextElement();
      if (dontCopyProperty(type))
        continue;
      Vector values = ((SeqFeature)from).getPropertyMulti(type);
      if (values == null)
        continue;
      for (int i = 0; i < values.size(); i++) {
        String value = (String) values.elementAt(i);
        to.addProperty(type, value);
      }
    }
  }

  /** Some properties shouldn't be copied from subject to feature pair, e.g. rank.
   *  (otherwise it gets rank=1 from the subject) */
  private static boolean dontCopyProperty(String prop) {
    if (ChadoXmlWrite.isSpecialProperty(prop))
      return true;
    else
      return false;
  }


  /** General-purpose methods for finding the desired child or grandchild. */

  /* Return child with the given elementType */
  private XMLElement getChild(XMLElement xml, String elementType) {
    return getChild(xml, -1, elementType);
  }

  /* Return numth child, or, if num is -1, look for child of type elementType */
  private XMLElement getChild(XMLElement xml, int num, String elementType) {
    if (xml == null)
      return null;
    Vector children = xml.getChildren();
    if (children == null || children.size() <= num)
      return null;

    if (num < 0 && elementType != null) { // No num specified--search for the right greatgrandchild
      for (int i = 0; i < children.size(); i++) {
        XMLElement child = (XMLElement) children.elementAt(i);
        if (child.getType().equalsIgnoreCase(elementType))
          return child;
      }
      return null;
    }

    XMLElement child = (XMLElement) children.elementAt(num);
    if (elementType != null && !(child.getType().equalsIgnoreCase(elementType)))
      return null;

    return child;
  }

  /** Return desired child of FIRST child of xml. */
  private XMLElement getGrandchild(XMLElement xml, String elementType) {
    return getGrandchild(xml, -1, elementType);
  }

  /** Return numth child of FIRST child of xml,
   *  or, if num is -1, look for appropriate child of FIRST child of xml. */
  private XMLElement getGrandchild(XMLElement xml, int num, String elementType) {
    XMLElement child = getChild(xml, 0, null);
    if (child == null)
      return null;
    Vector grandchildren = child.getChildren();
    if (grandchildren == null || grandchildren.size() <= num) {
      logger.error("getGrandchild: failed to find children of child " + ChadoXmlUtils.printXMLElement(child)); // DEL
      return null;
    }

    if (num < 0 && elementType != null) { // No num specified--search for the right grandchild
      for (int i = 0; i < grandchildren.size(); i++) {
        XMLElement grandchild = (XMLElement) grandchildren.elementAt(i);
        if (grandchild.getType().equalsIgnoreCase(elementType))
          return grandchild;
      }
      return null;
    }

    XMLElement grandchild = (XMLElement) grandchildren.elementAt(num);
    if (elementType != null && !(grandchild.getType().equalsIgnoreCase(elementType)))
      return null;

    return grandchild;
  }

  /* END OF READING.  BEGINNING OF WRITING. */

  /** Main method for writing ChadoXML file */
  public void commitChanges(CurationSet curation) {
    commitChanges(curation, true, true);  // Default is to save both annots and results
  }

  /** Main method for writing ChadoXML file */
  public void commitChanges(CurationSet curation, boolean saveAnnots, boolean saveResults) {
    // !! This doesn't work for command-line i/o, e.g. apollo -f foo.game -w foo.chado
    //    logger.debug("ChadoXmlAdapter.commitChanges: orig filename was " + getOriginalInputFilename()); // DEL
    String filename = apollo.util.IOUtil.findFile(getInput(), true);
    if (filename == null)
      filename = getInput();
    if (filename == null)
      return;

    // Get any header lines from the original input so they can be saved to the output.
    String msg = "Retrieving preamble from original file... ";
    fireProgressEvent(new ProgressEvent(this, new Double(5.0), msg));
    String preamble = getPreamble(curation.getInputFilename());

    // If file already exists, ask user to confirm that they want to overwrite
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
      }
    }

    setInput(filename);
    msg = "Saving Chado XML to file " + filename + "... ";
    fireProgressEvent(new ProgressEvent(this, new Double(20.0), msg));
    if (ChadoXmlWrite.writeXML(curation,
                               filename,
                               preamble,
                               saveAnnots, saveResults,
                               getNameAdapter(curation),
                               "Apollo version: " + Version.getVersion())) {
      logger.info("Saved Chado XML to " + filename);
      
      // Transactions should be associated with any saved file to avoid asynchronization!
      ChadoXmlWrite.saveTransactions(curation, filename);
    }
    else {
      String message = "Failed to save Chado XML to " + filename;
      logger.error(message);
      JOptionPane.showMessageDialog(null,message,"Warning",JOptionPane.WARNING_MESSAGE);
    }
  }

  private ApolloNameAdapterI getNameAdapter(CurationSet curation) {
    // getNameAdapter requires an annotation.  Just use the first one.
    StrandedFeatureSetI annots = curation.getAnnots();

    // If no annots, return default (GMOD) name adapter
    if (annots == null || annots.size() == 0)
      return new GmodNameAdapter();

    AnnotatedFeatureI annot = (AnnotatedFeatureI) annots.getFeatureAt(0);
    return (getCurationState().getNameAdapter(annot));
  }

  /** Check whether the input appears to be chadoXML by looking for the <chado> line */
  public boolean appearsToBeChadoXML(String filename, BufferedReader in)
    throws ApolloAdapterException {
    // Check up to 5000 lines, in case there's a lot of stuff in the preamble
    // before the <chado> line
    for (int i=0; i < 5000; i++) {
      String line = "";
      try {
        line = in.readLine();
      } catch (Exception e) {
        throw new ApolloAdapterException("Error: ChadoXML file " + filename + 
                                         " is empty.");
      }
      if (line == null)
        break;  // Finished reading the file, didn't find the <chado> line
      if (line.toLowerCase().indexOf("<chado") >= 0)
        return true;
    }
    return false;
  }

  /** Retrieve from filename any lines preceding the <chado> line (except for
   *  the initial <?xml> line) and return as a string. */
  private String getPreamble(String filename) {
    if (filename == null || filename.equals("")) {
      logger.warn("original input filename not set--couldn't retrieve preamble");
      return "";
    }
    logger.debug("Retrieving preamble from " + filename + "...");
    StringBuffer input = new StringBuffer();
    BufferedReader in;
    try {
      if (filename.startsWith("http")) {  // It's really an URL
        URL url = new URL(filename);
        InputStream stream = apollo.util.IOUtil.getStreamFromUrl(url, "URL "+url+" not found");
        in = new BufferedReader(new InputStreamReader(stream));
      }
      else  // Regular file
        in = new BufferedReader(new FileReader(filename));

      // Record original input source
      input.append("<!-- Header lines that follow were preserved from the input source: " + filename + " -->\n");

      String line = "";
      while ((line = in.readLine()) != null) {
        // If we get to the <chado> line, we're done reading the preamble.
        if (line.toLowerCase().indexOf("<chado") >= 0 && line.indexOf("<!--") < 0)
          return input.toString();
        // Or it might have been a GAME file
        if (line.toLowerCase().indexOf("<game>") >= 0 && line.indexOf("<!--") < 0)
          return input.toString();

        // Don't save the <?xml> line
        if (line.indexOf("<?xml") >= 0)
          ;
        else
          input.append(line + "\n");
      }
    } catch (Exception exception){
      logger.warn("failed to retrieve preamble from original input file " + filename);
      return "";
    }
    // If we didn't hit the <chado> line, then the original input wasn't a ChadoXML
    // (or GAME XML) file, so don't try to preserve the preamble
    return "";
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
  
}
