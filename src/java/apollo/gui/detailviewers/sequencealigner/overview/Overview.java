package apollo.gui.detailviewers.sequencealigner.overview;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.event.MouseInputAdapter;

import org.bdgp.swing.FastTranslatedGraphics;
import org.bdgp.util.RangeHash;

import apollo.datamodel.ExonI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceEdit;
import apollo.datamodel.SequenceI;
import apollo.datamodel.Transcript;
import apollo.gui.detailviewers.sequencealigner.MultiSequenceAlignerPanel;
import apollo.gui.detailviewers.sequencealigner.Orientation;
import apollo.gui.detailviewers.sequencealigner.ReadingFrame;
import apollo.gui.detailviewers.sequencealigner.SequenceType;
import apollo.gui.detailviewers.sequencealigner.Strand;
import apollo.gui.detailviewers.sequencealigner.TierPanel;
import apollo.gui.detailviewers.sequencealigner.TierPanelI;
import apollo.gui.detailviewers.sequencealigner.AAPanel.AAMultiSequenceAlignerPanel;
import apollo.gui.detailviewers.sequencealigner.filters.FilterFactory;
import apollo.gui.detailviewers.sequencealigner.filters.MultiFilter;
import apollo.gui.detailviewers.sequencealigner.filters.MultiFilterAnd;
import apollo.gui.drawable.DrawableUtil;
import apollo.util.interval.Interval;
import apollo.util.interval.IntervalTree;

/**
 * This draws the clickable transcript diagram at the bottom EDE
 */
public class Overview extends JComponent {

  //Color [][] colorList;
  
  
  private IntervalComponent top;
  //private TranscriptOverview mid;
  private IntervalComponent bottom;

  private MultiSequenceAlignerPanel editorPanel;
  private Transcript transcript;

  private int tier;
  private Strand strand;
  
  private boolean isForward = true;

  private RangeHash rh = new RangeHash();
  //int translationStart = -1;
  //int translationEnd = -1;
  private boolean drawIntrons = true;
  
  private int margin = 20; // horizontal? vertical? both?
  private int barHeight = 20;
  
  private int resultInflection = 25;

  private Color shiftColor = apollo.config.Config.getStyle().getSeqErrorColor();

  /**
   * Constructor
   * @param editorPanel
   */
  public Overview(MultiSequenceAlignerPanel editorPanel) {
    this.editorPanel = editorPanel;
    //colorList = editorPanel.getColorArray();
    this.top = new IntervalComponent();
    this.bottom = new IntervalComponent();
    this.strand = editorPanel.getStrand();
    setPreferredSize(new Dimension(300,barHeight+margin*2));// does this make sense?
    setForeground(Color.white);
    NavClickListener listener = new NavClickListener();
    addMouseListener(listener);
    addMouseMotionListener(listener);
  }

  /**
   * Determine whether or not to draw the introns in the overview
   * @param drawIntrons should be true if introns should be drawn false othewise
   * 
   * NOTE: not sure if this is still working
   */
  public void setDrawIntrons(boolean drawIntrons) {
    this.drawIntrons = drawIntrons;
    repaint();
  }

  /**
   * Gets the current value of drawIntrons
   * @return
   */
  public boolean getDrawIntrons() {
    return drawIntrons;
  }


  /**
   * Sets the new transcript as the main feature in the overview. 
   * @param transcript
   * 
   * Calls updateState(), repaint()
   */
  public void setTranscript(Transcript transcript) {
    if (transcript != null && transcript != this.transcript) {
      this.transcript = transcript;
      this.strand = Strand.valueOf(transcript.getStrand());
      updateState();
      this.repaint();
    }
    /* The exons should already be sorted */
    //this.tier = tier;
  }
  
  public Transcript getTranscript() {
    return this.transcript;
  }
  
  /**
   * Updates the two result components to reflect the current state of the
   * overview
   */
  public void updateState() {
    
    Orientation o = 
      editorPanel.getAnnotationPanel().getOrientation();
    isForward = o == Orientation.FIVE_TO_THREE;

    if (Strand.valueOf(transcript.getStrand()) == Strand.REVERSE)
      isForward = !isForward;
    
    if (Strand.valueOf(transcript.getStrand()) != editorPanel.getStrand())
      isForward = !isForward;
    
    
    
    updateResult(top, Strand.FORWARD);
    updateResult(bottom, Strand.REVERSE);
    
  }
  
