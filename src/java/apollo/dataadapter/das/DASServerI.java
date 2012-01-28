package apollo.dataadapter.das;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * I represent the capability of a general DASServer, as required by Apollo
 * to make das-data requests. I have a simple (no-frills) implementation in the
 * <code>simple</code> subpackage. NOTE - my name is DASServerI because I
 * wish to avoid conflict with an existing DASServer module historically
 * in the same package.
 * 
 * @see apollo.dataadapter.das.simple.SimpleDASServer
 * @author Vivek Iyer
**/

public interface DASServerI {
  public String getURL ();
  public void setURL (String url);
  public List getDSNs();
  public List getEntryPoints(DASDsn dsn);
  public List getFeatures(DASDsn dsn, DASSegment[] segmentSelection);
  public List getSequences(DASDsn dsn, DASSegment[] segmentSelection);
}


