package apollo.dataadapter.ensj19;

import java.util.*;
import java.io.*;
import apollo.seq.io.*;
import apollo.datamodel.*;
import apollo.config.Config;
import apollo.dataadapter.*;
import apollo.dataadapter.ensj.*;

import org.apache.log4j.*;
import org.bdgp.io.*;
import org.bdgp.util.*;
import java.util.Properties;
import org.bdgp.swing.widget.*;
import org.apache.log4j.*;

import org.ensembl19.*;
import org.ensembl19.driver.*;
import org.ensembl19.datamodel.Accessioned;
import org.ensembl19.datamodel.Location;
import org.ensembl19.datamodel.LinearLocation;
import org.ensembl19.datamodel.AssemblyLocation;
import org.ensembl19.datamodel.CloneFragmentLocation;
import org.ensembl19.datamodel.CloneFragment;
import org.ensembl19.datamodel.DnaProteinAlignment;
import org.ensembl19.datamodel.PredictionTranscript;
import org.ensembl19.datamodel.PredictionExon;
import org.ensembl19.datamodel.SimplePeptideFeature;
import org.ensembl19.datamodel.Feature;
import org.ensembl19.datamodel.RepeatFeature;
import org.ensembl19.util.*;

import apollo.dataadapter.otter.parser.*;

import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import java.net.*;

public class AnnotationEnsJAdapter extends EnsJAdapter {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(AnnotationEnsJAdapter.class);

  private static boolean DEBUG=false;

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  //  private AnnotationEnsJAdapterGUI theAdapterGUI;
  private InputStream inputStream;
  private OutputStream outputStream;
  private String inputFileName;
  private String outputFileName;
  private String inputServerName;
  private String serverPort;
  private String outputServerName;
  private String inputDataSet;
  private String outputDataSet;
  private String author;
  private String authorEmail;
  private String lock;//is a lock on the annotations requestsed?
  //  private CurationSet curationSet;

  IOOperation [] supportedOperations =
    {
      ApolloDataAdapterI.OP_READ_DATA,
      ApolloDataAdapterI.OP_READ_SEQUENCE,
      ApolloDataAdapterI.OP_APPEND_DATA,
      ApolloDataAdapterI.OP_WRITE_DATA
    };


  public IOOperation [] getSupportedOperations(){
    return supportedOperations;
  }

  /**
   * This is an arcane requirement to get the File->Save menu to light up.
  **/
  public DataInputType getInputType() {
    return DataInputType.FILE;
  }

  public String getName() {
    return "Ensembl/Otter - direct database access for multiple species";
  }


  public InputStream getInputStream(){
    return inputStream;
  }//end getInputStream

  public void setInputStream(InputStream newValue){
    inputStream = newValue;
  }//end setInputStream


  public OutputStream getOutputStream(){
    return outputStream;
  }//end getOutputStream

  public void setOutputStream(OutputStream newValue){
    outputStream = newValue;
  }//end setOutputStream

  /**
   * We will either draw our sequence from the main database, or from
   * another sequence database, if it was passed in.
  **/
  public SequenceI getSequence(DbXref dbxref) throws apollo.dataadapter.ApolloAdapterException {

    Properties sequenceProperties = new Properties();
    Iterator names = getStateInformation().keySet().iterator();
    String name;
    String value;

    while(names.hasNext()){
      name = (String)names.next();
      if(name.startsWith("sequence.")){
        value = getStateInformation().getProperty(name);
        name = name.substring(9);
        sequenceProperties.put(name, value);
      }//end if
    }//end while

    if (dbxref.getIdValue().equals(getRegion())) {

      try {
        logger.info("Getting sequence");

        logger.info("Region " + getRegion());
        logger.info("Location " + getLocation());
        AlternateEnsJSequence ejs =
          new AlternateEnsJSequence(
            getRegion().toString(),
            Config.getController(),
            (LinearLocation)getLocation(),
            sequenceProperties
          );

        ejs.getCacher().setMaxSize(1000000);

        return ejs;
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
        throw new apollo.dataadapter.ApolloAdapterException( e.getMessage() );
      }

    } else {

      throw new NotImplementedException();
    }

  }//end getSequence

