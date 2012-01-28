package apollo.gui.detailviewers.sequencealigner.renderers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Map;
import java.util.HashMap;

import apollo.datamodel.RangeI;
import apollo.datamodel.SequenceI;
import apollo.gui.detailviewers.sequencealigner.Orientation;
import apollo.gui.detailviewers.sequencealigner.TierI;

public class AminoAcidRendererSpaced extends TestBaseRenderer {

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

  private int phase;

  public AminoAcidRendererSpaced(int phase) {
    super();
    this.phase = phase;
  }

  public Component getBaseComponent(int position, TierI tier, Orientation o) {
    char base = '\0';

    if (position >= 0 && position < tier.getReference().getLength()) {
      base = tier.charAt(position);
    }
     
    // TODO: hit zones....

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
    int position = p/3;
    
    if (o == Orientation.THREE_TO_FIVE) {
      
      // the reference length is different depending on what strand the
      SequenceI s = t.getReference();
      int l = t.getReference().getLength(); 
      
      // Given a set genomic region, the length of the generated amino acid 
      // sequence may not be the same for every phase
      RangeI r = t.getReference().getRange();
      int rle = r.getHigh() - r.getLow() + 1;
      int extra_bases = (rle-(int)Math.abs(phase)) % 3;
      position = t.getHigh() - position;
      // will this work in every case? See translate()
      if (extra_bases == 0) {
        position -= 1;
      }
      if (extra_bases == 2) {
        position -= 1;
      }
      //position -= extra_bases;
      //position -= phase;
      p -= phase;
    }
    
    
    if (p % 3 != phase) {
      position = -1;
    }
    
    return position;
  }
  
  public int tierPositionToPixelPosition(int p, TierI t, Orientation o) {
    // is this right? does it matter?
    int position = p;
    if (o == Orientation.THREE_TO_FIVE) {
      position = t.getHigh()*3 - position;
    }
    
    return position;
  }

}
