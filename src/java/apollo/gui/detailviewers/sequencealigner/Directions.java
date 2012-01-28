package apollo.gui.detailviewers.sequencealigner;

/**
 *  Currently only used by SeqAlignPanel so putting in detail windows package,
 *  if other classes use then should be moved, could probably be declared in 
 *  SeqAlignPanel itself
 */

public interface Directions {
  public static final int NORTH = 1;
  public static final int SOUTH = 2;
  public static final int EAST = 3;
  public static final int WEST = 4;
}
