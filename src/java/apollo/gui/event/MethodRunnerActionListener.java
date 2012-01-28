/****
 * MethodRunnerActionListener
 *
 * Runs the named method when the actionEvent fires
 */

package apollo.gui.event;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.lang.reflect.*;

import org.apache.log4j.*;

public class MethodRunnerActionListener implements ActionListener {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(MethodRunnerActionListener.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------
 
  Object target;
  String methodName;
  Method method;

  /**
   * methodName is the name of the method to run (no parens)
   *    (the method must not take any args)
   * target is the object to run the method on
   *
   * if the method does not exist for that object, an exception is thrown
   */
  public MethodRunnerActionListener(Object target, String methodName) throws NoSuchMethodException {
    this.target = target;
    method = target.getClass().getMethod(methodName, new Class[0]);
  }

  /**
   * Runs the method named in the constructor. If the method throws an
   * exception, it's stack trace will be printed, but the exception will
   * not be propegated
   *
   * It should not be possible for the illegal access exception or
   * IllegalArgumentException methods to be thrown
   * (any IllegalAccessException should have happened on construction, and
   *  the method must be a zero parameter method, so there can't be an
   *  IllegalArgumentException)
   * If they are somehow thrown, a stack trace is printed and the exception
   * is not propegated
   */
  public void actionPerformed(ActionEvent evt) {
    try {
      method.invoke(target, new Object[0]);
    } catch (InvocationTargetException e) {
      logger.error(e.getMessage(), e);
    }
    catch (IllegalAccessException e) {
      logger.error("Unexpected condition, IllegalAccessException", e);
    }
    catch (IllegalArgumentException e) {
      logger.error("Unexpected condition, IllegalArgumentException", e);
    }
  }
}
