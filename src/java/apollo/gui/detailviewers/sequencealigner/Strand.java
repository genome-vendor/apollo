package apollo.gui.detailviewers.sequencealigner;

public enum Strand {

  FORWARD, REVERSE;

  public Strand getOpposite() {
    switch (this) {
    case FORWARD:
      return REVERSE;
    case REVERSE:
      return FORWARD;
    }
    throw new AssertionError("Unknown strand: " + this);
  }

  public int toInt() {
    switch (this) {
    case FORWARD:
      return 1;
    case REVERSE:
      return -1;
    }
    throw new AssertionError("Unknown strand: " + this);
  }

  public static Strand valueOf(int i) {
    for (Strand s : Strand.values()) {
      if (s.toInt() == i) {
        return s;
      }
    }
    throw new IllegalArgumentException("not a valid strand" + i);
  }
}
