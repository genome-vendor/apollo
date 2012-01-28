package apollo.gui.synteny;

import java.awt.Color;
//import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

//import apollo.datamodel.FeaturePairI;
import apollo.datamodel.Link;

/** Interface for the drawable that provides the visual for a link, used by SyntenyLinkPanel */

interface DrawableLink {
  /** setQueryPixels? is it bad to say query in a drawable? dont know how else to refer to
      it? setPixels1 and setPoints2? */
  public void setQueryPixels(int x1, int x2, int y);
  public void setHitPixels(int x1, int x2, int y);
  public Color getColour();
  public boolean isVisible();
//  public void draw(Graphics2D g);
  public void draw(Graphics g, boolean full);
  public boolean intersects(Rectangle r);
  /** Returns the mid point in user coords on the query end of link(top) */
  public Point getQueryMidUserPoint();
  /** Returns the mid point in user coords on the hit end of link(bottom) */
  public Point getHitMidUserPoint();
  /** Link is basically the model for the link */
  public Link getLink();
  //public FeaturePairI getFeaturePair();
  /** Get string used for status bar */
  public String getStatusBarString();
  public boolean contains(Point p);
  /** Returns true if query and hit have names - this is a precondition for firing 
      selection - not sure why */
  public boolean hasNames();
  public void setSelected(boolean selected);
}
