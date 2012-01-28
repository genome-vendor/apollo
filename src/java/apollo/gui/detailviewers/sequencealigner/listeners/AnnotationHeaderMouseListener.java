package apollo.gui.detailviewers.sequencealigner.listeners;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.SeqFeatureI;
import apollo.gui.detailviewers.sequencealigner.MultiSequenceAlignerPanel;
import apollo.gui.detailviewers.sequencealigner.MultiTierPanelHeaderTable;
import apollo.gui.detailviewers.sequencealigner.TierPanelI;
import apollo.gui.detailviewers.sequencealigner.TierI.Level;

public class AnnotationHeaderMouseListener implements MouseListener {

  private MultiSequenceAlignerPanel panel;
  
  public AnnotationHeaderMouseListener(MultiSequenceAlignerPanel panel) {
    this.panel = panel;
  }
  
  /**
   * Sets the annotation whos name is displayed in the box as the selected
   * annotation
   */
  public void mouseClicked(MouseEvent e) {
    Point point = e.getPoint();
    Object source = e.getSource();
    if (source instanceof MultiTierPanelHeaderTable) {
      MultiTierPanelHeaderTable table = (MultiTierPanelHeaderTable) source;
      int tier = table.calculateTierNumber(point);
      TierPanelI tierPanel = panel.getAnnotationPanel().getPanel(tier);
      int bp = panel.getVisibleBase();
      int position = tierPanel.tierPositionToPixelPosition(
        tierPanel.getTier().getPosition(bp));
      
      SeqFeatureI feature = tierPanel.featureAt(position, Level.TOP);
      if (feature == null) {
          feature = tierPanel.getNextFeature(position, Level.TOP);
      }

      if (feature != null && feature instanceof AnnotatedFeatureI
          && table.isVisible(feature)) {
        panel.setSelection((AnnotatedFeatureI)feature);
      }
    }
    
  }

  public void mouseEntered(MouseEvent e) {
    // TODO Auto-generated method stub
  }

  public void mouseExited(MouseEvent e) {
    // TODO Auto-generated method stub
  }

  public void mousePressed(MouseEvent e) {
    // TODO Auto-generated method stub
  }

  public void mouseReleased(MouseEvent e) {
    // TODO Auto-generated method stub
  }

}
