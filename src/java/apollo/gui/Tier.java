package apollo.gui;

/**
 * Judging from the methods here a Tier has drawing space, text end(?),
 * it might have a label, a non modifiable gap space of 1, and char space.
 * Its total space + drawSpace + charSpace + gapSpace
 * charSpace is where the labels go
 */

public abstract class Tier {

  int textEnd    = -1;

  public static final int MINHEIGHT = 3;

  int charSpace  = 0;
  int drawSpace  = 10;   // Starting height for tiers
  int gapSpace   = 1;

  int charLow;
  int charHigh;
  int drawLow;
  int drawCentre;
  int drawHigh;

  public Tier() {}

  public int getTextEnd() {
    return textEnd;
  }

  public void setTextEnd(int newEnd) {
    textEnd = newEnd;
  }

  public abstract String getTierLabel();

  /** adds size change to drawSpace, if labeled sets charHeight */
  public void setup(int sizeChange, int charHeight) {
    drawSpace += sizeChange;
    drawSpace = (drawSpace < MINHEIGHT) ? MINHEIGHT : drawSpace;

    if (isLabeled()) {
      setCharHeight(charHeight);
    } else {
      setCharHeight(0);
    }
  }

  protected abstract boolean isLabeled();

  public void updateUserCoordBoundaries(int lowBound) {
    drawLow = lowBound;
    drawHigh = drawLow + (int) (drawSpace * TierManager.Y_PIXELS_PER_FEATURE);

    drawCentre = (int) ((drawHigh - drawLow + 1) / 2 + drawLow);

    charLow = drawHigh;
    // shouldnt this drawSpace be charSpace?
    charHigh = charLow + (int) (drawSpace * TierManager.Y_PIXELS_PER_FEATURE);
  }

  public void setCharHeight(int height) {
    charSpace = height;
  }

  public int getTotalSpace() {
    return charSpace + drawSpace + gapSpace;
  }

  public int getDrawSpace() {
    return drawSpace;
  }

  public void setDrawSpace(int newSize) {
    drawSpace = newSize;
  }
  public int getDrawLow() {
    return drawLow;
  }
  public int getDrawHigh() {
    return drawHigh;
  }
  public int getDrawCentre() {
    return drawCentre;
  }
  public int getCharLow() {
    return charLow;
  }
  public int getCharHigh() {
    return charHigh;
  }
}
