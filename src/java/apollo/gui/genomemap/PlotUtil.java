package apollo.gui.genomemap;

import java.util.*;
import java.awt.*;

import apollo.gui.Transformer;

public class PlotUtil {

  private PlotUtil() {}

  public static int calcTickInterval(int [] range) {
    return calcTickInterval(range, null, null, null);
  }

  public static int calcTickInterval(int [] range, Graphics graphics,
                                     Transformer transformer, Vector labels) {

    int startCoord = (int)range[0];
    int endCoord   = (int)range[1];
    int length = endCoord - startCoord + 1;

    int tmp   = (int)(Math.log(length)/Math.log(10));
    int ii   = 1;

    int tick = (int)Math.pow(10,tmp-ii);

    // System.out.println("tick after pow = " + tick);

    if (tick == 0) {
      tick = 1;
    }

    double dtick = (double)tick;
    double dtickorig = dtick;
    double [] tickints = new double [6];
    if (tick > 1) {
      tickints[0] = 2.0;
      tickints[1] = 2.5;
      tickints[2] = 5.0;
      tickints[3] = 8.0;
      tickints[4] = 10.0;
      tickints[5] = 20.0;
    } else {
      tickints[0] = 2.0;
      tickints[1] = 4.0;
      tickints[2] = 5.0;
      tickints[3] = 8.0;
      tickints[4] = 10.0;
      tickints[5] = 20.0;
    }
    int intnum=0;
    while  ((double)(length)/dtick > 10.0) {
      dtick = dtickorig*tickints[intnum++];
    }
    tick = (int)dtick;

    // Check for text overlaps
    // Need to check whether any of the tick labels is longer than the space
    // between two tick marks.
    if (graphics != null) {
      FontMetrics fm = graphics.getFontMetrics();
      boolean overlaps = true;
      intnum--;

      // System.out.println("Initial tick = " + tick + " intnum = " + intnum);
      while (overlaps && intnum < tickints.length-1) {
        int startpos = startCoord/tick * tick;
        Point start = transformer.toPixel(new Point((int)startpos,0));
        Point end   = transformer.toPixel(new Point((int)startpos+tick,0));
        int tickPix = 0;
        if (transformer.getXOrientation() == Transformer.LEFT) {
          tickPix = end.x - start.x-1;
          tickPix -=  (end.x - start.x-1)/5;
        } else {
          tickPix = start.x - end.x-1;
          tickPix -=  (start.x - end.x-1)/5;
        }
        overlaps = false;
        for (int i = startpos; i <= endCoord && !overlaps; i = i + tick) {
          String label = getLabel (tick, startpos, i);
          if (fm.stringWidth(label) > tickPix) {
            overlaps = true;
            dtick = dtickorig*tickints[++intnum];
            // System.out.println("Modifying tick to " + dtick);
            tick = (int)dtick;
          }
        }
      }
    }
    if (labels != null) {
      int startpos = startCoord/tick * tick;
      for (int i = startpos; i <= endCoord; i = i + tick) {
        String label = getLabel (tick, startpos, i);
        labels.addElement(label);
      }
    }
    return tick;
  }

  public static double log10(double x) {
    return Math.log(x) * .43429448190325182765112891891660508229439700580366;
  }

  private static String getLabel (int tick, int startpos, int i) {
    String label;
    if (log10(tick) >= 4 || (startpos > 100000 && log10(tick) >= 3)) {
      double mbval = i/1000000.0;
      label = String.valueOf(mbval) + "Mb";
    } else {
      label = String.valueOf(i);
    }
    return label;
  }
}
