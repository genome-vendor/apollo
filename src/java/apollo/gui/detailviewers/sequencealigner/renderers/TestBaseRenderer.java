package apollo.gui.detailviewers.sequencealigner.renderers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.JComponent;

import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;
import apollo.gui.detailviewers.sequencealigner.Orientation;
import apollo.gui.detailviewers.sequencealigner.TierI;

public class TestBaseRenderer extends JComponent implements BaseRendererI {

  public static final int NO_OUTLINE = 0;
  public static final int LEFT_OUTLINE = 1;
  public static final int CENTER_OUTLINE = 2;
  public static final int RIGHT_OUTLINE = 3;

  private char character;

  public TestBaseRenderer() {
    setFont(apollo.config.Config.getExonDetailEditorSequenceFont());
    setBorder(null);
    setForeground(Color.white);
    setBackground(Color.black);
    setSize(getFont().getSize(), getFont().getSize());
    setPreferredSize(getSize());
    setOpaque(true);
  }

  public Color getBrokenEdgeColor() {
    return null;
  }

  public void paintNotify() {
  }

  public Component getBaseComponent(int pos, TierI tier, Orientation o) {
    init(tier.charAt(pos));
    return this;
  }

  protected void init(char base) {
    this.setCharacter(base);
  }

  public char getBase() {
    return this.getCharacter();
  }

  public Color getTextColor() {
    return getForeground();
  }

  public int getBaseHeight() {
    return getSize().height;
  }

  public int getBaseWidth() {
    return getSize().width;
  }

  public Color getBackgroundBoxColor() {
    return getBackground();
  }

  public Color getBackgroundLineColor() {
    return null;
  }

  public Color getHatchColor() {
    return null;
  }

  public int getOutlineType() {
    return NO_OUTLINE;
  }

  public Color getOutlineColor() {
    return null;
  }

  public void paint(Graphics g) {
    g.setFont(getFont());

    Color boxColor = getBackgroundBoxColor();
    Color hatchColor = getHatchColor();
    Color backgroundLineColor = getBackgroundLineColor();
    Color textColor = getTextColor();
 
    int outlineType = getOutlineType();
    Color outlineColor = getOutlineColor();
    Color brokenEdgeColor = getBrokenEdgeColor();

    if (boxColor != null) {
      g.setColor(boxColor);
      g.fillRect(0, 0, getSize().width, getSize().height);
    }

    if (backgroundLineColor != null) {
      g.setColor(backgroundLineColor);
      g.fillRect(0, getSize().height / 2 - 1, getSize().width, 2);
    }

    if (hatchColor != null) {
      g.setColor(hatchColor);

      // Draw box around residue (instead of crosshatches)
      g.drawLine(0, getSize().height, getSize().width, getSize().height); // bottom
      g.drawLine(0, 0, getSize().width, 0); // top
      g.drawLine(0, 0, 0, getSize().height); // left side
      // need to subtract one from width, else get drawn over by next base
      g.drawLine(getSize().width - 1, 0, getSize().width - 1, getSize().height); // right
                                                                                  // side
    }

    if (brokenEdgeColor != null) {
      final int squiggleCount = 4;
      int triangleHeight = 1 + (getSize().height / squiggleCount);
      int triangleWidth = 1 + (getSize().width / 5);
      for (int i = 0; i < squiggleCount - 1; i++) {
        g.setColor(brokenEdgeColor);

        g.drawLine(0, i * triangleHeight, triangleWidth, i * triangleHeight
            + triangleHeight / 2);
        g.drawLine(triangleWidth, i * triangleHeight + triangleHeight / 2, 0,
            (i + 1) * triangleHeight);
      }
    }

    // Draw a line on the right or left boarder
    if (outlineType != NO_OUTLINE && outlineColor != null) {
      g.setColor(outlineColor);
      if (outlineType == LEFT_OUTLINE) {
        g.drawLine(0, 0, 0, getSize().height);
      } else if (outlineType == RIGHT_OUTLINE) {
        g.drawLine(getSize().width - 1, 0, getSize().width - 1, getSize().height);
      }
    }

    // Print the base
    if (textColor != null) {
      FontMetrics metrics = this.getFontMetrics(this.getFont());
      int widthLead = (getSize().width - metrics.charWidth(getCharacter())) / 2;
      int heightLead = (getSize().height / 100) + 1; // XXX: set height lead correctly
      g.setColor(textColor);
      g.drawString(getCharacter() + "", widthLead, getSize().height - heightLead);
    }
  }

  public void setCharacter(char character) {
    this.character = character;
  }

  public char getCharacter() {
    return character;
  }
  
  public boolean isSelectable() {
    return false;
  }
  
  public boolean setTargetPosition(int pos) {
    return isSelectable();
  }
  
  
  public int pixelPositionToTierPosition(int p, TierI t, Orientation o) {
    if (o == Orientation.THREE_TO_FIVE) {
      p = t.getHigh() - p;
    }
    
    return p;
  }
  
  public int tierPositionToPixelPosition(int p, TierI t, Orientation o) {
    if (o == Orientation.THREE_TO_FIVE) {
      p = t.getHigh() - p;
    }

    return p;
  }
}
