package apollo.gui.genomemap;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

import javax.swing.*;

import apollo.config.FeatureProperty;
import apollo.datamodel.*;
import apollo.gui.Selection;
import apollo.gui.TierManagerI;
import apollo.gui.Transformer;
import apollo.gui.drawable.Drawable;
import apollo.gui.drawable.DrawableUtil;
import apollo.gui.event.*;

/**
 * A FeatureView which has extra methods to handle dragging.
 */
public class DragView extends ManagedView implements DragViewI {
  TierViewI originView;
  Point           originPoint;
  Point           relativePosition;
  Color           hotspotColor = Color.yellow;
  boolean         inHotspot    = false;
  int             hotspotType  = 0;
  Selection       selection;
  protected Vector drawables = new Vector();

  public static int EVIDENCE_HOTSPOT = 1;
  public static int OTHER_HOTSPOT    = 2;

  public DragView(JComponent ap, String name, Selection selection) {
    super(ap,name,false);
    this.selection = selection;
  }

  /** Sets ignoreScoreThresholds to true. 
      Makes stacker not consider score thresholds 
      in populating the tier.
      This is needed for when ya drag a feature that has
      a score under threshold, but since it has siblings(in feat set)
      above threshold it gets displayed. Since only items that have 
      somehow gotten through the threshold (either by self score or siblings)
      can be displayed and thus dragged, there is no need to do the 
      score threshold again. */
  public void setTierManager(TierManagerI manager) {
    /* this does assume that the drawables have already been provided
     */
    manager.setIgnoreScoreThresholds(true);
    manager.setTierData(drawables);
    super.setTierManager(manager);
  }

  public Selection getSelection() {
    return selection;
  }

  public void setHotspotColor(Color in) {
    hotspotColor = in;
  }

  public void setHotspotType(int Type) {
    hotspotType = Type;
  }

  public void setInHotspot(boolean hotspot) {
    inHotspot = hotspot;
  }

  public void paintView() {
    if (isInvalid() || visibleDrawables == null) {
      visibleDrawables = getVisibleDrawables();
    }
    // visibleDrawables set to null on clear() for a load 
    int visFeatSize = (visibleDrawables == null ? 0 : visibleDrawables.size());
    if (visFeatSize == 0)
      return;
    int i=0;
    for (i = 0; i < visFeatSize; i++) {
      Vector curVis = (Vector)visibleDrawables.elementAt(i);
      for (int j = 0; j < curVis.size(); j++) {
        Drawable dsf = (Drawable)curVis.elementAt(j);
        if (inHotspot) {
          FeatureProperty fp  = dsf.getFeatureProperty();
          Color           old = fp.getColour();

          int             red, green, blue;

          if (hotspotType == EVIDENCE_HOTSPOT) {
            red   = 255 - old.getRed();
            green = 255 - old.getGreen();
            blue  = 255 - old.getBlue();
          } else {
            red   = old.getRed()&0x8f;
            green = old.getGreen()&0x8f;
            blue  = old.getBlue()&0x8f;
          }
          fp.setColour(new Color(red, green, blue), true);
          dsf.draw(graphics, transformer, manager);
          fp.setColour(old, true);
        } else {
          dsf.draw(graphics, transformer, manager);
        }
      }
    }
  }

  public Vector collectShadows() {
    Vector shadowBoxes = new Vector();
    if (visibleDrawables == null)
      visibleDrawables = getVisibleDrawables();
    for (int i = 0; i < visibleDrawables.size(); i++) {
      Vector curVis = (Vector)visibleDrawables.elementAt(i);
      for (int j = 0; j < curVis.size(); j++) {
        Drawable dsf = (Drawable)curVis.elementAt(j);
	DrawableUtil.setBoxBounds(dsf,
				  transformer,
				  manager);
	shadowBoxes.addElement(dsf.getBoxBounds());
      }
    }
    return shadowBoxes;
  }

  public void setOrigin(TierViewI v, Point p) {
    originView  = v;
    originPoint = p;
  }

  public TierViewI getOriginView() {
    return originView;
  }

  public Point getOriginPosition() {
    return originPoint;
  }

  public void setLocation(Point p) {
    getBounds().x = p.x - relativePosition.x;

    if (getTransform().getYOrientation() == Transformer.DOWN) {
      getBounds().y = p.y;
    } else {
      getBounds().y = p.y-getBounds().height;
    }
    setBounds(getBounds());
  }

  public void setRelativePosition(Point p) {
    relativePosition = p;
  }

  protected void setDrawables(Vector drawables) {
    if (drawables != null)
      this.drawables = drawables;
    else
      drawables.clear();
    if (manager != null)
      manager.setTierData(drawables);
  }
}
