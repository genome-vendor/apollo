package apollo.gui.detailviewers.sequencealigner.filters;

import java.util.ArrayList;
import java.util.List;

import apollo.datamodel.SeqFeatureI;

public class MultiFilterAnd<T> extends ArrayList<Filter<T>> 
    implements MultiFilter<T> {


  public MultiFilterAnd() {
    super();
    this.add(new BaseFilter<T>(true));
  }
  
  public boolean keep(T f) {
    boolean result = true;
    
    for (Filter<T> filter : this) {
      result = result && filter.keep(f);
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
        s += " AND ";
      }
      s += "(" + f + ")";
      isFirst = false;
    }
    s += "]";
    return s;
  }
  
}
