package apollo.datamodel.seq;

import java.util.*;
import java.io.*;
import java.lang.String;
import java.net.*;

import apollo.datamodel.RangeI;
import apollo.datamodel.Range;
import apollo.datamodel.SequenceI;
import apollo.datamodel.Sequence;
import apollo.config.Config;
import apollo.gui.Controller;

public class PFetchSequence extends AbstractLazySequence implements LazySequenceI {

  public PFetchSequence(String id, Controller c) {
    super(id,c);
    length = -1;
  }

  public RangeI getRange() {
    if (genomicRange == null) {
      setRange(new Range(getName(),1,getLength()));
    }
    return genomicRange;
  }

  public void setRange(RangeI loc) {
    this.genomicRange = loc;
  }

  public SequenceI getSubSequence(int start, int end) {
    return new Sequence(getName(), getResidues(start,end));
  }

  public int getLength() {
    if (length == -1) {
      length = DataUtil.getLengthFromPFetch(getName());
    }
    return length;
  }

  /* Note: low and high are relative coordinates */
  protected String getResiduesFromSourceImpl(int low, int high) {
    String finalStr = "";
    try {
      Socket t = new Socket(Config.getPFetchServer(), 22100);

      t.setSendBufferSize(1000);

      DataOutputStream sos =
        new DataOutputStream(t.getOutputStream());

      String allArgs = new String();
      allArgs = "-s " + low + " -e " + high + " -q " + getName() + "\n";
      // System.out.println("Pfetch args = " + allArgs);
      sos.write(allArgs.getBytes());

      DataInputStream is =
        new DataInputStream(t.getInputStream());
      boolean more = true;
      while (more) {
        String str = is.readLine();
        if (str == null) {
          more = false;
        } else {
          // System.out.println(str);
          finalStr += str;
        }
      }
    } catch(IOException e) {
      System.out.println("PFetchSequence.getResiduesFromSourceImpl: error: " + e);
    }
    return finalStr;
  }

  public static void main(String [] argv) {
    Controller c = new Controller();
    PFetchSequence seq = new PFetchSequence("AK001640",c);

    seq.getCacher().setMinChunkSize(1000);
    System.out.println("Sequence first 10 = " + seq.getResidues(1,10));
    System.out.println("Sequence next 10 = " + seq.getResidues(11,20));
    System.out.println("Sequence 100-110 = " + seq.getResidues(100,110));
  }
}
