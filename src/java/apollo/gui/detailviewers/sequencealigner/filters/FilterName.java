package apollo.gui.detailviewers.sequencealigner.filters;

import apollo.datamodel.SeqFeatureI;

public class FilterName implements Filter<SeqFeatureI> {
  
  private String name;
  
  public FilterName(String name) {
    this.name = name;
  }
  
  public boolean keep(SeqFeatureI f) {
    return f.getName().equals(name);
  }

  public String toString() {
    return "Keep " + valueToString();
  }

  public String valueToString() {
    return "Name " + name;
  }

}
