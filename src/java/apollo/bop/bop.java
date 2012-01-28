package apollo.bop;

import apollo.datamodel.*;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.exception.NoOutputException;
import apollo.dataadapter.exception.BopException;
import apollo.dataadapter.gamexml.GAMESave;
import apollo.dataadapter.GFFAdapter;
import apollo.dataadapter.analysis.*;
import apollo.seq.io.*; // might not need this
import apollo.util.IOUtil;
import apollo.analysis.filter.*;
import apollo.util.FastaHeader;

import java.io.*;
import java.util.*;
import java.lang.*;

import org.apache.log4j.*;

public class bop {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(bop.class);

  static Vector arg_list = new Vector(7);
  static {
    arg_list.addElement("db");
    arg_list.addElement("host");
    arg_list.addElement("o");
    arg_list.addElement("f");
    arg_list.addElement("p");
    arg_list.addElement("s");
    arg_list.addElement("debug");
  }

  protected static String [] output_formats = {
    "gameII",
    "gameI",
    "gff"
  };

  /** the objects/values are of type AnalysisInput and
      the keys are the analysis type
  */
  static HashMap datatype2options;
  static Vector parsers;

  static boolean caught_exception;

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------
  
  public static void main(String[] args) {
    /* the optional arguments and their values passed in
       from the command line */
    caught_exception = false;

    HashMap arg_hash = parseArgs (args);

    CurationSet curation = new CurationSet();
    if (arg_hash.get ("s") != null) {
      try {
        FastaFile ff = new FastaFile((String) arg_hash.get("s"),
                                     "File",
                                     curation);
      } catch (IOException e) {
        logger.error("IOException caught reading sequence file " +
                     arg_hash.get("s") + " " + e, e);
        caught_exception = true;
      }
    }
    
    /* now process all of the possible input streams */
    for (int i = 0; i < args.length - 1 && !caught_exception; i += 2) {
      String arg_name = getArgType(args[i]);
      if (arg_name.equals("list")) {
	String listfile = args[i + 1];
	processFileList(curation, arg_hash, listfile);
	logger.info ("Processed complete list");
      }
      else if (arg_name.equals("job")) {
	String job_str = (String) args[i + 1];
	int job_id = Integer.valueOf(job_str).intValue();
	arg_hash.put ("job", job_str);
	processJob(curation, arg_hash, job_id);
      }
      else if (datatype2options.get(arg_name.toLowerCase()) != null) {
	String file_name = args[i + 1];
	processFile (curation, arg_name, file_name, arg_hash);
      }
    }

    if (caught_exception)
      System.exit(1);
    else {
      saveResults (curation, arg_hash);
      System.exit (0);
    }
  }

  private static void processFileList (CurationSet curation,
				       HashMap arg_hash,
				       String list_name) {
    //depending on analysis_file content (file name or job_id)
    //use different getResults
    String list_file = IOUtil.findFile (list_name);
    try {
      InputStream data_stream = new FileInputStream (list_file);
      Reader reader = new BufferedReader(new InputStreamReader(data_stream));
      StreamTokenizer tokens = new StreamTokenizer(reader);
      int tok;
      tokens.eolIsSignificant(true);  // make sure end-of-lines are noticed
      tokens.quoteChar('"');
      tokens.commentChar('#');
      try {
	tok = tokens.nextToken();
	while (tok != tokens.TT_EOF) {
	  // skip blank lines;
	  if (tok != tokens.TT_EOL && tok == tokens.TT_WORD) {
	    String input_type = tokens.sval;
	    if (input_type.equals ("file")) {
	      tokens.nextToken();
	      String data_type = tokens.sval;
	      tokens.nextToken ();
	      String file_name = IOUtil.findFile(tokens.sval);
	      processFile (curation, data_type, file_name, arg_hash);
	    }
	    // throw out anything that is on the rest of the line
	    while (tok != tokens.TT_EOL) {
	      tok = tokens.nextToken();
	    }
	  }
	  tok = tokens.nextToken();	      
	}
      }
      catch( Exception ex ) {
	logger.error(ex.getMessage(), ex);
        caught_exception = true;
      }
    }
    catch( Exception ex ) {
      logger.error(ex.getMessage(), ex);
      caught_exception = true;
    }
  }

