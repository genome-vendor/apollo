package apollo.gui.drawable;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Vector;

import apollo.config.Config;
import apollo.datamodel.*;
import apollo.gui.*;
import apollo.gui.genomemap.*;

public class DrawablePhaseHighlightGeneSeqFeature extends DrawableGeneSeqFeature
  implements Drawable, SelectableI {

  public DrawablePhaseHighlightGeneSeqFeature() {
    super();
  }

  public DrawablePhaseHighlightGeneSeqFeature(SeqFeatureI feature) {
    super(feature);
  }

  public void drawUnselected(Graphics g,
                             Rectangle boxBounds, 
                             Transformer transformer,
                             TierManagerI manager) {
    // Still need this for splice site calls, but trying to make
    // this glyph more useable by  FeatureSets, like the ensembl genes
    // which go in the results view
    ExonI exon = (feature instanceof ExonI) ? (ExonI) feature : null;

    int exonType = feature.getCodingProperties();


    int featurePhase = feature.getPhase();
    // tweak first exon colour if is mixed UTR and coding because phase is a bit
    // odd (it depends on the length of the 5' UTR which is just strange) in apollo 
    // for mixed 5' UTR/coding exons. As it has UTR it is hopefully complete or
    // at least starting with a complete codon so set phase to 0

    if (exonType == CodingPropertiesI.MIXED_5PRIME) {
      featurePhase = 0;
    } else if (exonType ==  CodingPropertiesI.MIXED_BOTH) {
      featurePhase = 0;
    }

    // Internal box
    Color color = getDrawableColor();

    if (featurePhase == 1) {
      color = color.darker().darker();
    }
    if (featurePhase == 2) {
      color = color.brighter().brighter();
    }
    g.setColor(color);
    int leftEdge = boxBounds.x;
    int rightEdge = boxBounds.x + boxBounds.width + 1;


    // mix of UTR and coding coloring or UNKNOWN coding property type
    if (exonType != CodingPropertiesI.CODING) {

      // utr coloring
      Color utrColor = manager.getView() instanceof LinearView ? ((LinearView)(manager.getView())).getBackgroundColour() : getFeatureProperty().getUtrColor();
      g.setColor(utrColor);
      g.fillRect(boxBounds.x+1, boxBounds.y+1,
                 boxBounds.width-1, boxBounds.height-1);
      // Do the outline draw after the background fill so printed output looks correct
      g.setColor(color);

      g.drawRect(boxBounds.x,boxBounds.y,boxBounds.width,boxBounds.height);
      
      if (exonType == CodingPropertiesI.MIXED_BOTH || 
          exonType == CodingPropertiesI.MIXED_5PRIME || 
          exonType == CodingPropertiesI.MIXED_3PRIME) {
        // Don't change the actual leftEdge and rightEdge because they may be
        // used later to draw unusual donor/acceptor site
        int left_edge = leftEdge + 1;
        int right_edge = rightEdge - 1;
        FeatureSetI trans = (FeatureSetI)(feature.getRefFeature());
     
        if ((feature.getStrand() == 1 && 
             transformer.getXOrientation() == Transformer.LEFT) ||
            (feature.getStrand() == -1 && 
             transformer.getXOrientation() == Transformer.RIGHT)) {
          if (exonType == CodingPropertiesI.MIXED_BOTH) {
            left_edge = transformer.toPixelX(trans.getTranslationStart());
            right_edge = transformer.toPixelX(trans.getTranslationEnd());
          } else if (exonType == CodingPropertiesI.MIXED_5PRIME) {
            left_edge = transformer.toPixelX(trans.getTranslationStart());
          } else if (exonType == CodingPropertiesI.MIXED_3PRIME) {
            right_edge = transformer.toPixelX(trans.getTranslationEnd());
          }
        } 
        else {
          if (exonType == CodingPropertiesI.MIXED_BOTH) {
            left_edge = transformer.toPixelX(trans.getTranslationEnd());
            right_edge = transformer.toPixelX(trans.getTranslationStart());
          } else if (exonType == CodingPropertiesI.MIXED_5PRIME) {
            right_edge = transformer.toPixelX(trans.getTranslationStart());
          } else if (exonType == CodingPropertiesI.MIXED_3PRIME) {
            left_edge = transformer.toPixelX(trans.getTranslationEnd());
          }
        }
        g.fillRect(left_edge,boxBounds.y,
                   (right_edge-left_edge+1),boxBounds.height+1);
      }
    } else if (exonType == CodingPropertiesI.CODING) { 
      g.fillRect(leftEdge, boxBounds.y,
                 (rightEdge - leftEdge), boxBounds.height+1);
    }

    // Indicate unusual splice acceptor site
    if (exon != null && exon.isNonConsensusAcceptor()) {
      int point_len = (((getStrand() >= 0 &&
                         transformer.getXOrientation()==Transformer.LEFT)
                        ||
                        (getStrand() < 0 &&
                         transformer.getXOrientation()==Transformer.RIGHT)) ?
                       boxBounds.height : -boxBounds.height);
      /* don't bother drawing this if the resolution is too low */
      if (Math.abs(point_len) < boxBounds.width + 1) {
        int splice_x [] = new int[3];
        int splice_y [] = new int[3];
        int center_y = getYCentre(boxBounds);
        int edge = (((getStrand() >= 0 &&
                      transformer.getXOrientation()==Transformer.LEFT)
                     ||
                     (getStrand() < 0 &&
                      transformer.getXOrientation()==Transformer.RIGHT)) ?
                       leftEdge : rightEdge);
        splice_x[0] = edge;
        splice_y[0] = boxBounds.y;
        splice_x[1] = edge + point_len;
        splice_y[1] = center_y;
        splice_x[2] = edge;
        splice_y[2] = boxBounds.y + boxBounds.height + 1;
        // g.setColor(getFeatureProperty().getUtrColor());
        g.setColor(weirdAcceptorSpliceColor);
        g.fillPolygon (splice_x, splice_y, 3);
      }
    }

    // Indicate unusual splice donor site
    if (exon != null && exon.isNonConsensusDonor()) {
      int point_len = (((getStrand() >= 0 &&
                         transformer.getXOrientation()==Transformer.LEFT)
                        ||
                        (getStrand() < 0 &&
                         transformer.getXOrientation()==Transformer.RIGHT)) ?
                       boxBounds.height : -boxBounds.height);
      /* don't bother drawing this if the resolution is too low */
      if (Math.abs(point_len) < boxBounds.width + 1) {
        int splice_x [] = new int[3];
        int splice_y [] = new int[3];
        int center_y = getYCentre(boxBounds);
        int edge = (((getStrand() >= 0 &&
                      transformer.getXOrientation()==Transformer.LEFT)
                     ||
                     (getStrand() < 0 &&
                      transformer.getXOrientation()==Transformer.RIGHT)) ?
                       rightEdge : leftEdge);
        splice_x[0] = edge;
        splice_y[0] = boxBounds.y;
        splice_x[1] = edge - point_len;
        splice_y[1] = center_y;
        splice_x[2] = edge;
        splice_y[2] = boxBounds.y + boxBounds.height + 1;
        //g.setColor(getFeatureProperty().getUtrColor());
        g.setColor(weirdDonorSpliceColor);
        g.fillPolygon (splice_x, splice_y, 3);
      }
    }


    if (Config.getDrawOutline()) {
      g.setColor(Config.getOutlineColor());
      g.drawRect(boxBounds.x,boxBounds.y,boxBounds.width,boxBounds.height);
    }
  }

  // Disable evidence finding for exons - I'm doing it on transcripts so don't want the slowness searching exons will cause.
  public void setHighlights(boolean state) {
  }
}