  /**
   * Updates an interval component so that it displays the results within
   * the bounds of the currently selected transcirpt on a particular strand.
   * 
   * @param result the component to be updated
   * @param strand the strand from which the results should be selected
   */
  public void updateResult(IntervalComponent result, Strand strand) {
    
    if (isForward) {
      result.setForward();
    } else {
      result.setReverse();
    }
    
    MultiFilter<SeqFeatureI> filters = new MultiFilterAnd();
    filters.addAll(editorPanel.getResultFilter());
    filters.add(
        FilterFactory.makeFilter(SequenceType.valueOf(editorPanel.getType())));
    filters.add(FilterFactory.makeFilter(strand));
    
    SeqFeatureI resultSet = this.editorPanel.getCurationState().getCurationSet()
    .getResults().getFeatSetForStrand(strand.toInt());
    
    Set<SeqFeatureI> results = MultiSequenceAlignerPanel.getResults(resultSet, filters);
    
    /*
    Set<SeqFeatureI> results =
      MultiSequenceAlignerPanel.getResults(editorPanel.getType(), resultSet);
    */
    
    result.setLength(transcript.getHigh() - transcript.getLow());
    result.clear();
    
    // NOTE: this probably produces too many points of interest, can probably
    // be done better
    TreeSet<Integer> poi = new TreeSet<Integer>(); // points of interest
    IntervalTree intervals = new IntervalTree();
    int offset = transcript.getLow();
    for (SeqFeatureI feature : results) {
      if (transcript.getLow()  <= feature.getHigh() &&
          transcript.getHigh() >= feature.getLow()) {
        int low = Math.max(0, feature.getLow() - offset);
        int high = Math.min(transcript.getHigh() - offset, feature.getHigh() - offset) - 1;
        Interval i = new Interval(low, high);
        intervals.insert(i);
        poi.add(low);
        poi.add(high);
        poi.add(high+1);
      }
    }
    
    Iterator<Integer> iter = poi.iterator();
    Integer low = null;
    Integer high = null;
    if (iter.hasNext()) {
      low = iter.next();
    }
    while (iter.hasNext()) {
      high = iter.next();
      Interval i = new Interval(low, high-1);
      List<Interval> overlaps = intervals.searchAll(i);
      int size = overlaps.size();
      int height = (int) Math.min(size, .6*margin);
      
      
      
      if (size > resultInflection) {
        height += (int) Math.log(size-resultInflection);
      }
      
      result.putInterval(new Interval(low, high), height);
      low = high;
    }
    
  }
  
  
  public void validate() {
    Dimension size = this.getSize();
    Dimension third = new Dimension(size.width - 2*margin, size.height/3);
    
    top.setPreferredSize(third);
    top.setSize(third);
    
    bottom.setPreferredSize(third);
    bottom.setSize(third);
    
    super.validate();
  }