  private AnnotatedFeatureI getXMLAndParse(CurationSet curationSet)
  throws apollo.dataadapter.ApolloAdapterException {
    XMLReader parser;
    OtterContentHandler handler;
    InputSource theFileReader;
    //    AnnotatedFeatureI theSet;
    LinearLocation location = null;
    AssemblyLocation assemblyLocation = null;
    String serverQueryString;
    URL inputURL;
    LocationConverter locationConverter;
    //    CloneFragmentLocation cloneFragmentLocation;

    //Set up the annotation server stuff.
    if(getInputServerName() != null && 
       getInputDataSet() != null && 
       getServerPort() != null){

      //request looks like:
      //http://ecs1d.sanger.ac.uk:19332/perl/get_region?chr=6&chrstart=1&chrend=100000&dataset=human
      //
      try{
        location = getLocation();
      }catch(ConfigurationException exception){
        throw new apollo.dataadapter.ApolloAdapterException("Location not valid "+location);
      }catch(AdaptorException exception){
        throw new apollo.dataadapter.ApolloAdapterException("Location not valid "+location);
      }//end try

      try{

        if ( getMode() == ASSEMBLY_LOCATION_MODE || 
             getMode() == STABLE_ID_MODE) {

          assemblyLocation = (AssemblyLocation)getLocation();

        } else if ( getMode() == CLONE_FRAGMENT_MODE ) {

          locationConverter = (LocationConverter)getDriver().getAdaptor("location_converter");
          assemblyLocation =
            (AssemblyLocation)locationConverter.convert(
              getLocation(),
              AssemblyLocation.DEFAULT_MAP,
              false,
              false
            );

        }//end if

      }catch(org.ensembl19.driver.ConfigurationException exception){
        throw new apollo.dataadapter.ApolloAdapterException(
          "Couldn't find an assembly location for input clone ",
          exception
        );
      }catch(org.ensembl19.driver.AdaptorException exception){
        throw new apollo.dataadapter.ApolloAdapterException(
          "Couldn't find an assembly location for input clone ",
          exception
        );
      }//end try

      serverQueryString =
        "http://"+getInputServerName()+
        ":"+getServerPort()+"/perl/get_region?chr="+assemblyLocation.getChromosome()+"&"+
        "chrstart="+assemblyLocation.getStart()+"&"+
        "chrend="+assemblyLocation.getEnd()+"&"+
        "dataset="+inputDataSet;

      if(getAuthor() != null){
        serverQueryString += "&author="+getAuthor();
      }

      if(getAuthorEmail() != null){
        serverQueryString += "&email="+getAuthorEmail();
      }

      if(getLock() != null && getLock().equals("true")){
        serverQueryString += "&lock="+"true";
      }

      try{
        inputURL = new URL(serverQueryString);
        if(!DEBUG){
          setInputStream(inputURL.openStream());
        }else{
          DataInputStream inputStream = new DataInputStream(inputURL.openStream());
          String line = inputStream.readLine();
          String outputString = "";
          while(line != null){
            outputString += line+"\n";
            line = inputStream.readLine();
          }//end while

          logger.debug(serverQueryString);
          logger.debug(outputString);

          setInputStream(new StringBufferInputStream(outputString));
        }//end if
      }catch(IOException exception){
        throw new NonFatalDataAdapterException("Unable to open input URL "+serverQueryString);
      }//end try

    }else if(getInputFileName() != null){

      try{
        setInputStream(new FileInputStream(getInputFileName()));
      }catch(FileNotFoundException theException){
        throw new NonFatalDataAdapterException("File "+getInputFileName()+" not found");
      }//end try
    }else{
      //
      //This should not be triggered: you can't get into this method unless you
      //either have complete server information, or input file information
      throw new apollo.dataadapter.ApolloAdapterException(
        "Attempt to fetch annotations without specifying input server URL or file name"
      );
    }//end if

    try{
      parser = XMLReaderFactory.createXMLReader();
      handler = new OtterContentHandler();
      handler.setCurationSet(curationSet);
      theFileReader = new InputSource(getInputStream());
      parser.setContentHandler(handler);
      parser.parse(theFileReader);
    }catch(IOException theException){
      logger.error(theException.getMessage(), theException);
      throw new apollo.dataadapter.ApolloAdapterException("IO Error parsing input xml-stream", theException);
    }catch(SAXException theException){
      logger.error(theException.getMessage(), theException);
      throw new apollo.dataadapter.ApolloAdapterException("SAX Error parsing input xml-stream", theException);
    }//end try

    Iterator returnedObjects = handler.getReturnedObjects().iterator();
    String message;
    Object returnedObject;
    AnnotatedFeatureI set = null;

    while(returnedObjects.hasNext()){

      returnedObject = returnedObjects.next();

      if(returnedObject instanceof String){
        message = (String)returnedObject;
        throw new NonFatalDataAdapterException(message);
      }else if(returnedObject instanceof AnnotatedFeatureI){
        set = (AnnotatedFeatureI)returnedObject;
      }//end if
    }//end while

    return set;
  }//end getXMLAndParse

