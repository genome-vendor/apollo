/*
 * SeqAnalysisRemote
 *
 */

package apollo.analysis;

/**
 * An analysis to be run remotely via the Remote
 * website. Results returned as html
 *
 *
 * @author Chris Mungall
 **/

import java.util.*;
import java.awt.event.*;
import org.bdgp.io.*;

import apollo.analysis.SeqAnalysisBase;
import apollo.datamodel.*;

public abstract class SeqAnalysisRemote extends SeqAnalysisBase {

  public SeqAnalysisRemote () {
    super();
    initSeqAnalysisRemote();
  }

  public SeqAnalysisRemote (Hashtable inproperties) {
    super(inproperties);
    initSeqAnalysisRemote();
  }

  // hardode these for now....
  public void initSeqAnalysisRemote() {
    Vector v = new Vector();
    v.addElement("programParams");
    v.addElement("datasourceName");
    setAllowedProperties(v);

    Vector progs = new Vector();
    progs.addElement("blast");
    progs.addElement("sim4");
    progs.addElement("genie");
    progs.addElement("genscan");
    setAllowedValues("programName", progs);

  }

  // make it conform to org.bdgp.io.VisualDataAdapter
  public DataAdapterUI getUI(IOOperation op) {
    return new SeqAnalysisGUI(this, op);
  }

  public boolean launch() {
    System.out.println("not implemented");
    return false;
  }

  public String getName() {
    return "SeqAnalysisRemote";
  }
  public String getType() {
    return "Remote web interface";
  }

}
