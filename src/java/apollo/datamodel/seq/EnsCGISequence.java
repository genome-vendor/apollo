package apollo.datamodel.seq;

import java.util.*;
import java.io.IOException;
import java.lang.String;

import apollo.dataadapter.CGI;
import apollo.datamodel.RangeI;
import apollo.datamodel.Range;
import apollo.datamodel.SequenceI;
import apollo.datamodel.Range;
import apollo.config.Config;
import apollo.gui.Controller;
import apollo.seq.io.FastaFile;

/**
 *
 * <p>EnsCGISequence: </p>
 * <p>Sequence class for implementing lazy load from
 * Ensembl Server </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Ensembl</p>
 * @Steve Searles (Suzi guesses)
 * @version 1.0
 *
 * It is important to bear in mind that the retrievals and sequences
 * at this level are 0-based. Although in the display the sequences
 * are shown as 1 based (counting bases, not spaces) this does not
 * apply here.
 */
public class EnsCGISequence extends AbstractLazySequence implements LazySequenceI {
  String          server;

  public EnsCGISequence(String id, Controller c, RangeI loc,
                        String server) {
    super(id,c);

    setRange(loc);
    // setLength(loc.length()); ??
    setServer(server);
  }


  /** AbstractSequence by default will add one to the end on getRes to counteract
      java Strings non inclusiveness. EnsCGI already retrieves sequence with the end
      included. Returning false disables AbstractSequence's inclusive end. */
  protected boolean needInclusiveEnd() { return false; }

  public void setServer(String server) {
    this.server = server;
  }

  public SequenceI getSubSequence(int start, int end) {
    Range subLoc = new Range(getRange().getName(),
                             getRange().getStart()+start-1,
                             getRange().getStart()+end-1);
    return new EnsCGISequence(getName(), llco.getController(), subLoc,
                              server);
  }


  /* Note: low and high are relative coordinates */
  protected String getResiduesFromSourceImpl(int low, int high) {

    int realStart = low; //  + getRange().getStart();
    int realEnd   = high; // + getRange().getStart();

    Hashtable var = new Hashtable();

//    var.put("bp_start",String.valueOf(new Integer(realStart)));
//    var.put("bp_end", String.valueOf(new Integer(realEnd)));
//    var.put("chr",getRange().getName());
//    var.put("type","basepairs");
//    var.put("format","fasta");
//    var.put("region","1");
//    var.put("btnsubmit","Export");
//    var.put("tab","fasta");
    //http://dev.ensembl.org:80/perl/exportview?tab=fasta&btnsubmit=Export&format=fasta&region=1&type=basepairs&chr=2&bp_start=123&bp_end=100000

    var.put("vc_start",String.valueOf(new Integer(realStart)));
    var.put("vc_end", String.valueOf(new Integer(realEnd)));
    var.put("chr",getRange().getName());
    var.put("seqonly","1");
    //http://dev.ensembl.org:80/perl/exportview?tab=fasta&btnsubmit=Export&format=fasta&region=1&type=basepairs&chr=2&bp_start=123&bp_end=100000

    String host = server;
    int port = Config.getCGIPort();
    String cgistr = "perl/apolloview";
//    String cgistr = "perl/exportview";

    CGI cgi = new CGI(host,port,cgistr,var,System.out);

    cgi.run();

    FastaFile fa = new FastaFile(cgi.getInput(),false);
    Vector seqs = fa.getSeqs();

    if (seqs.size() == 1) {
      System.out.println("Returning sequence");
      return ((SequenceI)seqs.elementAt(0)).getResidues();
    } else {
      System.out.println("Returning null");
      return null;
    }
  }

  public static void main(String [] argv) {
    Controller c = new Controller();
    EnsCGISequence seq = new EnsCGISequence("Dummy",c,new Range("22",20000000,20100000),
                                            "www.ensembl.org");

    seq.getCacher().setMinChunkSize(100);
    System.out.println("Sequence first 10 = " + seq.getResidues(1,10));
    System.out.println("Sequence 100-110 = " + seq.getResidues(100,110));
    System.out.println("Sequence 10000-11010 = " + seq.getResidues(10000,11010));
    System.out.println("Sequence 1-12010 = " + seq.getResidues(1,12010));
  }
}