  // I think this gets repainted every time the window moves...
  public void paint(Graphics g) {
    super.paint(g);
    
    // todo... create a exon component that will paint its self
    // then create a transcript component that is made up of exons
    // use those to create draw this...
    // mabe i can use some the code that draws the transcripts on the main
    // apollo window?
    g.setFont(getFont());
    if (transcript == null)
      return;
    
    rh.removeAll();

    // Translation start range
    int translationStart = transcript.getTranslationStart();
    int laststart_base = transcript.getPositionFrom(translationStart, 2);
    // Translation end range
    int translationEnd = transcript.getTranslationEnd();
    int laststop_base = transcript.getLastBaseOfStopCodon();
    
    // What are these?
    int readthrough_pos = transcript.readThroughStopPosition();
    int lastreadon_base = transcript.getPositionFrom(readthrough_pos, 2);

    int topDistance = (getSize().height - barHeight) / 2;
    java.util.Vector exons = transcript.getExons();
    int exon_count = exons.size();
    int featureWidth;
    
    // if we want to see the introns then set featureWidth to the transcript
    // length and draw a rectangle.
    if (drawIntrons) {
      featureWidth = (int) transcript.length();

      // Get the indicator color for the panel holding this overview
      g.setColor(editorPanel.getIndicatorColor());

      // Draw the line in the middle starting from the left margin
      g.fillRect(margin,
                 (getSize().height - 2) / 2,
                 getSize().width - margin*2,// - 1,
                 3);
      
    } else {
      featureWidth = 0;
      for(int i=0; i < exon_count; i++) {
        SeqFeatureI feature = (SeqFeatureI) exons.elementAt(i);
        featureWidth += (int) feature.length();
      }
    }

    double scaling_factor = ((double) featureWidth /
                             (double) (getSize().width - margin*2));

    int x = margin;
    // Loop through exons
    for(int i=0; i < exon_count; i++) {
      ExonI exon = null;
      if (isForward) {
        exon = transcript.getStrand() == 1 ? 
            transcript.getExonAt(i) :
            transcript.getExonAt(exon_count-i-1);
      } else {
        exon = transcript.getStrand() == 1 ?
            transcript.getExonAt(exon_count-i-1) :
            transcript.getExonAt(i);
      }
      int low = (int) exon.getLow();
      int high = (int) exon.getHigh();

      //int width = (int) Math.floor(exon.length() / scaling_factor);
      int width = (int) Math.round(exon.length() / scaling_factor);
      if (width < 1)
        width = 1;

     // int transcriptColorIndex = (editorPanel.getRangeIndex(tier,
    //                              lowPos,
    //                              highPos)
    //                              % colorList.length);

    //  int exonColorIndex = (editorPanel.getExonRangeIndex(tier,
    //                        lowPos,
    //                        highPos)
    //                        % colorList[transcriptColorIndex].length);

     // g.setColor(colorList[transcriptColorIndex][exonColorIndex]);
      g.setColor(Color.blue); //TODO: change?
      
      int offset = 0;
      if (isForward) {
        offset = transcript.getStrand() == 1 ?
            (int) Math.round(
              Math.abs(transcript.getStart()-exon.getStart())/scaling_factor) :
            (int) Math.round(
              Math.abs(transcript.getEnd()-exon.getEnd())/scaling_factor);
      } else {
        offset = transcript.getStrand() == 1 ?
            (int) Math.round(
                Math.abs(transcript.getEnd()-exon.getEnd())/scaling_factor) :
            (int) Math.round(
              Math.abs(transcript.getStart()-exon.getStart())/scaling_factor);
      }
      
      x = margin+offset;
      rh.put(x, x+width-1, exon);

      g.fillRect(x,
                 topDistance,
                 width,
                 barHeight+1);
      
      // paint the codons
      paintCodon (g,
                  exon, 
                  translationStart, laststart_base, 
                  low, high,
                  DrawableUtil.getStartCodonColor(transcript),
                  x, topDistance, scaling_factor);

      paintCodon (g,
                  exon, 
                  translationEnd, laststop_base, 
                  low, high,
                  Color.red,
                  x, topDistance, scaling_factor);

      paintCodon (g,
                  exon, 
                  readthrough_pos, lastreadon_base, 
                  low, high,
                  Color.pink,
                  x, topDistance, scaling_factor);

      // If exon is coding draw in frame number - what is the first frame?X
      paintFrame(g, exon, low, high, translationStart, laststop_base,
                 x, topDistance, scaling_factor);

    } // end of exon loop
    

    FastTranslatedGraphics transG = new FastTranslatedGraphics(g);
    transG.translateAbsolute(margin, 0);
    top.paint(transG);
    
    transG.translateAbsolute(margin, 2*bottom.getSize().height);
    bottom.paint(transG);
    
    int baseStart = editorPanel.getVisibleBase();
    int baseCount = editorPanel.getVisibleBaseCount();
    /*
    TierPanelI tierPanel =
      editorPanel.getAnnotationPanel().getModelTierPanel();
  */

    
    int baseOffset = 0; 
      
    if (isForward) {
      baseOffset = transcript.getStrand() == 1 ?
          baseStart - transcript.getStart() :
          baseStart - transcript.getEnd();
    } else {
      baseOffset = transcript.getStrand() == 1 ?
          transcript.getEnd() - baseStart :
          transcript.getStart() - baseStart;
    }
    
    int pixelOffset = (int) ((double) baseOffset / scaling_factor);
    
    int basePixelStart = margin+pixelOffset;
    int basePixelCount = (int) ((double) baseCount /
                                (double) scaling_factor);

    //    g.setColor(Color.yellow);
    // Box that goes over exon diagram at the bottom to show which region
    // you're seeing in the detailed view
    g.setColor(Color.black); //TODO config
    g.drawRect(basePixelStart, topDistance-4,
               basePixelCount, barHeight+8);
    // To make a two-pixel-wide rectangle, draw a one-pixel-bigger rectangle
    // around the first one
    g.drawRect(basePixelStart-1, topDistance-5,
               basePixelCount+2, barHeight+10);
    
  }

