package apollo.dataadapter.das.simple;

import apollo.dataadapter.das.*;

/**
 * I am a lightweight implementation of the <code>DASSegment</code> interface.
 * I am a simple bag of attributes with no further internal functionality.
 * 
 * @see apollo.dataadapter.das.DASSegment
 * @author Vivek Iyer
**/
public class
  SimpleDASSegment
implements
  DASSegment 
{
  private String segment;
  private String id;
  private String start;
  private String stop;
  private String orientation;
  private String subparts;
  private String length;

  public SimpleDASSegment(
    String theSegment,
    String theId,
    String theStart,
    String theStop,
    String theOrientation,
    String theSubparts,
    String theLength
  ) {
    segment = theSegment;
    id = theId;
    start = theStart;
    stop = theStop;
    orientation = theOrientation;
    subparts = theSubparts;
    length = theLength;
    //    System.out.println("SimpleDASSegment: creating new segment " + segment + ", " + id); // DEL
  }

  public String getSegment() {
    return segment;
  }

  public String getId() {
    return id;
  }

  public String getStart() {
    return start;
  }

  public String getStop() {
    return stop;
  }

  public String getOrientation() {
    return orientation;
  }

  public String getSubparts() {
    return subparts;
  }

  public String getLength() {
    return length;
  }

  public void setSegment(String newValue) {
    segment = newValue;
  }

  public void setId(String newValue) {
    id = newValue;
  }

  public void setStart(String newValue) {
    start = newValue;
  }

  public void setStop(String newValue) {
    stop = newValue;
  }

  public void setOrientation(String newValue) {
    orientation = newValue;
  }

  public void setSubparts(String newValue) {
    subparts = newValue;
  }

  public void setLength(String newValue) {
    length = newValue;
  }

  /** Segment is generally the ID without the segment/, e.g.,
   *  segment=chr1, id=segment/chr1 */
  public String toString() {
    if (getSegment() == null || getSegment().equals(""))
      return getId();
    else    
      return getSegment();
  }
}//end DASSegment
