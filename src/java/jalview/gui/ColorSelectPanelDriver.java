package jalview.gui;

import java.awt.*;
import java.applet.*;

public class ColorSelectPanelDriver extends Driver {

  // this method is run first - a frame is created and then
  // a ColorSelectPanel object.  The ColorSelectPanel object is put into
  // the frame and displayed.

  public static void main(String[] args) {
    // Create frame
    Frame f = new Frame("Colour selector");

    // Create ColorSelectPanel object
    ColorSelectPanel cs = new ColorSelectPanel(new Color(100,1,24));

    // How do we want the components laid out
    f.setLayout(new BorderLayout());

    // Put the color panel in the frame
    f.add("Center",cs);

    // resize the frame
    f.resize(300,200);

    // and finally display
    f.show();

  }


}
