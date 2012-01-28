package apollo.dataadapter;

import apollo.datamodel.*;
import java.util.*;
import java.io.*;

import org.bdgp.io.*;
import java.util.Properties;

/** Is this class still being used? - delete?  */

public class EfetchSequenceAdapter extends AbstractApolloAdapter {

  IOOperation [] supportedOperations = {
                                         ApolloDataAdapterI.OP_READ_SEQUENCE
                                       };

  public void init() {}

  public String getName() {
    return "Efetch Sequence Adapter";
  }

  public String getType() {
    return "Efetch Sequence Data";
  }

  public DataInputType getInputType() {
    return null;
  } // ???
  public String getInput() {
    return null;
  } // ???

  public IOOperation [] getSupportedOperations() {
    return supportedOperations;
  }

  public EfetchSequenceAdapter() {}

  public SequenceI getSequence(String id) throws ApolloAdapterException {
    String command = "efetch -q " + id;

    //	System.out.println("Running command: " +  command);

    Process p = null;
    StringBuffer sb = new StringBuffer();

    try {
      p = Runtime.getRuntime().exec(command);

      BufferedInputStream is = new BufferedInputStream(p.getInputStream());
      int len = 0;
      byte buf[] = new byte[100];

      while( p != null && is != null && (len = is.read(buf)) != -1 ) {
        String str = new String(buf,0,0,len);

        sb.append(str);
      }

      //	    System.out.println("Command thread is done");

    }
    catch( java.io.EOFException eof ) {
      System.out.println("Exception : " + eof);
    }
    catch (java.io.IOException e) {
      System.out.println("Exception : " + e);
    }

    if (sb.charAt(sb.length()-1) == '\n') {
      sb.setLength(sb.length()-1);
    }
    String seqStr = sb.toString();


    if (id.equals("AF300871") ) {
      System.out.println("Full string is " + seqStr);
    }
    Sequence seq = new Sequence(id,seqStr.toUpperCase());


    return seq;
  }
  public SequenceI getSequence(DbXref dbxref) throws ApolloAdapterException {
    return getSequence(dbxref.getIdValue());
  }

  public Vector    getSequences(DbXref[] dbxref) throws ApolloAdapterException {
    Vector seqs = new Vector();
    for (int i=0; i < dbxref.length; i++) {
      seqs.addElement(getSequence(dbxref[i]));
    }
    return seqs;
  }

  public SequenceI getSequence(DbXref dbxref, int start, int end) throws ApolloAdapterException {
    if (end < start) {
      int tmp = start;
      start = end;
      end   = tmp;
      System.out.println("WARNING: Correcting start and end in EfetchSequenceAdapter getSequence");
    }

    String command = "efetch -q -s " + start + " -e " + end + " " + dbxref.getIdValue();

    System.out.println("Running command: " +  command);

    Process p = null;
    StringBuffer sb = new StringBuffer();
    boolean done = false;
    try {
      p = Runtime.getRuntime().exec(command);

      BufferedInputStream is = new BufferedInputStream(p.getInputStream());
      int len = 0;
      byte buf[] = new byte[100];

      while( p != null && is != null && (len = is.read(buf)) != -1 && done == false) {
        String str = new String(buf,0,0,len);

        sb.append(str);
      }

      System.out.println("Command thread is done");

    } catch( java.io.EOFException eof ) {
      System.out.println("Exception : " + eof);
    }
    catch (java.io.IOException e) {
      System.out.println("Exception : " + e);
    }

    if (sb.charAt(sb.length()-1) == '\n') {
      sb.setLength(sb.length()-1);
    }
    String str = sb.toString().toUpperCase();

    System.out.println("Fullstring " + str);
    Sequence seq = new Sequence(dbxref.getIdValue(),str);

    System.out.println("Got sequence " + seq);

    return seq;
  }

  public Vector    getSequences(DbXref[] dbxref, int[] start, int[] end) throws ApolloAdapterException {
    Vector seqs = new Vector();

    for (int i=0; i < dbxref.length; i++) {
      seqs.addElement(getSequence(dbxref[i],start[i],end[i]));
    }
    return seqs;
  }

  public void addDataListener(DataListener dl) {}
  
  /** Send any necessary signals to the server to release annotation locks or undo edits
   * --after the user has been prompted that these will be lost.
   */
  protected boolean rollbackAnnotations(CurationSet curationSet) {
    return true;
  }
  /** No UI for this adapter */
  public DataAdapterUI getUI(IOOperation op) { return null; }
  
}
