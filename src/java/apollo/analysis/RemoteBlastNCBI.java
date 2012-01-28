package apollo.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import apollo.datamodel.CurationSet;
import apollo.datamodel.SequenceI;

/** Sends and retrieves a BLAST request to NCBI's qBLAST service.
 * 
 * @author elee
 *
 */

public class RemoteBlastNCBI {

  private static final String ENCODING = "UTF-8";
  private static final String BLAST_URL = "http://www.ncbi.nlm.nih.gov/blast/Blast.cgi?";
  private static final int SLEEP = 3000;
  private static final Pattern RID_PATTERN = Pattern.compile("^\\s*RID\\s*=\\s*(\\w+)$");
  private static final Pattern RTOE_PATTERN = Pattern.compile("^\\s*RTOE\\s*=\\s*(\\d+)$");
  private static final Pattern STATUS_PATTERN = Pattern.compile("^\\s*Status\\s*=\\s*(\\w+)$");
  
  /** Type of BLAST analysis to run.
   * 
   */
  public enum BlastType
  {
    blastn,
    blastx,
    tblastx
  }

  /** Options for running qBLAST.
   * 
   */
  public static class BlastOptions
  {

    private final static int FILTER_LOW_COMPLEXITY = 0x1;
    private final static int FILTER_HUMAN_REPEATS = 0x2;
    private final static int FILTER_MASK_LOOKUP = 0x4;
    
    private int geneticCode;
    private int filter;
    private int gapOpenCost;
    private int gapExtendCost;
    private int numberOfHits;
    
    /** Constructor.
     * 
     */
    public BlastOptions() {
      geneticCode = 0;
    }

    /** Get the organism's genetic code.
     * 
     * @return organism's genetic code
     */
    public int getGeneticCode()
    {
      return geneticCode;
    }
    
    /** Set the organism's genetic code.
     * 
     * @param geneticCode - organism's genetic code
     */
    public void setGeneticCode(int geneticCode)
    {
      this.geneticCode = geneticCode;
    }
    
    /** Checks whether low complexity filtering is enabled.
     * 
     * @return true if low complexity filtering is enabled
     */
    public boolean isSetFilterLowComplexity()
    {
      return (filter & FILTER_LOW_COMPLEXITY) != 0;
    }
    
    /** Set low complexity filtering.
     * 
     * @param lowComplexityFiltering - whether to enable low complexity filtering
     */
    public void setFilterLowComplexity(boolean lowComplexityFiltering)
    {
      if (lowComplexityFiltering) {
        filter |= FILTER_LOW_COMPLEXITY;
      }
      else {
        filter &= ~FILTER_LOW_COMPLEXITY;
      }
    }

    /** Checks whether human repeat filtering is enabled.
     * 
     * @return true if human repeat filtering is enabled
     */
    public boolean isSetFilterHumanRepeats()
    {
      return (filter & FILTER_HUMAN_REPEATS) != 0;
    }
    
    /** Set human repeat filtering.
     * 
     * @param humanRepeatFiltering - whether to enable human repeat filtering
     */
    public void setFilterHumanRepeats(boolean humanRepeatFiltering)
    {
      if (humanRepeatFiltering) {
        filter |= FILTER_HUMAN_REPEATS;
      }
      else {
        filter &= ~FILTER_HUMAN_REPEATS;
      }
    }

    /** Checks whether masked sequence filtering is enabled.
     * 
     * @return true if masked sequence filtering is enabled
     */
    public boolean isSetFilterMaskLookup()
    {
      return (filter & FILTER_MASK_LOOKUP) != 0;
    }
    
    /** Set masked sequence filtering.
     * 
     * @param maskFilter - whether to enable masked sequence filtering
     */
    public void setFilterMaskLookup(boolean maskFilter)
    {
      if (maskFilter) {
        filter |= FILTER_MASK_LOOKUP;
      }
      else {
        filter &= ~FILTER_MASK_LOOKUP;
      }
    }

