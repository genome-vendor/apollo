package apollo.dataadapter.flychadoxml;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;
import java.net.MalformedURLException;
import apollo.config.Config;
import apollo.config.Style;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.DataInput;
import apollo.dataadapter.DataInputType;
import apollo.dataadapter.Region;
import apollo.dataadapter.chadoxml.ChadoXmlAdapter;
import apollo.dataadapter.gamexml.GAMEAdapter;
import apollo.datamodel.CurationSet;
import org.apache.log4j.*;
import org.bdgp.io.DataAdapterUI;
import org.bdgp.io.IOOperation;
import org.bdgp.util.ProgressEvent;

/** 
 * A FlyBase-specific reader for Chado XML files. 
 * Currently handles only unmacroized Chado XML. */

public class FlyChadoXmlAdapter extends ChadoXmlAdapter {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(FlyChadoXmlAdapter.class);

  /** Must have empty constructor to work with org.bdgp.io.DataAdapterRegistry
      instance creation from config string */
  public FlyChadoXmlAdapter() {
    setName("Chado XML file (FlyBase v1.0, no macros)");
  }

  /** From org.bdgp.io.VisualDataAdapter interface */
  public DataAdapterUI getUI(IOOperation op) {
    // Need to cache UIs for the different operations--otherwise we either
    // recreate them over and over, or (if we try to create a new UI only
    // if it's null) reuse the first one that was created over and over.
    if (!super.operationIsSupported(op))
      return null; // shouldnt happen
    DataAdapterUI ui = super.getCachedUI(op);
    if (ui == null) {
      ui = new FlyChadoXmlAdapterGUI(op);
      super.cacheUI(op,ui);
    }
    return ui;
  }

  /** This is the main method for reading the data. */
  public CurationSet getCurationSet() throws ApolloAdapterException {
    try {
      fireProgressEvent(new ProgressEvent(this, new Double(5.0),
                                          "Finding data..."));
      // Throws DataAdapter exception if faulty input (like bad filename)
      InputStream xml_stream;
      xml_stream = chadoXmlInputStream(getDataInput());
      
      // defined in ChadoXmlAdapter
      CurationSet cs = getCurationSetFromInputStream(xml_stream);
      Config.getStyle().setLocalNavigationManagerEnabled(true);
      return cs;
    } catch (ApolloAdapterException dae) { 
      logger.error("Error while parsing " + getInput(), dae);
      throw dae;
    } catch ( Exception ex2 ) {
      logger.error("Error while parsing " + getInput(), ex2);
      throw new ApolloAdapterException(ex2.getMessage());
    }
  }

  private InputStream chadoXmlInputStream(DataInput dataInput) 
    throws ApolloAdapterException {
    InputStream stream = null;

    DataInputType type = dataInput.getType();
    String input = dataInput.getInputString();

    if (type == DataInputType.FILE) { // could be file or http/url
      stream = getStreamFromFile(input);
    }
    else if (type == DataInputType.URL) {
      logger.debug("xmlInputStream: type is URL");
      URL url = makeUrlFromString(input);
      if (url == null) {
        String message = "Couldn't find URL for " + getInput();
        logger.error(message);
        throw new ApolloAdapterException(message);
      }
      logger.info("Trying to open URL " + input + " to read Chado XML...");
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
      logger.info("Looking up Chado XML data for gene " + input + "...");
      stream = apollo.util.IOUtil.getStreamFromUrl(getURLForGene(input), err, notfound);
      setOriginalFilename((getURLForGene(input)).toString());
    }
    else if (type == DataInputType.CYTOLOGY) {
      String err = "Can't connect to URL for band="+input+"--server not responding.";
      String notfound = "Cytological band " + input + " not found (or server not responding)";
      logger.info("Looking up Chado XML data for band " + input + "...");
      stream = apollo.util.IOUtil.getStreamFromUrl(getURLForBand(input),err, notfound);
      setOriginalFilename((getURLForBand(input)).toString());
    }
//     else if (type == DataInputType.SCAFFOLD) {
//       String err = "Can't connect to URL for scaffold="+input+"--server not responding.";
//       String notfound = "Scaffold " + input + " not found (or server not responding)";
//       stream = apollo.util.IOUtil.getStreamFromUrl(getURLForScaffold(input),err, notfound);
//     }
    else if (type == DataInputType.BASEPAIR_RANGE) {
      String err = "Can't connect to URL for requested region--server not responding.";
      String notfound = "Region " + input + " not found (or server not responding)";
      logger.info("Looking up Chado XML data for range " +  dataInput.getRegion() + "...");
      stream = apollo.util.IOUtil.getStreamFromUrl(getURLForRange(dataInput.getRegion()),err, notfound);
      setOriginalFilename((getURLForRange(dataInput.getRegion())).toString());
    }

    return stream;
  }

