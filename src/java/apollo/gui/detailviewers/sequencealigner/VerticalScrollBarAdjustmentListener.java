package apollo.gui.detailviewers.sequencealigner;

import java.awt.Point;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class VerticalScrollBarAdjustmentListener 
    implements AdjustmentListener {
  JViewport viewport;

  public VerticalScrollBarAdjustmentListener(JViewport viewport) {
    this.viewport = viewport;
  }

  /** 
   * assumes that the scrollbar adjustment values have the same bounds
   * as the number of pixels in the component in the viewport
   */
  public void adjustmentValueChanged(AdjustmentEvent e) {

    int value = e.getValue();
    Point oldPosition = viewport.getViewPosition();
    viewport.setViewPosition(new Point(oldPosition.x, value));
    //for (ChangeListener listener : viewport.getChangeListeners()) {
    //  listener.stateChanged(new ChangeEvent(viewport));
    //}
  }

}
