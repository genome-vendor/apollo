package apollo.gui.detailviewers.sequencealigner;

public enum Orientation {

  FIVE_TO_THREE, THREE_TO_FIVE;

  public Orientation getOpposite() {
    switch (this) {
    case FIVE_TO_THREE:
      return THREE_TO_FIVE;
    case THREE_TO_FIVE:
      return FIVE_TO_THREE;
    }
    throw new AssertionError("Unknown orientation: " + this);
  }
}