  private FeatureSetI convertCurationSetToApolloFeatureSet(AnnotatedFeatureI theSet)
  throws apollo.dataadapter.ApolloAdapterException {
    StrandedFeatureSet returnSet;
    Iterator featureIterator;
    SeqFeatureI theResultFeature;
    int low = -1; //lower bound for annotation set
    int high = -1; //upper bound for annotation set
    FeatureSet theForwardFakeSet = new FeatureSet();
    FeatureSet theReverseFakeSet = new FeatureSet();
    // theForwardFakeSet.setHolder(true);
    // theReverseFakeSet.setHolder(true);

    returnSet =
      new StrandedFeatureSet(
        new FeatureSet(),
        new FeatureSet()
      );

    if(theSet != null && theSet.getFeatures() != null){
      featureIterator = theSet.getFeatures().iterator();

      while(featureIterator.hasNext()){
        theResultFeature = (SeqFeatureI)featureIterator.next();
        if(low > theResultFeature.getLow() || low < 0){
          low = theResultFeature.getLow();
        }
        if(high < theResultFeature.getHigh() || high < 0){
          high = theResultFeature.getHigh();
        }

        if(theResultFeature instanceof AssemblyFeatureI){
          theResultFeature.setFeatureType("otter_assembly");

          if(theResultFeature.getStrand() == 1){
            theForwardFakeSet.addFeature(theResultFeature);
            theForwardFakeSet.setStrand(theResultFeature.getStrand());
          }else{
            theReverseFakeSet.addFeature(theResultFeature);
            theReverseFakeSet.setStrand(theResultFeature.getStrand());
          }
        }else{

          theResultFeature.setFeatureType("otter");
          returnSet.addFeature(theResultFeature);

        }//end if

        returnSet.addFeature(theForwardFakeSet);
        returnSet.addFeature(theReverseFakeSet);
      }//end while

      returnSet.setLow(low);
      returnSet.setHigh(high);
      // returnSet.setHolder(true);
    }//end if

    return returnSet;
  }//end convertCurationSetToApolloFeatureSet

  /** need to cache guis. getUI called 6 times by the chooser (who knows why)
      for each load - that means if its not cached the gui gets recreated 6 times
      inefficient! so its now cached */
  public DataAdapterUI getUI(IOOperation op) {
    if (!super.operationIsSupported(op))
      return null; // shouldnt happen
    DataAdapterUI ui = super.getCachedUI(op);
    if (ui == null) {
      ui = new AnnotationEnsJAdapterGUI(op);
      super.cacheUI(op,ui);
    }
    return ui;
  }

