package apollo.gui.detailviewers.sequencealigner;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JComponent;

/**
 * Draws the horizontal header at the top of the EDE which displays the base
 * number which is currently in view.
 * 
 */
public class BaseFineEditorHorizontalColHeader extends JComponent {
  BaseEditorPanel baseEditorPanel;
  int height;
  int width;
  int margin;

  public BaseFineEditorHorizontalColHeader(BaseEditorPanel baseEditorPanel) {
    this.baseEditorPanel = baseEditorPanel;

    // TODO: Read from a config file?
    this.height = 30;
    this.width = baseEditorPanel.getPreferredSize().width;
    this.margin = 5;

    setPreferredSize(new Dimension(this.width, this.height));
    setSize(getPreferredSize());
  }

  public void paint(Graphics g) {
    int panelWidth = baseEditorPanel.getRowWidth();
    int colWidth = baseEditorPanel.getCharWidth();
    int lineHeight = this.height - this.margin;

    Rectangle clipBounds = g.getClipBounds();
    Rectangle clipPos = (baseEditorPanel.getPosRectangleForPixels(clipBounds));

    // Draws the line at the bottom of the header display
    g.drawLine(clipBounds.x, lineHeight, clipBounds.x + clipBounds.width,
        lineHeight);

    // go through each position that is currently in view and
    // show a tick mark where the background stripe color changes
    for (int col = clipPos.x; col < clipPos.x + clipPos.width; col++) {
      int pos = baseEditorPanel.posToBasePair(col);

      if (pos % baseEditorPanel.getStripeWidth() == 0) {
        String position = String.valueOf(pos);
        int offset = g.getFontMetrics().stringWidth(position) / 2;

        g.drawLine(col * colWidth, lineHeight, col * colWidth, lineHeight
            - this.margin);
        g.drawString(position, col * colWidth - offset, g.getFontMetrics()
            .getHeight());
      }
    }
  }

}
