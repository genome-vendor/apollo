package apollo.gui.drawable;

import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Font;
import java.util.*;

import apollo.datamodel.*;
import apollo.config.Config;
import apollo.gui.TierManagerI;
import apollo.gui.Transformer;
import apollo.gui.event.*;

import apollo.gui.genomemap.*;
import apollo.gui.synteny.*;
import apollo.util.*;

public class DrawablePhaseHighlightNoHatGeneFeatureSet extends DrawablePhaseHighlightGeneFeatureSet
  implements DrawableSetI {

  public void drawUnselected(Graphics g,
                             Rectangle boxBounds,
                             Transformer transformer,
                             TierManagerI manager) {
    if (size() > 1) {
      Color color = getDrawableColor();
      g.setColor(color);
      int y_center = getYCentre(boxBounds);

      g.drawLine(boxBounds.x < 0 ? 0 : boxBounds.x,
                 y_center,
                 // Hack for crap Java 1.1 which must use a short somewhere
                 // If displays get larger than 3000 pixels width
                 // this will fail
                 (boxBounds.x + boxBounds.width > 3000 ?
                 3000 : boxBounds.x + boxBounds.width),
                 y_center);
    }

  }
}