  /**
   * Write the (now edited) curation set back to the otter server or
   * file, depending on what was nominated.
  **/
  public void commitChanges(CurationSet curationSet)
  throws apollo.dataadapter.ApolloAdapterException {
    BufferedOutputStream buffer = new BufferedOutputStream(getOutputStream());
    OutputStreamWriter writer = new OutputStreamWriter(buffer);
    OtterXMLRenderingVisitor visitor = new OtterXMLRenderingVisitor();
    StrandedFeatureSet theSet = (StrandedFeatureSet)curationSet.getAnnots();
    curationSet.accept(visitor);
    String outputString = visitor.getReturnBuffer().toString();
    CGI cgi;
    Hashtable inputParameters = new Hashtable();
    String host;
    DataInputStream inputStream;
    String line;
    File outputFile;
    String serverResponse="";
    int port;

    try{

      //
      //If you have specified an output file name, annotations will be
      //written to that file regardless of any server communication.
      if(getOutputFileName() != null){
        outputFile = new File(outputFileName);
      }else{
        String apolloRoot = System.getProperty("APOLLO_ROOT");
        String timestamp = new java.sql.Timestamp(System.currentTimeMillis()).toString();
        String fileName = apolloRoot+"/data/apollo_annotations_"+timestamp+"+.xml";
        outputFile = new File(fileName);
      }//end if

      if(outputFile.exists()){
        outputFile.delete();
      }//end if

      try{
        outputFile.createNewFile();
        setOutputStream(new FileOutputStream(outputFile));
      }catch(IOException theException){
        throw new apollo.dataadapter.ApolloAdapterException("Unable to create output file "+outputFileName);
      }//end try

      buffer = new BufferedOutputStream(getOutputStream());
      writer = new OutputStreamWriter(buffer);
      writer.write(outputString);
      writer.flush();

      if(getOutputServerName()!= null){

        host = getOutputServerName();
        inputParameters.put("data",outputString);
        inputParameters.put("author", getAuthor());
        inputParameters.put("email", getAuthorEmail());
        inputParameters.put("unlock", "true");
        inputParameters.put("dataset", getOutputDataSet());

        logger.debug(outputString);

        try{
          port = Integer.valueOf(getServerPort()).intValue();
        }catch(NumberFormatException exception){
          throw new apollo.dataadapter.ApolloAdapterException("Port :"+getServerPort()+" is not a valid integer");
        }//end try

        cgi = new CGI(host, port, "perl/write_region", inputParameters, System.out, "POST");
        cgi.run();
        inputStream = cgi.getInput();

        if(inputStream != null){
          while((line = inputStream.readLine()) != null){
            serverResponse += line+"\n";
          }//end while

          if(serverResponse.length()>0){
            processResponse(serverResponse, outputString);

            if(serverResponse.indexOf("ERROR")<=0){
              setLock("false");
            }//end if
          }//end if
        }else{
          writeBackupFileIfNecessary("There has been a Server Error", outputString);
        }
      }

    }catch(IOException theException){

      writeBackupFileIfNecessary(theException.getMessage(), outputString);
      throw new apollo.dataadapter.ApolloAdapterException(
        "Error writing annotations",
        theException
      );

    }//end try
  }

  /**
   * If the response was "clone locked", then we need to allow the user
   * a chance to write the annotations out (if they already haven't done so).
  **/
  private void processResponse(
    String response,
    String annotations
  ) throws apollo.dataadapter.ApolloAdapterException{
    int isErrorPresent = response.indexOf("ERROR");

    if(
      isErrorPresent > 0
    ){
      writeBackupFileIfNecessary(response, annotations);
    }else{
      javax.swing.JOptionPane.showMessageDialog(
        null,
        response
      );
    }//end if
  }//end processResponse

  private void writeBackupFileIfNecessary(String response, String annotations)throws apollo.dataadapter.ApolloAdapterException{
    String apolloRoot = System.getProperty("APOLLO_ROOT");
    String timestamp = new java.sql.Timestamp(System.currentTimeMillis()).toString();
    String fileName = apolloRoot+"/data/apollo_annotations_"+timestamp+"+.xml";

    writeBackupFile(fileName, annotations);

    if(getOutputFileName() == null){
      javax.swing.JOptionPane.showMessageDialog(
        null,
        "The following error occurred: \n"+response+
        "\n But you have not been writing output to a file. \n"+
        "Your annotations have been written to "+fileName
      );
    }else{
      javax.swing.JOptionPane.showMessageDialog(
        null,
        "The following error occurred: \n"+response+
        "\n Your output was written to the file: "+
        getOutputFileName()
      );
    }//end if
  }//end writeBackupFileIfNecessary

  /**
   * In addition to superclass processing,
   * We will be passed otter-i/o file names. Create file handles from these.
  **/
  public void setStateInformation(Properties stateInformation) {
    //
    //most importantly - sets the range of the request, initialises the
    //database driver etc.
    super.setStateInformation(stateInformation);

    setWritingStateInformation(stateInformation);
  }//end setStateInformation

