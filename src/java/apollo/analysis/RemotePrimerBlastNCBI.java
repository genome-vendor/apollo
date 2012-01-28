package apollo.analysis;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import apollo.analysis.PrimerBlastHtmlParser.PrimerBlastHtmlParserException;
import apollo.datamodel.CurationSet;
import apollo.datamodel.SequenceI;
import apollo.util.FeatureList;

/** Sends and retrieves a request to NCBI's Primer-BLAST CGI.
 * 
 * @author elee
 *
 */
public class RemotePrimerBlastNCBI {

  private static final String ENCODING = "UTF-8";
  private static final String PRIMER_BLAST_URL = "http://www.ncbi.nlm.nih.gov/tools/primer-blast/primertool.cgi";
  private static final int SLEEP = 5000;

  private PrimerBlastOptions opts;
  
  /** Constructor.
   * 
   * @param opts - Primer-BLAST options
   */
  public RemotePrimerBlastNCBI(PrimerBlastOptions opts)
  {
    this.opts = opts;
  }

  /** Run Primer-BLAST analysis.
   * 
   * @param cs - CurationSet which will hold the BLAST results
   * @param seq - genomic sequence that will be blasted
   * @param offset - genomic position of the start of segment
   * @return name of the analysis run
   * @throws Exception - All encompassing exception should something go wrong
   */
  public String runAnalysis(CurationSet cs, SequenceI seq, int offset) throws Exception
  {
    return runAnalysis(cs, seq, offset, null);
  }
  
  /** Run Primer-BLAST analysis.
   * 
   * @param cs - CurationSet which will hold the BLAST results
   * @param seq - genomic sequence that will be blasted
   * @param offset - genomic position of the start of segment
   * @param fl - FeatureList of selected features to filter against
   * @return name of the analysis run
   * @throws Exception - All encompassing exception should something go wrong
   */
  public String runAnalysis(CurationSet cs, SequenceI seq, int offset, FeatureList fl) throws Exception
  {
    InputStream response = sendRequest(seq);
    
    //InputStream response = new java.io.FileInputStream("/Users/elee/blah/foobar.html");
    
    String type = retrieveResponse(response, cs, offset, fl);
    response.close();
    return type;
  }

  private InputStream sendRequest(SequenceI seq) throws UnsupportedEncodingException, IOException
  {
    StringBuilder putBuf = new StringBuilder();
    processOptions(putBuf);
    putBuf.append("INPUT_SEQUENCE=");
    putBuf.append(URLEncoder.encode(">" + seq.getName() + "\n", ENCODING));
    putBuf.append(URLEncoder.encode(seq.getResidues(), ENCODING));
    URL url = new URL(PRIMER_BLAST_URL);
    URLConnection conn = url.openConnection();
    conn.setDoOutput(true);
    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
    wr.write(putBuf.toString());
    wr.flush();
    wr.close();
    apollo.util.IOUtil.informationDialog("Primer-BLAST request sent");
    return conn.getInputStream();
  }
  
  private String retrieveResponse(InputStream is, CurationSet cs, int offset, FeatureList fl) throws IOException, InterruptedException,
  PrimerBlastHtmlParserException
  {
    InputStream copy = copyStream(is);
    String resultsUrl;
    while ((resultsUrl = getRedirectionUrl(copy)) != null) {
      Thread.sleep(SLEEP);
      is = new URL(resultsUrl).openStream();
      copy = copyStream(is);
    }
    copy.reset();
    PrimerBlastHtmlParser parser = new PrimerBlastHtmlParser();
    return parser.parse(copy, cs.getResults(), offset, opts.isRemovePairsNotInExons() ? fl : null);
    
  }
  
  private InputStream copyStream(InputStream is) throws IOException
  {
    StringBuilder buf = new StringBuilder();
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    String line;
    while ((line = br.readLine()) != null) {
      buf.append(line + "\n");
    }
    
    return new ByteArrayInputStream(buf.toString().getBytes());
  }
  
  private String getRedirectionUrl(InputStream is) throws IOException
  {
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    String line;
    while ((line = br.readLine()) != null) {
      if (line.contains("META HTTP-EQUIV=Refresh")) {
        String []tokens = line.split("\\s+");
        return tokens[3].substring(4, tokens[3].length() - 2);
      }
    }
    return null;
  }
  
