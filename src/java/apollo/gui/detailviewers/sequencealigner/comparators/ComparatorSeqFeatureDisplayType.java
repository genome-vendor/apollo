package apollo.gui.detailviewers.sequencealigner.comparators;

import java.util.Comparator;

import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.datamodel.SeqFeatureI;

public class ComparatorSeqFeatureDisplayType implements Comparator<SeqFeatureI>  {

  public int compare(SeqFeatureI o1, SeqFeatureI o2) {
    
    FeatureProperty fp1 = Config.getPropertyScheme()
    .getFeatureProperty(o1.getTopLevelType());
    
    FeatureProperty fp2 = Config.getPropertyScheme()
    .getFeatureProperty(o2.getTopLevelType());
    
    
  return fp1.getDisplayType().compareTo(fp2.getDisplayType());
  }

}
