package jalview.io;

import java.util.*;

public class PostscriptProperties {

  public static final int PORTRAIT = 0;
  public static final int LANDSCAPE = 1;

  public static int SHORTSIDE = 612;
  public static int LONGSIDE = 792;

  static Vector fonts = new Vector();

  static {
    fonts.addElement("Helvetica");
    fonts.addElement("Times-Roman");
    fonts.addElement("Courier");
  }

  static Vector fontsizes = new Vector();

  static {
    fontsizes.addElement("6");
    fontsizes.addElement("8");
    fontsizes.addElement("10");
    fontsizes.addElement("12");
    fontsizes.addElement("14");
    fontsizes.addElement("16");
  }

  public int orientation = PORTRAIT;
  public int width = SHORTSIDE;
  public int height = LONGSIDE;

  public int xoffset = 30;
  public int yoffset = 30;
  public int fsize = 8;
  public String font = "Helvetica";

  public PostscriptProperties() {}

  public PostscriptProperties(int or, int w, int h, int xoff, int yoff, int fsize, String font) {

    this.orientation = or;
    this.width = w;
    this.height = h;
    this.xoffset = xoff;
    this.yoffset = yoff;
    this.fsize = fsize;
    this.font = font;
  }

  public int getOrientation() {
    return orientation;
  }
  public int getWidth() {
    return width;
  }
  public int getHeight() {
    return height;
  }
  public int getXOffset() {
    return xoffset;
  }
  public int getYOffset() {
    return yoffset;
  }
  public int getFSize() {
    return fsize;
  }
  public String getFont() {
    return font;
  }
}
