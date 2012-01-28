package apollo.datamodel.seq;

import java.util.*;
import java.io.IOException;
import java.lang.String;

import apollo.gui.Controller;
import apollo.gui.ControllerDebugListener;
import apollo.datamodel.SequenceI;
import apollo.datamodel.Sequence;

public class DebugLazySequence extends AbstractLazySequence implements LazySequenceI {
  String residues;

  public DebugLazySequence(String id, Controller c, String res) {
    super(id,c);

    setResidues(res);
    cacher.setMinChunkSize(20);
  }

  public void setResidues(String seq) {
    this.residues = seq;
    setLength(this.residues.length());
  }

  public int getLength() {
    return residues.length();
  }

  // NOTE: This returns a Sequence not a DebugLazySequence
  public SequenceI getSubSequence(int start, int end) {
    return new Sequence(getName(), getResidues(start,end));
  }


  /* Note: low and high are relative coordinates */
  protected String getResiduesFromSourceImpl(int low, int high) {
    Sequence tmpSeq = new Sequence("tmp",residues);
    String seq = "";
    seq =  tmpSeq.getResidues(low+1,high+1);
    return seq;
  }

  public static void main(String [] argv) {
    Controller c = new Controller();
    DebugLazySequence seq = new DebugLazySequence("Dummy",c,"AAAAAAAAAACCCCCCCCCCGGGGGGGGGGTTTTTTTTTTAAAAAAAAAA");
    DebugLazySequence seq2 = new DebugLazySequence("Dummy",c,"ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ");

    ControllerDebugListener dl = new ControllerDebugListener(c);

    System.out.println("Sequence first 10 = " + seq.getResidues(1,10));
    System.out.println("Sequence 30-40 = " + seq.getResidues(30,40));
    System.out.println("Sequence 24-40 = " + seq.getResidues(24,40));
    System.out.println("Sequence 2-40 = " + seq.getResidues(2,40));
    System.out.println("Sequence 40-2 = " + seq.getResidues(40,2));

    System.out.println("Alphabetic Sequence first 10 = " + seq2.getResidues(1,10));
    System.out.println("Alphabetic Sequence 40-50 = " + seq2.getResidues(40,50));
    System.out.println("Alphabetic Sequence 34-52 = " + seq2.getResidues(34,52));
    System.out.println("Alphabetic Sequence 2-80 = " + seq2.getResidues(2,80));
  }

}
