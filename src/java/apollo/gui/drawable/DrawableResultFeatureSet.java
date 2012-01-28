package apollo.gui.drawable;

import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.*;

import apollo.datamodel.*;
import apollo.config.Config;
import apollo.gui.TierManagerI;
import apollo.gui.Transformer;
import apollo.gui.event.*;

/**
 * A drawable for drawing result (computational analysis) feature sets.
 * The main point of this class is to draw a dashed intron line in certain
 * cases, solid in others. Game uses dashes in its results. A dashed line is drawn
 * if 2 children have hit feature ref seq display ids that are different.
 * A more general
 * name for this class might be DashedIntronFeatureSet or something like that.
 * The model FeatureSet has to have FeaturePairs for children.
 * Another thought is to just parameterize the different ways of drawing intron
 * lines, not sure how that would jibe with tiers file.
 */
public class DrawableResultFeatureSet extends DrawableFeatureSet {

  public DrawableResultFeatureSet() {
    super(true);
  }

  public DrawableResultFeatureSet(FeatureSetI feature) {
    super(feature, true);
  }

  public Drawable createDrawable (SeqFeatureI sf) {
    return new DrawableResultSeqFeature(sf);
  }
  
  /** Draws either a dashed or solid intron line. Draws dashed line if the
      2 hit features ref seqs have different display ids */
  protected boolean drawDashedLines(Graphics g,
				    Rectangle boxBounds,
				    Transformer transformer,
				    TierManagerI manager,
				    Color color,
				    int y_center) {
    boolean drawn = false;
    if (Config.getDashSets()) {
      for (int i = 0; i < size(); i++) {
        if ((i + 1) < size()) {
          Drawable dsf_1 = getDrawableAt(i);
          Drawable dsf_2 = getDrawableAt(i+1);
          if (dsf_1.getFeature() != null &&
              dsf_2.getFeature() != null &&
              dsf_1.getFeature() instanceof FeaturePairI &&
              dsf_2.getFeature() instanceof FeaturePairI) {
            FeaturePairI fp_1 = (FeaturePairI) dsf_1.getFeature();
            FeaturePairI fp_2 = (FeaturePairI) dsf_2.getFeature();
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
            if (fp_1.getHitFeature() != null &&
                fp_2.getHitFeature() != null) {
              SequenceI seq_1 =
                  fp_1.getHitFeature().getRefSequence();
              SequenceI seq_2 =
                  fp_2.getHitFeature().getRefSequence();
              
              // Draw solid line if 2 hit display ids are the same or null
              if (seq_1 == null ||
                  seq_1.getName() == null ||
                  seq_2 == null ||
                  seq_2.getName() == null ||
                  seq_1.getName().equals (seq_2.getName())) {
                
                g.setColor(color);
                
                // boxBounds.y is the top of the box - 
                // this worked before only because
                // a feature set box had zero height. 
                // No longer true. Now it uses
                // the y value of the center of the tier as it should.
                g.drawLine(left_end, y_center,//boxBounds.y,
                           right_end, y_center);//boxBounds.y);
              } 
              
              /* Draw dashed line - 2 ids are different
              // These dashes dont necessarily line up with other 
              // resFeatSets dashes, they should so that when its
              collapsed it still appears dashed. Presently on 
              collapse it appears solid. Got to get the left ends
              in synch. Dashed line drawn if 2 hit display ids
              are different */
              else {
                g.setColor(Config.getSeqGapColor());
                for (int j = left_end; j < (right_end); j += 6) {
                  int dash = Math.min (4, right_end - j);
                  g.drawLine(j, y_center,//boxBounds.y,
                             j + dash, y_center);//boxBounds.y);
                }
              }
              drawn = true;
            }
          }
        }
      }
    }
    return drawn;
  }

}
