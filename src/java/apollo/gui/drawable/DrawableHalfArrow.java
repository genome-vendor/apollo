package apollo.gui.drawable;

import java.awt.Graphics;
import java.awt.Rectangle;

import apollo.datamodel.SeqFeatureI;
import apollo.gui.TierManagerI;
import apollo.gui.Transformer;

public class DrawableHalfArrow extends DrawableSeqFeature implements Drawable {

  public DrawableHalfArrow(boolean drawn)
  {
    super(drawn);
  }
  
  public DrawableHalfArrow(SeqFeatureI feature, boolean drawn)
  {
    super(feature, drawn);
  }

  @Override
  public void drawUnselected(Graphics g, Rectangle boxBounds,
      Transformer transformer, TierManagerI manager) {
    setLabeled();
    DrawableUtil.setBoxBounds(this, transformer, manager);

    g.setColor(getDrawableColor());
    int x1 = boxBounds.x;
    int y1 = boxBounds.y + (boxBounds.height / 2);
    int x2 = boxBounds.x + boxBounds.width;
    int y2 = y1;
    g.drawLine(x1, y1, x2, y2);
    if (feature.getId().contains("forward")) {
      g.drawLine(x1 + ((x2 - x1) / 2), boxBounds.y, x2, y2);
      if (feature.getStrand() == 1) {
      }
    }
    else {
      g.drawLine(x1, y1, x2 - ((x2 - x1) / 2), boxBounds.y + boxBounds.height);
      if (feature.getStrand() == -1) {
      }
    }

    addHighlights(g,boxBounds,transformer,manager);
    addDecorations(g,boxBounds,transformer,manager);
  }
  
}
