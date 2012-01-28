package jalview.gui.schemes;

import java.util.*;
import java.awt.*;

public class BuriedColourScheme extends ScoreColourScheme {

  public BuriedColourScheme() {
    super(ResidueProperties.buried,ResidueProperties.buriedmin,ResidueProperties.buriedmax);
  }

  public Color makeColour(float c) {
    return new Color(0,(float)(1.0-c),c);
  }
}