  public void setWritingStateInformation(Properties stateInformation){
    String inputFileName    = trim(stateInformation.getProperty(StateInformation.INPUT_FILE_NAME));
    String outputFileName   = trim(stateInformation.getProperty(StateInformation.OUTPUT_FILE_NAME));
    String inputServerName  = trim(stateInformation.getProperty(StateInformation.INPUT_SERVER_NAME));
    String serverPort       = trim(stateInformation.getProperty(StateInformation.PORT));
    String outputServerName = trim(stateInformation.getProperty(StateInformation.OUTPUT_SERVER_NAME));
    String inputDataSet     = trim(stateInformation.getProperty(StateInformation.INPUT_DATA_SET));
    String outputDataSet    = trim(stateInformation.getProperty(StateInformation.OUTPUT_DATA_SET));
    String author           = trim(stateInformation.getProperty(StateInformation.AUTHOR));
    String authorEmail      = trim(stateInformation.getProperty(StateInformation.AUTHOR_EMAIL));
    String lock             = trim(stateInformation.getProperty(StateInformation.LOCK));

    //    URL inputURL = null;
    //    File outputFile;
    //    URL outputURL = null;
    //    String serverQueryString;
    //    String region;
    //    LinearLocation location = null;
    //    URLDiagnosticTool tool;
    //    String responseString;
    //    InputStream stream;
    //    String diagnosticConfigurationOption;
    int serverPortNumber;

    if(Config.isEditingEnabled(getClass().getName())){

      try{
        serverPortNumber = Integer.valueOf(serverPort).intValue();
      }catch(NumberFormatException exception){
        if(
          inputServerName != null &&
          inputDataSet != null
        ){
          throw new NonFatalDataAdapterException("Server port: "+serverPort+" is not a valid number");
        }//end if
      }//end try

      if(inputServerName != null){
        setInputServerName(inputServerName);
      }
      
      if(author != null){
        setAuthor(author);
      }
      
      if(authorEmail != null){
        setAuthorEmail(authorEmail);
      }
      
      if(lock != null){
        setLock(lock);
      }
      
      if(outputDataSet != null){
        setOutputDataSet(outputDataSet);
      }
      
      if(outputServerName != null){
        setOutputServerName(outputServerName);
      }
      
      if(inputFileName != null){
        setInputFileName(inputFileName);
      }
      
      if(outputFileName != null){
        setOutputFileName(outputFileName);
      }
      
      if(inputDataSet != null){
       setInputDataSet(inputDataSet);
      }
      
      if(outputDataSet != null){
        setOutputDataSet(outputDataSet);
      }
      
      if(serverPort != null){
       setServerPort(serverPort);
      }
    }//end if
  }

  public void clearStateInformation(){
    super.clearStateInformation();
    setInputServerName(null);
    setAuthor(null);
    setAuthorEmail(null);
    setLock(null);
    setOutputDataSet(null);
    setOutputServerName(null);
    setInputFileName(null);
    setOutputFileName(null);
    setInputDataSet(null);
    setOutputDataSet(null);
    setServerPort(null);
  }//end if
    
  private String trim(String input){
    if(input != null && input.trim().length() >0){
      return input;
    }else{
      return null;
    }
  }

  private void setAuthor(String newValue){
    author = newValue;
  }

  private void setAuthorEmail(String newValue){
    authorEmail = newValue;
  }

  private void setLock(String newValue){
    lock = newValue;
  }

  private void setOutputServerName(String newValue){
    outputServerName = newValue;
  }

  private String getOutputServerName(){
    return outputServerName;
  }

  private String getAuthor(){
    return author;
  }

  private String getAuthorEmail(){
    return authorEmail;
  }

  private String getLock(){
    return lock;
  }

  private String getInputServerName(){
    return inputServerName;
  }

  private void setInputServerName(String newValue){
    inputServerName = newValue;
  }

  private String getServerPort(){
    return serverPort;
  }

  private void setServerPort(String port){
    serverPort = port;
  }

  private String getInputFileName(){
    return inputFileName;
  }

  private void setInputFileName(String newValue){
    inputFileName = newValue;
  }

  private String getOutputFileName(){
    return outputFileName;
  }

  private void setOutputFileName(String newValue){
    outputFileName = newValue;
  }

  private String getInputDataSet(){
    return inputDataSet;
  }

