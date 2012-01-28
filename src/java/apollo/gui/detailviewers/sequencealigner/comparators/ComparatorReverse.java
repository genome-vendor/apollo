package apollo.gui.detailviewers.sequencealigner.comparators;

import java.util.Comparator;

public class ComparatorReverse<T> implements Comparator<T> {
  
  private Comparator<T> comparator;
  
  public ComparatorReverse(Comparator<T> c) {
    this.comparator = c;
  }

  public int compare(T o1, T o2) {
    return comparator.compare(o2, o1);
  }
  

}