  private void paintCodon (Graphics g,
                           ExonI exon, 
                           int first_base, 
                           int last_base,
                           int low,
                           int high,
                           Color codon_color,
                           int x, 
                           int topDistance,
                           double scaling_factor) {
    int expected_last = first_base + (2 * exon.getStrand());
    if (first_base > 0) {
      if (first_base >= low && first_base <= high) {
        g.setColor(codon_color);
        // account for stop codons that span exon boundaries
        int codon_width = (last_base != expected_last ?
                           Math.abs(exon.getEnd() - first_base) + 1 : 3);
        int offset =  0;
        
        if (isForward) {
          offset = exon.getStrand() == 1 ?
              (int) (Math.abs(exon.getStart() - first_base) /scaling_factor) :
              (int) (Math.abs(exon.getEnd() - first_base) /scaling_factor);
        } else {
          offset = exon.getStrand() == 1 ?
              (int) (Math.abs(exon.getEnd() - first_base) /scaling_factor) :
              (int) (Math.abs(exon.getStart() - first_base) /scaling_factor);
        }
        int rect_width = (int) (((double) codon_width) / scaling_factor);
        if (rect_width < 1)
          rect_width = 1;
        g.fillRect(x + offset,
                   topDistance,
                   rect_width,
                   barHeight+1);
      }
      
      if (last_base != expected_last &&
          last_base >= low &&
          last_base <= high) {
        g.setColor(codon_color);
        // account for stop codons that span exon boundaries
        int codon_width = Math.abs(last_base - exon.getStart()) + 1;
        int rect_width = (int) (((double) codon_width) / scaling_factor);
        if (rect_width < 1)
          rect_width = 1;
        g.fillRect(x,
                   topDistance,
                   rect_width,
                   barHeight+1);
      }
    }
  }
  
  private void paintBase (Graphics g,
                          ExonI exon, 
                          int base_pos, 
                          int low,
                          int high,
                          Color base_color,
                          int x, 
                          int topDistance,
                          double scaling_factor) {
    if (base_pos > 0) {
      if (base_pos >= low && base_pos <= high) {
        g.setColor(base_color);
        // account for stop codons that span exon boundaries
        int offset = (int) (Math.abs(exon.getStart() - base_pos) /
                            scaling_factor);
        int rect_width = (int) (((double) 1) / scaling_factor);
        if (rect_width < 1)
          rect_width = 1;
        g.fillRect(x + offset,
                   topDistance,
                   rect_width,
                   barHeight+1);
      }
    }
  }

