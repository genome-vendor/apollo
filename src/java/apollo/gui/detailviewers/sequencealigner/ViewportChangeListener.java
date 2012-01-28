package apollo.gui.detailviewers.sequencealigner;

import javax.swing.JFrame;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ViewportChangeListener implements ChangeListener {
  
  private MultiTierPanelHeaderTable header;
  
  public ViewportChangeListener(MultiTierPanelHeaderTable h) {
    this.header = h;
  }

  public void stateChanged(ChangeEvent e) {
    JViewport viewport = (JViewport) e.getSource();
    MultiTierPanel panel = (MultiTierPanel) viewport.getView();
    if (panel != null) {
      int position = panel.getPositionForPixel(viewport.getViewPosition());
      int i = 1;
      header.update(position);
    }
    
  }

}
