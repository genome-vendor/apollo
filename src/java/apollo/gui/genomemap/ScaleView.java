package apollo.gui.genomemap;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

import apollo.datamodel.*;
import apollo.seq.*;
import apollo.config.Config;
import apollo.config.PropertyScheme;
import apollo.gui.Selection;
import apollo.gui.SelectionItem;
import apollo.gui.Transformer;
import apollo.gui.SelectionManager;
import apollo.gui.drawable.DrawableUtil;
import apollo.gui.event.*;
import apollo.gui.menus.*;
import apollo.gui.detailviewers.seqexport.SeqExport;
import apollo.seq.io.FastaFile;

import org.apache.log4j.*;

/**
 * Draw a scale line, BUT also draws sequence, and allows dragging of hairpins.
 *
 * I think it would be easier if pixels were used instead of user coords in
 * the vertical direction
 */
public class ScaleView extends ScrollAdjustedView
  // PickViewI is for rubberbanding around axis
  implements PopupViewI, PickViewI {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ScaleView.class);

  /** y value in user coords of scale */
  private final static int scaleLineY = 3;
  /** Y value of lower sequence placement - is there some way to calculate?
    This is in user coords NOT pixels, 1 user coord == 10 pixels */
  private final static int   viewYbottom = 10;
  /** Y val for top seq */
  private final static int   viewYtop = -4;

  public static final int    SPLICE   = 1;
  public static final int    FEATURES = 2;

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private CurationSet        curation;
  private boolean            minorTicks   = true;
  // For when user has rubberbanded a rectangle
  private Rectangle          selectedRect;
  // Currently rubberbanded region
  private int                startBase, endBase, selectedStrand;
  /** Don't pop up a sequence window for rubberbanded region if it's too big
      --just offer to save sequence as a file. */
  private int                maxSeqSelection = 100000;
  /** For indicating the beginning and end of the range that has sequence */
  private int                startOfRange = -1;
  private int                endOfRange = -1;

  /** ScaleColouringManager is an inner class */
  private ScaleColouringManager colouringManager = new ScaleColouringManager();

  private Hashtable sites = new Hashtable(1);

  public ScaleView(JComponent ap, String name) {
    super(ap, name, true, 46);
    // maybe take out user coords in y direction - kindof awkward
    // I think it might be easier for layout to just do the y
    // direction in pixels
    // assuming we dont ever want zoom vertically?
    transformer.setYRange(new int [] {-10,10});
  }

  /** This gets set to null by szap.getClearData to clear out old cur set on
      loads. All drawing access to curation set has to check for null curation
      set on every use of curation set, because they are on 2 different
      threads, curation set could be nulled at anytime during draw. An
      alternative would be to have this spin if in the middle of the draw but
      the funny thing there is you are then delaying the load in order to draw
      the scale view of the old cur set you dont care about anymore - so the
      null checks dont delay the load */
  public void setCurationSet(CurationSet set) {
    curation = set;
    setRangeThatHasSequence();
  }

  public void setMinorTicks(boolean state) {
    minorTicks = state;
  }

  public void setSelectedRange(int startBase, int endBase, int strand) {
    this.startBase = startBase;
    this.endBase = endBase;
    this.selectedStrand = strand;
  }

  public void clear() {
    startBase = endBase = 0;
  }

  // If user has rubberbanded at the axis, this will calculate the rectangle
  // around which to draw the selection box.  (Called by paintView.)
  public Rectangle getSelectedRectangle() {
    if (startBase == endBase)
      return null;

    int box_width;
    if (!reverseComplement)
      box_width = (int) (transformer.getXPixelsPerCoord() * 
                         (endBase-startBase+1));
    else
      box_width = (int) (transformer.getXPixelsPerCoord() * 
                         (startBase-endBase+1));

    int box_y = ((selectedStrand == 1) ? 
                 viewYtop : viewYbottom);
    box_y = box_y - 6;
    int box_height = graphics.getFontMetrics().getAscent();
    if (selectedStrand == -1)
      box_height -= 2;  // So it doesn't get cut off on the bottom

    Point box = ( !reverseComplement ?
                  transformer.toPixel(new Point(startBase-1, box_y)) :
                  transformer.toPixel(new Point(startBase, box_y)) );
    selectedRect = new Rectangle(box.x, box.y, box_width, box_height+4);
    return selectedRect;
  }

  // LinearView method that must be implemented
  public void paintView() {
    Vector labels = new Vector();

    if (curation == null) {
      logger.debug ("Still being initialized");
      return;
    }

    graphics.setColor(getBackgroundColour());

    // Clear the whole view
    graphics.fillRect(getBounds().x,
                      getBounds().y,
                      getBounds().width,
                      getBounds().height);


    int [] visRange = getVisibleRange();

    // Get the visible limits.
    int startCoord = (int)visRange[0];
    int endCoord   = (int)visRange[1];

    // Modified from jalview drawScale or the above
    graphics.setColor(getForegroundColour());

    // draw the big horizontal line across
    Point start = transformer.toPixel(new Point((int)visRange[0],
                                                scaleLineY));
    Point end  = transformer.toPixel(new Point((int)visRange[1],
                                               scaleLineY));
    graphics.drawLine(start.x,start.y,end.x,end.y);

    int tick = PlotUtil.calcTickInterval(visRange,graphics,transformer,labels);

    // Draw the scale
    int startpos = startCoord/tick * tick;

    // center the tick directly beneath the base
    int half_coord = (int) (transformer.getXPixelsPerCoord() * 0.5);
    int firsttick = (int) (startpos-tick * 0.5);
    int tickYtop = scaleLineY - 2;

    if (firsttick >= startCoord) {
      if (minorTicks) {
        start = transformer.toPixel(new Point(firsttick, scaleLineY));
        end   = transformer.toPixel(new Point(firsttick, tickYtop));
        graphics.drawLine(start.x - half_coord, start.y,
                          end.x - half_coord, end.y);
      }
    }
    int tickNum =0;
    // multiply is faster then / 2?
    // do both of these outside the loop
    int half_tick = (int) (tick * 0.5);
    graphics.setFont(Config.getDefaultFont());
    FontMetrics fm = graphics.getFontMetrics();
    for (int i = startpos; i <= endCoord; i = i + tick) {
      start = transformer.toPixel(new Point(i, scaleLineY));
      end   = transformer.toPixel(new Point(i, tickYtop));
      start.x = start.x - half_coord;
      end.x = end.x - half_coord;
      graphics.drawLine(start.x,start.y,end.x,end.y);

      // center the base number string directly above the tick
      String label = (String) labels.elementAt(tickNum++);
      start.x = start.x - (int) (fm.stringWidth(label) * 0.5);
      graphics.drawString(label, start.x, start.y - 3);

      if (minorTicks) { // half sized ticks halway between
        start = transformer.toPixel(new Point((int)i + half_tick,
                                              scaleLineY));
        end   = transformer.toPixel(new Point((int)i + half_tick,
                                              tickYtop));
        start.x = start.x - half_coord;
        end.x = end.x - half_coord;
        graphics.drawLine(start.x,start.y,end.x,end.y);
      }
    }

    // Outline the selected rectangle (if any)
    Rectangle rect = getSelectedRectangle();
    if (rect != null) {
      graphics.setColor(Config.getStyle().getSelectionColor());
      graphics.drawRect(rect.x,
                        rect.y,
                        rect.width,
                        rect.height);
    }

    int topStrand = reverseComplement ? -1 : 1;
    int bottomStrand = -1 * topStrand;

    /* color in all the sequencing errors before anything else so
       they end up as the background (except for the scale lines
       and ticks which get clobbered */
    drawSequencingEdits (graphics, transformer, startCoord, endCoord);

    drawOutOfRangeLines (graphics, transformer, startCoord, endCoord);

    // top sequence
    drawSequence(graphics,
                 transformer,
                 viewYtop,
                 topStrand);
    // bottom_sequence
    drawSequence(graphics,
                 transformer,
                 viewYbottom,
                 bottomStrand);
    
  }

  public void clearCutSites() {
    sites.clear();
    getComponent().repaint();
  }

  public void addCutSites(Vector forward_sites,
                          Vector reverse_sites,
                          Color site_color) {
    Vector [] match_positions = new Vector [2];
    match_positions[0] = forward_sites;
    match_positions[1] = reverse_sites;
    if (sites.get(site_color) != null)
      logger.warn ("Yikes, already using color " + site_color);
    sites.put(site_color, match_positions);
    getComponent().repaint();
  }

  /** @param complement if true causes retrieved sequence to complemented
   * NOT reverse complemented just complemented */
  private void drawSequence(Graphics graphics,
                            Transformer transformer,
                            int viewY,
                            int strand) {
    double pixelspercoord = transformer.getXPixelsPerCoord();

    boolean zoomed_out = pixelspercoord < 0.075;

    int [] visRange = getVisibleRange();
    if (curation == null) {
      // curation may get set to null mid draw - is it bad to kill draw?
      return;
    }
    int startCoord = (int) (visRange[0] < curation.getLow() ?
                            curation.getLow() : visRange[0]);
    int endCoord   = (int) (visRange[1] > curation.getHigh() ?
                            curation.getHigh() : visRange[1]);

    // don't get the sequence if we are zoomed out
    String seq = (!zoomed_out ?
                  getSequence(startCoord, endCoord, strand) : null);
    if (seq == null)
      zoomed_out = true;

    FontMetrics fm = graphics.getFontMetrics();
    // the g is arbitrary, an A, T, or C would be the same
    int offset = (int) ((pixelspercoord * 0.5) - (fm.stringWidth("G") / 2));

    // draw features first so that they are underneath the cut sites
    if (colouringManager.getColouringMode() == FEATURES && !zoomed_out) {
      graphics.setColor(Config.getSequenceColor());
      if (pixelspercoord >= 5) {
        drawFeatureBases (graphics, transformer,
                          startCoord, endCoord,
                          seq, offset, viewY, strand,
                          buildBaseColorArray(startCoord,
                                              endCoord,
                                              strand));
      } else if (pixelspercoord >= 1) {
        /* somewhat zoomed out, so only draw features (restriction sites)
           as boxes */
        drawFeatureBoxes (graphics, transformer,
                          startCoord, endCoord,
                          viewY, 
                          buildBaseColorArray(startCoord,
                                              endCoord,
                                              strand));
        }
    }

    /* the coloring of sites interferes with the coloring of splices
       so do one or the other, not both */
    if (colouringManager.getColouringMode() == FEATURES) {
        if (pixelspercoord < 5) {
            // zoomed out and at large scale, so only draw sites
            // do this second so that they are on top of the feature
            drawSites (graphics, transformer, 
                       startCoord, endCoord,
                       viewY, strand);
        }
        else {  // Zoomed way in--send sequence so bases can be drawn too
            drawSites(graphics, transformer,
                      startCoord, endCoord,
                      seq, offset, viewY, strand);
        }
    }

    if (!zoomed_out) {
      if (strand == 1) {
        /* no sense doing this for both strands, twice as much work
           and nothing gained since it applies to both */
        drawGaps (graphics, transformer,
                  startCoord, endCoord,
                  seq, offset);
      }

      graphics.setColor(Config.getSequenceColor());

      if (colouringManager.getColouringMode() == SPLICE) {
        if (pixelspercoord >= 5) {
          // At 5 pixelspercoord, bases run into each other.
          // Could change to 5.7
          // for less collision, but then you have to zoom more before 
          // bases appear.
          drawSpliceBases (graphics, transformer,
                           startCoord, endCoord,
                           seq, offset, viewY, strand);
        }
        else if (pixelspercoord >= 1) {
          // Do both strands.
          int topStrand = reverseComplement ? -1 : 1;
          int bottomStrand = -1 * topStrand;
          drawSpliceLines (graphics, transformer, startCoord, endCoord, seq, topStrand);
          drawSpliceLines (graphics, transformer, startCoord, endCoord, seq, bottomStrand);
        }
      }
    }
  }

  protected void drawSpliceBases (Graphics graphics, Transformer transformer,
                                  int startCoord, int endCoord,
                                  String seq, int offset, int viewY,
                                  int strand) {
    Color default_color = Config.getSequenceColor();
    Color prior_color = default_color;
    for (int i = startCoord; i <= endCoord; i++) {
      Color base_color = getSpliceColor (graphics, i, 
                                         startCoord, endCoord, 
                                         seq, strand, 
                                         prior_color);
      graphics.setColor(base_color);
      prior_color = (base_color == prior_color ? 
                     default_color : base_color);
      Point start = ( ! reverseComplement ?
                      transformer.toPixel(new Point((int)i - 1, viewY)) :
                      transformer.toPixel(new Point((int)i, viewY)) );
      int index = getSeqIndex(i, startCoord, endCoord);
      String base;
      if ((index + 1) < seq.length())
        base = seq.substring(index, index + 1);
      else
        base = seq.substring(index);
      graphics.drawString(base, start.x + offset, start.y);
    }
  }

  protected void drawSpliceLines (Graphics graphics, Transformer transformer,
                                  int startCoord, int endCoord, String seq,
                                  int strand) {
      Color default_color = getBackgroundColour();
      Color prior_color = default_color;
      int height = 10;
      int y = viewYtop;
      if (strand == -1)
        y = viewYbottom;

      for (int i = startCoord; i <= endCoord; i++) {
          Color base_color = getSpliceColor (graphics, i, 
                                             startCoord, endCoord, 
                                             seq, strand, 
                                             prior_color);
          graphics.setColor(base_color);
          prior_color = (base_color == prior_color ? 
                         default_color : base_color);
          if (base_color != default_color) {
              Point start = (reverseComplement ?
                             transformer.toPixel(new Point((int)i, y)) :
                             transformer.toPixel(new Point((int)i - 1, y)));
              graphics.drawLine(start.x,start.y,start.x,start.y-height);
          }
      }
  }

  private int getSeqIndex(int i, int startCoord, int endCoord) {
    return ( ! reverseComplement ?
             (int) (i - startCoord) :
             (int) (endCoord - i) );
  }

  protected Color getSpliceColor (Graphics graphics, int i,
                                  int startCoord, int endCoord, 
                                  String seq,
                                  int strand,
                                  Color prior_color) {
    int index = getSeqIndex(i, startCoord, endCoord);
    String seq2;
    int splice_length = 2;
    if (reverseComplement) {
      index--;
      if (index < 0) {
        index = 0;
        splice_length = 1;
      }
    } 
    if ((index + splice_length) < seq.length())
      seq2 = seq.substring(index, index + splice_length);
    else
      seq2 = seq.substring(index);
    
    /* GT is the canonical donor
    AG is the canonical acceptor
    */
    if ((strand == 1 && ! reverseComplement) ||
        (strand == -1 && reverseComplement)) {
      if (seq2.equals("GT")) {
        return Color.orange;
      } else if (seq2.equals("AG")) {
        return Color.cyan;
      } else {
        return prior_color;
      }
    } else {
      if (seq2.equals("TG")) {
        return Color.orange;
      } else if (seq2.equals("GA")) {
        return (Color.cyan);
      } else {
        return(prior_color);
      }
    }
  }
  
  /** Draws the bases and selection background if in selected area */
  private void drawFeatureBases (Graphics graphics, Transformer transformer,
                                 int startCoord, int endCoord, String seq,
                                 int xOffset, int viewY, int strand,
                                 Color [] base_colors) {

    if (seq == null || seq.length() < 2)
      return;

    char [] bases = seq.toCharArray();
    int box_width = (int) transformer.getXPixelsPerCoord() + 1;
    int box_y = viewY - 6;
    int height = graphics.getFontMetrics().getAscent() + 4;

    for (int i = startCoord; i <= endCoord; i++) {
      int index = ( ! reverseComplement ?
                    (int) (i - startCoord) :
                    (int) (endCoord - i) );

      // if region is selected, getBaseColor returns the color of the
      // selected item, if the base is unselected it returns black
      // thats why theres the mysterious if !color.equals(black)
      Color color = base_colors[index];
      if ( ! color.equals (Color.black) ) {
        // it's selected - fill in the selected color
        graphics.setColor (color);
        Point box = ( ! reverseComplement ?
                      transformer.toPixel(new Point((int)i - 1, box_y)) :
                      transformer.toPixel(new Point((int)i, box_y)) );
        // ACGT dont have descents, so ascent is enough for height
        graphics.fillRect (box.x, box.y, box_width, height);
        graphics.setColor(Config.getSequenceColor());
      }
      Point start = ( ! reverseComplement ?
                      transformer.toPixel(new Point((int)i - 1, viewY)) :
                      transformer.toPixel(new Point((int)i, viewY)) );
      graphics.drawChars(bases, index, 1, start.x + xOffset, start.y);
    }
  }

  /** This now seems to be responsible only for drawing restriction enzyme
      cut sites, and is only called when we're zoomed in enough to
      (almost?) see bases (?). */
  private void drawFeatureBoxes (Graphics graphics, Transformer transformer,
                                 int startCoord, int endCoord,
                                 int viewY, Color [] base_colors) {
    int start_x = 0;
    int end_x = 0;
    Color feature_color = null;
    boolean in_feature = false;
    int y_pixel = transformer.toPixelY(viewY) - 10;

    for (int i = startCoord; i <= endCoord; i++) {
      int index = ( ! reverseComplement ?
                    (int) (i - startCoord) :
                    (int) (endCoord - i) );
      Color color = base_colors[index];

      if (! color.equals (Color.black)) {
        // now we are in a feature
        feature_color = color;
        if (!in_feature)
          start_x = (reverseComplement ? i : i - 1);
        end_x = (reverseComplement ? i : i - 1);
        in_feature = true;
      } else {
        // and now we are not in a feature
        if (in_feature) {
          int start_pixel = transformer.toPixelX(reverseComplement ? end_x : start_x);
          int end_pixel = transformer.toPixelX(reverseComplement ? start_x : end_x);
          // The same restriction site at the same zoom level
          // sometimes ends up with a width of 0, and sometimes a
          // width of 1, probably due to roundoff error.  
          // Round up to 1 if it's 0.
          int width = Math.max(Math.abs(start_pixel - end_pixel),1);
          graphics.setColor (feature_color);
          graphics.fillRect(start_pixel, y_pixel, width, 10);
        }
        in_feature = false;
      }
    }
    if (in_feature) {
      graphics.setColor (feature_color);
      int start_pixel = transformer.toPixelX(reverseComplement ? end_x : start_x);
      int end_pixel = transformer.toPixelX(reverseComplement ? start_x : end_x);
      // The same restriction site at the same zoom level
      // sometimes ends up with a width of 0, and sometimes a
      // width of 1, probably due to roundoff error.  
      // Round up to 1 if it's 0.
      int width = Math.max(Math.abs(start_pixel - end_pixel),1);
      graphics.fillRect(start_pixel, y_pixel, width, 10);
    }
  }

  /** A simpler (and much faster) method for drawing restriction sites
      when we're zoomed out.  (I didn't bother replacing drawFeatureBoxes
      for more zoomed-in situations because its inefficiency isn't
      so bad then.) --NH */
  private void drawSite(Graphics graphics, Transformer transformer,
                        int startCoord, int endCoord,
                        int y, int height, Color color) {
    int y_pixel = transformer.toPixelY(y) - height;

    int x_pixel = transformer.toPixelX(reverseComplement ? 
                                       startCoord : startCoord - 1);
    int width = calcWidth(x_pixel, endCoord);
    graphics.setColor (color);
    graphics.fillRect(x_pixel, y_pixel, width, height);
  }

  /** Draws the bases and selection background if in selected area */
  private void drawGaps (Graphics graphics, Transformer transformer,
                         int startCoord, int endCoord, 
                         String seq, int xOffset) {
    if (seq.indexOf ("N") >= 0) {
      char [] bases = seq.toCharArray();
      int top = scaleLineY - 2;
      int bottom = scaleLineY + 2;
      logger.trace("drawGaps startCoord=" + startCoord + " endCoord=" + endCoord + " bases.length=" + bases.length);

      for (int i = startCoord; i < endCoord; i++) {
        int index = ( ! reverseComplement ?
                      (int) (i - startCoord) :
                      (int) (endCoord - i) );

        if (bases[index] == 'N') {
          Point left;
          Point right;
          if ((i % 2) == 0) {
            left = ( ! reverseComplement ?
                     transformer.toPixel(new Point((int)i - 1, bottom)) :
                     transformer.toPixel(new Point((int)i, bottom)) );
            right = ( ! reverseComplement ?
                      transformer.toPixel(new Point((int)i, top)) :
                      transformer.toPixel(new Point((int)i + 1, top)) );
          } else {
            left = ( ! reverseComplement ?
                     transformer.toPixel(new Point((int)i - 1, top)) :
                     transformer.toPixel(new Point((int)i, top)) );
            right = ( ! reverseComplement ?
                      transformer.toPixel(new Point((int)i, bottom)) :
                      transformer.toPixel(new Point((int)i + 1, bottom)) );
          }
          graphics.setColor(Color.red);
          graphics.drawLine(left.x,left.y,right.x,right.y);
        }
      }
    }
  }

  private String getSequence(int startCoord, int endCoord, int strand) {
    String seq = null;
    if (curation == null) {
      return null;
    }
    boolean got_sequence 
      = (curation != null
         && curation.getRefSequence() != null
         && curation.getRefSequence().getResidues(startCoord,
                                                  endCoord) != null);
    if (got_sequence) {
      // Revcomping is whether whole view is revcomped, strand is for
      // to indicate that we are drawing the forward or reverse
      // strand
      // Getting sequence from end to start (high to low) will cause the
      // sequence to be revcomped
      seq  = ( ! reverseComplement ?
               curation.getRefSequence().getResidues(startCoord,
                                                     endCoord) :
               curation.getRefSequence().getResidues(endCoord,
                                                     startCoord) );
    }
    if (seq != null && seq.length() > 1) { // No sequence
      seq = seq.toUpperCase();
      
      // Complement the sequence if reverse strand of non-revcomped
      // OR forward strand of reversed comped (because it was comped above)
      if (strand == -1 && !reverseComplement ||
          strand == 1 && reverseComplement)
        seq = org.bdgp.util.DNAUtils.complement(seq);
    }
    else
      seq = null;
    return seq;
  }
      
  /**
     return an array of Colors for all of the bases in this region
  */
  private Color [] buildBaseColorArray (int startCoord,
                                        int endCoord,
                                        int strand) {
    Color [] base_colors = new Color [(endCoord - startCoord) + 2];
    Selection selection = ((ApolloPanelI) getComponent()).getSelection();
    PropertyScheme ps = Config.getPropertyScheme();
    boolean doneWarn = false;

    for (int base_position = startCoord;
         base_position <= endCoord; 
         base_position++) {
      SeqFeatureI selected = null;
      Color color = getBaseColor(base_position);
      for (int i = 0; i < selection.size() && selected == null; i++) {
        SeqFeatureI sf = selection.getSelectedData(i);
        selected = featureContains (sf, base_position);
        if (selected != null && (sf.getStrand() == strand ||
                                 sf.getStrand() == 0)) {
          String type = sf.getTopLevelType();
          color 
            = DrawableUtil.getFeatureColor(sf, ps.getFeatureProperty(type));
          if (color == Color.black && !doneWarn) {
            logger.warn("Color of feature is black - this won't show in ScaleView");                   
            doneWarn = true;
          }   

        }
      }
      int index = ( ! reverseComplement ?
                    (int) (base_position - startCoord) :
                    (int) (endCoord - base_position) );

      base_colors[index] = color;
    }
    return base_colors;
  }
      
  private Color getBaseColor(int base_position) {
    return (curation.getRefSequence().isSequencingErrorPosition(base_position)
            ?
            new Color(255, 215, 0) : Color.black);
  }

  /**
     return an array of Colors for all of the bases in this region
  */
  private Color [] buildSiteColorArray (int startCoord,
                                        int endCoord,
                                        int siteLow,
                                        int siteHigh,
                                        Color site_color) {
    Color [] base_colors = new Color [(endCoord - startCoord) + 2];

    for (int base_position = startCoord;
         base_position <= endCoord; 
         base_position++) {
      int index = ( ! reverseComplement ?
                    (int) (base_position - startCoord) :
                    (int) (endCoord - base_position) );

      base_colors[index] = ((base_position >= siteLow &&
                             base_position <= siteHigh) ?
                            site_color : Color.black);
    }
    return base_colors;
  }
      
  // Class specific methods

  // nothing to select - selection is figured during paint with getBaseColor
  public void select(Selection selection) {
  }

  public void showPopupMenu(MouseEvent evt) {
    JPopupMenu popup = new ScaleMenu(this,
                                     new Point(evt.getX(),evt.getY()));
    popup.show((Component)evt.getSource(),evt.getX(),evt.getY());
  }

  public Action getColouringAction() {
    return colouringManager.getAction();
  }

  /** Triggered by rubberbanding.  Find and remember region that was selected
   *  by rubberbanding at the axis.
   * This shouldn't trigger when we rubberband in some other view, yet it is.
   */
  public Selection findFeaturesForSelection(Rectangle rect) {
    Selection selection = new Selection(); // Do we really need to do this?
    // if the view is not visible then it cant be selected via rectangles that come
    // from the mouse dragging or clicking
    if (!isVisible()) return selection;

    // Figure out whether user's rubberband was above or below the axis
    // (if both, that counts as above).
    // If rubberband was nowhere near axis, assume that they weren't really
    // trying to rubberband the axis.
    int strand = positionRelativeToAxis(rect.y, rect.y+rect.height);
    if (strand == 0) {
      //      logger.error("ScaleView: axis not selected");
      return selection;
    }

    ViewI focus = ((ApolloPanelI) getComponent()).getFocus();
    int clickPos = rect.getLocation().x;
    int startBase, endBase;
    // +1 for startBase and endBase is to correct for one-based sequence numbering.
    // +2/-2 added to clickPos is because it was tending to overinclude bases
    // that you had only included a teeny slice of in your rectangle.
    // ,0 is because toUser wants a point, but the y position is not important here.
    startBase = focus.getTransform().toUser(clickPos+2, 0).x + 1;
    endBase = focus.getTransform().toUser((int)(clickPos-2 + rect.getWidth()),
                                          0).x + 1;

    // Ignore selections of less than two bases--most likely unintentional.
    if (Math.abs(endBase - startBase) < 2)
      return selection;

    StrandedZoomableApolloPanel szap 
      = ((ApolloPanelI) getComponent()).getStrandedZoomableApolloPanel();

    SequenceI refSeq = szap.getCurationSet().getRefSequence();
    // Get bases from the right strand.  May need to revcomp.
    String residues = "";
    if (strand == 1) {
      residues = refSeq.getResidues(startBase, endBase);
    }
    else if (strand == -1) {
      residues = refSeq.getResidues(endBase, startBase);
    }

    // Remember that this is the range that was selected, when it comes 
    // time to paint this View.
    setSelectedRange(startBase, endBase, strand);
    String revStrand = ((reverseComplement && strand == 1) || (!reverseComplement && strand == -1)) ? " (reverse strand)" : "";

    String seqName = (refSeq.getName() + revStrand +
                      " from " + startBase + " to " + endBase);
    // If selection is really big, don't bring it up in sequence viewer--
    // just return now.
    if (residues.length() > maxSeqSelection) {
      saveSeqAsFile(residues, seqName);
      return selection;
    }

    // For popping up sequence in sequence viewer
    Sequence seq = new Sequence("Sequence selection", residues);
    SeqFeature seqsel = new SeqFeature(startBase, endBase,
                                       "Sequence selection", strand);
    // Need setStart/setEnd for SeqExport, because it ends up doing 
    // getResidues(start,end)
    seqsel.setStart(0);
    seqsel.setEnd(residues.length());
    seqsel.setRefSequence(seq);
    seqsel.setName(seqName);

    // Pop up selected sequence in sequence viewer
    SelectionItem si = new SelectionItem(this,seqsel);
    selection.add(si);
    new SeqExport(selection, ((ApolloPanelI) getComponent()).getController());

    return selection;
  }

  // Returns +1 if y position is above axis; -1 if it's below; and 0 if it's
  // actually not anywhere near the axis (or it extends too far below the axis)
  private int positionRelativeToAxis(int top_y, int bottom_y) {
    // Accept rectangles that are not *entirely* within axis region
    int tolerance = 10;
    int topOfScale = getBounds().y;
    int bottomOfScale = getBounds().y + getBounds().height;
    if (top_y < topOfScale - tolerance) {
      return 0;
    }
    if ((top_y > bottomOfScale + tolerance) || 
        (bottom_y > bottomOfScale + tolerance)) {
      return 0;
    }

    Point axis_pos = transformer.toPixel(new Point(0, scaleLineY));
    int strand = 1;
    if (top_y >= axis_pos.y) {
      strand = -1;
    }
    return strand;
  }

  /** This is the version for when we're not zoomed enough to see bases */
  private void drawSites(Graphics graphics,
                         Transformer transformer,
                         int startCoord,
                         int endCoord,
                         int viewY,                        
                         int strand) {
    Enumeration colors = sites.keys();
    // Look at each restriction enzyme
    while (colors.hasMoreElements()) {
      Color color = (Color) colors.nextElement();
      Vector [] cuts_array = (Vector []) sites.get(color);
      Vector cut_sites = (strand == 1 ? cuts_array[0] : cuts_array[1]);
      int cut_count = cut_sites.size();
      for (int i = 0; i < cut_count; i++) {
        int [] zone = (int []) cut_sites.elementAt(i);
        if (zone[0] <= endCoord && zone[1] > startCoord) {
          drawSite(graphics, transformer,
                   zone[0], zone[1],
                   // 10 is height of line
                   viewY, 10, color);
        }
      }
    }
  }

  /** This is the version for when we *are* zoomed in enough to see bases */
  private void drawSites(Graphics graphics,
                         Transformer transformer,
                         int startCoord,
                         int endCoord,
                         String seq,
                         int offset,
                         int viewY,
                         int strand) {
    if (seq != null) {
      Enumeration colors = sites.keys();
      while (colors.hasMoreElements()) {
        Color color = (Color) colors.nextElement();
        Vector [] cuts_array = (Vector []) sites.get(color);
        Vector cut_sites = (strand == 1 ? cuts_array[0] : cuts_array[1]);
        int cut_count = cut_sites.size();
        for (int i = 0; i < cut_count; i++) {
          int [] zone = (int []) cut_sites.elementAt(i); 
          // see if it is visible first
          if (zone[0] <= endCoord && zone[1] > startCoord) {
            Color [] site_colors = buildSiteColorArray(startCoord, endCoord, 
                                                       zone[0], zone[1],
                                                       color);
            drawFeatureBases (graphics,
                              transformer,
                              startCoord,
                              endCoord,
                              seq,
                              offset,
                              viewY,
                              strand,
                              site_colors);
          }
        }
      }
    }
  }

  /** This is the version for when we're not zoomed enough to see bases */
  private void drawSequencingEdits(Graphics graphics,
                                   Transformer transformer,
                                   int startCoord,
                                   int endCoord) {
    // curation may get set to null mid draw - is it bad to kill draw?
    if (curation != null && curation.getRefSequence() != null) {
      HashMap edits = curation.getRefSequence().getGenomicErrors();
      if (edits != null) {
        Iterator positions = edits.keySet().iterator();
        while (positions.hasNext()) {
          String position = (String) positions.next();
          int pos = Integer.parseInt(position);
          // see if this is in the visible range
          if (pos <= endCoord && pos > startCoord) {
            int y_pixel = transformer.toPixelY(viewYtop - 6);
            int height = transformer.toPixelY(15);
            int x_pixel = transformer.toPixelX(reverseComplement ? 
                                               pos : pos - 1);
            int width = calcWidth(x_pixel, 
                                  reverseComplement ?
                                  pos + 1 : pos);
            //            graphics.setColor (new Color(255, 215, 0));
            graphics.setColor (Config.getStyle().getSeqErrorColor());
            graphics.fillRect(x_pixel, y_pixel, width, height);
          }
        }
      }
    }
  }

  /** Record start/end positions of range that has sequence */
  private void setRangeThatHasSequence() {
    if (curation != null && curation.getRefSequence() != null) {
      RangeI refSeqRange = curation.getRefSequence().getRange();
      startOfRange = refSeqRange.getLow();
      endOfRange = refSeqRange.getHigh();
    }
  }

  /** Draw lines at the axis (much like the sequencing edit lines) to
   * indicate the beginning and end of the range for which we have sequence. */
  private void drawOutOfRangeLines(Graphics graphics,
                                   Transformer transformer,
                                   int startCoord,
                                   int endCoord) {
    if (startOfRange > startCoord) {
      int y_pixel = transformer.toPixelY(viewYtop - 6);
      int height = transformer.toPixelY(10) - y_pixel + 1;
      int x_pixel = transformer.toPixelX(startOfRange - 1);
      int width = calcWidth(x_pixel, 
                            reverseComplement ?
                            startOfRange - 1 : startOfRange);
      graphics.setColor (Color.green);
      graphics.fillRect(x_pixel, y_pixel, width, height);
    }
    if (endOfRange < endCoord) {
      int y_pixel = transformer.toPixelY(viewYtop - 6);
      int height = transformer.toPixelY(10) - y_pixel + 1;
      int x_pixel = transformer.toPixelX(endOfRange);
      int width = calcWidth(x_pixel, 
                            reverseComplement ?
                            endOfRange : endOfRange+1);
      graphics.setColor (Color.red);
      graphics.fillRect(x_pixel, y_pixel, width, height);
    }
  }

  public Selection findFeaturesForSelection(Point p, boolean selectParents) {
    return findFeaturesForSelection(getSelectionRectangle(p));
  }

  private int calcWidth (int x_pixel, int endCoord) {
    int end_x = (reverseComplement ? endCoord : endCoord - 1);
    // Round up to 1 if it's 0.
    return (Math.max(Math.abs(x_pixel - 
                              transformer.toPixelX(end_x)),
                     1));
  }

  private void saveSeqAsFile (String residues, String description) {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Save selected sequence as a FASTA file");
    int returnVal = chooser.showSaveDialog(null);
    if(returnVal == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();
      if (file==null) { // file might be null
        return;
      }
      try {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(FastaFile.format(">" + description + "\n",residues,50));
        writer.close();
      } catch (IOException ex) {
        JOptionPane.showMessageDialog(null,"Couldn't write sequence to "+file);
      }
    }
  }

  class ScaleColouringManager {

    private int colouring = FEATURES;
    private ColouringAction action;

    public ScaleColouringManager() {
      action = new ColouringAction(getColouringMenuString());
      action.setEnabled(true);
    }

    public Action getAction() {
      return action;
    }

    public int getColouringMode() {
      return colouring;
    }

    private String getColouringMenuString() {
      if (colouring == FEATURES) {
        return "Color bases by splice site potential";
      } else {
        return "Color by selected feature";
      }
    }
    class ColouringAction extends AbstractAction {
      public ColouringAction(String name)  {
        super(name);
      }
      public void actionPerformed(ActionEvent evt) {
        if (colouring == FEATURES) {
          colouring = SPLICE;
        } else {
          colouring = FEATURES;
        }
        putValue(Action.NAME,getColouringMenuString());
        getComponent().repaint();
      }
    }
  }
}
