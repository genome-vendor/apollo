package apollo.gui.genomemap;

import java.util.Vector;

import apollo.gui.TierManagerI;

public interface DrawableTierManagerI extends TierManagerI {
  /* all the views that are "managed" deal with drawables
     and need a way to get a list of the ones currently
     visible */
  public Vector getVisibleDrawables(int [] limits);
}
