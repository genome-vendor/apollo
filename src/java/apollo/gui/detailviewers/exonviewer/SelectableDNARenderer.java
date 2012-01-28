package apollo.gui.detailviewers.exonviewer;

import javax.swing.*;
import java.awt.*;
import java.util.*;

import apollo.datamodel.*;
import apollo.seq.*;
import apollo.gui.drawable.DrawableSeqFeature;
import apollo.gui.drawable.DrawableUtil;

public class SelectableDNARenderer extends DefaultBaseRenderer {
  protected final Color [][] transcriptColorList;

  protected int transcriptColorIndex = 0;
  protected int exonColorIndex = 0;

  SeqAlignPanel baseEditor;

  double currentRange [] = new double[2];
  double transcriptRange [] = new double[2];

  double oldRange [] = new double[2];
  SeqFeatureI currentFeature;
  FeatureSetI currentFeatureSet;
  int type;
  boolean isIntron;
  boolean isExon;

  int targetNucleotide = -1;
  int targetTier = -1;
  int hitIndex;

  Color targetColor;
  Color seqSelectColor;
  Color hatchColor;
  //  Color shiftColor = new Color(255, 153, 0); // bright orange
  Color shiftColor = apollo.config.Config.getStyle().getSeqErrorColor();

  public SelectableDNARenderer(SeqAlignPanel baseEditor,
                               int width, int height) {
    super(width, height);
    this.baseEditor = baseEditor;
    if (baseEditor instanceof BaseEditorPanel) {
      transcriptColorList = ((BaseEditorPanel) baseEditor).getColorArray();
      setSeqSelectColor(((BaseEditorPanel)baseEditor).getSeqSelectColor());
    }
    else {
      transcriptColorList = new Color[1][1];
      transcriptColorList[0][0] = Color.blue;
      setSeqSelectColor(Color.pink);
    }
    //    setTargetColor(Color.yellow);
    setTargetColor(shiftColor);
  }

  public void setTargetPos(int pos, int tier) {
    targetNucleotide = pos;
    targetTier = tier;
  }

  public void setTargetColor(Color in) {
    targetColor = in;
  }

  public void setSeqSelectColor(Color in) {
    seqSelectColor = in;
  }

  public void paintNotify() {
    currentRange[0] = Double.POSITIVE_INFINITY;
    currentRange[1] = Double.NEGATIVE_INFINITY;
    transcriptRange[0] = Double.POSITIVE_INFINITY;
    transcriptRange[1] = Double.NEGATIVE_INFINITY;
    currentFeature = null;
    transcriptColorIndex = 0;
    exonColorIndex = 0;
    hitIndex = 0;
  }

  /** Get component to be rendered, if pos outside of current range
      getFeatureAtPosition and reset currentRange, if feature is non null
      and not an instance of FeatureSetI then its an exon, and set isExon flag
  */
  public Component getBaseRendererComponent(char base,
                                            int pos,
                                            int tier,
                                            SequenceI seq) {
    init (base, pos, tier, seq);

    hatchColor = null;
    if (((BaseEditorPanel) baseEditor).getShowHitZones()) {
      Vector hitZones = ((BaseEditorPanel) baseEditor).hitZones;


      for(int hitIndex = 0; hitIndex < hitZones.size(); hitIndex++) {
        int [] hitZone = (int []) hitZones.elementAt(hitIndex);

        if (pos >= hitZone[0] && pos < hitZone[1]) {
          hatchColor = Color.yellow;
          break;
        }
      }
    }

    currentFeature = baseEditor.getFeatureAtPosition(pos, tier);
    double [] range = establishRange (currentFeature, pos, tier, false);
    
    if (range[1] != currentRange[1] || range[0] != currentRange[0]) {
      currentRange[0] = range[0];
      currentRange[1] = range[1];

      currentFeatureSet = baseEditor.getFeatureSetAtPosition(pos,tier);
      range = establishRange (currentFeatureSet, pos, tier, true);
      transcriptRange[0] = range[0];
      transcriptRange[1] = range[1];

      if (currentFeature != null) {
	// This decides the index into the color array, transcripts
	// can have different colors for their features
        transcriptColorIndex = (baseEditor.getRangeIndex(tier,
                                (int) currentRange[0],
                                (int) currentRange[1]))
                               % transcriptColorList.length;
        exonColorIndex = (baseEditor.getExonRangeIndex(tier,
                          (int) currentRange[0],
                          (int) currentRange[1]))
                         % transcriptColorList[transcriptColorIndex].length;
      }
      if (currentFeature == null) {
        isExon = false;
        isIntron = false;
      //} else if (currentFeature.canHaveChildren()) {
      } else if (currentFeature.hasKids()) { // 1 level annots can but dont
        isExon = false;
        isIntron = true;
      } else {
        isExon = true;
        isIntron = false;
      }
    }
    return this;
  }

