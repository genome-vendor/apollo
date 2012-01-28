package apollo.gui;

import apollo.datamodel.*;

import javax.swing.*;

public abstract class WindowScoreCalculator extends ScoreCalculator {

  private int winSize = 15; 
  /** arent these constants? */
  private final static int minWinSize = 1; 
  // I don't get this.  Setting maxWinSize to 500 makes the slider only go up to 497;
  // setting it to 501 makes the slider go to 501.  Must be some sort of rounding issue.
  // SMJS Hopefully fixed (see below)
  private final static int maxWinSize = 500;
  int halfWinSize = 7;  // What is this?
  BoundedRangeModel windowModel;

  // Just allocated once
  int []   winExtents = new int[2];

  public WindowScoreCalculator(int winSize) {
    super();
    // SMJS Seem to need -1 on minimum for range to get correct bounds range for slider,
    //      which seems to not allow you to select the minimum value.
    windowModel = new DefaultBoundedRangeModel(winSize,0,minWinSize-1,maxWinSize);
    setWinSize(winSize);
  }

  public void setWinSize(int size) {
    winSize = size;
    halfWinSize = size/2;

    if (halfWinSize*2+1 != winSize) {
      winSize++;
    }
    windowModel.setValue(winSize);
  }

  public int getWinSize() {
    return winSize;
  }

  protected void getWindowExtents(int pos, int [] extents,
                                  int min, int max) {
    int low  = pos-halfWinSize;
    int high = pos+halfWinSize;

    extents[0] = low < min ? min : low;
    extents[1] = high > max ? max : high;
  }

  public boolean hasModel() { return true; }

  public BoundedRangeModel getModel() {
    return windowModel;
  }
}
