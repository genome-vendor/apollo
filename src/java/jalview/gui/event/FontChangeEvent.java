package jalview.gui.event;

import java.util.EventObject;
import java.awt.*;

public class FontChangeEvent extends EventObject {
  Font font;

  public FontChangeEvent(Object source, Font font) {
    super(source);
    this.font = font;
  }

  public Font getFont() {
    return font;
  }

  public Object getSource() {
    return source;
  }
}