  private static void processFile (CurationSet curation,
				   String data_type,
				   String file_name,
				   HashMap arg_hash) {
    AnalysisInput parameters = getAnalysisInput (data_type, true);

    logger.info("Processing " + data_type + " from " + file_name);

    int index = data_type.indexOf(":");
    if (index > 0) {
      parameters.setType(data_type.substring(0, index));
      parameters.setDatabase(data_type.substring(index+1));
    }
    else
      parameters.setType (data_type);
    File af = new File (file_name);
    long modification = af.lastModified();
    Date run_date = new Date (modification);
    try {
      boolean parsed = false;
      for (int i = 0; i < parsers.size() && !parsed; i++) {
	AnalysisParserI parser = (AnalysisParserI) parsers.elementAt (i);
	InputStream input = getInputStream(file_name);
	parsed = processInput (curation,
			       input,
			       run_date,
			       parameters,
			       parser,
			       arg_hash);
	input.close();
      }
      if (!parsed)
        failureError("Unable to find a parser for these data in " +
                     file_name);
    }
    catch (Exception e) {
      logger.error ("Could not process file input for " + file_name, e);
      caught_exception = true;
    }
  }
  
  private static void processJob (CurationSet curation,
				  HashMap arg_hash,
				  int job_id) {
    //depending on analysis_file content (file name or job_id)
    //use different getResults
    String host = (String) arg_hash.get("host");
    String dbname = (String) arg_hash.get("db");
    JDBCPipelineAdapter ad = new JDBCPipelineAdapter(host, dbname);

    JobOutput job_data = null;
    try {
      job_data = ad.retrieveJob(job_id);
      SequenceI seq = job_data.getQuerySequence();
      if (seq != null) {
	curation.setRefSequence (seq);
	curation.setName (seq.getName());
	curation.setStrand(1);
	curation.setStart (1);
	curation.setEnd(seq.getLength());
      }
    } 
    catch (NoOutputException e) {
      logError(e);
      System.exit(2);
    } 
    catch (Exception e) {
      logger.fatal("Error using JDBC getting data: "+e.getMessage(), e);
      System.exit(3);
    }
    String data_type = job_data.getAnalysisType();
    logger.info("Processing a " + data_type + " job");
    AnalysisInput analysis_input = getAnalysisInput(data_type, true);
    analysis_input.setDatabase(job_data.getDatabase());
    try {
      boolean parsed = false;
      for (int i = 0; i < parsers.size() && !parsed; i++) {
	AnalysisParserI parser = (AnalysisParserI) parsers.elementAt (i);
	InputStream input 
	  = new ByteArrayInputStream(job_data.getRawOutput().getBytes());
	parsed = processInput (curation,
			       input,
			       job_data.getRunDate(), 
			       analysis_input,
			       parser,
			       arg_hash);
	input.close();
      }
      if (!parsed)
        failureError("Unable to find a parser for these data from job " +
                     job_id);
    }
    catch (Exception e) {
      logger.error ("Could not process job input for " + job_id, e);
      caught_exception = true;
    }
  }

