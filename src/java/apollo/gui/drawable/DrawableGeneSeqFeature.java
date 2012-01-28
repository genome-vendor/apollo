package apollo.gui.drawable;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Vector;
import java.util.HashMap;
import java.util.Iterator;

import apollo.config.Config;
import apollo.datamodel.*;
import apollo.gui.*;
import apollo.gui.genomemap.*;

/** This is currently used by ensembl - see ensembl.tiers. */
public class DrawableGeneSeqFeature extends DrawableSeqFeature
  implements Drawable, SelectableI {

  Color weirdDonorSpliceColor = Color.orange;
  Color weirdAcceptorSpliceColor = weirdDonorSpliceColor;

  public DrawableGeneSeqFeature() {
    super(true);
  }

  public DrawableGeneSeqFeature(SeqFeatureI feature) {
    super(feature, true);
  }

  public void drawSelected(Graphics g,
                           Rectangle boxBounds,
                           Transformer transformer,
                           TierManagerI manager) {
    // Selection box
    g.setColor(Config.getSelectionColor());
    if (Config.getDraw3D()) {
      g.fill3DRect(boxBounds.x-2,boxBounds.y-2,
                 boxBounds.width+5,boxBounds.height+5,true);
    } else {
      g.fillRect(boxBounds.x-2,boxBounds.y-2,
                 boxBounds.width+5,boxBounds.height+5);
    }

    drawUnselected(g,boxBounds,transformer,manager);
  }


  public void drawUnselected(Graphics g,
                             Rectangle boxBounds, 
                             Transformer transformer,
                             TierManagerI manager) {
    // Internal box
    Color color = getDrawableColor();
    g.setColor(color);
    int leftEdge = boxBounds.x;
    int rightEdge = boxBounds.x + boxBounds.width + 1;

    // Still need this for splice site calls, but trying to make
    // this glyph more useable by  FeatureSets, like the ensembl genes
    // which go in the results view
    ExonI exon = (feature instanceof ExonI) ? (ExonI) feature : null;

    int exonType = feature.getCodingProperties();

    // mix of UTR and coding coloring
    if (exonType != CodingPropertiesI.CODING && exonType != CodingPropertiesI.UNKNOWN) {
      // utr coloring
      //g.setColor(Config.getFeatureBackground());
      g.setColor(getFeatureProperty().getUtrColor());
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
    } 
    else { // CODING or UNKNOWN
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

  /** Draws in translated sequence if feature is an ExonI, its zoomed in,
      seq is available, and it is in a coding region */
  protected void addDecorations(Graphics g,
                             Rectangle box,
                             Transformer transformer,
                             TierManagerI manager) {

    super.addDecorations(g, box, transformer, manager);

    int exonType = feature.getCodingProperties();

    if ((exonType == CodingPropertiesI.MIXED_BOTH ||
         exonType == CodingPropertiesI.MIXED_5PRIME ||
         exonType == CodingPropertiesI.MIXED_3PRIME ||
         exonType == CodingPropertiesI.CODING) &&
        transformer.getXPixelsPerCoord() >= 5 &&
        feature.isSequenceAvailable (feature.getStart())) {

      FeatureSetI trans = (FeatureSetI) feature.getRefFeature();
      int transStart = trans.getTranslationStart();
      int transEnd   = trans.getTranslationEnd();
    
      int [] visRange = transformer.getXVisibleRange();
      int leftCoord = (int) visRange[0];
      int rightCoord = (int) visRange[1];

      boolean forward = transformer.getXOrientation() == Transformer.LEFT;

      String aa = trans.translate();

      int start = (feature.contains (transStart) ?
                   transStart :
                   getStart() + (feature.getPhase() * getStrand()));

      // For trans end, seems to actually want first base of last coding codon
      //  For sequences which have an end specified but which don't end with a
      //  complete codon may need something more complicated than the -3*getStrand()
      //  
      int end = (feature.contains (transEnd) ?
                 (transEnd-3*getStrand()) :
                 getEnd() - (((feature.getEndPhase() + 2) % 3) * getStrand()));

      if (getStrand() == 1) {
        if (leftCoord < start) {
          leftCoord = start;
        } else {
          if (feature.contains (leftCoord)) {
            int base_phase = ((leftCoord - start) % 3);
            leftCoord += (3 - base_phase) % 3;
          } else {
            leftCoord = start;
          }
        }
        if (rightCoord > end) {
          rightCoord = end;
        }
      } else {
        if (leftCoord < end) {
          leftCoord = end;
        } else {
          if (feature.contains (leftCoord)) {
            int base_phase = ((start - leftCoord) % 3);
            leftCoord -= (3 - base_phase) % 3;
          } else {
            leftCoord = end;
          }
        }
        if (rightCoord > start) {
          rightCoord = start;
        }
      }
      
      // get the Sequencing errors and calculate the shifting
      HashMap genomicsErrors = trans.getGenomicErrors();
      int shift = 0;
      if(genomicsErrors != null) {
        Iterator iterator = genomicsErrors.values().iterator();
	while(iterator.hasNext()) {
	  SequenceEdit seqEdit = (SequenceEdit) iterator.next();
	  if(getStrand() < 0 && seqEdit.getPosition() > leftCoord && leftCoord != (transEnd+3))
      	    shift += calcShiftForSequencingError(seqEdit);
	  if(getStrand() > 0 && seqEdit.getPosition() < leftCoord)
      	    shift += calcShiftForSequencingError(seqEdit);
	}
      } 
      
      /* this is okay, because both of these positions occur
         before there is the need to account for any sequencing
         edits. Or at least sequencing edits that affect the ORF
         Might need to revisit to account for genomic edits in
         the UTRs. */
      int aa_offset
        = (int) ((trans.getFeaturePosition(leftCoord) -
                  trans.getFeaturePosition(transStart)) / 3);
      if (aa_offset < 0) {
        return;
      }
      
      if(genomicsErrors != null) {      
        if (getStrand() > 0) { 
          if(leftCoord == start && aa_offset>3) {
	    aa_offset-=shift;
	    leftCoord-=(shift*3);
	  }
        } else {
          if(rightCoord == start) {
	    rightCoord-=(shift*3);
	  }
	}
      }
      
      FontMetrics fm = g.getFontMetrics();
      int res_y = getCharY (fm, box);

      // Set font color for residues based on how dark annotation color is
      Color labelColor = residueColorForTranscript(trans);

      g.setColor(labelColor);

      for (int i = leftCoord; i <= rightCoord; i += 3) {
        String res;
        try {
          // Sometimes aa_offset > aa.length()--not sure why.
          if (aa_offset >= aa.length()) {
            //            System.out.println("DrawableGeneSeqFeature: aa_offset " + aa_offset + "> aa.length + " + aa.length() + " for " + getName());
            return;
          }
          else if ((aa_offset + 1) < aa.length())
            res = aa.substring(aa_offset, aa_offset + 1);
          else 
            res = aa.substring(aa_offset);
          aa_offset += getStrand();
        } catch (Exception e) {
          System.out.println ("DrawableGeneSeqFeature " +
                              "(object class = " + getClass().getName() + "): " +
                              "caught exception: name = " + getName() +
                              ", leftCoord = " + leftCoord +
                              " on feature at " +
                              trans.getFeaturePosition(leftCoord) +
                              ", TSS = " + transStart +
                              " on feature at " +
                              trans.getFeaturePosition(transStart) +
                              ", leftVisible = " + visRange[0] +
                              ", feature Tstart = " + start +
                              ", aa_offset = " + aa_offset +
                              ", for peptide " + // aa +
                              "of length " + aa.length());
          //          e.printStackTrace();
          return;
        }
        
	int x_position = i + shift;
	//don't display the aa out of range
	if(
	(getStrand() > 0 && x_position>=feature.getStart() && x_position <= feature.getEnd())
	 || (getStrand() < 0 && x_position>=feature.getEnd() && x_position <= feature.getStart())
	) {
	  
	  if(genomicsErrors != null && (
	     genomicsErrors.get(""+i) != null ||
	     genomicsErrors.get(""+(i+1)) != null ||
	     genomicsErrors.get(""+(i+2)) != null)
	  ) {
	    if(getStrand() > 0) {
	      shift+=calcShiftForSequencingError((SequenceEdit) genomicsErrors.get(""+i));
	      shift+=calcShiftForSequencingError((SequenceEdit) genomicsErrors.get(""+(i+1)));
	      shift+=calcShiftForSequencingError((SequenceEdit) genomicsErrors.get(""+(i+2)));
	    } else {
	      shift-=calcShiftForSequencingError((SequenceEdit) genomicsErrors.get(""+i));
	      shift-=calcShiftForSequencingError((SequenceEdit) genomicsErrors.get(""+(i+1)));
	      shift-=calcShiftForSequencingError((SequenceEdit) genomicsErrors.get(""+(i+2)));	  
	    }
	  }
         
	  int res_x = ( forward ?
                      transformer.toPixelX ( (int) x_position - 1 ) :
                      transformer.toPixelX ( (int) x_position ) );
        
          res_x += (int) ((transformer.getXPixelsPerCoord() / 2) -
                        (fm.stringWidth(res) / 2));
        
          g.drawString(res, res_x, res_y);
	}
      }
    }
  }
  
  private int calcShiftForSequencingError(SequenceEdit seqEdit) {
    int shift = 0;
    if(seqEdit != null) {
      if(seqEdit.isDeletion())
        shift = 1;
      else if(seqEdit.isInsertion())
        shift = -1;
      shift *= getStrand();	
    }
    return shift;
  }
  
  // Use white for showing residues on dark-colored transcripts, 
  // black on light-colored transcripts.
  protected Color residueColorForTranscript(FeatureSetI fs) {
    Color fs_color;
    if (fs instanceof Transcript) {
      Transcript transcript = (Transcript) fs;
      String owner = Config.getProjectName(transcript.getOwner());
      fs_color = Config.getAnnotationColor(owner, 
                                           getFeatureProperty());
    } else {
      fs_color = getDrawableColor();
    }
    //    int lightness = (fs_color.getRed() + 
    //                     fs_color.getGreen() + 
    //                     fs_color.getBlue());
    return (fontColorForBackground(fs_color));
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
