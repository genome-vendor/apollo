package apollo.gui.detailviewers.sequencealigner.filters;

import apollo.datamodel.SeqFeatureI;

public class BaseFilter<T> implements Filter<T>{
  
  private boolean val;
  
  public BaseFilter() {
    val = true;
  }
  
  public BaseFilter(boolean v) {
    val = v;
  }
  
  public boolean keep(T f) {
    return val;
  }
  
  public String toString() {
    return valueToString();
  }

  public String valueToString() {
    return "BaseFilter " + val;
  }

}