  private static HashMap parseArgs (String [] args) {
    /** 
	collects from the input line the optional parameters
	1. -db database name
	2. -host database server
	3. -p a preferences file for filtering otherwise the 
	default is bop.prefs file
	4. -o the name of the output file
	default is sequencename.xml in local directory
	5. -f the format that the output should take
        6. -s a file containing the fasta sequence of the query
    */
    if ((args.length % 2) != 0) {
      if (args.length == 1 &&
	  args[0].equalsIgnoreCase("-help")) {
	printHelp();
      }
      else if (args.length == 1 &&
	       args[0].equalsIgnoreCase("-version")) {
	printVersion();
      }
      else {
	invocationError ("Odd number of arguments, something is missing?");
      }
    }
    
    HashMap arg_hash = new HashMap();
    String data_type = null;
    for (int i = 0; i < args.length - 1; i += 2) {
      String arg_name = getArgType(args[i]);
      if (arg_list.contains (arg_name)) {
	arg_hash.put (arg_name.toLowerCase(), args[i + 1]);
      }
      else {
	if (!arg_name.equals("list") &&
	    !arg_name.equals("job")) {
	  String analysis_file = IOUtil.findFile(args[i + 1]);
	  if (analysis_file != null) {
	    data_type = arg_name;
	    // save this in case it is needed to name the output file
	    arg_hash.put ("input_file", analysis_file);
	  }
	}
	else if (arg_name.equals("list")) {
	  // save this in case it is needed to name the output file
	  String list_file = IOUtil.findFile(args[i + 1]);
	  if (list_file != null)
	    arg_hash.put ("input_file", list_file);
	}
      }
    }
    
    // now initialize the filtering parameters
    /* this will fill in 
       the vector of parser classes
       the hashmap of data_types->filtering options
    */
    if (arg_hash.get("p") == null)
      arg_hash.put ("p", "bop.prefs");
    if (arg_hash.get("f") == null)
      arg_hash.put("f", "gamei");
    loadPrefs (arg_hash);

    if (data_type != null) {
      logger.info ("Getting options for " + data_type);
      AnalysisInput params = getAnalysisInput(data_type, false);
      for (int i = 0; i < args.length - 1; i += 2) {
	String arg_name = getArgType(args[i]);
	if (!arg_list.contains (arg_name) &&
	    !arg_name.equals("list") &&
	    !arg_name.equals("job") &&
	    !arg_name.equals(data_type)) {
	  setOption (params, arg_name, args[i + 1]);
	}
      }
    }
    return arg_hash;
  }

  private static void failureError(String msg) {
    logger.fatal (msg);
    System.exit (5);
  }

  private static void failureError(String msg, Exception e) {
    logger.fatal (msg, e);
    System.exit (5);
  }

  private static void invocationError(String msg) {
    logger.fatal (msg);
    logger.fatal (getUsage());
    System.exit (4);
  }

  private static void printHelp() {
    logger.fatal (getUsage());
    System.exit (0);
  }
  
  private static String getVersion() {
    return("BOP v.alpha, August 2003");
  }

  private static void printVersion() {
    logger.info ("\n" + getVersion());
    System.exit (0);
  }
  
  /**
   * wrapper class that works on command line
   */
  /** the user must provide
      1. at least one source of raw analysis output (or more)
      they may optionally provide
      2. a preferences file for filtering otherwise the 
      default is bop.prefs file
      3. the name of the output file
      default is sequencename.xml in local directory
      4. a file containing regular expressions for 
      parsing fasta header lines
      5. a file containing the fasta sequence of the query
  */
  private static String getUsage() {
    return ("To invoke the program:\n\tjava -mx800m apollo.bop.bop " +
	    "\nSpecify where to find the analysis results\n\t" +
	    "-list file_listing_all_resultfiles\n\t" +
	    "-type result_file (where type is a Apollo display types)\n\t" +
	    "[-db db_name -host server_name] -job job_id\n" +
	    "Additional optional arguments are:\n\t" +
	    "-o output_file (default is genomic sequence name)\n\t" +
	    "-f output format (default GAMEI)\n\t" +
	    "-p filtering_preferences_file (default bop.prefs)\n\t" +
	    "-s query_sequence_fasta_file\n" + 
	    "Filtering parameters can be set either by putting them in " +
	    " bop.prefs\nor on the command line\n\t" +
	    "-min_score\n\t" +
	    "-min_identity\n\t" +
	    "-min_length\n\t" +
	    "-percent_length\n\t" +
	    "-wordsize\n\t" +
	    "-max_compression\n\t" +
	    "-max_expect\n\t" +
	    "-max_coverage\n\t" +
	    "-max_exons\n\t" +
	    "-max_cDNA_gap\n\t" +
	    "-max_coincidence\n\t" +
	    "-remove_twilight\n\t" +
	    "-remove_shadows\n\t" +
	    "-separate_HSP\n\t" +
	    "-split_frames\n\t" +
	    "-split_duplicates\n\t" +
	    "-join_EST_ends\n\t" +
	    "-revcomp_3primeESTs\n\t" +
	    "-trim_polyA\n\t" +
	    "-keep_polyA\n\t" +
	    "-keep_promoter\n\t" +
	    "-debug\n\t" +
	    "\n\nFor example:\n" +
	    "bop -list BAC12345.bopin -f YOUR_APOLLO_DIR/conf/bop.prefs\n" +
	    "\tor\n" +
	    "bop -primate BAC1234.na_dbEST.primate.blastn\n" +
	    "\tor\n" +
	    "bop -job 14");
  }

