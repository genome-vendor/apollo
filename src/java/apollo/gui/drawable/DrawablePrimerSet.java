package apollo.gui.drawable;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;

import apollo.config.Config;
import apollo.datamodel.FeaturePairI;
import apollo.datamodel.FeatureSetI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;
import apollo.datamodel.Transcript;
import apollo.gui.TierManagerI;
import apollo.gui.Transformer;
import apollo.gui.genomemap.PixelMaskI;

public class DrawablePrimerSet extends DrawableFeatureSet { // DrawableSeqFeature {

  public DrawablePrimerSet()
  {
    super(true);
  }
  
  public DrawablePrimerSet(FeatureSetI feature, boolean drawn) {
    super(feature, drawn);
  }

  protected boolean drawDashedLines(Graphics g,
      Rectangle boxBounds,
      Transformer transformer,
      TierManagerI manager,
      Color color,
      int y_center) {
    Drawable dsf_1 = getDrawableAt(0);
    Drawable dsf_2 = getDrawableAt(1);
    DrawableUtil.setBoxBounds(dsf_1, transformer, manager);
    DrawableUtil.setBoxBounds(dsf_2, transformer, manager);
    Rectangle fp1_bounds = dsf_1.getBoxBounds();
    Rectangle fp2_bounds = dsf_2.getBoxBounds();
    int left_end;
    int right_end;
    if (fp1_bounds.x < fp2_bounds.x) {
      left_end = fp1_bounds.x;
      right_end = fp2_bounds.x;
    } 
    else {
      right_end = fp1_bounds.x;
      left_end = fp2_bounds.x;
    }
    g.setColor(getDrawableColor());
    for (int j = left_end; j < (right_end); j += 6) {
      int dash = Math.min (1, right_end - j);
      g.drawLine(j, y_center,//boxBounds.y,
          j + dash, y_center);//boxBounds.y);
    }
    return true;
  }
  
  /*
  protected void drawDashedLines(Graphics g, int x1, int y1, int x2, int y2) {
    for (int i = x1; i < x2; i += 6) {
      int dash = Math.min (4, x2 - i);
      g.drawLine(i, y1, i + dash, y2);
    }
  }
  */

  /*
  public void drawArrow(Graphics g, Rectangle boxBounds, Transformer transformer) {
    FeatureSetI parent = feature.getParent();
    g.setColor(getDrawableColor());
    int x1 = boxBounds.x;
    int y1 = boxBounds.y + (boxBounds.height / 2);
    int x2 = boxBounds.x + boxBounds.width;
    int y2 = y1;
    g.drawLine(x1, y1, x2, y2);
    if (feature == parent.getFeatureAt(0)) {
      g.drawLine(x1, boxBounds.y, x2, y2);
      Transformer.PixelRange range = transformer.basepairRangeToPixelRange(parent.getFeatureAt(1));
      drawDashedLines(g, x2, y1, range.low, y2);
    }
    else {
      g.drawLine(x1, y1, x2, boxBounds.y + boxBounds.height);
    }
  }
  */
  
  public void drawArrow(Graphics g, Rectangle boxBounds, Transformer transformer, TierManagerI manager, PixelMaskI mask, boolean forwardPrimer) {
    setLabeled();
    DrawableUtil.setBoxBounds(this, transformer, manager);

    g.setColor(getDrawableColor());
    int x1 = boxBounds.x;
    int y1 = boxBounds.y + (boxBounds.height / 2);
    int x2 = boxBounds.x + boxBounds.width;
    int y2 = y1;
    g.drawLine(x1, y1, x2, y2);
    if (forwardPrimer) {
      g.drawLine(x1, boxBounds.y, x2, y2);
    }
    else {
      g.drawLine(x1, y1, x2, boxBounds.y + boxBounds.height);
    }

    addHighlights(g,boxBounds,transformer,manager);
    addDecorations(g,boxBounds,transformer,manager);

  }

  
  /*
  public boolean draw(Graphics g, Transformer transformer,
      TierManagerI manager, PixelMaskI mask) {
    setLabeled(getFeatureProperty().getTier().isLabeled());
    DrawableUtil.setBoxBounds(this, transformer, manager);
    
    if (!wantToDraw(g,manager,transformer,boxBounds)) {
      return false;
    }
    
    Drawable fPrimer = getDrawableAt(0);
    Drawable rPrimer = getDrawableAt(1);
    drawArrow(g, fPrimer.getBoxBounds(), transformer);
    drawArrow(g, rPrimer.getBoxBounds(), transformer);
    return true;
  }
  */
  
  /*
  public boolean draw(Graphics g, Transformer transformer,
      TierManagerI manager, PixelMaskI mask) {
    setLabeled(getFeatureProperty().getTier().isLabeled());
    DrawableUtil.setBoxBounds(this, transformer, manager);
    //drawArrow(g, getDrawableAt(0).getBoxBounds(), transformer, manager, mask, true);
    //drawArrow(g, getDrawableAt(1).getBoxBounds(), transformer, manager, mask, false);
    //getDrawableAt(0).draw(g, transformer, manager, mask);
    //getDrawableAt(1).draw(g, transformer, manager, mask);
    
    //Drawable s = getDrawableAt(0); // new DrawableSeqFeature(getDrawableAt(0).getFeature(), true);
    Drawable s = DrawableUtil.createDrawable(getDrawableAt(0).getFeature());
    s.draw(g, transformer, manager, mask);
    
    
    feature_draw(g,transformer,manager,boxBounds);
    return true;
  }
  */
  
}
