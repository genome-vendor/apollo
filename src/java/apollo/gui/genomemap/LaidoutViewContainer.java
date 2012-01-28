package apollo.gui.genomemap;

import java.util.*;
import apollo.gui.*;
import apollo.datamodel.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import apollo.util.*;

public class LaidoutViewContainer extends ContainerView 
  implements PickViewI {

  protected ColumnApolloLayout layout;

  public LaidoutViewContainer(JComponent ap, 
			      ColumnApolloLayout layout,
			      String name) { 
    super(ap,name,true);
    this.layout = layout;
  }

  public Rectangle setScrollSpace(int where) {
    JScrollBar jb          = new JScrollBar(JScrollBar.VERTICAL);
    int        scrollWidth = jb.getMinimumSize().width;

    Rectangle  rect = new Rectangle(getBounds());

    rect.width -= scrollWidth;
    if (where == ViewI.LEFTSIDE) {
      rect.x += scrollWidth;
    }
    return rect;
  }

  public void setBounds(Rectangle rect) {
    super.setBounds(rect);
    layout.layoutViews(views,rect,getBounds().y);
  }

  public void add(ViewI v, Object constraints ) {
    views.addElement(v);
    layout.addLayoutView(v, constraints);
  }

  public void addFirst(ViewI v, Object constraints ) {
    views.insertElementAt(v,0);
    layout.addLayoutView(v, constraints);
  }

  public void remove(ViewI v) {
    views.removeElement(v);
    layout.removeLayoutView(v);
  }

  public void invertViews() {
    Vector reverseViews = new Vector();
    for (int i=views.size()-1; i>=0; i--) {
      ViewI v = (ViewI)views.elementAt(i);
      if (v instanceof SplitterView) {
        ((SplitterView)v).invertViews();
      }
      reverseViews.addElement(v);
    }
    views = reverseViews;
  }
}