  /* Establish the currentRange of bases covered by the
     currentFeature of this renderer
  */
  public double [] establishRange (SeqFeatureI sf,
                                   int pos,
                                   int tier,
                                   boolean use_set) {
    double range [];
    if (sf == null ||
        (!use_set && (sf.canHaveChildren()))) {
      range = baseEditor.getRangeAtPosition(tier, pos);
    } else {
      range = new double[2];
      range[0] = (double)baseEditor.basePairToPos(sf.getLow());
      range[1] = (double)baseEditor.basePairToPos(sf.getHigh());
    }
    return range;
  }

  public Color getHatchColor() {
    return hatchColor;
  }

  public Color getBackgroundBoxColor() {
    if (pos == targetNucleotide && tier == targetTier)
      return targetColor;
    int lowSeqSelect;
    int highSeqSelect;
    if (baseEditor instanceof BaseEditorPanel) {
      lowSeqSelect = ((BaseEditorPanel) baseEditor).selectLowPos();
      highSeqSelect = ((BaseEditorPanel) baseEditor).selectHighPos();
    } else {
      lowSeqSelect = -1;
      highSeqSelect = -1;
    }

    if (lowSeqSelect != -1 && highSeqSelect != -1 &&
        pos >= lowSeqSelect && pos <= highSeqSelect) {
      return seqSelectColor;
    }

    int basePos = baseEditor.posToBasePair (pos);

    if (((BaseEditorPanel) baseEditor).isSequencingErrorPosition(basePos))
      return shiftColor;

    if (isExon) {
      ExonI exon = null;
      if (currentFeature instanceof ExonI)
        exon = (ExonI) currentFeature;
      else if (currentFeature instanceof DrawableSeqFeature)
        exon = (ExonI) ((DrawableSeqFeature) currentFeature).
               getFeature();
      if (exon != null && exon.getTranscript() != null) {
        Transcript transcript = exon.getTranscript();
        int tss = (int) exon.getTranscript().getTranslationStart();
        if (basePos == tss ||
            basePos == (tss + exon.getStrand()) ||
            basePos == (tss + (2 * exon.getStrand())))
          return DrawableUtil.getStartCodonColor(transcript);
        else {
          int tes = (int) transcript.getTranslationEnd();
          if (basePos == tes ||
              basePos == (tes + exon.getStrand()) ||
              basePos == (tes + (2 * exon.getStrand())))
            return Color.red;
          else {
            int shift_pos
              = (int) transcript.plus1FrameShiftPosition();
            if (basePos == shift_pos)
              return shiftColor;
            else {
              shift_pos
                = (int) transcript.minus1FrameShiftPosition();
              if (basePos == shift_pos)
                return shiftColor;
              else {
                int stop_pos
                  = (int) transcript.readThroughStopPosition();
                if (basePos == stop_pos ||
                    basePos == (stop_pos + exon.getStrand()) ||
                    basePos == (stop_pos + (2 * exon.getStrand())))
                  return Color.pink;
              }
            }
          }
        }
      }
      return transcriptColorList[transcriptColorIndex][exonColorIndex];
    }
    return null;
  }

  public Color getBackgroundLineColor() {
    if (pos == targetNucleotide && tier == targetTier)
      return null;
    if (isIntron)
      return transcriptColorList[transcriptColorIndex][0];
    return null;
  }

  public Color getTextColor() {
    return Color.white;
  }

