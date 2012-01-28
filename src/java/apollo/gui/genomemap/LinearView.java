package apollo.gui.genomemap;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import apollo.gui.Transformer;
import apollo.gui.event.*;
import apollo.gui.Controller;

import org.apache.log4j.*;

/**
 * A basic, non useful, implementation of the LinearViewI interface.
 */
public abstract class LinearView implements ViewI {
 
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(LinearView.class);
  
  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  //needed to get the Graphics to draw on
  protected JComponent         apollo_panel; 
  protected Transformer        transformer;
  protected Graphics           graphics;
  private   String             name;
  private   boolean            invalid;
  private   Vector             viewListeners    = new Vector();
  protected Color              backgroundColour = Color.white;
  protected Color              foregroundColour = Color.black;
  protected Rectangle          viewBounds;
  protected boolean            visible          = true;
  protected boolean            debug            = false;
  protected boolean            limitsSet        = false;
  protected boolean            transparent      = false;
  /** Not sure if LinearView should know strand. Defaults to forward */
  private int strand = 1;
  
  protected java.util.List<VisibilityListener> visibilityListeners =
    new LinkedList<VisibilityListener>();

  public LinearView(JComponent ap, String name, boolean visible) {
    init(ap, name, visible);
  }

  protected void init(JComponent ap, String name, boolean visible) {
    setVisible(visible);
    setComponent(ap);
    viewBounds = new Rectangle(1,1,1,1);
    
    transformer = new Transformer(viewBounds);
    transformer.setYRange(new int [] {-10000,10000});
    transformer.setXCentre(0);
    transformer.setYCentre(0);

    setName(name);
  }
  
  // LinearViewI methods
  //  1. ViewI
  public void setComponent(JComponent ap) {
    this.apollo_panel = ap;
  }

  /** LinearViews components that are part of an ApolloPanel
      which is a JComponent */
  public JComponent getComponent() {
    return this.apollo_panel;
  }

  public void setInvalidity(boolean state) {
    invalid = state;
  }

  public boolean isInvalid() {
    return this.invalid;
  }

  public void setBounds(Rectangle rect) {
    viewBounds = new Rectangle(rect);
    setDrawBounds(viewBounds);
  } 

  public Rectangle getBounds() {
    return viewBounds;
  }

  public Rectangle getDrawBounds() {
    return transformer.getPixelBounds();
  }

  public void setDrawBounds(Rectangle rect) {
    transformer.setPixelBounds(rect);
  }

  public void setName(String name) {
    this.name = new String(name);
  }

  public String getName() {
    return name;
  }

  public void setGraphics(Graphics graphics) {
    this.graphics = graphics;
  }

  public Graphics getGraphics() {
    /* What happens if we don't use all of this rigamarole
       and simply use the graphics we have in the view 
    */
    if (graphics == null) {
      Image image_buffer = ((ApolloPanelI) getComponent()).getBackBuffer();
      if (image_buffer != null) {
	graphics = image_buffer.getGraphics();
      } else {
	logger.error ("Oh oh, now what??");
      }
    }
    return graphics;
  }

  /** paintView draws a cross in the centre of the View 
      and a small cross in the upper left quadrant */
  public void paintView() {
    logger.error (this.getClass().getName() + 
                  " needs to implement method for paintView()");
    graphics.setColor(Color.white);
    if (!transparent) {
      graphics.fillRect(transformer.getPixelBounds().x,
			transformer.getPixelBounds().y,
			transformer.getPixelBounds().width,
			transformer.getPixelBounds().height);
    } else {
      graphics.drawRect(transformer.getPixelBounds().x,
			transformer.getPixelBounds().y,
			transformer.getPixelBounds().width,
			transformer.getPixelBounds().height);
    }

    graphics.setColor(Color.red);

    Point start    = new Point((int)transformer.getXMinimum()+11000,0);
    Point end      = new Point((int)transformer.getXMaximum()-11000,0);
    Point pixstart = transformer.toPixel(start);
    Point pixend   = transformer.toPixel(end);
    graphics.drawLine(pixstart.x,pixstart.y,pixend.x,pixend.y);

    Point centre    = new Point((int)(transformer.getXVisibleRange()[0]+
                                      (transformer.getXVisibleRange()[1]-
				       transformer.getXVisibleRange()[0])/2),
                                transformer.getYVisibleRange()[0]+
                                (transformer.getYVisibleRange()[1]-
                                 transformer.getYVisibleRange()[0])/2);
    Point pixcentre = transformer.toPixel(centre);
    logger.debug("Centre = " + centre);
    logger.debug("PixCentre = " + pixcentre);
    logger.debug("name = " + name);
    graphics.drawString(name,pixcentre.x,pixcentre.y);

    start    = new Point(0,(int)transformer.getYMinimum()+1000);
    end      = new Point(0,(int)transformer.getYMaximum()-1000);
    pixstart = transformer.toPixel(start);
    pixend   = transformer.toPixel(end);
    graphics.drawLine(pixstart.x,pixstart.y,pixend.x,pixend.y);

    start    = new Point(-1000,1250);
    end      = new Point(-1500,1250);
    pixstart = transformer.toPixel(start);
    pixend   = transformer.toPixel(end);
    graphics.drawLine(pixstart.x,pixstart.y,pixend.x,pixend.y);

    start    = new Point(-1250,1000);
    end      = new Point(-1250,1500);
    pixstart = transformer.toPixel(start);
    pixend   = transformer.toPixel(end);
    graphics.drawLine(pixstart.x,pixstart.y,pixend.x,pixend.y);
  }