  private static void logError (BopException e) {
    String msg = e.getMessageWStackTrace();
    //put into a file if asked for
    String bop_err_file = IOUtil.findFile ("bop.errors");
    if (bop_err_file.length() > 0) {
      try {
	FileOutputStream fos = new FileOutputStream(bop_err_file);
	fos.write(msg.getBytes());
      } 
      catch (Exception ex) {
	logger.error("Problem putting msg to file "+bop_err_file, ex);
      }
    } 
    else {
      logger.error(msg);
    }
    caught_exception = true;
  }

  private static String getArgType (String arg) {
    return (arg.startsWith("-") ? arg.substring (1) : arg).toLowerCase();
  }

  /** parse the input file */
  private static boolean processInput (CurationSet curation,
				       InputStream raw_data,
				       Date run_date,
				       AnalysisInput analysis_input,
				       AnalysisParserI parser,
				       HashMap arg_hash) {
    String analysis_type = null;
    // TODO - this should use Log4J configuration
    if (arg_hash.get("debug") != null) {
      analysis_input.setDebug ((String) arg_hash.get("debug"));
      logger.info ("Debugging: " + arg_hash.get("debug"));
    }
    logger.info ("Trying to parse with " + 
                 parser.getClass().getName());
    analysis_type = parser.load (curation,
				 curation.getRefSequence() == null,
				 raw_data,
				 analysis_input);
    boolean parsed = analysis_type != null;
    if (parsed) {
      if (analysis_input.runFilter()) {
	AnalysisFilterI filter = new AnalysisFilter();
	analysis_input.setAnalysisType (analysis_type);
	filter.cleanUp (curation, analysis_type, analysis_input);
      }
    }
    /* don't delete this. need to eradicate the huge
       hash tables generated when the xml is parsed */
    System.gc();
    return parsed;
  }

  private static InputStream getInputStream(String filename)
    throws ApolloAdapterException {
    InputStream stream = null;
    try {
      stream = new FileInputStream (filename);
    }
    catch (Exception e) { // FileNotFoundException
      stream = null;
      // If we couldn't find filename, and it's a relative, 
      // rather than absolute, path,
      // try prepending APOLLO_ROOT.  --NH, 01/2002
      // but check that its not already prepended
      String rootdir = System.getProperty("APOLLO_ROOT");
      if (!(filename.startsWith("/")) && !(filename.startsWith("\\"))
	  && !filename.startsWith(rootdir)) {
	String absolute = rootdir + "/" + filename;
	try {
	  stream = new FileInputStream (absolute);
	}
	catch (Exception e2) {
	  // Try sticking "data/" in after APOLLO_ROOT
	  absolute = rootdir + "/data/" + filename;
	  try {
	    stream = new FileInputStream (absolute);
	  }
	  catch (Exception e3) {
            caught_exception = true;
	    throw new ApolloAdapterException("Error: could not open file " + 
					   filename + " for reading.");
	  }
	}
      }
      caught_exception = true;
      throw new ApolloAdapterException("Error: could not open " + filename + 
				     " for reading.");
    }
    return stream;
  }

  private static AnalysisInput getAnalysisInput (String data_type, 
                                                 boolean report) {
    AnalysisInput options = null;
	    
    if (datatype2options == null)
      datatype2options = new HashMap();
    else
      options = (AnalysisInput) datatype2options.get(data_type.toLowerCase());
    if (options == null) {
      if (report)
        logger.info ("No filtering found for " + data_type);
      options = new AnalysisInput();
      options.setType (data_type);
      datatype2options.put(data_type.toLowerCase(), options);
    }
    return options;
  }

