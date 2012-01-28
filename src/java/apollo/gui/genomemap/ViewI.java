package apollo.gui.genomemap;

import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JComponent;

import apollo.gui.Transformer;
import apollo.gui.event.ViewListener;

/**
 * An interface representing a View object which has defined
 * extents eg a start and end base.
 */

public interface ViewI {

  public final static int RIGHTSIDE =  1;
  public final static int LEFTSIDE  = -1;
  public final static int NONE      = 0;

  /**
   * Set the component the view belongs to.
   * 
   * @param component The component containing the View. 
   */
  public void         setComponent(JComponent ap);

  /**
   * Get the component the view belongs to.
   * 
   */
  public JComponent getComponent();

  /**
   * Paint the Drawable objects in the View.
   * The graphics are clipped appropriately in ApolloPanel
   * prior to calling paintView.
   *
   */
  public void         paintView();


  /**
   * Set a flag indicating whether the view is currently invalid
   */
  public void         setInvalidity(boolean state);

  /**
   * Get a flag indicating whether the view is invalid
   */
  public boolean      isInvalid();

  /**
   * Get the coordinates of the rectangle containing the View.
   *
   */
  public Rectangle    getBounds();

  /**
   * Get the tranform object to convert between View and component 
   * coordinates.
   *
   */
  public Transformer   getTransform();

  /**
   * Set the coordinates of the rectangle containing the View.
   *
   * @param rect The rectangle describing the new bounds of the View in
   *             parent component coordinates.
   */
  public void         setBounds(Rectangle rect);

  /**
   * Get the preferred size for the view
   */
  public Rectangle    getPreferredSize();

  /**
   * Set the Graphics to draw to.
   *
   * @param g The new graphics to draw to. This will usually be the Graphics for the
   *          containing component (or for its offscreen buffer).
   */
  public void         setGraphics(Graphics g);

  /**
   * Returns the name of the view.
   */
  public String      getName();


  /**
   * Sets the name of the view
   * 
   * @param name The name of the view.
   */
  public void        setName(String name);


  /**
   * Add a View event listener
   */
  public void addViewListener(ViewListener vl);

  /**
   * Set whether or not a view is visible
   */
  public void setVisible(boolean state);

  /**
   * return whether or not a view is visible
   */
  public boolean isVisible();

  /**
   * Sets the minimum and maximum limits for the extent
   */
  public void setLimits(int [] limits);

  /**
   * Sets the minimum limit for the extent
   */
  public void setMinimum(int min);

  /**
   * Sets the maximum limit for the extent
   */
  public void setMaximum(int max);

  /**
   * Gets the minimum and maximum limits
   */
  public int [] getLimits();

  /**
   * Get the minimum limit.
   */
  public int getMinimum();

  /**
   * Get the maximum limit.
   */
  public int getMaximum();

  /**
   * Set the centre position.
   */
  public void setCentre(int centre);

  /**
   * Get the maximum limit.
   */
  public int getCentre();

  /**
   * Set the ZoomFactor along the linear axis
   */
  public void setZoomFactor(double factor);

  /**
   * Get the range on the linear axis that is visible
   */
  public int [] getVisibleRange();

  public boolean areLimitsSet();
  public void setLimitsSet(boolean state);

  public void setTransparent (boolean state);
  public boolean isTransparent();

  public void clear();

}

