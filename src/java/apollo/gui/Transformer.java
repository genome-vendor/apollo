package apollo.gui;

import java.awt.*;

/**
 * A class to perform coordinate transformations between pixel and user coordinates.
 */
public class Transformer {
  /**
   * Directions for Y axis (setYOrientation)
   */
  public final static int UP   = -1;
  public final static int DOWN =  1;

  public final static int LEFT  =  1;
  public final static int RIGHT = -1;

  private Rectangle pixelBounds; // Pixel bounds

  private int []   xRange = new int [2];
  private int []   yRange = new int [2];

  private int      xCentre;
  private int      yCentre;

  private double    xCoordsPerPixel;
  private double    yCoordsPerPixel;


  private double    xPixelsPerCoord;
  private double    yPixelsPerCoord;

  private double    xZoomFactor;
  private double    yZoomFactor;

  private int      xOrientation; // int?
  private int      yOrientation; // whether origin is at bottom left or top left

  // SMJS xOffset MUST be a long - it can become very large, and overflows an int, messing up the transformations
  private long     xOffset; // int - NO 
  private int      yOffset;

  // These don't seem to be used anywhere
  //
  //private int      toPixFactorX;
  //private int      toPixFactorY;

  private int []   xVisibleRange = new int [2];
  private int []   yVisibleRange = new int [2];

  public Transformer(Rectangle bounds) {
    this(bounds,new Rectangle(-10,-10,10,10));
  }

  public Transformer(Rectangle pixelBounds, Rectangle userBounds) {

    this.pixelBounds = new Rectangle(pixelBounds);

    xOrientation = LEFT;
    yOrientation = DOWN;

    xRange[0] = userBounds.x;
    yRange[0] = userBounds.y;
    xRange[1] = userBounds.x + userBounds.width;
    yRange[1] = userBounds.y + userBounds.height;

    reset();
    //    System.out.println("pixelBounds " + pixelBounds);
  }

  public void reset() {
    xZoomFactor = 1.0;
    yZoomFactor = 1.0;

    xCentre   = (xRange[1]+xRange[0])/2;
    yCentre   = (yRange[1]+yRange[0])/2;

    calcConversions();
  }

  public void setPixelBounds(Rectangle rect) {
    this.pixelBounds = new Rectangle(rect);
    //      System.out.println("Setting pixelBounds " + this.pixelBounds);
    calcConversions();
    //      System.out.println("Now pixelBounds are " + this.pixelBounds);
  }

  public Rectangle getPixelBounds() {
    return this.pixelBounds;
  }

  public void setYOrientation(int orientation) {
    yOrientation = orientation;
    calcConversions();
  }
  public void setXOrientation(int orientation) {
    xOrientation = orientation;
    calcConversions();
  }

  public void setXZoomFactor(double factor) {
    //      System.out.println("Set X Zoom Factor to " + factor);
    xZoomFactor = factor;
    calcConversions();
  }

  public void setYZoomFactor(double factor) {
    //      System.out.println("Set Y Zoom Factor to " + factor);
    yZoomFactor = factor;
    calcConversions();
  }

  public void setXRange(int[] limits) {
    xRange[0] = limits[0];
    xRange[1] = limits[1];
    calcConversions();
  }

  public void setYRange(int[] limits) {
    // System.out.println("Set Y Range to (" + limits[0] + "," + limits[1] + ")");
    yRange[0] = limits[0];
    yRange[1] = limits[1];
    yCentre   = (yRange[1]+yRange[0])/2;
    calcConversions();
  }

  public void setXMinimum(int min) {
    //      System.out.println("Set X Minimum to " + min);
    xRange[0] = min;
    calcConversions();
  }

  public void setYMinimum(int min) {
    //      System.out.println("Set Y Minimum to " + min);
    yRange[0] = min;
    calcConversions();
  }

  public void setXMaximum(int max) {
    //      System.out.println("Set X Maximum to " + max);
    xRange[1] = max;
    calcConversions();
  }

  public void setYMaximum(int max) {
    //      System.out.println("Set Y Maximum to " + max);
    yRange[1] = max;
    calcConversions();
  }

  public void setXCentre(int centre) {
    //      System.out.println("Set X Centre to " + centre);
    xCentre = centre;
    calcConversions();
  }

  public void setYCentre(int centre) {
    //      System.out.println("Set Y Centre to " + centre);
    yCentre = centre;
    calcConversions();
  }

  public int [] getXRange() {
    return _copyRange(xRange);
  }

  public int [] getYRange() {
    return _copyRange(yRange);
  }

  private int [] _copyRange(int [] fromRange) {
    int [] retRange = new int [2];

    retRange[0] = fromRange[0];
    retRange[1] = fromRange[1];

    return retRange;
  }

  public int getXMaximum() {
    return xRange[1];
  }

  public int getYMaximum() {
    return yRange[1];
  }

  public int getXMinimum() {
    return xRange[0];
  }

  public int getYMinimum() {
    return yRange[0];
  }

  public int getXCentre() {
    return xCentre;
  }

  public int getYCentre() {
    return yCentre;
  }

