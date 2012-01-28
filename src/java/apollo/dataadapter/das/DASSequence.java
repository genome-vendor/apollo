package apollo.dataadapter.das;

/**
 * I represent the data contained in a dna sequence (as defined by the DAS-spec) 
 * needed by Apollo to display a sequence. I have a reference implementation in the
 * <code> simple </code> sub-package
 * 
 * @see apollo.dataadapter.das.simple.SimpleDASSequence
 * @author: Vivek Iyer
**/
public interface DASSequence {
  public abstract String getId();
  public abstract String getStart();
  public abstract String getStop();
  public abstract String getVersion();
  public abstract String getDNALength();
  public abstract String getDNA();

  public abstract void setId(String newValue);
  public abstract void setStart(String newValue);
  public abstract void setStop(String newValue);
  public abstract void setVersion(String newValue);
  public abstract void setDNALength(String newValue);
  public abstract void setDNA(String newValue);
}


