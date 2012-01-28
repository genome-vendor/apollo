package apollo.gui.drawable;

import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Font;
import java.util.*;

import apollo.datamodel.*;
import apollo.config.Config;
import apollo.gui.TierManagerI;
import apollo.gui.Transformer;
import apollo.gui.event.*;

/**
 * A drawable for drawing result (computational analysis) feature sets.
 * This draws the intron line as an "arc" (line up to midpoint and down from 
 * midpoint. This is how ensembl indicates that a feature is a gene. 
 * (If we reuse this for other non gene feats we may want to rename it 
 * ArcedIntronLineFeatureSet or something like that)
 */
public class DrawableGeneFeatureSet extends DrawableFeatureSet
  implements DrawableSetI {

  private static byte NEEDS_SORT  = 1<<2;
  // Optimisation use static array for edges so we don't have to keep
  // allocating it
  static int maxEdge = 100;
  static int edges[] = new int[maxEdge];
  private byte sortFlags = NEEDS_SORT;
  private byte currOrientation;

  public DrawableGeneFeatureSet() {
    super(true);
  }

  public DrawableGeneFeatureSet(FeatureSetI fset) {
    super(fset, true);
  }

  public Drawable createDrawable (SeqFeatureI sf) {
    return new DrawableGeneSeqFeature(sf);
  }

  /** Draws arced intron line */
  public void drawUnselected(Graphics g,
                             Rectangle boxBounds,
                             Transformer transformer,
                             TierManagerI manager) {
    int setSize = size();

    if ((sortFlags & NEEDS_SORT) == NEEDS_SORT ||
        currOrientation != transformer.getXOrientation()) {
      sort(transformer.getXOrientation());
      sortFlags ^= NEEDS_SORT;
      currOrientation = (byte)transformer.getXOrientation();
    }

    if (setSize > 1) {
      // Features are not necessarily in correct order in set.
      // SO collect all boundaries and sort into order.
      //    Then draw tents between each pairs ignoring first and last
      //    points

      int twoSetSize = 2*setSize;

      // NOTE: Static variables edges and maxEdge

      if (twoSetSize > maxEdge) {
        maxEdge = twoSetSize;
        edges = new int[maxEdge];
      }

      int j = 0;
      Vector features = getDrawables();
      for (int i = 0; i < setSize; i++, j+=2) {
        Drawable dsf = (Drawable)features.elementAt(i);
	DrawableUtil.setBoxBounds(dsf, transformer, manager);
        Rectangle fp_bounds = dsf.getBoxBounds();

        edges[j] = fp_bounds.x;
        edges[j+1] = fp_bounds.x + fp_bounds.width;
      }

      int twoSetSizeMin1 = twoSetSize-1;
      int ip1;
      int centre;

      if (Config.getDrawOutline())
	g.setColor(Config.getOutlineColor());
      else
	g.setColor(getDrawableColor());

      int y_mid = getYCentre(boxBounds);
      int y_top = boxBounds.y + (((transformer.getXOrientation() == 
                                   Transformer.LEFT &&
                                   getStrand() == 1) ||
                                  (transformer.getXOrientation() == 
                                   Transformer.RIGHT &&
                                   getStrand() == -1)) ? 
                                 0: boxBounds.height);
      for (int i = 1; i < twoSetSizeMin1; i+=2) {
        ip1 = i + 1;
        centre = (edges[ip1]-edges[i]+1)/2+edges[i];
        // Diagonal up from x start(y_mid) to x midpoint(y_top)
        g.drawLine(edges[i],  y_mid, centre, y_top);
        // Diagonal down from midpoint(top y) to end(mid y)
        g.drawLine(centre,  y_top, edges[ip1], y_mid);
      }
    }
  }

  public Drawable addFeatureDrawable(SeqFeatureI sf) {
    Drawable dsf = super.addFeatureDrawable(sf);
    sortFlags |= NEEDS_SORT;
    return dsf;
  }

}