  //draws the frame number for this exon?
  private void paintFrame(Graphics g, ExonI exon, 
                          int low, int high, 
                          int translationStart, int laststop_base,
                          int exon_x,
                          int topDistance, double scaling_factor) {

    boolean coding = exon.containsCoding();
    int frame = exon.getFrame();
    FontMetrics metrics = getFontMetrics(getFont());
    SequenceEdit [] edit_list = exon.buildEditList();
    int edits = (edit_list != null ? edit_list.length : 0);
    int x = exon_x;
    int prev_pos;
    if (exon.contains(translationStart)) {
      prev_pos = translationStart;
      int length = Math.abs(exon.getStart() - prev_pos) + 1;
      int width = (int) Math.floor(length / scaling_factor);
      if (width < 1)
        width = 1;
      x += width;
    } else {
      prev_pos = exon.getStart();
      x = exon_x;
    }
    
    int last_pos = (exon.contains(laststop_base) ? 
                    laststop_base : exon.getEnd());

    for (int i = 0; i < edits; i++) {
      SequenceEdit edit = edit_list[i];
      int pos = edit.getPosition();
      paintBase(g,
                exon,
                pos, low, high,
                shiftColor,  // was Color.yellow
                exon_x, topDistance, scaling_factor);
      if (coding) {
        g.setColor(getForeground());
        String drawMe = ""+(frame);
        int strWidth = metrics.stringWidth(drawMe);
        int length = Math.abs(pos - prev_pos) + 1;
        int width = (int) Math.floor(length / scaling_factor);
        if (width < 1)
          width = 1;
        int fontx = x + width / 2 - (strWidth / 2);
        int fonty = topDistance + barHeight / 2 + getFont().getSize() / 2;

        g.drawString(drawMe, fontx, fonty);
        
        x += width;
        prev_pos = pos;
        if (edit.getEditType().equals(SequenceI.DELETION)) {
          frame = (frame == 3 ? 1 : frame - 1);
        }
        if (edit.getEditType().equals(SequenceI.INSERTION)) {
          frame = (frame == 1 ? 3 : frame + 1);
        }
      }
    }
    if (coding) {
      g.setColor(getForeground());
      String drawMe = ""+(frame);
      int strWidth = metrics.stringWidth(drawMe);
      int length = Math.abs(last_pos - prev_pos) + 1;
      int width = (int) Math.floor(length / scaling_factor);
      if (width < 1)
        width = 1;
      int fontx = x + width / 2 - (strWidth / 2);
        int fonty = topDistance + barHeight / 2 + getFont().getSize() / 2;
        g.drawString(drawMe, fontx, fonty);
        x += width;
      }
  }
  
  
  private class NavClickListener extends MouseInputAdapter {

    public void mouseMoved(MouseEvent e) {}

    public void mouseClicked(MouseEvent e) {
      if (transcript != null) {
        double [] range = rh.getInterval(e.getX());
        SeqFeatureI feature = (SeqFeatureI) rh.get(e.getX());
        int basepair;
        
       //if (range == null || feature == null) {
          double offset = e.getX() - margin;
          double scale = ((double) transcript.length()) /
            ((double) getSize().width - margin*2);
          
          basepair = 0;
          if (isForward) {
            basepair = transcript.getStrand() == 1 ?
                (int) (transcript.getStart() +
                       (transcript.getStrand() * offset * scale)) :
                (int) (transcript.getEnd() -
                       (transcript.getStrand() * offset * scale));
          } else  {
            basepair = transcript.getStrand() == 1 ?
                (int) (transcript.getEnd() -
                       (transcript.getStrand() * offset * scale)) :
                (int) (transcript.getStart() +
                        (transcript.getStrand() * offset * scale));
          }
        editorPanel.scrollToBase(basepair);
        
        // Switch to the frame of the exon that is being scrolled to
        if (editorPanel.getType() == SequenceI.AA && 
            editorPanel instanceof AAMultiSequenceAlignerPanel) {
          AAMultiSequenceAlignerPanel panel = 
            (AAMultiSequenceAlignerPanel) editorPanel;
          Vector exons = transcript.getExons();
          Iterator iter = exons.iterator();
          while (iter.hasNext()) {
            ExonI exon = (ExonI) iter.next();
            if (exon.getLow() <= basepair && exon.getHigh() >= basepair &&
                ReadingFrame.valueOf(exon.getFrame()) != ReadingFrame.NONE &&
                ReadingFrame.valueOf(exon.getFrame()) != panel.getFrame()) {
              panel.setFrame(ReadingFrame.valueOf(exon.getFrame())); 
              panel.repaint();
            }
          }
        }
        
      }
    }
  }

}
