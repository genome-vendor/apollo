package apollo.gui.genomemap;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import apollo.datamodel.*;
import apollo.seq.*;
import apollo.config.Config;
import apollo.gui.ScoreCalculator;
import apollo.gui.Transformer;
import apollo.gui.WindowScoreCalculator;
import apollo.gui.menus.*;

public class GraphView extends ScrollAdjustedView
  implements ChangeListener {
  ScoreCalculator            calculator;
  Color   plotColour = Config.getCoordForeground();

  public GraphView(JComponent ap, String name) {
    super(ap, name, false, 31);
    getTransform().setYOrientation(Transformer.UP);
  }

  public ScoreCalculator getScoreCalculator() {
    return calculator;
  }

  public void clear() {
    setScoreCalculator(null);
  }

  public void setScoreCalculator(ScoreCalculator calc) {
    if (calculator != null && calculator instanceof WindowScoreCalculator) {
      ((WindowScoreCalculator)calculator).getModel().removeChangeListener(this);
    }

    calculator = calc;
    if (calc == null) 
      return; // just a clear

    int [] yrange = calc.getYRange();
    double tenpc = (double)(yrange[1]-yrange[0]+1)/10;
    if (tenpc == 0) {
      tenpc = 1;
    }
    yrange[0] -= tenpc;
    yrange[1] += tenpc;
    transformer.setYRange(calc.getYRange());
    // X range will be set on sync
    transformer.setXRange(calc.getXRange());
    setXOrientation(transformer.getXOrientation());
    if (calculator instanceof WindowScoreCalculator) {
      ((WindowScoreCalculator)calculator).getModel().addChangeListener(this);
    }
  }

  /** I dont think this is actually used */
  public void stateChanged(ChangeEvent evt) {
    WindowScoreCalculator wsc = ((WindowScoreCalculator)calculator);
    wsc.setWinSize(wsc.getModel().getValue());
    getComponent().repaint();
  }

  // Overridden LinearView methods
  public void paintView() {

    graphics.setColor(getBackgroundColour());

    // Clear the whole view
    if (!transparent)
      graphics.fillRect(getBounds().x,
			getBounds().y,
			getBounds().width,
			getBounds().height);
    else
      graphics.drawRect(getBounds().x,
			getBounds().y,
			getBounds().width,
			getBounds().height);

    // if paint happens mid load with no curation set or calculator
    if (calculator == null) 
      return; 

    // Modified from jalview drawScale

    graphics.setColor(getForegroundColour());

    int [] visRange = getVisibleRange();

    // Get the visible limits.
    int startCoord = (int)visRange[0];
    int endCoord   = (int)visRange[1];

    int [] yrange = calculator.getYRange();
    Point start;
    Point end;
    start = transformer.toPixel(new Point((int)visRange[0],(int)yrange[0]));
    end   = transformer.toPixel(new Point((int)visRange[1],(int)yrange[0]));
    graphics.drawLine(start.x,start.y,end.x,end.y);
    start = transformer.toPixel(new Point((int)visRange[0],(int)yrange[1]));
    end   = transformer.toPixel(new Point((int)visRange[1],(int)yrange[1]));
    graphics.drawLine(start.x,start.y,end.x,end.y);

    if (transformer.getXPixelsPerCoord() > 0.001) {

      graphics.setColor(plotColour);

      if (startCoord < 1) {
        startCoord = 1;
      }
      if (endCoord > (int)calculator.getXRange()[1]) {
        endCoord = (int)calculator.getXRange()[1];
      }
      int lowPix;
      int highPix;
      int incr;
      if (!reverseComplement) {
        lowPix = start.x;
        highPix = end.x;
        incr = 1;
      } else {
        lowPix = end.x;
        highPix = start.x;
        incr = -1;
      }

      int [] positions = new int [highPix-lowPix+1];
      int nPos = 0;
      int lastX = -10;
      for (int i = start.x; i != end.x + incr; i+=incr) {
        Point pos = transformer.toUser(i,0);
        if (pos.x > 1 && pos.x <= (int)calculator.getXRange()[1] &&
            pos.x != lastX) {
          positions[nPos++] = pos.x;
          lastX = pos.x;
        }
      }

      int [] tmpPos = new int[nPos];
      System.arraycopy(positions,0,tmpPos,0,nPos);
      positions = tmpPos;
      double  [] scores = calculator.getScoresForPositions(positions);

      Point oldpnt = null;
      nPos = 0;
      lastX = -10;
      for (int i = start.x; i != end.x + incr; i+=incr) {
        Point pos = transformer.toUser(i,0);
        if (pos.x > 1 && pos.x <= (int)calculator.getXRange()[1] &&
            pos.x != lastX) {
          Point pnt = transformer.toPixel(new Point((int)positions[nPos],
                                          (int)scores[nPos++]));
          //System.out.println("pixel = " + pnt);
          if (oldpnt != null) {
            graphics.drawLine(oldpnt.x,oldpnt.y,pnt.x,pnt.y);
            oldpnt.x = pnt.x;
            oldpnt.y = pnt.y;
          } else {
            oldpnt = new Point(pnt);
          }
          lastX = pos.x;
        }
      }
    }
  }

  public void setPlotColour(Color colour) {
    plotColour = colour;
  }

  public Color getPlotColour() {
    return plotColour;
  }

  // Class specific methods
  public void setXOrientation(int direction) {
    super.setXOrientation (direction);
    // validate the direction before using it
    if (direction == Transformer.LEFT ||
	direction == Transformer.RIGHT)
      calculator.setXOrientation(direction);
  }

}
