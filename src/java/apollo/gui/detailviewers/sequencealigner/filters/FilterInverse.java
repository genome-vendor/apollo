package apollo.gui.detailviewers.sequencealigner.filters;

import apollo.datamodel.SeqFeatureI;

public class FilterInverse<T> implements Filter<T> {
  
  private Filter<T> filter;
  
  public FilterInverse(Filter<T> f) {
    filter = f;
  }

  public boolean keep(T f) {
    return !filter.keep(f);
  }
  
  public String toString() {
    return  "Remove " + filter.valueToString();
  }
  
  public String valueToString() {
    return filter.valueToString();
  }

}