  private static void loadPrefs (HashMap arg_hash) {
    datatype2options = null;
    parsers = new Vector();

    String file_name = (String) arg_hash.get("p");
    logger.info ("Reading filtering preferences from " + file_name);
    String prefs_file = IOUtil.findFile (file_name);
    if (prefs_file != null && ! prefs_file.equals ("")) {
      if ( ! (new File (prefs_file)).canRead () ) {
	failureError ("Can't read options from " + prefs_file);
      }
    }
    else
      failureError ("Can't find options file " + prefs_file);

    StreamTokenizer tokens;
    InputStream prefs_stream;
      
    try {
      prefs_stream = new FileInputStream (prefs_file);
      Reader r = new BufferedReader(new InputStreamReader(prefs_stream));
      tokens = new StreamTokenizer(r);
      tokens.eolIsSignificant(false);
      tokens.quoteChar('"');
      try {
	while ((tokens.nextToken()) != tokens.TT_EOF) {
          try {
            String pref_type = tokens.sval;
            
            if (pref_type.equals("filter")) {
              tokens.nextToken();
              String data_type = tokens.sval;
              tokens.nextToken();
              String option_values = tokens.sval;
              AnalysisInput options = getAnalysisInput(data_type, false);
              parseOptions(options, data_type, option_values);
            }
            else if (pref_type.equals("database")) {
              tokens.nextToken();
              String firstWord = tokens.sval;
              tokens.nextToken();
              String secondWord = tokens.sval;
              if (firstWord.toUpperCase().equals("HOSTNAME")) {
                if (arg_hash.get("host") == null)
                  arg_hash.put("host", secondWord);
              } 
              else if (firstWord.toUpperCase().equals("DBNAME")) {
                if (arg_hash.get("db") == null)
                  arg_hash.put("db", secondWord);
              }
            } 
            else if (pref_type.equals("parser")) {
              tokens.nextToken();
              String parser_name = tokens.sval;
              try {
                Class a_class  = Class.forName (parser_name);	
                Object parser = a_class.newInstance();
                parsers.addElement (parser);
              }
              catch (Exception e) {
                logger.error ("HEY!, couldn't create a " + parser_name, e);
                
              }
            } 
            else if (pref_type.equals("autopromote")) {
              tokens.nextToken();
              String data_type = tokens.sval;
              tokens.nextToken();
              String namer = tokens.sval;
              AnalysisInput options = getAnalysisInput(data_type, false);
              options.promoteResults(namer);
            } 
            else if (pref_type.equals("collapse")) {
              tokens.nextToken();
              String data_type = tokens.sval;
              AnalysisInput options = getAnalysisInput(data_type, false);
              options.collapseResults (true);
            } 
            else if (pref_type.equals("outputformat")) {
              tokens.nextToken();
              arg_hash.put ("f", tokens.sval.toLowerCase());
            } 
            else if (pref_type.equals("analysis")) {
              // ignore these lines
              tokens.nextToken();
              tokens.nextToken();
            }
            else {
              logger.info("pref type " + pref_type + 
                          " not recognized--tokens = " + tokens);
            }
          } catch (Exception e) {
            logger.error(e.getMessage() + " parsing tokens: " + tokens.toString(), e);
            caught_exception = true;
          }            
	} //end while
      }
      catch( Exception ex ) {
	logger.error(ex.getMessage(), ex);
        caught_exception = true;
      }
      try {
	prefs_stream.close();
      }
      catch( Exception ex ) {
	logger.error(ex.getMessage(), ex);
        caught_exception = true;
      }    
    }
    catch( Exception ex ) {
      failureError ("Error loading preferences:  " + ex.getMessage(), ex);
    }
  }
  
  private static void parseOptions (AnalysisInput options,
				    String feature_type, 
				    String inputs) {
    String key, value;
    StringTokenizer tokens = new StringTokenizer(inputs);
    while (tokens.hasMoreElements()) {
      key = tokens.nextToken();
      value = tokens.nextToken();
      setOption (options, key, value);
    }
  }

