package apollo.gui.detailviewers.sequencealigner.comparators;

import java.util.Comparator;

import apollo.datamodel.SeqFeatureI;

public class ComparatorSeqFeatureName implements Comparator<SeqFeatureI> {

  public int compare(SeqFeatureI o1, SeqFeatureI o2) {
    return o1.getName().compareToIgnoreCase(o2.getName());
  }

}
