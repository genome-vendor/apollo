package apollo.gui.drawable;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Polygon;
import java.awt.Color;

import apollo.datamodel.SeqFeatureI;
import apollo.gui.TierManagerI;
import apollo.gui.genomemap.PixelMaskI;
import apollo.gui.Transformer;
import apollo.config.Config;

/** Draws a double-headed arrow (an arrowhead pointing inwards on each end of
    a rectangle). */

public class DoubleHeadedArrow extends DrawableSeqFeature {
  
  public DoubleHeadedArrow() {
    super(true);
  }

  public DoubleHeadedArrow(SeqFeatureI feature) {
    super(feature, true);
  }

  public void drawUnselected (Graphics g, 
			      Rectangle boxBounds,
			      Transformer transformer,
			      TierManagerI manager) {
    int y_height = (int) ((double) boxBounds.height * 0.5);
    int x_width = y_height + 1;
    // must have room to draw both triangles or else just
    // draw the basic box
    if (boxBounds.width < (x_width + x_width)) {
      super.drawUnselected(g, boxBounds, transformer, manager);
    } else {
      int x_begin = boxBounds.x; // < 0 ? 0 : boxBounds.x;
      int x_end = x_begin + boxBounds.width;
      int y_center = getYCentre(boxBounds);
      // Hack for crap Java 1.1 which must use a short somewhere
      // If displays get larger than 3000 pixels width this will fail
      if (x_end > 3000)
        x_end = 3000;
      
      g.setColor(getDrawableColor());
      g.drawLine(x_begin, y_center, x_end, y_center);//
      // Right arrowhead (ok)
      drawArrowHead (g, 
                     x_begin + x_width - 1,  // tip_x
                     x_begin, // flat_x
                     y_center,
                     y_height);
      // Left arrowhead (looks a little weird)
      drawArrowHead (g, 
                     x_end - x_width + 1,  // tip_x
                     x_end + 1, // flat_x
                     y_center,
                     y_height);
    }
  }

  public void drawArrowHead(Graphics g,
			    int x_tip, 
			    int x_flat, 
			    int y_center,
			    int y_height) {
    int x[] = new int[3];
    int y[] = new int[3];
    Polygon poly = new Polygon(x, y, 3);

    x = poly.xpoints;
    y = poly.ypoints;
    x[0] = x_flat;
    y[0] = y_center - y_height;
    x[1] = x_tip;
    y[1] = y_center;
    x[2] = x_flat;
    y[2] = y_center + y_height;
    g.fillPolygon(x, y, 3);
  }
}
