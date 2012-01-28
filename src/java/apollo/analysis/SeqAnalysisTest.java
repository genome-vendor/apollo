/*
 * SeqAnalysisTest
 *
 */

package apollo.analysis;

/**
 * @author Chris Mungall
 **/

import java.util.*;
import junit.framework.*;

import apollo.analysis.*;
import apollo.datamodel.*;

public class SeqAnalysisTest extends TestCase {

  SeqAnalysisFactory fac = new SeqAnalysisFactory ();

  public SeqAnalysisTest(String name) {
    super(name);
  }
  public static void main(String args[]) {
    junit.textui.TestRunner.run(SeqAnalysisTest.class);
  }
  public void testLocalRun() {
    CurationSet cs;
    Hashtable ht = new Hashtable();
    SequenceI sequence   = new Sequence("blah", "MSTVDKEELVQKAKLAEQSERYDDMAQAMKSVTETGVELSNEERNLLSVAYKNVVGARRS");

    ht.put("seqAnalysisType", "local");
    SeqAnalysisI sa =
      fac.getSeqAnalysis(ht);
    sa.addProperty("programName", "fakeblast");
    sa.addProperty("programShellCommand", "echo hello");
    sa.addProperty("fastaDatabaseName", "fakeblastdb");
    sa.setInputSequence(sequence);
    sa.launch();
    while (!sa.isFinished()) {
      System.out.println("waiting....");
      //	    System.sleep(3);
    }
    cs = sa.getCurationSet();
    String raw = sa.getAllRawResults();
    System.out.println("Output = " + raw);
    assertEquals(raw, "hello");

    // test an actual seq analysis program
    sa = fac.getSeqAnalysis(ht);
    sa.addProperty("programName", "blastall");
    sa.addProperty("datasourceName", "aa");
    sa.setInputSequence(sequence);
    sa.launch();
    while (!sa.isFinished()) {
      System.out.println("waiting....");
      //	    System.sleep(3);
    }
    cs = sa.getCurationSet();
    raw = sa.getAllRawResults();
    System.out.println("Output = " + raw);

    assertTrue(true);
  }
}
