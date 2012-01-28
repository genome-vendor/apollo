package apollo.main;

import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// from jars/te-common.jar
import com.townleyenterprises.command.CommandOption;
import com.townleyenterprises.command.CommandParser;
import com.townleyenterprises.command.DefaultCommandListener;
import com.townleyenterprises.command.RequiresAnyOptionConstraint;

import org.apache.log4j.*;

import org.bdgp.io.DataAdapterRegistry;

import apollo.config.Config;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.DataInput;
import apollo.dataadapter.DataInputType;
import apollo.dataadapter.DataInputType.UnknownTypeException;
import apollo.dataadapter.GFFAdapter;
import apollo.dataadapter.gff3.GFF3Adapter;
import apollo.dataadapter.SerialDiskAdapter;
import apollo.dataadapter.chado.ChadoAdapter;
import apollo.dataadapter.chado.ChadoDatabase;
import apollo.dataadapter.chadoxml.ChadoXmlAdapter;
import apollo.dataadapter.gamexml.GAMEAdapter;
import apollo.dataadapter.genbank.GenbankAdapter;
import apollo.dataadapter.das2.DAS2Adapter;
import apollo.datamodel.GenomicRange;

/** 
 * Class for dealing with command line options. Command line options for apollo 
 * are for reading and writing data. uses te-common.jar which provides the Command
 * objects that deal with the command line parsing. Used by Apollo class. 
 */
