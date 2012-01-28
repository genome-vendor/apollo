package apollo.gui.genomemap;

import java.awt.*;
import java.awt.image.*;
import java.text.*;

public class FastClippingGraphics extends Graphics {

  protected Graphics g;
  protected Rectangle clip;

  public FastClippingGraphics(Graphics g) {
    this.g = g;
    clip = g.getClipBounds();
  }

  public void translate(int x, int y) {
    g.translate(x,y);
  }

  public void clearRect(int x, int y, int width, int height) {
    g.clearRect(x,y,width,height);
  }

  public void clipRect(int x, int y, int width, int height) {
    g.clipRect(x,y,width,height);
    clip = g.getClipBounds();
  }

  public void copyArea(int x, int y, int width, int height,
                       int dx, int dy) {
    g.copyArea(x,y,width,height,dx,dy);
  }

  public Graphics create() {
    return g.create();
  }

  public Graphics create(int x, int y, int width, int height) {
    return g.create(x,y,width,height);
  }

  public void dispose() {
    //g.dispose();
    //g = null;
  }

  public void draw3DRect(int x, int y, int width, int height,
                         boolean raised) {
    g.draw3DRect(x,y,width,height,raised);
  }

  public void drawArc(int x, int y, int width, int height,
                      int startAngle, int arcAngle) {
    g.drawArc(x,y,width,height,startAngle,arcAngle);
  }
  public void drawBytes(byte[] data, int offset, int length,
                        int x, int y) {
    g.drawBytes(data,offset,length,x,y);
  }
  public void drawChars(char[] data, int offset, int length,
                        int x, int y) {
    //System.out.println("Drawing "+ data + " " + offset + " " + length + " " + x + " " + y);
    if ((length+offset) <= data.length) {
      g.drawChars(data,offset,length,x,y);
     }
  }
  public boolean drawImage(Image img, int x, int y,
                           Color bgcolor, ImageObserver observer) {
    return g.drawImage(img, x,y,bgcolor,observer);
  }

  public boolean drawImage(Image img, int x, int y,
                           ImageObserver observer) {
    return g.drawImage(img, x,y,observer);
  }

  public boolean drawImage(Image img, int x, int y,int width,int height,
                           Color bgcolor, ImageObserver observer) {
    return g.drawImage(img, x,y,width,height,bgcolor,observer);
  }

  public boolean drawImage(Image img, int x, int y,int width,int height,
                           ImageObserver observer) {
    return g.drawImage(img, x,y,width,height,observer);
  }

  public boolean drawImage(Image img, int dx1, int dy1, int dx2,
                           int dy2, int sx1, int sy1, int sx2, int sy2,
                           Color bgcolor, ImageObserver observer) {
    return g.drawImage(img, dx1, dy1, dx2, dy2,
                       sx1, sy1, sx2, sy2, bgcolor, observer);
  }

  public boolean drawImage(Image img, int dx1, int dy1, int dx2,
                           int dy2, int sx1, int sy1, int sx2, int sy2,
                           ImageObserver observer) {
    return g.drawImage(img, dx1, dy1, dx2, dy2,
                       sx1, sy1, sx2, sy2, observer);
  }

  public void drawLine(int x1, int y1, int x2, int y2) {
    g.drawLine(x1, y1, x2, y2);
  }

  public void drawOval(int x, int y, int width, int height) {
    g.drawOval(x, y, width, height);
  }

  public void drawPolygon(int[] xPoints, int[] yPoints,
                          int nPoints) {
    g.drawPolygon (xPoints, yPoints, nPoints);
  }

  public void drawPolygon(Polygon p) {
    g.drawPolygon (p);
  }

  public void drawPolyline(int[] xPoints, int[] yPoints,
                           int nPoints) {
    drawPolyline (xPoints, yPoints, nPoints);
  }

  public void drawRect(int x, int y, int width, int height) {
    g.drawRect(x, y, width, height);
  }

  public void drawRoundRect(int x, int y, int width,
                            int height, int arcWidth, int arcHeight) {
    g.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
  }

  /* Changed so that it can be compiled with 1.1. To get round the absence
     of java.text.AttributedCharacterIterator, a dummy version of this class
     has been created in apollo/ver1.1/apollo/gui and apollo/ver1.1 added to
     the CLASSPATH for compiling with java version 1.1.
     This is a hack.
   
     NOTE: The method is NON functional
  */
  public void drawString(AttributedCharacterIterator iterator,
                         int x, int y) {
    // does nothing
    // g.drawString(iterator, x, y);
  }

  public void drawString(String str, int x, int y) {
    g.drawString(str, x, y);
  }

  public void fill3DRect(int x, int y, int width, int height,
                         boolean raised) {
    g.fill3DRect(x, y, width, height, raised);
  }

  public void fillArc(int x, int y, int width, int height,
                      int startAngle, int arcAngle) {
    g.fillArc(x, y, width, height, startAngle, arcAngle);
  }

  public void fillOval(int x, int y, int width, int height) {
    g.fillOval(x, y, width, height);
  }

  public void fillPolygon(int[] xPoints, int[] yPoints,
                          int nPoints) {
    g.fillPolygon (xPoints, yPoints, nPoints);
  }

  public void fillPolygon(Polygon p) {
    g.fillPolygon (p);
  }

  public void fillRect(int x, int y, int width, int height) {
    g.fillRect(x, y, width, height);
  }

  public void fillRoundRect(int x, int y, int width,
                            int height, int arcWidth, int arcHeight) {
    g.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
  }

  public void finalize() {
    g.finalize();
    super.finalize();
  }

  public Shape getClip() {
    return g.getClip();
  }

  public Rectangle getClipBounds() {
    return clip;
  }

  public Rectangle getClipBounds(Rectangle r) {
    r.x = clip.x;
    r.y = clip.y;
    r.width = clip.width;
    r.height = clip.height;
    return r;
  }

  /**
   * @deprecated As of JDK 1.2
   */
  public Rectangle getClipRect() {
    return g.getClipBounds();
  }

  public Color getColor() {
    return g.getColor();
  }

  public Font getFont() {
    return g.getFont();
  }

  public FontMetrics getFontMetrics() {
    return g.getFontMetrics();
  }

  public FontMetrics getFontMetrics(Font f) {
    return g.getFontMetrics(f);
  }

  public boolean hitClip(int x, int y, int width, int height) {
    return new Rectangle(x,y,width,height).intersects(g.getClipBounds());
  }

  public void setClip(int x, int y, int width, int height) {
    g.setClip(x,y,width,height);
    clip = g.getClipBounds();
  }

  public void setClip(Shape clipS) {
    if (clip instanceof Rectangle) {
      Rectangle clip = (Rectangle)clipS;
      g.setClip(clip.x,clip.y,clip.width,clip.height);
      this.clip = g.getClipBounds();
    }
  }
  public void setClip(Rectangle clip) {
    g.setClip(clip.x,clip.y,clip.width,clip.height);
    this.clip = g.getClipBounds();
  }

  public void setColor(Color c) {
    g.setColor(c);
  }

  public void setFont(Font f) {
    g.setFont(f);
  }

  public void setPaintMode() {
    g.setPaintMode();
  }

  public void setXORMode(Color c1) {
    g.setXORMode(c1);
  }

  public String toString() {
    return g.toString()+" [clip "+clip+"]";
  }
}
