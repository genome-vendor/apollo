package apollo.gui.genomemap;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import apollo.util.FeatureList;
import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.config.PropertyScheme;
import apollo.datamodel.*;
import apollo.seq.*;
import apollo.gui.event.*;
import apollo.gui.drawable.*;
import apollo.gui.*;
import apollo.gui.menus.*;

import org.bdgp.util.DNAUtils;

/** Not used - work in progress or pase? */

public class TranslationView extends ScrollAdjustedView {
  private CurationSet        cset;
  private boolean            showSequence = true;
  //  private boolean            complement   = false;
  private static int         rowLocations[] = new int [] {-3, 3, 10};


  // public TranslationView() {} - doesnt compile

  public TranslationView(JComponent ap, String name) {
    super(ap,name,false,31);
    transformer.setYRange(new int [] {-10,10});
    transformer.setXRange(new int [] {-10,10});
  }

  public void setCurationSet(CurationSet cset) {
    this.cset = cset;
  }

  public void setShowSequence(boolean flag) {
    this.showSequence = flag;
  }

  // Overridden LinearView methods
  public void paintView() {
    graphics.setColor(getBackgroundColour());

    // Clear the whole view
    graphics.fillRect(getBounds().x,
                      getBounds().y,
                      getBounds().width,
                      getBounds().height);


    graphics.setColor(getForegroundColour());

    if (showSequence == true && cset.getRefSequence() != null) {
      drawSequence(graphics,transformer);
    }
  }

