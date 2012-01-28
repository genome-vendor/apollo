/*
 * SeqAnalysisFactory
 *
 */

package apollo.analysis;

/**
 * @author Chris Mungall
 **/

import java.util.*;

import apollo.analysis.*;
import apollo.datamodel.*;

public class SeqAnalysisFactory {

  private Vector sats;
  private SeqAnalysisI seqAnalysis;

  public SeqAnalysisFactory() {
    sats = new Vector();
    // should be in config!!!
    sats.addElement("local");
    sats.addElement("ncbi");
    sats.addElement("bdgp");
  }

  public SeqAnalysisI getSeqAnalysis (Hashtable properties) {
    String sat =
      (String)properties.get("seqAnalysisType");
    if (sat.equals("local")) {
      seqAnalysis = new SeqAnalysisLocal();
    }
    if (sat.equals("ncbi")) {}
    if (sat.equals("bdgp")) {}
    return seqAnalysis;

  }

  public Vector getSeqAnalysisTypes() {
    return sats;
  }
}
