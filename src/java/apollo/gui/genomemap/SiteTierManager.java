package apollo.gui.genomemap;

import java.util.*;

import apollo.gui.Controller;
import apollo.gui.TierManager;

public class SiteTierManager extends TierManager {

  protected Sites  []       sites;

  public SiteTierManager(Controller c) {
    setController(c);
    setIgnoreScoreThresholds(true);
  }

  public void setTierData(Object data) {
    setSites((Sites []) data);
  }

  public void fillTiers() {
    tiers.removeAllElements();
    for (int i=0; i<sites.length; i++) {
      tiers.addElement(new SiteTier());
    }
  }

  protected void setSites(Sites [] sites) {
    this.sites = sites;
    doLayoutTiers();
  }

}
