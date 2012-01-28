package apollo.gui.detailviewers.sequencealigner;

import javax.swing.*;
import java.awt.*;

import apollo.datamodel.*;
import apollo.gui.detailviewers.sequencealigner.renderers.BaseRenderer;

public class DefaultBaseRenderer extends JComponent implements BaseRenderer {
  protected char c;
  protected int pos;
  protected int tier;
  protected SequenceI seq;

  protected FontMetrics metrics;

  public static final int NO_OUTLINE = 0;
  public static final int LEFT_OUTLINE = 1;
  public static final int CENTER_OUTLINE = 2;
  public static final int RIGHT_OUTLINE = 3;

  public DefaultBaseRenderer(int width, int height) {
    setFont(apollo.config.Config.getExonDetailEditorSequenceFont());
    setBorder(null);
    setForeground(Color.white);
    setBackground(Color.blue);
    setSize(width,height);
    setOpaque(false);
  }
  
  public boolean canRender(SeqFeatureI feature){
	  return true;
  }

  public void setFont(Font in) {
    metrics = getFontMetrics(in);
    super.setFont(in);
  }

  public Color getBrokenEdgeColor() {
    return null;
  }

  public void paintNotify() {}

  public Component getBaseRendererComponent(char base,
      int pos,
      int tier,
      SequenceI seq) {
    init (base, pos, tier, seq);
    return this;
  }

  protected void init (char base, int pos, int tier, SequenceI seq) {
    c = base;
    this.pos = pos;
    this.tier = tier;
    this.seq = seq;
  }

  public Color getTextColor() {
    return getForeground();
  }

  public Color getBackgroundBoxColor() {
    return null;
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
    //    Color textColor = getForeground();
    int outlineType = getOutlineType();
    Color outlineColor = getOutlineColor();
    Color brokenEdgeColor = getBrokenEdgeColor();

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

      // Draw three diagonal hatches
      //      g.drawLine(0,0,getSize().width,getSize().height);
      //      g.drawLine(getSize().width/2, 0,
      //                 getSize().width, getSize().height/2);
      //      g.drawLine(0, getSize().height/2,
      //                 getSize().width/2, getSize().height);

      // Draw box around residue (instead of crosshatches)
      g.drawLine(0,getSize().height, getSize().width,getSize().height);  // bottom
      g.drawLine(0,0, getSize().width,0); // top
      g.drawLine(0,0, 0,getSize().height);  // left side
      //g.drawLine(getSize().width,0, getSize().width,getSize().height);  // right side
      // need to subtract one from width, else get drawn over by next base
      g.drawLine(getSize().width-1,0, getSize().width-1,getSize().height);  // right side

    }

    if (brokenEdgeColor != null) {
      final int squiggleCount = 4;
      int triangleHeight = 1 + (getSize().height / squiggleCount);
      int triangleWidth = 1 + (getSize().width / 5);
      for(int i=0; i < squiggleCount - 1; i++) {
        g.setColor(brokenEdgeColor);

        g.drawLine(0,i*triangleHeight,triangleWidth,i*triangleHeight + triangleHeight/2);
        g.drawLine(triangleWidth,i*triangleHeight + triangleHeight/2,
                   0,(i+1)*triangleHeight);
      }
    }

    if (outlineType != NO_OUTLINE && outlineColor != null) {
      g.setColor(outlineColor);
      g.drawLine(0,0,getSize().width-1,0);
      g.drawLine(0,getSize().height-1,getSize().width-1,getSize().height-1);
      if (outlineType == LEFT_OUTLINE)
        g.drawLine(0,0,0,getSize().height);
      else if (outlineType == RIGHT_OUTLINE) {
        g.drawLine(getSize().width-1,0,
                   getSize().width-1,getSize().height);
      }
    }
    if (getTextColor() != null) {
    	int s = getSize().height;
      int leadDistance = (getSize().width - metrics.charWidth(c))/2;
      g.setColor(getTextColor());
      g.drawString(c+"",leadDistance,getSize().height); // XXX: was height-2
    }
  }
}
