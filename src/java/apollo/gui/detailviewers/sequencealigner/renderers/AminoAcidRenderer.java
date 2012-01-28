package apollo.gui.detailviewers.sequencealigner.renderers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import apollo.gui.detailviewers.sequencealigner.Orientation;
import apollo.gui.detailviewers.sequencealigner.ReadingFrame;
import apollo.gui.detailviewers.sequencealigner.TierI;

public class AminoAcidRenderer extends TestBaseRenderer {
  
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

  private ReadingFrame frame;

  public AminoAcidRenderer(ReadingFrame frame) {
    super();
    this.frame = frame;
  }

  public Component getBaseComponent(int position, TierI tier, Orientation o) {
    char base = '\0';
    int bp = tier.getBasePair(position);
    
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

}
