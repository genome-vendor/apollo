/*
 * ProgramHandlerFactory
 *
 */

package apollo.analysis;

/**
 * generates ProgramHandlerI objects
 * 
 * see design patterns for description of Factory pattern
 * 
 *
 * @author Chris Mungall
 **/

import java.util.*;

import apollo.analysis.*;
import apollo.datamodel.*;

public class ProgramHandlerFactory {

  private ProgramHandlerI ph;
  private Vector recognisedPrograms;

  public ProgramHandlerFactory() {
    recognisedPrograms = new Vector();
  }

  public ProgramHandlerI getProgramHandler (SeqAnalysisI seqAnalysis) {

    String prog = seqAnalysis.getProgramName();
    String classname;
    ph = null;


    // THIS IS YUCKY AND NON
    // EXTENSIBLE!!!
    // just here as a test
    if (prog.equals("blastall") ||
        prog.equals("blastn") ||
        prog.equals("blastx") ||
        prog.equals("blastp") ||
        prog.equals("tblastn") ||
        prog.equals("tblastx")) {
      classname = "BlastHandler";
    } else if (prog.equals("sim4")) {
      classname = "Sim4Handler";
    } else {
      classname = "GenericProgramHandler";
    }

    try {
      Class cls = java.lang.Class.forName("apollo.analysis."+classname);
      ph = (ProgramHandlerI) cls.newInstance();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      System.out.println(e);
    }
    catch (InstantiationException e) {
      System.out.println(e);
    }
    catch (IllegalAccessException e) {
      System.out.println(e);
    }
    System.out.println("Got ph = " + ph);
    return ph;
  }

  public Vector getRecognisedPrograms() {
    return recognisedPrograms;
  }
}
