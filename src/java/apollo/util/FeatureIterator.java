package apollo.util;

import java.util.*;
import apollo.datamodel.SeqFeatureI;

/** Iterator for FeatureList, puts out SeqFeatureIs, add more Iterator
 * methods as needed. This should be incorporated with FeatureSet once
 * FeatureSet is refactored and replaces FeatureList. */

public class FeatureIterator implements ListIterator {
  private FeatureList features;
  private int index=0;

  public FeatureIterator(FeatureList feats) {
    features = feats;
  }

  public boolean hasNextFeature() {
    return hasNext();
  }

  public boolean hasNext() {
    return index < features.size();
  }

  public SeqFeatureI nextFeature() {
    return features.getFeature(index++);
  }

  public Object next() {
    return nextFeature();
  }

  public int nextIndex() {
    return index;
  }

  public SeqFeatureI previousFeature() {
    return features.getFeature(--index);
  }

  public Object previous() {
    return previousFeature();
  }

  public int previousIndex() {
    return index - 1;
  }

  public boolean hasPrevious() {
    return (index > 0);
  }

  public void add (Object o) {}
  public void remove () {}
  public void set (Object o) {}

}
