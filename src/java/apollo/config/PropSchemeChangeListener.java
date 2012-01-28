package apollo.config;

import java.util.EventListener;

public interface PropSchemeChangeListener extends EventListener {

  // well is it tiers or types - discuss
  //public boolean handleTiersChangedEvent(TypesChangedEvent evt);
  public boolean handlePropSchemeChangeEvent(PropSchemeChangeEvent evt);

}
