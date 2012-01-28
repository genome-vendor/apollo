package apollo.gui.detailviewers.sequencealigner.filters;

import java.util.ArrayList;
import java.util.List;

import apollo.datamodel.SeqFeatureI;

public class MultiFilterOr<T> extends ArrayList<Filter<T>> 
    implements MultiFilter<T> {

  public MultiFilterOr() {
    super();
    this.add(new BaseFilter<T>(false));
  }
  
  public boolean keep(T f) {
    boolean result = false;
    
    for (Filter<T> filter : this) {
      result = result || filter.keep(f);
    }
    return result;
  }
  
  public String toString() {
    return "Keep " + valueToString();
  }
  
  public String valueToString() {
    String s = "[";
    boolean isFirst = true;
    for (Filter f : this) {
      if (!isFirst) {
        s += " OR ";
      }
      s += "(" + f + ")";
      isFirst = false;
    }
    s += "]";
    return s;
  }
  
}
