package apollo.gui.drawable;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Color;

import apollo.datamodel.SeqFeatureI;
import apollo.gui.TierManagerI;
import apollo.config.Config;
import apollo.gui.Transformer;

/** Draws a zig zag */

public class Zigzag extends DrawableSeqFeature {
  
  public Zigzag() {
    super(true);
  }

  public Zigzag(SeqFeatureI feature) {
    super(feature, true);
  }

  public void drawUnselected(Graphics g, 
			     Rectangle boxBounds,
			     Transformer transformer,
			     TierManagerI manager) {
    g.setColor(getDrawableColor());

    int interval = boxBounds.height;
    int zagInterval;
    if (boxBounds.width < interval) {
      zagInterval = boxBounds.width;
    }
    else {
      int adjust = boxBounds.width % interval;
      if (adjust > (interval * 0.5)) {
        int zig_count = boxBounds.width / interval;
	zagInterval = (int) ((double) (boxBounds.width) / 
			     (double) zig_count + 1);
      }
      else
	zagInterval = interval;
    }

    int x_end = boxBounds.x + boxBounds.width;
    int y_start = boxBounds.y;
    int y_end = boxBounds.y + boxBounds.height;
    /* bracket the zigzag with 2 vertical lines */
    g.drawLine(boxBounds.x, y_start, boxBounds.x, y_end);
    g.drawLine(x_end, y_start, x_end, y_end);
    
    for (int zig_Xstart = boxBounds.x;
	 zig_Xstart < x_end; 
	 zig_Xstart += zagInterval) {
      // fillPolygon? 2dgraphics?
      int zig_Xend = zig_Xstart + zagInterval;
      if (zig_Xend > x_end) 
	zig_Xend = x_end;
      g.drawLine(zig_Xstart, y_start, zig_Xend, y_end);
      int tmp = y_start;
      y_start = y_end;
      y_end = tmp;
    }
  }
  
}
