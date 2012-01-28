package jalview.gui;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;

public class ColorSelectPanel extends Panel implements ActionListener,
                                                       AdjustmentListener {

  SliderPanel sliderPanel;         // Contains the colour sliders
  public ColorPanel colorPanel;    // A canvas displaying the colour selected

  // RGB values
  int Red;
  int Green;
  int Blue;


  // Constructors - these are run when the object is created
  public ColorSelectPanel() {

    componentInit();
  }

  public ColorSelectPanel(Color c) {

    Red = c.getRed();
    Green = c.getGreen();
    Blue = c.getBlue();
 
    componentInit();
  }
  

  protected void componentInit() {
    sliderPanel = new SliderPanel();
    colorPanel = new ColorPanel();

    setLayout(new GridLayout(1,2));

    setColor(Red,Green,Blue);

    add(sliderPanel);
    add(colorPanel);

    sliderPanel.redSlider.tfield.addActionListener(this);
    sliderPanel.greenSlider.tfield.addActionListener(this);
    sliderPanel.blueSlider.tfield.addActionListener(this);

    sliderPanel.redSlider.slider.addAdjustmentListener(this);
    sliderPanel.greenSlider.slider.addAdjustmentListener(this);
    sliderPanel.blueSlider.slider.addAdjustmentListener(this);

    //This is needed for the canvas to be seen at all

    colorPanel.pogCanvas.resize(size().width/2,size().height);
  }

  public Color getColor() {
    return new Color(Red,Green,Blue);
  }


  // Two methods which set the colour on the panel
  // They update the stored values in the object (Red,Green,Blue)
  // and also update the screen (sliders and canvas)
  public void setColor(Color c) {
    setColor(c.getRed(),c.getGreen(),c.getBlue());
  }

  public void setColor(int red, int green, int blue) {
    Red = red;
    Green = green;
    Blue = blue;

    colorPanel.pogCanvas.red = red;
    colorPanel.pogCanvas.green = green;
    colorPanel.pogCanvas.blue = blue;

    sliderPanel.redSlider.setValue(red);
    sliderPanel.greenSlider.setValue(green);
    sliderPanel.blueSlider.setValue(blue);
  }

  //Handle the scrollbar events
  // This method is called automatically when a graphics event occurs
  public void adjustmentValueChanged(AdjustmentEvent e) {

    if (e.getSource() == sliderPanel.redSlider.slider) {
      Red = sliderPanel.redSlider.slider.getValue();

      setColor(Red,Green,Blue);
      colorPanel.pogCanvas.paint(colorPanel.pogCanvas.getGraphics());

    } else if (e.getSource() == sliderPanel.greenSlider.slider) {
      Green = sliderPanel.greenSlider.slider.getValue();

      setColor(Red,Green,Blue);
      colorPanel.pogCanvas.paint(colorPanel.pogCanvas.getGraphics());

    } else if (e.getSource() == sliderPanel.blueSlider.slider) {
      Blue = sliderPanel.blueSlider.slider.getValue();

      setColor(Red,Green,Blue);
      colorPanel.pogCanvas.paint(colorPanel.pogCanvas.getGraphics());
    }
  }


  //Deal with textfields in here - updates only done after a return
  public void actionPerformed(ActionEvent e) {

    if (e.getSource() == sliderPanel.redSlider.tfield) {
      Red = Integer.valueOf(sliderPanel.redSlider.tfield.getText()).intValue();
      sliderPanel.redSlider.slider.setValue(Red);
      setColor(Red,Green,Blue);
      colorPanel.pogCanvas.paint(colorPanel.pogCanvas.getGraphics());

    } else if (e.getSource() == sliderPanel.greenSlider.tfield) {
      Green = Integer.valueOf(sliderPanel.greenSlider.tfield.getText()).intValue();
      sliderPanel.greenSlider.slider.setValue(Green);
      setColor(Red,Green,Blue);
      colorPanel.pogCanvas.paint(colorPanel.pogCanvas.getGraphics());

    } else if (e.getSource() == sliderPanel.blueSlider.tfield) {
      Blue = Integer.valueOf(sliderPanel.blueSlider.tfield.getText()).intValue();
      sliderPanel.blueSlider.slider.setValue(Blue);
      setColor(Red,Green,Blue);
      colorPanel.pogCanvas.paint(colorPanel.pogCanvas.getGraphics());
    }
  }

} //End of class ColorSelectPanel


// This is the SliderPanel class

class SliderPanel extends Panel {

  public SPanel redSlider, greenSlider, blueSlider;
  int red,green,blue;



  public SliderPanel() {

    redSlider = new SPanel(red,0,255);
    greenSlider = new SPanel(green,0,255);
    blueSlider = new SPanel(blue,0,255);

    setLayout(new GridLayout(3,1));

    add(redSlider);
    add(greenSlider);
    add(blueSlider);

  }

} // End of class SliderPanel


class  SPanel extends Panel {

  Scrollbar slider;
  Canvas tmpCanvas;
  TextField tfield;
  int scrollvalue;

  public SPanel(int value, int min, int max) {

    super();
    scrollvalue = value;

    slider = new Scrollbar(Scrollbar.HORIZONTAL,0,5,min,max);

    tmpCanvas = new Canvas();

    tfield = new TextField(Integer.toString(scrollvalue));
    setLayout(new GridLayout(2,1));
    add(slider);
    add(tfield);

  }

  public void setValue(int value) {
    this.scrollvalue = value;
    slider.setValue(scrollvalue);
    tfield.setText(Integer.toString(value));
  }
}

class ColorPanel extends Panel {

  PogCanvas pogCanvas;
  int red,green,blue;

  public ColorPanel() {
    super();
    pogCanvas = new PogCanvas();
    setLayout(new BorderLayout());

    add("Center",pogCanvas);
  }

  public Dimension preferredSize() {
    return new Dimension(100,100);
  }

  public Dimension minimumSize() {
    return new Dimension(25,25);
  }
} //End of class ColorPanel


// Class canvas needs to be subclassed otherwise the paint method
// doens't seem to be called when the scrollvar events occur


class PogCanvas extends Canvas {

  int red,green,blue;

  public PogCanvas() {
    super();
  }

  public void paint(Graphics g) {
    if (g != null) {
      g.setColor(new Color(red,green,blue));
      g.fill3DRect(10,10,size().width-20,size().height-20,true);
    }
  }

  public void update(Graphics g) {
    paint(g);
  }

  public Dimension preferredSize() {
    return new Dimension(200,100);
  }

  public Dimension minimumSize() {
    return new Dimension(25,25);
  }
} //End of class myCanvas

