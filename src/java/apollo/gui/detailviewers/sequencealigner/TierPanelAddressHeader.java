package apollo.gui.detailviewers.sequencealigner;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.Scrollable;

import apollo.gui.BaseScrollable;

/**
 * Draws the horizontal header which displays the base number that is currently
 * in view.
 * 
 */
public class TierPanelAddressHeader extends AbstractScrollablePanel {

  MultiTierPanel tierPanel;
  int height;
  int width;
  int margin;
  int tickInterval;

  public TierPanelAddressHeader(MultiTierPanel tierPanel) {
    this.tierPanel = tierPanel;
  }
  
  public void reformat() {
    // TODO: Read from a config file?
    this.tickInterval = 10;
    this.margin = 5;
    this.height = 30;
    this.width = 
      tierPanel.getPanel(0).getTier().getReference().getLength()
        * tierPanel.getBaseWidth();
    setFont(apollo.config.Config.getExonDetailEditorSequenceFont());
    setForeground(Color.black);
    setBackground(Color.gray);
    setOpaque(true);
    setBorder(null);

    setPreferredSize(new Dimension(this.width, this.height));
  }

  public void paint(Graphics g) {
    int colWidth = tierPanel.getBaseWidth();
    int lineHeight = this.height - this.margin;

    Rectangle clipBounds = g.getClipBounds();
    int lowerBound = Math.max(0, clipBounds.x - 100);
    int upperBound = Math.min(this.getPreferredSize().width,
                              clipBounds.x + clipBounds.width + 100);
    
    Point lowPixel = new Point(lowerBound, 0);
    Point highPixel = new Point(upperBound, 0);

    int low = tierPanel.getPositionForPixel(lowPixel);
    int high = tierPanel.getPositionForPixel(highPixel);

    // clear what has previously been drawn
    g.setColor(this.getBackground());
    g.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);

    // Draws the line at the bottom of the header display
    g.setColor(this.getForeground());
    g.drawLine(clipBounds.x, lineHeight, clipBounds.x + clipBounds.width,
        lineHeight);

    // go through each base that is currently in view and
    // display the base location at a set interval
    for (int pos = low; pos < high; pos++) {
      int tierPosition = 
        tierPanel.pixelPositionToTierPosition(pos);
      int basePair = 
        tierPanel.tierPositionToBasePair(tierPosition);
      
      if (basePair % tickInterval == 0) {
        String position = String.valueOf(basePair);
        int addrOffset = g.getFontMetrics().stringWidth(position) / 2;
        int charOffset = tierPanel.getBaseWidth() / 2;
        

        g.drawLine(pos * colWidth + charOffset, lineHeight, pos * colWidth
            + charOffset, lineHeight - this.margin);
        g.drawString(position, pos * colWidth - addrOffset + charOffset, g
            .getFontMetrics().getHeight());
      }
    }
  }

  /*****************************************************************************
   * These Are the methods needed to implement the Scrollable interface.
   * 
   * @see javax.swing.Scrollable 
   * ***********************************************
   */

  public Dimension getPreferredScrollableViewportSize() {
    return this.getPreferredSize();
  }

  public int getScrollableBlockIncrement(Rectangle visibleRect,
      int orientation, int direction) {
    return this.tierPanel.getScrollableBlockIncrement(visibleRect, orientation,
        direction);
  }

  public boolean getScrollableTracksViewportHeight() {
    return this.tierPanel.getScrollableTracksViewportHeight();
  }

  public boolean getScrollableTracksViewportWidth() {
    return this.tierPanel.getScrollableTracksViewportWidth();
  }

  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation,
      int direction) {
    return this.tierPanel.getScrollableUnitIncrement(visibleRect, orientation,
        direction);
  }

  public int getVisibleBase() {
    return this.tierPanel.getVisibleBase();
  }

  public int getVisibleBaseCount() {
    return this.tierPanel.getVisibleBaseCount();
  }

  public void scrollToBase(int pos) {
    this.tierPanel.scrollToBase(pos);
  }

}