  public void drawSequence(Graphics graphics, Transformer transformer) {
    int [] visRange = getVisibleRange();
    int startCoord = (int)visRange[0];
    int endCoord   = (int)visRange[1];
    String aa[] = new String[3];

    if (startCoord < cset.getLow()) {
      startCoord = (int)cset.getLow();
    }
    if (endCoord > (int)cset.getHigh()) {
      endCoord = (int)cset.getHigh();
    }

    PropertyScheme ps = Config.getPropertyScheme();
    if (endCoord > cset.getLow()-1) {
      graphics.setColor(apollo.config.Config.getCoordForeground());
      if (transformer.getXPixelsPerCoord() >= 1) {
        int relLow = (!reverseComplement ? startCoord : endCoord);
        int relHigh = (!reverseComplement ? endCoord : startCoord);
        String visSeq 
	  = cset.getRefSequence().getResidues(relLow,
					      relHigh).toUpperCase();

	// Drawing reverse complement goes from right to left so that the
	// residues should be drawn to the left of the residue position 
	// on the scale bar not to the right of it, so we right justify them.
        aa[0] = (DNAUtils.translate (visSeq,
                                     DNAUtils.FRAME_ONE,
                                     DNAUtils.ONE_LETTER_CODE));
        aa[1] = (DNAUtils.translate (visSeq,
                                     DNAUtils.FRAME_TWO,
                                     DNAUtils.ONE_LETTER_CODE));
        aa[2] = (DNAUtils.translate (visSeq,
                                     DNAUtils.FRAME_THREE,
                                     DNAUtils.ONE_LETTER_CODE));
        if (reverseComplement) {
          for (int i=0; i<3; i++) {
            aa[i] = tidyEnd(aa[i]);
            aa[i] = new StringBuffer(aa[i]).reverse().toString();
          }
        }
        if ((startCoord-1)%3 == 1 && !reverseComplement) {
          String tmp = aa[0];
          aa[0] = aa[2];
          aa[2] = aa[1];
          aa[1] = tmp;
        }
        else if ((startCoord-1)%3 == 2 && !reverseComplement) {
          String tmp = aa[0];
          aa[0] = aa[1];
          aa[1] = aa[2];
          aa[2] = tmp;
        }
        else if ((endCoord)%3 == 0 && reverseComplement) {
          String tmp = aa[0];
          aa[0] = aa[2];
          aa[2] = tmp;
        }
        else if ((endCoord)%3 == 2 && reverseComplement) {
          String tmp = aa[0];
          aa[0] = aa[1];
          aa[1] = tmp;
        }
        else if ((endCoord)%3 == 1 && reverseComplement) {
          String tmp = aa[1];
          aa[1] = aa[2];
          aa[2] = tmp;
        }
         System.out.println("aa[0] = " + aa[0]);
         System.out.println("aa[1] = " + aa[1]);
         System.out.println("aa[2] = " + aa[2]);

        if (transformer.getXPixelsPerCoord() >= 5) {
          int dnaPos[] = new int[3];
          dnaPos[0] = dnaPos[1] = dnaPos[2] = 0;

          for (int i = 0; i < endCoord-startCoord + 1; i++) {
            graphics.setColor(apollo.config.Config.getCoordForeground());
            
            int arrInd = (i+startCoord-1)%3;
 
            Point aastart = transformer.toPixel(new Point(i+startCoord,rowLocations[arrInd]));
            SeqFeatureI sf = (getFeatureAtBase((int)i+startCoord));
            if ( sf != null && sf.getPhase() != -1) {
              // System.out.println("Group type = " + sf.getType());
              Integer grpFlag = ps.getFeatureProperty(sf.getFeatureType()).getGroupFlag();
              if (grpFlag == FeatureProperty.GRP_GENE) {
                graphics.setColor (Color.blue);
                System.out.println("Got phase " + sf.getPhase());
        
                int phaseInd;
                int phase = ((3-sf.getPhase())%3);
                if (!reverseComplement) {
                  phaseInd  = (sf.getStart() + ((3-phase)%3)-1)%3;
                } else {
                  phaseInd  = (sf.getStart() - ((3-phase)%3)-1)%3;
                }

                Point top_corner = transformer.toPixel(
                                          new Point(i+startCoord+1,rowLocations[(int)phaseInd]-5));
                if (!reverseComplement) {
                  int width = top_corner.x - aastart.x;
                  graphics.fillRect (aastart.x,
                                     top_corner.y,
                                     width,
                                     8);
                } else {
                  int width = aastart.x - top_corner.x;
                  graphics.fillRect (top_corner.x-1,
                                     top_corner.y,
                                     width,
                                     8);
                }
                graphics.setColor(apollo.config.Config.getCoordForeground());
              }
            }
            if (dnaPos[arrInd] < aa[arrInd].length()) {
              String seq = aa[arrInd].substring(dnaPos[arrInd],++dnaPos[arrInd]);
              if (seq.equals("*")) {
                graphics.setColor(Color.red);
              }
              int xpos;
              int width;
              Point top_corner = transformer.toPixel(new Point(i+startCoord+transformer.getXOrientation(),rowLocations[arrInd]));
              if (!reverseComplement) {
                width = top_corner.x - aastart.x;
                xpos = aastart.x;
              } else {
                width = aastart.x - top_corner.x;
                xpos = top_corner.x;
              }
              graphics.drawString(seq, (xpos+width/2), aastart.y);
            }
          }
        }
      }
    }
  }


  private String tidyEnd(String orig) {
    if (orig.endsWith("  ")) {
      return orig.substring(0,orig.length()-1);
    }
    return orig;
  }

  private SeqFeatureI getFeatureAtBase (int base_position) {
    Selection selection = ((ApolloPanelI)getComponent()).getSelection();

    SeqFeatureI selected = null;

    for (int i = 0; i < selection.size() && selected == null; i++) {
      SeqFeatureI sf = selection.getSelectedData(i);
      //System.out.println("Looking for " + sf + " at " + base_position);
      selected = featureContains (sf, base_position);
    }
    return selected;
  }


  // Class specific methods
  public Selection findSelected(FeatureSelectionEvent evt) {
    FeatureList    features = evt.getFeatures();
    Selection selection = new Selection();
    // System.out.println("Looking for selection " + selection);
    for (int i = 0; i < features.size(); i++) {
      SeqFeatureI sf  = features.getFeature(i);
      if (sf.getFeatureType().equals ("String"))
        selection.add (new SelectionItem(this,sf));
    }
    return selection;
  }

}
