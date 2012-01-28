package jalview.gui.event;

import java.util.EventObject;
import java.awt.*;

public class RubberbandEvent extends EventObject {
    Rectangle bandBounds;
    
    public RubberbandEvent(Object source,Rectangle Bounds) {
	super(source);
	this.bandBounds = Bounds;
    }

    public Rectangle getBounds() {
	return bandBounds;
    }

    public Object getSource() {
	return source;
    }
}

	
