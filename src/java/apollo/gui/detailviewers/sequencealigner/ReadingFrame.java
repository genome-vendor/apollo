package apollo.gui.detailviewers.sequencealigner;

import org.bdgp.util.DNAUtils;

public enum ReadingFrame {
  // Do we need none?
  NONE, ONE, TWO, THREE;

  public int getIndex() {
    switch (this) {
    case ONE: return 0;
    case TWO: return 1;
    case THREE: return 2;
    case NONE: new AssertionError("No Index for ReadingFrame: " + this);
    }
    throw new AssertionError("Unknown ReadingFrame: " + this);
  }
  
  public static ReadingFrame valueOf(int i) {
    switch (i) {
    case 1: return ONE;
    case 2: return TWO;
    case 3: return THREE;
    default: return NONE;
    }
  }
  
  public int toInt() {
    switch (this) {
    case ONE: return 1;
    case TWO: return 2;
    case THREE: return 3;
    case NONE: return -1;
    //case NONE: new AssertionError("No int value for ReadingFrame: " + this);
    }
    throw new AssertionError("Unknown ReadingFrame: " + this);
  }
  
  public int getDNAUtilsValue() {
    switch (this) {
    case NONE: return DNAUtils.FRAME_NEG_ONE;
    case ONE: return DNAUtils.FRAME_ONE;
    case TWO: return DNAUtils.FRAME_TWO;
    case THREE: return DNAUtils.FRAME_THREE;
    }
    throw new AssertionError("Unknown ReadingFrame: " + this);
  }
}
