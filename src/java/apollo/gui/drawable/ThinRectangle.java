package apollo.gui.drawable;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Color;

import apollo.datamodel.SeqFeatureI;
import apollo.gui.TierManagerI;
import apollo.gui.Transformer;
import apollo.config.Config;

/** Draws a thin (half height) rectangle, vertically centered in boxBounds */

public class ThinRectangle extends DrawableSeqFeature {
  
  public ThinRectangle() {
    super (true);
  }

  public ThinRectangle(SeqFeatureI feature) {
    super(feature, true);
  }

  public void drawUnselected (Graphics g, 
			      Rectangle boxBounds,
			      Transformer transformer,
			      TierManagerI manager) {
    g.setColor(getDrawableColor());

    int xStart = boxBounds.x;
    int yStart = boxBounds.y; // + (boxBounds.height/4);
    int width = boxBounds.width;
    int height = boxBounds.height;

    g.drawRect(xStart,yStart,width,height);

  }
}


