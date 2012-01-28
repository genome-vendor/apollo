/*
 * SeqAnalysisLocal
 *
 */

package apollo.analysis;

/**
 * Any analysis run on the local machine/network
 * (ie one in which the user has control over a shell)
 * These differ from remote analyses in that the user
 * has a greater control over configuration - program
 * directory, fasta file paths, tmp dir for fasta files, etc.
 *
 * (In a remote analysis these are taken care of by the
 * server)
 *
 * One unresolved issue here is how many parameters should
 * be set "behind the scenes" and how many the user should
 * have direct control over.
 * 
 * Too much control makes for a confusing UI, too little will
 * be too inflexible for more sophisticated users.
 *
 *
 *
 *
 * @see BlastHandler, ProgramHandlerI
 * @author Chris Mungall
 **/

import java.util.*;
import java.io.*;
import java.lang.*;
import java.awt.event.*;
import javax.swing.JOptionPane;
import javax.swing.JDialog;

import org.apache.log4j.*;

import org.bdgp.io.*;

import apollo.analysis.SeqAnalysisBase;
import apollo.datamodel.*;
import apollo.dataadapter.gamexml.GAMEAdapter;
import apollo.seq.io.*;
import apollo.dataadapter.DataInputType;
import apollo.config.Config;

public class SeqAnalysisLocal extends SeqAnalysisBase {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(SeqAnalysisLocal.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private Runtime runner = Runtime.getRuntime();
  private Process proc;
  private String progOutput;
  private ProgramHandlerFactory phf;
  protected String seqFile;

  public SeqAnalysisLocal () {
    super();
    initSeqAnalysisLocal();
  }

  public SeqAnalysisLocal (Hashtable inproperties) {
    super(inproperties);
    initSeqAnalysisLocal();
  }

  public void init() {
    super.init();
  }

  public void initSeqAnalysisLocal() {

    // this REALLY should be in a config

    progOutput = "";
    phf = new ProgramHandlerFactory();
    Vector v = new Vector();
    v.addElement("programName");
    v.addElement("programParams");
    v.addElement("datasourcePath");
    v.addElement("filter");
    v.addElement("tmpdir");
    setAllowedProperties(v);

    setPropertyDescription("programName",
                           "name of an executable program to run "+
                           "should be in your PATH; otherwise you "+
                           "must specify the full path");

    Vector progs = new Vector();
    progs.addElement("blastx");
    progs.addElement("blastp");
    progs.addElement("blastall");
    progs.addElement("sim4");
    progs.addElement("genie");
    progs.addElement("genscan");
    setAllowedValues("programName", progs);

    Vector filteropts = new Vector();
    filteropts.addElement("none");
    filteropts.addElement("BOP");
    setAllowedValues("filter", filteropts);

    setPropertyType("datasourcePath", "path");
    setPropertyType("tmpdir", "dir");

  }

  // make it conform to org.bdgp.io.VisualDataAdapter
  public DataAdapterUI getUI(IOOperation op) {
    return new SeqAnalysisGUI((SeqAnalysisBase)this, op);
  }

  public String getName() {
    return "SeqAnalysisLocal";
  }

  public String getType() {
    return "locally executed analysis";
  }

  public boolean launch() {
    String cmd = getProgramShellCommand();
    boolean success = true;
    logger.info("Launching "+ cmd);
    try {
      proc = runner.exec(cmd);
      addProperty("isStarted", "true");

      if (proc.exitValue() == 0) {
        // put the output in the results hash
        slurpResults(cmd);
        filter();
        addProperty("isFinished", "true");
      } else {
        slurpError(cmd);
        addProperty("isFinished", "false");
        success = false;
      }
      // synchronous!
    }
    catch(IOException e) {
      alertUserToError(cmd, e.getMessage());
      success = false;
    }
    return success;
  }

  /**
   * writes input sequence as a fasta file
   *
   * @returns path of file as String
   **/
  protected String getSeqFile() {
    String seqpath = "tmp.fa";
    try {
      String fastr = FastaFile.print(getInputSequence());

      FileWriter w = new FileWriter(seqpath);
      w.write(fastr);
      logger.debug(fastr);
      w.close();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    seqFile = seqpath;
    return seqpath;
  }

  private void slurpResults(String cmd) {

    InputStreamReader isr = new InputStreamReader(proc.getInputStream());

    // probably inefficient.....
    setProgOutput (cmd, isr);
    // how do we catch the error stream???
    rawResults.put("out", progOutput);
  }

  private void slurpError(String cmd) {
    InputStreamReader isr = new InputStreamReader(proc.getErrorStream());

    setProgOutput (cmd, isr);
    alertUserToError(cmd, progOutput);
  }

  private void alertUserToError (String cmd, String err) {
    JOptionPane.showMessageDialog (null,
                                   err,
                                   "Unable to carry out " + cmd,
                                   JOptionPane.ERROR_MESSAGE);
    addProperty("isFinished", "false");
  }

  private void setProgOutput (String cmd, InputStreamReader isr) {
    BufferedReader br = new BufferedReader(isr);
    StringBuffer buf = new StringBuffer();

    try {
      String line = br.readLine();
      while  (line != null) {
        buf.append (line + "\n");
        line = br.readLine();
      }
    } catch(IOException e) {
      logger.error(e.getMessage(), e);
      alertUserToError(cmd, e.getMessage());
      // DO SOMETHING!!!
    }
    progOutput = buf.toString();
  }

  private void filter() {

    String filter =
      getProperty("filter");
    if (filter != null &&
        filter.equals("BOP")) {
      String out = (String)rawResults.get("out");

      String outpath = "tmp.out";
      try {
        FileWriter w = new FileWriter(outpath);
        w.write(out);
        w.close();

        String seqpath = getSeqFile();
        // yes i know it seems odd to call java from
        // java but this is the best thing to do for this
        // right now, as the datamodels are different
        String bopclasspath =
          "/home/cjm/cvs/flybase/software/java-apps/classes:/home/cjm/cvs/flybase/software/java-apps/classes/jakarta-oro.jar";
        String bopcmd =
          "java -classpath "+bopclasspath+" bop.bop -fasta " +
          seqpath +
          " -o tmp.xml/home/cjm/cvs/flybase/software/java-apps/data/bop.prefs tmp.out";
        logger.info("bopcmd = "+bopcmd);
        Process bopproc = runner.exec(bopcmd);
        GAMEAdapter ga =
          new GAMEAdapter();
        ga.init();
        ga.setInputType (DataInputType.FILE);
        ga.setInput("tmp.xml");
        curationSet = ga.getCurationSet();
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    }

  }

  public Vector getRawResultVector() {
    return new Vector();
  }


  /**
   * programShellCommand = programPath programArgs
   **/
  protected String getProgramShellCommand() {
    String cmd;

    cmd = getProperty("programShellCommand");
    if (cmd == null) {

      ProgramHandlerI ph =
        phf.getProgramHandler(this);
      logger.debug("Got ph = " + ph);
      cmd = ph.createUnixShellCommand(this);
    }
    return cmd;
  }

  /**
   * programPath = programDirectory / programName
   **/
  protected String getProgramPath() {
    String path;

    path = getProperty("programPath");
    if (path == null) {
      String pdir =
        getProgramDirectory();
      String pname =
        getProgramName();
      if (pdir == null) {

        //assume its in users path
        path = pname;
      } else {
        path = pdir + "/" + pname;
      }
    }
    return path;
  }

  protected String getProgramDirectory() {
    return getProperty("programDirectory");
  }

  protected void run() { }

}
