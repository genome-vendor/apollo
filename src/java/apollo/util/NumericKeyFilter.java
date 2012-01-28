package apollo.util;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/** Add as key listener to a text field. Only allows numeric values to get typed.
 * Singleton pattern - only need one of these things - 
 * retrieve with NumericKeyFilter.getFilter();
 */

public class NumericKeyFilter extends KeyAdapter {

  private static NumericKeyFilter singleton;

  private NumericKeyFilter() {}


  public static NumericKeyFilter getFilter() {
    if (singleton==null)
      singleton = new NumericKeyFilter();
    return singleton;
  }

  public void keyTyped(KeyEvent evt) {
    if (!Character.isDigit(evt.getKeyChar()) &&
        !Character.isISOControl(evt.getKeyChar())) {
      evt.consume();
      _beep();
    }
  }
  /** on linux this just prints out cntrl-G */
  private void _beep() {
    byte []  beep = new byte[1];
    beep[0] = 007;
    System.out.print(new String(beep));
  }
}
