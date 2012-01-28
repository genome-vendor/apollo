/*
	Copyright (c) 2000
	BDGP, University of California Berkeley
	All Rights Reserved
*/

package apollo.gui.drawable;

import apollo.datamodel.*;
import apollo.config.Config;
import apollo.gui.TierManagerI;
import apollo.gui.Transformer;
import apollo.config.FeatureProperty;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * A glyph that draws a solid bar with a triangle at the top or bottom.
 * This might be useful for showing transposon insertions, for example.
 * Renamed from DrawableInsertion to Triangle in order to be more descriptive of 
 * what is actually drawn
 */
public class PromoterGlyph extends DrawableSeqFeature {
  /**
   * constructs a glyph with the triangle at the top
   * pointing in the direction of the insertion.
   * when selected a 1 pixel line is draw down to the insertion site
   * the triangle is filled with gray and outlined in black,
   * and the bar is black.
   */

  public PromoterGlyph() {
    super(true);
  }

  public PromoterGlyph(SeqFeatureI feature) {
    // this will call set feature below
    super(feature, true);
  }

  public void drawUnselected(Graphics g,
                             Rectangle boxBounds,
                             Transformer transformer,
                             TierManagerI manager) {
    if (boxBounds.width < 3) {
      super.drawUnselected(g, boxBounds, transformer, manager);
    }
    else {
      // if right then the DNA has been reverse-complemented
      // by the user
      int arrow_tip = boxBounds.x;
      int arrow_bot;
      int y = getYCentre(boxBounds);
      int transcription_start_x = boxBounds.x;
      int transcription_start_y = boxBounds.y;
      if ((getStrand() >= 0 &&
         transformer.getXOrientation() == Transformer.LEFT) ||
        (getStrand() < 0 &&
         transformer.getXOrientation() == Transformer.RIGHT)) {
        arrow_tip += boxBounds.width;
        arrow_bot = arrow_tip - (Math.min(8, boxBounds.width));
        transcription_start_y += boxBounds.height;
      }
      else {
        arrow_bot = arrow_tip + (Math.min(8, boxBounds.width));
        transcription_start_x += boxBounds.width;
      }
      g.setColor(getDrawableColor());
      // horizontal line
      g.drawLine(boxBounds.x, y, boxBounds.x + boxBounds.width, y);
      // vertical line
      g.drawLine(transcription_start_x, y,
                 transcription_start_x, transcription_start_y);
      // arrowhead
      g.drawLine(arrow_tip, y, arrow_bot, boxBounds.y);
      g.drawLine(arrow_tip, y, arrow_bot, boxBounds.y + boxBounds.height);
    }
  }
}

