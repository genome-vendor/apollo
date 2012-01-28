package apollo.gui.detailviewers.sequencealigner.renderers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import apollo.gui.detailviewers.sequencealigner.Orientation;
import apollo.gui.detailviewers.sequencealigner.ReadingFrame;
import apollo.gui.detailviewers.sequencealigner.TierI;

public class AminoAcidBaseRenderer extends TestBaseRenderer {

  private static final Map<String, Color> colors = new HashMap<String, Color>();
  private static final Color disabled = new Color(200, 200, 200);

  private static final Font regularFont = apollo.config.Config
      .getExonDetailEditorSequenceFont();
  private static final Font boldFont = new Font("Dialog", Font.BOLD,
      regularFont.getSize());

  static {
    colors.put("M", Color.green);
    colors.put("*", Color.red);
  }

  private int offset;
  private Orientation orientation;

  public AminoAcidBaseRenderer(int offset, Orientation orientation) {
    super();
    this.offset = offset;
    this.orientation = orientation;
  }

  // this is a base pair position even though we mean to 
  // use this to render only 1/3 of the bases
  public Component getBaseComponent(int position, TierI tier, Orientation o) {
    char base = '\0';
    
    if (orientation == Orientation.FIVE_TO_THREE) {
      position = position*3 + offset;
    } else {
      int min = tier.getHigh() - (tier.getHigh()/3);
      int index = position - min;
      position = index*3 + offset;
    }
    
    int bp = tier.getBasePair(position);
    
    String r = tier.getReference().getResidues();
    if (position < tier.getReference().getLength()) {
      base = tier.charAt(position);
    }

    if (colors.get(base + "") == null)
      setFont(regularFont);
    else
      setFont(boldFont);

    super.init(base);
    return this;
  }

  public Color getTextColor() {
    Color stored = colors.get(getBase() + "");
    if (stored == null)
      return disabled;
    else
      return stored;
  }
  
  
  public int pixelPositionToTierPosition(int p, TierI t, Orientation o) {
    if (o == Orientation.THREE_TO_FIVE) {
      p = t.getHigh()/3 - p;
    }
    
    return p;
  }
  
  public int tierPositionToPixelPosition(int p, TierI t, Orientation o) {
    if (o == Orientation.THREE_TO_FIVE) {
      p = t.getHigh()/3 - p;
    }

    return p;
  }

}