    /** Get the cost of opening a gap in an alignment.
     * 
     * @return cost of opening a gap
     */
    public int getGapOpenCost()
    {
      return gapOpenCost;
    }

    /** Set the cost of opening a gap in an alignment.
     * 
     * @param gapOpenCost - cost of opening a gap
     */
    public void setGapOpenCost(int gapOpenCost)
    {
      this.gapOpenCost = gapOpenCost;
    }

    /** Get the cost of extending a gap in an alignment.
     * 
     * @return cost of extending a gap
     */
    public int getGapExtendCost()
    {
      return gapExtendCost;
    }

    /** Set the cost of extending a gap in an alignment.
     * 
     * @param gapExtendCost - cost of extending a gap
     */
    public void setGapExtendCost(int gapExtendCost)
    {
      this.gapExtendCost = gapExtendCost;
    }

    /** Get the maximum number of hits to calculate.
     * 
     * @return - maximum number of hits to calculate
     */
    public int getNumberOfHits()
    {
      return numberOfHits;
    }

    /** Set the maximum number of hits to calculate.
     * 
     * @param numberOfHits - maximum number of hits to calculate
     */
    public void setNumberOfHits(int numberOfHits)
    {
      this.numberOfHits = numberOfHits;
    }

  }

  private BlastType type;
  private BlastOptions opts;
  
  /** Constructor.
   * 
   * @param type - BLAST type to run
   * @param opts - BLAST options
   */
  public RemoteBlastNCBI(BlastType type, BlastOptions opts)
  {
    this.type = type;
    this.opts = opts;
  }
  
  /** Run BLAST analysis.
   * 
   * @param cs - CurationSet which will hold the BLAST results
   * @param seq - genomic sequence that will be blasted
   * @param offset - genomic position of the start of segment
   * @param strand - genomic strand (1 for plus, -1 for minus)
   * @return name of the type of BLAST and database run
   * @throws Exception - All encompassing exception should something go wrong
   */
  public String runAnalysis(CurationSet cs, SequenceI seq, int strand, int offset) throws Exception
  {
    BlastRequest req = sendRequest(seq, strand);
    String type = retrieveResponse(req, cs, strand, seq.getLength(), offset);
    closeRequest(req);
    return type;
  }
  
  private BlastRequest sendRequest(SequenceI seq, int strand) throws UnsupportedEncodingException, IOException
  {
    //StringBuilder putBuf = new StringBuilder(BLAST_URL);
    StringBuilder putBuf = new StringBuilder();
    processOptions(putBuf);
    putBuf.append("QUERY=");
    putBuf.append(URLEncoder.encode(">" + seq.getName() + "\n", ENCODING));
    putBuf.append(URLEncoder.encode(strand == 1 ? seq.getResidues() : seq.getReverseComplement(), ENCODING));
    putBuf.append("&DATABASE=nr&");
    putBuf.append("QUERY_BELIEVE_DEFLINE=no&");
    putBuf.append("PROGRAM=" + type.toString() + "&");
    putBuf.append("CMD=Put");
    //URL putUrl = new URL(putBuf.toString());
    URL url = new URL(BLAST_URL);
    URLConnection conn = url.openConnection();
    conn.setDoOutput(true);
    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
    wr.write(putBuf.toString());
    wr.flush();
    wr.close();
    //return parseRequest(putUrl.openStream());
    BlastRequest req = parseRequest(conn.getInputStream());
    apollo.util.IOUtil.informationDialog("Expected time before analysis starts: " + req.rtoe + " seconds (" + req.rid + ")");
    return req;
  }
  