  private static boolean setOption (AnalysisInput options,
				    String key, String value) {
    boolean known = true;

    if (key.startsWith("-"))
      key = key.substring(1);

    if (key.equals("min_score"))
      options.setMinScore(value);
    else if (key.equals("min_identity"))
      options.setMinIdentity(value);
    else if (key.equals("min_length")) {
      options.setMinLength(value);
      options.setLengthUnits(false);
    }
    else if (key.equals("percent_length")) {
      options.setMinLength(value);
      options.setLengthUnits(true);
    }
    else if (key.equals("wordsize"))
      options.setWordSize(value);
    else if (key.equals("max_compression"))
      options.setMaxRatio(value);
    else if (key.equals("max_expect"))
      options.setMaxExpect(value);
    else if (key.equals("max_coverage"))
      options.setMaxCover(value);
    else if (key.equals("max_exons"))
      options.setMaxExons(value);
    else if (key.equals("max_cDNA_gap"))
      options.setMaxAlignGap(value);
    else if (key.equals("max_coincidence"))
      options.setCoincidence(value);
    else if (key.equals("remove_twilight"))
      options.setRemoveTwilights(value.equalsIgnoreCase("t") ||
				 value.equalsIgnoreCase("true"));
    else if (key.equals("remove_shadows"))
      options.filterShadows(value.equalsIgnoreCase("t") ||
			       value.equalsIgnoreCase("true"));
    else if (key.equals("separate_HSP"))
      options.autonomousHSPs(value.equalsIgnoreCase("t") ||
				value.equalsIgnoreCase("true"));
    else if (key.equals("split_frames"))
      options.setSplitFrames(value.equalsIgnoreCase("t") ||
			     value.equalsIgnoreCase("true"));
    else if (key.equals("split_duplicates"))
      options.setSplitTandems(value.equalsIgnoreCase("t") ||
			      value.equalsIgnoreCase("true"));
    else if (key.equals("join_EST_ends"))
      options.joinESTends(value.equalsIgnoreCase("t") ||
			     value.equalsIgnoreCase("true"));
    else if (key.equals("revcomp_3primeESTs"))
      options.revComp3Prime(value);
    else if (key.equals("trim_polyA"))
      options.trimPolyA(value.equalsIgnoreCase("t") ||
			value.equalsIgnoreCase("true"));
    else if (key.equals("keep_polyA"))
      options.keepPolyA(value.equalsIgnoreCase("t") ||
			value.equalsIgnoreCase("true"));
    else if (key.equals("keep_promoter"))
      options.keepPromoter(value.equalsIgnoreCase("t") ||
			   value.equalsIgnoreCase("true"));
    else if (key.equals("debug"))
      options.setDebug(value);
    else {
      logger.error ("Unknown option " + key + "=" + value);
      known = false;
    }
    return known;
  }

  private static void saveResults (CurationSet curation, HashMap arg_hash) {
    /* the output file name was either passed in or not */
    String output_file = (String) arg_hash.get ("o");
    if (output_file == null) {
      /* need to contrive a name to save the results in */
      // first choice is the name of the sequence with a .xml suffix
      // if that fails use input filename, and replace suffix with xml
      if (curation.getRefSequence() != null) {
	String job_id = (String) arg_hash.get("job");
	if (job_id != null)
	  //add job id to front of output file name -- Shu
	  output_file = (job_id + "." + 
			 curation.getRefSequence().getName());
	else {
	  output_file = (String) arg_hash.get("input_file");
	  if (output_file == null) {
            logger.warn ("Don't know what to call the output, " +
                         " because there was no input file.");
	    output_file = curation.getRefSequence().getName();
          }
	  int index = output_file.lastIndexOf ("/");
	  if (index > 0)
	    output_file = (output_file.substring (0, index) + "/" +
			   curation.getRefSequence().getName());
	  else
	    output_file = curation.getRefSequence().getName();
	}
      }
      else {
	logger.error("There is no curated sequence? " + 
                     curation.getName());
	output_file = (String) arg_hash.get("input_file");
	if (output_file == null)
	  failureError ("Don't know what to call the output, " +
                        " because there was no input file.");
	int index = output_file.lastIndexOf (".");
	if (index > 0)
	  output_file = output_file.substring (0, index);
      }
      output_file = output_file + ".xml";
    }
    String format = (String) arg_hash.get("f");
    if (format.equals("gamei")) {
      GAMESave.writeXML(curation, output_file, getVersion(), false);
    }
    else if (format.equals("gameii"))
      GAMESave.writeXML(curation, output_file, getVersion(), true);
    else if (format.equals("gff")) {
      try {
	GFFAdapter gff = new GFFAdapter();
	gff.setFilename(output_file);
	gff.commitChanges(curation);
      }
      catch (Exception e) {
	logger.error ("Could not save gff to " + output_file, e);
      }
    }
    else {
      logger.warn ("Don't know about format " + format +
                   " so saving as GAMEI in " + output_file);
      GAMESave.writeXML(curation, output_file, getVersion(), false);
    }
  }
}
