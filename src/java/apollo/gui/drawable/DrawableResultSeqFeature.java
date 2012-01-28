package apollo.gui.drawable;

import java.awt.*;
import java.util.Date;

import apollo.config.FeatureProperty;
import apollo.datamodel.*;
import apollo.config.Config;
import apollo.gui.TierManagerI;
import apollo.gui.SelectableI;
import apollo.gui.Transformer;

/**
 * A drawable for drawing result (computational analysis) spans.
 */
public class DrawableResultSeqFeature extends DrawableSeqFeature
  implements Drawable,
  SelectableI {

  // protected static final int hatch_width = 4;

  public DrawableResultSeqFeature() {
    super(true);
  }

  public DrawableResultSeqFeature(SeqFeatureI feature) {
    super(feature, true);
  }

  protected void addDecorations(Graphics g, Rectangle boxBounds,
                             Transformer transformer,
                             TierManagerI manager) {
    super.addDecorations(g,boxBounds,transformer,manager);

    // Draw box around "new" sequences
    // !! Is it too expensive to do all this here?  
    // Is there a better place for it?
    // Maybe move to DrawableResultFeatureSet?
    Date recentDate = getFeatureProperty().getRecentDate();
    // If this feature type doesn't have a date, we can stop now...
    if ((recentDate != null)
  && (getFeature() instanceof FeaturePair)) {
      FeaturePair fp = (FeaturePair)getFeature();
      SeqFeatureI hit = fp.getHitFeature();
      SequenceI seq = hit.getRefSequence();
      /* Is this cast to AbstractSequence ever going to be a problem?
         I don't want to declare isNewerThan in SequenceI because then 
         all these other classes have to define it even though they 
         won't use it. */
      if (seq != null && ((AbstractSequence)seq).isNewerThan(recentDate)) {
        /* Using the edgematch color, draw a box around the feature that's 
           a hit to a new sequence */
        g.setColor(Config.getEdgematchColor());
        g.drawRect(boxBounds.x-2,boxBounds.y-2,
                   boxBounds.width+3,boxBounds.height+3);
      }
    }

    // Experiment: overlay tagged results with translucent diagonal line
    if (tagged()) {
      Color taggedColor = Config.getStyle().getTaggedColor();
      g.setColor(taggedColor);
      int zagInterval;
      // make them as square as possible
      int hatch_width = boxBounds.height;
      if (boxBounds.width <= hatch_width) {
        zagInterval = boxBounds.width;
      }
      else {
        // lets assume for the most part that we won't have a prime#
        zagInterval = hatch_width;
        int adjust = boxBounds.width % zagInterval;
        if (adjust > (zagInterval * 0.5)) {
          int zig_count = boxBounds.width / zagInterval;
          zagInterval = (int) ((double) (boxBounds.width) / 
                               (double) zig_count + 1);
        }
      }

      int x_end = boxBounds.x + boxBounds.width;
      int y_start = boxBounds.y;
      int y_end = boxBounds.y + boxBounds.height;

      for (int zig_Xstart = boxBounds.x;
           zig_Xstart < x_end; 
           zig_Xstart += zagInterval) {
        int zig_Xend = zig_Xstart + zagInterval;
        if (zig_Xend > x_end) 
          zig_Xend = x_end;
        g.drawLine(zig_Xstart, y_start, zig_Xend, y_end);
        g.drawLine(zig_Xstart, y_end, zig_Xend, y_start);
      }
    }

    // show sequence bases inside result
    if (transformer.getXPixelsPerCoord() >= 5) {
      if (getFeature() instanceof FeaturePair) {
        FeaturePair fp = (FeaturePair)getFeature();
        SeqFeatureI hit = fp.getHitFeature();

        /* getHitAlignment is the proper way to access the alignment - 
           it uses the alignment property and if not that, the cigar
           peptide alignments come with spacing */
        String align_seq = fp.getHitFeature().getAlignment();//getHitAlignment();
        if (align_seq == null || align_seq.equals("")) {
          if (hit.getRefSequence() != null && hit.getLow() >= 0) {
            // why getRefSequence? why not getFeatureSequence?
            align_seq = hit.getRefSequence().getResidues((int)hit.getLow(),
               (int)hit.getHigh());
          }
        }

        if (align_seq != null && !align_seq.equals ("")) {
          drawAlignSeq (g,
                        transformer,
                        fp,
                        align_seq,
                        boxBounds);
        }
      }
    }
  }

  /** Draw the alignment sequence in the feature */
  private void drawAlignSeq(Graphics g,
                            Transformer transformer,
                            FeaturePair fp,
                            String align_seq,
                            Rectangle box) {
    // draw aligned sequence
    SeqFeatureI result = fp.getRefFeature();

    int leftCoord = getLow();
    int rightCoord = getHigh();

    // these are the coordinates that are currently visible in the view
    // user coordinates(base pairs)
    int [] visRange = transformer.getXVisibleRange(); 
    // seq_screen_start is where in the sequence does it become seen, whats the
    // offest due to the screen/frame chopping it off on the left side
    int seq_screen_start=0;
    if (leftCoord < visRange[0]) {
      // if feature is cut off on left, set seq offset accordingly
      // just forward strand?
      seq_screen_start = (int)visRange[0] - (int)leftCoord;
      leftCoord = visRange[0];
      // if its protein make sure index is set at the beginning of codon
      // left for forward, right for reverse
      if (isProtein(result)) {
        if (isForwardStrand())
          leftCoord -= seq_screen_start % 3;
        else
          leftCoord -= ((seq_screen_start % 3) + 1);
      }
    } else {
      // if reverse strand protein with left end not cut off hafta shift by 2
      // to get the beginning of the codon in reverse direction
      if (isProtein(result) && !isForwardStrand())
        leftCoord += 2;
    }
    int seq_screen_stop = 0; // sequence offset due to screen cutoff on right
    if (rightCoord > visRange[1]) {
      seq_screen_stop = (int)rightCoord - (int)visRange[1];
      rightCoord = visRange[1];
    }

    FontMetrics fm = g.getFontMetrics();
    int res_y = getCharY (fm, box); // y coordinate for text

    int coord_increment = 1; // increment 1 for dna, 3 for protein
    // Peptide alignments are now retrieved pre-spaced with getHitAlignment
    //if (isProtein(result))  coord_increment = 3;

    // whether origin on left or right - right for reverse complement
    boolean forward = transformer.getXOrientation() == Transformer.LEFT;

    // Use a font color chosen to contrast with the feature color
    Color fontColor = fontColorForBackground(getFeatureProperty().getColour());
    g.setColor(fontColor);

    for (int i = leftCoord; i <= rightCoord; i += coord_increment) {
      // index is index on the sequence in bp or aa coords
      int index = ( isForwardStrand() ?
                    (int) (i - leftCoord + seq_screen_start) :
                    (int) (rightCoord - i + seq_screen_stop) ) / coord_increment;

      String res;
      if ((index + 1) < align_seq.length())
        res = align_seq.substring(index, index + 1);
      else if (index < align_seq.length())
        res = align_seq.substring(index);
      else
        res = " ";

      int offset = (int) ((transformer.getXPixelsPerCoord() / 2) -
                          (fm.stringWidth(res) / 2));
      int res_x = ( forward ?
                    transformer.toPixelX ( (int) i - 1 ) :
                    transformer.toPixelX ( (int) i ) );
      res_x += offset;
      g.drawString(res, res_x, res_y);
    }
  }

  /** Figure from result program name(blastx,blastp)
      or group property tier label(Protein) if its a protein 
      This needs to be more configurable. A new user cant presently plop there 
      new type in thats protein and have apollo recognize it as such unless it has
      blastx or blastp in its program name of Protein in its tier label.
  */
  private boolean isProtein(SeqFeatureI result) {
    // used to use ref feature to get program name, when that happens
    // now for sptr the ref feature returns the strandedFeatureSet which has
    // no program at first - has something changed? this used to work
    // leaving in comment to show how it was
    SeqFeatureI analysis = result;
    String program=null;
    if (analysis != null)
      program = analysis.getProgramName();
    if (program != null) {
      program = program.toLowerCase();
      if (program.indexOf("blastp") >= 0 || program.indexOf("blastx") >= 0)
        return true;
    }
    // group property inherited from DrawableSeqFeature
    FeatureProperty fp = getFeatureProperty();
    if (fp.getTier().getLabel().indexOf("Protein") >= 0)
      return true;
    return fp.getTier().isProtein();
  }

  /** In GAME XML, seq features are tagged at the result set level.
      In Chado XML, they are tagged at the seq feature level (below result set)
      but copied up to the parent result set. */
  private boolean tagged() {
    // Check parent
    String tag = getFeature().getRefFeature().getProperty("tag");
    return (tag != null && !(tag.equals("")));
  }
}