  private InputStream getStreamFromFile(String filename) throws ApolloAdapterException {
    InputStream stream=null;
    String path = apollo.util.IOUtil.findFile(filename, false);
    try {
      logger.debug("Trying to open ChadoXML file " + path + " for reading");
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

   /** Retrieve style from config
    *  (Need to do it this way because style hasn't been set yet when we're about
    *  to open a Chado XML file.) */
   private Style getFlyChadoStyle() {
     return Config.getStyle(getClass().getName()); 
   }

  /** make URL from urlString, replace %DATABASE% with selected database */
  public URL makeUrlFromString(String urlString) {
    URL url;
    urlString = fillInDatabase(urlString);
    // cgi just fetches the closest scaffold anyway, not the actual requested
    // region, so no point worrying about padding around requested region.
    //    urlString = fillInPadding(urlString);
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
    String dbField = getFlyChadoStyle().getDatabaseURLField();
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
      return getFlyChadoStyle().getDefaultDatabase();
    }
  }

  public URL getURLForScaffold(String scaffold) {
    String query = getFlyChadoStyle().getScaffoldUrl()+scaffold;
    URL url = makeUrlFromString(query);
    String msg = "Searching for location of scaffold " + scaffold + "...";
    fireProgressEvent(new ProgressEvent(this, new Double(2.0),msg));
    return(url);
  }

  public URL getURLForGene(String gene) {
    // This CGI actually does a redirect to the relevant XML, so the URL we
    // return here will yield the XML when opened.
    String query = getFlyChadoStyle().getGeneUrl()+gene;

    URL url = makeUrlFromString(query);
    String msg = "Searching for scaffold containing gene " + gene + "...";
    fireProgressEvent(new ProgressEvent(this, new Double(2.0),msg));
    return(url);
  }

  public URL getURLForBand(String band) {
    String query = getFlyChadoStyle().getBandUrl() + band; // check config
    URL url = makeUrlFromString(query);
    fireProgressEvent(new ProgressEvent(this, new Double(2.0),
                                        "Searching for scaffold closest to cytological location "+
                                        band + "--please be patient..."));
    return(url);
  }

//   // ! Proof-of-concept method gets the URL for the XML for the scaffold 
//   // that has the best blast hit to the given sequence (uses the fruitfly
//   // blast server)
//   // 2005 No longer used
//   public URL getURLForSequence(String seq) {
//     String query = getFlyChadoStyle().getSeqUrl() + seq;
//     URL url = makeUrlFromString(query);
//     fireProgressEvent(new ProgressEvent(this, new Double(5.0),
//                                         "Running BLAST query--please be patient..."));
//     return(url);
//   }

  /** parse range string "Chr 2L 10000 20000" -> "2L:10000:20000"   */
  public URL getURLForRange(Region region) {
    // in GameSynteny case already in colon format! - check for this
    String rangeForUrl = region.getColonDashString();
    String query = getFlyChadoStyle().getRangeUrl() +rangeForUrl;
    URL url = makeUrlFromString(query);
    fireProgressEvent(new ProgressEvent(this,new Double(5),
                                        "Searching for scaffold that overlaps region "+rangeForUrl + "..."));
    return url;
  }

}
