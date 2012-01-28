package apollo.gui.detailviewers.sequencealigner.renderers;

import java.awt.Component;

import apollo.gui.detailviewers.sequencealigner.Orientation;
import apollo.gui.detailviewers.sequencealigner.TierI;

public interface BaseRendererI {
  
  public Component getBaseComponent(int pos, TierI tier, Orientation o);
  
  public int pixelPositionToTierPosition(int p, TierI t, Orientation o);
  
  public int tierPositionToPixelPosition(int p, TierI t, Orientation o);
  
  public int getBaseWidth();

  public int getBaseHeight();

}