  private void processOptions(StringBuilder buf) throws UnsupportedEncodingException
  {
    buf.append("PRIMER5_START=" + convertOptionToString(opts.getPrimer5Start()) + "&");
    buf.append("PRIMER5_END=" + convertOptionToString(opts.getPrimer5End()) + "&");
    buf.append("PRIMER3_START=" + convertOptionToString(opts.getPrimer3Start()) + "&");
    buf.append("PRIMER3_END=" + convertOptionToString(opts.getPrimer3End()) + "&");
    buf.append("PRIMER_LEFT_INPUT=" + convertOptionToString(opts.getPrimerLeftInput()) + "&");
    buf.append("PRIMER_RIGHT_INPUT=" + convertOptionToString(opts.getPrimerRightInput()) + "&");
    buf.append("PRIMER_PRODUCT_MIN=" + convertOptionToString(opts.getPrimerProductMin()) + "&");
    buf.append("PRIMER_PRODUCT_MAX=" + convertOptionToString(opts.getPrimerProductMax()) + "&");
    buf.append("PRIMER_NUM_RETURN=" + convertOptionToString(opts.getPrimerNumReturn()) + "&");
    buf.append("PRIMER_MIN_TM=" + convertOptionToString(opts.getPrimerMinTm()) + "&");
    buf.append("PRIMER_OPT_TM=" + convertOptionToString(opts.getPrimerOptTm()) + "&");
    buf.append("PRIMER_MAX_TM=" + convertOptionToString(opts.getPrimerMaxTm()) + "&");
    buf.append("PRIMER_MAX_DIFF_TM=" + convertOptionToString(opts.getPrimerMaxDiffTm()) + "&");
    buf.append("SEARCH_SPECIFIC_PRIMER=" + (opts.isSearchSpecificPrimer() ? "on" : "off") + "&");
    buf.append("ORGANISM=" + convertOptionToString(opts.getOrganism()) + "&");
    buf.append("PRIMER_SPECIFICITY_DATABASE=" + convertOptionToString(opts.getPrimerSpecificityDatabase().toCGIParameter()) + "&");
    buf.append("TOTAL_PRIMER_SPECIFICITY_MISMATCH=" + convertOptionToString(opts.getTotalPrimerSpecificityMismatch()) + "&");
    buf.append("PRIMER_3END_SPECIFICITY_MISMATCH=" + convertOptionToString(opts.getPrimer3endSpecificityMismatch()) + "&");
    buf.append("MISMATCH_REGION_LENGTH=" + convertOptionToString(opts.getMismatchRegionLength()) + "&");
    buf.append("PRODUCT_SIZE_DEVIATION=" + convertOptionToString(opts.getProductSizeDeviation()) + "&");
  }
  
  private String convertOptionToString(Object opt) throws UnsupportedEncodingException
  {
    if (opt == null) {
      return "";
    }
    return URLEncoder.encode(opt.toString(), ENCODING);
  }

  /** Options for running Primer-BLAST.
   * 
   */
  public static class PrimerBlastOptions
  {
    /** Database to search against for primer specificity.
     * refseq_rna - Refseq mRNA
     * genome_selected_species - Genome (reference assembly from selected organisms)
     * ref_assembly - Genome (chromosomes from all organisms)
     * nt - non-redundant set of transcripts
     * 
     */
    public enum Database
    {
      refseq_rna,
      genome_selected_species,
      ref_assembly,
      nt;
      
      /** Convert database to the corresponding CGI parameter.
       * 
       * @return database convereted to the correspoding CGI parameter
       */
      public String toCGIParameter()
      {
        switch (this) {
        case refseq_rna:
          return "refseq_rna";
        case genome_selected_species:
          return "primerdb/genome_selected_species";
        case ref_assembly:
          return "ref_assembly";
        case nt:
          return "nt";
        default:
          return null;
        }
      }

      /** Displays the corresponding label to the database.
       * 
       * @return database label
       */
      public String toString()
      {
        switch (this) {
        case refseq_rna:
          return "Refseq mRNA (refseq_rna)";
        case genome_selected_species:
          return "Genome (reference assembly from selected organisms)";
        case ref_assembly:
          return "Genome (chromosomes from all organisms)";
        case nt:
          return "nr";
        default:
          return null;
        }
      }
      
    }
    
    private Integer start;
    private Integer end;
    private Integer primer5Start;
    private Integer primer5End;
    private Integer primer3Start;
    private Integer primer3End;
    private String primerLeftInput;
    private String primerRightInput;
    private Integer primerProductMin;
    private Integer primerProductMax;
    private Integer primerNumReturn;
    private Double primerMinTm;
    private Double primerOptTm;
    private Double primerMaxTm;
    private Double primerMaxDiffTm;
    private boolean searchSpecificPrimer;
    private String organism;
    private Database primerSpecificityDatabase;
    private Integer totalPrimerSpecificityMismatch;
    private Integer primer3endSpecificityMismatch;
    private Integer mismatchRegionLength;
    private Integer productSizeDeviation;
    private boolean removePairsNotInExons;
    
    /** Get the forward primer start genomic coordinate.
     * 
     * @return start genomic coordinate
     */
    public Integer getPrimer5Start() {
      return primer5Start;
    }
    
    /** Set the forward primer start genomic coordinate.
     * 
     * @param primer5Start - start genomic coordinate
     */
    public void setPrimer5Start(Integer primer5Start) {
      this.primer5Start = primer5Start;
    }
    
    /** Get the forward primer end genomic coordinate.
     * 
     * @return end genomic coordinate
     */
    public Integer getPrimer5End() {
      return primer5End;
    }
    
