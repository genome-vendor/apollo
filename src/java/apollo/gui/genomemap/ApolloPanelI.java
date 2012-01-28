package apollo.gui.genomemap;

import java.awt.Image;
import java.awt.Component;

import apollo.gui.Controller;
import apollo.gui.Selection;

/**
   This was invented to eliminate all of the casting that
   was going on. This way it is possible to use the
   views in another sort of panel and doesn't assume that
   the view's panel is always an ApolloPanel */

public interface ApolloPanelI {
  public Image getBackBuffer();

  public Selection getSelection();
  //  public void hideSelection();
  public void clearSelection();
  // public void showAll();

  public ViewI getFocus();
  public Controller getController();

  public StrandedZoomableApolloPanel getStrandedZoomableApolloPanel();
}