  private void setInputDataSet(String newValue){
    inputDataSet = newValue;
  }

  private String getOutputDataSet(){
    return outputDataSet;
  }

  private void setOutputDataSet(String newValue){
    outputDataSet = newValue;
  }

  /**
   * There are various times we might need to write the annotations out to
   * a file.
  **/
  private void writeBackupFile(
    String fileName,
    String outputString
  ) throws apollo.dataadapter.ApolloAdapterException {
    File outputFile = new File(fileName);
    BufferedOutputStream buffer;
    OutputStreamWriter writer;

    if(outputFile.exists()){
      outputFile.delete();
    }//end if

    try{
      outputFile.createNewFile();
      buffer =
        new BufferedOutputStream(
          new FileOutputStream(outputFile)
        );
      writer = new OutputStreamWriter(buffer);
      writer.write(outputString);
      writer.flush();
    }catch(IOException theException){
      throw new apollo.dataadapter.ApolloAdapterException(
        "Attempt to write annotations to a file failed:\n"+
        theException.getMessage()
      );
    }//end try

  }

  protected boolean rollbackAnnotations(CurationSet curationSet){
    BufferedOutputStream buffer = new BufferedOutputStream(getOutputStream());
    //    OutputStreamWriter writer = new OutputStreamWriter(buffer);
    OtterXMLRenderingVisitor visitor = new OtterXMLRenderingVisitor();
    StrandedFeatureSet theSet = (StrandedFeatureSet)curationSet.getAnnots();
    theSet.accept(visitor);
    String outputString = visitor.getReturnBuffer().toString();
    CGI cgi;
    Hashtable inputParameters = new Hashtable();
    String host;
    DataInputStream inputStream;
    String line;
    String serverResponse="";
    int port;
    boolean success = true;

    try{

      if(getOutputServerName()!= null && getLock().equals("true")){

        host = getOutputServerName();
        inputParameters.put("data",outputString);
        inputParameters.put("author", getAuthor());
        inputParameters.put("email", getAuthorEmail());
        inputParameters.put("unlock", "true");
        inputParameters.put("dataset", getOutputDataSet());

        logger.debug(outputString);

        try{
          port = Integer.valueOf(getServerPort()).intValue();
        }catch(NumberFormatException exception){
          throw new apollo.dataadapter.ApolloAdapterException("Port :"+getServerPort()+" is not a valid integer");
        }

        cgi = new CGI(host, port, "perl/unlock_region", inputParameters, System.out, "POST");
        cgi.run();
        inputStream = cgi.getInput();

        if(inputStream != null){
          while((line = inputStream.readLine()) != null){
            serverResponse += line+"\n";
          }//end while

          if(serverResponse.indexOf("ERROR") >0){
            success = false;
            processResponse(serverResponse, outputString);
          }//end if
        }else{
          success = false;
          //
          //Disable this for a minute: it turns out, right now, that the
          //server is not returning ANY input stream...
          //writeBackupFileIfNecessary("There has been a Server Error", outputString);
        }
      }

    }catch(apollo.dataadapter.ApolloAdapterException theException){
      //
      //I can't re-throw an exception here! There is no machinery to catch it from the
      //parts of Apollo doing the invocation.
      success = false;
    }catch(IOException theException){
      //
      //I can't re-throw an exception here! There is no machinery to catch it from the
      //parts of Apollo doing the invocation.
      success = false;
    }//end try

    return true;
  }//end   rollbackAnnotations

  public Boolean addToCurationSet() throws apollo.dataadapter.ApolloAdapterException {
    //data already existing
    CurationSet existingCurationSet = getExistingCurationSet();

    //should pull out features based on current state
    CurationSet newCurationSet = super.getCurationSetWithoutClearingData();
    Iterator features = newCurationSet.getResults().getFeatures().iterator();
    while(features.hasNext()){
      existingCurationSet.getResults().addFeature((SeqFeatureI)features.next());
    }

    features = newCurationSet.getAnnots().getFeatures().iterator();
    while(features.hasNext()){
      existingCurationSet.getAnnots().addFeature((SeqFeatureI)features);
    }

    return Boolean.TRUE;
  }

  public CurationSet getExistingCurationSet(){
    return curation_set; //stored on superclass.
  }//end getExistingCurationSet
}





