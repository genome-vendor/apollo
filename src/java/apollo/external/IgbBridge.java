package apollo.external;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import apollo.config.Config;
import apollo.datamodel.GenomicRange;
import apollo.datamodel.SeqFeatureI;
import apollo.gui.event.FeatureSelectionEvent;
import apollo.gui.event.FeatureSelectionListener;
import apollo.gui.menus.LinksMenu;
import apollo.util.HTMLUtil;

import org.apache.log4j.*;

/** Gets selection events from apollo - via controller, set up by CuiCurationState, 
    configured in style - sends selection to igb via http */

public class IgbBridge implements FeatureSelectionListener {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(IgbBridge.class);
  private final static int FIRST_PORT_TO_TRY = 7085;
  private final static int NUMBER_OF_PORTS_TO_TRY = 5;

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private String chromosome;
  private String organism;
  private int igbPort = -1;

  public IgbBridge(GenomicRange genRng) {
    setGenomicRange(genRng);
  }
  
  public void setGenomicRange(GenomicRange genRng) {
    chromosome = genRng.getChromosome();
    organism = genRng.getOrganism();
  }

  public boolean handleFeatureSelectionEvent(FeatureSelectionEvent evt) {
    
    // is it cheesy to query LinksMenu directly? i think its ok
    if (!LinksMenu.igbLinksEnabled())
      return false;

    // only communicate with IGB when Apollo is in DEBUG mode
    // TODO - introduce a parameter that controls this feature, instead of using global DEBUG (deprecated)
    if (apollo.config.Config.DEBUG) {

      if (!igbIsRunning()) {
        logger.debug("can't send off selection to IGB - it's not running");
        return false;
      }

      SeqFeatureI selectedFeat = evt.getFeature(); // just 1st feat for now
      if (selectedFeat == null)
        return false;
      
      try {
        URL url = makeRegionUrl(selectedFeat);

        logger.debug("Connecting to IGB with URL "+url);
        URLConnection conn = url.openConnection();
        conn.connect(); // throws IOException if not found
        // for some reason, need to get input stream and close it to trigger
        //    "response" at other end...
        conn.getInputStream().close();
      }
      catch (MalformedURLException me) {
        logger.debug("malformed url", me);
        return false; // ??
      }
      catch (IOException ie) {
        logger.error("unable to connect to igb", ie);
        return false;
      }
      //HTMLUtil.loadIntoBrowser(u);
      
    }
    return true;
  }

  private void connectToUrl(URL url) {

  }

  private boolean igbIsRunning() {
    findIgbLocalhostPort(); // sets igbPort if found
    return igbPort != -1;
  }

  /** sets igbPort with port # */
  private void findIgbLocalhostPort() {
    // actually relook for port every time - as igb couldve gone down in interim
    //if (igbPort != -1)  return;
    // is this overkill?
    for (int i=0; i < NUMBER_OF_PORTS_TO_TRY && igbPort == -1; i++) {
      int port = FIRST_PORT_TO_TRY + i;
      try {
        URL url = makePingUrl(port);
        URLConnection conn = url.openConnection();
        conn.connect(); // throws IOException if not found
        logger.debug("Found an igb port "+port);
        igbPort = port; // no exception - we found a port - set igbPort
      }
      catch (MalformedURLException e) { 
        logger.error("malformed url");
        return; // ??
      }
      catch (IOException e) {
        logger.error("No port found at "+port, e);
      }
    }
  }
  
  private URL makeRegionUrl(SeqFeatureI selectedFeat) throws MalformedURLException {
    int padding = 400; // eventually config this
    int low = selectedFeat.getLow() - padding;
    if (low < 1)
      low = 1; // 0? interbase or base oriented? 
    int high = selectedFeat.getHigh() + padding;
    String urlPrefix = makeUrlPrefix();
    // is http start end low/high or 5'/3'?
    String u = urlPrefix + "seqid=chr" + chromosome + 
      "&start=" + low + "&end=" + high + "&version=D_melanogaster_Apr_2004 ";
    URL url = new URL(u); // throws MalformedURLException
    return url;
  }


  private URL makePingUrl(int port) throws MalformedURLException {
    String pingString = makeUrlPrefix(port) + "ping";
    logger.debug("pinging "+pingString);
    return new URL(pingString);
  }

  private String makeUrlPrefix() {
    return makeUrlPrefix(igbPort);
  }

  private String makeUrlPrefix(int port) {
    return "http://localhost:"+port+"/UnibrowControl?";
  }

}
