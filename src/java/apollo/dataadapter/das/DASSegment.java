package apollo.dataadapter.das;

/**
 * I represent the contained in a Segment (as defined by the DAS-spec) 
 * needed by Apollo to request features for the segment, and display
 * the features of the segment. I have a reference implementation in the
 * <code> simple </code> sub-package
 * 
 * @see apollo.dataadapter.das.simple.SimpleDASSegment
 * @author: Vivek Iyer
**/
public interface DASSegment {
  public abstract String getSegment();
  public abstract String getId();
  public abstract String getStart();
  public abstract String getStop();
  public abstract String getOrientation();
  public abstract String getSubparts();
  public abstract String getLength();

  public abstract void setSegment(String newValue);
  public abstract void setId(String newValue);
  public abstract void setStart(String newValue);
  public abstract void setStop(String newValue);
  public abstract void setOrientation(String newValue);
  public abstract void setSubparts(String newValue);
  public abstract void setLength(String newValue);

}//end DASSegment

