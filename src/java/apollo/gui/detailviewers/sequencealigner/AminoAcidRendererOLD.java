package apollo.gui.detailviewers.sequencealigner;
// could go in detailviewers, but for now only used by exonviewer

import javax.swing.*;
import java.awt.*;

import java.util.*;

import apollo.datamodel.*;

public class AminoAcidRendererOLD extends DefaultBaseRenderer {

  private static Hashtable colors = new Hashtable();
  Color bgcolor;
  Color disabled = new Color(200,200,200);

  int   phase;
  Font regularFont = apollo.config.Config.getExonDetailEditorSequenceFont();
  Font boldFont = apollo.config.Config.getExonDetailEditorSequenceFont();
  SeqAlignPanel panel;

  Color hatchColor;

  static {
    colors.put("M", Color.green);
    colors.put("*", Color.red);
  }

  public AminoAcidRendererOLD(int width, int height, int phase,
                           SeqAlignPanel panel) {
    super(width,height);
    setFontSizes(12);
    this.phase = phase;
    this.panel = panel;
  }

  public void setFontSizes(int size) {
    regularFont = new Font("Courier", 0, size);
    boldFont = new Font("Dialog", Font.BOLD, size);
  }

  public Color getHatchColor() {
    return hatchColor;
  }

  public Component getBaseRendererComponent(char base,
      int pos,
      int tier,
      SequenceI seq) {

    if (panel instanceof BaseEditorPanel &&
        ((BaseEditorPanel) panel).getShowHitZones()) {
      Vector hitZones = ((BaseEditorPanel) panel).
                        getTranslatedHitZones(phase);

      hatchColor = null;
      for(int hitIndex = 0; hitZones != null &&
          hitIndex < hitZones.size(); hitIndex++) {
        int [] hitZone = (int []) hitZones.elementAt(hitIndex);

        if (pos >= hitZone[0]*3+phase&&
            pos < hitZone[1]*3+phase) {
          hatchColor = Color.yellow;
          break;
        }
      }
    }

    if (colors.get(base+"") == null)
      setFont(regularFont);
    else
      setFont(boldFont);
    return super.getBaseRendererComponent(base, pos, tier, seq);
  }

  public Color getTextColor() {
    Color stored = (Color) colors.get(c+"");
    if (stored == null)
      return disabled;
    else
      return stored;
  }
}
