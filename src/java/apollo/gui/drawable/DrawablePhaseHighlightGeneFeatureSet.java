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

import apollo.gui.synteny.*;

/**
 * A drawable for drawing result (computational analysis) feature sets.
 * This draws the intron line as an "arc" (line up to midpoint and down from 
 * midpoint. This is how ensembl indicates that a feature is a gene. 
 * (If we reuse this for other non gene feats we may want to rename it 
 * ArcedIntronLineFeatureSet or something like that)
 */
public class DrawablePhaseHighlightGeneFeatureSet extends DrawableGeneFeatureSet
  implements DrawableSetI {

  public Drawable createDrawable (SeqFeatureI sf) {
    return new DrawablePhaseHighlightGeneSeqFeature(sf);
  }

  public void setHighlights(boolean state) {
    if (feature instanceof AnnotatedFeatureI) {
      AnnotatedFeatureI gi = (AnnotatedFeatureI)feature;
      Vector evidence = gi.getEvidence();
      if (evidence.size() != 0) {
        if (gi.getEvidenceFinder() != null) {
          EvidenceFinder ef = gi.getEvidenceFinder();
          for (int i=0; i<evidence.size(); i++) {
            String evidenceId = ((Evidence)evidence.elementAt(i)).getFeatureId();
            SeqFeatureI sf = ef.findEvidence(evidenceId);
            if (sf instanceof Drawable) {
              Drawable dsf = (Drawable)sf;
              dsf.setHighlighted(state);
            } else {
// This is an insane way to get the drawable but this is very experimental
              Drawable d = CurationManager.getCurationManager().getActiveCurState().getSZAP().findDrawableForFeatureInResultViews(sf);
              if (d != null) {
                d.setHighlighted(state);
              }
            }
          }
        }
      }
    }
  }

  public void  setSelected(boolean state) {
    super.setSelected(state);

    setHighlights(state);
  }


}
