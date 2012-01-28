package apollo.gui.detailviewers.sequencealigner.overview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.JFrame;

import apollo.datamodel.AnnotatedFeatureI;
import apollo.gui.detailviewers.sequencealigner.Orientation;
import apollo.gui.detailviewers.sequencealigner.Strand;
import apollo.util.interval.Interval;

/**
 * This is the class which draws intervals.
 * Assumes intervals can be uniquely identified by their start position.
 */
public class IntervalComponent extends JComponent {
  
  private static final long serialVersionUID = -8127066813604422951L;
  
  public enum Valignment { TOP, BOTTOM, CENTER };
  
  private enum Directions { FORWARD, REVERSE };
  
  private TreeMap<Interval, Integer> intervalHeight;
  private HashMap<Interval, Color> colors;
  private Directions direction;
  private int length;
  private Valignment vAlignment;
  private Color primaryColor;

  /**
   * Empty Constructor
   */
  public IntervalComponent() {
    intervalHeight = new TreeMap<Interval, Integer>();
    colors = new HashMap<Interval, Color>();
    direction = Directions.FORWARD;
    length = 0;
    vAlignment = Valignment.CENTER;
    primaryColor = Color.cyan;
  }
  
  public void clear() {
    intervalHeight.clear();
    colors.clear();
  }
  
  /**
   * Paints the overview for this result set.
   */
  public void paint(Graphics g) {
    super.paint(g);
    Set<Interval> intervals = intervalHeight.keySet();
    
    double scalingFactor = (double) length / (double) this.getSize().width;
    
    for (Interval i : intervals){
      
      int height = calculateHeight(i);
      int width = calculateWidth(i, scalingFactor);
      
      int y = calculateVerticalPosition(i);
      int x = calculateHorizontalPosition(i, scalingFactor);
      
      if (height > 0) {
        g.setColor(getColor(i));
        g.fillRect(x, y, width, height);
      }
      
      x += width;
    }
  }
  
  /**
   * Associates the specified height with the specified Interval in this
   * component. If this component previously contained a mapping for the given
   * Interval, the old height is replaced. 
   * 
   * @param i the interval to be added
   * @param height the height of the given interval
   * @return previous height associated with specified Interval, or null 
   * if there was no mapping for this Interval.
   */
  public Integer putInterval(Interval i, int height) {
    return intervalHeight.put(i, new Integer(height));
  }
  
  /**
   * Removes the mapping for this Interval from this component if present. 
   * @param i the interval to be removed.
   * @return previous count associated with specified Interval, or null 
   * if there was no mapping for this Interval.
   */
  public Integer remove(Interval i) {
    return intervalHeight.remove(i);
  }
   
  /** true if the component will be rendered forwards false otherwise */
  public boolean isForward() {
    return direction == Directions.FORWARD;
  }
  
  /** Sets the direction of this component to be forward */
  public void setForward() {
    direction = Directions.FORWARD;
  }
  
  /** Sets the direction of this component to be reverse */
  public void setReverse() {
    direction = Directions.REVERSE;
  }
  
  public void switchDirection() {
    if (direction == Directions.FORWARD)
      direction = Directions.REVERSE;
    else 
      direction = Directions.FORWARD;
  }
  
  /** Gets the length of this component */
  public double getLength() {
    return length;
  }
  
  /** Sets the scaling factor for this component */
  public void setLength(int l) {
    length = l;
  }
  
  public Color getColor(Interval i) {
    Color result = colors.get(i);
    if (result == null) {
      result = primaryColor;
    }
    return result;
  }

  public void setColors(Interval i, Color c) {
    colors.put(i, c);
  }

  public Color getPrimaryColor() {
    return primaryColor;
  }

  public void setPrimaryColor(Color primaryColor) {
    this.primaryColor = primaryColor;
  }
  
  /** Private class used to compare intervals.
   * Assumes intervals can be uniquely identified by their start position. */
  /*
  private class IntervalComparator implements Comparator<Interval> {
    public int compare(Interval i0, Interval i1) {
      return i0.getStart() - i1.getStart();
    }
  }*/
  
  /**
   * Calculates the pixel height for a given Interval
   * @param i the interval
   * @return the pixel height for the given Interval
   */
  private int calculateHeight(Interval i) {
    int result = intervalHeight.get(i).intValue();
    
    if (result < 0) {
      result = 0;
    }
    
    if (result > this.getSize().height) {
      result = this.getSize().height;
    }
    
    return result;
  }
  
  /**
   * Calculates the pixel width for the given interval
   * @param i the interval
   * @param sf the scaling factor for this interval
   * @return the pixel width for the given interval
   */
  private int calculateWidth(Interval i, double sf) {
    int length = i.getHigh() - i.getLow();
    int width = (int) Math.floor((double)length / (double) sf);
    if (width < 1)
      width = 1;
    return width;
  }
  
  private int calculateVerticalPosition(Interval i){
    // TOP
    int result = 0;
    int height = this.calculateHeight(i);
    
    if (vAlignment == Valignment.CENTER) {
      result = (this.getHeight() - height) / 2;
    }
    
    if (vAlignment == Valignment.BOTTOM) {
      result = this.getHeight() - height;
    }
    
    return result;
  }
  
  /**
   * Calculates the pixel position for the start given an interval.
   * @param i the interval
   * @param sf the scaling factor for this interval
   * @return the pixel position for the start
   */
  private int calculateHorizontalPosition(Interval i, double sf) {
    int result = i.getLow();
    
    if (direction == Directions.REVERSE) {
      result = length - i.getHigh();
    }
    
    result = (int) Math.floor((double)result/ (double) sf);
    
    return result;
  }
  
  public static void main(String [ ] args) {
    Dimension d = new Dimension(500, 300);
    int length = 200;
    
    JFrame f = new JFrame("TopLevelDemo");
    IntervalComponent o = new IntervalComponent();
    o.setPreferredSize(d);
    o.setSize(d);
    o.setLength(length);
    o.setVisible(true);
    
    for (int x = 0; x < 150; x += 20) {
      Interval i = new Interval(x, x+20);
      if (x%40 == 0)
        o.putInterval(i, 20);
     // else 
        //o.putInterval(i, 2);
    }
    
    f.add(o);
    
    f.setPreferredSize(d);
    f.pack();
    f.setVisible(true);

  }

}