    /** Set the forward primer end genomic coordinate.
     * 
     * @param primer5End - end genomic coordinate
     */
    public void setPrimer5End(Integer primer5End) {
      this.primer5End = primer5End;
    }
    
    /** Get the reverse primer start genomic coordinate.
     * 
     * @return start genomic coordinate
     */
    public Integer getPrimer3Start() {
      return primer3Start;
    }
    
    /** Set the reverse primer start genomic coordinate.
     * 
     * @param primer3Start - start genomic coordinate
     */
    public void setPrimer3Start(Integer primer3Start) {
      this.primer3Start = primer3Start;
    }
    
    /** Get the reverse primer end genomic coordinate.
     * 
     * @return end genomic coordinate
     */
    public Integer getPrimer3End() {
      return primer3End;
    }
    
    /** Set the reverse primer end genomic coordinate.
     * 
     * @param primer3End - end genomic coordinate
     */
    public void setPrimer3End(Integer primer3End) {
      this.primer3End = primer3End;
    }
    
    
    public String getPrimerLeftInput() {
      return primerLeftInput;
    }
    public void setPrimerLeftInput(String primerLeftInput) {
      this.primerLeftInput = primerLeftInput;
    }
    public String getPrimerRightInput() {
      return primerRightInput;
    }
    public void setPrimerRightInput(String primerRightInput) {
      this.primerRightInput = primerRightInput;
    }
    public Integer getPrimerProductMin() {
      return primerProductMin;
    }
    public void setPrimerProductMin(Integer primerProductMin) {
      this.primerProductMin = primerProductMin;
    }
    public Integer getPrimerProductMax() {
      return primerProductMax;
    }
    public void setPrimerProductMax(Integer primerProductMax) {
      this.primerProductMax = primerProductMax;
    }
    public Integer getPrimerNumReturn() {
      return primerNumReturn;
    }
    public void setPrimerNumReturn(Integer primerNumReturn) {
      this.primerNumReturn = primerNumReturn;
    }
    public Double getPrimerMinTm() {
      return primerMinTm;
    }
    public void setPrimerMinTm(Double primerMinTm) {
      this.primerMinTm = primerMinTm;
    }
    public Double getPrimerOptTm() {
      return primerOptTm;
    }
    public void setPrimerOptTm(Double primerOptTm) {
      this.primerOptTm = primerOptTm;
    }
    public Double getPrimerMaxTm() {
      return primerMaxTm;
    }
    public void setPrimerMaxTm(Double primerMaxTm) {
      this.primerMaxTm = primerMaxTm;
    }
    public Double getPrimerMaxDiffTm() {
      return primerMaxDiffTm;
    }
    public void setPrimerMaxDiffTm(Double primerMaxDiffTm) {
      this.primerMaxDiffTm = primerMaxDiffTm;
    }
    public boolean isSearchSpecificPrimer() {
      return searchSpecificPrimer;
    }
    public void setSearchSpecificPrimer(boolean searchSpecificPrimer) {
      this.searchSpecificPrimer = searchSpecificPrimer;
    }
    public String getOrganism() {
      return organism;
    }
    public void setOrganism(String organism) {
      this.organism = organism;
    }
    public Database getPrimerSpecificityDatabase() {
      return primerSpecificityDatabase;
    }
    public void setPrimerSpecificityDatabase(Database primerSpecificityDatabase) {
      this.primerSpecificityDatabase = primerSpecificityDatabase;
    }
    public Integer getTotalPrimerSpecificityMismatch() {
      return totalPrimerSpecificityMismatch;
    }
    public void setTotalPrimerSpecificityMismatch(Integer totalPrimerSpecificityMismatch) {
      this.totalPrimerSpecificityMismatch = totalPrimerSpecificityMismatch;
    }
    public Integer getPrimer3endSpecificityMismatch() {
      return primer3endSpecificityMismatch;
    }
    public void setPrimer3endSpecificityMismatch(Integer primer3endSpecificityMismatch) {
      this.primer3endSpecificityMismatch = primer3endSpecificityMismatch;
    }
    public Integer getMismatchRegionLength() {
      return mismatchRegionLength;
    }
    public void setMismatchRegionLength(Integer mismatchRegionLength) {
      this.mismatchRegionLength = mismatchRegionLength;
    }
    public Integer getProductSizeDeviation() {
      return productSizeDeviation;
    }
    public void setProductSizeDeviation(Integer productSizeDeviation) {
      this.productSizeDeviation = productSizeDeviation;
    }
    public Integer getStart() {
      return start;
    }
    public void setStart(Integer start) {
      this.start = start;
    }
    public Integer getEnd() {
      return end;
    }
    public void setEnd(Integer end) {
      this.end = end;
    }
    public boolean isRemovePairsNotInExons() {
      return removePairsNotInExons;
    }
    public void setRemovePairsNotInExons(boolean removePairsNotInExons) {
      this.removePairsNotInExons = removePairsNotInExons;
    }
    
  }
}