  public Transformer getTransform() {
    return this.transformer;
  }

  public void setTransform(Transformer transformer) {
    this.transformer = transformer;
  }

  //  2. LinearViewI
  public void setLimits(int [] limits) {
    transformer.setXRange(limits);
    limitsSet = true;
  }

  public boolean areLimitsSet() {
    return limitsSet;
  }

  public void setLimitsSet(boolean state) {
    limitsSet = state;
  }

  public void setMinimum(int min) {
    transformer.setXMinimum(min);
  }

  public void setMaximum(int max) {
    transformer.setXMaximum(max);
  }

  /** Limits not necasarily equal to seq start and end,
   * Component.syncViewLimits pads out the limits beyond sequence
   */
  public int [] getLimits() {
    return transformer.getXRange();
  }

  public int getMaximum() {
    return transformer.getXMaximum();
  }

  public int getMinimum() {
    return transformer.getXMinimum();
  }

  public void setCentre(int centre) {
    transformer.setXCentre(centre);
  }

  public int getCentre() {
    return transformer.getXCentre();
  }

  public Rectangle getPreferredSize() {
    return getBounds();
  }

  public void setZoomFactor(double factor) {
    transformer.setXZoomFactor(factor);
  }

  /** visible range in base pairs */
  public int [] getVisibleRange() {
    return transformer.getXVisibleRange();
  }

  public void setVisible(boolean state, boolean remove)
  {
    visible = state;
    if (isVisible())
      setInvalidity(true);
    if (getComponent() != null) {
      ((ApolloPanel)getComponent()).setInvalidity(true);
      getComponent().doLayout();
      ((ApolloPanel)getComponent()).setInvalidity(false);
    }
    for (VisibilityListener l : visibilityListeners) {
      l.visibilityChanged(new VisibilityEvent(this, remove));
    }
  }
  
  public void setVisible(boolean state) {
    setVisible(state, false);
  }

  public boolean isVisible() {
    return visible;
  }

  public void setDebug(boolean state) {
    debug = state;
  }

  // Event routines
  public void addViewListener(ViewListener l) {
    viewListeners.addElement(l);
  }

  public void fireViewEvent(ViewEvent evt) {
    for (int i=0;i<viewListeners.size();i++) {
      ViewListener l = (ViewListener)viewListeners.elementAt(i);
      l.handleViewEvent(evt);
    }
  }

  public void setBackgroundColour(Color colour) {
    this.backgroundColour = colour;
  }

  public Color getBackgroundColour() {
    return backgroundColour;
  }

  public void setForegroundColour(Color colour) {
    this.foregroundColour = colour;
  }

  public Color getForegroundColour() {
    return foregroundColour;
  }

  public void setTransparent(boolean state) {
    transparent = state;
  }

  public boolean isTransparent() {
    return transparent;
  }

  public void clear() {
    logger.error (this.getClass().getName() + 
                  " needs to implement clear method");
  }

  /**
   * I moved strand from FeatureView to here, because scrolling is different on
   * reverse strand than forward strand. But this is funny for subclasses like
   * SequenceView that are not stranded.
   */
  public void setStrand(int strand) {
    this.strand = strand;
  }
  public int getStrand() {
    return strand;
  }
  boolean isReverseStrand() {
    return getStrand() == -1;
  }
  boolean isForwardStrand() {
    return getStrand() == 1;
  }

  protected Rectangle getSelectionRectangle(Point pnt) {
    if ((getTransform().getXOrientation() == Transformer.LEFT &&
	 getStrand() == 1) ||
	(getTransform().getXOrientation() == Transformer.RIGHT &&
	 getStrand() == -1)) {
      return new Rectangle(pnt.x-3,pnt.y-1,4,1);
    } else {
      return new Rectangle(pnt.x,pnt.y,4,1);
    }
  }
  
  public void addVisibilityListener(VisibilityListener l)
  {
    visibilityListeners.add(l);
  }
}
