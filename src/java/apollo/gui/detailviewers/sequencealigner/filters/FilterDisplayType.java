package apollo.gui.detailviewers.sequencealigner.filters;

import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.datamodel.SeqFeatureI;

public class FilterDisplayType implements Filter<SeqFeatureI> {
  
  private String displayType;
  
  public FilterDisplayType(String dt) {
    this.displayType = dt;
  }

  public boolean keep(SeqFeatureI f) {
    FeatureProperty fp = Config.getPropertyScheme()
      .getFeatureProperty(f.getTopLevelType());
    return displayType.equalsIgnoreCase(fp.getDisplayType());
  }

  public String toString() {
    return "Keep " + valueToString();
  }
  
  public String valueToString() {
    return "DisplayType " + displayType;
  }

}
