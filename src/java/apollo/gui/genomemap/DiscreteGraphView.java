package apollo.gui.genomemap;

import java.awt.Point;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.PopupFactory;
import javax.swing.Popup;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JColorChooser;

import apollo.gui.Transformer;
import apollo.gui.event.MouseButtonEvent;

import java.util.List;
import java.util.LinkedList;

/** This is a view for a discrete graph
 * 
 * @author elee
 *
 */
public class DiscreteGraphView extends GraphView {

  //instance variables
  private Transformer scaler;
  private MouseMotionListener mouseMotionListener;
  private MouseListener mouseListener;

  /** Construct a new DiscreteGraphView object
   * 
   * @param ap - ApolloPanel to display graph on
   * @param name - name of graph
   * @param plotColour - color for the graph (axis will be that color, peaks will be darker)
   * @param scaler - Transformer for the display's scale to be used for centering peaks at tick marks
   */
  public DiscreteGraphView(JComponent ap, String name, Color plotColour,
      Transformer scaler) {
    super(ap, name);
    this.plotColour = plotColour;
    this.scaler = scaler;
    mouseMotionListener = new DiscreteGraphMouseMotionListener();
    mouseListener = new DiscreteGraphMouseListener();
    
    ap.addMouseMotionListener(mouseMotionListener);
    ap.addMouseListener(mouseListener);
  }

  /** Paints the graph
   * 
   */
  public void paintView() {
    //since drawing is multithreaded, the component can be requested to be drawn
    //before the graphics object is set - in that case just exit drawing
    if (graphics == null) {
      return;
    }
    graphics.setColor(getBackgroundColour());

    // Clear the whole view
    if (!transparent) {
      graphics.fillRect(getBounds().x, getBounds().y, getBounds().width,
          getBounds().height);
    } else {
      graphics.drawRect(getBounds().x, getBounds().y, getBounds().width,
          getBounds().height);
    }

    // if paint happens mid load with no curation set or calculator
    if (calculator == null) {
      return;
    }

    //no data to display
    if (calculator.getXRange()[0] == 1 && calculator.getXRange()[1] == 1) {
      return;
    }

    // Modified from jalview drawScale

    graphics.setColor(getForegroundColour());

    // Get the visible limits.
    //int[] visRange = getVisibleRange();
    int[] visRange = scaler.getXVisibleRange();
    int startCoord = (int) visRange[0];
    int endCoord = (int) visRange[1];

    int[] yrange = calculator.getYRange();

    Point start;
    Point end;
    start = transformer.toPixel(new Point((int) visRange[0], (int) yrange[0]));
    end = transformer.toPixel(new Point((int) visRange[1], (int) yrange[0]));
    graphics.drawLine(start.x, start.y, end.x, end.y);
    start = transformer.toPixel(new Point((int) visRange[0], (int) yrange[1]));
    end = transformer.toPixel(new Point((int) visRange[1], (int) yrange[1]));
    graphics.drawLine(start.x, start.y, end.x, end.y);

    graphics.setColor(plotColour.darker().darker());

    Point splitterStart = transformer.toPixel(visRange[0], 0);
    Point splitterEnd = transformer.toPixel(visRange[1], 0);
    graphics.drawLine(splitterStart.x, splitterStart.y, splitterEnd.x,
        splitterEnd.y);

    graphics.setColor(plotColour);

    if (startCoord < 1) {
      startCoord = 1;
    }
    if (endCoord < 1) {
      endCoord = 1;
    }
    if (endCoord > (int) calculator.getXRange()[1]) {
      endCoord = (int) calculator.getXRange()[1];
    }
    if (startCoord > (int) calculator.getXRange()[1]) {
      startCoord = (int) calculator.getXRange()[1];
    }

    java.util.List<Integer> posList = new java.util.LinkedList<Integer>();
    int prev = Integer.MIN_VALUE;
    for (int i = startCoord; i <= endCoord; ++i) {
      int x = transformer.toPixelX(i);
      if (x != prev && calculator.getScoreForPosition(i) != 0) {
        prev = x;
        posList.add(i);
      }
    }
    int[] positions = new int[posList.size()];
    for (int i = 0; i < posList.size(); ++i) {
      positions[i] = posList.get(i);
    }

    double[] scores = calculator.getScoresForPositions(positions);
    for (int i = 0; i < positions.length; ++i) {
      if (scores[i] != 0) {
        int x = scaler.toPixelX(positions[i]) - (int)(scaler.getXPixelsPerCoord() * 0.5);
        Point posStart = new Point(x, transformer.toPixelY(0));
        Point posEnd = new Point(x, transformer.toPixelY((int)scores[i]));
        graphics.drawLine(posStart.x, posStart.y, posEnd.x, posEnd.y);
      }
    }
  }

