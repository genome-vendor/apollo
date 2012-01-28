package apollo.gui.genomemap;

import java.awt.Graphics;
import java.util.Vector;

import apollo.gui.Selection;
import apollo.gui.TierManagerI;
import apollo.gui.Transformer;

public interface FeatureTierManagerI extends TierManagerI {

  public void synchDrawablesWithTiers();

  public String getTierLabel(int tier_number);

  public void clearFeatures();

  public void setTextAvoidance(Transformer t, Graphics g);

  public void unsetTextAvoidance();

  public boolean isAvoidingTextOverlaps();

  public boolean areAnyTiersLabeled();

  public Vector getHiddenTiers();

  public void setVisible(String type, boolean state);

  public void collapseTier(String tier_label);
  public void expandTier(String tier_label);
  
  /** Searches through the tiers to find all drawables that have
      features in the current selection */
  public Selection getViewSelection(Selection selection);
}