  private String retrieveResponse(BlastRequest req, CurationSet cs, int strand, int genomicLength, int offset)
  throws MalformedURLException, IOException, InterruptedException, ParserConfigurationException, SAXException, BlastXMLParser.BlastXMLParserException
  {
    try {
      Thread.sleep(req.rtoe * 1000);
    }
    catch (InterruptedException e) {
    }
    URL getUrl = new URL(createGetUrl(req, true));
    InputStream is = getUrl.openStream();
    while (!checkResponse(is)) {
      Thread.sleep(SLEEP);
      is.close();
      is = getUrl.openStream();
    }
    is.close();
    getUrl = new URL(createGetUrl(req, false));
    is = getUrl.openStream();

    BlastXMLParser parser = new BlastXMLParser();

    String type = parser.parse(is, cs.getResults(), strand, genomicLength, offset);
    
    is.close();
    
    if (cs.getResults().getForwardSet() != null) {
      System.out.println("forward hits: " + cs.getResults().getForwardSet().size());
    }
    if (cs.getResults().getReverseSet() != null) {
      System.out.println("reverse hits: " + cs.getResults().getReverseSet().size());
    }
    return type;
  }

  private String createGetUrl(BlastRequest req, boolean getText) {
    StringBuffer getBuf = new StringBuffer(BLAST_URL);
    getBuf.append("RID=" + req.rid + "&");
    getBuf.append("FORMAT_TYPE=" + (getText ? "Text&" : "XML&"));
    getBuf.append("CMD=Get");
    return getBuf.toString();
  }
  
  private void closeRequest(BlastRequest req) throws MalformedURLException, IOException
  {
    StringBuilder deleteBuf = new StringBuilder(BLAST_URL);
    deleteBuf.append("RID=" + req.rid + "&");
    deleteBuf.append("CMD=Delete");
    URL deleteUrl = new URL(deleteBuf.toString());
    deleteUrl.openConnection();
  }
  
  private BlastRequest parseRequest(InputStream is) throws IOException
  {
    InputStreamReader isr = new InputStreamReader(is);
    BufferedReader br = new BufferedReader(isr);
    BlastRequest res = new BlastRequest();
    String line;
    while ((line = br.readLine()) != null) {
      if (res.rid != null && res.rtoe > 0) {
        break;
      }
      if (res.rid == null) {
        Matcher m = RID_PATTERN.matcher(line);
        if (m.matches()) {
          res.rid = m.group(1);
          continue;
        }
      }
      if (res.rtoe == 0) {
        Matcher m = RTOE_PATTERN.matcher(line);
        if (m.matches()) {
          res.rtoe = Integer.parseInt(m.group(1));
          continue;
        }
      }
    }
    is.close();
    return res;
  }
  
  private void processOptions(StringBuilder buf) throws UnsupportedEncodingException
  {
    if (opts.isSetFilterLowComplexity()) {
      buf.append("FILTER=L&");
    }
    if (opts.isSetFilterHumanRepeats()) {
      buf.append("FILTER=R&");
    }
    if (opts.isSetFilterMaskLookup()) {
      buf.append("FILTER=m&");
    }
    buf.append(URLEncoder.encode("GAPCOSTS=" + opts.getGapOpenCost() + " " + opts.getGapExtendCost() + "&", ENCODING));
    buf.append("GENETIC_CODE=" + opts.getGeneticCode() + "&");
    buf.append("HITLIST_SIZE=" + opts.getNumberOfHits() + "&");
  }

  private boolean checkResponse(InputStream is) throws IOException
  {
    //InputStreamReader isr = new InputStreamReader(is);
    //BufferedReader br = new BufferedReader(isr);
    String line;
    while ((line = readLine(is)) != null) {
      Matcher m = STATUS_PATTERN.matcher(line);
      if (m.matches()) {
        if (!m.group(1).equals("READY")) {
          is.close();
          return false;
        }
      }
      if (line.equals("<PRE>")) {
        break;
      }
    }
    return true;
  }
  
  private String readLine(InputStream is) throws IOException
  {
    int c;
    StringBuilder line = null;
    while ((c = is.read()) != -1) {
      if (line == null) {
        line = new StringBuilder();
      }
      if (c == '\n') {
        break;
      }
      line.append((char)c);
    }
    if (line == null) {
      return null;
    }
    return line.toString();
  }
  
  private class BlastRequest
  {
    public String rid;
    public int rtoe;
    
    public BlastRequest()
    {
      rid = null;
      rtoe = 0;
    }
  }
  
}