  public void setVisible(boolean state, boolean remove)
  {
    // if removing the graph permanently, make sure to detach mouse listeners
    // for this graph from apollo_panel
    if (remove) {
      apollo_panel.removeMouseMotionListener(mouseMotionListener);
      apollo_panel.removeMouseListener(mouseListener);
    }
    super.setVisible(state, remove);
  }
  
  /** Custom MouseMotionListener for handling mouse movement within the view
   * 
   */
  private class DiscreteGraphMouseMotionListener extends MouseMotionAdapter
  {
    private Popup popup;
    private PopupFactory popupFactory;
    
    public DiscreteGraphMouseMotionListener()
    {
      popupFactory = PopupFactory.getSharedInstance();
    }
    
    /** Action to be performed when the mouse in moved
     */
    public void mouseMoved(MouseEvent e)
    {
      if (getBounds().contains(e.getPoint())) {
        if (popup != null) {
          popup.hide();
        }
        int genomicX = scaler.toUser(e.getPoint()).x + 1;
        if (genomicX > 0) {
          List<Integer> genomicPositions = new LinkedList<Integer>();
          genomicPositions.add(genomicX);
          for (int i = genomicX - 1; scaler.toPixelX(i) == e.getPoint().x; --i) {
            genomicPositions.add(i);
          }
          for (int i = genomicX + 1; scaler.toPixelX(i) == e.getPoint().x; ++i) {
            genomicPositions.add(i);
          }
          int []genomicPositionsArray = new int[genomicPositions.size()];
          for (int i = 0; i < genomicPositions.size(); ++i) {
            genomicPositionsArray[i] = genomicPositions.get(i);
          }
          double []scores = calculator.getScoresForPositions(genomicPositionsArray);
          double maxScore = 0;
          int maxPosition = 0;
          for (int i = 0; i < scores.length; ++i) {
            if (Math.abs(scores[i]) > Math.abs(maxScore)) {
              maxScore = scores[i];
              maxPosition = genomicPositionsArray[i];
            }
          }

          //only display popup if there's a score and the mouse is hovering over the
          //drawn score
          Point p = transformer.toUser(e.getPoint());
          if (maxScore > 0 && p.y <= maxScore && p.y >= 0 ||
              maxScore < 0 && p.y >= maxScore && p.y <= 0) {
            Point screen = new Point(e.getPoint());
            SwingUtilities.convertPointToScreen(screen, getComponent());
            popup = popupFactory.getPopup(getComponent(), new JLabel(maxPosition + ": " + Double.toString(maxScore / calculator.getFactor())), screen.x, screen.y - 15);
            popup.show();
          }
        }
      }
    }
  }
  
  private class DiscreteGraphMouseListener extends MouseAdapter
  {
    
    private JPopupMenu popupMenu;
    
    public void mouseClicked(MouseEvent e)
    {
      if (popupMenu != null) {
        dispose();
      }
      if (getBounds().contains(e.getPoint())) {
        if (MouseButtonEvent.isRightMouseClick(e)) {
          popupMenu = new JPopupMenu();
          JMenuItem color = new JMenuItem("Change color");
          color.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
              Color c = JColorChooser.showDialog(getComponent(), "Choose graph color", getPlotColour());
              setPlotColour(c);
              setVisible(false, false);
              setVisible(true, false);
            }
          });
          popupMenu.add(color);
          JMenuItem remove = new JMenuItem("Remove graph");
          remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
              setVisible(false, true);
              dispose();
            }
          });
          popupMenu.add(remove);
          popupMenu.addSeparator();
          JMenuItem close = new JMenuItem("Close this menu");
          close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
              dispose();
            }
          });
          popupMenu.add(close);
          popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
      }
    }
    
    private void dispose()
    {
      popupMenu.setVisible(false);
      popupMenu = null;
    }
  }
  
}
