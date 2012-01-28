package apollo.gui;

import java.util.Vector;

import apollo.gui.genomemap.ViewI;

public interface TierManagerI extends ControlledObjectI {

  /**
     I believe COORDS_PER_PIXEL is only used in the vertical/y direction.
     It can't be for the x direction as its dynamic with zooming. So I 
     guess I'm wondering why bother having coords per pixel in the y 
     direction. Why not just have pixels? There is no real y axis, its 
     just a layout issue. I guess I'm missing what the coordinates are 
     doing for us and why is it 10? Is this to leave open the possibility
     of zooming in the y direction in which case these parameter would 
     be dynamic.   */
  public static final long Y_PIXELS_PER_FEATURE = 10;

  public void setView (ViewI view);
  public ViewI getView ();

  public void setViewHeight(int height);
  public void setCharHeight(int height);

  public void doLayoutTiers();

  public int [] getYRange();

  public void setTierData (Object data);

  public void incrementTierHeight();
  public void decrementTierHeight();
  public int getTotalHeight();

  public int getNumTiers();
  public int getNumVisible();
  public int getLowestVisible();
  public void setLowestVisible(int lowest);
  public int getMaxVisibleTierNumber();

  public int getMinimumVisibleTransformCoord();
  public long getVisibleUserCoord();
  public long getMaxUserCoord();

  public long getMaxTierUserHeight();

  public long toTier(long userCoord);
  public int toUser(int tierNum);

  public Vector getTiers();
  public Tier getTier(int tier_number);

  public int getAggregateSizeChange();
  public void setAggregateSizeChange(int change);

  public void setIgnoreScoreThresholds(boolean ignore);

  public void fireTierManagerEvent(int type);

}