  public void paint(Graphics g) {
    g.setFont(getFont());

    Color boxColor = getBackgroundBoxColor();
    Color hatchColor = getHatchColor();
    Color backgroundLineColor = getBackgroundLineColor();
    //    Color textColor = getForeground();
    int outlineType = getOutlineType();
    Color outlineColor = getOutlineColor();

    if (backgroundLineColor != null) {
      g.setColor(backgroundLineColor);
      g.fillRect(0,getSize().height/2 - 1, getSize().width,
                 2);
    }

    if (boxColor != null) {
      g.setColor(boxColor);
      g.fillRect(0,0,getSize().width,getSize().height);
    }

    if (hatchColor != null) {
      g.setColor(hatchColor);

      // Bev thinks the cross-hatching makes it too hard to read the base.
      // For a single diagonal line, replace the following three lines with
      // g.drawLine(0,getSize().height, getSize().width,0);
      // For an underline, replace with 
      // g.drawLine(0,getSize().height, getSize().width,getSize().height);

      // Draw three cross-hatch lines
      // g.drawLine(0,0,getSize().width,getSize().height);
      // g.drawLine(getSize().width/2,0,getSize().width,getSize().height/2);
      // g.drawLine(0,getSize().height/2,getSize().width/2,getSize().height);

      // Draw a line at top and bottom of matching base
      g.drawLine(0,getSize().height, getSize().width,getSize().height);  // bottom
      g.drawLine(0,0, getSize().width,0); // top
      //      g.drawLine(0,0, 0,getSize().height);  // left side
      //      g.drawLine(getSize().width,0, getSize().width,getSize().height);  // right side
    }

    if (outlineType != NO_OUTLINE && outlineColor != null) {
      g.setColor(outlineColor);
      g.drawLine(0,0,getSize().width-1,0);
      g.drawLine(0,getSize().height-1,getSize().width-1,getSize().height-1);
      if (outlineType == LEFT_OUTLINE)
        g.drawLine(0,0,0,getSize().height-1);
      else if (outlineType == RIGHT_OUTLINE) {
        g.drawLine(getSize().width-1,0,
                   getSize().width-1,getSize().height-1);
      }
    }

    if (getTextColor() != null) {
      int leadDistance = (getSize().width - metrics.charWidth(c))/2;
      g.setColor(getTextColor());
      g.drawString(c+"",leadDistance,getSize().height-2);
    }
  }

  /** Checks if selected or selected refs feature is the current feature set,
      so an exon as well as a transcript will have an outline type */
  public int getOutlineType() {
    SeqFeatureI selected = null;
    if (baseEditor instanceof BaseEditorPanel) {
      selected = ((BaseEditorPanel) baseEditor).getSelectedFeature();
    }
    if (featSetContainsFeat(currentFeatureSet,selected)) {
      if (pos == transcriptRange[0])
        return LEFT_OUTLINE;
      else if (pos == transcriptRange[1])
        return RIGHT_OUTLINE;
      else
        return CENTER_OUTLINE;
    }
    return NO_OUTLINE;
  }

  /** Checks if selected or selected refs feature is the feature set at current pos, so an 
      exon as well as a transcript will have an outline color */
  public Color getOutlineColor() {
    SeqFeatureI selected = null;
    if (baseEditor instanceof BaseEditorPanel) {
      selected = ((BaseEditorPanel) baseEditor).getSelectedFeature();
    }
    FeatureSetI featureSet = baseEditor.getFeatureSetAtPosition(pos,tier);
    if (featSetContainsFeat(featureSet,selected)) 
      return Color.magenta;
    else
      return null;
  }

  /** True if feat or feats refFeat is fs */
  private boolean featSetContainsFeat(FeatureSetI fs, SeqFeatureI feat) {
    if (fs == null || feat == null) return false;
    if (fs == feat) return true;
    if (fs == feat.getRefFeature()) return true;
    // This should probably be generalized(recursive) for whole ancestor tree and added to
    // FeatureSetI - for now only need to do feat and ref feat
    // could use FeatureSet.find i guess - is that what thats intended for? 
    return false;
  }

  public void setTier(int tier) {
    this.tier = tier;
  }

}
