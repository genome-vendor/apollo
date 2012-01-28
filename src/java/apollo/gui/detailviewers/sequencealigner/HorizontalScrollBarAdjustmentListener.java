package apollo.gui.detailviewers.sequencealigner;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class HorizontalScrollBarAdjustmentListener 
    implements AdjustmentListener {
  JViewport viewport;

  public HorizontalScrollBarAdjustmentListener(JViewport viewport) {
    this.viewport = viewport;
  }

  /** 
   * assumes that the scrollbar adjustment values have the same bounds
   * as the number of pixels in the component in the viewport
   */
  public void adjustmentValueChanged(AdjustmentEvent e) {

    int value = e.getValue();
    Component c = viewport.getView();
    Point oldPosition = viewport.getViewPosition();
    viewport.setViewPosition(new Point(value, oldPosition.y));
  }

}
