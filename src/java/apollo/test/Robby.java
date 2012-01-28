package apollo.test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

// Robby is a test class for apollo on my laptop. It moves the mouse around
// using a Robot. If apollo is at its startup dialog when this class
// is run, part of chromosome 1 will be loaded and a new annotation created,
// This will ONLY work on a machine with a screen resolution of 1400x1050.
public class Robby {

  static Robot robby;
  private Robby() {}

  public static void main(String [] args) {

    try {
      GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      GraphicsDevice gs = ge.getDefaultScreenDevice();
      robby = new Robot(gs);
      robby.mouseMove(100,100);
      robby.mouseMove(650,550);
      robby.mousePress(InputEvent.BUTTON1_MASK);
      robby.mouseRelease(InputEvent.BUTTON1_MASK);
      robby.keyPress(KeyEvent.VK_1);
      robby.mouseMove(650,590);
      robby.mousePress(InputEvent.BUTTON1_MASK);
      robby.mouseRelease(InputEvent.BUTTON1_MASK);
      robby.keyPress(KeyEvent.VK_1);
      Thread.currentThread().sleep(100);
      robby.mouseMove(650,610);
      robby.mousePress(InputEvent.BUTTON1_MASK);
      robby.mouseRelease(InputEvent.BUTTON1_MASK);
      robby.keyPress(KeyEvent.VK_1);
      robby.keyPress(KeyEvent.VK_0);
      robby.keyPress(KeyEvent.VK_0);
      robby.keyPress(KeyEvent.VK_0);
      robby.keyPress(KeyEvent.VK_0);
      robby.keyPress(KeyEvent.VK_0);
      Thread.currentThread().sleep(100);
      robby.mouseMove(650,670);
      robby.mousePress(InputEvent.BUTTON1_MASK);
      robby.mouseRelease(InputEvent.BUTTON1_MASK);
      Thread.currentThread().sleep(15000);
      robby.mouseMove(1000,425);
      robby.mousePress(InputEvent.BUTTON1_MASK);
      slowMove(new Point(1000,425), new Point(1000,460),5);
      robby.mouseRelease(InputEvent.BUTTON1_MASK);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  protected static void slowMove(Point from, Point to,int steps) throws java.lang.InterruptedException {

    int diffX = to.x - from.x;
    int diffY = to.y - from.y;

    int stepX = diffX/steps;
    int stepY = diffY/steps;

    if (stepX > 1 || stepY > 1) {
      for (int i=0;i<steps;i++) {
        robby.mouseMove(from.x + i*stepX ,from.y+i*stepY);
        Thread.currentThread().sleep(300);
      }
    }
    robby.mouseMove(to.x, to.y);
    Thread.currentThread().sleep(300);
  }
}

