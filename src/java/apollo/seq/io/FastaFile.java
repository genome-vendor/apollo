package apollo.seq.io;

import apollo.io.FileParse;
import apollo.datamodel.*;
import apollo.util.FastaHeader;

import java.io.*;
import java.util.*;
import java.net.*;

import org.apache.log4j.*;

/**
 * Parses a fasta file into a Vector of Sequence objects, which
 * can be retrieved with getSeqs
 */

public class FastaFile extends FileParse {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(FastaFile.class);

  protected static Vector seqs;  //Vector of Sequences

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  int noSeqs;
  int maxLength = 0;

  Hashtable myHash;  //hashtable containing the sequences
  Vector headers;

  Vector words = new Vector();  //Stores the words in a line after splitting

  private void processReader(BufferedReader reader, boolean inHtml)
  {
    boolean hadPre = !inHtml;

    myHash  = new Hashtable();
    seqs        = new Vector();
    String data;
    lineArray = new Vector();
    try {
      while ((data = reader.readLine()) != null) {
        if (data.indexOf("</pre>") >= 0)
          break;
        if (hadPre)
          lineArray.addElement(data);
        if (!hadPre)
          hadPre = (data.indexOf("<pre>") >= 0);
      }
    } catch (IOException ioex) {
      logger.error(ioex.getMessage(), ioex);
    }
    parse();
  }

  /**Construct a FastaFile object from a Reader.  It does not care what the
  *  source is
  *
  * @param reader - Reader source
  * @param inHtml - is the data encoded in html
  */
  public FastaFile(Reader reader, boolean inHtml)
  {
      processReader(new BufferedReader(reader), inHtml);
  }

  public FastaFile(DataInputStream in, boolean inHtml) {
    
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    processReader(reader, inHtml);
  }

  public FastaFile(String inStr) {
    myHash  = new Hashtable();
    seqs    = new Vector();
    headers = new Vector();
    readLines(inStr);
    parse();
  }

  public FastaFile(String inFile, String type, CurationSet curation)
    throws IOException {
    this(inFile, type);
    SequenceI seq = (SequenceI) seqs.elementAt(0);

    String seq_id = seq.getName();

    curation.setStrand(1);

    String start_str = null;
    String end_str = null;
    // If we have start-end numbers on the end of the id strip them off
    int index1 = seq_id.indexOf("/");
    int index2 = seq_id.indexOf("-");
    if (index1 > 0 && index2 > (index1 + 1)) {
      seq.setName(seq_id.substring(0, index1));
      start_str = seq_id.substring(index1 + 1, index2);
      end_str = seq_id.substring(index2 + 1);
    } else if (seq.getDescription() != null) {
      StringTokenizer str = new StringTokenizer(seq.getDescription());
      if (str.countTokens() >= 2) {
        if (str.countTokens() == 3) {
          String chr = str.nextToken();
          logger.info ("Setting chromosome to " + chr);
          curation.setChromosome(chr);
        }
        start_str = str.nextToken();
        end_str = str.nextToken();
      }
    }
    if (start_str != null && end_str != null) {
      try {
        int chrstart = Integer.parseInt(start_str);
        int chrend   = Integer.parseInt(end_str);
        curation.setStart(chrstart);
        curation.setEnd(chrend);
        logger.info("Region is " + chrstart + "-" + chrend);
      } catch (Exception e) {
        logger.error("Couldn't parse start and end from " +
                     start_str + "-" + end_str, e);
        curation.setStart (1);
        curation.setEnd(seq.getLength());
      }
    } else {
      curation.setStart (1);
      curation.setEnd(seq.getLength());
    }
    curation.setName (seq.getName());
    curation.setRefSequence(seq);
  }

  public FastaFile(String inFile, String type) throws IOException {
    //Read in the file first
    super(inFile,type);

    myHash = new Hashtable();
    seqs = new Vector();
    headers = new Vector();

    //Read lines from file
    logger.info("Reading FASTA file " + inFile + "....");
    readLines();
    parse();
  }

