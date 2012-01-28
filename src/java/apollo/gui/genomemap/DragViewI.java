package apollo.gui.genomemap;

import java.awt.*;

/**
 * Defines the methods required for dragging.
 */
public interface DragViewI extends ViewI {

  public void setOrigin(TierViewI view, Point position);

  public TierViewI getOriginView();

  public Point getOriginPosition();

  public void setLocation(Point position);

  public void setRelativePosition(Point position);
}
