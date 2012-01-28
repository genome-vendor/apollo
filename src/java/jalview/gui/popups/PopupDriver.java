package jalview.gui.popups;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class PopupDriver {
  public static void main(String [] argv) {
    JFrame frame = new JFrame("Popup driver");
    
    Popup poppy = new Popup(frame,null,null,"Test popup");

    poppy.addBasic();

    frame.pack();
    frame.show();
  }
}

