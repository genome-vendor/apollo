package apollo.gui.detailviewers.sequencealigner.filters;

import apollo.datamodel.SeqFeatureI;

public interface Filter<T> {

  public abstract boolean keep(T f);
  
  public abstract String valueToString();
}
