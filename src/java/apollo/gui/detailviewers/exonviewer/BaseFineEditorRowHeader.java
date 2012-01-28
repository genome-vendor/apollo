package apollo.gui.detailviewers.exonviewer;

import javax.swing.*;
import java.awt.*;
public class BaseFineEditorRowHeader extends JComponent {

  BaseEditorPanel baseEditorPanel;
  int textMargin = 10;

  public BaseFineEditorRowHeader(BaseEditorPanel baseEditorPanel) {
    this.baseEditorPanel = baseEditorPanel;
    int panelHeight = (baseEditorPanel.getRowHeight() +
                       baseEditorPanel.getRowMargin());
    setPreferredSize(new Dimension(100,
                                   baseEditorPanel.getRowCount()*panelHeight));
    setSize(getPreferredSize());
  }

  public void paint(Graphics g) {
    int panelHeight = (baseEditorPanel.getRowHeight() +
                       baseEditorPanel.getRowMargin());
    Rectangle clipBounds = g.getClipBounds();

    Rectangle clipPos = (baseEditorPanel.
                         getPosRectangleForPixels(clipBounds));

    for (int row=clipPos.y; row < clipPos.y+clipPos.height; row++) {
      g.drawLine(0,row*panelHeight,getSize().width,row*panelHeight);
      for(int tier=0; tier < baseEditorPanel.getTierCount(); tier++) {
        int y = row*panelHeight+
                (baseEditorPanel.getCharHeight()*(tier+1));
        g.drawString(getString(row,tier), textMargin, y);
      }
    }
  }

  public String getString(int row, int tier) {
    int pos = row*baseEditorPanel.getColumnCount();
    int basepair = baseEditorPanel.posToBasePair(pos);
    return (tier == 0 ? "" + basepair : "");
  }

}
