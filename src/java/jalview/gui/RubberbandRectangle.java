// From Graphic JAVA volume 1
package jalview.gui;

import java.awt.*;

/**
 * A rectangular rubberband.
 */
public class RubberbandRectangle extends Rubberband {
   public RubberbandRectangle() {
   }
   public RubberbandRectangle(Component component) {
     super(component);
   }
   public void drawLast(Graphics graphics) {
      Rectangle rect = lastBounds();
      graphics.drawRect(rect.x, rect.y, 
                        rect.width, rect.height);
   }
   public void drawNext(Graphics graphics) {
      Rectangle rect = getBounds();
      graphics.drawRect(rect.x, rect.y, 
                        rect.width, rect.height);
   }
}
