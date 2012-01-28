package apollo.gui.genomemap;

import org.apache.log4j.*;

/**
 * Class which records which pixels in a tier have been drawn already, used in 
 * drawing optimisation.
 * The yindex in these methods corresponds to a given "tier" that is being masked.
 * Separate masks are tracked for each tier.
 */
public class PixelMask implements PixelMaskI {
 
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(PixelMask.class);
  
  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  boolean [][] mask;
  int        size;
  int levels;

  /** levels is the number of vertical levels/tiers
   * size is the maximum x coord
   */
  public PixelMask(int levels,int size) {
    this.mask = new boolean[levels] [size];
    this.size = size;
    this.levels = levels;
  }

  private boolean checkLimits(int start, int stop) {
    if (start < 0 || stop >= size) {
      //         logger.error("Trying to access pixel outside mask bounds for range " + start + "-" + stop);
      return false;
    }
    return true;
  }
  public void setPixelState(int pix, boolean state,int yindex) {
    if (checkLimits(pix,pix)) {
      mask[yindex][pix] = state;
    }
  }
  /**
   * start and stop are for x coords, state is to set to toggle mask,
   * yindex corresponds to vertical level
   */
  public void setPixelRangeState(int start, int stop, boolean state,int yindex) {
    int realStart = (start < 0 ? 0 : start);
    int realStop  = (stop >= size ? size-1 : stop);
    for (int i=realStart;i<=realStop;i++) {
      mask[yindex][i] = state;
    }
  }

  /**
   * start and stop are for x coords,
   * yindex corresponds to vertical level
   * Returns true if the range of xcoords in the yindex level 
   * is completely masked out
   */
  public boolean isCompletelyObscured(int start,int stop,int yindex) {
    int realStart = (start < 0 ? 0 : start);
    int realStop  = (stop >= size ? size-1 : stop);
    for (int i=realStart;i<=realStop;i++) {
      if (!mask[yindex][i]) {
        return false;
      }
    }
    return true;
  }

  // for debug
  public String toString() {
    String s = "\nmask true vals: ";//+this;// this is for oid
    for (int y=0; y<levels; y++)
      for (int i=0; i<size; i++)
        if (mask[y][i])
          s+=y+":"+i+",";
    return s+"\n";
  }
}
