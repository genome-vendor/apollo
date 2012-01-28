package apollo.dataadapter.ensj19;

import java.util.*;
import java.io.IOException;
import java.lang.String;

import apollo.gui.event.*;
import apollo.gui.*;
import apollo.datamodel.*;
import apollo.datamodel.seq.AbstractLazySequence;
import apollo.datamodel.seq.LazySequenceI;
import apollo.seq.io.*;

import org.apache.log4j.*;
import org.ensembl19.*;
import org.ensembl19.driver.*;
import org.ensembl19.datamodel.*;


public class EnsJSequence extends AbstractLazySequence implements LazySequenceI {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(EnsJSequence.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private Location location;

  // Important note: we must store driverPath rather than the driver it
  // refers to. Apollo occasionally serialises the ensj adapter and associated
  // instances. If we store driver then the whole ensj driver will be
  // serialised. By resolving the driver dynamically we avoid apollo
  // serialising the the ensj driver.
  private String driverPath;

  public EnsJSequence(String id, Controller c, Location loc, String driverPath) {
    super(id,c);

    this.location = loc;

    setRange(new Range("dummy",loc.getStart(), loc.getEnd()));

    setDriverPath(driverPath);
    setLength( loc.getLength() );
  }

  public void setDriverPath(String path) {
    this.driverPath = path;
  }


  /**
   * @return new location based on current one but with different start and
   * end.  */
  private LinearLocation derivedLocation(int start, int end) {

    LinearLocation subLoc = (LinearLocation)location.copy();
    subLoc.setStart( start );
    subLoc.setEnd( end );
    return subLoc;

  }



  public SequenceI getSubSequence(int start, int end) {

    return new EnsJSequence(getName(),
                            llco.getController(),
                            derivedLocation(start, end),
                            driverPath);
  }


  /* Note: low and high are relative coordinates */
  protected String getResiduesFromSourceImpl(int start, int end) {

    try {

      logger.debug("getting sequence " + start + " to " + end + " from " + driverPath + " for seq obj " + this);
      SequenceAdaptor sa = (SequenceAdaptor)DriverManager.get(driverPath).getAdaptor("sequence");
      LinearLocation loc = derivedLocation(start, end);
      org.ensembl19.datamodel.Sequence seq = sa.fetch(loc);
      String s = seq.getString();
      logger.debug("s=" + start + "\te="+end);
      logger.debug("EnsJSequence retrieved: " + s.length() + "\t" + s + "\t" + loc);

      return s;

    } catch (Exception e) {
      logger.error("Failed getting sequence residues", e);
      return null;
    }
  }

  public static void main(String [] argv) throws Exception {
    DriverManager.load("org/ensembl/conf/current_driver.conf:org/ensembl/conf/kaka_mysql_server.conf");
    Controller c = new Controller();
    EnsJSequence seq = new EnsJSequence("Dummy",c,new AssemblyLocation("22",20000000,20100000, 0),
                                        "current");

    SequenceAdaptor sa = (SequenceAdaptor)DriverManager.get("current").getAdaptor("sequence");

    seq.getCacher().setMinChunkSize(100);
    System.out.println("Have sequence. Dumping regions");

    String processedSeq = seq.getResidues(1,10);
    String rawSeq = sa.fetch(new AssemblyLocation("22",20000000,20000009, 0)).getString();
    String diff = ( rawSeq.equals(processedSeq) ) ? "SAME" : "protected = " +processedSeq + "\nraw = " +rawSeq;
    System.out.println("Sequence first 10 = " + diff);

    processedSeq = seq.getResidues(100,110);
    rawSeq = sa.fetch(new AssemblyLocation("22",20000099,20000109, 0)).getString();
    diff = ( rawSeq.equals(processedSeq) ) ? "SAME" : "protected = " +processedSeq + "\nraw = " +rawSeq;
    System.out.println("Sequence 100-110 = " + diff);

    processedSeq = seq.getResidues(10000,11010);
    rawSeq = sa.fetch(new AssemblyLocation("22",20009999,20011009, 0)).getString();
    diff = ( rawSeq.equals(processedSeq) ) ? "SAME" : "protected = " +processedSeq + "\nraw = " +rawSeq;
    System.out.println("Sequence 10000-11010 = " + diff);

    processedSeq = seq.getResidues(1,12010);
    rawSeq = sa.fetch(new AssemblyLocation("22",20000000,20012009, 0)).getString();
    diff = ( rawSeq.equals(processedSeq) ) ? "SAME" : "protected = " +processedSeq + "\nraw = " +rawSeq;
    System.out.println("Sequence 1-12010 = " + diff);


    processedSeq = seq.getResidues(12010, 1);
    rawSeq = sa.fetch(new AssemblyLocation("22",20000000, 20012009, -1)).getString();
    diff = ( rawSeq.equals(processedSeq) ) ? "SAME" : "protected = " +processedSeq + "\nraw = " +rawSeq;
    System.out.println("Sequence 12010-1 = " + diff);



  }
}









