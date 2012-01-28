/**
 * I represent the contained in a Feature (as defined by the DAS-spec) 
 * needed by Apollo to display features. I have a reference implementation in the
 * <code> simple </code> sub-package
 * 
 * @see apollo.dataadapter.das.simple.SimpleDASFeature
 * @author: Vivek Iyer
**/

package apollo.dataadapter.das;


public interface DASFeature {
  public abstract String getId();
  public abstract String getLabel();
  public abstract String getTypeId();
  public abstract String getTypeCategory();
  public abstract String getTypeLabel();
  public abstract String getTypeReference();
  public abstract String getTypeSubparts();

  public abstract String getMethodId();
  public abstract String getMethodLabel();

  public abstract String getStart();
  public abstract String getEnd();
  public abstract String getScore();

  public abstract String getPhase();
  public abstract String getOrientation();
  public abstract String getNote();
  public abstract String getTargetId();
  public abstract String getTargetStart();

  public abstract String getTargetStop();
  public abstract String getGroupId();

  public abstract String getGroupTargetStart();
  public abstract String getGroupTargetStop();

  public abstract void setId(String newValue);
  public abstract void setLabel(String newValue);
  public abstract void setTypeId(String newValue);
  public abstract void setTypeCategory(String newValue);
  public abstract void setTypeLabel(String newValue);
  public abstract void setTypeReference(String newValue);
  public abstract void setTypeSubparts(String newValue);

  public abstract void setMethodId(String newValue);
  public abstract void setMethodLabel(String newValue);

  public abstract void setStart(String newValue);
  public abstract void setEnd(String newValue);
  public abstract void setScore(String newValue);

  public abstract void setPhase(String newValue);
  public abstract void setOrientation(String newValue);
  public abstract void setNote(String newValue);
  public abstract void setTargetId(String newValue);
  public abstract void setTargetStart(String newValue);

  public abstract void setTargetStop(String newValue);
  public abstract void setGroupId(String newValue);

  public abstract void setGroupTargetStart(String newValue);
  public abstract void setGroupTargetStop(String newValue);
}
