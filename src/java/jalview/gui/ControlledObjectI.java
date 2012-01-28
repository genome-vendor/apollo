package jalview.gui;

/**
 * Interface defining methods required to add a Controller to an object
 */

public interface ControlledObjectI extends java.io.Serializable {
  /**
   * Sets the Controller for the object
   */
  public void setController(Controller controller);
  /**
   * Gets the Controller for the object
   */
  public Controller getController();
  public Object getControllerWindow();
}