  public int [] getXVisibleRange() {
    return _copyRange(xVisibleRange);
  }

  public int [] getYVisibleRange() {
    return _copyRange(yVisibleRange);
  }

  // From centre and zoom calculate visible min and max, coordsperpixel and offset
  // coordsperpixel = range/(zoom * pixrange)
  // vismin = centre - coordsperpixel*pixrange/2
  // vismax = centre + coordsperpixel*pixrange/2+1
  // offset = (vismin - min)*coordsperpixel-pixmin
  // pixpos = offset + position*coordsperpixel

  private void calcConversions() {
    xCoordsPerPixel = (double)((double)(xRange[1]-xRange[0]+1)) /
                      (xZoomFactor*(double)(pixelBounds.width));
    yCoordsPerPixel = (double)((double)(yRange[1]-yRange[0]+1)) /
                      (yZoomFactor*(double)(pixelBounds.height));

    xVisibleRange[0] = (int)((double)xCentre -
                             xCoordsPerPixel*(double)pixelBounds.width/2.0);
    yVisibleRange[0] = (int)((double)yCentre -
                             yCoordsPerPixel*(double)(pixelBounds.height)/2.0);
    xVisibleRange[1] = (int)((double)xCentre +
                             xCoordsPerPixel*(double)(pixelBounds.width)/2.0) + 1;
    yVisibleRange[1] = (int)((double)yCentre +
                             yCoordsPerPixel*(double)(pixelBounds.height)/2.0) + 1;

    if (xOrientation > 0) { // dont know what this means
      xOffset = (long)((double)(xVisibleRange[0]-xRange[0])/xCoordsPerPixel)-
                pixelBounds.x;
    } else {
      xOffset = (long)((double)(xVisibleRange[0]-xRange[0])/xCoordsPerPixel)+
                pixelBounds.x+pixelBounds.width-1;
    }
    //      xOffset = (int)((double)(xVisibleRange[0]-xRange[0])/xCoordsPerPixel)-
    //                pixelBounds.x;
    if (yOrientation > 0) {
      yOffset = (int)((double)(yVisibleRange[0]-yRange[0])/yCoordsPerPixel)-
                pixelBounds.y;
    } else {
      yOffset = (int)((double)(yVisibleRange[0]-yRange[0])/yCoordsPerPixel)+
                pixelBounds.y+pixelBounds.height-1;
    }

    xPixelsPerCoord = 1.0/xCoordsPerPixel;
    yPixelsPerCoord = 1.0/yCoordsPerPixel;

    //these don't seem to be used anywhere
    //
    //toPixFactorX = (-xOrientation*xOffset)+xOrientation;
    //toPixFactorY = (-yOrientation*yOffset)+yOrientation;

     //writeFactors();
  }

  public void setYVisibleMinimum(int min) {
    yCentre = (int)((double)min + pixelBounds.height/2.0*yCoordsPerPixel);
    calcConversions();
  }
  public void setXVisibleMinimum(int min) {
    xCentre = (int)((double)min + pixelBounds.width/2.0*xCoordsPerPixel);
    calcConversions();
  }

  public void writeFactors() {
    System.out.println("Conversion factors:-");
    System.out.println("  xOffset:         " + xOffset);
    System.out.println("  yOffset:         " + yOffset);
    System.out.println("  xCoordsPerPixel: " + xCoordsPerPixel);
    System.out.println("  yCoordsPerPixel: " + yCoordsPerPixel);
    System.out.println("  xVisibleRange:   (" + xVisibleRange[0] + "," +
                       xVisibleRange[1] + ")");
    System.out.println("  yVisibleRange:   (" + yVisibleRange[0] + "," +
                       yVisibleRange[1] + ")");
    System.out.println("  xZoomFactor: " + xZoomFactor);
    System.out.println("  yZoomFactor: " + yZoomFactor);
    System.out.println("Settings:-");
    System.out.println("  xRange:          (" + xRange[0] + "," + xRange[1] + ")");
    System.out.println("  yRange:          (" + yRange[0] + "," + yRange[1] + ")");
    System.out.println("  pixelBounds:     " + pixelBounds);
    System.out.println("  xCentre:         " + xCentre);
    System.out.println("  yCentre:         " + yCentre);
    System.out.println("  xOrientation:    " + xOrientation);
    System.out.println("  yOrientation:    " + yOrientation);
  }

  /** used to be a long version of these functions as well -
      took them out because we dont need them anymore i believe.
      toPixelX actually returns the pixel value of the end
      of the base pair (which is halfway between the basepair(userX) and 
      the next bp(userX+1). To the left for xorientation LEFT, to the 
      right for xorientation RIGHT */
  public int toPixelX(int userX) {
    return (int)(xOrientation *
                 (-xOffset + 
                  (long)((double)(userX-xRange[0]) * xPixelsPerCoord)));
  }

