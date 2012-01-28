/*
	Copyright (c) 2000
	BDGP, University of California Berkeley
	All Rights Reserved
*/

package apollo.gui.drawable;

import apollo.datamodel.*;
import apollo.config.Config;
import apollo.gui.TierManagerI;
import apollo.gui.Transformer;
import apollo.config.FeatureProperty;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * A glyph that draws a solid bar with a triangle at the top or bottom.
 * This might be useful for showing transposon insertions, for example.
 * Renamed from DrawableInsertion to Triangle in order to be more descriptive of 
 * what is actually drawn
 */
public class Triangle extends DrawableSeqFeature {
  int x[];
  int y[];

  private int site;

  /**
   * constructs a glyph with the triangle at the top
   * pointing in the direction of the insertion.
   * when selected a 1 pixel line is draw down to the insertion site
   * the triangle is filled with gray and outlined in black,
   * and the bar is black.
   */

  public Triangle() {
    super(true);
    this.x = new int[3];
    this.y = new int[3];
  }

  public Triangle (SeqFeatureI feature) {
    // this will call set feature below
    super(feature, true);
    this.x = new int[3];
    this.y = new int[3];
  }

  public void setFeature(SeqFeatureI feature) {
    super.setFeature(feature); // sets featureProperty as well as feat
    String site_str = feature.getProperty("insertion_site");
    if ((site_str == null || site_str.equals("")) && 
        feature.getRefFeature() != null)
      site_str = feature.getRefFeature().getProperty("insertion_site");
    try {
      site = Integer.parseInt (site_str) - feature.getStrand();
    } catch (Exception e) {
      site = (feature.length() / 2);
    }
  }

  public void drawSelected(Graphics g,
                           Rectangle boxBounds,
                           Transformer transformer,
                           TierManagerI manager) {
    drawSite (g, boxBounds, transformer, manager);
    // draw a line from base of triangle to the same relative
    // distance across on the opposite strand
    // 10 is the height of the central axis
    // 0 is the y for the top of the central axis
    g.setColor(Color.red);
    int y2;
    Rectangle pixBox = transformer.getPixelBounds();
    if ((getStrand() >= 0 &&
         transformer.getXOrientation() == Transformer.LEFT) ||
        (getStrand() < 0 &&
         transformer.getXOrientation() == Transformer.RIGHT))
      y2 = pixBox.y + pixBox.height;
    else
      y2 = pixBox.y;
    g.drawLine (x[0], y[0], x[0], y2);
  }

  public void drawUnselected(Graphics g,
                             Rectangle boxBounds,
                             Transformer transformer,
                             TierManagerI manager) {
    drawSite (g, boxBounds, transformer, manager);
  }

  public void addHighlights(Graphics g,
                            Rectangle boxBounds,
                            Transformer transformer,
                            TierManagerI manager) {
    if (isHighlighted()) {
      // assume that the polygon has already been set by
      // drawSite
      g.setColor(Config.getHighlightColor());
      g.drawPolygon (x, y, 3);
    }

  }

  protected void drawSite(Graphics g,
                          Rectangle boxBounds,
                          Transformer transformer,
                          TierManagerI manager) {
    int offset
      = (int) (site * transformer.getXPixelsPerCoord());

    // if right then the DNA has been reverse-complemented
    // by the user
    int xtricenter;
    int xpoint;
    if ((getStrand() >= 0 &&
         transformer.getXOrientation() == Transformer.LEFT) ||
        (getStrand() < 0 &&
         transformer.getXOrientation() == Transformer.RIGHT)) {
      xtricenter = boxBounds.x + offset;
      xpoint = xtricenter + 8; // (int) ((boxBounds.height+1) * 0.5 );
    }
    else {
      xtricenter = (boxBounds.x + boxBounds.width) - offset;
      xpoint = xtricenter - 8; // (int) ((boxBounds.height+1) * 0.5 );
    }

    int ytricenter = boxBounds.y + (int) ((boxBounds.height+1) * 0.5);

    x[0] = xtricenter;
    y[0] = boxBounds.y;

    x[2] = xtricenter;
    y[2] = boxBounds.y + boxBounds.height + 1;

    x[1] = xpoint;
    y[1] = ytricenter;

    g.setColor(getDrawableColor());
    g.fillPolygon (x, y, 3);
  }
}

