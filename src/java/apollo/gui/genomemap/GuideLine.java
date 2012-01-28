package apollo.gui.genomemap;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.text.*;

import apollo.datamodel.*;
import apollo.seq.*;
import apollo.gui.BaseScrollable;

import apollo.util.*;
import org.bdgp.util.*;
import org.bdgp.swing.FastTranslatedGraphics;

public class GuideLine implements BaseScrollable {
  ScaleView sv;
  int base;

  public GuideLine(ScaleView sv) {
    this.sv = sv;
    base = (int)sv.getCentre();
  }

  public void scrollToBase(int pos) {
    base = pos;
  }

  public int getVisibleBase() {
    if (base < (int)sv.getVisibleRange()[0]) {
      base = (int)sv.getVisibleRange()[0];
    } else if (base > (int)sv.getVisibleRange()[1]) {
      base = (int)sv.getVisibleRange()[1]-2;
    }
    // System.out.println("Current base = " + base);
    return base;
  }

  public int getVisibleBaseCount() {
    return 0;
  }
}