  public Vector getSeqs() {
    return seqs;
  }

  public void parse() {
    StringBuffer residues   = new StringBuffer();
    SequenceI sequence = null;
    logger.debug ("File is " + lineArray.size() + " lines");

    for (int i = 0; i < lineArray.size(); i++) {
      //Do we have an id line?
      if (((String)lineArray.elementAt(i)).length() > 0) {
        if (((String)lineArray.elementAt(i)).substring(0,1).equals(">")) {
          FastaHeader header 
            = new FastaHeader ((String) lineArray.elementAt(i));
          logger.debug("Parsing header " + lineArray.elementAt(i));
          /* If this isn't the first sequence 
             add the previous sequence to the array */
          if (sequence != null) {
            sequence.setResidues(residues.toString());
          }
          residues = new StringBuffer();
          sequence = header.generateSequence();
          seqs.addElement(sequence);
          debugPrintSequence(sequence);
        } else {
          // We have sequence
          residues.append((String)lineArray.elementAt(i));
        }
      }
    }
    if (sequence != null) {
      sequence.setResidues(residues.toString());
    }
  }

  public static String print(SequenceI s) {
    SequenceI[] sa = new SequenceI[1];
    sa[0] = s;
    return print(sa,60);
  }

  public static String print(SequenceI[] s) {
    return print(s,60);
  }

  public static String print(SequenceI[] s, int len) {
    return print(s,len,true);
  }

  public static String print(SequenceI[] s, int linesize,boolean gaps) {

    StringBuffer out = new StringBuffer();
    int si;

    for (si = 0; si < s.length; si++) {
      SequenceI seq = s[si];
      out.append (format(">" + seq.getName() + "\n",
                         seq.getResidues(),
                         linesize));

    }
    return out.toString();
  }

  public static String printGenomic (SeqFeatureI feature) {
    String header = (">" + feature.getName() +
                     " (genomic sequence with introns)\n");
    return (format (header, feature.getResidues(), 50));
  }

  public static String print_cDNA (SeqFeatureI feature) {
    String header = (">" + feature.getName() +
                     " (transcript)\n");
    return (format (header, feature.get_cDNA(), 50));
  }

  public static String printTranslation (SeqFeatureI feature) {
    String header = (">" + feature.getName() +
                     " (translation)\n");
    return (format (header, feature.translate(), 50));
  }

  public static String format (String header,
                               String residues,
                               int linesize) {
    StringBuffer out = new StringBuffer(header);

    int i;
    for(i=0; i < residues.length(); i += linesize) {
      String nextln;
      if (i + linesize < residues.length()) {
        nextln = residues.substring(i, i + linesize);
      } else {
        nextln = residues.substring(i);
      }
      out.append(nextln);
      out.append("\n");
    }
    return out.toString();
  }

  private void debugPrintSequence(SequenceI seq) {
    String org = seq.getOrganism();
    String org_str = (org != null && !org.equals("") ?
                      "\n\torganism:\"" + org + "\"" : "");
    logger.debug(seqs.size() + ". " + seq.getName() + 
                 org_str + "\tdesc: " +
                 seq.getDescription());
    Vector xrefs = seq.getDbXrefs();
    int xref_count = (xrefs == null ? 0 : xrefs.size());
    for (int j = 0; j < xref_count; j++) {
      DbXref xref = (DbXref) xrefs.elementAt(j);
      logger.debug("\t" + xref.getDbName() + ":" +
                   xref.getIdValue());
    }
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println ("Please provide the name of a fasta file");
      System.exit(0);
    }
    System.out.println ("Parsing " + args[0]);
    try {
      FastaFile fa = new FastaFile(args[0], "File");
      System.out.println ("Parsed " + seqs.size() + " sequences");
    } catch (Exception e) {
      System.out.println ("IO error parsing " + args[0] + " " + e);
    }
  }
}



