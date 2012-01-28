package apollo.gui.detailviewers.sequencealigner;

import javax.swing.JComponent;
import java.awt.Graphics;

public class ReferenceTierPanelHeader extends JComponent {

  public static ReferenceTierPanelHeader makeHeader(Strand strand) {
    return new ReferenceTierPanelHeader(strand);
  }

  private Strand strand;

  private ReferenceTierPanelHeader() {
    super();
  }

  private ReferenceTierPanelHeader(Strand strand) {
    super();
    this.strand = strand;
  }

  public void paint(Graphics g) {
    int height = this.getPreferredSize().height;
    int width = this.getPreferredSize().width;

    g.setColor(this.getForeground());
    g.drawOval(0, 0, height, height);
  }
}
