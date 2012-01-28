package apollo.gui.drawable;

import apollo.datamodel.SeqFeature;
import apollo.gui.genomemap.Sites;

/**
 * SiteCodon is a class to accomodate the optimizations in SiteView and Sites.
 * SiteView no longer uses to do its drawing. But it needs
 * to send DrawableSeqFeatures for the findFeaturesForSelection method. And it needs to
 * be aware of the selection of the DrawableSeqFeatures that it sends out but does not
 * draw. SiteCodon is a DrawableSeqFeature used by Sites that never gets drawn but
 * notifies its Sites object of any selections it receives.
 * This class will also probably be needed to deal with dragging which is currently
 * not implemented with the SiteView optimizations.
 * I didnt call it DrawableSiteCodon because it is not meant to be drawn.
 * And i dont think it needs to extend DrawableTerminalCodon
 * Should see if its possible to also not subclass DrawableSeqFeature
 */

public class SiteCodon extends DrawableSeqFeature { // implements SelectableI?

  private Sites sites;

  /** SiteCodon created for selection. */
  public SiteCodon(SeqFeature feature,Sites sites) {
    super(feature, true);
    this.sites = sites;
  }

  /** This is for dragging.
      SiteView.selectionToSiteCodon (used by createDragView)
      creates a DrawableFeatureSet and DrawableFeatureSet 
      creates SiteCodons with
      an empty constructor. Without Sites there can be no selection, but 
      codons being dragged do not get selected so its ok. */
  public SiteCodon() {
    super(true);
  }

  /**
   * Pass selection state onto Sites
   * If this is being set to false then it no longer needs to exist
   */
  public void setSelected(boolean state) {
      super.setSelected(state); // SUZ, not sure if this is needed
    sites.selectSite(getStart(),state);
  }
}
