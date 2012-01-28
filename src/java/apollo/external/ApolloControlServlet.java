package apollo.external;

import java.util.Map;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import apollo.datamodel.CurationSet;
import apollo.dataadapter.Region;
import apollo.config.Config;
import apollo.gui.Controller;
import apollo.gui.event.BaseFocusEvent;
import apollo.gui.synteny.CurationManager;
import apollo.gui.synteny.GuiCurationState;

import org.apache.log4j.*;

/** take in http request for a location and load it - 
 *   for now hardwired to game adapter loading fly 

 *    actually i dont think thats true anymore - i think this just scrolls & zooms to
 * the region if its within range, rather than causing a whole reload as reloading in
 * apollo isnt zippy like igb so it would be too laggy

 *   url that it responds to looks something like this
 *  http://localhost:8085/ApolloControl?seqid=chr2R&start=10000&end=12000
 *  maybe add ?organism=dmel
*/

public class ApolloControlServlet extends HttpServlet {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ApolloControlServlet.class);

  public void service(HttpServletRequest request, HttpServletResponse response) {

    logger.info("ApolloControlServlet received request");
    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    if (request.getParameter("ping") != null) {
      // query to see if port is occupied...
      logger.info("received ping request");
      return;
    }
    Map map = request.getParameterMap();
    loadFromRequestMap(map);
  }

  private void loadFromRequestMap(Map requestMap) {
    try {
      Region region = getHttpRegion(requestMap);
      logger.debug("got request for "+region);
      // puts region in bounds, throws ex if entirely out of bounds
      checkRegion(region);
      selectRegionInApollo(region);
    }
    catch (ParameterException e) {
      logger.error("Selecting http request failed.", e);
      // popup...
    }
  }

  private String getChromosome(Map requestMap) throws ParameterException {
    String chromosome = getStringParameter(requestMap,"seqId");
    if (chromosome == null)
      throw new ParameterException("No chromosome specified");

    // strip off chr prefix
    if (chromosome.startsWith("chr"))
      chromosome = chromosome.substring(3);
    return chromosome;
  }

  private void checkChromosome(String httpChrom) throws ParameterException {
    String chr = getCurSet().getChromosome();
    if (!httpChrom.equals(chr)) {
      throw new ParameterException("Requested chromosome "+httpChrom+" is not "+
                                   "currently active("+chr+")");
    }
  }

  private void checkRegion(Region region) throws ParameterException {
    checkChromosome(region.getChromosome()); // throws ex if wrong chrom
    int curStart = getCurSet().getStart();
    int curEnd = getCurSet().getEnd();
    if (region.getEnd() < curStart || region.getStart() > curEnd)
      throw new ParameterException("Region "+region+" outside of current region");
    if (region.getStart() < curStart)
      region.setStart(curStart);
    if (region.getEnd() > curEnd)
      region.setEnd(curEnd);
  }

  private Region getHttpRegion(Map requestMap) throws ParameterException {
    String chrom = getChromosome(requestMap);
    int start = getStart(requestMap);
    int end = getEnd(requestMap);
    return new Region(chrom,start,end);
  }

  private int getStart(Map requestMap) throws ParameterException {
    return getNumber(requestMap,"start");
  }

  private int getEnd(Map requestMap) throws ParameterException {
    return getNumber(requestMap,"end");
  }

  private int getNumber(Map requestMap, String key) throws ParameterException {
    String numString = getStringParameter(requestMap,key);
    if (numString == null)
      throw new ParameterException("No "+key+" specified");
    logger.debug("got "+key+" with value "+numString);
    try {
      return Integer.parseInt(numString);
    } 
    catch (NumberFormatException e) {
      throw new ParameterException(key+" is not a number");
    }
  }

  private String getStringParameter(Map map, String key) {
    Object o = map.get(key);
    if (o == null)
      return null;
    if (o instanceof String)
      return (String)o;
    // the params actually come in as arrays - not sure why, or how to get more
    // than 1 string array
    if (o instanceof String[])
      return ((String[]) o)[0];
    return o.toString();
  }

  private void selectRegionInApollo(Region region) {
    // ??? - check out zoom to selected & BaseFocusEvent...
    // also query apollo for its current range - see if in range 
    // and query chrom
    BaseFocusEvent bfe = new BaseFocusEvent(this,region);
    //Controller.getMasterController().handleBaseFocusEvent(bfe);
    getCurState().getController().handleBaseFocusEvent(bfe);
  }

  private GuiCurationState getCurState() {
    return CurationManager.getActiveCurationState();
  }

  private CurationSet getCurSet() {
    return getCurState().getCurationSet();
  }



  private class ParameterException extends Exception {
    private ParameterException(String m) { super(m); }
  }

}
