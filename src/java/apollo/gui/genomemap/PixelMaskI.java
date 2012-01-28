package apollo.gui.genomemap;

/**
 * A PixelMaskI can set pixels to be masked at a given level/yindex
 * and be queried for a masking range at a given level
 */
public interface PixelMaskI {

  public void setPixelState(int pix, boolean state,int yindex);
  public void setPixelRangeState(int start, int stop, boolean state,int yindex);
  public boolean isCompletelyObscured(int start,int stop,int yindex);
}
