package apollo.gui.event;

import java.awt.event.*;
import java.awt.Event;
import javax.swing.SwingUtilities;
import java.io.*;
import java.util.Date;
import apollo.config.Config;

import org.apache.log4j.*;

/** 
 * Static methods for determining whether certain mouse button events have
 * taken place.  This class hides the messiness of checking mouse events on
 * Mac, where the swing utilities are buggy in JDK1.3.1 but ok in JDK1.4.

With JDK1.3.1:
On a ONE-button Mac:
- ctrl-click and option(alt)-click both answer true to isMiddleButton.  I
interpret this as a RIGHT mouse event (pop up menu).
- apple(cloverleaf)-click answers true to isRightButton.  I interpret this
as a MIDDLE mouse event (center the display).

On a FIVE-button Mac:
- The right of the middle three buttons answers false to isRightButton and
true to isMiddleButton.  I interpret this as a RIGHT mouse event.
- The middle (wheel) button answers false to isRightButton, false to
isMiddleButton, and true to isLeftButton.  I don't see any logical way to
make this behave as anything other than a left click (selection), since
the event appears identical to a left click in every aspect I've checked.
- The left of the three middle buttons answers true to isLeftButton.  Fine.
- The far-left buttons (on the side of the mouse--buttons I wouldn't
normally use) answer true to isRightButton, false to the others.  This
gets treated as MIDDLE button (center the display).

 * (Although this isn't currently used here, note that Windows responds TRUE to isAltDown when
 * it's not (and false when it is!) and same with isControlDown.)
 */

public class MouseButtonEvent {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(MouseButtonEvent.class);

  // True whether or not shift is held down
  public static boolean isLeftMouseClick(MouseEvent evt) {
    if (SwingUtilities.isLeftMouseButton(evt)) {
      logger.debug((new Date()).toString() + " MouseButtonEvent: got left mouse click");
      // On Macs with on single-button mouse, when you hit apple-click, it
      // answers yes to "is this a right-mouse event?" *and* yes to "is this a
      // left-mouse event"?  This is a Java bug that I thought had been fixed!
      // So if we're on a Mac, need to make sure this isn't really a right-mouse event.
      if (apollo.util.IOUtil.isMac() && SwingUtilities.isRightMouseButton(evt)) {
        logger.debug((new Date()).toString() + " MouseButtonEvent: hey, this isn't a left mouse click, it's a *right* mouse click!");
        return false;
      }
      else
        return true;
    }
    else
      return false;
  }

  public static boolean isMiddleMouseClick(MouseEvent evt) {
    if (logger.isDebugEnabled()) {
      logger.debug((new Date()).toString() + 
                   " MouseButtonEvent: checking for middle mouse event.\nJVM = " + Config.getJVM() + "\n  isRightMouseButton = " + SwingUtilities.isRightMouseButton(evt)  + ", isMiddleMouseButton = " + SwingUtilities.isMiddleMouseButton(evt)  + ", isLeftMouseButton = " + SwingUtilities.isLeftMouseButton(evt)  + ", apollo.util.IOUtil.isMac = " + apollo.util.IOUtil.isMac() + ", evt.isAltDown() = " + evt.isAltDown() + "; evt.isControlDown() = " + evt.isControlDown());
    }
    if ((!(macJDK1_3()) && SwingUtilities.isMiddleMouseButton(evt)) ||
	(macJDK1_3() && SwingUtilities.isRightMouseButton(evt))) {
      logger.debug((new Date()).toString() + " MouseButtonEvent: got middle mouse event");
      return true;
    }
    // On a five-button Mac, middle mouse event answers false to both
    // isMiddleMouseButton and isRightMouseButton.  If event also is not
    // a left click, it must, by process of elimination, be a middle mouse
    // event on a five-button Mac!
    else if (macJDK1_3() && 
	     (!(SwingUtilities.isLeftMouseButton(evt)) && !(SwingUtilities.isMiddleMouseButton(evt)) && !(SwingUtilities.isRightMouseButton(evt)))) {
      logger.debug("MouseButtonEvent: got middle mouse click on five-button Mac mouse");
      return true;
    }
    else
      return false;
  }

  public static boolean isMiddleMouseClickNoShift(MouseEvent evt) {
    return (isMiddleMouseClick(evt) && !isShift(evt));
  }
    
  public static boolean isRightMouseClick(MouseEvent evt) {
    if ((!(macJDK1_3()) && SwingUtilities.isRightMouseButton(evt)) ||
	(macJDK1_3() && SwingUtilities.isMiddleMouseButton(evt))) {
      logger.debug("MouseButtonEvent: got right mouse event.");
      return true;
    }
    else
      return false;
  }

  public static boolean isRightMouseClickNoShift(MouseEvent evt) {
    return (isRightMouseClick(evt) && !isShift(evt));
  }
    
  public static boolean isRightMouseClickWithShift(MouseEvent evt) {
    return (isRightMouseClick(evt) && isShift(evt));
  }

  /** Returns true if shift key is held down */
  public static boolean isShift(MouseEvent evt) {
    return ((evt.getModifiers() & Event.SHIFT_MASK) != 0);
  }

  private static boolean macJDK1_3() {
    if (apollo.util.IOUtil.isMac() && Config.getJVM().startsWith("1.3"))
      return true;
    else
      return false;
  }
}