public class CommandLine {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(CommandLine.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  // stores adapter class, filenames, etc. for reading data
  private IOOptions readOptions;
  // stores adapter class, filenames, etc. for writing data
  private IOOptions writeOptions;
  // the thing that parses all the options
  private CommandParser parser;

  // ------------------------------------------------
  // input/read CommandOptions
  // ------------------------------------------------    

  private InputFormatCommandOption inputFmtOption = new InputFormatCommandOption();
  private CommandOption inputFileOption = new InputFileCommandOption();
  private CommandOption seqFileOption = new SequenceFileCommandOption();
  private CommandOption featureNameOption = new FeatureNameCommandOption();
  private CommandOption locationOption = new LocationCommandOption();
  private CommandOption gameFileOption = new GameFileCommandOption();
  private CommandOption backupFileOption = new BackupFileCommandOption();

  // input/options specific to the CHADO_JDBC adapter:
  // database password
  // JC: wouldn't it be better to have the adapter itself provide a (static) list of 
  // miscellaneous options to this class?
  private CommandOption dbPassOption = new DbPassCommandOption();
  // database username
  private CommandOption dbUserOption = new DbUserCommandOption();
  // batch load to chado

  private CommandOption inputFilesList = new InputFilesListCommandOption();
  
  //run in headless mode
  private CommandOption headlessOption = new HeadlessCommandOption();

  // ------------------------------------------------
  // output/write CommandOptions
  // ------------------------------------------------    

  private OutputFormatCommandOption outputFmtOption = new OutputFormatCommandOption();
  private CommandOption writeFile = new WriteFileCommandOption();
  private CommandOption inputListFileOption = new InputListFileCommandOption();
  private CommandOption skipExistBatchEntry = new SkipExistBatchEntryCommandOption();
  private InputTypeCommandOption inputTypeOption = new InputTypeCommandOption();

  /** inner class that deals with batch file */
  private BatchFile batchFile;

  // option to write data to flat file instead of database
  private CommandOption dbFlatFileOutputOption = new DbFlatFileOutputCommandOption();

  // option to add GFF_source DbXref as requested by Scott Cain for community annotation
  private CommandOption gffSourceOption = new GFFSourceCommandOption();
  
  // all command-line options
  CommandOption[] options = new CommandOption[] { 
    inputFmtOption, inputFileOption, featureNameOption, locationOption, gameFileOption, 
    backupFileOption, seqFileOption, outputFmtOption, writeFile, inputListFileOption, 
    skipExistBatchEntry, inputTypeOption, dbUserOption, dbPassOption, dbFlatFileOutputOption, 
    gffSourceOption, inputFilesList, headlessOption };

  // -----------------------------------------------------------------------
  // Class variables
  // -----------------------------------------------------------------------

  // this guarantees that we get the right classes (compile time check)
  private final static String GAME = GAMEAdapter.class.getName();
  private final static String CHADOXML = ChadoXmlAdapter.class.getName();
  private final static String CHADO_JDBC = ChadoAdapter.class.getName();
  private final static String SERIALIZED = SerialDiskAdapter.class.getName();
  private final static String GFF = GFFAdapter.class.getName();
  private final static String GFF3 = GFF3Adapter.class.getName();
  private final static String GENBANK = GenbankAdapter.class.getName();
  private final static String DAS2 = DAS2Adapter.class.getName();

  // there can be only one instance of this class (per JVM)
  private static CommandLine commandLineSingleton;

  // -----------------------------------------------------------------------
  // CommandLine - static methods
  // -----------------------------------------------------------------------

  public static CommandLine getCommandLine() {
    if (commandLineSingleton == null)
      commandLineSingleton = new CommandLine();
    return commandLineSingleton;
  }

  /** 
   * If input & output (or batch) is fully specified, then we are in command line
   * mode; no need for GUI.
   */
  public static boolean isInCommandLineMode() {
    CommandLine cl = getCommandLine();
    return cl.writeIsSpecified() || cl.isBatchMode();
  }

  // -----------------------------------------------------------------------
  // Constructors
  // -----------------------------------------------------------------------

  /**
   * Private constructor; this class uses the singleton pattern -- see <code>getCommandLine()</code>
   */
  private CommandLine() {}

  // -----------------------------------------------------------------------
  // CommandLine - public methods
  // -----------------------------------------------------------------------

  /** 
   * Set command-line arguments passed along from main().
   *
   * @param args argStrings from command line 
   */
  public void setArgs(String[] args) throws ApolloAdapterException {
    if (args.length == 0)
      return;
    if (parser != null) 
      logger.warn("CommandLine - multiple calls to setArgs() - ignoring all but the first");
    processArgs(args);
  }

  // -----------------------------------------------------------------------
  // CommandLine - private methods
  // -----------------------------------------------------------------------

  // not clear why this is a separate method; the only call is in setArgs
  private void processArgs(String[] args) throws ApolloAdapterException {
    parser = new CommandParser("Apollo"); // help text?
    parser.addCommandListener(new DefaultCommandListener("Options",options));
    parser.parse(args);
    addConstraints(parser);
    // does execute() on CommandOptions, throws generic Exception
    try {
      parser.executeCommands(); 
    } 
    catch (Exception e) {
      throw new ApolloAdapterException(e);
    }
  }
  
  private void addConstraints(CommandParser parser) {
    CommandOption[] inFmtDep = new CommandOption[]{inputFmtOption};
    // location has to have input format specified
    int exitStatus = -2;
    parser.addConstraint(new RequiresAnyOptionConstraint(exitStatus,locationOption,inFmtDep));
  }

  private IOOptions getReadOptions() {
    if (readOptions == null)
      readOptions = new IOOptions(true);
    return readOptions;
  }

  private IOOptions getWriteOptions() {
    if (writeOptions == null)
      writeOptions = new IOOptions(false);
    return writeOptions;
  }

  private boolean haveReadAdapter() throws Exception {
    return getReadAdapter() != null;
  }

  private boolean haveInputType() throws Exception {
    return inputTypeOption.haveType();
  }

  private ApolloDataAdapterI getDataAdapter(String classString) {
    DataAdapterRegistry dar = Config.getAdapterRegistry();
    // check instanceof ADAI?
    ApolloDataAdapterI a = (ApolloDataAdapterI)dar.getAdapter(classString);
    if (a == null) {
      String e = classString+" is not in adapterRegistry. check apollo.cfg";
      throw new RuntimeException(e); // DataAdapterException?
    }
    return a;
  }

  private void error(String m) throws Exception {
    Exception e = new Exception(m);
    logger.error(m, e);
    printHelp();
    throw e;
  }

  private boolean haveSequenceFilename() {
    return seqFileOption.getMatched();
  }
  
  private String getSequenceFilename() {
    return seqFileOption.getArg();
  }

  /** 
   * Read & write file method. Retrieves data adapter either from format option or
   * suffix and sets its DataInput with filename 
   */
  private void setAdapterForFile(IOOptions options, boolean setAdapterInput)
    throws Exception 
  {
    ApolloDataAdapterI adapter = getAdapterForFile(options);
    if (!options.hasAdapter())
      options.setAdapter(adapter);
    // is it scandalous to use DataInput for output?? should be called DataInfo?
    // of DataSpecifitation?
    String inputFile = options.getFilename();
    DataInputType inputType = getInputType(inputFile);
//     DataInputType inputType = DataInputType.FILE;
//     if (inputFile.startsWith("http:") ||
//         inputFile.startsWith("file:")) {
//       //      System.out.println(inputFile+ " is an URL"); // DEL
//       inputType = DataInputType.URL;
//     }
    DataInput di = new DataInput(inputType, inputFile);
    // gff input may have seq file.
    if (haveSequenceFilename()) di.setSequenceFilename(getSequenceFilename());
    options.setDataInput(di);
    options.setSpecifiedState(true);
    if (setAdapterInput) adapter.setDataInput(di);
  }

  /** returns URL if starts with http: or file: */
  private DataInputType getInputType(String inputFile) {
    if (inputFile.startsWith("http:") || inputFile.startsWith("file:"))
      return DataInputType.URL;
    else
      return DataInputType.FILE;
  }

  
  /**
   * Return the IOOptions which has a db dataadapter. For now only test if the dataAdapter is a ChadoAdapter
   * @return IOOptions
   * @throws Exception if no database found
   */
  private IOOptions getDbIOOptions() throws Exception{
    
    // For now only test if any adapter is an instance of chadoDB
    // We need a better way to identify DB dataadapter. (ApolloDataAdapterI sub interface or attribute? DataInput ?)
    
    // First, get sure that the IOOptions have their dataadapter. 
    if (!getReadOptions().hasAdapter())
      getReadOptions().setAdapter(inputFmtOption.getAdapter());
    if (!getWriteOptions().hasAdapter())
      getWriteOptions().setAdapter(outputFmtOption.getAdapter());
    
    if (getReadAdapter() instanceof ChadoAdapter)
      return getReadOptions();
    
    if (getWriteAdapter() instanceof ChadoAdapter)
      return getWriteOptions();
    
    throw new Exception("Unable to find a database (ie chadoDB) for either input or output");
  }

  private ApolloDataAdapterI getAdapterForFile(IOOptions options)
    throws Exception {
    ApolloDataAdapterI adapter; // class var/cache?
    // first see if input format explicitly specified //if(getAdapter(forRead)!=null)
    if (options.hasAdapter())
      return options.getAdapter();

    // if nothing specified try to get format from file suffix
    adapter = getDataAdapterFromSuffix(options.getFilename());

    if (adapter == null) {
      error("\nNo input format specified and can't determine it from suffix "
            +getFileSuffix(options.getFilename()));
    }
    return adapter;
  }

  private ApolloDataAdapterI getDataAdapterFromSuffix(String filename) {
    String suffix = getFileSuffix(filename);
    // anything with game in it??
    if (suffix.matches(".*game.*"))
      return getDataAdapter(GAME);
    if (suffix.matches(".*chado.*") || filename.matches(".*chado.*"))
      return getDataAdapter(CHADOXML);
    if (suffix.equalsIgnoreCase("gff"))
      return getDataAdapter(GFF);
    if (suffix.matches("gbk?|genbank")) // genbank suffixes?
      return getDataAdapter(GENBANK);
    if (suffix.matches("backup|ser"))
      return getDataAdapter(SERIALIZED);
    if (suffix.matches(".*das2.*"))
      return getDataAdapter(DAS2);
    // configuration can specify what to do with .xml suffix (game or chado)
    else if (suffix.equals("xml") && xmlSuffixIsConfigged())
      return getConfiggedXmlDataAdapter();
    return null;
  }

  private ApolloDataAdapterI getAdapterForString(String adapString) throws Exception {
    adapString = adapString.toLowerCase();
    if (adapString.equalsIgnoreCase("chadoDB")) {
      return getDataAdapter(CHADO_JDBC);
    } 
    else if (adapString.matches("game|gamexml|gameXML")) {
      return getDataAdapter(GAME);
    }
    else if (adapString.matches("chado|chadoxml|chadoXML")) {
      return getDataAdapter(CHADOXML);
    }
    else if (adapString.matches("backup|serialized|serial")) {
      return getDataAdapter(SERIALIZED);
    }
    else if (adapString.equalsIgnoreCase("gff")) {
      return getDataAdapter(GFF);
    }
    else if (adapString.matches("genbank|gb")) {
      return getDataAdapter(GENBANK);
    }
    else if (adapString.toLowerCase().startsWith("das")) {
      return getDataAdapter(DAS2);
    }
    else if (adapString.toLowerCase().equalsIgnoreCase("gff3")) {
      return getDataAdapter(GFF3);
    }
    else {
      error("Don't recognize input/output format "+adapString);
      return null;
    }
  }

  /** should this include the '.'? probably not. changing it to not include . */
  private String getFileSuffix(String filename) {
    int index = filename.lastIndexOf('.');
    return filename.substring(index+1); // 1 past .
  }

  private boolean xmlSuffixIsConfigged() {
    return Config.commandLineXmlFileFormatIsConfigged();
  }

  private ApolloDataAdapterI getConfiggedXmlDataAdapter() {
    String config = Config.getCommandLineXmlFileFormat();
    if (config.equalsIgnoreCase("game"))
      return getDataAdapter(GAME);
    if (config.equalsIgnoreCase("chado"))
      return getDataAdapter(CHADOXML);
    return null;
  }

  // handy for backwards compatibility options
  private void setReadFileAdapter(String adapterClass,String filename) {
    ApolloDataAdapterI adapter = getDataAdapter(adapterClass);
    getReadOptions().setAdapter(adapter);
    DataInputType type = getInputType(filename);
    // WRONG! could be an url! - fix MG 5/21/07
    //DataInput di = new DataInput(DataInputType.FILE,filename);
    DataInput di = new DataInput(type,filename);
    //    adapter.setDataInput(di);
    getReadOptions().setDataInput(di);
    getReadOptions().setSpecifiedState(true);
  }

  // -----------------------------------------------------------------------
  // CommandLine - protected methods
  // -----------------------------------------------------------------------

  void printHelp() {
    parser.help();
  }

  // -----------------------------------------------------------------------
  // CommandLine - package methods
  // -----------------------------------------------------------------------

  /** if read has been (correctly) specified, read adapter is non null */
  boolean readIsSpecified() {
    return getReadOptions().getSpecifiedState();
  }

  boolean writeIsSpecified() {
    return getWriteOptions().getSpecifiedState();
  }

  /** 
   * If input/read was specified on the command line this returns the 
   * correctly-initialized read data adapter for it.  Note that
   * adapter returned may be the same object as that returned by 
   * <code>getWriteAdapter()</code>
   * @return null if no read adapter specified on command line. 
   */
  ApolloDataAdapterI getReadAdapter() throws Exception {
    IOOptions ioo = getReadOptions();
    ApolloDataAdapterI adapter = ioo.getAdapter();
    adapter.setDataInput(ioo.getDataInput());
    return adapter;
  }

  /** 
   * If output/write was specified on the command line this returns the 
   * correctly-initialized write data adapter for it.  Note that the
   * adapter returned may be the same object as that returned by 
   * <code>getReadAdapter()</code>
   * @return null if no read adapter specified on command line. 
   */
  ApolloDataAdapterI getWriteAdapter() throws ApolloAdapterException {
    logger.debug("CommandLine: getWriteAdapter called");

    try {
      IOOptions ioo = getWriteOptions();
      ApolloDataAdapterI adapter = ioo.getAdapter();
      adapter.setDataInput(ioo.getDataInput());
      return adapter;
    } catch (Exception e) {
      throw new ApolloAdapterException(e);
    }
  }

  boolean isBatchMode() {
    return (batchFile != null);
  }

  boolean hasMoreBatchEntries() {
    if (batchFile == null) // shouldn't happen
      return false;
    return batchFile.hasMoreBatchEntries();
  }

  void setToNextBatchEntry() throws Exception {
    batchFile.setToNextBatchEntry();
  }

  // -----------------------------------------------------------------------
  // CommandOption inner classes
  // -----------------------------------------------------------------------

  /** 
   * Specifies data adapter to use for reading.
   */
  private class InputFormatCommandOption extends CommandOption {

    private ApolloDataAdapterI readAdapter;

    private final static String help =
      "Specify input format. [chadoDB|game|chadoxml|genbank|gff|das2|backup|gff3]";

    private InputFormatCommandOption() {
      super("inputFormat",'i',true,"input_format",help);
    }

    private ApolloDataAdapterI getAdapter() throws Exception {
      if (!getMatched())
        return null;
      if (readAdapter == null)
        readAdapter = getAdapterForString(getArg());
      // set IO Operation?? - do in options class -> dont need op, just for gui
      return readAdapter;
    }
  }

  /** 
   * Specifies data adapter to use for writing.
   */
  private class OutputFormatCommandOption extends CommandOption {

    private ApolloDataAdapterI writeAdapter;
    /** For default output files from batch file */
    private String fileSuffix;

    private final static String help =
      "Specify output format. [chadoDB|game|chadoxml|genbank|gff|das2|backup]";

    private OutputFormatCommandOption() { 
      super("outputFormat",'o',true,"output_format",help); // true -> hasArg
    }

    public void execute() throws Exception {
      ApolloDataAdapterI ad = getAdapter();
      if (ad != null)
        getWriteOptions().setAdapter(getAdapter());
    }

    private ApolloDataAdapterI getAdapter() throws Exception {
      if (!getMatched())
        return null;

      if (writeAdapter == null)
        writeAdapter = getAdapterForString(getArg());
      
      // if it's a database adapter then the output is fully specified. databases
      // dont require any other params, like filenames.
      if (writeAdapter != null && adapterStringIsDatabase(getArg()))
        getWriteOptions().setSpecifiedState(true);
      
      // arg is file suffix - why not?
      setFileSuffix(getArg());

      return writeAdapter;
    }

    /** Returns true if adapter is for a database. currently this is just
        chadoDB. */
    private boolean adapterStringIsDatabase(String adapterString) {
      return adapterString.equalsIgnoreCase("chadoDB");
    }
    
    /** file suffix for default file making in batch mode - just uses arg actually */
    private void setFileSuffix(String fileSuffix) {
      this.fileSuffix = fileSuffix;
    }
    private String getFileSuffix() { return fileSuffix; }

  }

  /**
   * Specifies a feature name (but no location) for input
   */
  private class FeatureNameCommandOption extends CommandOption {

    private final static String help = 
      "Specify a feature name for input.  The type of name will depend on --inputType";

    private FeatureNameCommandOption() {
      super("featureName",'u',true,"feature_name",help);
    }

    public void execute() throws Exception {
      if (!haveReadAdapter())
        error("Feature name specified without input format");
      if (!inputTypeOption.haveType())
        error("Feature name specified without input (SO) type");

      DataInputType type = null;
      type = getReadOptions().getInputType();
      DataInput input = new DataInput(type,getArg());
      IOOptions ioo = getReadOptions();
      ioo.setDataInput(input);
      ioo.setSpecifiedState(true);
    }
  }

  /** 
   * Specifies a location for input - output doesnt need a location 
   */
  private class LocationCommandOption extends CommandOption {
    
    private final static String help = 
      "Specify a location for input. Chromosome:start-end, e.g. 1:1000-6000";

    private LocationCommandOption() {
      super("location",'l',true,"location",help);
    }

    public void execute() throws Exception {
      if (!haveReadAdapter())
        error("Location specified without input format");

      DataInputType type = null;
      type = DataInputType.BASEPAIR_RANGE;

      // arg should be location string of 1:1000-2000 ilk
      DataInput input = new DataInput(type,getArg());
      IOOptions ioo = getReadOptions();
      ioo.setDataInput(input);
      ioo.setSpecifiedState(true);
    }
  }

  /**
   * Specifies input file name.
   */
  // TODO: make this an outer class? (not sure why)
  private class InputFileCommandOption extends CommandOption {
    
    private final static String help =
      "Specify filename to read in (game,gff,gb,chadoxml,backup)";

    private InputFileCommandOption() {
      // true -> has argument
      super("inputFile",'f',true,"filename",help);
    }
    
    public void execute() throws Exception {
      getReadOptions().setFilename(getArg());
      setAdapterForFile(getReadOptions(), false);
    }
  }

  private class SequenceFileCommandOption extends CommandOption {
    private final static String help =
      "Specify FASTA sequence file to load with gff file";

    private SequenceFileCommandOption() {
      // true -> has argument
      super("sequenceFile",'s',true,"sequence_file",help);
    }
    
    public void execute() throws Exception {
      getReadOptions().setSequenceFilename(getArg());
    }
  }

  /** 
   * Specifies file with a list of items to query & save in batch mode.
   */
  private class InputListFileCommandOption extends CommandOption {

    private final static String help = 
      "Specify a file which contains a list of items to query of type inputType. "+
      "These will then be queried with inputFormat and written out to ouputFormat.";

    private InputListFileCommandOption() {
      // true -> hasArgument
      super("inputListFile",'n',true,"filename",help);
    }

    // called via parser.executeCommands() called in processArgs
    public void execute() throws Exception {
      // if an input list is specified, we're in batch mode
      batchFile = new BatchFile(getArg()); // throws FileNotFoundException

      // if have read adapter(input format) && inputType then read is specified
      if (!haveReadAdapter())
        error("Input list/batch file specified without input format");
      if (!haveInputType())
        error("Input list file specified without input type");
      getReadOptions().setSpecifiedState(true);
    }
  }

  /**
   * Specifies that batch mode inputs for which output files exist should be skipped.
   */
  private class SkipExistBatchEntryCommandOption extends CommandOption {
    private final static String help = "If in batch mode, skip entry if "
      +"the output (file) already exists";
    
    private SkipExistBatchEntryCommandOption() {
      //boolean hasArg = false;
      super("skipExistBatchEntry",'k',false,null,help);
    }
    
  }

  /**
   * Specifies file with a list items to query & save in batch mode.
   */
  // TODO: can this be merged with InputListFileCO?
  private class InputFilesListCommandOption extends CommandOption {

    private final static String help = 
      "Specify a file which contains a list of files to process as inputFormat and to write to outputFormat." +
      " Intended for batch loading files into a DB (chado)";

    private InputFilesListCommandOption() {
      super("inputFilesList",'p',true,"filename",help);
    }

    public void execute() throws Exception {
      // if an input list is specified, we're in batch mode
      batchFile = new BatchInputFile(getArg()); // throws FileNotFoundException
    
      if (!haveReadAdapter())
        error("Input list/batch file specified without input format");
      
      getReadOptions().setSpecifiedState(true);
    }
  }
  
  /**
   * Specifies the type of region/sequence to be read into Apollo.  The valid choices
   * will depends on the input data adapter.
   */
  private class InputTypeCommandOption extends CommandOption {
    
    private final static String help = "Input type is dependent on input format. For "
      + "chadoDB use the seqTypes for the db being loaded (e.g. chromosome_arm|"
      + "chromosome, gene, golden_path_region...";

    private InputTypeCommandOption() {
      super("inputType",'t',true,"type",help);
    }

    private boolean haveType() throws Exception {
      if (!getMatched())
        return false;
      execute(); // throws Ex
      return getReadOptions().haveInputType();
    }

    public void execute() throws Exception {
      getReadOptions().setInputType(getArg());
    }
  }

  // TODO - reinstate the following useful method:
  //   /** Gets input type help by querying DataInputType */
  //   private static String inputTypeHelp() {
  //     String[] types = DataInputType.getTypeStrings();
  //     StringBuffer sb = new StringBuffer("Specify input type.\n[");
  //     for (int i=0; i<types.length; i++) {
  //       sb.append(types[i]);
  //       if (i < types.length-1)
  //         sb.append('|');
  //       if (i == types.length/2)
  //         sb.append('\n');
  //     }
  //     sb.append("]");
  //     return sb.toString();
  //   }

  /**
   * Specifies name of target/output file.
   */
  private class WriteFileCommandOption extends CommandOption {
    
    private WriteFileCommandOption() {
      super("writeFile",'w',true,"filename","Filename to write to");
    }

    public void execute() throws Exception {
      getWriteOptions().setFilename(getArg());
      setAdapterForFile(getWriteOptions(), false); // sets data adapter from file suffix
    }
  }

  /** 
   * Specifies a GAME XML file for reading/input. This is mainly for backwards 
   * compatibility. The old way of loading a game file was with "-x". The new 
   * way is "-i game -f gamefile.xml" (or -f gamefile.game or gamefile.xml with
   *  xml config param set to game) but to be backwards compatible -x will be 
   * sugar for -i game -f gamefile.xml. -x is for reading not writing. 
   */
  private class GameFileCommandOption extends CommandOption {

    private final static String help =
      "Read in a game file. (Shorthand for -i game -f gamefile)";

    private GameFileCommandOption() {
      super("readGameFile",'x',true,"game_file",help);
    }

    public void execute() throws Exception {
      setReadFileAdapter(GAME,getArg());
    }
  }

  /**
   * Specifies a file to which output should be written in SERIALIZED format.
   * For convenience & backwards compatibility, -b is synonomous with 
   * -i serialized -f backupFile
   */
  private class BackupFileCommandOption extends CommandOption {

    private final static String help =
      "Read in a backup (serialized) file. (Shorthand for -i serialized -f serFile)";

    private BackupFileCommandOption() {
      super("readBackupFile",'b',true,"backup_file",help);
    }

    public void execute() throws Exception {
      setReadFileAdapter(SERIALIZED,getArg());
    }
  }

  /**
   * Specifies a password to use to connect to a chado database for reading or writing.
   */
  // JC: note that the provision of a single password suggests that you can't read from one
  // chado database and write to another
  private class DbPassCommandOption extends CommandOption {
    private final static String help ="Read in the password for the chado database you want to read from/write to. " ;
    
    public DbPassCommandOption() {
      super("readDbPass", 'D', true, "db_pass", help);
    }
    
    public void execute() throws Exception {
      IOOptions ioo = null;
      try {
        ioo = getDbIOOptions();
      } catch (Exception e) {
        logger.error("Exception calling getDbIOOptions", e);
      }
      if (ioo == null) {
        logger.fatal("A password was specified without using a DB (chadoDB) as input or output");
        parser.usage();
        System.exit(1);//Maybe a bit too brutal
      }
      try {
        ((ChadoAdapter)ioo.getAdapter()).setDbPassForDefaultDb(getArg());
      } catch (Exception e) {
        logger.fatal("Exception calling setDbPassForDefaultDb", e);
        System.exit(1);
      }
    }
  }

  /**
   * Specifies a username to use to connect to a chado database for reading or writing.
   */
  private class DbUserCommandOption extends CommandOption {
    private final static String help ="Username for the chado database you want to read from/write to. " ;
    
    public DbUserCommandOption() {
      super("readDbUser", 'U', true, "db_user", help);
    }
    
    public void execute() throws Exception {
      
      IOOptions ioo = null;
      try {
        ioo = getDbIOOptions();
      } catch (Exception e) {
        logger.error("Exception in call to getDbIOOptions", e);
      } 
      if (ioo == null) {
        logger.fatal("A username was specified without using a DB (chadoDB) as input or output");
        parser.usage();
        System.exit(1);//Maybe a bit too brutal
      }
      try {
        ((ChadoAdapter)ioo.getAdapter()).setDbLoginForDefaultDb(getArg());
      } catch (Exception e) {
        logger.fatal("Exception calling setDbLoginForDefaultDb", e);
        System.exit(1);
      }
    }
  }

  /**
   * Specifies (boolean) option to write database updates to a flat file instead of to the database.
   */
  private class DbFlatFileOutputCommandOption extends CommandOption {
    private final static String help = "Whether to dump database updates to an ad-hoc flat file format.";
    
    public DbFlatFileOutputCommandOption() {
      super("dbFlatFileOutput", 'F', false, "db_flat_file", help);
    }
    
    public void execute() throws Exception {
      boolean bval = getMatched();
      IOOptions ioo = getDbIOOptions();
      
      try {
        ((ChadoAdapter)ioo.getAdapter()).setFlatFileWriteMode(bval);
      } catch (Exception e) {
        logger.fatal("Exception calling setFlatFileWriteMode", e);
        System.exit(1);
      }
    }
  }

  /**
   * Specifies DbXref value to write to Chado database under GFF_source as requested by Scott Cain
   */
  private class GFFSourceCommandOption extends CommandOption {
    private final static String help = "ID to add to DbXref with database of GFF_source when writing to Chado";
    
    public GFFSourceCommandOption() {
      super("GFFSource", 'G', true, "GFF_source", help);
    }
    
    public void execute() throws Exception {
      IOOptions ioo = null;
      try {
        ioo = getDbIOOptions();
      } catch (Exception e) {
        logger.error("Exception in call to getDbIOOptions", e);
      } 
      if (ioo == null) {
        logger.fatal("A GFF_source was specified without using a DB (chadoDB) as input or output");
        parser.usage();
        System.exit(1);//Maybe a bit too brutal
      }
      try {
        ((ChadoAdapter)ioo.getAdapter()).setGFFSource(getArg());
      } catch (Exception e) {
        logger.fatal("Exception calling ", e);
        System.exit(1);
      }
    }
  }
  
  /**
   * Sets whether or not to run Apollo in headless mode
   */
  private class HeadlessCommandOption extends CommandOption {
    private final static String help ="Run Apollo in headless mode." ;
    
    public HeadlessCommandOption() {
      super("headless", 'H', false, "headless mode", help);
    }
    
    public void execute() throws Exception {
      System.setProperty("java.awt.headless", "true"); 
    }
  }
  
  // -----------------------------------------------------------------------
  // IOOptions inner class
  // -----------------------------------------------------------------------

  /** 
   * IOOptions holds state for either read or write 
   */
  private class IOOptions {
    private ApolloDataAdapterI adapter;
    private DataInput dataInput;
    private String filename;
    private String sequenceFilename;
    private boolean specified = false;
    private boolean isRead;
    private DataInputType inputType;

    // database-specific options (currently for chado only)
    private String dbUser = null;
    private String dbPassword = null;
    private boolean dbFlatFileOutput;

    // ----------------------------------------------
    // Constructor
    // ----------------------------------------------

    private IOOptions(boolean isRead) {
      this.isRead = isRead;
    }

    // ----------------------------------------------
    // CommandLine - simple getters/setters
    // ----------------------------------------------

    private void setFilename(String filename) { this.filename = filename; }
    private String getFilename() { return filename; }

    private void setSequenceFilename(String filename) { this.sequenceFilename = sequenceFilename; }
    private String getSequenceFilename() { return sequenceFilename; }

    private void setSpecifiedState(boolean specified) { this.specified = specified; }
    private boolean getSpecifiedState() { return specified; }

    private void setDbUser(String dbUser) { this.dbUser = dbUser;}
    private String getDbUser() { return dbUser;}

    private void setDbPassword(String dbPassword) { this.dbPassword = dbPassword;}
    private String getDbPassword() { return dbPassword;}

    private void setDbFlatFileOutput(boolean dbFlatFileOutput) { this.dbFlatFileOutput = dbFlatFileOutput;}
    private boolean getDbFlatFileOutput() { return dbFlatFileOutput;}    

    // ----------------------------------------------
    // CommandLine
    // ----------------------------------------------

    private void setAdapter(ApolloDataAdapterI adapter) {
      this.adapter = adapter;
    }
    private ApolloDataAdapterI getAdapter() throws Exception {
      if (adapter == null) {
        if (isRead) {
          adapter = inputFmtOption.getAdapter();
        } else {
          adapter = outputFmtOption.getAdapter();
        }
      }

      return adapter;
    }
    private boolean hasAdapter() throws Exception { return getAdapter() != null; }

    /** type of input in list filename (or in future --input) - type only needed for input */
    private void setInputType(String inputTypeString) {
      // actually i think this is adapter dependent - for game want to do 
      // stringToType - for chado its a so type
      //       try {
      //         inputType = DataInputType.stringToType(inputTypeString);
      //       }
      //       catch (UnknownTypeException e) {
      //         System.out.println(e.getMessage()+" Can not set input type");
      //       }
      // for now doing the way chado needs it
      inputType = DataInputType.getDataTypeForSoType(inputTypeString);
    }
    private DataInputType getInputType() { return inputType; }
    private boolean haveInputType() { return inputType != null; }

    private void setDataInput(DataInput dataInput) { this.dataInput = dataInput; }
    private DataInput getDataInput() { return this.dataInput; }

  }

  // -----------------------------------------------------------------------
  // BatchFile inner class
  // -----------------------------------------------------------------------

  /**
   * Used to process a file containing a list of items (gene, regions (Chr_1:1-10000), golden_path, etc...) 
   * to use as input.  Each of the items can be associated with a filename to use for output.
   */
  private class BatchFile {

    /** List of BatchEntry's */
    private List entryList;
    private Iterator batchFileIterator = null;
    private LineNumberReader reader;

    private BatchFile(String batchFilename) throws FileNotFoundException,IOException {
      init(batchFilename);
    }

    private void init(String filename) throws FileNotFoundException, IOException {
      reader = new LineNumberReader(new FileReader(filename));
      parseBatchFile();
    }

    void parseBatchFile() throws FileNotFoundException, IOException {
      String line = reader.readLine(); // IOEx
      while (line != null) {
        BatchEntry entry = new BatchEntry(line);
        if (entry.isValid()) {
          getEntryList().add(entry);
        } else {
          int ln = reader.getLineNumber();
          logger.error("Unable to parse line " + ln + " of batch file: '" + line + "'");
        }
        line = reader.readLine();
      }      
    }

    List getEntryList() {
      if (entryList == null)
        entryList = new ArrayList();
      return entryList;
    }

    private boolean hasMoreBatchEntries() {
      if (entryList == null)
        return false;
      if (batchFileIterator == null)
        batchFileIterator = entryList.iterator();
      if (!batchFileIterator.hasNext()) {
        batchFileIterator = null;
        return false;
      }
      // need to check if next one isnt a skipper (-k)
      return batchFileIterator.hasNext();
    }

    void setToNextBatchEntry() throws Exception {
      BatchEntry entry = getNextBatchEntry();
      logger.info("Loading "+entry.getEntry());
      getReadAdapter().setDataInput(entry.makeDataInput()); // throw ex
      // this is bad - it assumes output is a file - need to generalize at some point
      getWriteOptions().setFilename(entry.getFilename());
      setAdapterForFile(getWriteOptions(), true);
    }

    BatchEntry getNextBatchEntry() {
      while (batchFileIterator.hasNext()) {
        BatchEntry be = (BatchEntry)batchFileIterator.next();
        if (!skipEntry(be))
          return be;
        logger.debug("skipping existing "+be.getFilename());
      }
      return null; // rest of entries were skippers
    }
     
    private boolean skipEntry(BatchEntry entry) {
      if (!skipExistingOutputFile())
        return false;
      String outputFile = entry.getFilename();
      return fileExists(outputFile);
    }

    private boolean fileExists(String filename) {
      return new File(filename).exists();
    }

    private boolean skipExistingOutputFile() {
      return skipExistBatchEntry.getMatched();
    }

    int getLineNumber() { //Set to package visibility to access this from BatchInputFile.BatchEntry
      return reader.getLineNumber(); 
    }
  
    LineNumberReader getReader() {
      return reader;
    }

    // -----------------------------------------------------------------------
    // BatchEntry inner (inner!) class
    // -----------------------------------------------------------------------

    /**
     * Represent a line of the inputFile. this lines specifies the item to get from input and the output file
     */
    class BatchEntry {
      private String entry;
      private String filename;
      private boolean isValid = false;
      private BatchEntry(String line) {
        parseLine(line);
      }
      
      private BatchEntry() {}
      
      void parseLine(String line) {
        String[] splitLine = line.split("\\s+");
        if (splitLine.length == 0) {
          logger.warn("Ignoring line "+getLineNumber()+" failed to parse "+line);
          return;
        }
        String s1 = splitLine[0].trim();
        if (s1 != null && !s1.equals("")) {
          entry = s1;
          isValid = true;
        }
        if (splitLine.length > 1) {
          filename = splitLine[1].trim();
          filename = addCurrentDirIfNoDir(filename);
        }
      }

      boolean isValid() { return isValid; }//Set to package visibility to access this from BatchInputFile.BatchEntry

      String getEntry() { return entry; }//Set to package visibility to access this from BatchInputFile.BatchEntry

      String getFilename() {//Set to package visibility to access this from BatchInputFile.BatchEntry
        if (filename == null)
          filename = makeDefaultFilename();
        return filename;
      }

      String addCurrentDirIfNoDir(String file) {//Set to package visibility to access this from BatchInputFile.BatchEntry
        if (file.indexOf('/') != -1 && file.indexOf('\\') != -1)
          return file; // already has path
        return "./" + file; // unix specific? work on windows?
      }

      private String makeDefaultFilename() {
        return "./" + entry + "." + outputFmtOption.getFileSuffix();
      }
      
      DataInput makeDataInput() {//Set to package visibility to access this from BatchInputFile.BatchEntry
        return new DataInput(getReadOptions().getInputType(),entry);
      }

      void setFilename(String filename) {
        this.filename = filename;
      }

      void setValid(boolean isValid) {
        this.isValid = isValid;
      }
    }
  }

  // -----------------------------------------------------------------------
  // BatchInputFile inner class
  // -----------------------------------------------------------------------

  /**
   * This specialisation of BatchFile allows to process files containing a list of files to use as input.
   *
   * @author cpommier
   */
  private class BatchInputFile extends BatchFile {

    private BatchInputFile(String batchFilename) throws FileNotFoundException, IOException {
      super(batchFilename);
    }
    
    void parseBatchFile() throws FileNotFoundException, IOException {
      String line = getReader().readLine(); // IOEx
      while (line != null) {
        BatchEntry entry = new BatchEntry(line);
        if (entry.isValid())
          getEntryList().add(entry);
        line = getReader().readLine();
      }      
    }
    
    /**
     * Only the input is set here; we assume that the output has already been set.
     * TODO: This is probably highly database-specific and should be generalized 
     * @throws Exception
     */
    void setToNextBatchEntry() throws Exception {
      BatchEntry entry = (BatchInputFile.BatchEntry)getNextBatchEntry();
      logger.info("Loading "+entry.getEntry());
      getReadAdapter().setDataInput(entry.makeDataInput(DataInputType.FILE)); // throw ex
      // Assumes input is a file - need to generalize at some point
      getReadOptions().setFilename(entry.getFilename());
      setAdapterForFile(getReadOptions(), true);
    }
    
    private class BatchEntry extends BatchFile.BatchEntry {

      private BatchEntry(String line) {
        parseLine(line); // Calling super would instantiate the wrong BatchEntry       
      }
      
      private BatchEntry(){
        super();
      }
      
      void parseLine(String line) {
        String[] splitLine = line.split("\\s+");
        if (splitLine.length == 0) {
          logger.warn("Ignoring line "+getLineNumber()+" failed to parse "+line);
          return;
        }
        String s1 = splitLine[0].trim();
        if (s1 != null && !s1.equals("")) {        
          setFilename( addCurrentDirIfNoDir(s1));
          setValid( true);
        }
      }
      
      DataInput makeDataInput(DataInputType dt) {
        return new DataInput(dt,getEntry());
      }
    }
  }
}

