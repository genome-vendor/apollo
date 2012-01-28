package apollo.gui.genomemap;

import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.JScrollBar;

import apollo.gui.Transformer;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.FeatureSetI;

public abstract class ScrollAdjustedView extends LinearView {

  /* this is used to indicate whether space needs to be allocated
     for some other view's scroll bar */
  protected int   scroller     = ViewI.NONE;
  protected int   prefHeight   = 46;
  /** Whether whole view is being reverse complemented */
  protected boolean            reverseComplement   = false;

  public ScrollAdjustedView(JComponent ap,
			    String name, 
			    boolean visible,
			    int prefHeight) {
    super(ap, name, visible);
    this.prefHeight = prefHeight;
    Rectangle rect = new Rectangle(1,1,1,prefHeight);
    setBounds(rect);
  }

  public void setScrollFlag(int scroller) {
    this.scroller = scroller;
    setDrawBounds(getBounds());
  }

  public Rectangle setScrollSpace(int where) {
    JScrollBar jb          = new JScrollBar(JScrollBar.VERTICAL);
    int        scrollWidth = jb.getMinimumSize().width;

    Rectangle  rect = new Rectangle(getBounds());

    rect.width -= scrollWidth;
    if (scroller == ViewI.LEFTSIDE) {
      rect.x += scrollWidth;
    }
    return rect;
  }

  public void setDrawBounds(Rectangle rect) {
    Rectangle newBounds = new Rectangle(rect);

    if (scroller != ViewI.NONE) {
      newBounds = setScrollSpace(scroller);
    }
    transformer.setPixelBounds(newBounds);
  }

  public Rectangle getPreferredSize() {
    // Width is unimportant. Height is.
    return new Rectangle(0,0,0,prefHeight);//60,60);//30,30);
  }

  public void setXOrientation(int direction) {
    switch (direction) {
    case Transformer.LEFT:
      reverseComplement = false; // added from berkeley branch
      getTransform().setXOrientation(direction);
      break;
    case Transformer.RIGHT:
      getTransform().setXOrientation(direction);
      reverseComplement = true; // added from berkeley branch
      break;
    default:
      System.out.println("Unknown direction in setXOrientation");
    }
  }

  protected SeqFeatureI featureContains (SeqFeatureI sf, int base_position) {
    SeqFeatureI selected = null;

    if (sf.canHaveChildren()) {
      FeatureSetI fs = (FeatureSetI) sf;
      for (int i = 0; i < fs.size() && selected != null; i++) {
        selected = featureContains (fs.getFeatureAt (i), base_position);
      }
    } else {
      if (sf.contains (base_position)) {
        selected = sf;
      }
    }
    return selected;
  }

}
