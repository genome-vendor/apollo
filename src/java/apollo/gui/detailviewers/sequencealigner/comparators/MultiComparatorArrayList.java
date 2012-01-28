package apollo.gui.detailviewers.sequencealigner.comparators;

import java.util.LinkedList;
import java.util.Comparator;
import java.util.Iterator;

import apollo.datamodel.SeqFeatureI;

public class MultiComparatorArrayList<T> extends LinkedList<Comparator<T>> 
    implements MultiComparator <T>{

  public MultiComparatorArrayList() {
    super();
    this.add(new ComparatorBase<T>());
  }
  
  public boolean add(Comparator<T> o) {
    this.addFirst(o);
    if (this.size() > 4) {
      this.removeLast();
    }
    return true;
  }
  
  public int compare(T o1, T o2) {
    int result = 0;
    
    Iterator<Comparator<T>> itr = this.iterator();
    while(itr.hasNext() && result == 0) {
      Comparator<T> c = itr.next();
      result = c.compare(o1, o2);
    }
    
    return result;
  }

}
