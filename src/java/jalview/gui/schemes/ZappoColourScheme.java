package jalview.gui.schemes;

import jalview.gui.*;
import jalview.datamodel.*;

import java.util.*;
import java.awt.*;

public class ZappoColourScheme extends ResidueColourScheme {

  public ZappoColourScheme() {
    super(ResidueProperties.color,0);
  }

  public boolean isUserDefinable() {
    return true;
  }

}
