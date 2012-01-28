/*
 * SeqAnalysisNCBI
 *
 */

package apollo.analysis;

/**
 * An analysis to be run remotely via the NCBI
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

public class SeqAnalysisNCBI extends SeqAnalysisRemote {

  public SeqAnalysisNCBI () {
    super();
    initSeqAnalysisNCBI();
  }

  public SeqAnalysisNCBI (Hashtable inproperties) {
    super(inproperties);
    initSeqAnalysisNCBI();
  }

  // hardcode these for now....
  // should get from  a config file.
  // note that we assume program=blast for ncbi
  // rename this NCBIBlast???
  public void initSeqAnalysisNCBI() {
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

  public boolean        launch() {
    System.out.println("not implemented");
    return false;
  }

  public String getName() {
    return "SeqAnalysisNCBI";
  }
  public String getType() {
    return "NCBI web interface";
  }

}
