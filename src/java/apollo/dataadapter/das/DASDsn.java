/**
 * <p>I an interface representing a single DAS datasource.</p>
 *
 * <p>I am a tentative attempt at an interface approach to DAS-reads,
 * regardless of how I am actually backed - adhoc, or via omnigene etc</p>
 *
 * @author: Vivek Iyer
**/

package apollo.dataadapter.das;


public interface DASDsn {
  public abstract String getSource();
  public abstract String getSourceId();
  public abstract String getSourceVersion();
  public abstract String getMapMaster();
  public abstract String getDescription();

  public abstract void setSource(String newValue);
  public abstract void setSourceId(String newValue);
  public abstract void setSourceVersion(String newValue);
  public abstract void setMapMaster(String newValue);
  public abstract void setDescription(String newValue);
}