  /** This takes care of doing the inclusion at the end of base pair ranges and
      compensates for xOrientation. the low basepair needs to subtract one 
      from it to include it. the low basepair is low in LEFT (normal) and high
      in RIGHT x orientation (revcomp), so hi and lo get flipped for revcomp.
  */
  public PixelRange basepairRangeToPixelRange(apollo.datamodel.RangeI range) {
    PixelRange pixRng = new PixelRange();
    if (getXOrientation() == LEFT) { // normal
      pixRng.low = toPixelX(range.getLow() - 1); // inclusive
      pixRng.hi = toPixelX(range.getHigh());
    }  
    else if (getXOrientation() == RIGHT) { // revcomp
      pixRng.low = toPixelX(range.getHigh());
      pixRng.hi = toPixelX(range.getLow() - 1); // inclusive
    }
    return pixRng;
  }

  public class PixelRange {
    public int low;
    public int hi;
    public int getWidth() {
      // mimicing code that was in DrawableUtil, not sure why its "+ 1" - 
      // inclusive pixel?
      return hi - low + 1; 
    }
    // ensure a minimal width of pixels
    public void ensureMinimumWidth(int min) {
      if (getWidth() >= min) {
        return;
      }
      if (getXOrientation() == LEFT) {
        hi = low + min - 1; // move hi to make min
      }
      else if (getXOrientation() == RIGHT) {
        low = hi - min + 1; // move lo to make min
      }
    }
  }

  public int toPixelY(int userY) {
    return (int)(yOrientation*(-yOffset+(int)((double)(userY-yRange[0])*yPixelsPerCoord)));
  }

  public Point toPixel(Point user) {
    return new Point(toPixelX(user.x),toPixelY(user.y));
  }

  public Point toPixel(int x,int y) {
    return toPixel(new Point(x,y));
  }

  public Point fromPixel(Point pixel) {
    return toUser(pixel);
  }

  public Point toUser(int x,int y) {
    return toUser(new Point(x,y));
  }
  public Point toUser(Point pixel) {
    int x;
    int y;
    if (xOrientation == Transformer.LEFT) {
      x = (int)((double)(pixel.x-pixelBounds.x) *
                xCoordsPerPixel + (double)xVisibleRange[0]);
    } else {
      x = (int)((double)(pixelBounds.x+pixelBounds.width-pixel.x) *
                xCoordsPerPixel + (double)xVisibleRange[0]);
    }

    if (yOrientation == Transformer.DOWN) {
      y = (int)((double)(pixel.y-pixelBounds.y) *
                yCoordsPerPixel + (double)yVisibleRange[0]);
    } else {
      y = (int)((double)(pixelBounds.y+pixelBounds.height-pixel.y) *
                yCoordsPerPixel + (double)yVisibleRange[0]);
    }
    return new Point(x,y);
  }

  public Point fromUser(Point user) {
    return toPixel(user);
  }

  // this doesnt seem to be used?
  public int minXUserCoordAtPixel(int pixel) {
    return -1;
  }

  public int maxXUserCoordAtPixel(int pixel) {
    return -1;
  }

  /** This gives the pixel value at the beginning of the base pair, which is halfway
      between the basepair(user) and the previous basepair(user-1). This is actually
      toPixelX(user-1) because toPixelX actually returns the pixel value of the end
      of the base pair (which is halfway between the basepair(user) and the next bp
      (user+1) */
  public int minXPixelAtUserCoord(int user) {
    return toPixelX(user-1);
  }

  /** This gives the pixel value at the end of the base pair, which is halfway
      between the basepair(user) and the next basepair(user+1). This is actually
      toPixelX(user) because toPixelX actually returns the pixel value of the end
      of the base pair  */
  public int maxXPixelAtUserCoord(int user) {
    return toPixelX(user);
  }
  public double getYCoordsPerPixel() {
    return yCoordsPerPixel;
  }
  public double getXCoordsPerPixel() {
    return xCoordsPerPixel;
  }
  public double getYPixelsPerCoord() {
    return yPixelsPerCoord;
  }
  public double getXPixelsPerCoord() {
    return xPixelsPerCoord;
  }

  public int getYOrientation() {
    return (int)yOrientation;
  }
  /** This can be Transformer.LEFT and Transformer.RIGHT. I believe that LEFT
   is normal (left to right) and RIGHT is revcomped (axis goes right to left) */
  public int getXOrientation() {
    return (int)xOrientation;
  }

  public static void main(String argv[]) {
    Transformer trans = new Transformer(new Rectangle(0,0,100,100),new Rectangle(1,1,10,10));
    trans.writeFactors();
    System.out.println("toPixelX for 1 = " + trans.toPixelX(1));
    System.out.println("toPixelX for 0 = " + trans.toPixelX(0));
  }

  public Object clone() throws CloneNotSupportedException {
    Transformer clone = new Transformer(pixelBounds,
                                        new Rectangle((int)xRange[0],
                                                      (int)yRange[0],
                                                      (int)(xRange[1]-xRange[0]),
                                                      (int)(yRange[1]-yRange[0])));
    clone.setXOrientation((int)xOrientation);
    clone.setYOrientation((int)yOrientation);
    clone.setXZoomFactor(xZoomFactor);
    clone.setYZoomFactor(yZoomFactor);
    clone.setXCentre(xCentre);
    clone.setYCentre(yCentre);
    return clone;
  }
}
